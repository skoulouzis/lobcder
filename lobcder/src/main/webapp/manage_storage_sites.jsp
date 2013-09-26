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
                    {   key: 'select',
                        allowHTML:  true, // to avoid HTML escaping
                        label:      '<input type="checkbox" class="protocol-select-all" title="Toggle ALL records"/>',
                        formatter:      '<input type="checkbox" checked/>',
                        emptyCellValue: '<input type="checkbox"/>'
                    },
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
                
                table.detach('*:change');
                
                // Define a listener on the DT first column for each record's "checkbox",
                //   to set the value of `select` to the checkbox setting
                table.delegate("click", function(e){
                    // undefined to trigger the emptyCellValue
                    var checked = e.target.get('checked') || undefined;
                    
                    this.getRecord(e.target).set('select', checked);

                    // Uncheck the header checkbox
                    this.get('contentBox')
                    .one('.protocol-select-all').set('checked', false);
                }, ".yui3-datatable-data .yui3-datatable-col-select input", table);


                // Also define a listener on the single TH "checkbox" to
                //   toggle all of the checkboxes
                table.delegate('click', function (e) {
                    // undefined to trigger the emptyCellValue
                    var checked = e.target.get('checked') || undefined;

                    // Set the selected attribute in all records in the ModelList silently
                    // to avoid each update triggering a table update
                    this.data.invoke('set', 'select', checked, { silent: true });

                    // Update the table now that all records have been updated
                    this.syncUI();
                }, '.protocol-select-all', table);
                //----------------
                //   "checkbox" Click listeners ...
                //----------------
                
                
                table.qe.start();

                
                // Make another request later
                //table.datasource.load({request:"zip=94089&query=pizza"});
            });
        </script>
    </body>
</html>
