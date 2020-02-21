package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.jakarta.transformer.JakartaTransformException;
import com.ibm.ws.jakarta.transformer.action.Action;
import com.ibm.ws.jakarta.transformer.action.ActionType;
import com.ibm.ws.jakarta.transformer.action.ContainerAction;
import com.ibm.ws.jakarta.transformer.util.ByteData;

public abstract class ContainerActionImpl extends ActionImpl implements ContainerAction {

	public ContainerActionImpl(
		LoggerImpl logger,
		InputBufferImpl buffer,
		SelectionRuleImpl selectionRule,
		SignatureRuleImpl signatureRule) {

		super(logger, buffer, selectionRule, signatureRule);

		this.actions = new ArrayList<ActionImpl>();
	}

	public <A extends ActionImpl> A addUsing(ActionInit<A> init) {
		A action = createUsing(init);
		addAction(action);
		return action;
	}

	//

	@Override
	public abstract String getName();

	@Override
	public abstract ActionType getActionType();

	//

	@Override
	protected abstract ContainerChangesImpl newChanges();

	@Override
	public ContainerChangesImpl getChanges() {
		return (ContainerChangesImpl) super.getChanges();
	}

	//

	protected void recordUnaccepted(String resourceName) {
		verbose( "Resource [ %s ]: Not accepted\n", resourceName );

		getChanges().record();
	}

	protected void recordUnselected(Action action, boolean hasChanges, String resourceName) {
		verbose(
			"Resource [ %s ] Action [ %s ]: Accepted but not selected\n",
			resourceName, action.getName() );

		getChanges().record(action, hasChanges);
	}

	protected void recordTransform(Action action, String resourceName) {
		verbose(
			"Resource [ %s ] Action [ %s ]: Changes [ %s ]\n",
			resourceName, action.getName(), action.hasChanges() );

		getChanges().record(action);
	}

	//

	private final List<ActionImpl> actions;

	@Override
	public List<ActionImpl> getActions() {
		return actions;
	}

	protected void addAction(ActionImpl action) {
		getActions().add(action);
	}

	@Override
	public ActionImpl selectAction(String inputName) {
		for ( ActionImpl action : getActions() ) {
			if ( action.select(inputName) ) {
				return action;
			}
		}
		return null;
	}

	//

	@Override
	public boolean accept(String resourceName) {
		return resourceName.endsWith(".jar");
	}

	//

	public abstract void apply(
		String inputPath, InputStream inputStream,
		String outputPath, OutputStream outputStream) throws JakartaTransformException;

	// Byte base container conversion is not supported.

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputLength) throws JakartaTransformException {
		throw new UnsupportedOperationException();
	}
}