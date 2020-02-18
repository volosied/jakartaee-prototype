package transformer.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.JakartaTransformProperties;
import com.ibm.ws.jakarta.transformer.JakartaTransformer;
import com.ibm.ws.jakarta.transformer.action.Action;
import com.ibm.ws.jakarta.transformer.action.ClassAction;
import com.ibm.ws.jakarta.transformer.action.ClassChanges;
import com.ibm.ws.jakarta.transformer.action.impl.ActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.ClassActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.JarActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.ServiceConfigActionImpl;
import com.ibm.ws.jakarta.transformer.util.FileUtils;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

import transformer.test.data.Sample_InjectAPI_Jakarta;
import transformer.test.data.Sample_InjectAPI_Javax;
import transformer.test.util.ClassData;

public class TestTransformClass {

	@Test
	public void testJavaxAsJavax_inject() {
		System.out.println("test null conversion javax class load");
		testLoad( JAVAX_CLASS_NAME, getClassLoader_null() );
	}

	@Test
	public void testJakartaAsJakarta_inject() {
		System.out.println("test null conversion jakarta class load");
		testLoad( JAKARTA_CLASS_NAME, getClassLoader_null() );
	}

	@Test
	public void testJavaxAsJakarta_inject() {
		System.out.println("test javax to jakarta class load");
		Class<?> testClass = testLoad( JAVAX_CLASS_NAME, getClassLoader_toJakarta() );
		ClassData testData = new ClassData(testClass);
		testData.log( new PrintWriter(System.out, true) ); // autoflush
	}

	@Test
	public void testJakartaAsJavax_inject() {
		System.out.println("test jakarta to javax class load");
		Class<?> testClass = testLoad( JAKARTA_CLASS_NAME, getClassLoader_toJavax() );
		ClassData testData = new ClassData(testClass);
		testData.log( new PrintWriter(System.out, true) ); // autoflush
	}

	//

	public static final String JAVAX_CLASS_NAME = Sample_InjectAPI_Javax.class.getName();
	public static final String JAKARTA_CLASS_NAME = Sample_InjectAPI_Jakarta.class.getName();

	protected Set<String> includes;
	
	public Set<String> getIncludes() {
		if ( includes == null ) {
			includes = new HashSet<String>();
			includes.add( Action.classNameToBinaryTypeName(JAVAX_CLASS_NAME) );
			includes.add( Action.classNameToBinaryTypeName(JAKARTA_CLASS_NAME) );
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> packageRenames;

	public static final String JAVAX_INJECT_PACKAGE_NAME = "javax.inject";
	public static final String JAKARTA_INJECT_PACKAGE_NAME = "jakarta.inject";
	
	public Map<String, String> getPackageRenames() {
		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			packageRenames.put(
				Action.classNameToBinaryTypeName(JAVAX_INJECT_PACKAGE_NAME),
				Action.classNameToBinaryTypeName(JAKARTA_INJECT_PACKAGE_NAME) );
		}
		return packageRenames;
	}

	public ClassLoader getClassLoader_null() {
		return getClass().getClassLoader();
	}

	public JarActionImpl jakartaJarAction;
	public JarActionImpl javaxJarAction;

	public JarActionImpl getJakartaJarAction() {
		if ( jakartaJarAction == null ) {
			jakartaJarAction = new JarActionImpl(
				getIncludes(), getExcludes(),
				getPackageRenames(), null, null );
		}
		return jakartaJarAction;
	}

	public JarActionImpl getJavaxJarAction() {
		if ( javaxJarAction == null ) {
			Map<String, String> invertedRenames = JakartaTransformProperties.invert( getPackageRenames() );
			javaxJarAction = new JarActionImpl(
				getIncludes(), getExcludes(),
				invertedRenames, null, null );
		}
		return javaxJarAction;
	}

	public ClassLoader getClassLoader_toJakarta() {
		JarActionImpl jarAction = getJakartaJarAction();
		ClassActionImpl classAction = new ClassActionImpl(jarAction);
		ServiceConfigActionImpl configAction = new ServiceConfigActionImpl(jarAction);

		return new TransformClassLoader(
			getClass().getClassLoader(),
			jarAction, classAction, configAction );
	}

	public ClassLoader getClassLoader_toJavax() {
		JarActionImpl jarAction = getJavaxJarAction();
		ClassActionImpl classAction = new ClassActionImpl(jarAction);
		ServiceConfigActionImpl configAction = new ServiceConfigActionImpl(jarAction);

		return new TransformClassLoader(
			getClass().getClassLoader(),
			jarAction, classAction, configAction );
	}

	public Class<?> testLoad(String className, ClassLoader classLoader) {
		System.out.println("Loading [ " + className + " ] using [ " + classLoader + " ]");

		@SuppressWarnings("unused")
		Class<?> objectClass;
		try {
			objectClass = classLoader.loadClass( java.lang.Object.class.getName() );
		} catch ( Throwable th ) {
			th.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + java.lang.Object.class.getName() + " ]: " + th);
			return null;
		}

		Class<?> testClass;
		try {
			testClass = classLoader.loadClass(className);
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + className + " ]: " + e);
			return null;
		} catch ( Throwable th ) {
			th.printStackTrace(System.out);
			Assertions.fail("Failed to load class [ " + className + " ]: " + th);
			return null;
		}

