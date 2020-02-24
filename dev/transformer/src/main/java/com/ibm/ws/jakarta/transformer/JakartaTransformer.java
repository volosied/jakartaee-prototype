package com.ibm.ws.jakarta.transformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ibm.ws.jakarta.transformer.action.ActionType;
import com.ibm.ws.jakarta.transformer.action.BundleData;
import com.ibm.ws.jakarta.transformer.action.ClassAction;
import com.ibm.ws.jakarta.transformer.action.ClassChanges;
import com.ibm.ws.jakarta.transformer.action.DirectoryChanges;
import com.ibm.ws.jakarta.transformer.action.JarChanges;
import com.ibm.ws.jakarta.transformer.action.ManifestAction;
import com.ibm.ws.jakarta.transformer.action.ManifestChanges;
import com.ibm.ws.jakarta.transformer.action.ServiceConfigAction;
import com.ibm.ws.jakarta.transformer.action.ServiceConfigChanges;
import com.ibm.ws.jakarta.transformer.action.impl.ClassActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.InputBufferImpl;
import com.ibm.ws.jakarta.transformer.action.impl.DirectoryActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.JarActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.LoggerImpl;
import com.ibm.ws.jakarta.transformer.action.impl.ManifestActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.SelectionRuleImpl;
import com.ibm.ws.jakarta.transformer.action.impl.ServiceConfigActionImpl;
import com.ibm.ws.jakarta.transformer.action.impl.SignatureRuleImpl;
import com.ibm.ws.jakarta.transformer.util.FileUtils;

import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.uri.URIUtil;

public class JakartaTransformer {
    public static final int SUCCESS_RC = 0;
    public static final int PARSE_ERROR_RC = 1;
    public static final int RULES_ERROR_RC = 2;
    public static final int TRANSFORM_ERROR_RC = 3;

    public static void main(String[] args) throws Exception {
        JakartaTransformer jTrans =
            new JakartaTransformer(System.out, System.err);
        jTrans.setArgs(args);

        @SuppressWarnings("unused")
        int rc = jTrans.run();
        // System.exit(rc); // TODO: How should this code be returned?
    }

    //

    public static class OptionSettings {
        private static final boolean HAS_ARG = true;
        private static final boolean IS_REQUIRED = true;
        private static final String NO_GROUP = null;

        private OptionSettings (
            String shortTag, String longTag, String description,
            boolean hasArg,
            boolean isRequired, String groupTag) {

            this.shortTag = shortTag;
            this.longTag = longTag;
            this.description = description;

            this.isRequired = isRequired;

            this.hasArg = hasArg;
            this.groupTag = groupTag;
        }

        private final String shortTag;
        private final String longTag;
        private final String description;

        public String getShortTag() {
            return shortTag;
        }

        public String getLongTag() {
            return longTag;
        }

        public String getDescription() {
            return description;
        }

        //

        // Is this option required.
        // If in a group, is at least one of the group required.

        private final boolean isRequired;

        //

        private final boolean hasArg;
        private final String groupTag;

        public boolean getHasArg() {
            return hasArg;
        }

        public String getGroupTag() {
            return groupTag;
        }

        public boolean getIsRequired() {
            return isRequired;
        }

        //

        public static Options build(OptionSettings[] settings) {
            Options options = new Options();

            Map<String, OptionGroup> groups = new HashMap<String, OptionGroup>();

            for ( OptionSettings optionSettings : settings ) {
                String groupTag = optionSettings.getGroupTag();
                OptionGroup group;
                if ( groupTag != null ) {
                    group = groups.get(groupTag);
                    if ( group == null ) {
                        group = new OptionGroup();
                        if ( optionSettings.getIsRequired() ) {
                            group.setRequired(true);
                        }
                        groups.put(groupTag, group);

                        options.addOptionGroup(group);
                    }

                } else {
                    group = null;
                }

                Option option = Option.builder( optionSettings.getShortTag() )
                    .longOpt( optionSettings.getLongTag() )
                    .desc( optionSettings.getDescription() )
                    .hasArg( optionSettings.getHasArg() )
                    .required( (group == null) && optionSettings.getIsRequired() )
                    .build();

                if ( group != null ) {
                    group.addOption(option);
                } else {
                    options.addOption(option);
                }
            }

            return options;
        }
    }

