package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.HrefStatus;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.webdav.WebDavProtocol;

import java.io.OutputStream;
import java.util.List;

public class MultiStatusEntity implements Response.Entity {

    private List<HrefStatus> statii;

    public MultiStatusEntity(List<HrefStatus> statii) {
        this.statii = statii;
    }

    public List<HrefStatus> getStatii() {
        return statii;
    }

    @Override
    public void write(Response response, OutputStream outputStream) throws Exception {

        XmlWriter writer = new XmlWriter( response.getOutputStream() );
        writer.writeXMLHeader();
        writer.open( "multistatus xmlns:D" + "=\"" + WebDavProtocol.NS_DAV + ":\"" ); // only single namespace for this method
        writer.newLine();
        for( HrefStatus status : getStatii()) {
            XmlWriter.Element elResponse = writer.begin( "response" ).open();
            writer.writeProperty( "", "href", status.href );
            writer.writeProperty( "", "status", status.status.code + "" );
            elResponse.close();
        }
        writer.close( "multistatus" );
        writer.flush();
    }
}