		System.out.println("Loaded [ " + className + " ]: " + testClass);
		return testClass;
	}

	public static InputStream getResourceStream(String resourceName) throws IOException {
		InputStream inputStream = TestUtils.getResourceStream(resourceName);
		if ( inputStream == null ) {
			throw new IOException("Resource not found [ " + resourceName + " ]");
		}
		return inputStream;
	}

	public static final String TEST_DATA_RESOURCE_NAME = "transformer/test/data";
	public static final String ANNOTATED_SERVLET_RESOURCE_NAME = "AnnotatedServlet.class";
	
	public static final String TRANSFORMER_RESOURCE_NAME = "com/ibm/ws/jakarta/transformer";

	public static Map<String, String> getStandardRenames() throws IOException {
		String transformerResourceName = JakartaTransformer.class.getPackage().getName().replace('.', '/');
		String renamesResourceName = transformerResourceName + '/' + JakartaTransformer.DEFAULT_RENAMES_REFERENCE;

		InputStream renamesInputStream = getResourceStream(renamesResourceName); // throws IOException
		Reader renamesReader = new InputStreamReader(renamesInputStream);

		Properties renameProperties = new Properties();
		renameProperties.load(renamesReader); // throws IOException

		Map<String, String> renames = new HashMap<String, String>( renameProperties.size() );
		for ( Map.Entry<Object, Object> renameEntry : renameProperties.entrySet() ) {
			String initialPackageName = (String) renameEntry.getKey(); 
			String finalPackageName = (String) renameEntry.getValue();
			renames.put(initialPackageName, finalPackageName);
		}

		return renames;
	}

	public ClassAction createStandardClassAction() throws IOException {
		return new ClassActionImpl(
			System.out, !ActionImpl.IS_TERSE, ActionImpl.IS_VERBOSE,
			Collections.emptySet(), Collections.emptySet(),
			getStandardRenames(), null, null );
		// 'getStandardRenames' throws IOException
	}

	@Test
	public void testAnnotatedServlet() throws JakartaTransformException, IOException {
		ClassAction classAction = createStandardClassAction(); // throws IOException

		String resourceName = TEST_DATA_RESOURCE_NAME + '/' + ANNOTATED_SERVLET_RESOURCE_NAME;
		InputStream inputStream = getResourceStream(resourceName); // throws IOException

		InputStreamData outputStreamData = classAction.apply(resourceName, inputStream); // throws JakartaTransformException
		display( classAction.getChanges() );

		OutputStream outputStream = new FileOutputStream("build" + '/' + ANNOTATED_SERVLET_RESOURCE_NAME); // throws FileNotFoundException
		try {
			FileUtils.transfer(outputStreamData.stream, outputStream); // throws IOException
		} finally {
			outputStream.close(); // throws IOException
		}
	}

	public void display(ClassChanges classChanges) {
		System.out.println("Input class [ " + classChanges.getInputClassName() + " ]");
		System.out.println("Output class [ " + classChanges.getOutputClassName() + " ]");

		System.out.println("Input super class [ " + classChanges.getInputSuperName() + " ]");
		System.out.println("Output super class [ " + classChanges.getOutputSuperName() + " ]");
		
		System.out.println("Modified interfaces [ " + classChanges.getModifiedInterfaces() + " ]");
		System.out.println("Modified fields [ " + classChanges.getModifiedFields() + " ]");
		System.out.println("Modified methods [ " + classChanges.getModifiedMethods() + " ]");
		System.out.println("Modified constants [ " + classChanges.getModifiedConstants() + " ]");
		System.out.println("Modified attributes [ " + classChanges.getModifiedAttributes() + " ]");
	}
}
