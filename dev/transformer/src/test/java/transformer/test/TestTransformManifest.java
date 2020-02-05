package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.JakartaTransformProperties;
import com.ibm.ws.jakarta.transformer.action.impl.ActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.ManifestActionImpl;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

public class TestTransformManifest {
	public static final String JAVAX_SERVLET = "javax.servlet";
	public static final String JAVAX_SERVLET_ANNOTATION = "javax.servlet.annotation";
	public static final String JAVAX_SERVLET_DESCRIPTOR = "javax.servlet.descriptor";
	public static final String JAVAX_SERVLET_HTTP = "javax.servlet.http";
	public static final String JAVAX_SERVLET_RESOURCES = "javax.servlet.resources";

	public static final String JAKARTA_SERVLET = "jakarta.servlet";
	public static final String JAKARTA_SERVLET_ANNOTATION = "jakarta.servlet.annotation";
	public static final String JAKARTA_SERVLET_DESCRIPTOR = "jakarta.servlet.descriptor";
	public static final String JAKARTA_SERVLET_HTTP = "jakarta.servlet.http";
	public static final String JAKARTA_SERVLET_RESOURCES = "jakarta.servlet.resources";	

	public static final int SERVLET_COUNT = 67;
	public static final int SERVLET_ANNOTATION_COUNT = 1;	
	public static final int SERVLET_DESCRIPTOR_COUNT = 3;	
	public static final int SERVLET_RESOURCES_COUNT = 1;	
	public static final int SERVLET_HTTP_COUNT = 23;

	protected Set<String> includes;

	public Set<String> getIncludes() {
		if ( includes == null ) {
			includes = new HashSet<String>();
			includes.add(TEST_MANIFEST_PATH);
		}

		return includes;
	}

	public Set<String> getExcludes() {
		return Collections.emptySet();
	}

	protected Map<String, String> packageRenames;

	public Map<String, String> getPackageRenames() {
		if ( packageRenames == null ) {
			packageRenames = new HashMap<String, String>();
			packageRenames.put(JAVAX_SERVLET, JAKARTA_SERVLET);
			packageRenames.put(JAVAX_SERVLET_ANNOTATION, JAKARTA_SERVLET_ANNOTATION);
			packageRenames.put(JAVAX_SERVLET, JAKARTA_SERVLET);
			packageRenames.put(JAVAX_SERVLET, JAKARTA_SERVLET);
			packageRenames.put(JAVAX_SERVLET_HTTP, JAKARTA_SERVLET_HTTP);
		}
		return packageRenames;
	}

	public ManifestActionImpl jakartaManifestAction;
	public ManifestActionImpl javaxManifestAction;

	public ManifestActionImpl getJakartaManifestAction() {
		if ( jakartaManifestAction == null ) {
			jakartaManifestAction =
				new ManifestActionImpl(
					System.out, !ActionImpl.IS_TERSE, ActionImpl.IS_VERBOSE,
					getIncludes(), getExcludes(), getPackageRenames() );
		}
		return jakartaManifestAction;
	}

	public ManifestActionImpl getJavaxManifestAction() {
		if ( javaxManifestAction == null ) {
			Map<String, String> invertedRenames = JakartaTransformProperties.invert( getPackageRenames() );
			javaxManifestAction = new ManifestActionImpl( getIncludes(), getExcludes(), invertedRenames );
		}
		return javaxManifestAction;
	}

	public ManifestActionImpl jakartaFeatureAction;
	public ManifestActionImpl javaxFeatureAction;

	public ManifestActionImpl getJakartaFeatureAction() {
		if ( jakartaFeatureAction == null ) {
			jakartaFeatureAction =
				new ManifestActionImpl(
					System.out, !ActionImpl.IS_TERSE, ActionImpl.IS_VERBOSE,
					getIncludes(), getExcludes(), getPackageRenames(), ManifestActionImpl.IS_FEATURE );
		}
		return jakartaFeatureAction;
	}

	public ManifestActionImpl getJavaxFeatureAction() {
		if ( javaxFeatureAction == null ) {
			Map<String, String> invertedRenames = JakartaTransformProperties.invert( getPackageRenames() );
			javaxFeatureAction = new ManifestActionImpl(
				getIncludes(), getExcludes(), invertedRenames, ManifestActionImpl.IS_FEATURE );
		}
		return javaxFeatureAction;
	}

	//

	protected static final class Occurrences {
		public final String tag;
		public final int count;
		
