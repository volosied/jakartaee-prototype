package com.ibm.ws.jakarta.transformer.action;

public interface ManifestAction extends Action {
	@Override
	ManifestChanges getChanges();

	String META_INF = "META-INF/";
	String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";
}
