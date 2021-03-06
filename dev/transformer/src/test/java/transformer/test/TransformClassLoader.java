package transformer.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.Vector;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.ClassAction;
import com.ibm.ws.jakarta.transformer.action.JarAction;
import com.ibm.ws.jakarta.transformer.action.ServiceConfigAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;
import com.ibm.ws.jakarta.transformer.util.FileUtils;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

public class TransformClassLoader extends ClassLoader {

	public TransformClassLoader(
		ClassLoader parent,
		JarAction jarAction, ClassAction classAction, ServiceConfigAction configAction) {

		super(parent);

		this.jarAction = jarAction;
		this.classAction = classAction;
		this.serviceConfigAction = configAction;
	}

	//

	private final JarAction jarAction;

	public JarAction getJarAction() {
		return jarAction;
	}

	public boolean selectResource(String resourceName) {
		return getJarAction().select(resourceName);
	}

	public String getResourceName(String className) {
		return getJarAction().asResourceName(className);
	}

	//

	private final ClassAction classAction;

	public ClassAction getClassAction() {
		return classAction;
	}

	public boolean acceptClass(String resourceName) {
		return getClassAction().accept(resourceName);
	}

	public InputStream applyClass(String resourceName, InputStream inputStream)
		throws JakartaTransformException {

		InputStreamData outputData = getClassAction().apply(resourceName, inputStream);
		 // 'apply' throws JakartaTransformException

		return outputData.stream;
	}

	//

	private final ServiceConfigAction serviceConfigAction;

	public ServiceConfigAction getServiceConfigAction() {
		return serviceConfigAction;
	}

	public boolean acceptServiceConfig(String resourceName) {
		return getServiceConfigAction().accept(resourceName);
	}

	public InputStream applyServiceConfig(String resourceName, InputStream inputStream)
		throws JakartaTransformException {

		InputStreamData outputData = getServiceConfigAction().apply(resourceName, inputStream);
		// 'apply' throws JakartaTransformException

		return outputData.stream;
	}

	//	

	public static class TransformClassURLStreamHandler extends URLStreamHandler {
		private final URL baseURL;

		public TransformClassURLStreamHandler(URL baseURL) {
			this.baseURL = baseURL;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new TransformClassURLConnection(baseURL); // throws IOException
		}
	}

	public static class TransformClassURLConnection extends URLConnection {
		public TransformClassURLConnection(URL baseURL) throws IOException {
			super(baseURL);

			this.baseConnection = this.getURL().openConnection(); // 'openConnection' throws IOException
		}

		//

		private final URLConnection baseConnection;

		public URLConnection getBaseConnection() {
			return baseConnection;
		}

		//

		@Override
		public void connect() throws IOException {
			getBaseConnection().connect(); // 'connect' throws IOException
		}

		public InputStream getInputStream() throws IOException {
			URLConnection useBaseConnection = getBaseConnection();
			String baseName = useBaseConnection.getURL().toString();
			InputStream baseStream = useBaseConnection.getInputStream(); // throws IOException

			ByteData inputData = FileUtils.read(baseName, baseStream);

			return new ByteArrayInputStream(inputData.data, 0, inputData.length);
		}
	}

	public static class TransformConfigURLStreamHandler extends URLStreamHandler {
		private final URL baseURL;

		public TransformConfigURLStreamHandler(URL baseURL) {
			this.baseURL = baseURL;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new TransformConfigURLConnection(baseURL); // throws IOException
		}
	}

	public static class TransformConfigURLConnection extends URLConnection {
		public TransformConfigURLConnection(URL baseURL) throws IOException {
			super(baseURL);

			this.baseConnection = this.getURL().openConnection(); // 'openConnection' throws IOException
		}

		//

		private final URLConnection baseConnection;

		public URLConnection getBaseConnection() {
			return baseConnection;
		}

		//

		@Override
		public void connect() throws IOException {
			getBaseConnection().connect(); // 'connect' throws IOException
		}

		public InputStream getInputStream() throws IOException {
			URLConnection useBaseConnection = getBaseConnection();
			String baseName = useBaseConnection.getURL().toString();
			InputStream baseStream = useBaseConnection.getInputStream(); // throws IOException

			ByteData inputData = FileUtils.read(baseName, baseStream);

			return new ByteArrayInputStream(inputData.data, 0, inputData.length);
		}
	}

    //

