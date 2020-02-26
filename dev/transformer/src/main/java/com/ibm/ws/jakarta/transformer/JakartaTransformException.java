package com.ibm.ws.jakarta.transformer;

import java.io.IOException;

public class JakartaTransformException extends Exception {
	private static final long serialVersionUID = 1L;

	public JakartaTransformException() {
		super();
	}
	
	public JakartaTransformException(String message) {
		super(message);
	}
	
	public JakartaTransformException(String message, Throwable cause) {
		super(message, cause);
	}

    public JakartaTransformException(Exception e) {
        super(e.getMessage(), e.getCause());
    }
}
