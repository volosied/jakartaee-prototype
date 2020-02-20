package com.ibm.ws.jakarta.transformer.action;

import java.io.OutputStream;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;

import java.io.InputStream;

public interface DirectoryAction extends Action {
	void apply(
		String inputPath, InputStream inputStream,
		String outputPath, OutputStream outputStream)
		throws JakartaTransformException;

	@Override
	DirectoryChanges getChanges();
}
