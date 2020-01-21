package com.ibm.ws.jakarta.transformer.action;

public interface ServiceConfigChanges extends Changes {
	String getInputResourceName();
	void setInputResourceName(String inputResourceName);

	String getOutputResourceName();
	void setOutputResourceName(String outputResourceName);

	void addChangedProvider();
	int getChangedProviders();

	void addUnchangedProvider();
	int getUnchangedProviders();
}