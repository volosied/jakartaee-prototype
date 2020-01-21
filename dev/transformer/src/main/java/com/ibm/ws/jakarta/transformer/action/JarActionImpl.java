package com.ibm.ws.jakarta.transformer.action;

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

	@SuppressWarnings("resource") // Streams are closed by the caller.
	@Override
	public void apply(
		String inputPath, InputStream inputStream,
		String outputPath, OutputStream outputStream,
		JarChanges jarChanges) throws JakartaTransformException {

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
			applyJar(inputPath, jarInputStream, outputPath, jarOutputStream, jarChanges);
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
		String outputPath, JarOutputStream jarOutputStream,
		JarChanges jarChanges) throws JakartaTransformException {

		String prevName = null;
		String inputName = null;

		try {
			ClassAction classAction = new ClassActionImpl(this);
			ClassChanges classChanges = new ClassChangesImpl();

			ServiceConfigAction configAction = new ServiceConfigActionImpl(this);
			ServiceConfigChanges configChanges = new ServiceConfigChangesImpl();

			byte[] buffer = new byte[FileUtils.BUFFER_ADJUSTMENT];

			JarEntry inputEntry;
			while ( (inputEntry = (JarEntry) jarInputStream.getNextEntry()) != null ) {
				inputName = inputEntry.getName();

				boolean isClass;
				boolean isConfig;

				if ( classAction.accept(inputName) ) {
					isClass = true;
					isConfig = false;
				} else if ( configAction.accept(inputName) ) {
					isClass = false;
					isConfig = true;
				} else {
					isClass = false;
					isConfig = false;
				}

				if ( !select(inputName) || (!isClass && !isConfig) ) {
					if ( isClass ) {
						jarChanges.addUnchangedClass();
					} else if ( isConfig ) {
						jarChanges.addUnchangedServiceConfig();
					} else {
						jarChanges.addAdditionalResource();
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

					if ( isClass ) {
						classChanges.clearChanges();

						outputData = classAction.apply(
							inputName, jarInputStream, intInputLength,
							classChanges);

						if ( classChanges.hasChanges() ) {
							jarChanges.addChangedClass();
						} else {
							jarChanges.addUnchangedClass();
						}

					} else {
						configChanges.clearChanges();

						outputData = configAction.apply(
							inputName, jarInputStream, intInputLength,
							configChanges);

						if ( configChanges.hasChanges() ) {
							jarChanges.addChangedServiceConfig();
						} else {
							jarChanges.addUnchangedServiceConfig();
						}
					}

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
}