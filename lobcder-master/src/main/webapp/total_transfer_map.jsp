<html>
    <head>
        <!--<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.5/jquery.min.js"></script>-->
        <script type="text/javascript" src="https://www.google.com/jsapi"></script>
        <script type="text/javascript">


            google.load("visualization", "1", {packages: ["geochart"]});
            google.setOnLoadCallback(drawRegionsMap);


//            function drawRegionsMap() {
//                // grab the CSV
//                $.get("country.csv", function(csvString) {
//                    // transform the CSV string into a 2-dimensional array
//                    var arrayData = $.csv.toArrays(csvString, {onParseValue: $.csv.hooks.castToScalar});
//
//                    // this new DataTable object holds all the data
//                    var data = new google.visualization.arrayToDataTable(arrayData);
//
//
//                    var options = {
//                        colorAxis: {colors: ['#B2B2FF', '#FF0000']} // orange to blue
//                    };
//
//
//
//                    var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));
//
//                    chart.draw(data, options);
//                });
//            }

            function drawRegionsMap() {

                var data = google.visualization.arrayToDataTable([
                    ['country', 'total (GB)'],
                    ['PL', 358.8091688109],
                    ['IE', 7.7975992403],
                    ['GB', 627.6294833673],
                    ['FR', 12.9400476804],
                    ['NL', 652.302247717],
                    ['IT', 8.6213761363],
                    ['RU', 12.7534867655],
                    ['US', 5.9071000479],
                    ['BE', 3.6217030874],
                    ['null', 0.0738442224],
                    ['GR', 2.477423233],
                    ['LU', 3.3061364256],
                    ['DE', 6.4845218938]
                ]);

                var options = {
                    colorAxis: {colors: ['#B2B2FF', '#FF0000']} // orange to blue
                };



                var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));

                chart.draw(data, options);
            }
        </script>
    </head>
    <body>
        <div id="regions_div" style="width: 900px; height: 500px;"></div>
    </body>
</html>
