package com.bradmcevoy.io;

import java.io.IOException;

public class WritingException extends IOException{
    
    private static final long serialVersionUID = 1L;

    public WritingException(IOException cause) {
        super(cause.getMessage());
    }
}
