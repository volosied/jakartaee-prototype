package com.ibm.ws.jakarta.transformer.action;

public class ArchiveChangesImpl extends ChangesImpl implements ArchiveChanges {

	protected ArchiveChangesImpl() {
		super();

		this.clearChanges();
	}

	//

	@Override
	public boolean hasChanges() {
		return ( super.hasChanges() ||
		         ( (changedClasses == 0) && (changedServiceConfigs == 0) ) );
	}

	@Override
	public void clearChanges() {
		changedClasses = 0;
		unchangedClasses = 0;

		changedServiceConfigs = 0;
		unchangedServiceConfigs = 0;

		additionalResources = 0;

		super.clearChanges();
	}

	//

	private int changedClasses;
	private int unchangedClasses;

	private int changedServiceConfigs;
	private int unchangedServiceConfigs;

	private int additionalResources;

	//

	@Override
	public int getChangedClasses() {
		return changedClasses;
	}

	@Override
	public void addChangedClass() {
		changedClasses++;
	}

	@Override
	public int getUnchangedClasses() {
		return unchangedClasses;
	}

	@Override
	public void addUnchangedClass() {
		unchangedClasses++;
	}

	//

	@Override
	public int getChangedServiceConfigs() {
		return changedServiceConfigs;
	}

	@Override
	public void addChangedServiceConfig() {
		changedServiceConfigs++;
	}

	@Override
	public int getUnchangedServiceConfigs() {
		return unchangedServiceConfigs;
	}

	@Override
	public void addUnchangedServiceConfig() {
		unchangedServiceConfigs++;
	}

	//

	@Override
	public int getAdditionalResources() {
		return additionalResources;
	}

	@Override
	public int addAdditionalResource() {
		return ++additionalResources;
	}
}
