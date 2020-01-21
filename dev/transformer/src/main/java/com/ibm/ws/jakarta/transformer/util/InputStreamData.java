package com.ibm.ws.jakarta.transformer.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class InputStreamData {
	public final String name;
	public final InputStream stream;
	public final int length;

	public InputStreamData(String name, InputStream stream, int length) {
		this.name = name;
		this.stream = stream;
		this.length = length;
	}
	
	public InputStreamData(ByteData byteData) {
		this.name = byteData.name;
		this.stream = new ByteArrayInputStream( byteData.data, 0, byteData.length );
		this.length = byteData.length;
	}
}
