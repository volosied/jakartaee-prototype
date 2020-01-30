package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ibm.ws.jakarta.transformer.JakartaTransformer;

public class TestLoad {

	public static final String RULES_PATH =
		JakartaTransformer.DEFAULT_RULES_REFERENCE;

	@Test
	public void testRulesLoad() throws IOException {
		System.out.println("Load [ " + RULES_PATH + " ]");

		InputStream simpleInput = TestUtils.getResourceStream(RULES_PATH);

		List<String> actualLines;
		try {
			actualLines = TestUtils.loadLines(simpleInput);
		} finally {
			simpleInput.close();
		}

		System.out.println("Loaded [ " + RULES_PATH + " ] [ " + actualLines.size() + " ]");
		for ( String line : actualLines ) {
			System.out.println(" [ " + line + " ]");
		}
	}

	public static final String SIMPLE_RESOURCE_PATH = "transform/test/data/simple.resource";
	public static final String[] SIMPLE_RESOURCE_LINES = {
		"Simple Resource 1",
		"Simple Resource 2"
	};

	@Test
	public void testSimpleLoad() throws IOException {
		System.out.println("Load [ " + SIMPLE_RESOURCE_PATH + " ]");

		InputStream simpleInput = TestUtils.getResourceStream(SIMPLE_RESOURCE_PATH);

		List<String> actualLines;
		try {
			actualLines = TestUtils.loadLines(simpleInput);
		} finally {
			simpleInput.close();
		}

		System.out.println("Loaded [ " + SIMPLE_RESOURCE_PATH + " ] [ " + actualLines.size() + " ]");
		System.out.println("Expected [ " + SIMPLE_RESOURCE_PATH + " ] [ " + SIMPLE_RESOURCE_LINES.length + " ]");

		TestUtils.verify(SIMPLE_RESOURCE_PATH, SIMPLE_RESOURCE_LINES, actualLines);
	}
}
