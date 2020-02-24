package com.ibm.ws.jakarta.transformer.action;

import java.util.List;

public interface ContainerAction extends Action {
	@Override
	ContainerChanges getChanges();

	CompositeAction getAction();
	List<? extends Action> getActions();
	Action acceptAction(String inputName);
}