    // Not in use, until option grouping is figured out.

    public static final String INPUT_GROUP = "input";
    public static final String LOGGING_GROUP = "logging";

    public static final String DEFAULT_SELECTIONS_REFERENCE = "jakarta-selections.properties";
    public static final String DEFAULT_RENAMES_REFERENCE = "jakarta-renames.properties";
    public static final String DEFAULT_VERSIONS_REFERENCE = "jakarta-versions.properties";
    public static final String DEFAULT_BUNDLES_REFERENCE = "jakarta-bundles.properties";

    public static enum AppOption {
        USAGE  ("u", "usage",    "Display usage",
            	!OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        HELP   ("h", "help",    "Display help",
            	!OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        TERSE  ("q", "quiet",   "Display quiet output",
        	!OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        VERBOSE("v", "verbose", "Display verbose output",
        	!OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),        

        RULES_SELECTIONS("ts", "selection", "Transformation selections URL",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_RENAMES("tr", "renames", "Transformation package renames URL",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_VERSIONS("tv", "versions", "Transformation package versions URL",
            OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RULES_BUNDLES("tb", "bundles", "Transformation bundle updates URL",
            OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        INVERT("i", "invert", "Invert transformation rules",
           	!OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        CLASS("c", "class", "Input class",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        MANIFEST("m", "manifest", "Input manifest",
            OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        FEATURE("f", "feature", "Input feature manifest",
            OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        SERVICE_CONFIG("s", "service config", "Input service configuration",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        XML("x", "xml", "Input XML",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        ZIP  ("z", "zip",   "Input zip archive",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        JAR  ("j", "jar",   "Input java archive",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        WAR  ("w", "war",   "Input web application archive",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        RAR  ("r", "rar",   "Input resource archive",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),
        EAR  ("e", "ear",   "Input enterprise application archive",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

        OUTPUT("o", "output", "Output file",
        	OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP),

    	DRYRUN("d", "dryrun", "Dry run",
            !OptionSettings.HAS_ARG, !OptionSettings.IS_REQUIRED, OptionSettings.NO_GROUP);

        private AppOption(
            String shortTag, String longTag, String description, boolean hasArg,
            boolean isRequired, String groupTag) {

            this.settings = new OptionSettings(
                shortTag, longTag, description, hasArg,
                isRequired, groupTag);
        }

        private final OptionSettings settings;

        public OptionSettings getSettings() {
            return settings;
        }

        public String getShortTag() {
            return getSettings().getShortTag();
        }
        
        public String getLongTag() {
            return getSettings().getLongTag();
        }

        public String getDescription() {
            return getSettings().getDescription();
        }

        public boolean getIsRequired() {
            return getSettings().getIsRequired();
        }

        public boolean getHasArg() {
            return getSettings().getHasArg();
        }

        public String getGroupTag() {
            return getSettings().getGroupTag();
        }

        //

        private static OptionSettings[] getAllSettings() {
            AppOption[] allAppOptions =  AppOption.values();

            OptionSettings[] allSettings = new OptionSettings[ allAppOptions.length ];

            for ( int optionNo = 0; optionNo < allAppOptions.length; optionNo++ ) {
                allSettings[optionNo] = allAppOptions[optionNo].getSettings();
            }

            return allSettings;
        }

        public static Options build() {
            return OptionSettings.build( getAllSettings() );
        }
    }

    public JakartaTransformer(PrintStream infoStream, PrintStream errorStream) {
        this.infoStream = infoStream;
        this.errorStream = errorStream;

        this.appOptions = AppOption.build();
    }

    private final PrintStream infoStream;
    private final PrintStream errorStream;

    public PrintStream getInfoStream() {
        return infoStream;
    }

    public void info(String text, Object... parms) {
        getInfoStream().printf(text, parms);
    }

    public PrintStream getErrorStream() {
        return errorStream;
    }

    protected void error(String message, Object... parms) {
        getErrorStream().printf(message, parms);
    }

    protected void error(String message, Throwable th, Object... parms) {
        getErrorStream().printf(message, th.getMessage(), parms);
        th.printStackTrace( getErrorStream() );
    }

    private final Options appOptions;

    public Options getAppOptions() {
        return appOptions;
    }

    private String[] args;
    private CommandLine parsedArgs;

    protected void setArgs(String[] args) {
        this.args = args;
    }

    protected String[] getArgs() {
        return args;
    }

    protected void setParsedArgs() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        this.parsedArgs = parser.parse( getAppOptions(), getArgs());
    }

    protected CommandLine getParsedArgs() {
        return parsedArgs;
    }
    
    protected String getInputFileName() {
    	String[] args = parsedArgs.getArgs();
    	if ( args != null ) {
        	if (args.length > 0) {
        		return FileUtils.normalize(args[0]);
        	} 
    	}
    	return null;
    }
    
    protected String getOutputFileName() {
    	String[] args = parsedArgs.getArgs();
    	if ( args != null ) {
        	if (args.length > 1) {
        		return FileUtils.normalize(args[1]);
        	} 
    	}
    	return null;
    }
    
    protected boolean hasOption(AppOption option) {
        return getParsedArgs().hasOption( option.getShortTag() );
    }
    
    protected String getOptionValue(AppOption option) {
        CommandLine useParsedArgs = getParsedArgs();
        String useShortTag = option.getShortTag();
        if ( useParsedArgs.hasOption(useShortTag) ) {
            return useParsedArgs.getOptionValue(useShortTag);
        } else {
            return null;
        }
    }

    //

    private void help(PrintStream helpStream) {
        try ( PrintWriter helpWriter = new PrintWriter(helpStream) ) {
            helpWriter.println();

            HelpFormatter helpFormatter = new HelpFormatter();
            boolean AUTO_USAGE = true;

            helpFormatter.printHelp(
                helpWriter,
                HelpFormatter.DEFAULT_WIDTH,
                JakartaTransformer.class.getName() + " [ options ]", // Command line syntax
                "\nOptions:", // Header
                getAppOptions(),
                HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD,
                "\n", // Footer
                !AUTO_USAGE);

            helpWriter.flush();
        }
    }

    //

    protected UTF8Properties loadProperties(AppOption ruleOption, String defaultReference) throws IOException, URISyntaxException {

    	URL rulesUrl;
        String rulesReference = getOptionValue(ruleOption);
        if ( rulesReference != null ) {
            info("Using external [ " + ruleOption + " ]: [ " + rulesReference + " ]\n");
            URI currentDirectoryUri = IO.work.toURI();
            rulesUrl = URIUtil.resolve(currentDirectoryUri, rulesReference).toURL();
            info("External [ " + ruleOption + " ] URL [ " + rulesUrl + " ]\n");
        } else {
            rulesReference = defaultReference;
            info("Using internal [ " + ruleOption + " ]: [ " + rulesReference + " ]\n");
            rulesUrl = getClass().getResource(rulesReference);
            if ( rulesUrl == null ) {
        		info("Default [ " + AppOption.RULES_SELECTIONS + " ] were not found [ " + rulesReference + " ]");
        		return null;
            } else {
                info("Default [ " + ruleOption + " ] URL [ " + rulesUrl + " ]\n");
            }
        }

        try ( InputStream rulesStream = rulesUrl.openStream() ) {
            UTF8Properties properties = new UTF8Properties();
            properties.load(rulesStream);
            return properties;
        }
    }

    private class TransformOptions {
    	public boolean isVerbose;
    	public boolean isTerse;

    	public Set<String> includes;
    	public Set<String> excludes;

    	public boolean invert;
    	public Map<String, String> packageRenames;
    	public Map<String, String> packageVersions;
    	public Map<String, BundleData> bundleUpdates;

    	public ActionType actionType;

    	public String inputName;
    	public String outputName;

        public File inputFile;
        public String inputPath;
        
        public File outputFile;
        public String outputPath;

    	protected void setLogging() {
            if ( hasOption(AppOption.TERSE) ) {
            	isTerse = true;
            	isVerbose = false;
                info("Terse output requested\n");
            } else if ( hasOption(AppOption.VERBOSE) ) {
            	isTerse = false;
            	isVerbose = true;
                info("Verbose output requested\n");
            } else {
            	isTerse = false;
            	isVerbose = false;
            }
    	}

    	private LoggerImpl logger;

    	protected LoggerImpl getLogger() {
    		if ( logger == null ) {
    			logger = new LoggerImpl( getInfoStream(), isTerse, isVerbose );
    		}
    		return logger;
    	}

    	private InputBufferImpl buffer;
    	
    	protected InputBufferImpl getBuffer() {
    		if ( buffer == null ) {
    			buffer = new InputBufferImpl();
    		}
    		return buffer;
    	}
    	
    	protected boolean setRules() throws IOException, URISyntaxException, IllegalArgumentException {
    		UTF8Properties selectionProperties = loadProperties(AppOption.RULES_SELECTIONS, DEFAULT_SELECTIONS_REFERENCE);
    		UTF8Properties renameProperties = loadProperties(AppOption.RULES_RENAMES, DEFAULT_RENAMES_REFERENCE);
    		UTF8Properties versionProperties = loadProperties(AppOption.RULES_VERSIONS, DEFAULT_VERSIONS_REFERENCE);
    		UTF8Properties updateProperties = loadProperties(AppOption.RULES_BUNDLES, DEFAULT_BUNDLES_REFERENCE);

        	invert = hasOption(AppOption.INVERT);

        	includes = new HashSet<String>();
        	excludes = new HashSet<String>();

        	if ( selectionProperties != null ) {
        		JakartaTransformProperties.setSelections(includes, excludes, selectionProperties);
        	} else {
        		info("All resources will be selected");
        	}

        	if ( renameProperties != null ) {
        		Map<String, String> renames = JakartaTransformProperties.getPackageRenames(renameProperties);
        		if ( invert ) {
        			renames = JakartaTransformProperties.invert(renames);
        		}
        		packageRenames = renames;
        	} else {
        		info("No package renames are available");
        		packageRenames = null;
        	}

        	if ( versionProperties != null ) {
        		packageVersions = JakartaTransformProperties.getPackageVersions(versionProperties);
        	} else {
        		info("Package versions will not be updated");
        	}

        	if ( updateProperties != null ) {
        		bundleUpdates = JakartaTransformProperties.getBundleUpdates(updateProperties);
        		// throws IllegalArgumentException
        	} else {
        		info("Bundle identities will not be updated");
        	}
        	
        	return ( packageRenames != null );
    	}

    	protected void logRules(PrintStream logStream) {
    		logStream.println("Includes:");
    		if ( includes.isEmpty() ) {
    			logStream.println("  [ ** NONE ** ]");
    		} else {
    			for ( String include : includes ) {
    				logStream.println("  [ " + include + " ]");
    			}
    		}

      		logStream.println("Excludes:");
    		if ( excludes.isEmpty() ) {
    			logStream.println("  [ ** NONE ** ]");
    		} else {
    			for ( String exclude : excludes ) {
    				logStream.println("  [ " + exclude + " ]");
    			}
    		}

    		if ( invert ) {
          		logStream.println("Package Renames: [ ** INVERTED ** ]");
    		} else {
          		logStream.println("Package Renames:");
    		}

    		if ( packageRenames.isEmpty() ) {
    			logStream.println("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, String> renameEntry : packageRenames.entrySet() ) {
        			logStream.println("  [ " + renameEntry.getKey() + " ]: [ " + renameEntry.getValue() + " ]");
    			}
    		}

    		logStream.println("Package Versions:");
    		if ( packageVersions.isEmpty() ) {
    			logStream.println("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, String> versionEntry : packageVersions.entrySet() ) {
        			logStream.println("  [ " + versionEntry.getKey() + " ]: [ " + versionEntry.getValue() + " ]");
    			}
    		}

    		logStream.println("Bundle Updates:");
    		if ( bundleUpdates.isEmpty() ) {
    			logStream.println("  [ ** NONE ** ]");
    		} else {
    			for ( Map.Entry<String, BundleData> updateEntry : bundleUpdates.entrySet() ) {
    				BundleData updateData = updateEntry.getValue();

    				logStream.println("  [ " + updateEntry.getKey() + " ]: [ " + updateData.getSymbolicName() + " ]");

        			logStream.println("    [ Version ]: [ " + updateData.getVersion() + " ]");

        			if ( updateData.getAddName() ) {
        				logStream.println("    [ Name ]: [ " + BundleData.ADDITIVE_CHAR + updateData.getName() + " ]");
        			} else {
        				logStream.println("    [ Name ]: [ " + updateData.getName() + " ]");
        			}

        			if ( updateData.getAddDescription() ) {
        				logStream.println("    [ Description ]: [ " + BundleData.ADDITIVE_CHAR + updateData.getDescription() + " ]");
        			} else {
        				logStream.println("    [ Description ]: [ " + updateData.getDescription() + " ]");
        			}
    			}
    		}
    	}

    	private SelectionRuleImpl selectionRules;

    	protected SelectionRuleImpl getSelectionRule() {
    		if ( selectionRules == null ) {
    			selectionRules = new SelectionRuleImpl(
    				getLogger(),
    				includes, excludes);
    		}
    		return selectionRules;
    	}

    	private SignatureRuleImpl signatureRules;

    	protected SignatureRuleImpl getSignatureRule() {
    		if ( signatureRules == null ) {
    			signatureRules =  new SignatureRuleImpl(
        			getLogger(),
        			packageRenames, packageVersions, bundleUpdates);
    		}
    		return signatureRules;
    	}

//    	protected void setInput(String inputName, ActionType transformType) {
//    		//this.inputName = FileUtils.normalize(inputName);
//    		this.actionType = transformType;
//            info("Input %s [ %s ]\n", this.actionType, this.inputName);
//    	}

        protected boolean setInput() {
        	
        	inputName = getInputFileName();
        	String lowerInputName = inputName.toLowerCase();
        	
//        	File test = new File(inputName);
//        	System.out.println("File name is: [" + test.getName() + "]");
//        	System.out.println("File path is: [" + test.getAbsolutePath() + "]");
//        	System.out.println("File exists?: [" + test.exists() + "]");
//        	System.out.println("File is dir: [" + test.isDirectory() + "]");
        	
        	if ((new File(inputName)).isDirectory()) {
        		this.actionType = ActionType.DIRECTORY;
        		
        	} else if (lowerInputName.endsWith(".class")) {
        		this.actionType = ActionType.CLASS;
        		
        	} else if (lowerInputName.endsWith(".mf")) {
        		
        		// TODO - detect MANIFEST vs FEATURE MANIFEST
        		//    read file and check line lengths > 72
        		//   OR have a -type option to specify the file type
        		this.actionType = ActionType.FEATURE;
        		this.actionType = ActionType.MANIFEST;
        		
        	} else if (lowerInputName.endsWith(".??????")) {
        		this.actionType = ActionType.SERVICE_CONFIG;
        		
        	} else if (lowerInputName.endsWith(".xml")) {
        		this.actionType = ActionType.XML;
        		
        	} else if (lowerInputName.endsWith(".zip")) {
        		this.actionType = ActionType.ZIP;
        		
        	} else if (lowerInputName.endsWith(".jar")) {
        		this.actionType = ActionType.JAR;
        		
        	} else if (lowerInputName.endsWith(".war")) {
        		this.actionType = ActionType.WAR;
        		
        	} else if (lowerInputName.endsWith(".rar")) {
        		this.actionType = ActionType.RAR;
        		
        	} else if (lowerInputName.endsWith(".ear")) {
        		this.actionType = ActionType.EAR;
        		
        	} else {
        		return false;
        	}
        	
            info("Input %s [ %s ]\n", this.actionType, this.inputName);

            return true;
        }

        protected boolean setOutput() {
        	
            outputName = getOutputFileName(); //getOptionValue(AppOption.OUTPUT);
            if ( outputName != null ) {
            	info("Transformation output to [ %s ]\n", outputName);
            	return true;
            } else {
            	return false;
            }
        }

        protected boolean validateFiles() {
            inputFile = new File(inputName);
            inputPath = inputFile.getAbsolutePath();
            info("Input path [ %s ]\n", inputPath);

            outputFile = new File(outputName);
            outputPath = outputFile.getAbsolutePath();
            info("Output path [ %s ]\n", outputPath);

            if ( !inputFile.exists() ) {
            	error("Input does not exist [ " + inputFile.getAbsolutePath() + " ]\n");
            	return false;

            }

            if ( outputFile.exists() ) {
            	error("Output already exists [ " + outputFile.getAbsolutePath() + " ]\n");
            	return false;
            }

            return true;
        }

        protected void transform() throws JakartaTransformException {
            long inputLength = inputFile.length();

            if (inputFile.isDirectory()) {
            	transformDirectory(inputFile, outputFile);  // throws JakartaTransformException
            	return;
            }                   

            try ( InputStream inputStream = IO.stream(inputFile) ) {
                try ( OutputStream outputStream = IO.outputStream(outputFile) ) {
                	transform(inputStream, inputLength, outputStream); // throws JakartaTransformException
                } catch ( IOException e ) {
                	throw new JakartaTransformException("Failed to open input [ " + inputFile.getAbsolutePath() + " ]", e);
                }
            } catch ( IOException e ) {
            	throw new JakartaTransformException("Failed to open output [ " + outputFile.getAbsolutePath() + " ]", e);
            }
        }

        protected void transformClass(
            	InputStream inputStream, long inputLength,
            	OutputStream outputStream) throws JakartaTransformException {

    		int intLength = FileUtils.verifyArray(0, inputLength);

    		ClassAction classAction = new ClassActionImpl(
        		getLogger(), getBuffer(), getSelectionRule(), getSignatureRule() ); 

    		classAction.apply(inputPath, inputStream, intLength, outputStream);

    		if ( classAction.hasChanges() ) {
    			ClassChanges classChanges = classAction.getChanges();

    			info( "Class name [ %s ] [ %s ]\n",
            		classChanges.getInputClassName(),
    				classChanges.getOutputClassName() );

    			String inputSuperName = classChanges.getInputSuperName();
    			if ( inputSuperName != null ) {
    				info( "Class name [ %s ] [ %s ]\n",
    					inputSuperName,
    					classChanges.getOutputSuperName() );
    			}

    			info( "Modified interfaces [ %s ]\n", classChanges.getModifiedInterfaces() );
    			info( "Modified fields     [ %s ]\n", classChanges.getModifiedFields() );
    			info( "Modified methods    [ %s ]\n", classChanges.getModifiedMethods() );
    			info( "Modified constants  [ %s ]\n", classChanges.getModifiedConstants() );
    		}
        }

        protected void transformServiceConfig(
        	InputStream inputStream, long inputLength,
            OutputStream outputStream) throws JakartaTransformException {

    		int intLength = FileUtils.verifyArray(0, inputLength);

    		ServiceConfigAction configAction = new ServiceConfigActionImpl(
            	getLogger(), getBuffer(), getSelectionRule(), getSignatureRule() ); 

    		configAction.apply(inputPath, inputStream, intLength, outputStream);

    		if ( configAction.hasChanges() ) {
    			ServiceConfigChanges configChanges = configAction.getChanges();

    			info( "Resource name [ %s ] [ %s ]\n",
            		configChanges.getInputResourceName(),
    				configChanges.getOutputResourceName() );

    			info( "Replacements [ %s ]\n", configChanges.getChangedProviders() );
    		}
        }

        protected void transformManifest(
            InputStream inputStream, long inputLength,
            OutputStream outputStream) throws JakartaTransformException {

    		int intLength = FileUtils.verifyArray(0, inputLength);

    		ManifestAction manifestAction = new ManifestActionImpl(
    			getLogger(), getBuffer(), getSelectionRule(), getSignatureRule(),
            	ManifestActionImpl.IS_MANIFEST);

    		manifestAction.apply(inputPath, inputStream, intLength, outputStream);

    		if ( manifestAction.hasChanges() ) {
    			ManifestChanges configChanges = manifestAction.getChanges();

    			info( "Resource name [ %s ] [ %s ]\n",
            		configChanges.getInputResourceName(),
    				configChanges.getOutputResourceName() );

    			info( "Replacements [ %s ]\n", configChanges.getReplacements() );
    		}
        }

        protected void transformFeature(
        	InputStream inputStream, long inputLength,
        	OutputStream outputStream) throws JakartaTransformException {

        	int intLength = FileUtils.verifyArray(0, inputLength);

        	ManifestAction featureAction = new ManifestActionImpl(
        		getLogger(), getBuffer(), getSelectionRule(), getSignatureRule(),
        		ManifestActionImpl.IS_FEATURE );

        	featureAction.apply(inputPath, inputStream, intLength, outputStream);

        	if ( featureAction.hasChanges() ) {
        		ManifestChanges configChanges = featureAction.getChanges();

        		info( "Resource name [ %s ] [ %s ]\n",
               		configChanges.getInputResourceName(),
        			configChanges.getOutputResourceName() );

        		info( "Replacements [ %s ]\n", configChanges.getReplacements() );
        	}
        }

        protected void transformDirectory(File inputFile,
        		                          File outputFile) throws JakartaTransformException {
    		
    		DirectoryActionImpl dirAction = new DirectoryActionImpl(
                    getLogger(), getBuffer(), getSelectionRule(), getSignatureRule());
                dirAction.addUsing( ClassActionImpl::new );
                dirAction.addUsing( ServiceConfigActionImpl::new );
                dirAction.addUsing( ManifestActionImpl::newManifestAction );
                dirAction.addUsing( ManifestActionImpl::newFeatureAction );
                dirAction.addUsing( JarActionImpl::new );

    		dirAction.apply(inputFile, outputFile);

    		if ( dirAction.hasChanges() ) {
    			DirectoryChanges dirChanges = dirAction.getChanges();
                dirChanges.displayChanges();
    		}
        }
        
        //

        protected void transformJar(
        		InputStream inputStream, long inputLength,
        		OutputStream outputStream) throws JakartaTransformException {

    		JarActionImpl jarAction = new JarActionImpl(
    			getLogger(), getBuffer(), getSelectionRule(), getSignatureRule());
    		jarAction.addUsing( ClassActionImpl::new );
    		jarAction.addUsing( ServiceConfigActionImpl::new );
    		jarAction.addUsing( ManifestActionImpl::newManifestAction );
    		jarAction.addUsing( ManifestActionImpl::newFeatureAction );

        	jarAction.apply(inputPath, inputStream, outputPath, outputStream); 
            
            if ( jarAction.hasChanges() ) {
                JarChanges jarChanges = jarAction.getChanges();
                jarChanges.displayChanges();
            }
        }

    	protected void transformOther( 
    		InputStream inputStream, long inputLength,
    		OutputStream outputStream) throws JakartaTransformException {

    		info("Stub transfer [ " + inputPath + " ] to [ " + outputPath + " ]\n");

    		try {
    			FileUtils.transfer(inputStream, outputStream);
    		} catch ( IOException e ) {
    			throw new JakartaTransformException(
    				"Raw transfer failure from [ " + inputPath + " ] to [ " + outputPath + " ]",
    				e);
    		}
    	}
    	
        protected void transform(
        	InputStream inputStream, long inputLength,
        	OutputStream outputStream) throws JakartaTransformException {

        	if ( actionType == ActionType.CLASS ) {
        		transformClass(inputStream, inputLength, outputStream);
        	} else if ( actionType == ActionType.SERVICE_CONFIG) {
        		transformServiceConfig(inputStream, inputLength, outputStream);
        	} else if ( actionType == ActionType.MANIFEST) {
        		transformManifest(inputStream, inputLength, outputStream);
        	} else if ( actionType == ActionType.FEATURE) {
        		transformFeature(inputStream, inputLength, outputStream);

        	} else if ( actionType == ActionType.XML ) {
        		transformOther(inputStream, inputLength, outputStream);

        	} else if ( actionType == ActionType.JAR ) {
        		transformJar(inputStream, inputLength, outputStream);

        	} else if ( actionType == ActionType.ZIP ) {
        		transformOther(inputStream, inputLength, outputStream);
        	} else if ( actionType == ActionType.WAR ) {
        		transformOther(inputStream, inputLength, outputStream);
        	} else if ( actionType == ActionType.RAR) {
        		transformOther(inputStream, inputLength, outputStream);
        	} else if ( actionType == ActionType.EAR ) {
        		transformOther(inputStream, inputLength, outputStream);

        	} else {
        		throw new IllegalArgumentException("Unknown transform type [ " + actionType + " ]");
        	}
        }
    }

    public int run() {
        try {
            setParsedArgs();
        } catch ( ParseException e ) {
            error("Exception parsing command line arguments: %s\n", e);
            help( getErrorStream() );
            return PARSE_ERROR_RC;
        }

        if ( hasOption(AppOption.HELP) || hasOption(AppOption.USAGE) ) {
            help( getErrorStream() );
            // TODO: Split help and usage
            return SUCCESS_RC; // TODO: Is this the correct return value?
        }

        TransformOptions options = new TransformOptions();

        options.setLogging();

        if ( !options.setInput() ) { 
            error("No input file was specified");
            return TRANSFORM_ERROR_RC;
        } else if ( !options.setOutput() ) {
            error("No output file was specified");
            return TRANSFORM_ERROR_RC;
        } else if ( !options.validateFiles() ) {
            return TRANSFORM_ERROR_RC;
        }

        boolean loadedRules;
        try {
        	loadedRules = options.setRules();
        } catch ( Exception e ) {
            error("Exception loading rules: %s\n", e);
            return RULES_ERROR_RC;
        }
        if ( !loadedRules ) {
        	error("Transformation rules cannot be used");
        	return RULES_ERROR_RC;
        }
        if ( options.isVerbose ) {
        	options.logRules( getInfoStream() );
        }

        try {
        	options.transform(); // throws JakartaTransformException
        } catch ( JakartaTransformException e ) {
            error("Transform failure: %s\n", e);
            return TRANSFORM_ERROR_RC;
        } catch ( Throwable th) {
        	error("Unexpected failure: %s\n", th);
            return TRANSFORM_ERROR_RC;
        }

        return SUCCESS_RC;
    }
}
