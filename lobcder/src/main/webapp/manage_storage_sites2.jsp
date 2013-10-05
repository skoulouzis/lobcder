<%-- 
    Document   : manage_storage_sites2
    Created on : Oct 5, 2013, 3:42:24 PM
    Author     : alogo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body class="yui3-skin-sam" >

        <div id="dtable"></div>

        <div class="ba_content">
            <button id="btnSelected" class="yui3-button">Process Selections</button>
            <!--<button id="btnClearSelected" class="yui3-button">Clear Selections</button>-->
            <!--<button id="btnRows" class="yui3-button">Set Rows 1,3,5,7 Checked </button>-->
        </div>


        <script src="http://yui.yahooapis.com/3.12.0/build/yui/yui-min.js"></script>

        <script>
            YUI({
            }).use( "datatable-scroll",  "datatable-mutable", "event-custom",
            "datatype", "cssfonts", "cssbutton",
            'gallery-datatable-paginator','gallery-paginator-view',
            "gallery-datatable-checkbox-select",
            "node",  "cssfonts", "cssbutton", 'event-mouseenter', "datatable-sort",  "calendar",
            //  "datatable-scroll",
            "gallery-calendar-jumpnav", "gallery-datatable-formatters",
            'gallery-datatable-editable', 'gallery-datatable-celleditor-popup',
            function (Y) {
                
                //==================================================================================================
                //                              Execution Begins
                //==================================================================================================

                var data = [
                    { ID:0,  resourceURI:'file://localhost',password:'secret', username:'uname1', currentNum:'-1',encrypted :'-1', quotaNum:'-1' ,cache:'true', encrypted:'false'},
                    { ID:1,  resourceURI:'sftp://user@localhost',password:'secret', username:'uname2', currentNum:'-1',encrypted :'-1', quotaNum:'-1' ,cache:'false', encrypted:'false'},
                    { ID:2,  resourceURI:'swift://localhost',password:'secret', username:'uname3', currentNum:'-1',encrypted :'-1', quotaNum:'-1' ,cache:'false', encrypted:'true'}
                ];

                var dtable = new Y.DataTable({
                    columns : [
                        { key: 'ID',   primaryKey:true, editable:false },
                        { key: 'resourceURI'},
                        { key: 'password'},
                        { key:'username'}
                    ],
                    data: data,
                    // set the checkboxSelect mode based on the checkbox above the DT ...
                    checkboxSelectMode:  true,
                    editable:       true,
                    editOpenType:   'click',
                    defaultEditor:  'text'

                }).render("#dtable");

                //----------------
                //  CSS-Button click handlers ....
                //----------------
                //                Y.one('#btnRows').on('click',function(){
                //                    dtable.set('checkboxSelected', [1,3,5,7] );
                //                });

                function process(recs) {
                    var msg       = '',
                    msgNumOff = '<li>({numOff} records selected are off of the current page)</li>',
                    numOff    = 0,
                    template  = '<li>Record index = {index} Data = {ID} : {resourceURI}</li>';

                    // Loop thru returned records,
                    // for records "off current page" they will have no TR or 
                    // Record setting, but will have pkvalues set.
                    Y.Array.each(recs,function (r) {
                        if(r.tr) {
                            var data    = r.record.getAttrs(['select', 'ID', 'resourceURI']),
                            recindx = dtable.data.indexOf(r.record);

                            data.index = recindx;
                            msg += Y.Lang.sub(template, data);
                        } else
                            numOff++;
                    },dtable);

                    msg = msg || '<li>(None)</li>';
                    if(numOff>0) msg += Y.Lang.sub(msgNumOff,{numOff:numOff});
                    Y.log(msg);
                }

                Y.one("#btnSelected").on("click", function(){
                    var recs = dtable.get('checkboxSelected');
                    process(recs);
                    dtable.checkboxClearAll();
                });
                
                
                dtable.after('cellEditorSave', function(o){
                    /*    td:  record:  colKey:   newVal:  prevVal:  editorName:     */
                    var msg = 'Editor: ' + o.editorName + ' saved newVal=' + o.newVal + ' oldVal=' + o.prevVal;
                    Y.log(msg);
                });

                //                Y.one("#btnClearSelected").on("click",function () {
                //                    dtable.checkboxClearAll();
                //                });
                

            });
        </script>

    </body>
</html>
