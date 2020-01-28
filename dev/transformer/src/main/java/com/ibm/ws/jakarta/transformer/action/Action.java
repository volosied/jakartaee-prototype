package com.ibm.ws.jakarta.transformer.action;

import java.io.InputStream;
import java.io.OutputStream;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

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

	//

	/**
	 * Answer a short name for this action.  This must be unique among
	 * the collection of actions in use, and will be used to record change
	 * information.
	 *
	 * @return A unique short name for this action.
	 */
	String getName();

	/**
	 * Tell if a resource is to be transformed.
	 *
	 * @param resourceName The name of the resource.
	 *
	 * @return True or false telling if the resource is to be transformed.
	 */
	boolean select(String resourceName);

	//

	/**
	 * Tell if a resource is of a type which is handled by this action.
	 *
	 * @param resourceName The name of the resource.
	 *
	 * @return True or false telling if the resource can be handled by this action.
	 */
	boolean accept(String resourceName);

	//

	/**
	 * Apply this action on an input stream.
	 *
	 * Answer a data structure containing output data.  The output data
	 * will be the original data if the input stream if this action declined
	 * to process the input data.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 *g
	 * @return Transformed input data.
	 *
	 * @throws JakartaTransformException Thrown if the transform failed. 
	 */
	InputStreamData apply(String inputName, InputStream inputStream)
		throws JakartaTransformException;

	/**
	 * Apply this action on an input stream.
	 *
	 * Answer a data structure containing output data.  The output data
	 * will be the original data if the input stream if this action declined
	 * to process the input data.
	 *
	 * The input count may be {@link InputStreamData#UNKNOWN_LENGTH}, in which
	 * case all available data will be read from the input stream.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 * @param inputCount The count of bytes available in the input stream.
	 *
	 * @return Transformed input data.
	 *
	 * @throws JakartaTransformException Thrown if the transform failed. 
	 */
	InputStreamData apply(String inputName, InputStream inputStream, int inputCount)
		throws JakartaTransformException;

	/**
	 * Apply this action on an input stream.
	 *
	 * Write transformed data to the output stream.
	 *
	 * The input count may be {@link InputStreamData#UNKNOWN_LENGTH}, in which
	 * case all available data will be read from the input stream.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputStream A stream containing input data.
	 * @param inputCount The count of bytes available in the input stream.
	 * @param outputStream A stream to which to write transformed data.
	 *
	 * @return True or false telling if any changes were made to the input
	 *     data.
	 *
	 * @throws JakartaTransformException Thrown if the transform failed. 
	 */	
	boolean apply(String inputName, InputStream inputStream, int inputCount, OutputStream outputStream)
		throws JakartaTransformException;

	/**
	 * Apply this action on an input bytes.
	 *
	 * Answer transformed bytes.  Answer null if no changes were made
	 * by this action.
	 *
	 * @param inputName A name associated with the input data.
	 * @param inputBytes Input data.
	 * @param inputCount The count of input bytes.  This will often be
	 *     different than the length of the input data array.
	 *
	 * @return Transformed bytes.  Null if this action made no changes to
	 *     the input data.
	 *
	 * @throws JakartaTransformException Thrown if the transform failed. 
	 */
	ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws JakartaTransformException;

	//

	/**
	 * Answer changes recorded during the most recent application
	 * of this action.
	 *
	 * Each call to the same action instance obtains the same change instance.
	 *  
	 * @return The changes recorded during the most recent application
	 *     of this action.
	 */
	Changes getChanges();

	/**
	 * Tell if the last application of this action had changes.
	 * 
	 * @return True or false telling if the last application of this action
	 *     had changes.
	 */
	boolean hasChanges();
}
