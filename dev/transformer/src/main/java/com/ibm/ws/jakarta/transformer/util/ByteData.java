package com.ibm.ws.jakarta.transformer.util;

public class ByteData {
	public final String name;
	public final byte[] data;
	public final int offset;
	public final int length;

	public ByteData(String name, byte[] data, int offset, int length) {
		// System.out.println("ByteData [ " + name + " ] [ " + offset + " ] [ " + length + " ] [ " + data + " ]");

		this.name = name;
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

}
