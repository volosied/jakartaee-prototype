package com.ibm.ws.jakarta.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import aQute.lib.utf8properties.UTF8Properties;

public class JakartaTransformProperties {
	/** Character used to define a package rename. */
	public static final char PACKAGE_RENAME_ASSIGNMENT = '=';

	/** Prefix character for resources which are to be excluded. */
	public static final char RESOURCE_EXCLUSION_PREFIX = '!';

	/** Used to demark head and tail regions in resource selections. */
	public static final char RESOURCE_WILDCARD = '*';

	//

	public static void setSelections(
		Set<String> included, Set<String> excluded,
		UTF8Properties selections) {

		for ( Map.Entry<Object, Object> selectionEntry : selections.entrySet() ) {
			String selection = (String) selectionEntry.getKey();
			if ( selection.charAt(0) == RESOURCE_EXCLUSION_PREFIX ) {
				excluded.add( selection.substring(1) );
			} else {
				included.add(selection);
			}
		}
	}

	public static void processSelections(
		Set<String> selections,
		Set<String> selectionsExact,
		Set<String> selectionsHead,
		Set<String> selectionsTail,
		Set<String> selectionsAny ) {

		for ( String selection : selections ) {
			int selectionLength = selection.length();

			boolean matchHead = ( selection.charAt(0) == RESOURCE_WILDCARD );
			boolean matchTail = ( selection.charAt(selectionLength - 1) == RESOURCE_WILDCARD );

			if ( matchHead ) {
				if ( matchTail ) {
					selectionsAny.add( selection.substring(1, selectionLength - 1) );
				} else {
					selectionsHead.add( selection.substring(1) );
				}
			} else if ( matchTail ) {
				selectionsTail.add( selection.substring(0, selectionLength - 1) );
			} else {
				selectionsExact.add(selection);
			}
		}
	}

	public static Map<String, String> getPackageRenames(UTF8Properties renameProperties) {
		Map<String, String> packageRenames = new HashMap<String, String>( renameProperties.size() );
		for ( Map.Entry<Object, Object> renameEntry : renameProperties.entrySet() ) {
			packageRenames.put( (String) renameEntry.getKey(), (String) renameEntry.getValue() );
		}
		return packageRenames;
	}

	public static Map<String, String> invert(Map<String, String> properties) {
		Map<String, String> inverseProperties = new HashMap<>( properties.size() );
		for ( Map.Entry<String, String> entry : properties.entrySet() ) {
			inverseProperties.put( entry.getValue(), entry.getKey() );
		}
		return inverseProperties;
	}
	
	public static Map<String, String> getPackageVersions(UTF8Properties versionProperties) {
		Map<String, String> packageVersions = new HashMap<String, String>( versionProperties.size() );
		for ( Map.Entry<Object, Object> versionEntry : versionProperties.entrySet() ) {
			packageVersions.put( (String) versionEntry.getKey(), (String) versionEntry.getValue() );
		}
		return packageVersions;
	}
}
