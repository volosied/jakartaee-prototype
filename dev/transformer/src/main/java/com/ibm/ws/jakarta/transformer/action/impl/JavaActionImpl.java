package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.ActionType;
import com.ibm.ws.jakarta.transformer.action.JavaAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;

public class JavaActionImpl extends ActionImpl implements JavaAction {	

	public JavaActionImpl(
			LoggerImpl logger,
			InputBufferImpl buffer,
			SelectionRuleImpl selectionRule,
			SignatureRuleImpl signatureRule) {

		super(logger, buffer, selectionRule, signatureRule);
	}

	//

	public String getName() {
		return ( "Java Action" );
	}

	@Override
	public ActionType getActionType() {
		return ( ActionType.JAVA );
	}

	//

	@Override
	protected JavaChangesImpl newChanges() {
		return new JavaChangesImpl();
	}

	@Override
	public JavaChangesImpl getChanges() {
		return (JavaChangesImpl) super.getChanges();
	}

	protected void addReplacement() {
		getChanges().addReplacement();
	}

	protected void addReplacements(int additions) {
		getChanges().addReplacements(additions);
	}

	//

	@Override
	public boolean accept(String resourceName) {
        return resourceName.endsWith(".java");
	}

	//
    
	   /**
     * Replace all embedded packages of specified text with replacement
     * packages.
     *
     * @param text Text embedding zero, one, or more package names.
     *
     * @return The text with all embedded package names replaced.  Null if no
     *     replacements were performed.
     */
    protected String replacePackages(String text) {

        //System.out.println("replacePackages: Initial text [ " + text + " ]");

        String initialText = text;

        for ( Map.Entry<String, String> renameEntry : getPackageRenames().entrySet() ) {
            String key = renameEntry.getKey();
            int keyLen = key.length();

            //System.out.println("replacePackages: Next target [ " + key + " ]");

            int textLimit = text.length() - keyLen;

            int lastMatchEnd = 0;
            while ( lastMatchEnd <= textLimit ) {
                int matchStart = text.indexOf(key, lastMatchEnd);
                if ( matchStart == -1 ) {
                    break;
                }

                if ( !isTrueMatch(text, textLimit, matchStart, keyLen) ) {
                    lastMatchEnd = matchStart + keyLen;
                    continue;
                }

                String value = renameEntry.getValue();
                int valueLen = value.length();

                String head = text.substring(0, matchStart);
                String tail = text.substring(matchStart + keyLen);

//                int tailLenBeforeReplaceVersion = tail.length();            
//                tail = replacePackageVersion(tail, getPackageVersions().get(value));
//                int tailLenAfterReplaceVersion = tail.length();

                text = head + value + tail;

                lastMatchEnd = matchStart + valueLen;

                // Replacing the key or the version can increase or decrease the text length.
                textLimit += (valueLen - keyLen);
//                textLimit += (tailLenAfterReplaceVersion - tailLenBeforeReplaceVersion);

                // System.out.println("Next text [ " + text + " ]");
            }
        }

        if ( initialText == text) {
            // System.out.println("Final text is unchanged");
            return null;
        } else {
            // System.out.println("Final text [ " + text + " ]");
            return text;
        }
    }
	
    public void apply(File inputFile, File outputFile)  throws JakartaTransformException {
        
        setResourceNames(inputFile.getName(), outputFile.getName());
        int replacements = 0;
        try (BufferedReader   bReader = new BufferedReader(new FileReader(inputFile));
             FileOutputStream fos     = new FileOutputStream(outputFile);
             BufferedWriter   bWriter = new BufferedWriter(new OutputStreamWriter(fos))) {
            
               String inLine;

               while ((inLine = bReader.readLine()) != null) {

                   String outLine = replacePackages(inLine);
                   if (outLine == null) {
                       outLine = inLine;     
                   } else {
                       replacements++;
                   }
                   bWriter.write(outLine + "\n");
               }

           } catch (IOException e) {
               throw new JakartaTransformException(e.getMessage(), e.getCause());
           } finally {
               addReplacements(replacements); 
           }
    }

    @Override
    public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws JakartaTransformException {
        throw new UnsupportedOperationException();
    }
}
