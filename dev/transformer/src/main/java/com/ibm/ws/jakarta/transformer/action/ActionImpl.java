package com.ibm.ws.jakarta.transformer.action;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.FileUtils;

import aQute.bnd.signatures.ArrayTypeSignature;
import aQute.bnd.signatures.BaseType;
import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.JavaTypeSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.SimpleClassTypeSignature;
import aQute.bnd.signatures.ThrowsSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;
import aQute.bnd.signatures.TypeVariableSignature;

public abstract class ActionImpl implements Action {

	//

	public ActionImpl(ActionImpl parent) {
		ActionImpl useRoot = parent.getRoot(); 
		this.root = ( (useRoot == null) ? parent : useRoot );
		this.parent = parent;

		this.logStream = null;
		this.isTerse = false;
		this.isVerbose = false;

		//

		this.included = null;
		this.excluded = null;

		this.packageRenames = null;

		//

		this.unchangedBinaryTypes = null;
		this.changedBinaryTypes = null;

		this.unchangedSignatures = null;
		this.changedSignatures = null;

		this.unchangedDescriptors = null;
		this.changedDescriptors = null;
	}

	//

	private final ActionImpl root;

	public ActionImpl getRoot() {
		return root;
	}

	private final ActionImpl parent;

	public ActionImpl getParent() {
		return parent;
	}

	//

	public static final PrintStream NULL_STREAM = null;

	public static final boolean IS_TERSE = true;
	public static final boolean IS_NOT_TERSE = false;
	
	public static final boolean IS_VERBOSE = true;
	public static final boolean IS_NOT_VERBOSE = false;

	public static final boolean DO_INVERT = true;
	public static final boolean DO_NOT_INVERT = false;

	public ActionImpl(Set<String> includes, Set<String> excludes, Map<String, String> renames) {
		this(NULL_STREAM, IS_NOT_TERSE, IS_NOT_VERBOSE,
			 includes, excludes, renames);
	}

