package com.ibm.ws.jakarta.transformer.action;

import java.io.OutputStream;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

import java.io.InputStream;

public interface ClassAction extends Action {
	/**
	 * Tell if a resource is a class type resource.
	 *
	 * @param resourceName The name of the resource.
	 *
	 * @return True or false telling if the resource is a class type resource.
	 */
	boolean accept(String resourceName);

	/**
	 * Constant value to use to perform a class action without
	 * asking for class change data.
	 */
	ClassChanges NULL_CHANGES = null;

	/**
	 * Apply the class action to a named input stream.
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
	 * @param classChanges Data indicating what changes were
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
		ClassChanges classChanges) throws JakartaTransformException;

	/**
	 * Helper: Apply this action to input bytes.  Provide the result bytes
	 * as an input stream.  The count of input bytes is not known.  Do
	 * not track changes.
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
	 * @param classChanges Data indicating what changes were
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
		ClassChanges classChanges) throws JakartaTransformException;

	/**
	 * Apply this action to input bytes.  Answer the result bytes.
	 *
	 * The result bytes may be the same as the input bytes.
	 *
	 * @param inputName A name associated with the input bytes.
	 * @param inputBytes Bytes to which to apply this action.
	 * @param inputCount The count of input bytes.
	 * @param classChanges Data indicating what changes were
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
		ClassChanges classChanges) throws JakartaTransformException;
}
