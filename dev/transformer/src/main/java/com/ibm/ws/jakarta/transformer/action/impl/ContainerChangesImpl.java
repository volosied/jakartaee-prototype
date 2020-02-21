package com.ibm.ws.jakarta.transformer.action.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.jakarta.transformer.action.Action;
import com.ibm.ws.jakarta.transformer.action.ContainerChanges;

public class ContainerChangesImpl extends ChangesImpl implements ContainerChanges {

	protected ContainerChangesImpl() {
		super();

		this.changedByAction = new HashMap<String, int[]>();
		this.unchangedByAction = new HashMap<String, int[]>();

		this.allChanged = 0;
		this.allUnchanged = 0;

		this.allSelected = 0;
		this.allUnselected = 0;
		this.allResources = 0;
	}

	//

	@Override
	public boolean hasNonResourceNameChanges() {
		return ( allChanged > 0 );
	}

	@Override
	public void clearChanges() {
		changedByAction.clear();
		unchangedByAction.clear();

		allChanged = 0;
		allUnchanged = 0;

		allSelected = 0;
		allUnselected = 0;
		allResources = 0;

		super.clearChanges();
	}

	//

	private final Map<String, int[]> changedByAction;
	private final Map<String, int[]> unchangedByAction;

	private int allUnchanged;
	private int allChanged;

	private int allSelected;
	private int allUnselected;	
	private int allResources;

	//

	@Override
	public Set<String> getActionNames() {
		Set<String> changedNames = changedByAction.keySet();
		Set<String> unchangedNames = unchangedByAction.keySet();

		Set<String> allNames =
			new HashSet<String>( changedNames.size() + unchangedNames.size() );

		allNames.addAll(changedNames);
		allNames.addAll(unchangedNames);

		return allNames;
	}

	//

	@Override
	public int getAllResources() {
		return allResources;
	}

	@Override
	public int getAllUnselected() {
		return allUnselected;
	}

	@Override
	public int getAllSelected() {
		return allSelected;
	}

	@Override
	public int getAllUnchanged() {
		return allUnchanged;
	}

	@Override
	public int getAllChanged() {
		return allChanged;
	}

	@Override
	public int getChanged(Action action) {
		return getChanged( action.getName() );
	}

	@Override
	public int getChanged(String name) {
		int[] changes = changedByAction.get(name);
		return ( (changes == null) ? 0 : changes[0] );
	}

	@Override
	public int getUnchanged(Action action) {
		return getUnchanged( action.getName() );
	}

	@Override
	public int getUnchanged(String name) {
		int[] changes = unchangedByAction.get(name);
		return ( (changes == null) ? 0 : changes[0] );
	}

	@Override
	public void record(Action action) {
		record( action.getName(), action.hasChanges() );
	}

	@Override
	public void record(Action action, boolean hasChanges) {
		record( action.getName(), hasChanges );
	}

	@Override
	public void record(String name, boolean hasChanges) {
		allResources++;
		allSelected++;

		Map<String, int[]> target;
		if ( hasChanges ) {
			allChanged++;
			target = changedByAction;
		} else {
			allUnchanged++;
			target = unchangedByAction;
		}

		int[] changes = target.get(name);
		if ( changes == null ) {
			changes = new int[] { 1 };
			target.put(name, changes);
		} else {
			changes[0]++;
		}
	}

	@Override
	public void record() {
		allResources++;
		allUnselected++;
	}
}