    @Override
    public InputStream getResourceAsStream(String resourceName) {
    	InputStream baseStream = super.getResourceAsStream(resourceName);
    	if ( baseStream == null ) {
        	System.out.println("Get resource stream: " + resourceName + ": No base stream");
    		return null;

    	} else if ( !selectResource(resourceName) ) {
    		System.out.println("Get resource stream: " + resourceName + ": Not selected");
    		return baseStream;

   		} else if ( acceptClass(resourceName) ) {
   			System.out.println("Get resource stream: " + resourceName + ": Accepted as class");
    		try {
    			return applyClass(resourceName, baseStream); // throws JakartaTransformException

    		} catch ( JakartaTransformException e ) {
    			System.err.println("Class transform failure [ " + resourceName + " ]");
    			e.printStackTrace();

    			return super.getResourceAsStream(resourceName);
    		}

   		} else if ( acceptServiceConfig(resourceName) ) {
    		System.out.println("Get resource stream: " + resourceName + ": Accepted as service config");

    		try {
    			return applyServiceConfig(resourceName, baseStream); // throws JakartaTransformException

    		} catch ( JakartaTransformException e ) {
    			System.err.println("Servic configuration transform failure [ " + resourceName + " ]");
    			e.printStackTrace();

    			return super.getResourceAsStream(resourceName);
    		}

    	} else {
    		System.out.println("Get resource stream: " + resourceName + ": Not accepted");
    		return baseStream;
    	}
	}

    protected URL transformAsClass(URL baseURL) {
    	String baseText = baseURL.toString();
    	try {
    		return new URL(null, baseText, new TransformClassURLStreamHandler(baseURL) );
    	} catch ( MalformedURLException e ) {
    		System.err.println("Failed to wrap base URL [ " + baseText + " ] for class transformation: " + e);
    		return baseURL;
    	}
    }

    protected URL transformAsServiceConfig(URL baseURL) {
    	String baseText = baseURL.toString();
    	try {
    		return new URL(null, baseText, new TransformConfigURLStreamHandler(baseURL) );
    	} catch ( MalformedURLException e ) {
    		System.err.println("Failed to wrap base URL [ " + baseText + " ] for service configuration transformation: " + e);
    		return baseURL;
    	}
    }

    @Override
    protected URL findResource(String name) {
    	URL baseURL = super.findResource(name);
    	if ( baseURL == null ) {
    		return null;
    	} else {
    		return transform(name, baseURL);
    	}
    }
    
    protected URL transform(String name, URL baseURL) {
    	 if ( !selectResource(name) ) {
     		return baseURL;
     	} else if ( acceptClass(name) ) {
     		return transformAsClass(baseURL);
     	} else if ( acceptServiceConfig(name) ) {
     		return transformAsServiceConfig(baseURL);
     	} else {
     		return baseURL;
     	}
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
    	Enumeration<URL> baseURLs = super.findResources(name);
    	if ( !baseURLs.hasMoreElements() ) {
    		return baseURLs;

    	} else if ( !selectResource(name) ) {
    		return baseURLs;

    	} else {
    		Vector<URL> transformedURLs = new Vector<URL>();
    		while ( baseURLs.hasMoreElements() ) {
    			transformedURLs.add( transform( name, baseURLs.nextElement() ) );
    		}
    		return transformedURLs.elements();
    	}
    }

    protected Class<?> loadClass(String className, boolean resolveClass) throws ClassNotFoundException {
    	String resourceName = getResourceName(className);

    	System.out.println("Load [ " + className + " ] as [ " + resourceName + " ]");

    	if ( !selectResource(resourceName) ) {
    		System.out.println("Load [ " + className + " ]: Not selected");
    		return super.loadClass(className, resolveClass); // throws ClassNotFoundException
    	}

    	Class<?> loadedClass = findLoadedClass(className);
    	if ( loadedClass == null ) {
    		System.out.println("Load [ " + className + " ]: Not previously loaded: Find");
    		loadedClass = findClass(className); // throws ClassNotFoundException
    	} else {
    		System.out.println("Load [ " + className + " ]: Previously loaded");
    	}

    	if ( resolveClass ) {
    		System.out.println("Load [ " + className + " ]: Resolve");
    		resolveClass(loadedClass);
    	}

    	return loadedClass;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
    	String resourceName = getResourceName(className);

    	System.out.println("Find [ " + className + " ] as [ " + resourceName + " ]");

    	if ( !selectResource(resourceName) ) {
    		System.out.println("Find [ " + className + " ]: Not selected");
    		return super.findClass(className); // throws ClassNotFoundException
    	}

    	Class<?> loadedClass = findLoadedClass(className);
    	if ( loadedClass != null ) {
    		System.out.println("Find [ " + className + " ]: Previously loaded");
    		return loadedClass;
    	}

    	InputStream classStream = getResourceAsStream(resourceName);
    	if ( classStream == null ) {
    		System.out.println("Find [ " + className + " ]: Not found");
    		throw new ClassNotFoundException(className);
    	}

    	System.out.println("Find [ " + className + " ]: Reading class stream.");
    	ByteData classData;
    	try {
    		classData = FileUtils.read(className, classStream);
    	} catch ( IOException e ) {
    		throw new ClassNotFoundException(className, e);
    	}

    	System.out.println("Find [ " + className + " ]: Define class.");
    	return super.defineClass(classData.name, classData.data, 0, classData.length);
    }
}