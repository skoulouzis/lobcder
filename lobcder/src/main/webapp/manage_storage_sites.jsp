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

        <script src="http://yui.yahooapis.com/3.10.3/build/yui/yui-min.js"></script>


        <div id="controls">
            <button id="save" style="display:none;">Save</button>
            <button id="cancel" style="display:none;">Cancel</button>
        </div>

        <div id="storage_site_table"></div>

        <script type="text/javascript">
            YUI().use(
            "datasource-io", 
            "datasource-xmlschema", 
            "datatable-datasource", 
            "node",  
            "cssfonts", 
            "cssbutton", 
            'event-mouseenter', 
            "datatable-sort",  
            "calendar",
            "datatable-scroll",
            "gallery-calendar-jumpnav", 
            "gallery-datatable-formatters",
            'gallery-datatable-editable', 
            'gallery-datatable-celleditor-popup',
            
             
            function(Y) {
                var ds = new Y.DataSource.IO({
                    source: "http://localhost:8080/lobcder/rest/storage_sites/"
                });

                ds.plug(Y.Plugin.DataSourceXMLSchema, {
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
                
                
                var yesNoformatConfig = { Flase:'false',True:'true'};
                

                  
                var cols =
                    [
                    { key: 'ID', label: 'ID',editable:false},
                    { key: 'resourceURI', label: 'URI' },
                    { key: 'username', label: 'username'},
                    { key: 'password', label: 'password'},
                    { key:'encrypted',  label:"encrypted?",
                        formatConfig:yesNoformatConfig,
                        editor:"radio",
                        editorConfig:{
                            radioOptions: yesNoformatConfig,
                            overlayWidth: 250
                        }
                    },
                        
                    { key: 'currentNum', label: 'currentNum'},
                    { key: 'quotaNum', label: 'quotaNum'},
                ];
                
                                        
                        
                var table = new Y.DataTable({columns: cols,
                    sortable:  true,

                    scrollable: 'y',
                    //                    height: '160px',

                    // Setup editing for this DT ... "click" to invoke, "text" type if not set on columns
                    editable:       true,
                    editOpenType:   'click',
                    defaultEditor:  'text'});
    
                table.plug(Y.Plugin.DataTableDataSource, {
                    datasource: ds,
                    initialRequest: "query?id=all&output=xml"
                });
                
                
                // Make another request later
                //table.datasource.load({request:"zip=94089&query=pizza"});
               
                
                ds.after("response", function() {
                    table.render("#storageSite")
                });       
                
                
                Y.DataTable.prototype.getCellColumnKey = function (node) {
                    var classRE = new RegExp( this.getClassName('col') + '-(\\w+)'),
                    cname = (node.get('className').match(classRE) || [])[1];
                    return cname;
                };
    
                
                var args;
                table.after("cellEditorSave", function(oArgs){
                    /*    td:  record:  colKey:   newVal:  prevVal:  editorName:     */
                    //                    var msg = 'Editor: ' + e.editorName + ' saved newVal=' + e.newVal + ' oldVal=' + e.prevVal;
                    var msg = 'Editor: ' + oArgs.editorName + ' saved newVal=' + oArgs.newVal
                        + ' oldVal=' + oArgs.prevVal + ' colKey=' + oArgs.colKey +' currentTarget:'+oArgs.currentTarget.get("id");
                    Y.log(msg);
                    
                    
                    var rec = this.getRecord(0);
                    Y.log("Rec: "+rec);
                     
                     
                    save.show();
                    cancel.show();
                    args = oArgs;
                });
                
                
                var save   = Y.one('#save');
                var cancel = Y.one('#cancel');
                
                
                function finish()
                {
                    save.hide();
                    cancel.hide();
                }
                
                
                save.on('click', function()
                {
                    
                    var msg = 'Editor: ' + event.editorName + ' saved newVal=' + event.newVal + ' oldVal=' + event.prevVal;
                    Y.log(msg);
                                        
                    finish();
                });
                
                
                cancel.on('click', function ()
                {
                    var msg = 'Editor: ' + event.editorName + ' saved newVal=' + event.newVal + ' oldVal=' + changes.prevVal;
                    Y.log(msg);
                    finish();
                });

            });
        </script>





<!--                <script>
                    YUI({
                        //                combine:false
                        //                filter:'raw'
                        // need gallery tag for cellediting, jumpnav and formatters
                        //                gallery: 'gallery-2013.01.16-21-05'
                    }).use( "node",  "cssfonts", "cssbutton", 'event-mouseenter', "datatable-sort",  "calendar",
                    "datatable-scroll",
                    "gallery-calendar-jumpnav", "gallery-datatable-formatters",
                    'gallery-datatable-editable', 'gallery-datatable-celleditor-popup',
                    function(Y){
                        
                        //======================================================================================
                        //          Test Case - Example usage
                        //======================================================================================
                        
                        // Define some sample DT data ...
                        var someData = [
                            {sid:10, sname:'Sneakers', sopen:0, stock:0, sprice:59.93, sdate:new Date(2009,3,11) },
                            {sid:11, sname:'Varnished Cane Toads', sopen:1, stock:2, sprice:17.49, sdate:new Date(2009,4,12) },
                            {sid:12, sname:'JuJu Beans', sopen:0, stock:1, sprice:1.29, sdate:new Date(2009,5,13) },
                            {sid:13, sname:'Tent Stakes', sopen:1, stock:1, sprice:7.99, sdate:new Date(2010,6,14) },
                            {sid:14, sname:'Peanut Butter', sopen:0, stock:0, sprice:3.29, sdate:new Date(2011,7,15) },
                            {sid:15, sname:'Garbage Bags', sopen:1, stock:2, sprice:17.95, sdate:new Date(2012,8,18) }
                        ];
                        
                        //                Y.Array.each(someData,function(d,di){
                        //                    d.sdesc = 'Description for Item ' + d.sid + ' : ' + d.sname;
                        //                    d.stype = di*10;
                        //                });
                        
                        //
                        // Define some Arrays / Object Hashes to be used by formatters / editor options ...
                        //
                        var stypes = [
                            {value:0,  text:'Standard'},
                            {value:10, text:'Improved'},
                            {value:20, text:'Deluxe'},
                            {value:30, text:'Better'},
                            {value:40, text:'Subpar'},
                            {value:50, text:'Junk'}
                        ];
                        
                        var stypesObj = {};
                        Y.Array.each(stypes,function(r){
                            stypesObj[r.value] = r.text;
                        });
                        
                        var stock = { 0:'No ', 1:'Yes ', 2:'B/O ' };
                        var sopen = { 0:'No', 1:'Yes'};
                        
                        // ... and here we go,
                        //
                        // We use pre-named editors on the "editor" property of the Columns,
                        //   in some cases, editorConfig are added to provide stuff to 
                        //   pass to the editor Instance ...
                        
                        var dt = new Y.DataTable({
                            columns:[
                                { key:'sid',    label:"sID", editable:false },
                                
                                { key:'sopen',  label:"Open?",
                                    formatter:"custom", formatConfig:sopen,
                                    editor:"checkbox", editorConfig:{ checkboxHash:{ 'true':1, 'false':0 } }
                                },
                                
                                { key:'sname',  label:"Item Name" },
                                
                                { key:'sdesc',  label:"Description",  editor:"textarea" },
                                
                                { key:'stype',  label:"Condition",
                                    formatter:"custom", formatConfig:stypesObj,
                                    editor:"select", editorConfig:{
                                        selectOptions: stypesObj
                                    }
                                },
                                
                                { key:'stock',  label:"In Stock?",
                                    formatter:"custom", formatConfig:stock,
                                    editor:"radio",
                                    editorConfig:{
                                        radioOptions: stock,
                                        overlayWidth: 250
                                    }
                                },
                                
                                { key:'sprice', label:"Retail Price",
                                    formatter:"currency2", className:'align-right',
                                    editor:"number"
                                },
                                
                                { key:'sdate',  label:"Trans Date",
                                    formatter:"shortDate", className:'align-right',
                                    editor:"calendar", editorConfig:{
                                        overlayConfig:{ buttons:false }
                                    }
                                }
                            ],
                            
                            data: someData,
                            sortable:  true,
                            
                            scrollable: 'y',
                            height: '160px',
                            
                            // Setup editing for this DT ... "click" to invoke, "text" type if not set on columns
                            
                            editable:       true,
                            editOpenType:   'click',
                            defaultEditor:  'text'
                        }).render('#dtable');
                        
                        // Set some other listeners .....
                        dt.after('cellEditorSave', function(o){
                            /*    td:  record:  colKey:   newVal:  prevVal:  editorName:     */
                            var msg = 'Editor: ' + o.editorName + ' saved newVal=' + o.newVal + ' oldVal=' + o.prevVal;
                            Y.log(msg);
                        });
                        
                        //                Y.one("#btnPaws").on("click",function(){
                        //                    Y.log("Currently registered column Editor names:");
                        //                    Y.log( this.getCellEditorNames() );
                        //                },dt);
                        
                    });
                </script>-->
    </body>
</html>
