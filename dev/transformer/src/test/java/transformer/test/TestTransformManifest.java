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
import com.ibm.ws.jakarta.transformer.action.impl.ManifestActionImpl;
import com.ibm.ws.jakarta.transformer.util.InputStreamData;

public class TestTransformManifest {
	public static final String TEST_MANIFEST_PATH = "transform/test/data/META-INF/MANIFEST.MF";

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
			jakartaManifestAction = new ManifestActionImpl( getIncludes(), getExcludes(), getPackageRenames() );
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

	protected static final class OccurrenceData {
		public final String tag;
		public final int occurrences;
		
		public OccurrenceData(String tag, int occurrences) {
			this.tag = tag;
			this.occurrences = occurrences;
		}
	}
	
	public static final OccurrenceData[] TO_JAKARTA_DATA = {
		new OccurrenceData(JAVAX_SERVLET, 0),
		new OccurrenceData(JAKARTA_SERVLET, SERVLET_COUNT),
		new OccurrenceData(JAKARTA_SERVLET_ANNOTATION, SERVLET_ANNOTATION_COUNT),
		new OccurrenceData(JAKARTA_SERVLET_DESCRIPTOR, SERVLET_DESCRIPTOR_COUNT),
		new OccurrenceData(JAKARTA_SERVLET_HTTP, SERVLET_HTTP_COUNT),
		new OccurrenceData(JAKARTA_SERVLET_RESOURCES, SERVLET_RESOURCES_COUNT)
	};

	public static final OccurrenceData[] TO_JAVAX_DATA = {
		new OccurrenceData(JAKARTA_SERVLET, 0),
		new OccurrenceData(JAVAX_SERVLET, SERVLET_COUNT),
		new OccurrenceData(JAVAX_SERVLET_ANNOTATION, SERVLET_ANNOTATION_COUNT),
		new OccurrenceData(JAVAX_SERVLET_DESCRIPTOR, SERVLET_DESCRIPTOR_COUNT),
		new OccurrenceData(JAVAX_SERVLET_HTTP, SERVLET_HTTP_COUNT),
		new OccurrenceData(JAVAX_SERVLET_RESOURCES, SERVLET_RESOURCES_COUNT)
	};

	@Test
	public void testTransform() throws JakartaTransformException, IOException {
		String inputName = TEST_MANIFEST_PATH;
		System.out.println("Read [ " + inputName + " ]");
		InputStream manifestInput = TestUtils.getResourceStream(inputName);

		System.out.println("Transform [ " + inputName + " ]");
		InputStreamData manifestOutput = getJakartaManifestAction().apply(inputName, manifestInput);

		System.out.println("Verify [ " + inputName + " ]");
		List<String> manifestOutputLines = TestUtils.loadLines(manifestOutput.stream);

		List<String> collapsedLines = TestUtils.manifestCollapse(manifestOutputLines);

		int numLines = collapsedLines.size();
		for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
			System.out.println("[ " + lineNo + " ] [ " + collapsedLines.get(lineNo) + " ]");
		}
		
		for ( OccurrenceData data : TO_JAKARTA_DATA ) {
			String tag = data.tag;
			int expected = data.occurrences;
			int actual = TestUtils.occurrences(collapsedLines, tag);
			System.out.println("Tag [ " + tag + " ] Expected [ " + expected + " ] Actual [ " + actual + " ]");
			Assertions.assertEquals(expected, actual, tag);
		}
	}
}
