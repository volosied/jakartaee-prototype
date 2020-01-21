package com.ibm.ws.jakarta.transformer.action;

import java.io.InputStream;

import com.ibm.ws.jakarta.transformer.util.ByteData;

import java.io.IOException;

public interface Action {

	public static String resourceNameToClassName(String resourceName) {
		String className = resourceName.substring( resourceName.length() - ".class".length() );
		className = className.replace('/',  '.');
		return className;
	}

	public static String classNameToResourceName(String className) {
		String resourceName = className.replace('.',  '/');
		resourceName = resourceName + ".class";
		return resourceName;
	}

	public static String classNameToBinaryTypeName(String className) {
		return className.replace('.',  '/');
	}

	//

	/**
	 * Convert a resource name to a class name.
	 *
	 * @param resourceName The resource name which is to be converted.
	 *
	 * @return The class name obtained from the resource name.
	 */
	String asClassName(String resourceName);

	/**
	 * Convert a class name to a resource name.
	 *
	 * @param className The class name which is to be converted.
	 *
	 * @return The resource name obtained from the class name.
	 */
	String asResourceName(String className);

	/**
	 * Tell if a resource is to be transformed.
	 *
	 * @param resourceName The name of the resource.
	 *
	 * @return True or false telling if the resource is to be transformed.
	 */
	boolean select(String resourceName);

	/**
	 * Tell if a class is to be transformed.
	 *
	 * @param className The name of the class.
	 *
	 * @return True or false telling if the class is to be transformed.
	 */
	boolean selectClass(String className);

	//

	int UNKNOWN_LENGTH = -1;

	/**
	 * Read bytes from an input stream.  Answer byte data and
	 * a count of bytes read.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream A stream to be read.
	 * @param inputCount The count of bytes to read from the stream.
	 *     {@link Action#UNKNOWN_LENGTH} if the count of
	 *     input bytes is not known.
	 *
	 * @return Byte data from the read.
	 * 
	 * @throws IOException Indicates a read failure.
	 */
	ByteData read(String inputName, InputStream inputStream, int inputCount) throws IOException;
}
