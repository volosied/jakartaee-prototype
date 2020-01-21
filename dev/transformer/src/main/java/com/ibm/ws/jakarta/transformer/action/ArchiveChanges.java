package com.ibm.ws.jakarta.transformer.action;

public interface ArchiveChanges extends Changes {
	int getChangedClasses();
	void addChangedClass();

	int getUnchangedClasses();
	void addUnchangedClass();

	//

	int getChangedServiceConfigs();
	void addChangedServiceConfig();

	int getUnchangedServiceConfigs();
	void addUnchangedServiceConfig();

	//

	int getAdditionalResources();
	int addAdditionalResource();
}
