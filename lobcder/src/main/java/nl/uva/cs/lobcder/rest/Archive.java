/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 *
 * @author S. Koulouzis, D. Vasyunin
 */
@Log
@Path("compress/")
public class Archive extends CatalogueHelper {

    @Context
    HttpServletRequest request;

    @GET
    @Path("/getzip/{name:.+}")
    public Response getZip(@PathParam("name") String path)  {
        @AllArgsConstructor
        class Folder {
            String path;
            LogicalData logicalData;
        }

        final String rootPath;
        if(path.endsWith("/")) {
            rootPath = path.substring(0, path.length() - 1);
        } else {
            rootPath = path;
        }
        int index = rootPath.lastIndexOf('/');
        final String rootName;
        if(index != -1) {
            rootName = rootPath.substring(index + 1);
        } else {
            rootName = rootPath;
        }
        if(rootName.isEmpty()){
            throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE);
        }
        final MyPrincipal principal = (MyPrincipal) request.getAttribute("myprincipal");
        final JDBCatalogue catalogue = getCatalogue();

        StreamingOutput result = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                try (Connection connection = catalogue.getConnection()) {
                    LogicalData rootElement = catalogue.getLogicalDataByPath(io.milton.common.Path.path(rootPath), connection);
                    if (rootElement == null) {
                        throw new WebApplicationException(404);
                    }
                    try (ZipOutputStream zip = new ZipOutputStream(out)) {
                        ZipEntry ze;
                        Stack<Folder> folders = new Stack<>();
                        Permissions p = catalogue.getPermissions(rootElement.getUid(), rootElement.getOwner(), connection);
                        if (principal.canRead(p)) {
                            if(rootElement.isFolder()) {
                                folders.add(new Folder("/" + rootName, rootElement));
                            } else {
                                ze = new ZipEntry("/" + rootName);
                                zip.putNextEntry(ze);
                                copyStream(catalogue.getPdriDescrByGroupId(rootElement.getPdriGroupId()), zip);
                                zip.closeEntry();
                            }
                        }

                        while (!folders.isEmpty()) {
                            Folder folder = folders.pop();
                            ze = new ZipEntry(folder.path + "/");
                            ze.setTime(folder.logicalData.getModifiedDate());
                            zip.putNextEntry(ze);
                            for (LogicalData ld : catalogue.getChildrenByParentRef(folder.logicalData.getUid(), connection)) {
                                Permissions entry_p = catalogue.getPermissions(ld.getUid(), ld.getOwner(), connection);
                                if (principal.canRead(entry_p)) {
                                    if (ld.isFolder()) {
                                        folders.push(new Folder(folder.path + "/" + ld.getName(), ld));
                                    } else {
                                        ze = new ZipEntry(folder.path + "/" + ld.getName());
                                        ze.setTime(ld.getModifiedDate());
                                        zip.putNextEntry(ze);
                                        copyStream(catalogue.getPdriDescrByGroupId(ld.getPdriGroupId()), zip);
                                        zip.closeEntry();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if(e instanceof WebApplicationException){
                        throw (WebApplicationException)e;
                    } else {
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        };
        Response.ResponseBuilder response = Response.ok(result, "application/zip" );
        return response.header("Content-Disposition", "attachment; filename=" + rootName + ".zip").build();
    }

    private void copyStream(List<PDRIDescr> pdriDescrList, OutputStream os) {
        for (PDRIDescr pdriDescr : pdriDescrList) {
            try {
                PDRI pdri = PDRIFactory.getFactory().createInstance(pdriDescr, false);
                InputStream in = pdri.getData();
                byte[] copyBuffer = new byte[Constants.BUF_SIZE];
                int len;
                while ((len = in.read(copyBuffer)) != -1) {
                    os.write(copyBuffer, 0, len);
                }
                in.close();
                return;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

}
