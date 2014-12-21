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
            <button id="btnSave" class="yui3-button">Save Selections</button>
            <button id="btnInsert" class="yui3-button">Insert New Row</button>
            <button id="btnDelete" class="yui3-button">Delete Selections</button>
        </div>
        <!--<script src="http://yui.yahooapis.com/3.10.3/build/yui/yui-min.js"></script>-->

<!--        <script type="text/javascript">
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
            "gallery-datatable-formatters",
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
                            {key:"currentSize", locator:"*[local-name() ='currentSize']"},
                            {key:"encrypt", locator:"*[local-name() ='encrypt']"},
                            {key:"quotaNum", locator:"*[local-name() ='quotaNum']"},
                            {key:"quotaSize", locator:"*[local-name() ='quotaSize']"},
                            {key:"ID", locator:"*[local-name() ='storageSiteId']"},
                            {key:"cache", locator:"*[local-name() ='cache']"}
                        ]
                    }
                });
            
            
                var encr = { 0:'false', 1:'true'};
            
                var cols =
                    [
                    { key: 'ID', label: 'ID',editable:false},
                    { key: 'resourceURI', label: 'URI' },
                    { key: 'username', label: 'username'},
                    { key: 'password', label: 'password'},
                    //                    { key:'encrypt',  label:"encrypt?",
                    //                        formatConfig: encr,
                    //                        editor:"checkbox", editorConfig:{ checkboxHash:{ 'true':true, 'false':false } }
                    //                    },
                    { key:'encrypt',  label:"encrypt?"},
                    //                    { key:'cache',  label:"cache?",
                    //                        formatConfig: encr,
                    //                        editor:"checkbox", editorConfig:{ checkboxHash:{ 'true':true, 'false':false } }
                    //                    },
                    { key:'cache',  label:"cache?"},
                    { key: 'currentNum', label: 'currentNum'},
                    { key: 'currentSize', label: 'currentSize'},
                    { key: 'quotaSize', label: 'quotaSize'},
                    { key: 'quotaNum', label: 'quotaNum'}
                ];
            
                var table = new Y.DataTable({
                    columns:	cols,
                    data:		ds,
                    checkboxSelectMode:   true,
                    sortable:  true,
                    editable:       true,
                    editOpenType:   'click',
                    defaultEditor:  'text',
                    primaryKeys: ['ID']
                });
                
                
                table.plug(Y.Plugin.DataTableDataSource, {
                    datasource: ds,
                    initialRequest: "query?id=all&output=xml"
                });
                
                ds.after("response", function() {
                    table.sort('ID');
                    table.render("#dtable");
                }); 
            
            
                table.after('cellEditorSave', function(o){
                    /*    td:  record:  colKey:   newVal:  prevVal:  editorName:     */
                    var msg = 'Editor: ' + o.editorName + ' saved newVal=' + o.newVal + ' oldVal=' + o.prevVal;
                    //                    Y.log(msg);
                });
                
                
                
                Y.one("#btnSave").on("click", function(){
                    var recs = table.get('checkboxSelected');
                    var dataMsg = buildDataMsg(recs);
                    var uriPUT = "rest/storage_sites/set";
                    send(dataMsg,uriPUT);
                    table.checkboxClearAll();
                });
                
                var buildDataMsg = function(recs) {
                    var dataMsg = "<storageSiteWrapperList>"
                    Y.Array.each(recs,function (r) {
                        if(r.tr) {
                            if ( confirm("Are you sure you want to save this record ?\n"+ r.record.get('ID')+" : "+r.record.get('resourceURI')) === true ) {
                                dataMsg += "<sites>"
                                var recindx = table.data.indexOf(r.record);
            
                                dataMsg += "<cache>"
                                dataMsg +=r.record.get('cache');
                                dataMsg += "</cache>";
                            
                                dataMsg += "<credential><storageSitePassword>";
                                dataMsg +=r.record.get('password');
                                dataMsg +="</storageSitePassword>";
                                dataMsg +="<storageSiteUsername>";
                                dataMsg += r.record.get('username');
                                dataMsg +="</storageSiteUsername>";
                                dataMsg += "</credential>";
                            
                                dataMsg +="<currentNum>";
                                dataMsg +=r.record.get('currentNum');
                                dataMsg +="</currentNum>";
                                
                                dataMsg +="<currentSize>";
                                dataMsg +=r.record.get('currentSize');
                                dataMsg +="</currentSize>";
                                                            
                                dataMsg +="<encrypt>";
                                dataMsg += r.record.get('encrypt');
                                dataMsg+="</encrypt>";
                                
                                dataMsg +="<quotaNum>";
                                dataMsg += r.record.get('quotaNum');
                                dataMsg+="</quotaNum>";
                                    
                                dataMsg +="<quotaSize>";
                                dataMsg += r.record.get('quotaSize');
                                dataMsg+="</quotaSize>";
                            
                                dataMsg +="<resourceURI>";
                                dataMsg += r.record.get('resourceURI');
                                dataMsg+="</resourceURI>";
                                
                                dataMsg +="<storageSiteId>";
                                dataMsg += r.record.get('ID');
                                dataMsg+="</storageSiteId>";
                              
                                dataMsg += "</sites>"
                            }
                          
                        }
                    });
                    dataMsg += "</storageSiteWrapperList>"
                    Y.log("dataMsg"+dataMsg);
                    return dataMsg;
                }
                
                var send = function(dataMsg,uriPUT) {
                    
                    // Define a function to handle the response data.
                    function complete(id, o, args) {
                        //Y.log("complete. id: "+id);
                    };
                    
                    // Subscribe to event "io:complete", and pass an array
                    // as an argument to the event handler "complete", since
                    // "complete" is global.   At this point in the transaction
                    // lifecycle, success or failure is not yet known.
                    Y.on('io:complete', complete, Y, ['lorem', 'ipsum']);
                
                    var cfg = {
                        method: 'PUT',
                        data: dataMsg,
                        headers: {
                            'Content-Type': 'application/xml'
                        }
                    };
                    var requestPUT = Y.io(uriPUT,cfg);           
                    Y.log("request: "+requestPUT);
                    
                    
                    table.render("#dtable");
                    table.datasource.load();
                }
                
                Y.one("#btnDelete").on("click", function(){
                    var recs = table.get('checkboxSelected');
                    // returns array of objects {tr,record,pkvalues} 
                    // 
                     var dataMsg = buildDataMsg(recs);
                    
                    var uriPUT = "rest/storage_sites/delete";
                    send(dataMsg,uriPUT);
                    
                    table.checkboxClearAll();
                });
                
                
                Y.one("#btnInsert").on("click",function() {
                    Y.log("Insert new! "+ table.data.recordset);
                    
                    var max = -1;
                    var colIndex=0;
                    var record;
                    while ( record = table.data.item(colIndex++)) {
                        var id = record.get('ID');
                        var number = parseInt(id);
                        Y.log("ID: "+number);
                        if(number > max){
                            max = number;
                            Y.log("max= "+max);
                        }
                    }
                    Y.log("max= "+max);
                    table.addRow( [{ID:++max,resourceURI:'file://',
                            username:'uname',password:'pass',encrypt:'false',
                            cache:'false',currentNum:'-1',quotaNum:'-1',
                            quotaSize:'-1',currentSize:'-1'}] );
                });
            });
        </script>-->


    </body>
</html>
