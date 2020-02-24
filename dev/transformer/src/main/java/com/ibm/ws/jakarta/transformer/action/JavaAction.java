package com.ibm.ws.jakarta.transformer.action;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.util.ByteData;

public interface JavaAction extends Action {

	@Override
	JavaChanges getChanges();

	public String getName();

	public boolean accept(String resourceName);

	public ByteData apply(String initialName, byte[] initialBytes, int initialCount)
			throws JakartaTransformException;
}
