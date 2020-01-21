package com.ibm.ws.jakarta.transformer.action;

public abstract class ChangesImpl implements Changes {
	public ChangesImpl() {
		this.clearChanges();
	}

	@Override
	public boolean hasChanges() {
		return ( !inputResourceName.equals(outputResourceName) );
	}

	@Override
	public void clearChanges() {
		inputResourceName = null;
		outputResourceName = null;
	}

	//

	private String inputResourceName;
	private String outputResourceName;

	@Override
	public String getInputResourceName() {
		return inputResourceName;
	}

	@Override
	public void setInputResourceName(String inputResourceName) {
		this.inputResourceName = inputResourceName;
	}

	@Override
	public String getOutputResourceName() {
		return outputResourceName;
	}

	@Override
	public void setOutputResourceName(String outputResourceName) {
		this.outputResourceName = outputResourceName;
	}
}
