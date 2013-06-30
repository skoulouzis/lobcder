/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import io.milton.http.Range;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.extern.java.Log;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WorkerServlet extends HttpServlet {

    private Client restClient;
    private String restURL;
    private final String username;
    private final String password;
    private Map<String, Long> weightPDRIMap;
    private long size;
//    private HttpClient client;
    private final String davURL;
    private int numOfTries = 0;
    private long numOfGets;

    public WorkerServlet() throws FileNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/auth.properties");

//        String propBasePath = File.separator + "test.proprties";
        Properties prop = Util.getTestProperties(in);

        restURL = prop.getProperty(("rest.url"), "http://localhost:8080/lobcder/rest/");
        davURL = prop.getProperty(("rest.url"), "http://localhost:8080/lobcder/dav/");
        username = prop.getProperty(("rest.username"), "user");
        password = prop.getProperty(("rest.password"), "pass");


        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        restClient = Client.create(clientConfig);
        restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(username, password));

        weightPDRIMap = new HashMap<String, Long>();
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String filePath = request.getPathInfo();
        if (filePath.length() > 1) {
            try {
                numOfGets++;
                long start = System.currentTimeMillis();
                OutputStream out = response.getOutputStream();
                PDRI pdri = getPDRI(filePath);
                String rangeStr = request.getHeader(Constants.RANGE_HEADER_NAME);
                if (rangeStr != null) {
                    Range range = Range.parse(rangeStr.split("=")[1]);
                    pdri.copyRange(range, out);
                    response.setStatus(206);
                } else {
                    trasfer(pdri, out, true);
                }
                long elapsed = System.currentTimeMillis() - start;
                long elapsedSec = elapsed / 1000;
                if (elapsedSec <= 0) {
                    elapsedSec = 1;
                }

                long speed = size / elapsedSec;
                Long oldSpeed = weightPDRIMap.get(pdri.getHost());
                if (oldSpeed == null) {
                    oldSpeed = speed;
                }
                long averagre = (speed + oldSpeed) / numOfGets;
                this.weightPDRIMap.put(pdri.getHost(), averagre);
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Average speed for  : {0} : " + averagre, pdri.getHost());
            } catch (URISyntaxException ex) {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "LOBCDER worker";
    }

    private PDRI getPDRI(String filePath) throws IOException, URISyntaxException {
        WebResource webResource = restClient.resource(restURL);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("path", filePath);
        WebResource res = webResource.path("items").path("query").queryParams(params);
        List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                get(new GenericType<List<LogicalDataWrapped>>() {
        });
//
        PDRIDesc pdriDesc = null;//new PDRIDesc();

        for (LogicalDataWrapped ld : list) {
            if (ld != null) {
                Set<PDRIDesc> pdris = ld.pdriList;
                size = ld.logicalData.length;
                if (pdris != null && !pdris.isEmpty()) {
                    pdriDesc = selectBestPDRI(pdris);
                }
            }
        }

//        pdriDesc = new PDRIDesc();
//        pdriDesc.encrypt = false;
//        pdriDesc.id = 2;
//        pdriDesc.name = "c4611769-c33e-4287-bee4-2de858a26783-tmp";
//        pdriDesc.password = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUlaakNDQ0JDZ0F3SUJBZ0lDRU5Rd0RRWUpLb1pJaHZjTkFRRUZCUUF3YmpFU01CQUdBMVVFQ2hNSlpIVjAKWTJobmNtbGtNUTR3REFZRFZRUUtFd1YxYzJWeWN6RU1NQW9HQTFVRUNoTURkWFpoTVEwd0N3WURWUVFMRXdSMwphVzV6TVJzd0dRWURWUVFERXhKVGNIbHlhV1J2YmlCTGIzVnNiM1Y2YVhNeERqQU1CZ05WQkFNVEJYQnliM2g1Ck1CNFhEVEV6TURZek1ERXhNVEl4TkZvWERURXpNRFl6TURJek1UWXpNVm93ZmpFU01CQUdBMVVFQ2hNSlpIVjAKWTJobmNtbGtNUTR3REFZRFZRUUtFd1YxYzJWeWN6RU1NQW9HQTFVRUNoTURkWFpoTVEwd0N3WURWUVFMRXdSMwphVzV6TVJzd0dRWURWUVFERXhKVGNIbHlhV1J2YmlCTGIzVnNiM1Y2YVhNeERqQU1CZ05WQkFNVEJYQnliM2g1Ck1RNHdEQVlEVlFRREV3VndjbTk0ZVRCY01BMEdDU3FHU0liM0RRRUJBUVVBQTBzQU1FZ0NRUURIQ0FoSmZYUnIKTHRBSmprRUIyb1E5cmlld3IxNE54S2Jsb1NqK3krVzUwNGJ4b0lzQzlVTkJCSU12WGNqWUNOc1JKS09ZaE8vMgowZXFSNnpSRjhpSHBBZ01CQUFHamdnYUdNSUlHZ2pDQ0JuRUdDaXNHQVFRQnZrVmtaQVVFZ2daaE1JSUdYVENDCkJsa3dnZ1pWTUlJRnZnSUJBVEJxb0dnd1lxUmdNRjR4RWpBUUJnTlZCQW9UQ1dSMWRHTm9aM0pwWkRFT01Bd0cKQTFVRUNoTUZkWE5sY25NeEREQUtCZ05WQkFvVEEzVjJZVEVOTUFzR0ExVUVDeE1FZDJsdWN6RWJNQmtHQTFVRQpBeE1TVTNCNWNtbGtiMjRnUzI5MWJHOTFlbWx6QWdJUTFLQm9NR2FrWkRCaU1SQXdEZ1lEVlFRS0V3ZEhVa2xFCkxVWlNNUXN3Q1FZRFZRUUdFd0pHVWpFTk1Bc0dBMVVFQ2hNRVEwNVNVekVSTUE4R0ExVUVDeE1JUTBNdFNVNHkKVURNeEh6QWRCZ05WQkFNVEZtTmpiR05uZG05dGMyeHBNREV1YVc0eWNETXVabkl3RFFZSktvWklodmNOQVFFRgpCUUFDRVFEOUorVlJyVmxDbnBhL1B3dGxtLy94TUNJWUR6SXdNVE13TmpNd01URXhOekUwV2hnUE1qQXhNekEyCk16QXlNekUzTVRSYU1HQXdYZ1lLS3dZQkJBRytSV1JrQkRGUU1FNmdKNFlsWW1sdmJXVmtPaTh2WTJOc1kyZDIKYjIxemJHa3dNUzVwYmpKd015NW1jam94TlRBd01EQWpCQ0V2WW1sdmJXVmtMMUp2YkdVOVRsVk1UQzlEWVhCaApZbWxzYVhSNVBVNVZURXd3Z2dRNU1JSUVDUVlLS3dZQkJBRytSV1JrQ2dTQ0Eva3dnZ1AxTUlJRDhUQ0NBKzB3CmdnTFZvQU1DQVFJQ0FoMDdNQTBHQ1NxR1NJYjNEUUVCQlFVQU1DOHhDekFKQmdOVkJBWVRBa1pTTVEwd0N3WUQKVlFRS0V3UkRUbEpUTVJFd0R3WURWUVFERXdoSFVrbEVNaTFHVWpBZUZ3MHhNakE1TVRNeE5UQXlORE5hRncweApNekE1TVRNeE5UQXlORE5hTUdJeEVEQU9CZ05WQkFvVEIwZFNTVVF0UmxJeEN6QUpCZ05WQkFZVEFrWlNNUTB3CkN3WURWUVFLRXdSRFRsSlRNUkV3RHdZRFZRUUxFd2hEUXkxSlRqSlFNekVmTUIwR0ExVUVBeE1XWTJOc1kyZDIKYjIxemJHa3dNUzVwYmpKd015NW1jakNCbnpBTkJna3Foa2lHOXcwQkFRRUZBQU9CalFBd2dZa0NnWUVBeWV5VQpiNGx3cmNnNUZuM0ZWTDJqNy9QdzNBcmR1QmthTE9rK2xVNmVkR1hTaXVkQktKbGVVNThEalc0NDZ5Y1BVNkRLCnkvVUQrWFpzUG5lcm5yclVjdWZaMTJndmkyNkN3R2hvcXd0a2F1VnQ1VUdYK2NMV3VLZXVzYWpvd0tHcFNXRXMKYWxKcFhTMmo0cTNtM1M2bWdCSnFsWWdWc2pwK1J2Q1RLMkdKVm1zQ0F3RUFBYU9DQVdJd2dnRmVNQXdHQTFVZApFd0VCL3dRQ01BQXdFUVlKWUlaSUFZYjRRZ0VCQkFRREFnYkFNQTRHQTFVZER3RUIvd1FFQXdJRCtEQXFCZ2xnCmhrZ0JodmhDQVEwRUhSWWJSMUpKUkRJdFJsSWdjMlZ5ZG1WeUlHTmxjblJwWm1sallYUmxNQjBHQTFVZERnUVcKQkJSYnNkZ3RTZW8xQTJiWXFqS0lCaFJsc0N2TXJEQmNCZ05WSFNNRVZUQlRnQlFubGtnbjdpRzI4cSt4TFgzNgo5OWRJSlhDVms2RTRwRFl3TkRFTE1Ba0dBMVVFQmhNQ1JsSXhEVEFMQmdOVkJBb1RCRU5PVWxNeEZqQVVCZ05WCkJBTVREVU5PVWxNeUxWQnliMnBsZEhPQ0FRTXdKd1lEVlIwZ0JDQXdIakFPQmd3ckJnRUVBZFE5QVFFSUFnRXcKREFZS0tvWklodmRNQlFJQ0FUQkJCZ05WSFI4RU9qQTRNRGFnTktBeWhqQm9kSFJ3T2k4dlkzSnNjeTV6WlhKMgphV05sY3k1amJuSnpMbVp5TDBkU1NVUXlMVVpTTDJkbGRHUmxjaTVqY213d0ZnWUlLd1lCQkFHN1lnRUVDblZ1CmFXTnZjbVZPU2xNd0RRWUpLb1pJaHZjTkFRRUZCUUFEZ2dFQkFCMUpjZXRJSEJTejlIOS9aRTlIQVNyODFUNHUKNXdvRHRuSUk1Uk9Sa2FzUytvaVVxUkJLNTFWRldtOXJxWkJ6djRjSDNnY1pIb2xDek8vWHFtQVhkaU5iY0ZOYgpiUnJXNU9EclB1NEw2MkE0UG1Ta1FQNC9QMUJsbUg1U1pVQXNFUDhMcFhoRi9HR3lobnBBYkp3Y2RyTldlaklPClBGMCtPcncrWlFmZEZsYjRhSWUyOEluS3UrbFdjNkh5bDRQNTZVRENiMmNsTGlIeWVsTTlFMXZvTWpDMlRyWDQKU1U4K01GR2dMeCtjVnZ5UEQwOVQ5aW16OXBxNzlROUtDdDFBUW83b00yUWEwMWp3NGlUY29ueVlOaURSbGloKwo1aUZRMEtpVmo5c295TU1xUEg1ZS9reCs4bElqVlhldEtJTFVmcUpTTktMMzRuY1BJdjduWFFlWkE4NHdDUVlEClZSMDRCQUlGQURBZkJnTlZIU01FR0RBV2dCUmJzZGd0U2VvMUEyYllxaktJQmhSbHNDdk1yREFOQmdrcWhraUcKOXcwQkFRVUZBQU9CZ1FCUXppdy8yQ1d0S0Y3N2N2MkorNDVEdmFPbVEzTXBkREFtZkdYWXVTaE9BL21zRzVHZQp4MGtRejRoS3B3djQ1L0p2d1BmVkgwWWFwSDV6SkdGZjh0RVdidXcweEtBNVZLck1qcXhMK004ZVJVTGJsVjN3Cjc3WEtvTTRHMTFwSkowczBnVFgrVm01cUtDbStNUDdYcTdERGo2aGN1dkRNM21odWlmYksxM21TM0RBTEJnTlYKSFE4RUJBTUNCTEF3RFFZSktvWklodmNOQVFFRkJRQURRUUNRUXp4TGV1bTZvcU91eXZmUGJZamgzWEZSQ1lCKwpjdjV6ZDRPaGY4MVVLYTJDdUF1VjJmUUdyU0pFZWo2ejZ6Mmo5MXZ2OXV1RXp0eHNJMHAwZExPQQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCi0tLS0tQkVHSU4gUlNBIFBSSVZBVEUgS0VZLS0tLS0KTUlJQk93SUJBQUpCQU1jSUNFbDlkR3N1MEFtT1FRSGFoRDJ1SjdDdlhnM0VwdVdoS1A3TDViblRodkdnaXdMMQpRMEVFZ3k5ZHlOZ0kyeEVrbzVpRTcvYlI2cEhyTkVYeUlla0NBd0VBQVFKQkFJUW03akcxcmlZNDJBTndRL095Ckh0cEJsN0wyVGhJQVAvejZqQmphVTdlVThuM2VpV0tTS3YyeHNxRzBiTUVyNUlPbWFzRmNudkUwdk9qQzY3Y2cKRHEwQ0lRRHhFejhFQktseVE3M0IrY1NRTURaa2pTcTBhamRZNnhtaTRvbk9uRUc1c3dJaEFOTmFjUzQ1UTY3RwpSQmw4Mi93dnI5Wll6UmpzR1dIYU9sRmNGdXZLTFMvekFpRUF4eTF5bkJ4Sjc1bGVkNXlvTmNWUHUyWVRDWHJVCmZjZklwNVpwNjJXWXZCY0NJR1hLelhBQnpJTkVvay9VUHU4NjJHbFg0NC81UmxCR2xvZlEzb2s1bW9IWkFpQTUKM09VbzhidFlWOVAyY2dwRjhsclJGbXJvd3l2S2ZoN0J6dGd6ZGNtMU9nPT0KLS0tLS1FTkQgUlNBIFBSSVZBVEUgS0VZLS0tLS0KLS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUIvVENDQVdhZ0F3SUJBZ0lDRU5Rd0RRWUpLb1pJaHZjTkFRRUZCUUF3WGpFU01CQUdBMVVFQ2hNSlpIVjAKWTJobmNtbGtNUTR3REFZRFZRUUtFd1YxYzJWeWN6RU1NQW9HQTFVRUNoTURkWFpoTVEwd0N3WURWUVFMRXdSMwphVzV6TVJzd0dRWURWUVFERXhKVGNIbHlhV1J2YmlCTGIzVnNiM1Y2YVhNd0hoY05NVE13TmpNd01URXhNVE15CldoY05NVE13TmpNd01qTXhOak15V2pCdU1SSXdFQVlEVlFRS0V3bGtkWFJqYUdkeWFXUXhEakFNQmdOVkJBb1QKQlhWelpYSnpNUXd3Q2dZRFZRUUtFd04xZG1FeERUQUxCZ05WQkFzVEJIZHBibk14R3pBWkJnTlZCQU1URWxOdwplWEpwWkc5dUlFdHZkV3h2ZFhwcGN6RU9NQXdHQTFVRUF4TUZjSEp2ZUhrd1hEQU5CZ2txaGtpRzl3MEJBUUVGCkFBTkxBREJJQWtFQW9adlY3cWZBa3JiUU1qRUhIaUNDR251cjRqWnNhU28yTFdrNHFKUVh3T0trKzR6SEtJc2EKWEJocDgxa1crR3hpK1poYTRZNW8vSUNYTzg4WTB6WXJSd0lEQVFBQk1BMEdDU3FHU0liM0RRRUJCUVVBQTRHQgpBSWRxZExDZmd2c2VqTVBHN2Q1dlBidXdrS3ZDQ05iTWgxdHBtS1NUbWZ2dC8zb29WdmxUZlBFUTBuY0QxVEMvClRtazhlSUhFUUxRNGhqZzlnenV2TTZaYzFZOVNzZG1aVTBTS2xLOVYxdW9uSittMFlrZzlaMGdOOXF3dUhhZEIKVzh6LzY2a25PZldRN2E1d3N4N29uSjFackpZcDlkMmdzaFovMmhjclY0aTMKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQotLS0tLUJFR0lOIENFUlRJRklDQVRFLS0tLS0KTUlJRWVUQ0NBMkdnQXdJQkFnSUNFTlF3RFFZSktvWklodmNOQVFFRkJRQXdVakVMTUFrR0ExVUVCaE1DVGt3eApEekFOQmdOVkJBb1RCazVKUzBoRlJqRXlNREFHQTFVRUF4TXBUa2xMU0VWR0lHMWxaR2wxYlMxelpXTjFjbWwwCmVTQmpaWEowYVdacFkyRjBhVzl1SUdGMWRHZ3dIaGNOTVRNd01UTXdNREF3TURBd1doY05NVFF3TVRNd01UVXoKTWpNM1dqQmVNUkl3RUFZRFZRUUtFd2xrZFhSamFHZHlhV1F4RGpBTUJnTlZCQW9UQlhWelpYSnpNUXd3Q2dZRApWUVFLRXdOMWRtRXhEVEFMQmdOVkJBc1RCSGRwYm5NeEd6QVpCZ05WQkFNVEVsTndlWEpwWkc5dUlFdHZkV3h2CmRYcHBjekNCbnpBTkJna3Foa2lHOXcwQkFRRUZBQU9CalFBd2dZa0NnWUVBbyt5SThQWkJOV2JrN3kxLzNQZ0oKWXoxWGtVQWpsWSt3VDlGZmZ5dU9pc3VsUDh6WGRPTTcwWE5KVm9laTIvaFVxT3ZiZjAzM1IxNVVmWCttZXNmbQo1enZmTHhBRDVhRW1EcjBQZ0U4bUlTS085c3FtK0tpYUJTQitHNHFnZGVIbUMxaXlYdHBBdkZvZW56Ykd2RVJtCnBlbEZaN2FzZjU1UHJ4VTZHaHp3bE1jQ0F3RUFBYU9DQWM4d2dnSExNQXdHQTFVZEV3RUIvd1FDTUFBd0RnWUQKVlIwUEFRSC9CQVFEQWdTd01CMEdBMVVkSlFRV01CUUdDQ3NHQVFVRkJ3TUNCZ2dyQmdFRkJRY0RCREE0QmdOVgpIUjhFTVRBdk1DMmdLNkFwaGlkb2RIUndPaTh2WTJFdVpIVjBZMmhuY21sa0xtNXNMMjFsWkdsMWJTOWpZV055CmJDNWtaWEl3S0FZRFZSMGdCQ0V3SHpBUEJnMHJCZ0VFQWRGQ0JBSUNBUU1DTUF3R0NpcUdTSWIzVEFVQ0FnRXcKSHdZRFZSMGpCQmd3Rm9BVVd3VTZtY2JWSXIzOWxJRDhFYWpROFhIV1M2UXdIUVlEVlIwT0JCWUVGUDBSbjB3Wgo1TzZDeUg0Nk9CS0RTZHZmazhPNk1CRUdDV0NHU0FHRytFSUJBUVFFQXdJRm9EQTBCZ2xnaGtnQmh2aENBUWdFCkp4WWxhSFIwY0RvdkwyTmhMbVIxZEdOb1ozSnBaQzV1YkM5dFpXUnBkVzB2Y0c5c2FXTjVMekNCbmdZSllJWkkKQVliNFFnRU5CSUdRRm9HTlJVVkRJR2x6YzNWbFpDQjFibVJsY2lCd2IyeHBZM2tnZG1WeWMybHZiaUF6TGpJZwpMU0JzYVcxcGRHVmtJR3hwWVdKcGJHbDBhV1Z6SUdGd2NHeDVMQ0J6WldVZ2FIUjBjRG92TDJOaExtUjFkR05vClozSnBaQzV1YkM5dFpXUnBkVzB2Y0c5c2FXTjVMeUF0SUVObGNuUnBabWxqWVhSbElGUmhaem9nTURBeU0yVTMKWVdNdFlUTmxZemc0TUEwR0NTcUdTSWIzRFFFQkJRVUFBNElCQVFCSUlVcXZ2MkpuV0ZuOGVYMXBkRTFNYmlVMgpNa3hYVkNuZWdTYlgxMTRwdkQySkdQTXNBUG1mZ2xscnM2SXhROXRkMUtwNnhYM3RIbE9KMUwzS3BlZ1J3SzRtClExbkl0WHlSYm1HVS85a3RYN0dWendOWUM5R3V5VHpxRjYxcG9iZmlvL3JTem1RU0JGOGVlM2VPSkN2VGRkOVMKd3RxbUs4ckdsSFFpTXR6ZDhMS1lnd2lYeW5FODk5a2FGSU9YYTF6amtuekFvMFM1c2Nra3Bwb2NYWHk3WHE3aAppMjM5WjVmNUFIL2FiSnZ4ZTIzSzZmYmtIODJzazZtWFY3enczNmNQOW1CcjkyVEpuZVk1L1gzUCsrZXZrTStKCnB3bStLOUNJOWJZMnpyNHdyOE1RazBrYnJzNHd4cVE4ZlVyUEVkVTdzUHZYTStQZVFjU29HYU94NC9rNAotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==";//"-----BEGIN CERTIFICATE-----\nMIIIZjCCCBCgAwIBAgICENQwDQYJKoZIhvcNAQEFBQAwbjESMBAGA1UEChMJZHV0\nY2hncmlkMQ4wDAYDVQQKEwV1c2VyczEMMAoGA1UEChMDdXZhMQ0wCwYDVQQLEwR3\naW5zMRswGQYDVQQDExJTcHlyaWRvbiBLb3Vsb3V6aXMxDjAMBgNVBAMTBXByb3h5\nMB4XDTEzMDYzMDA5NTAwN1oXDTEzMDYzMDIxNTQxMVowfjESMBAGA1UEChMJZHV0\nY2hncmlkMQ4wDAYDVQQKEwV1c2VyczEMMAoGA1UEChMDdXZhMQ0wCwYDVQQLEwR3\naW5zMRswGQYDVQQDExJTcHlyaWRvbiBLb3Vsb3V6aXMxDjAMBgNVBAMTBXByb3h5\nMQ4wDAYDVQQDEwVwcm94eTBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQDE5ee9I6YI\np7YXadlE9vaKq3i/oEFPXqXCzlBYGY2uI/QJDi0RUT2C4vYb3PT9V3bgJvP8WxbT\nBfTXzEpNF/5dAgMBAAGjggaGMIIGgjCCBnEGCisGAQQBvkVkZAUEggZhMIIGXTCC\nBlkwggZVMIIFvgIBATBqoGgwYqRgMF4xEjAQBgNVBAoTCWR1dGNoZ3JpZDEOMAwG\nA1UEChMFdXNlcnMxDDAKBgNVBAoTA3V2YTENMAsGA1UECxMEd2luczEbMBkGA1UE\nAxMSU3B5cmlkb24gS291bG91emlzAgIQ1KBoMGakZDBiMRAwDgYDVQQKEwdHUklE\nLUZSMQswCQYDVQQGEwJGUjENMAsGA1UEChMEQ05SUzERMA8GA1UECxMIQ0MtSU4y\nUDMxHzAdBgNVBAMTFmNjbGNndm9tc2xpMDEuaW4ycDMuZnIwDQYJKoZIhvcNAQEF\nBQACEQDfzCsySt1B7JUuUh6RDxi8MCIYDzIwMTMwNjMwMDk1NTA2WhgPMjAxMzA2\nMzAyMTU1MDZaMGAwXgYKKwYBBAG+RWRkBDFQME6gJ4YlYmlvbWVkOi8vY2NsY2d2\nb21zbGkwMS5pbjJwMy5mcjoxNTAwMDAjBCEvYmlvbWVkL1JvbGU9TlVMTC9DYXBh\nYmlsaXR5PU5VTEwwggQ5MIIECQYKKwYBBAG+RWRkCgSCA/kwggP1MIID8TCCA+0w\nggLVoAMCAQICAh07MA0GCSqGSIb3DQEBBQUAMC8xCzAJBgNVBAYTAkZSMQ0wCwYD\nVQQKEwRDTlJTMREwDwYDVQQDEwhHUklEMi1GUjAeFw0xMjA5MTMxNTAyNDNaFw0x\nMzA5MTMxNTAyNDNaMGIxEDAOBgNVBAoTB0dSSUQtRlIxCzAJBgNVBAYTAkZSMQ0w\nCwYDVQQKEwRDTlJTMREwDwYDVQQLEwhDQy1JTjJQMzEfMB0GA1UEAxMWY2NsY2d2\nb21zbGkwMS5pbjJwMy5mcjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAyeyU\nb4lwrcg5Fn3FVL2j7/Pw3ArduBkaLOk+lU6edGXSiudBKJleU58DjW446ycPU6DK\ny/UD+XZsPnernrrUcufZ12gvi26CwGhoqwtkauVt5UGX+cLWuKeusajowKGpSWEs\nalJpXS2j4q3m3S6mgBJqlYgVsjp+RvCTK2GJVmsCAwEAAaOCAWIwggFeMAwGA1Ud\nEwEB/wQCMAAwEQYJYIZIAYb4QgEBBAQDAgbAMA4GA1UdDwEB/wQEAwID+DAqBglg\nhkgBhvhCAQ0EHRYbR1JJRDItRlIgc2VydmVyIGNlcnRpZmljYXRlMB0GA1UdDgQW\nBBRbsdgtSeo1A2bYqjKIBhRlsCvMrDBcBgNVHSMEVTBTgBQnlkgn7iG28q+xLX36\n99dIJXCVk6E4pDYwNDELMAkGA1UEBhMCRlIxDTALBgNVBAoTBENOUlMxFjAUBgNV\nBAMTDUNOUlMyLVByb2pldHOCAQMwJwYDVR0gBCAwHjAOBgwrBgEEAdQ9AQEIAgEw\nDAYKKoZIhvdMBQICATBBBgNVHR8EOjA4MDagNKAyhjBodHRwOi8vY3Jscy5zZXJ2\naWNlcy5jbnJzLmZyL0dSSUQyLUZSL2dldGRlci5jcmwwFgYIKwYBBAG7YgEECnVu\naWNvcmVOSlMwDQYJKoZIhvcNAQEFBQADggEBAB1JcetIHBSz9H9/ZE9HASr81T4u\n5woDtnII5RORkasS+oiUqRBK51VFWm9rqZBzv4cH3gcZHolCzO/XqmAXdiNbcFNb\nbRrW5ODrPu4L62A4PmSkQP4/P1BlmH5SZUAsEP8LpXhF/GGyhnpAbJwcdrNWejIO\nPF0+Orw+ZQfdFlb4aIe28InKu+lWc6Hyl4P56UDCb2clLiHyelM9E1voMjC2TrX4\nSU8+MFGgLx+cVvyPD09T9imz9pq79Q9KCt1AQo7oM2Qa01jw4iTconyYNiDRlih+\n5iFQ0KiVj9soyMMqPH5e/kx+8lIjVXetKILUfqJSNKL34ncPIv7nXQeZA84wCQYD\nVR04BAIFADAfBgNVHSMEGDAWgBRbsdgtSeo1A2bYqjKIBhRlsCvMrDANBgkqhkiG\n9w0BAQUFAAOBgQCHGcyrkzYhtiLaae1gNOQNSg6BUKPdIVWRun6j6p4ldNoHv3rB\nj3/0WfSJzxO+K519Krhlu8Lxm8/+/AtGf6yxn59A89lL3dbe2dFecbZUa7zK9xLA\nezoSyTrhD0NXepel2MGvmBsyi4zgUJgCM9BG5b5otGnilgHuZ22Mb4ZhZzALBgNV\nHQ8EBAMCBLAwDQYJKoZIhvcNAQEFBQADQQC9aykxFqx5ObNUbb5vJ0OFax22cOlt\nzn0beIHDOSfCeM5IUudrkgGkuZxn5EbfZIOxOzthAYhBU+qnTAU51URW\n-----END CERTIFICATE-----\n-----BEGIN RSA PRIVATE KEY-----\nMIIBPAIBAAJBAMTl570jpginthdp2UT29oqreL+gQU9epcLOUFgZja4j9AkOLRFR\nPYLi9hvc9P1XduAm8/xbFtMF9NfMSk0X/l0CAwEAAQJBALcdLTCsd5wzBNivev11\nRAHgyHJGQpbi6fvKzdOxKB9a8snKho/+dRpdloF4hqKWjh+jLRmnhN0OVpo+PTbU\nfu0CIQD4GWkoc+uAUhQ3732cSW9q8Ve1FIPXHSS0GxZvOLE7gwIhAMsrFcFHWfLV\nI4iT/wsJKGMR8GUjCUrfTT8a4mwrBVifAiEAtyUy/pzL/VcZ+8y8QjHnoN92KoNx\na8vku1u/rO0B0HcCIQCJ5lWe2dJIy7iRpHpxQCcEj5GO5CgThNvGoHXdG5pGWwIg\nCrWd67G7ZUcaV0dXBywYnYTHjgjdgqf0c+w3NdSaBn8=\n-----END RSA PRIVATE KEY-----\n-----BEGIN CERTIFICATE-----\nMIIB/TCCAWagAwIBAgICENQwDQYJKoZIhvcNAQEFBQAwXjESMBAGA1UEChMJZHV0\nY2hncmlkMQ4wDAYDVQQKEwV1c2VyczEMMAoGA1UEChMDdXZhMQ0wCwYDVQQLEwR3\naW5zMRswGQYDVQQDExJTcHlyaWRvbiBLb3Vsb3V6aXMwHhcNMTMwNjMwMDk0OTEx\nWhcNMTMwNjMwMjE1NDExWjBuMRIwEAYDVQQKEwlkdXRjaGdyaWQxDjAMBgNVBAoT\nBXVzZXJzMQwwCgYDVQQKEwN1dmExDTALBgNVBAsTBHdpbnMxGzAZBgNVBAMTElNw\neXJpZG9uIEtvdWxvdXppczEOMAwGA1UEAxMFcHJveHkwXDANBgkqhkiG9w0BAQEF\nAANLADBIAkEAwZ+VH+lD+PL74HNmP+oO7XwJQlEdQ+sWZ8goe+aVWWrdtc/Ih2tH\nZ7Af2c3CXFINx0v29n4sTff/IfNLVLWLiwIDAQABMA0GCSqGSIb3DQEBBQUAA4GB\nAFcFnxCJckK4Gitpr1MwPAkaezKpSmhcsBcGiRI9bY2lMq7HV7c3/wzc14AX+Jlo\nnm3XvHlvRPW7h0cVwxqUifTwomDGZF8Tb6nEWUC7QiwoHZMo8mR8aHUftQYNDAeh\n+3GARmt7VQNM/U1CRG6AXIhU1kC/NkVJ0/f7GO9lLf7L\n-----END CERTIFICATE-----\n-----BEGIN CERTIFICATE-----\nMIIEeTCCA2GgAwIBAgICENQwDQYJKoZIhvcNAQEFBQAwUjELMAkGA1UEBhMCTkwx\nDzANBgNVBAoTBk5JS0hFRjEyMDAGA1UEAxMpTklLSEVGIG1lZGl1bS1zZWN1cml0\neSBjZXJ0aWZpY2F0aW9uIGF1dGgwHhcNMTMwMTMwMDAwMDAwWhcNMTQwMTMwMTUz\nMjM3WjBeMRIwEAYDVQQKEwlkdXRjaGdyaWQxDjAMBgNVBAoTBXVzZXJzMQwwCgYD\nVQQKEwN1dmExDTALBgNVBAsTBHdpbnMxGzAZBgNVBAMTElNweXJpZG9uIEtvdWxv\ndXppczCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAo+yI8PZBNWbk7y1/3PgJ\nYz1XkUAjlY+wT9FffyuOisulP8zXdOM70XNJVoei2/hUqOvbf033R15UfX+mesfm\n5zvfLxAD5aEmDr0PgE8mISKO9sqm+KiaBSB+G4qgdeHmC1iyXtpAvFoenzbGvERm\npelFZ7asf55PrxU6GhzwlMcCAwEAAaOCAc8wggHLMAwGA1UdEwEB/wQCMAAwDgYD\nVR0PAQH/BAQDAgSwMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDBDA4BgNV\nHR8EMTAvMC2gK6AphidodHRwOi8vY2EuZHV0Y2hncmlkLm5sL21lZGl1bS9jYWNy\nbC5kZXIwKAYDVR0gBCEwHzAPBg0rBgEEAdFCBAICAQMCMAwGCiqGSIb3TAUCAgEw\nHwYDVR0jBBgwFoAUWwU6mcbVIr39lID8EajQ8XHWS6QwHQYDVR0OBBYEFP0Rn0wZ\n5O6CyH46OBKDSdvfk8O6MBEGCWCGSAGG+EIBAQQEAwIFoDA0BglghkgBhvhCAQgE\nJxYlaHR0cDovL2NhLmR1dGNoZ3JpZC5ubC9tZWRpdW0vcG9saWN5LzCBngYJYIZI\nAYb4QgENBIGQFoGNRUVDIGlzc3VlZCB1bmRlciBwb2xpY3kgdmVyc2lvbiAzLjIg\nLSBsaW1pdGVkIGxpYWJpbGl0aWVzIGFwcGx5LCBzZWUgaHR0cDovL2NhLmR1dGNo\nZ3JpZC5ubC9tZWRpdW0vcG9saWN5LyAtIENlcnRpZmljYXRlIFRhZzogMDAyM2U3\nYWMtYTNlYzg4MA0GCSqGSIb3DQEBBQUAA4IBAQBIIUqvv2JnWFn8eX1pdE1MbiU2\nMkxXVCnegSbX114pvD2JGPMsAPmfgllrs6IxQ9td1Kp6xX3tHlOJ1L3KpegRwK4m\nQ1nItXyRbmGU/9ktX7GVzwNYC9GuyTzqF61pobfio/rSzmQSBF8ee3eOJCvTdd9S\nwtqmK8rGlHQiMtzd8LKYgwiXynE899kaFIOXa1zjknzAo0S5sckkppocXXy7Xq7h\ni239Z5f5AH/abJvxe23K6fbkH82sk6mXV7zw36cP9mBr92TJneY5/X3P++evkM+J\npwm+K9CI9bY2zr4wr8MQk0kbrs4wxqQ8fUrPEdU7sPvXM+PeQcSoGaOx4/k4\n-----END CERTIFICATE-----";
//        pdriDesc.resourceUrl = "srm://tbn18.nikhef.nl:8446/dpm/nikhef.nl/home/biomed/skoulouz";
//        pdriDesc.username = "biomed";
        return new WorkerVPDRI(pdriDesc.name, pdriDesc.id, pdriDesc.resourceUrl, pdriDesc.username, pdriDesc.password, pdriDesc.encrypt, BigInteger.ZERO, false);
    }

    private void trasfer(PDRI pdri, OutputStream out, boolean withCircularStream) throws IOException {
        InputStream in = null;
        try {
            in = pdri.getData();
            if (!pdri.getEncrypted() && withCircularStream) {
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, out);
                cBuff.startTransfer(new Long(-1));
            } else if (!pdri.getEncrypted() && !withCircularStream) {
                int read;
                byte[] copyBuffer;
                if (pdri.getLength() < Constants.BUF_SIZE) {
                    copyBuffer = new byte[(int) pdri.getLength()];
                } else {
                    copyBuffer = new byte[Constants.BUF_SIZE];
                }

                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                }
            }
            numOfTries = 0;
        } catch (Exception ex) {
//            if (numOfTries < Constants.RECONNECT_NTRY) {
//                numOfTries++;
//                trasfer(pdri, out, false);
//            } else {
//                throw new IOException(ex.getMessage());
//            }
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.flush();
            out.close();
            if (in != null) {
                in.close();
            }
        }
    }

    private PDRIDesc selectBestPDRI(Set<PDRIDesc> pdris) throws URISyntaxException {
        if (weightPDRIMap.isEmpty() || weightPDRIMap.size() < pdris.size()) {
            //Just return one at random;
            int index = new Random().nextInt(pdris.size());
            PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
            return array[index];
        }

        long sumOfSpeed = 0;
        for (PDRIDesc p : pdris) {
            Long speed = weightPDRIMap.get(new URI(p.resourceUrl).getHost());
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Speed: : {0}", speed);
            sumOfSpeed += speed;
        }
        int itemIndex = new Random().nextInt((int) sumOfSpeed);

        for (PDRIDesc p : pdris) {
            Long speed = weightPDRIMap.get(new URI(p.resourceUrl).getHost());
            if (itemIndex < speed) {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Returning : {0}", p.resourceUrl);
                return p;
            }
            itemIndex -= speed;
        }
//        long sum = 0;
//        int i = 0;
//
//        while (sum < itemIndex) {
//            i++;
//            winner = pdris.iterator().next();
////            sum = sum + weightPDRIMap.get(new URI(winner.resourceUrl).getHost());
//            sum = sum + weightPDRIMap.get(new URI(winner.resourceUrl).getHost());
//        }
//        PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
//        int index;
//        if (i > 0) {
//            index = i - 1;
//        } else {
//            index = i;
//        }

        return null;
    }

    @XmlRootElement
    public static class PDRIDesc {

        public boolean encrypt;
        public long id;
        public String name;
        public String password;
        public String resourceUrl;
        public String username;
    }

    @XmlRootElement
    public static class LogicalDataWrapped {

        public LogicalData logicalData;
        public String path;
        public Set<PDRIDesc> pdriList;
        public Set<Permissions> permissions;
    }

    @XmlRootElement
    public static class LogicalData {

        public int checksum;
        public String contentTypesAsString;
        public long createDate;
        public long lastValidationDate;
        public long length;
        public int lockTimeout;
        public long modifiedDate;
        public String name;
        public String owner;
        public int parentRef;
        public int pdriGroupId;
        public boolean supervised;
        public String type;
        public int uid;
    }

    @XmlRootElement
    public static class Permissions {

        public String owner;
        public Set<String> read;
        public Set<String> write;
    }
}
