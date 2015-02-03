package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.Response;

import java.io.OutputStream;
import java.io.PrintWriter;

public class StringEntity implements Response.Entity{

    private String string;

    public StringEntity(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {
        PrintWriter pw = new PrintWriter(outputStream, true);
        pw.print(string);
        pw.flush();
    }
}
