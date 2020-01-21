package com.ibm.ws.jakarta.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import aQute.bnd.header.OSGiHeader;
import aQute.lib.strings.Strings;

public class JakartaTransformProperties {
	/** Name of the property which is used to convey package renames. */
	public static final String PACKAGE_RENAME_PROPERTY_NAME = "-package-rename";
	/** Character used to define a package rename. */
	public static final char PACKAGE_RENAME_ASSIGNMENT = '=';
	/** Character which separates package renames. */
	public static final char PACKAGE_RENAME_SEPARATOR = ';';

	/** Name of the property which is used to convey resource selections. */
	public static final String RESOURCE_SELECTION_PROPERTY_NAME = "-resource-selector";
	/** Character which separates resource selection values. */
	public static final char RESOURCE_SELECTION_SEPARATOR = ',';
	/** Prefix character for resources which are to be excluded. */
	public static final char RESOURCE_EXCLUSION_PREFIX = '!';

	public static void setProperties(
		Properties properties,
		Set<String> selections, Set<String> rejections, Map<String, String> renames) {

		properties.setProperty( RESOURCE_SELECTION_PROPERTY_NAME, getSelections(selections, rejections) );
		properties.setProperty( PACKAGE_RENAME_PROPERTY_NAME, getRenames(renames) );
	}

	public static String getSelections(Set<String> selections, Set<String> rejections) {
		StringBuilder selectionBuilder = new StringBuilder();

		boolean isFirst = true;

		for ( String selection : selections ) {
			if ( !isFirst ) {
				selectionBuilder.append(RESOURCE_SELECTION_SEPARATOR);
			} else {
				isFirst = false;
			}
			selectionBuilder.append(selection);
		}

		for ( String rejection : rejections ) {
			if ( !isFirst ) {
				selectionBuilder.append(RESOURCE_SELECTION_SEPARATOR);
			} else {
				isFirst = false;
			}
			selectionBuilder.append(RESOURCE_EXCLUSION_PREFIX);
			selectionBuilder.append(rejection);
		}

		return selectionBuilder.toString();
	}

	public static String getRenames(Map<String, String> renames) {
		StringBuilder renameBuilder = new StringBuilder();

		boolean isFirst = true;

		for ( Map.Entry<String, String> renameEntry : renames.entrySet() ) {
			if ( isFirst ) {
				renameBuilder.append(PACKAGE_RENAME_SEPARATOR);
			} else {
				isFirst = false;
			}
			renameBuilder.append( renameEntry.getKey() );
			renameBuilder.append(PACKAGE_RENAME_ASSIGNMENT);
			renameBuilder.append( renameEntry.getValue() );
		}

		return renameBuilder.toString();
	}

	//

	public static void setSelections(
		Set<String> included, Set<String> excluded,
		String selectionValue) {

		Strings.splitAsStream(selectionValue)
		    .forEach( selector -> {
		    	if ( !selector.isEmpty() && (selector.charAt(0) == RESOURCE_EXCLUSION_PREFIX) ) {
		    		excluded.add( selector.substring(1) );
		    	} else {
		    		included.add(selector);
		    	}
		    });
	}

	public static Map<String, String> getPackageRenames(String renameValue) {
		return OSGiHeader.parseProperties(renameValue);
	}

	public static Map<String, String> invert(Map<String, String> properties) {
		Map<String, String> inverseProperties = new HashMap<>( properties.size() );
		for ( Map.Entry<String, String> entry : properties.entrySet() ) {
			inverseProperties.put( entry.getValue(), entry.getKey() );
		}
		return inverseProperties;
	}	
}
