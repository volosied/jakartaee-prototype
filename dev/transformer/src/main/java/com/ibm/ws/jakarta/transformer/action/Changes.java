package com.ibm.ws.jakarta.transformer.action;

public interface Changes {
	String getInputResourceName();
	void setInputResourceName(String inputResourceName);

	String getOutputResourceName();
	void setOutputResourceName(String outputResourceName);

	boolean hasChanges();
	boolean hasNonResourceNameChanges();
	boolean hasResourceNameChange();

	void clearChanges();
}
