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
        <script src="http://yui.yahooapis.com/3.8.0/build/yui/yui-min.js"></script>

        <!--<div id="storageSite"></div>-->

        <script type="text/javascript">
            YUI().use(
            "datasource-io", 
            "datasource-xmlschema", 
            "datatable-datasource", 
            "gallery-quickedit",
            
             
            function(Y) {
                var ds = new Y.DataSource.IO({
                    source: "http://localhost:8080/lobcder/rest/storage_sites/"
                });

                ds.plug(Y.Plugin.DataSourceXMLSchema, {
                    schema: {
                        resultListLocator: "storageSiteWrapper",
                        resultFields: [
                            {key:"resourceURI", locator:"*[local-name() ='resourceURI']",quickEdit: true},
                            {key:"password", locator:"*[local-name()='credential']/*[local-name()='storageSitePassword']",quickEdit: true},
                            {key:"username", locator:"*[local-name()='credential']/*[local-name()='storageSiteUsername']",quickEdit: true},
                            {key:"currentNum", locator:"*[local-name() ='currentNum']",quickEdit: true},
                            {key:"encrypted", locator:"*[local-name() ='encrypt']",quickEdit: true},
                            {key:"quotaNum", locator:"*[local-name() ='quotaNum']",quickEdit: true},
                            {key:"ID", locator:"*[local-name() ='storageSiteId']",quickEdit: true},
                        ]
                    }
                });
                  
                var cols =
                    [
                    { key: 'ID', label: 'ID'},
                    { key: 'resourceURI', label: 'URI',quickEdit:true },
                    { key: 'username', label: 'username', quickEdit:true},
                    { key: 'password', label: 'password', quickEdit:true},
                    { key: 'encrypted', label: 'encrypted', quickEdit:true},
                    { key: 'currentNum', label: 'currentNum', quickEdit:true},
                    { key: 'quotaNum', label: 'quotaNum', quickEdit:true},
                ];
                
                var table = new Y.DataTable({columns: cols});
    
                table.plug(Y.Plugin.DataTableDataSource, {
                    datasource: ds,
                    initialRequest: "query?id=all&output=xml"
                });
                table.plug(Y.Plugin.DataTableQuickEdit);
                
                
                ds.after("response", function() {
                    table.render("#storageSite")
                });
                
                table.qe.start();

                
                // Make another request later
                //table.datasource.load({request:"zip=94089&query=pizza"});
            });
        </script>
    </body>
</html>
