package com.ibm.ws.jakarta.transformer.action;

public interface ServiceConfigAction extends Action {
	@Override
	ServiceConfigChanges getChanges();

	String META_INF = "META-INF/";
	String META_INF_SERVICES = "META-INF/services/";
}
