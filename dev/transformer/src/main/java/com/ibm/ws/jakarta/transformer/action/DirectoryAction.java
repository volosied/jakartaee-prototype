package com.ibm.ws.jakarta.transformer.action;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;

import java.io.File;

public interface DirectoryAction extends Action {
    
    public void apply(File inputFile, File outputFile)  throws JakartaTransformException; 
    
	@Override
	DirectoryChanges getChanges();
}
