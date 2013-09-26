<%-- 
    Document   : manage_storage_sites
    Created on : Sep 26, 2013, 4:00:48 PM
    Author     : alogo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body class="yui3-skin-sam">
        <script src="http://yui.yahooapis.com/3.12.0/build/yui/yui-min.js"></script>

        <div id="storageSite"></div>

        <script>
            YUI().use("datasource-io", "datasource-xmlschema", "datatable-datasource", function(Y) {
                var dataSource = new Y.DataSource.IO({
                    source: "http://localhost:8080/lobcder/rest/storage_sites/"
                });

                dataSource.plug(Y.Plugin.DataSourceXMLSchema, {
                    schema: {
                        resultListLocator: "storageSiteWrapper",
                        resultFields: [
                            {key:"resourceURI", locator:"*[local-name() ='resourceURI']"},
                            {key:"password", locator:"*[local-name()='credential']/*[local-name()='storageSitePassword']"},
                            {key:"username", locator:"*[local-name()='credential']/*[local-name()='storageSiteUsername']"},
                            {key:"currentNum", locator:"*[local-name() ='currentNum']"},
                            {key:"encrypted", locator:"*[local-name() ='encrypt']"},
                            {key:"quotaNum", locator:"*[local-name() ='quotaNum']"},
                            {key:"ID", locator:"*[local-name() ='storageSiteId']"},
                        ]
                    }
                });
                


                var table = new Y.DataTable({
                    columns: ["ID","resourceURI", "username", "password","encrypted","currentNum","quotaNum","currentNum"],
                    summary: "Aveilable Storage Sites",
                    caption: "Aveilable Storage Sites"
                });
    
                table.plug(Y.Plugin.DataTableDataSource, {
                    datasource: dataSource,
                    initialRequest: "query?id=all&output=xml"
                });

                dataSource.after("response", function() {
                    table.render("#storageSite")}
            );

                // Make another request later
                //table.datasource.load({request:"zip=94089&query=pizza"});
            });
        </script>
    </body>
</html>
