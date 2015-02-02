package com.bradmcevoy.io;

import java.io.IOException;

public class ReadingException extends IOException{
    
    private static final long serialVersionUID = 1L;

    public ReadingException(IOException cause) {
        super(cause.getMessage());
    }
}
