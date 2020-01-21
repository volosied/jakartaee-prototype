package com.ibm.ws.jakarta.transformer.action;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

public class WarActionImpl extends ActionImpl implements WarAction {
	public WarActionImpl(ActionImpl parentAction) {
		super(parentAction);
	}

	public WarActionImpl(Set<String> includes, Set<String> excludes, Map<String, String> renames) {
		super(includes, excludes, renames);
	}

	public WarActionImpl(
		PrintStream logStream, boolean isTerse, boolean isVerbose,
		Set<String> includes, Set<String> excludes, Map<String, String> renames) {

		super(logStream, isTerse, isVerbose, includes, excludes, renames);
	}
}