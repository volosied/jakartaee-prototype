package com.ibm.ws.jakarta.transformer.action.impl;

import com.ibm.ws.jakarta.transformer.action.InputBuffer;

public class InputBufferImpl implements InputBuffer {
	public InputBufferImpl() {
		this.inputBuffer = null;
	}

	private byte[] inputBuffer;

	@Override
	public byte[] getInputBuffer() {
		return inputBuffer;
	}

	@Override
	public void setInputBuffer(byte[] inputBuffer) {
		this.inputBuffer = inputBuffer;
	}
}
