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

	@Override
	public boolean accept(String resourceName) {
		return resourceName.contains(MANIFEST_MF);
	}

	//

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputCount)
		throws JakartaTransformException {

		clearChanges();
		setResourceNames(inputName, inputName);

		InputStream inputStream = new ByteArrayInputStream(inputBytes, 0, inputCount);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputCount);

		if ( !transform(inputName, inputStream, outputStream) ) {
			return null;
		}

		byte[] outputBytes = outputStream.toByteArray();
		return new ByteData(inputName, outputBytes, 0, outputBytes.length);
	}

	protected boolean transform(String inputName, InputStream inputStream, OutputStream outputStream) {
		Manifest initialManifest;
		try {
			initialManifest = new Manifest(inputStream);
		} catch ( IOException e ) {
			error("Failed to read manifest [ %s ]\n", e, inputName);
			return false;
		}

		Manifest finalManifest = new Manifest();

		transform(inputName, initialManifest, finalManifest);

		if ( !hasChanges() ) {
			return false;
		}

		try {
			finalManifest.write(outputStream);
			return true;
		} catch ( IOException e ) {
			error("Failed to write manifest [ %s ]\n", e, inputName);
			return false;
		}
	}

	protected void transform(String inputName, Manifest initialManifest, Manifest finalManifest) {
		Attributes initialMainAttributes = initialManifest.getMainAttributes();
		Attributes finalMainAttributes = finalManifest.getMainAttributes();

		transform(initialMainAttributes, finalMainAttributes);

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
