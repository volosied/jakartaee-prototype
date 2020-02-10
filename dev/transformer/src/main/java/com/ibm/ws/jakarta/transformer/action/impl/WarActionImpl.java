package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.WarAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;

public class WarActionImpl extends ActionImpl implements WarAction {
	public WarActionImpl(ActionImpl parentAction) {
		super(parentAction);
	}

	public WarActionImpl(Set<String> includes, Set<String> excludes, Map<String, String> renames,
			Map<String, String> versions) {
		super(includes, excludes, renames, versions);
	}

	public WarActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames,
		Map<String, String> versions) {

		super(logStream, isTerse, isVerbose, includes, excludes, renames, versions);
	}

	//

	public String getName() {
		return "WAR Action";
	}

	//

	@Override
	protected WarChangesImpl newChanges() {
		return new WarChangesImpl();
	}

	@Override
	public WarChangesImpl getChanges() {
		return (WarChangesImpl) super.getChanges();
	}

	//

	@Override
	public boolean accept(String resourceName) {
		// TODO: This is *not* sufficient: WAR files which are explicitly listed in
		//       an application descriptor may have any file extension.

		return resourceName.endsWith(".war");
	}

	// WAR do not support byte based conversion

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws JakartaTransformException {
		throw new UnsupportedOperationException();
	}
}