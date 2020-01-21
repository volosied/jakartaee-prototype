package com.ibm.ws.jakarta.transformer.action;

public interface ClassChanges extends Changes {
	String getInputClassName();
	void setInputClassName(String inputClassName);

	String getOutputClassName();
	void setOutputClassName(String outputClassName);

	String getInputSuperName();
	void setInputSuperName(String inputSuperName);

	String getOutputSuperName();
	void setOutputSuperName(String outputSuperName);

	int getModifiedInterfaces();
	void setModifiedInterfaces(int modifiedInterfaces);
	void addModifiedInterface();

	int getModifiedFields();
	void setModifiedFields(int modifiedFields);
	void addModifiedField();

	int getModifiedMethods();
	void setModifiedMethods(int modifiedMethods);
	void addModifiedMethod();

	int getModifiedAttributes();
	void setModifiedAttributes(int modifiedAttributes);
	void addModifiedAttribute();

	int getModifiedConstants();
	void setModifiedConstants(int modifiedContants);
	void addModifiedConstant();
}