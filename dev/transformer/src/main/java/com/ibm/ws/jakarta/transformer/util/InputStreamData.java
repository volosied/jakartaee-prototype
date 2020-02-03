package com.ibm.ws.jakarta.transformer.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class InputStreamData {
	/** A name associated with the input stream. */
	public final String name;

	/** A stream containing input data. */
	public final InputStream stream;

	/**
	 * Control parameter: Indicates that the number of bytes which are
	 * available in an input stream is unknown.
	 */
	public static final int UNKNOWN_LENGTH = -1;

	/**
	 * The count of bytes available in the input stream.
	 * will be {@link #UNKNOWN_LENGTH} if the number of
	 * available bytes is not known.
	 */
	public final int length;

	/**
	 * Create input data for a name stream.
	 *
	 * @param name A name associated with the data.
	 * @param stream An stream containing the input data.
	 * @param length The number of bytes available in the
	 *     stream.  Possibly {@link #UNKNOWN_LENGTH}.
	 */
	public InputStreamData(String name, InputStream stream, int length) {
		this.name = name;
		this.stream = stream;
		this.length = length;
	}

	/**
	 * Create input data from byte data.
	 *
	 * The input data is almost a direct conversion of the byte data,
	 * except that the bytes of the byte data are converted to a
	 * byte array based input stream.
	 *
	 * @param byteData Byte data from which to create input data.
	 */
	public InputStreamData(ByteData byteData) {
		this.name = byteData.name;
		this.stream = new ByteArrayInputStream( byteData.data, byteData.offset, byteData.length );
		this.length = byteData.length;
	}
}
