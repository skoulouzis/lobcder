package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.Response;

import java.io.OutputStream;

public class ByteArrayEntity implements Response.Entity{

    private byte[] arr;

    public ByteArrayEntity(byte[] arr) {
        this.arr = arr;
    }

    public byte[] getArr() {
        return arr;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {
        outputStream.write(arr);
    }
}
