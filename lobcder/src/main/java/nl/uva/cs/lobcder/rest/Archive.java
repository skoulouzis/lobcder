/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 *
 * @author S. Koulouzis
 */
@Log
@Path("compress/")
public class Archive extends CatalogueHelper {

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
    @Context
    UriInfo info;

    @Path("get_zip/")
    @GET
    @Produces({"application/zip"})
    public StreamingOutput getZip() throws Exception {

        return new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException, UnsupportedEncodingException {
                try {
                    Map<String, List<PDRIDescr>> paths = getPaths();
                    PDRIDescr p = paths.entrySet().iterator().next().getValue().get(0);
                    try (ZipOutputStream zip = new ZipOutputStream(out)) {
                        ZipEntry ze = new ZipEntry(p.getName());
                        zip.putNextEntry(ze);
                        PDRI pdri = PDRIFactory.getFactory().createInstance(p, false);
                        InputStream in = pdri.getData();
                        byte[] copyBuffer = new byte[Constants.BUF_SIZE];
                        int len;
                        while ((len = in.read(copyBuffer)) > 0) {
                            zip.write(copyBuffer, 0, len);
                        }

                        in.close();
                        zip.closeEntry();
                    }
                } catch (SQLException ex) {
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    private Map<String, List<PDRIDescr>> getPaths() throws SQLException, UnsupportedEncodingException, IOException {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        Map<String, List<PDRIDescr>> pdrisMap = new HashMap<>();
        if (queryParameters.containsKey("path")) {
            Iterator<String> it = queryParameters.get("path").iterator();
            while (it.hasNext()) {
                String path = it.next();
                LogicalData resLD = getCatalogue().getLogicalDataByPath(io.milton.common.Path.path(path));
                if (resLD == null) {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                Permissions p = getCatalogue().getPermissions(resLD.getUid(), resLD.getOwner());
                if (!mp.canRead(p)) {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
                List<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(resLD.getPdriGroupId());
                pdrisMap.put(path, pdris);
            }
        }
        return pdrisMap;
    }
}
