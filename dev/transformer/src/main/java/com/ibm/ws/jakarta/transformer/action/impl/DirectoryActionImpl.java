package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.Action;
import com.ibm.ws.jakarta.transformer.action.ActionType;
import com.ibm.ws.jakarta.transformer.action.ContainerChanges;
import com.ibm.ws.jakarta.transformer.action.DirectoryAction;
import com.ibm.ws.jakarta.transformer.action.JarAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.FileUtils;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

public class DirectoryActionImpl extends ContainerActionImpl implements DirectoryAction {

    public DirectoryActionImpl(LoggerImpl logger,
                               InputBufferImpl buffer,
                               SelectionRuleImpl selectionRule,
                               SignatureRuleImpl signatureRule) {

            super(logger, buffer, selectionRule, signatureRule);
    }

	//

	public String getName() {
		return "Directory Action";
	}

	//

	@Override
	protected DirectoryChangesImpl newChanges() {
		return new DirectoryChangesImpl();
	}

	@Override
	public DirectoryChangesImpl getChanges() {
		return (DirectoryChangesImpl) super.getChanges();
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
		return new File(resourceName).isDirectory();
	}
	
	@Override
	public void apply(String inputPath, InputStream inputStream, String outputPath, OutputStream outputStream)
			throws JakartaTransformException {
		throw new JakartaTransformException("Dude! Don't give me streams.  It's Files I need.");
		
	}
	
	public void apply(File inputFile, File outputFile)  throws JakartaTransformException {

        transformDirectoryTree(".", inputFile, outputFile);
    	
	}
	
	/**
	 * 
	 * @param inputRelativePath path of inputFile not including the file name
	 * @param inputFile
	 * @param outputFile
	 * @throws JakartaTransformException
	 */
	protected void transformDirectoryTree(String inputRelativePath, File inputFile, File outputFile)  throws JakartaTransformException {
		
		setResourceNames(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
		String inputRelPath = inputRelativePath + "/" + inputFile.getName();

		try {

			if (inputFile.isDirectory()) {

				if (!outputFile.exists() ) {
					outputFile.mkdir();
				}

				String[] files = inputFile.list();

				for (String file : files) {
					File srcFile = new File(inputFile, file);
					File destFile = new File(outputFile, file);
					transformDirectoryTree(inputRelPath, srcFile, destFile);
				}

			} else {

				Action selectedAction = acceptAction(inputRelPath);
				
				if ( !select(inputRelPath) || (selectedAction == null) ) {

					if ( selectedAction == null ) {
						recordUnaccepted(inputRelPath);
					} else {
						recordUnselected(selectedAction, !ContainerChanges.HAS_CHANGES, inputRelPath);
					}

				} else {
					InputStream inStream = new FileInputStream(inputFile);
					OutputStream outStream = new FileOutputStream(outputFile); 

					long inputLength = inputFile.length();
					int intLength = FileUtils.verifyArray(0, inputLength);

					if (selectedAction instanceof JarAction) {
					    ((JarActionImpl)selectedAction).apply(inputFile.getAbsolutePath(), 
                                                              inStream, 
                                                              outputFile.getAbsolutePath(), 
                                                              outStream);
					} else {
						InputStreamData outputData = selectedAction.apply(inputFile.getName(), inStream, intLength);

						recordTransform(selectedAction, inputFile.getName());

						FileUtils.transfer(outputData.stream, outStream);

						inStream.close();
						outStream.close();
					}

				} 
			}
		} catch (IOException ioe) {

			throw new JakartaTransformException(ioe.getMessage(), ioe);
		}
	}	

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws JakartaTransformException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ActionType getActionType() {
		return ActionType.DIRECTORY;
	}
}