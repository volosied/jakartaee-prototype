package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.ManifestAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

public class ManifestActionImpl extends ActionImpl implements ManifestAction {

	public static final boolean IS_MANIFEST = true;
	public static final boolean IS_FEATURE = !IS_MANIFEST;

	public ManifestActionImpl(ActionImpl parent, boolean isManifest) {
		super(parent);
		this.isManifest = isManifest;
	}

	public ManifestActionImpl(
		Set<String> includes, Set<String> excludes, Map<String, String> renames,
		boolean isManifest) {

		super(includes, excludes, renames);

		this.isManifest = isManifest;
	}

	public ManifestActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames,
		boolean isManifest) {

		super(logStream, isTerse, isVerbose,
		      includes, excludes, renames);

		this.isManifest = isManifest;
	}

	public ManifestActionImpl(ActionImpl parent) {
		this(parent, IS_MANIFEST);
	}

	public ManifestActionImpl(Set<String> includes, Set<String> excludes, Map<String, String> renames) {
		this(includes, excludes, renames, IS_MANIFEST);
	}

	public ManifestActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames) {

		this(logStream, isTerse, isVerbose, includes, excludes, renames, IS_MANIFEST);
	}

	//

	public String getName() {
		return ( getIsManifest() ? "Manifest Action" : "Feature Action" );
	}

	//

	private final boolean isManifest;

	public boolean getIsManifest() {
		return isManifest;
	}

	public boolean getIsFeature() {
		return !isManifest;
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
		int resourceLen = resourceName.length();

		int minLen;
		String reqEnd;
		if ( getIsManifest() ) {
			minLen = 11;
			reqEnd = "MANIFEST.MF";
		} else {
			minLen = 3;
			reqEnd = ".MF";
		}

		if ( resourceLen < minLen ) {
			return false;
		} else {
			return resourceName.regionMatches(true, resourceLen - minLen, reqEnd, 0, minLen);
		}
	}

	//

	@Override
	public ByteData apply(String initialName, byte[] initialBytes, int initialCount)
		throws JakartaTransformException {

		String className = getClass().getSimpleName();
		String methodName = "apply";

		verbose("[ %s.%s ]: [ %s ] Initial bytes [ %s ]\n", className, methodName, initialName, initialCount);

		clearChanges();
		setResourceNames(initialName, initialName);

		ByteData initialData = new ByteData(initialName, initialBytes, 0, initialCount);

		Manifest initialManifest;
		try {
			initialManifest = new Manifest( initialData.asStream() );
		} catch ( IOException e ) {
			error("Failed to parse manifest [ %s ]\n", e, initialName);
			return null;
		}

		Manifest finalManifest = new Manifest();

		transform(initialName, initialManifest, finalManifest);

		log("[ %s.%s ]: [ %s ] Replacements [ %s ]\n",
			getClass().getSimpleName(), "transform",
			initialName, getChanges().getReplacements());

		if ( !hasChanges() ) {
			verbose("[ %s.%s ]: [ %s ] Null transform", className, methodName, initialName);
			return null;
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(initialCount);
		try {
			write(finalManifest, outputStream); // throws IOException
		} catch ( IOException e ) {
			error("Failed to write manifest [ %s ]\n", e, initialName);
			return null;
		}

		byte[] finalBytes = outputStream.toByteArray();
		verbose("[ %s.%s ]: [ %s ] Active transform; final bytes [ %s ]\n", className, methodName, initialName, finalBytes.length);

		return new ByteData(initialName, finalBytes); 
	}

	protected void transform(String inputName, Manifest initialManifest, Manifest finalManifest) {
		Attributes initialMainAttributes = initialManifest.getMainAttributes();
		Attributes finalMainAttributes = finalManifest.getMainAttributes();

		transform(inputName, "main", initialMainAttributes, finalMainAttributes);

		Map<String, Attributes> initialEntries = initialManifest.getEntries();
		Map<String, Attributes> finalEntries = finalManifest.getEntries();

		for ( Map.Entry<String, Attributes> initialAttributeEntry : initialEntries.entrySet() ) {
			String entryKey = initialAttributeEntry.getKey();
			Attributes initialAttributes = initialAttributeEntry.getValue();

			Attributes finalAttributes = new Attributes( initialAttributes.size() );
			finalEntries.put(entryKey, finalAttributes);

			transform(inputName, entryKey, initialAttributes, finalAttributes);
		}
	}

	protected void transform(
		String inputName, String entryName,
		Attributes initialAttributes, Attributes finalAttributes) {

		verbose(
			"Transforming [ %s ]: [ %s ] Attributes [ %d ]\n",
			inputName, entryName, initialAttributes.size() );

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

		verbose(
			"Transformed [ %s ]: [ %s ] Attributes [ %d ]\n",
			inputName, entryName, finalAttributes.size() );
	}

	protected void write(Manifest manifest, OutputStream outputStream) throws IOException {
		if ( getIsManifest() ) {
			writeAsManifest(manifest, outputStream); // throws IOException
		} else {
			writeAsFeature(manifest, outputStream); // throws IOException
		}
	}

	protected void writeAsManifest(Manifest manifest, OutputStream outputStream) throws IOException {
		manifest.write(outputStream); // throws IOException
	}

	// Copied and updated from:
	// https://github.com/OpenLiberty/open-liberty/blob/integration/
	// dev/wlp-featureTasks/src/com/ibm/ws/wlp/feature/tasks/FeatureBuilder.java
	
	@SuppressWarnings("unused")
	protected void writeAsFeature(Manifest manifest, OutputStream outputStream) throws IOException {
		PrintWriter writer = new PrintWriter(outputStream);

		StringBuilder builder = new StringBuilder();

		for ( Map.Entry<Object, Object> mainEntry : manifest.getMainAttributes().entrySet() ) {
			writer.append( mainEntry.getKey().toString() );
			writer.append(": ");

			String value = (String) mainEntry.getValue();
			if ( value.indexOf(',') == -1 ) {
				writer.append(value);

			} else {
				Parameters parms = OSGiHeader.parseHeader(value);

				boolean continuedLine = false;
				for ( Map.Entry<String, Attrs> parmEntry : parms.entrySet() ) {
					if ( continuedLine ) {
						writer.append(",\r\n ");
					}

					// bnd might have added ~ characters if there are duplicates in 
					// the source, so we should remove them before we output it so we
					// get back to the original intended content.

					String parmName = parmEntry.getKey();
					int index = parmName.indexOf('~');
					if ( index != -1 ) {
						parmName = parmName.substring(0, index);
					}
					writer.append(parmName);

					Attrs parmAttrs = parmEntry.getValue();
					for (Map.Entry<String, String> parmAttrEntry : parmAttrs.entrySet()) {
						String parmAttrName = parmAttrEntry.getKey();
						String parmAttrValue = quote( builder, parmAttrEntry.getValue() );

						writer.append("; ");
						writer.append(parmAttrName);
						writer.append('=');
						writer.append(parmAttrValue);
					}

					continuedLine = true;
				}
			}

			writer.append("\r\n");
		}

		writer.flush();
	}

	public String quote(StringBuilder sb, String value) {
		@SuppressWarnings("unused")
		boolean isClean = OSGiHeader.quote(sb, value);
		String quotedValue = sb.toString();
		sb.setLength(0);
		return quotedValue;
	}
}
