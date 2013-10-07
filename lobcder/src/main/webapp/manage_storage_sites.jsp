<%-- 
    Document   : manage_storage_sites
    Created on : Oct 6, 2013, 4:51:01 PM
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
        <div id="dtable"></div>

        <div class="ba_content">
            <button id="btnSelected" class="yui3-button">Save Selections</button>
        </div>
        <script src="http://yui.yahooapis.com/3.10.3/build/yui/yui-min.js"></script>

        <script type="text/javascript">
            YUI().use( 
            "datatable", 
            "datatype",  
            "datasource-io", 
            "datasource-xmlschema", 
            "datatable-datasource", 
            "datatable-scroll", 
            'gallery-datatable-editable', 
            'gallery-datatable-celleditor-popup',
            "gallery-datatable-checkbox-select",
            "datatable-scroll", "datatable-sort",  "datatable-mutable", "event-custom",
            "datatype", "cssfonts", "cssbutton",
            'gallery-datatable-paginator','gallery-paginator-view',
            function (Y) {
               
               
                var ds = new Y.DataSource.IO({
                    source: "rest/storage_sites/"
                });
                
                
                ds.plug(Y.Plugin.DataSourceXMLSchema, {
                    schema: {
                        resultListLocator: "sites",
                        resultFields: [
                            {key:"resourceURI", locator:"*[local-name() ='resourceURI']"},
                            {key:"password", locator:"*[local-name()='credential']/*[local-name()='storageSitePassword']"},
                            {key:"username", locator:"*[local-name()='credential']/*[local-name()='storageSiteUsername']"},
                            {key:"currentNum", locator:"*[local-name() ='currentNum']"},
                            {key:"encrypted", locator:"*[local-name() ='encrypt']"},
                            {key:"quotaNum", locator:"*[local-name() ='quotaNum']"},
                            {key:"ID", locator:"*[local-name() ='storageSiteId']"},
                            {key:"cache", locator:"*[local-name() ='cache']"}
                            //                        
                        ]
                    }
                });
            
            
                var cols =
                    [
                    { key: 'ID', label: 'ID',editable:false},
                    { key: 'resourceURI', label: 'URI' },
                    { key: 'username', label: 'username'},
                    { key: 'password', label: 'password'},
                    { key:'encrypted',  label:"encrypted?"},
                    { key:'cache',  label:"cache?"},
                    { key: 'currentNum', label: 'currentNum'},
                    { key: 'quotaNum', label: 'quotaNum'}
                ];
            
                var table = new Y.DataTable({
                    columns:	cols,
                    data:		ds,
                    checkboxSelectMode:   true,
                    editable:       true,
                    editOpenType:   'click',
                    defaultEditor:  'text',
                    primaryKeys: ['ID','resourceURI']
                });
                
                
                table.plug(Y.Plugin.DataTableDataSource, {
                    datasource: ds,
                    initialRequest: "query?id=all&output=xml"
                });
                
                ds.after("response", function() {
                    table.render("#dtable")
                }); 
            
            
                table.after('cellEditorSave', function(o){
                    /*    td:  record:  colKey:   newVal:  prevVal:  editorName:     */
                    var msg = 'Editor: ' + o.editorName + ' saved newVal=' + o.newVal + ' oldVal=' + o.prevVal;
                    Y.log(msg);
                });
                
                
                
                Y.one("#btnSelected").on("click", function(){
                    var recs = table.get('checkboxSelected');
                    process(recs);
                });
                
                
                function process(recs) {
                    var msg       = '',
                    msgNumOff = '<li>({numOff} records selected are off of the current page)</li>',
                    numOff    = 0,
                    template  = '<li>Record index = {index} Data = {port} : {pname}</li>';

                    // Loop thru returned records,
                    // for records "off current page" they will have no TR or Record setting, but will have pkvalues set.
                    Y.Array.each(recs,function (r) {
                        if(r.tr) {
                            var data    = r.record.getAttrs(['select', 'port', 'pname']),
                            recindx = table.data.indexOf(r.record);

                            data.index = recindx;
                            msg += Y.Lang.sub(template, data);
                        } else
                            numOff++;
                    },table);
                }
            
            });
        </script>


    </body>
</html>
