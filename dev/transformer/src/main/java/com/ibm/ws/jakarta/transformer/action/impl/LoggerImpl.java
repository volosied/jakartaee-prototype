package com.ibm.ws.jakarta.transformer.action.impl;

import java.io.PrintStream;

public class LoggerImpl {
	public static final PrintStream NULL_STREAM = null;

	public static final boolean IS_TERSE = true;
	public static final boolean IS_VERBOSE = true;

	public static LoggerImpl createStandardLogger() {
		return new LoggerImpl(NULL_STREAM, !IS_TERSE, !IS_VERBOSE);
	}

	public LoggerImpl(PrintStream logStream, boolean isTerse, boolean isVerbose) {
		this.logStream = logStream;
		this.isTerse = isTerse;
		this.isVerbose = isVerbose;
	}

	//

	private final PrintStream logStream;
	private final boolean isTerse;
	private final boolean isVerbose;

	public PrintStream getLogStream() {
		return logStream;
	}

	public boolean getIsTerse() {
		return isTerse;
	}

	public boolean getIsVerbose() {
		return isVerbose;
	}

	public void log(String text, Object... parms) {
		if ( (logStream != null) && !isTerse ) {
			if ( parms.length == 0 ) {
				logStream.println(text);
			} else {
				logStream.printf(text, parms);
			}
		}
	}

	public void verbose(String text, Object... parms) {
		if ( (logStream != null) && isVerbose ) {
			if ( parms.length == 0 ) {
				logStream.print(text);
			} else {
				logStream.printf(text, parms);
			}
		}
	}

    public void error(String message, Object... parms) {
   		if ( logStream != null ) {
   			if ( parms.length == 0 ) {
   				logStream.print("ERROR: " + message);
   			} else {
   				logStream.printf("ERROR: " + message, parms);
   			}
   		}
    }

    public void error(String message, Throwable th, Object... parms) {
   		error(message, parms);
   		th.printStackTrace( getLogStream() );
    }
}
