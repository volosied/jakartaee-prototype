package com.ibm.ws.jakarta.transformer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteData {
	public final String name;
	public final byte[] data;
	public final int offset;
	public final int length;

	public ByteData(String name, byte[] data) {
		this(name, data, 0, data.length);
	}

	public ByteData(String name, byte[] data, int offset, int length) {
		// System.out.println("ByteData [ " + name + " ] [ " + offset + " ] [ " + length + " ] [ " + data + " ]");

		this.name = name;
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	public ByteArrayInputStream asStream() {
		return new ByteArrayInputStream(data, offset, length);
	}

	public void write(OutputStream outputStream) throws IOException {
		outputStream.write(data, offset, length); // throws IOException
	}
}
