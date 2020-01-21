package com.ibm.ws.jakarta.transformer.action;

import java.io.InputStream;
import java.io.OutputStream;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

public interface ServiceConfigAction extends Action {
	/**
	 * Tell if a resource is a service configuration type resource.
	 *
	 * @param resourceName The name of the resource.
	 *
	 * @return True or false telling if the resource is a service configuration type resource.
	 */
	boolean accept(String resourceName);

	/**
	 * Constant value to use to perform a class action without
	 * asking for class change data.
	 */
	ServiceConfigChanges NULL_CHANGES = null;

	/**
	 * Apply this action to a named input stream.
	 * 
	 * Answer an input stream which provides transformed
	 * input bytes.
	 *
	 * Answer true or false telling if the output stream
	 * contains the same bytes as the input stream.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream The stream containing input bytes.
	 * @param inputCount The count of input bytes.
	 *     {@link Action#UNKNOWN_LENGTH_INT} if the count of
	 *     input bytes is not known.
	 * @param outputStream A stream to receive output bytes.
	 * @param configChanges Data indicating what changes were
	 *     made to the input bytes.
	 *
	 * @return True or false telling if any changes were made to
	 *     the input bytes.
	 *
	 * @throws JakartaTransformException Indicates a read failure,
	 *     a write failure, or a transformation failure.
	 */
	boolean apply(
		String inputName, InputStream inputStream, int inputCount,
		OutputStream outputStream,
		ServiceConfigChanges configChanges) throws JakartaTransformException;

	/**
	 * Apply this action to input bytes.  Provide the result bytes
	 * as an input stream.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream The stream containing input bytes.
	 *
	 * @return An stream containing result bytes.  This will be the
	 *     input stream if this action does not apply to the input
	 *     stream.
	 *
	 * @throws JakartaTransformException Indicates a read failure or
	 *     a transformation failure.
	 */
	InputStreamData apply(String inputName, InputStream inputStream)
		throws JakartaTransformException;

	/**
	 * Apply this action to input bytes.  Provide the result bytes
	 * as an input stream.
	 *
	 * @param inputName The name of the input stream.
	 * @param inputStream The stream containing input bytes.
	 * @param inputCount The count of input bytes.
	 *     {@link Action#UNKNOWN_LENGTH_INT} if the count of
	 *     input bytes is not known.
	 * @param configChanges Data indicating what changes were
	 *     made to the input bytes.
	 *
	 * @return An stream containing result bytes.  This will be the
	 *     input stream if this action does not apply to the input
	 *     stream.
	 *
	 * @throws JakartaTransformException Indicates a read failure or
	 *     a transformation failure.
	 */
	InputStreamData apply(
		String inputName, InputStream inputStream, int inputCount,
		ServiceConfigChanges configChanges) throws JakartaTransformException;

	/**
	 * Apply this action to input bytes.  Answer the result bytes.
	 *
	 * The result bytes may be the same as the input bytes.
	 *
	 * @param inputName A name associated with the input bytes.
	 * @param inputBytes Bytes to which to apply this action.
	 * @param inputCount The count of input bytes.
	 * @param configChanges Data indicating what changes were
	 *     made to the input bytes.
	 *
	 * @return Result data as a byte array.  Answer null if no changes
	 *     were made. 
	 *
	 * @throws JakartaTransformException Indicates a transformation
	 *     failure.
	 */
	ByteData apply(
		String inputName, byte[] inputBytes, int inputCount,
		ServiceConfigChanges configChanges) throws JakartaTransformException;
}