		public Occurrences(String tag, int count) {
			this.tag = tag;
			this.count = count;
		}
	}

	//

	public List<String> displayManifest(String manifestPath, InputStream manifestStream) throws IOException {
		System.out.println("Manifest [ " + manifestPath + " ]");
		List<String> manifestLines = TestUtils.loadLines(manifestStream); // throws IOException

		List<String> collapsedLines = TestUtils.manifestCollapse(manifestLines);

		int numLines = collapsedLines.size();
		for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
			System.out.printf( "[ %3d ] [ %s ]\n", lineNo, collapsedLines.get(lineNo) );
		}

		return collapsedLines;
	}

	public void testTransform(String inputPath, Occurrences[] outputOccurrences, boolean isManifest)
		throws JakartaTransformException, IOException {

		System.out.println("Read [ " + inputPath + " ]");
		InputStream manifestInput = TestUtils.getResourceStream(inputPath); // throws IOException

		@SuppressWarnings("unused")
		List<String> inputLines = displayManifest(inputPath, manifestInput);

		manifestInput = TestUtils.getResourceStream(inputPath); // throws IOException

		ManifestActionImpl manifestAction = ( isManifest ? getJakartaManifestAction() : getJakartaFeatureAction() );

		System.out.println("Transform [ " + inputPath + " ] using [ " + manifestAction.getName() + " ]");

		InputStreamData manifestOutput = manifestAction.apply(inputPath, manifestInput);
		 // 'apply' throws JakartaTransformException

		List<String> manifestLines = displayManifest(inputPath, manifestOutput.stream);

		System.out.println("Verify [ " + inputPath + " ]");

		for ( Occurrences occurrence : outputOccurrences ) {
			String tag = occurrence.tag;
			int expected = occurrence.count;
			int actual = TestUtils.occurrences(manifestLines, tag);
			System.out.println("Tag [ " + tag + " ] Expected [ " + expected + " ] Actual [ " + actual + " ]");
			Assertions.assertEquals(expected, actual, tag);
		}

		System.out.println("Passed [ " + inputPath + " ]");
	}

	//

	public static final String TEST_FEATURE_PATH = "transformer/test/data/META-INF/servlet-4.0.mf";

	public static final Occurrences[] MANIFEST_TO_JAKARTA_DATA = {
		new Occurrences(JAVAX_SERVLET, 0),
		new Occurrences(JAKARTA_SERVLET, SERVLET_COUNT),
		new Occurrences(JAKARTA_SERVLET_ANNOTATION, SERVLET_ANNOTATION_COUNT),
		new Occurrences(JAKARTA_SERVLET_DESCRIPTOR, SERVLET_DESCRIPTOR_COUNT),
		new Occurrences(JAKARTA_SERVLET_HTTP, SERVLET_HTTP_COUNT),
		new Occurrences(JAKARTA_SERVLET_RESOURCES, SERVLET_RESOURCES_COUNT)
	};

	public static final Occurrences[] MANIFEST_TO_JAVAX_DATA = {
		new Occurrences(JAKARTA_SERVLET, 0),
		new Occurrences(JAVAX_SERVLET, SERVLET_COUNT),
		new Occurrences(JAVAX_SERVLET_ANNOTATION, SERVLET_ANNOTATION_COUNT),
		new Occurrences(JAVAX_SERVLET_DESCRIPTOR, SERVLET_DESCRIPTOR_COUNT),
		new Occurrences(JAVAX_SERVLET_HTTP, SERVLET_HTTP_COUNT),
		new Occurrences(JAVAX_SERVLET_RESOURCES, SERVLET_RESOURCES_COUNT)
	};

	//

	public static final String TEST_MANIFEST_PATH = "transformer/test/data/META-INF/MANIFEST.MF";

	public static final Occurrences[] FEATURE_TO_JAKARTA_DATA = {
		// EMPTY
	};

	@Test
	public void testTransformManifest() throws JakartaTransformException, IOException {
		testTransform(TEST_MANIFEST_PATH, MANIFEST_TO_JAKARTA_DATA, ManifestActionImpl.IS_MANIFEST);
		// throws JakartaTransformException, IOException
	}

	@Test
	public void testTransformFeature() throws JakartaTransformException, IOException {
		testTransform(TEST_FEATURE_PATH, FEATURE_TO_JAKARTA_DATA, ManifestActionImpl.IS_FEATURE);
		// throws JakartaTransformException, IOException
	}
}
