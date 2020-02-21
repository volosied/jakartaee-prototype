package com.ibm.ws.jakarta.transformer.action;

import java.util.List;

public interface ContainerAction extends Action {
	@Override
	ContainerChanges getChanges();

	List<? extends Action> getActions();
	Action selectAction(String inputName);
}
