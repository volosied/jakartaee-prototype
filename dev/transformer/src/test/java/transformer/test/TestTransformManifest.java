package transformer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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

	public static final String JAKARTA_SERVLET_VERSION = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_ANNOTATION_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_DESCRIPTOR_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_HTTP_VERSION  = "[2.6, 6.0)";
	public static final String JAKARTA_SERVLET_RESOURCES_VERSION  = "[2.6, 6.0)";	
	
	public static final int SERVLET_COUNT = 66;
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
			packageRenames.put(JAVAX_SERVLET_DESCRIPTOR, JAKARTA_SERVLET_DESCRIPTOR);
			packageRenames.put(JAVAX_SERVLET_HTTP, JAKARTA_SERVLET_HTTP);
			packageRenames.put(JAVAX_SERVLET_RESOURCES,JAKARTA_SERVLET_RESOURCES);		
		}
		return packageRenames;
	}
	
	protected Map<String, String> packageVersions;
	
	public Map<String, String> getPackageVersions() {
		if ( packageVersions == null ) {
			packageVersions = new HashMap<String, String>();
			packageVersions.put(JAVAX_SERVLET,            JAKARTA_SERVLET_VERSION );
			packageVersions.put(JAVAX_SERVLET_ANNOTATION, JAKARTA_SERVLET_ANNOTATION_VERSION );
			packageVersions.put(JAVAX_SERVLET_DESCRIPTOR, JAKARTA_SERVLET_DESCRIPTOR_VERSION );
			packageVersions.put(JAVAX_SERVLET_HTTP,       JAKARTA_SERVLET_HTTP_VERSION );
			packageVersions.put(JAVAX_SERVLET_RESOURCES,  JAKARTA_SERVLET_RESOURCES_VERSION );		
		}
		return packageVersions;
	}

	public ManifestActionImpl jakartaManifestAction;
	public ManifestActionImpl javaxManifestAction;

	public ManifestActionImpl getJakartaManifestAction() {
		if ( jakartaManifestAction == null ) {
			jakartaManifestAction =
				new ManifestActionImpl(
					System.out, !ActionImpl.IS_TERSE, ActionImpl.IS_VERBOSE,
					getIncludes(), getExcludes(), getPackageRenames(), getPackageVersions() );
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
					getIncludes(), getExcludes(), getPackageRenames(), ManifestActionImpl.IS_FEATURE,
					getPackageVersions());
		}
		return jakartaFeatureAction;
	}

	public ManifestActionImpl getJavaxFeatureAction() {
		if ( javaxFeatureAction == null ) {
			Map<String, String> invertedRenames = JakartaTransformProperties.invert( getPackageRenames() );
			javaxFeatureAction = new ManifestActionImpl(
				getIncludes(), getExcludes(), invertedRenames, ManifestActionImpl.IS_FEATURE,
				getPackageVersions());   // versions not inverted );
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
		new Occurrences(JAVAX_SERVLET, 1),
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
	
    String newVersion = "[4.0,5)";
	
	// Embedding text is the input for each test
	String embeddingText1 = ";version=\"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText2 = ";version= \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText3 = ";version =\"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText4 = ";version = \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";	
	String embeddingText5 = ";version = \"[2.6,3)\";resolution:=\"optional\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String embeddingText6 = ";resolution:=\"optional\";version = \"[2.6,3)\",javax.servlet.annotation;version=\"[2.6,3)\"";	
	String embeddingText7 = ";version=\"[2.6,3)\"";
	String embeddingText8 = "";
	String embeddingText9 = ",";
	String embeddingText10 = ";resolution:=\"optional\"";   // no version
	String embeddingText11 = ",javax.servlet.annotation;version=\"[2.6,3)\""; // leading comma
	String embeddingText12 = ";version=\"[2.6,3),javax.servlet.annotation;version=\"[2.6,3)\"";  // missing quote after version
	String embeddingText13 = "\",com.ibm.ws.webcontainer.core;version=\"1.1.0\""; // first char is a quote (no package attributes)
	
	// Expected results: When replacing the version, the expected result is the entire 
	// embedding text with the version of the first package replaced with the new version.
	String expectedResultText1_ReplaceVersion = ";version=\""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText2_ReplaceVersion = ";version= \""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText3_ReplaceVersion = ";version =\""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText4_ReplaceVersion = ";version = \""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";	
	String expectedResultText5_ReplaceVersion = ";version = \""+ newVersion + "\";resolution:=\"optional\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText6_ReplaceVersion = ";resolution:=\"optional\";version = \""+ newVersion + "\",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText7_ReplaceVersion = ";version=\""+ newVersion + "\"";
	String expectedResultText8_ReplaceVersion = "";
	String expectedResultText9_ReplaceVersion = ",";
	String expectedResultText10_ReplaceVersion = ";resolution:=\"optional\"";
	String expectedResultText11_ReplaceVersion = ",javax.servlet.annotation;version=\"[2.6,3)\"";
	String expectedResultText12_ReplaceVersion = ";version=\"[2.6,3),javax.servlet.annotation;version=\"[2.6,3)\""; // missing quote (no version replacement)
	String expectedResultText13_ReplaceVersion = "\",com.ibm.ws.webcontainer.core;version=\"1.1.0\""; 
	
	// Expected results: When getting package attributes, expected result is 
	// just the package attribute text which is at the beginning of the embedding text
	String expectedResultText1_GetPackageText = ";version=\"[2.6,3)\",";
	String expectedResultText2_GetPackageText = ";version= \"[2.6,3)\",";
	String expectedResultText3_GetPackageText = ";version =\"[2.6,3)\",";
	String expectedResultText4_GetPackageText = ";version = \"[2.6,3)\",";	
	String expectedResultText5_GetPackageText = ";version = \"[2.6,3)\";resolution:=\"optional\",";	
	String expectedResultText6_GetPackageText = ";resolution:=\"optional\";version = \"[2.6,3)\",";	
	String expectedResultText7_GetPackageText = ";version=\"[2.6,3)\"";
	String expectedResultText8_GetPackageText = "";  //empty string produces empty string
	String expectedResultText9_GetPackageText = "";  // comma produces empty string
	String expectedResultText10_GetPackageText = ";resolution:=\"optional\"";
	String expectedResultText11_GetPackageText = ""; // leading comma followed by package is empty string
	String expectedResultText12_GetPackageText = ";version=\"[2.6,3),"; // missing quote (no version replacement)
	String expectedResultText13_GetPackageText = "";  //Not starting with ';' produces empty string
	
	
	/**
	 * Subclass which allows us to call protected methods of ManifestActionImpl
	 */
	class ManifestActionImplSubClass extends ManifestActionImpl {
		public ManifestActionImplSubClass(
				PrintStream logStream, boolean isTerse, boolean isVerbose,
				Set<String> includes, Set<String> excludes, Map<String, String> renames,
				Map<String, String> versions) {

				super(logStream, isTerse, isVerbose,
				      includes, excludes, renames, versions);
			}
		
		public boolean callIsTrueMatch(String text, int textLimit, int matchStart, int keyLen ) {
			return isTrueMatch(text, textLimit, matchStart, keyLen );
		}
		
		public String callReplacePackageVersion(String embeddingText, String newVersion) {
			return replacePackageVersion(embeddingText, newVersion);
		}
		
		public String callGetPackageAttributeText(String embeddingText) {
			return getPackageAttributeText(embeddingText);
		}
		
	}
	
	private ManifestActionImplSubClass getManifestActionImplSubClass() {
		return new ManifestActionImplSubClass(System.out, 
                                              !ActionImpl.IS_TERSE, 
                                              ActionImpl.IS_VERBOSE,
                                              getIncludes(), 
                                              getExcludes(), 
                                              getPackageRenames(),
                                              getPackageVersions());
	}
	
	/**
	 * Test the ManifestActionImpl.isTrueMatch(...) method, which verifies that
	 * the package found is not part of a larger package name.  
	 * For example when searching for javax.servelet, the following are NOT true
	 * matches:
	 *      my.javax.servlet
	 *      javax.servlet.http
	 */
	@Test
	void testIsTrueMatch() {
		
		ManifestActionImplSubClass manifestAction = getManifestActionImplSubClass();
		
		boolean result;
		
		//  *** TEST CASES ****
        // 1.  myPackage            YES  -- package is exact length of search text
        //
        // 2.  myPackage.           NO   -- trailing '.' indicates part of a larger package
        // 3.  myPackage,           YES  -- trailing ',' not part of a package name
        // 4.  myPackage;           YES  -- trailing ';' not part of a package name
        // 5.  myPackage$           NO   -- trailing '$' indicates part of a larger package
        // 6.  myPackage=           YES  -- trailing '=' not part of a package name, but likely busted syntax
        // 7.  "myPackage" + " "    YES  -- trailing ' ' not part of a package name
        //
        // 8.  =myPackage           YES  -- leading '=' not part of a package name, but likely busted syntax
        // 9.  .myPackage           NO   -- leading '.' indicates part of a larger package
        //10.  ,myPackage           YES  -- leading ',' not part of a package name
        //11.  ;myPackage           YES  -- leading ';' not part of a package name
        //12.  $myPackage           NO   -- leading '$' indicates part of a larger package
        //13.  " " + myPackage      YES  -- leading ' ' not part of a package name
        //
        //14.  myPachagePlus        NO   -- trailing valid java identifier indicates part of a larger package
        //15.  plusmyPackage        NO   -- leading valid java identifier indicates part of a larger package

		
		final String TEXT = "=abc.defgh,ijklm;nopqrs$tuv wxyz= ";
		int textLen = TEXT.length();
		int matchStart;
		int keyLen;
		
	    matchStart = 0;  // Test 1
	    keyLen = 3;
		result = manifestAction.callIsTrueMatch("abc", 3, matchStart, keyLen);
		assertTrue(result, "(Package name == text) is MATCH");    
		
	    matchStart = 1;  // Test 2 "abc"   Trailing period   
	    keyLen = 3;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertFalse(result, "('.' after key) is NO MATCH");
	
	    matchStart = 1; //  Test 3 "abc.defgh"   Trailing comma 
	    keyLen = 9;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "(',' after key) is MATCH");	
		
	    matchStart = 11; //  Test 4 "ijklm"   Trailing semicolon 
	    keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "(';' after key) is MATCH");
		
	    matchStart = 17; //  Test 5 "nopqrs"   Trailing $ 
	    keyLen = 6;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertFalse(result, "('$' after key) is MATCH");
		
	    matchStart = 28; //  Test 6 "wxyz"   Trailing = 
	    keyLen = 4;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "('=' after key) is MATCH");
		
	    matchStart = 17; //  Test 7 "nopqrs$tuv"   Trailing ' ' 
	    keyLen = 10;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "(' ' after key) is MATCH");
		
	    matchStart = 1; //  Test 8 "abc.defgh"   Prior char "="
	    keyLen = 9;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "('=' before key) is MATCH");
				
	    matchStart = 5; //  Test 9 "defgh"   Prior char "."
	    keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertFalse(result, "('.' before key) is NO MATCH");
		
	    matchStart = 11; //  Test 10 "ijklm"   Prior char ","
	    keyLen = 5;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "(',' before key) is MATCH");
		
	    matchStart = 17; //  Test 11 "nopqrs$tuv"   Prior char ";"
	    keyLen = 10;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "(';' before key) is MATCH");
		
	    matchStart = 24; //  Test 12 "tuv"   Prior char "$"
	    keyLen = 3;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertFalse(result, "('$' before key) is NO MATCH");
				
	    matchStart = 28; //  Test 13 "wxyz"   Prior char " "
	    keyLen = 4;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertTrue(result, "(' ' before key) is MATCH");
			
	    matchStart = 17; //  Test 14 "no"   char after is a valid package char
	    keyLen = 2;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertFalse(result, "(valid package character after key) is NO MATCH");
		
	    matchStart = 8; //  Test 15 "gh"   char before is a valid package char
	    keyLen = 2;
		result = manifestAction.callIsTrueMatch(TEXT, textLen, matchStart, keyLen);
		assertFalse(result, "(valid package character before key) is NO MATCH");
	}
	
	@Test
	void testReplacePackageVersionInEmbeddingText() {
		
		ManifestActionImplSubClass manifestAction = getManifestActionImplSubClass();
		String result;
		
		result = manifestAction.callReplacePackageVersion(embeddingText1, newVersion);
		assertEquals(expectedResultText1_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText1_ReplaceVersion + "\nactual:" + result + "\n");	
		
		result = manifestAction.callReplacePackageVersion(embeddingText2, newVersion);
		assertEquals(expectedResultText2_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText2_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText3, newVersion);
		assertEquals(expectedResultText3_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText3_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText4, newVersion);
		assertEquals(expectedResultText4_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText4_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText5, newVersion);
		assertEquals(expectedResultText5_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText5_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText6, newVersion);
		assertEquals(expectedResultText6_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText6_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText7, newVersion);
		assertEquals(expectedResultText7_ReplaceVersion,
                   result,
				     "Result not expected:\nexpected: " + expectedResultText7_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText9, newVersion);
		assertEquals(expectedResultText9_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText9_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText10, newVersion);
		assertEquals(expectedResultText10_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText10_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText11, newVersion);
		assertEquals(expectedResultText11_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText11_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText12, newVersion);
		assertEquals(expectedResultText12_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText12_ReplaceVersion + "\nactual:" + result + "\n");
		
		result = manifestAction.callReplacePackageVersion(embeddingText13, newVersion);
		assertEquals(expectedResultText13_ReplaceVersion,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText13_ReplaceVersion + "\nactual:" + result + "\n");
	}
	
	@Test
	void testGetPackageAttributeText() {
		
		ManifestActionImplSubClass manifestAction = getManifestActionImplSubClass();
		
		String result;
		
		result = manifestAction.callGetPackageAttributeText(embeddingText1);		
		assertEquals(expectedResultText1_GetPackageText,
				     result, 
				     "Result not expected:\nexpected: " + expectedResultText1_GetPackageText + "\nactual:" + result + "\n");	
		
		result = manifestAction.callGetPackageAttributeText(embeddingText2);
		assertEquals(expectedResultText2_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText2_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText3);
		assertEquals(expectedResultText3_GetPackageText, 
				     result,
				     "Result not expected:\nexpected: " + expectedResultText3_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText4);
		assertEquals(expectedResultText4_GetPackageText, 
				     result,
				     "Result not expected:\nexpected: " + expectedResultText4_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText5);
		assertEquals(expectedResultText5_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText5_GetPackageText + "\nactual:" + result + "\n");
	
		result = manifestAction.callGetPackageAttributeText(embeddingText7);
		assertEquals(expectedResultText7_GetPackageText, 
                   result,
				     "Result not expected:\nexpected: " + expectedResultText7_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText8);
		assertEquals(expectedResultText8_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText8_GetPackageText + "\nactual:" + result + "\n");
	
		result = manifestAction.callGetPackageAttributeText(embeddingText9);
		assertEquals(expectedResultText9_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText9_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText10);
		assertEquals(expectedResultText10_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText10_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText11);
		assertEquals(expectedResultText11_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText11_GetPackageText + "\nactual:" + result + "\n");
		
		result = manifestAction.callGetPackageAttributeText(embeddingText12);
		assertEquals(expectedResultText12_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText12_GetPackageText + "\nactual:" + result + "\n");
	
		result = manifestAction.callGetPackageAttributeText(embeddingText13);
		assertEquals(expectedResultText13_GetPackageText,
				     result,
				     "Result not expected:\nexpected: " + expectedResultText13_GetPackageText + "\nactual:" + result + "\n");
	}
}
