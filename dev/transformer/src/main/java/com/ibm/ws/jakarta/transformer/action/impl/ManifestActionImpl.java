package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.ManifestAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;

public class ManifestActionImpl extends ActionImpl implements ManifestAction {

	public ManifestActionImpl(ActionImpl parent) {
		super(parent);
	}

	public ManifestActionImpl(Set<String> includes, Set<String> excludes, Map<String, String> renames) {
		super(includes, excludes, renames);
	}

	public ManifestActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames) {

		super(logStream, isTerse, isVerbose,
		      includes, excludes, renames);
	}

	//

	public String getName() {
		return "Manifest Action";
	}

	//

	@Override
	protected ManifestChangesImpl newChanges() {
		return new ManifestChangesImpl();
	}

	@Override
	public ManifestChangesImpl getChanges() {
		return (ManifestChangesImpl) super.getChanges();
	}

	protected void addReplacement() {
		getChanges().addReplacement();
	}

	//

	public static final String MANIFEST_RESOURCE_NAME = "META-INF/MANIFEST.MF";
	
	@Override
	public boolean accept(String resourceName) {
		return resourceName.equals(MANIFEST_RESOURCE_NAME);
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputCount)
		throws JakartaTransformException {

		clearChanges();
		setResourceNames(inputName, inputName);

		InputStream inputStream = new ByteArrayInputStream(inputBytes, 0, inputCount);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputCount);

		transform(inputName, inputStream, outputStream);

		if ( !hasChanges() ) {
			return null;

		} else {
			byte[] outputBytes = outputStream.toByteArray();
			return new ByteData(inputName, outputBytes, 0, outputBytes.length);
		}
	}

	public void transform(String inputName, InputStream inputStream, OutputStream outputStream)
		throws JakartaTransformException {

		Manifest initialManifest;
		try {
			initialManifest = new Manifest(inputStream);
		} catch ( IOException e ) {
			throw new JakartaTransformException("Failed to read manifest [ " + inputName + " ]", e);
		}

		Manifest finalManifest = new Manifest();

		transform(inputName, initialManifest, finalManifest);

		if ( hasChanges() ) {
			try {
				finalManifest.write(outputStream);
			} catch ( IOException e ) {
				throw new JakartaTransformException("Failed to write manifest [ " + inputName + " ]", e);
			}
		}
	}

	protected void transform(String inputName, Manifest initialManifest, Manifest finalManifest) {
		Attributes initialMainAttributes = initialManifest.getMainAttributes();
		Attributes finalMainAttributes = finalManifest.getMainAttributes();

		for ( Map.Entry<Object, Object> mainEntries : initialMainAttributes.entrySet() ) {
			String mainKey = (String) mainEntries.getKey();
			String initialMainValue = (String) mainEntries.getValue();

			String finalMainValue = replaceEmbeddedPackages(initialMainValue);
			if ( finalMainValue == null ) {
				finalMainValue = initialMainValue;
			} else {
				addReplacement();
			}

			finalMainAttributes.put(mainKey, finalMainValue);
		}

		Map<String, Attributes> initialEntries = initialManifest.getEntries();
		Map<String, Attributes> finalEntries = finalManifest.getEntries();

		for ( Map.Entry<String, Attributes> initialAttributeEntry : initialEntries.entrySet() ) {
			String entryKey = initialAttributeEntry.getKey();
			Attributes initialAttributes = initialAttributeEntry.getValue();

			Attributes finalAttributes = new Attributes( initialAttributes.size() );
			finalEntries.put(entryKey, finalAttributes);

			transform(initialAttributes, finalAttributes);
		}
	}

	protected void transform(Attributes initialAttributes, Attributes finalAttributes) {
		for ( Map.Entry<Object, Object> entries : initialAttributes.entrySet() ) {
			Object key = entries.getKey();
			String initialValue = (String) entries.getValue();

			String finalValue = replaceEmbeddedPackages(initialValue);
			if ( finalValue == null ) {
				finalValue = initialValue;
			} else {
				addReplacement();
			}

			finalAttributes.put(key, finalValue);
		}
	}
}
