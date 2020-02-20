package com.ibm.ws.jakarta.transformer.action.impl;

import com.ibm.ws.jakarta.transformer.action.Changes;
import com.ibm.ws.jakarta.transformer.action.JarChanges;

public abstract class ChangesImpl implements Changes {
	public ChangesImpl() {
		// Empty
	}

	@Override
	public boolean hasChanges() {
		return hasResourceNameChange() || hasNonResourceNameChanges();
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

	@Override
	public boolean hasResourceNameChange() {
		return ( !inputResourceName.equals(outputResourceName) );
	}

	//

	@Override
	public boolean hasNonResourceNameChanges() {
		return false;
	}
}
