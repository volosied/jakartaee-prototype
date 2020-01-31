package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.Action;
import com.ibm.ws.jakarta.transformer.action.ArchiveChanges;
import com.ibm.ws.jakarta.transformer.action.ClassAction;
import com.ibm.ws.jakarta.transformer.action.JarAction;
import com.ibm.ws.jakarta.transformer.action.ManifestAction;
import com.ibm.ws.jakarta.transformer.action.ServiceConfigAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.FileUtils;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

public class JarActionImpl extends ActionImpl implements JarAction {

	public JarActionImpl(ActionImpl parentAction) {
		super(parentAction);
	}

	public JarActionImpl(Set<String> includes, Set<String> excludes, Map<String, String> renames) {
		super(includes, excludes, renames);
	}

	public JarActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames) {

		super(logStream, isTerse, isVerbose, includes, excludes, renames);
	}

	//

	public String getName() {
		return "Jar Action";
	}

	//

	@Override
	protected JarChangesImpl newChanges() {
		return new JarChangesImpl();
	}

	@Override
	public JarChangesImpl getChanges() {
		return (JarChangesImpl) super.getChanges();
	}

	protected void recordUnaccepted(String resourceName) {
		verbose( "Resource [ %s ]: Not accepted\n", resourceName );

		getChanges().record();
	}

	protected void recordUnselected(Action action, boolean hasChanges, String resourceName) {
		verbose(
			"Resource [ %s ] Action [ %s ]: Accepted but not selected\n",
			resourceName, action.getName() );

		getChanges().record(action, hasChanges);
	}

	protected void recordTransform(Action action, String resourceName) {
		verbose(
			"Resource [ %s ] Action [ %s ]: Changes [ %s ]\n",
			resourceName, action.getName(), action.hasChanges() );

		getChanges().record(action);
	}

	//

	@Override
	public boolean accept(String resourceName) {
		return resourceName.endsWith(".jar");
	}

	//

	@SuppressWarnings("resource") // Streams are closed by the caller.
	@Override
	public void apply(
		String inputPath, InputStream inputStream,
		String outputPath, OutputStream outputStream) throws JakartaTransformException {

		setResourceNames(inputPath, outputPath);

		JarInputStream jarInputStream;
		try {
			jarInputStream = new JarInputStream(inputStream); // throws IOException
		} catch ( IOException e ) {
			throw new JakartaTransformException("Failed to open jar input [ " + inputPath + " ]", e);
		}

		JarOutputStream jarOutputStream;
		try {
			jarOutputStream = new JarOutputStream(outputStream);
		} catch ( IOException e ) {
			throw new JakartaTransformException("Failed to open jar output [ " + outputPath + " ]", e);
		}

		try {
			applyJar(inputPath, jarInputStream, outputPath, jarOutputStream);
			// throws JakartaTransformException

		} finally {
			try {
				jarOutputStream.finish(); // throws IOException
			} catch ( IOException e ) {
				throw new JakartaTransformException("Failed to complete output [ " + outputPath + " ]", e);
			}
		}
	}

	protected void applyJar(
		String inputPath, JarInputStream jarInputStream,
		String outputPath, JarOutputStream jarOutputStream) throws JakartaTransformException {

		String prevName = null;
		String inputName = null;

		try {
			ClassAction classAction = new ClassActionImpl(this);
			ServiceConfigAction configAction = new ServiceConfigActionImpl(this);
			ManifestAction manifestAction = new ManifestActionImpl(this);

			byte[] buffer = new byte[FileUtils.BUFFER_ADJUSTMENT];

			JarEntry inputEntry;
			while ( (inputEntry = (JarEntry) jarInputStream.getNextEntry()) != null ) {
				inputName = inputEntry.getName();

				Action selectedAction;

				if ( classAction.accept(inputName) ) {
					selectedAction = classAction;
				} else if ( configAction.accept(inputName) ) {
					selectedAction = configAction;
				} else if ( manifestAction.accept(inputName) ) {
					selectedAction = manifestAction;
				} else {
					selectedAction = null;
				}

				if ( !select(inputName) || (selectedAction == null) ) {
					if ( selectedAction == null ) {
						recordUnaccepted(inputName);
					} else {
						recordUnselected(selectedAction, !ArchiveChanges.HAS_CHANGES, inputName);
					}

					// TODO: Should more of the entry details be transferred?

					JarEntry outputEntry = new JarEntry(inputName);
					jarOutputStream.putNextEntry(outputEntry); // throws IOException
					FileUtils.transfer(jarInputStream, jarOutputStream, buffer); // throws IOException 
					jarOutputStream.closeEntry(); // throws IOException					

				} else {
					long inputLength = inputEntry.getSize();

					int intInputLength;
					if ( inputLength == -1L ) {
						intInputLength = -1;
					} else {
						intInputLength = FileUtils.verifyArray(0, inputLength);
					}

					InputStreamData outputData;

					outputData = selectedAction.apply(inputName, jarInputStream, intInputLength);

					recordTransform(selectedAction, inputName);

					// TODO: Should more of the entry details be transferred?

					JarEntry outputEntry = new JarEntry(inputName);
					jarOutputStream.putNextEntry(outputEntry); // throws IOException
					FileUtils.transfer(outputData.stream, jarOutputStream, buffer); // throws IOException 
					jarOutputStream.closeEntry(); // throws IOException					
				}

				prevName = inputName;
				inputName = null;
			}

		} catch ( IOException e ) {
			String message;
			if ( inputName != null ) { // Actively processing an entry.
				message = "Failure while processing [ " + inputName + " ] from [ " + inputPath + " ] to [ " + outputPath + " ]";
			} else if ( prevName != null ) { // Moving to a new entry but not the first entry.
				message = "Failure after processing [ " + prevName + " ] from [ " + inputPath + " ] to [ " + outputPath + " ]";
			} else { // Moving to the first entry.
				message = "Failed to process first entry of [ " + inputPath + " ] to [ " + outputPath + " ]";
			}
			throw new JakartaTransformException(message, e);
		}
	}

	// Byte base JAR conversion is not supported.

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws JakartaTransformException {
		throw new UnsupportedOperationException();
	}
}