	public ActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames) {

		this.root = null;
		this.parent = null;

		this.logStream = logStream;
		this.isTerse = isTerse;
		this.isVerbose = isVerbose;

		this.included = new HashSet<String>(includes);
		this.excluded = new HashSet<String>(excludes);

		Map<String, String> useRenames = new HashMap<String, String>( renames.size() );
		for ( Map.Entry<String, String> renameEntry : renames.entrySet() ) {
			System.out.println("Binary conversion from [ " + renameEntry.getKey() + " ] to [ " + renameEntry.getValue() + " ]");
			useRenames.put( renameEntry.getKey(), renameEntry.getValue() );
		}
		this.packageRenames = useRenames;
		
		this.unchangedBinaryTypes = new HashSet<>();
		this.changedBinaryTypes = new HashMap<>();

		this.unchangedSignatures = new HashSet<>();
		this.changedSignatures = new HashMap<>();

		this.unchangedDescriptors = new HashSet<>();
		this.changedDescriptors = new HashMap<>();
	}

	//

	private final PrintStream logStream;
	private final boolean isTerse;
	private final boolean isVerbose;

	public PrintStream getLogStream() {
		return logStream;
	}

	public boolean getIsTerse() {
		return ( (root == null) ? root.getIsTerse() : isTerse );
	}

	public boolean getIsVerbose() {
		return ( (root == null) ? root.getIsVerbose() : isVerbose );
	}

	public void log(String text, Object... parms) {
		if ( root != null ) {
			root.log(text, parms);
		} else {
			if ( (logStream != null) && !isTerse ) {
				if ( parms.length == 0 ) {
					logStream.println(text);
				} else {
					logStream.printf(text, parms);
				}
			}
		}
	}

	protected void verbose(String text, Object... parms) {
		if ( root != null ) {
			root.verbose(text, parms);
		} else {
			if ( (logStream != null) && isVerbose ) {
				if ( parms.length == 0 ) {
					logStream.println(text);
				} else {
					logStream.printf(text, parms);
				}
			}
		}
	}

	//

	@Override
	public String asClassName(String resourceName) {
		return Action.resourceNameToClassName(resourceName);
	}

	@Override
	public String asResourceName(String className) {
		return Action.classNameToResourceName(className);
	}

	private final Set<String> included;
	private final Set<String> excluded;

	@Override
	public boolean select(String resourceName) {
		if ( root != null ) {
			return root.select(resourceName);

		} else {
			if ( !included.isEmpty() ) {
				return included.contains(resourceName);
			} else {
				return !excluded.contains(resourceName);
			}
		}
	}

	@Override
	public boolean selectClass(String className) {
		return select( asResourceName(className) );
	}

	//

	private final Map<String, String> packageRenames;

	protected String renamePackage(String packageName) {
		if ( root != null ) {
			return root.renamePackage(packageName);
		} else {
			return packageRenames.getOrDefault(packageName, null);
		}
	}

    //

	private final Map<String, String> changedBinaryTypes;
	private final Set<String> unchangedBinaryTypes;

	/**
	 * Modify a fully qualified type name according to the package rename table.
	 * Answer either the transformed type name, or, if the type name was not changed,
	 * a wrapped null.
	 * 
	 * @param inputName A fully qualified type name which is to be transformed.
	 *
	 * @return The transformed type name, or a wrapped null if no changed was made.
	 */
	protected String transformBinaryType(String inputName) {
		if ( root != null ) {
			return root.transformBinaryType(inputName);
		}

		System.out.println("Input type [ " + inputName + " ]");

		if ( unchangedBinaryTypes.contains(inputName) ) {
			System.out.println("Unchanged (Prior)");
			return null;
		}

		String outputName = changedBinaryTypes.get(inputName);
		if ( outputName != null ) {
			System.out.println("Change to [ " + outputName + " ] (Prior)");
			return outputName;
		}

		char c = inputName.charAt(0);
		if ( (c == '[') || ((c == 'L') && (inputName.charAt(inputName.length() - 1) == ';')) ) {
			JavaTypeSignature inputSignature = JavaTypeSignature.of( inputName.replace('$', '.') );
			JavaTypeSignature outputSignature = transform(inputSignature);
			if ( outputSignature != null ) {
				outputName = outputSignature.toString().replace('.', '$');
			} else {
				// Leave outputName null.
			}

		} else {
			int lastSlashOffset = inputName.lastIndexOf('/');
			if ( lastSlashOffset != -1 ) {
				String inputPackage = inputName.substring(0, lastSlashOffset);
				System.out.println("Input package [ " + inputPackage + " ]");
				String outputPackage = renamePackage(inputPackage);
				if ( outputPackage != null ) {
					System.out.println("Output package [ " + outputPackage + " ]");
					outputName = outputPackage + inputName.substring(lastSlashOffset);
				} else {
					// Leave outputName null.
				}
			} else {
				// Leave outputName with null;
			}
		}

		if ( outputName == null ) {
			unchangedBinaryTypes.add(inputName);
			System.out.println("Unchanged");
		} else {
			changedBinaryTypes.put(inputName, outputName);
			System.out.println("Change to [ " + outputName + " ]");
		}

		return outputName;
	}

	/**
	 * Cache of transformed signatures.
	 * 
	 * A single unified mapping is used, even through there are three different
	 * types of signatures.  The different types of signatures each has its
	 * own syntax, meaning, there are not equal values across signature types.
	 */
	
	private final Set<String> unchangedSignatures;
	private final Map<String, String> changedSignatures;

	protected static enum SignatureType {
		CLASS, FIELD, METHOD
	}

	/**
	 * Transform a class, field, or method signature.
	 * 
	 * Answer a wrapped null if the signature is not changed by the transformation
	 * rules.
	 *
	 * @param input The signature value which is to be transformed.
	 * @param signatureType The type of the signature value.
	 *
	 * @return The transformed signature value.  A wrapped null if no change
	 *     was made to the value.
	 */
	protected String transform(String input, SignatureType signatureType) {
		if ( root != null ) {
			return root.transform(input, signatureType);
		}

		if ( unchangedSignatures.contains(input) ) {
			return null;
		}

		String output = changedSignatures.get(input);
		if ( output != null ) {
			return output;
		}

		if ( signatureType == SignatureType.CLASS ) {
			ClassSignature inputSignature = ClassSignature.of(input);
			ClassSignature outputSignature = transform(inputSignature);
			if ( outputSignature != null ) {
				output = outputSignature.toString();
			} else {
				// leave output null;
			}

		} else if ( signatureType == SignatureType.FIELD ) {
			FieldSignature inputSignature = FieldSignature.of(input);
			FieldSignature outputSignature = transform(inputSignature);
			if ( outputSignature != null ) {
				output = outputSignature.toString();
			} else {
				// leave output null;
			}

		} else if ( signatureType == SignatureType.METHOD ) {
			MethodSignature inputSignature = MethodSignature.of(input);
			MethodSignature outputSignature = transform(inputSignature);
			if ( outputSignature != null ) {
				output = outputSignature.toString();
			} else {
				// leave output null
			}

		} else {
			throw new IllegalArgumentException(
				"Signature [ " + input + " ] uses unknown type [ " + signatureType + " ]");
		}

		if ( output == null ) {
			unchangedSignatures.add(input);
		} else {
			changedSignatures.put(input, output);
		}

		return output;
	}

	protected ClassSignature transform(ClassSignature classSignature) {
		TypeParameter[] inputTypes = classSignature.typeParameters;
		TypeParameter[] outputTypes = null;

		for ( int parmNo = 0; parmNo < inputTypes.length; parmNo++ ) {
			TypeParameter inputType = inputTypes[parmNo];
			TypeParameter outputType = transform(inputType);

			if ( outputType != null ) {
				if ( outputTypes == null ) {
					outputTypes = inputTypes.clone();
				}
				outputTypes[parmNo] = outputType;
			}
		}

		ClassTypeSignature inputSuperClass = classSignature.superClass;
		ClassTypeSignature outputSuperClass = transform(inputSuperClass);

		ClassTypeSignature[] inputInterfaces = classSignature.superInterfaces;
		ClassTypeSignature[] outputInterfaces = null;
		
		for ( int interfaceNo = 0; interfaceNo < inputInterfaces.length; interfaceNo++ ) {
			ClassTypeSignature inputInterface = inputInterfaces[interfaceNo];
			ClassTypeSignature outputInterface = transform(inputInterface);

			if ( outputInterface != null ) {
				if ( outputInterfaces == null ) {
					outputInterfaces = inputInterfaces.clone();
				}
				outputInterfaces[interfaceNo] = outputInterface;
			}
		}

		if ( (outputTypes == null) && (outputSuperClass == null) && (outputInterfaces == null) ) {
			return null;
		} else {
			return new ClassSignature(
				( (outputTypes == null) ? inputTypes : outputTypes ),
				( (outputSuperClass == null) ? inputSuperClass : outputSuperClass ),
				( (outputInterfaces == null) ? inputInterfaces : outputInterfaces) );
		}
	}

	protected FieldSignature transform(FieldSignature fieldSignature) {
		ReferenceTypeSignature inputType = fieldSignature.type;
		ReferenceTypeSignature outputType = transform(inputType);

		if ( outputType == null ) {
			return null;
		} else {
			return new FieldSignature(outputType);
		}
	}

	protected MethodSignature transform(MethodSignature methodSignature) {
		TypeParameter[] inputTypeParms = methodSignature.typeParameters;
		TypeParameter[] outputTypeParms = null;

		for ( int parmNo = 0; parmNo < inputTypeParms.length; parmNo++ ) {
			TypeParameter inputTypeParm = inputTypeParms[parmNo];
			TypeParameter outputTypeParm = transform(inputTypeParm);
			if ( outputTypeParm != null ) {
				if ( outputTypeParms == null ) {
					outputTypeParms = inputTypeParms.clone();
				}
				outputTypeParms[parmNo] = outputTypeParm;
			}
		}

		JavaTypeSignature[] inputParmTypes = methodSignature.parameterTypes;
		JavaTypeSignature[] outputParmTypes = null;

		for ( int parmNo = 0; parmNo < inputParmTypes.length; parmNo++ ) {
			JavaTypeSignature inputParmType = inputParmTypes[parmNo];
			JavaTypeSignature outputParmType = transform(inputParmType);
			if ( outputParmType != null ) {
				if ( outputParmTypes == null ) {
					outputParmTypes = inputParmTypes.clone();
				}
				outputParmTypes[parmNo] = outputParmType;
			}
		}

		Result inputResult = methodSignature.resultType;
		Result outputResult = transform(inputResult);

		ThrowsSignature[] inputThrows = methodSignature.throwTypes;
		ThrowsSignature[] outputThrows = null;
		
		for ( int throwNo = 0; throwNo < inputThrows.length; throwNo++ ) {
			ThrowsSignature inputThrow = inputThrows[throwNo];
			ThrowsSignature outputThrow = transform(inputThrow);
			if ( outputThrow != null ) {
				if ( outputThrows == null ) {
					outputThrows = inputThrows.clone();
				}
				outputThrows[throwNo] = outputThrow;
			}
		}

		if ( (outputTypeParms == null) &&
			 (outputParmTypes == null) &&
			 (outputResult == null) &&
			 (outputThrows == null) ) {
			return null;

		} else {
			return new MethodSignature(
				( (outputTypeParms == null) ? inputTypeParms : outputTypeParms ),
				( (outputParmTypes == null) ? inputParmTypes : outputParmTypes ),
				( (outputResult == null) ? inputResult : outputResult ),
				( (outputThrows == null) ? inputThrows : outputThrows ) );
		}
	}

	protected Result transform(Result type) {
		if ( type instanceof JavaTypeSignature ) {
			return transform((JavaTypeSignature) type);
		} else {
			return null;
		}
	}

	protected ThrowsSignature transform(ThrowsSignature type) {
		if ( type instanceof ClassTypeSignature ) {
			return transform((ClassTypeSignature) type);
		} else {
			return null;
		}
	}
	
	protected ArrayTypeSignature transform(ArrayTypeSignature inputType) {
		JavaTypeSignature inputComponent = inputType.component;
		int componentDepth = 1;
		while ( inputComponent instanceof ArrayTypeSignature ) {
			componentDepth++;
			inputComponent = ((ArrayTypeSignature) inputComponent).component;
		}
		if ( (inputComponent instanceof BaseType) || (inputComponent instanceof TypeVariableSignature) ) {
			return null;
		}

		JavaTypeSignature outputComponent = transform((ClassTypeSignature) inputComponent);
		if ( outputComponent == null ) {
			return null;
		}

		ArrayTypeSignature outputType = new ArrayTypeSignature(outputComponent);
		while ( --componentDepth > 0 ) {
			outputType = new ArrayTypeSignature(outputType);
		}
		return outputType;
	}

	protected TypeParameter transform(TypeParameter inputTypeParameter) {
		ReferenceTypeSignature inputClassBound = inputTypeParameter.classBound;
		ReferenceTypeSignature outputClassBound = transform(inputClassBound);

		ReferenceTypeSignature[] inputBounds = inputTypeParameter.interfaceBounds;
		ReferenceTypeSignature[] outputBounds = null;

		for ( int boundNo = 0; boundNo < inputBounds.length; boundNo++ ) {
			ReferenceTypeSignature inputBound = inputBounds[boundNo];
			ReferenceTypeSignature outputBound = transform(inputBound);
			if ( outputBound != null ) {
				if ( outputBounds == null ) {
					outputBounds = inputBounds.clone();
				}
				outputBounds[boundNo] = outputBound;
			}
		}

		if ( (outputClassBound == null) && (outputBounds == null) ) {
			return null;
		} else {
			return new TypeParameter(
				inputTypeParameter.identifier,
				( (outputClassBound == null) ? inputClassBound : outputClassBound ),
				( (outputBounds == null) ? inputBounds : outputBounds ) );
		}
	}

	protected ClassTypeSignature transform(ClassTypeSignature inputType) {
		String inputPackageSpecifier = inputType.packageSpecifier;
		String outputPackageSpecifier = null;

		int length = inputPackageSpecifier.length();
		if ( length > 0 ) {
			String inputBinaryPackage = inputPackageSpecifier.substring(0, length - 1);
			String outputBinaryPackage = renamePackage(inputBinaryPackage);
			if ( outputBinaryPackage != null ) {
				outputPackageSpecifier = outputBinaryPackage + '/';
			}
		}

		SimpleClassTypeSignature inputClassType = inputType.classType;		
		SimpleClassTypeSignature outputClassType = transform(inputClassType);

		SimpleClassTypeSignature[] inputInnerTypes = inputType.innerTypes;
		SimpleClassTypeSignature[] outputInnerTypes = null;

		for ( int typeNo = 0; typeNo < inputInnerTypes.length; typeNo++ ) {
			SimpleClassTypeSignature inputInnerType = inputInnerTypes[typeNo]; 
			SimpleClassTypeSignature outputInnerType = transform(inputInnerType);
			if ( outputInnerType != null ) {
				if ( outputInnerTypes == null ) {
					outputInnerTypes = inputInnerTypes.clone();
				}
				outputInnerTypes[typeNo] = outputInnerType;
			}
		}

		// Do not transform 'type.binary'.

		if ( (outputPackageSpecifier == null) && (outputClassType == null) && (outputInnerTypes == null) ) {
			return null;
		} else {
			return new ClassTypeSignature(
				inputType.binary,
				( (outputPackageSpecifier == null) ? inputPackageSpecifier : outputPackageSpecifier ),
				( (outputClassType == null) ? inputClassType : outputClassType ),
				( (outputInnerTypes == null) ? inputInnerTypes : outputInnerTypes ) );
		}
	}

	protected SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature) {
		TypeArgument[] inputArgs = inputSignature.typeArguments;
		TypeArgument[] outputArgs = null;

		for ( int argNo = 0; argNo < inputArgs.length; argNo++ ) {
			TypeArgument inputArg = inputArgs[argNo];
			TypeArgument outputArg = transform(inputArg);
			if ( outputArg != null ) {
				if ( outputArgs == null ) {
					outputArgs = inputArgs.clone();
				}
				outputArgs[argNo] = outputArg;
			}
		}

		if ( outputArgs == null ) {
			return null;
		} else {
			return new SimpleClassTypeSignature(inputSignature.identifier, outputArgs);
		}
	}

	protected TypeArgument transform(TypeArgument inputArgument) {
		ReferenceTypeSignature inputSignature = inputArgument.type;
		ReferenceTypeSignature outputSignature = transform(inputSignature);
		if ( outputSignature == null ) {
			return null;
		} else {
			return new TypeArgument(inputArgument.wildcard, outputSignature);
		}
	}

	protected JavaTypeSignature transform(JavaTypeSignature type) {
		if ( type instanceof ReferenceTypeSignature ) {
			return transform((ReferenceTypeSignature) type);
		} else {
			return null;
		}
	}

	protected ReferenceTypeSignature transform(ReferenceTypeSignature type) {
		if ( type instanceof ClassTypeSignature ) {
			return transform((ClassTypeSignature) type);

		} else if ( type instanceof ArrayTypeSignature ) {
			return transform((ArrayTypeSignature) type);

		} else {
			return null;
		}
	}

	//

	private final Set<String> unchangedDescriptors;
	private final Map<String, String> changedDescriptors;

	protected String transformDescriptor(String inputDescriptor) {
		if ( root != null ) {
			return root.transformDescriptor(inputDescriptor);
		}

		if ( unchangedDescriptors.contains(inputDescriptor) ) {
			return null;
		}

		String outputDescriptor = changedDescriptors.get(inputDescriptor);
		if ( outputDescriptor != null ) {
			return outputDescriptor;
		}

		char c = inputDescriptor.charAt(0);
		if ( c == '(' ) {
			String inputSignature = inputDescriptor.replace('$', '.');
			String outputSignature = transform(inputSignature, SignatureType.METHOD);
			if ( outputSignature != null ) {
				outputDescriptor = outputSignature.replace('.', '$');
			} else {
				// leave outputDescriptor null
			}

		} else  if ( (c == '[') || ((c == 'L') && (inputDescriptor.charAt(inputDescriptor.length() - 1) == ';')) ) {
			String inputSignature = inputDescriptor.replace('$', '.');
			String outputSignature = transform(inputSignature, SignatureType.FIELD);
			if ( outputSignature != null ) {
				outputDescriptor = outputSignature.replace('.', '$');
			} else {
				// leave outputDescriptor null
			}

		} else {
			// leave outputDescriptor null
		}

		if ( outputDescriptor == null ) {
			unchangedDescriptors.add(inputDescriptor);
		} else {
			changedDescriptors.put(inputDescriptor, outputDescriptor);
		}
		return outputDescriptor;
	}

	//

	private byte[] inputBytes;

	private byte[] getInputBytes() {
		if ( parent != null ) {
			return parent.getInputBytes();
		} else {
			return inputBytes;
		}
	}

	private void setInputBytes(byte[] inputBytes) {
		if ( parent != null ) {
			parent.setInputBytes(inputBytes);
		} else {
			this.inputBytes = inputBytes;
		}
	}

	@Override
	public ByteData read(String inputName, InputStream inputStream, int inputCount) throws IOException {
		if ( parent != null ) {
			return parent.read(inputName, inputStream, inputCount);

		} else {
			byte[] readBytes = getInputBytes();
			ByteData readData = FileUtils.read(inputName, inputStream, readBytes, inputCount); // throws IOException
			setInputBytes(readData.data);
			return readData;
		}
	}
}
