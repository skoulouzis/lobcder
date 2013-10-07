<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
    <head>
        <title>YUI 3 DataTable Basic Usage</title>
        <meta http-equiv="content-type" content="text/html; charset=utf-8">

    </head>

    <body class="yui3-skin-sam"  style="visibility: hidden;">

        <script type="text/x-template" id="dialog-template">	<!--  used in Function showDT_Panel   -->
            <form name="roweditor">
                <fieldset id="myfieldset">
                    <table>
                        <!--<tr><th>Storage Site ID :</th><td><input type="text" name="frmEID" value="{valID}" /></td></tr>-->
                        <tr><th>Storage Site URL :</th><td><input type="text" name="frmURI" value="{valURI}" /></td></tr>
                        <tr><th>Storage Site Username :</th><td><input type="text" name="frmUsername" value="{valUsername}" /></td></tr>
                        <tr><th>Storage Site password :</th><td><input type="text" name="frmPassword" value="{valpasswd}" id="idCalInput" /></td></tr>
                        <tr><th>Is Storage Site Encrypted :</th><td><input type="text" name="frmEncrypted" value="{valEncrypted}" /></td></tr>
                        <tr><th>Is Storage Site Cache :</th><td><input type="text" name="frmCache" value="{valCache}" /></td></tr>
                    </table>
                </fieldset>
                <input type="hidden" name="frmRecord" value="{valRecord}" />
                <input type="hidden" name="frmInsertFlag" value="{valInsert}" />
            </form>
        </script>


        <div class="ba_content">
            <table>
                <tr valign="top">
                    <td>
                        <div id="dtable"></div>
                        <br/><button id="btnProcess" class="yui3-button">Process SELECTED Rows</button>
                        &nbsp; &nbsp; <button id="btnInsert" class="yui3-button">Insert New Row</button>
                    </td>
                </tr>
            </table>
        </div>

    <style type="text/css">

        /*	Define align-center, left, right cell classes for cell alignment   */
        .right { text-align:right; }
        .left { text-align:left;  }
        .center { text-align:center;  }
        .yui3-skin-sam .yui3-datatable td.myhighlight,
        .yui3-skin-sam .yui3-datatable-sorted .yui3-datatable-cell td.myhighlight {
            background-color:#EBF09E;
        }

        /*  define images to be used in TD cell formatters for Edit / Delete  */

        .cell-delete {
            background: url("images/delete.png") no-repeat scroll center center transparent;
            cursor: pointer;
            height: 8px;
            width: 8px;
        }		

        .cell-edit {
            background: url("images/edit.png") no-repeat scroll center center transparent;
            cursor: pointer;
            height: 8px;
            width: 8px;
        }		

        #dtable table tr td {
            cursor: pointer;
        }

        /*  CSS for this page display */

        table #dt_info {
            border-collapse:collapse;
        }

        table #dt_info td {
            border: 1px solid #444;			
            padding: 8px 8px;
        } 

        table #dt_info th {
            border: 1px solid #444;			
            padding: 8px 8px;
            background-color: #090;  
            color: #fcfcfc;
        } 

        #myfieldset {
            background-color:#FFF5DF;
            border-radius: 7px;
            -moz-border-radius: 7px;
            -webkit-border-radius: 7px;
        }

        #myfieldset input[type=text]{
            width: 10em;
        }

    </style>

    <script src="http://yui.yahooapis.com/3.10.3/build/yui/yui-min.js"></script>


    <script type="text/javascript">
        YUI().use( 
        "datatable", 
        "datatype", 
        "panel", 
        "dd-plugin", 
        "cssfonts", 
        "cssbutton", 
        "datasource-io", 
        "datasource-xmlschema", 
        "datatable-datasource", 
        "gallery-datatable-checkbox-select",
        "datatable-scroll", 
        "datatable-sort",  
        "gallery-datatable-checkbox-select",
        "datatype", "cssfonts", "cssbutton",
        function (Y) {

            Y.on('domready',function(){	Y.one("body").setStyle('visibility','inherit');  });

            /**
             Method returns the column key (or name) from the provided TD node
             @method getColumn
             @param target {Node} the TD node for the requested column
             @returns {String} Column key or name
             **/
            Y.DataTable.prototype.getCellColumnKey = function (node) {
                var classRE = new RegExp( this.getClassName('col') + '-(\\w+)'),
                cname = (node.get('className').match(classRE) || [])[1];
                return cname;
            };


            //==================================================================================================
            //                              Execution Begins
            //==================================================================================================
                
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
                
                
            //            var fmtChkBox = function(o){
            //                var chkd = (o.data.select) ? "checked" : "";
            //                o.value = '<input type="checkbox" class="myCheckboxFmtr" ' + chkd + '/>';
            //                o.className += ' center';
            //            }
                
            var fmtBlank = function(o) {
                if (o.column.className) o.className += ' center '+o.column.className;
                o.value = ' ';
            }
                
            var cols =
                [
                //                    { key: 'select',
                //                        allowHTML: true, // to avoid HTML escaping
                //                        label: '<input type="checkbox" class="protocol-select-all" title="Toggle ALL records"/>',
                //                        formatter: '<input type="checkbox" checked/>',
                //                        emptyCellValue: '<input type="checkbox" unchecked/>'
                //                    },
                //            
                //                    { key:'select', label:'<input type="checkbox" id="selAll" title="Click to toggle ALL records"/>',
                //                        allowHTML:  true,   // must use allowHTML:true if we insert HTML in the label and the formatter ...
                //                        formatter:  fmtChkBox,
                //                        emptyCellValue: '<input type="checkbox" unchecked/>',
                //                        nohighlight: true   // custom property
                //                    },
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
                
                { key:'cache',  label:"cache?",
                    formatConfig:yesNoformatConfig,
                    editor:"radio",
                    editorConfig:{
                        radioOptions: yesNoformatConfig,
                        overlayWidth: 250
                    }
                },
                        
                { key: 'currentNum', label: 'currentNum'},
                { key: 'quotaNum', label: 'quotaNum'},
                    
                { name:'edit',   	 label:'- Edit -',   	formatter: fmtBlank, className:'cell-edit', nohighlight:true },
                { name:'delete',   	 label:'- Delete -',   	formatter: fmtBlank, className:'cell-delete', nohighlight:true }
            ];
                
            var yesNoformatConfig = { Flase:'false',True:'true'};
                
                
            var table = new Y.DataTable({
                columns:	cols,
                data:		ds,
                checkboxSelectMode:   true,
                // Use a custom recordType to add functionality to the checkbox formatter ...
                recordType: {
                    select:{ value:false },     // a custom property ... defined to allow "checkbox" select to work easily !
                    em_id:{}, ename:{},  etitle:{}, estart_date:{}, esalary:{}
                },
                primaryKeys: ['ID','resourceURI']
            });
                
            table.plug(Y.Plugin.DataTableDataSource, {
                datasource: ds,
                initialRequest: "query?id=all&output=xml"
            });
                
            ds.after("response", function() {
                table.render("#dtable")
            });       
                
    
    
            // -------------------------
            //  Create a Panel to serve as the Row Editor box
            //    ... this uses a template defined in <script> templates below, nice way to
            //        hide content until we want to display it.
            //   (see the beginning of http://yuilibrary.com/yui/docs/app/app-todo.html for an example of using <script> templates)
            // -------------------------
    
            var editorPanel = new Y.Panel({
                srcNode : '#idPanel',
                //                    width   : 420,
                //                    xy 		: [ 750, 170 ],
                visible : false,
                render  : true,
                //                    zIndex  : 10,
                plugins : [Y.Plugin.Drag],
                buttons: [
                    {
                        value  : 'Save',
                        section: Y.WidgetStdMod.FOOTER,
                        action : function (e) {
                            if (e) e.preventDefault();
                            saveFormData();
                            this.hide();
                        }
                    },
                    {
                        value  : 'Cancel',
                        section: Y.WidgetStdMod.FOOTER,
                        action : function (e) {
                            e.preventDefault();
                            this.hide();
                        }
                    }
                ]
            });
                
                
                
                

            //-----------------
            //  Function to save the FORM data, based on current values within the Panel.
            //
            //   We define a mapping object (called "record_map") to help us figure out how to
            //   apply which INPUT[name=xxx] element to which column of the Datatable.  Additionally,
            //   se set a "parser" to determine if the INPUT(value) needs to be parsed (typically to
            //   remove commas or currency symbols) before saving to the Datatable.
            //
            //  Uses the setting of FORM hidden value "frmInsertFlag" to determine if this is a NEW
            //   record or if we are saving EDITS on an existing record.
            //  If existing, the record "clientId" is saved in FORM hidden value "frmRecord"
            //-----------------
            var saveFormData = function() {
                var theForm = document.forms[0],
                rec_id 	= theForm.frmRecord.value,	// if INSERT, this is disregarded ...
                newData = {},
                raw_value  = 0,
                data_value = 0;
                Y.log("theForm.frmRecord.value: "+theForm.frmRecord.value);
                //
                //  Define a mapping between the INPUT 'name' settings and the record "key" names ...
                //    also, define a parser on a few numeric items
                //
                var record_map = [
                    //                        { field:'frmEID',    ckey:'ID'},
                    { field:'frmURI',  ckey:'resourceURI'},
                    { field:'frmUsername',  ckey:'username'},
                    { field:'frmPassword',  ckey:'password'},
                    { field:'frmEncrypted', ckey:'encrypted'},
                    { field:'frmCache', ckey:'cache'}
                ];

                //
                //  Run through the "record_map" FORM variables, inserting data values into "newData"
                //   that will serve as the data object for DataTable
                //
                Y.Array.each( record_map, function(item){
                    raw_value  = theForm[item.field].value;
                    data_value = ( item.parser && Y.Lang.isFunction(item.parser) ) ? item.parser.call(this,raw_value)  : raw_value ;
                    newData[ item.ckey ] = data_value;
                });

                //
                //  Now insert the "newData" object into DataTable's data,
                //    check frmInsertFlag for whether it is "new" or "updated" data
                //
                
                //                if ( parseInt( theForm.frmInsertFlag.value ) != 0 ){
                if(rec_index >= 0){
                    Y.log("modifyRow");
                    //table.modifyRow( rec_id, newData );
                    table.modifyRow( rec_index, newData );
                      
                }else{
                    Y.log("Insert");
                    table.addRow( newData );
                }
                
                rec_index = -1;
            }
        
            // trap an ENTER key on the form, save the data ...
            editorPanel.get('srcNode').on('key', function() {
                //editorPanel.delegate('key', function() {
                saveFormData();
                editorPanel.hide();
            }, 'enter');
        
        
            //
            //  Define DEFAULT data for a "New" inserted row
            //
            var default_data = {
                valURI : 'swift://host/path', // ename : 'New Storage Site',
                valUsername : 'uName',	  	 // etitle : '',
                valpasswd : 'secret',
                valEncrypted : 'flase',
                valCache : 'flase'
            };
        
            // position of "Insert Row" dialog
            var default_dialog_xy = [ 220, 130 ];
        
        
            //-----------------
            //  Displays the Panel (i.e. Dialog) for either EDIT of existing data or INSERT of new data.
            //  The passed in "record" and "xy" provide which record to edit and the XY position of the Panel when displayed.
            //  For a NEW record, these will be null.
            //
            //  If "insert_obj" is defined, then it assumes we are INSERTing a new record, and uses the default_data above.
            //-----------------
            var showDT_Panel = function( record, xy, insert_obj ) {
                var thePanel, DialogTMPL, body_html, header_html;
        
                //
                //  Grab the dialog internal content from the <script> template
                //
                DialogTMPL = Y.one("#dialog-template").getContent();
                thePanel = editorPanel;
        
                if ( !insert_obj ) {	// we are EDITING an existing row ...
                    //
                    //  Define the substitution objects to fill in the INPUT default values
                    //
                    var form_data = {
                        valID : 	record.get('ID'),
                        valURI : 	record.get('resourceURI'),
                        valUsername :	record.get('username'),
                        valpasswd :	record.get('password'),
                        valEncrypted :	record.get('encrypted')
                    }
        
                    xy[0] += 50;	// offset the dialog a tinch, from the Edit TD ...
        
                    header_html = 'Editing Row No. '  + (table.get('data').indexOf(record)+1);
                    body_html = Y.Lang.sub( DialogTMPL, form_data );
                    // thePanel.get('buttons.footer')[0].set("label","Update");
                    thePanel.getButton(0, 'footer').set('label', 'Update');
                } else {	//  we are INSERTING a new row ...
                    insertFlag = true;   // used
                    xy = default_dialog_xy;
                    header_html = 'Inserting NEW Row';
                    body_html = Y.Lang.sub( DialogTMPL, insert_obj );
                    //thePanel.get('buttons.footer')[0].set("label","Insert");
                    thePanel.getButton(0, 'footer').set('label', 'Insert');
                }
        
                //
                //	Fill the Panel content, position it and display it
                //
                thePanel.set( 'xy', xy );
                thePanel.set( 'headerContent', header_html );
                thePanel.set( 'bodyContent', body_html );
                thePanel.show();
            }
        
        
            // Button click handler for the "Insert New Row" button
            //var btnIns = new Y.Button({srcNode:"#btnInsert"}).render();
            Y.one("#btnInsert").on("click",function() {
                editorPanel.hide();
                showDT_Panel( 0, 0, default_data );
            });
        
    
            // -------------------------
            //  Define a click handler on table cells ...
            //   Note: use Event Delegation here (instead of just .on() ) because we may be
            //         deleting rows which may cause problems with just .on
            // -------------------------
            var lastTD;
            var rec_index;
            table.delegate("click", function(e) {

                var cell = e.currentTarget,					// the clicked TD
                row  = cell.ancestor(),					// the parent of TD, which is TR

                rec  = this.getRecord( cell ),			//  Call the helper method above to return the "data" record (a Model)
                //ckey = this.getCellColumnKey( cell ),	//
                ckey  = this.getCellColumnKey( cell ),
                col   = this.getColumn(ckey);			//
                //
                rec_index= this.get('data').indexOf(rec);       
                Y.log('------------------');
                //
                //  check for TD cell highlighting
                //
                if ( !col.nohighlight ) { 	// if col has nohighlight=true, then don't highlight the cell ....
                    if ( lastTD ) lastTD.removeClass("myhighlight");
                    cell.addClass("myhighlight");
                    lastTD = cell;
                }
                    

                //
                //  If a column 'action' is available, process it
                //
                switch( col.name || null ) {
                    case 'edit':
                        showDT_Panel( rec, cell.getXY() );
                        break;

                    case 'delete':
                        if ( confirm("Are you sure you want to delete this record ?") === true ) {
                            table.removeRow( rec.get('clientId') );
                            Y.one("#idStatus").setContent("<br/><b>Row was Deleted!</b>");
                        }

                        break;
                }
            }, "tbody tr td", table);
            //  the selector,  internal scope
    
    
            // -------------------------
            //   Click handler on "Select" TH checkbox, toggle the settings of all rows
            // -------------------------
            //                Y.one("#selAll").on("click", function(e){
            //                    var selAll = this.get('checked');	// the checked status of the TH checkbox
            //                    //
            //                    //  Get a NodeList of each of INPUT with class="myCheckboxFmtr" in the TBODY
            //                    //
            //                    var chks = table.get('srcNode').all("tbody input.myCheckboxFmtr");
            //                    chks.each( function(item){
            //                        item.set('checked', selAll);	// set the individual "checked" to the TH setting
            //                    });
            //                });
                
                
            
            Y.one("#btnProcess").on("click", function(){
                
                                
                //                Y.YQLRESTClient.request({
                //                    method: 'get',
                //                    contentType: 'application/xml',
                //                    content: '<storageSiteWrapperList><sites><cache>false</cache><credential><storageSitePassword>************</storageSitePassword><storageSiteUsername>skoulouz</storageSiteUsername></credential><currentNum>-1</currentNum><encrypt>false</encrypt><quotaNum>-1</quotaNum><quotaSize>-1</quotaSize><resourceURI>sftp://skoulouz@fs2.das4.science.uva.nl/home/skoulouz/tmp</resourceURI><storageSiteId>2</storageSiteId></sites><sites><cache>true</cache><credential><storageSitePassword>************</storageSitePassword><storageSiteUsername>fakeuser</storageSiteUsername></credential><currentNum>-1</currentNum><encrypt>false</encrypt><quotaNum>-1</quotaNum><quotaSize>-1</quotaSize><resourceURI>file:///tmp/</resourceURI><storageSiteId>1</storageSiteId></sites></storageSiteWrapperList>',
                //                    url: 'http://localhost:8080/lobcder/rest/storage_sites/set'
                //                    //                    url: 'http://term.ie/oauth/example/request_token.php'
                //                }, function (result) {
                //                    Y.log("----------------"+result);
                ////                    Y.log("----------------"+result.response);
                //                    //                    alert(result.response);
                //                });

    
                
                var recs = table.get('checkboxSelected');
                if(recs.length>0){
                    process(recs);
                }
                table.checkboxClearAll();
            });
                
            function process(recs) {
                var msg = "The following Storage Sites will be processed;\n\n";	// define the beginning of our message string

                Y.Array.each(recs,function (r) {
                    var record    = r.record;
                    Y.log("record: "+record);
                    msg += record.get('ID') + ' : ' + record.get('resourceURI') + "\n";
                });
                Y.log("msg "+msg);
                alert(msg);
            }
                
            // -------------------------
            //  Handle the "Process Selected Rows" BUTTON press,
            // -------------------------
                
          
            //                Y.one("#btnProcess").on( "click", function(){
            //                    //
            //                    //  Get a NodeList of all nodes on the DT which have the checkboxes I defined,
            //                    //    with class="myCheckBoxFmtr" AND that are currently "checked"
            //                    //
            //                    //                     var recs = dtable.get('checkboxSelected');
            //                    //                    var chks = this.get("srcNode").all("tbody tr td input.myCheckboxFmtr");		// get all checks
            //                    var chks = this.get("srcNode");
            //                    Y.log("chks: "+chks);
            //                
            //                    // in a perfect world ... i.e. one without IE 8-, we could just do ...
            //                    // var chkd = this.get("srcNode").all("tbody tr td input.myCheckboxFmtr:checked");
            //                
            //                    //
            //                    //  Loop over the NodeList (using it's .each method) and append the Storage Site name to the message.
            //                    //	 Note: 'chkd' contains nodes of the INPUT checkboxes, step back twice to get the parent TR node
            //                    //
            //                    var msg = "The following Storage Sites will be processed;\n\n";	// define the beginning of our message string
            //                
            //                    chks.each( function(item){
            //                        if ( !item.get('checked') ) return;
            //                        var rec = this.getRecord( item.ancestor("tr") );	// item is INPUT, first parent is TD, second is TR
            //                        msg += rec.get('em_id') + ' : ' + rec.get('ename') + "\n";
            //                    }, this);
            //                
            //                    alert(msg);
            //                
            //                }, table);

        });
    </script>


</body>
</html>
