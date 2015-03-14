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
                    ['country', 'Num. Of Requests'],
                    ['PL', 1103663],
                    ['IE', 801306],
                    ['GB', 476445],
                    ['FR', 86446],
                    ['NL', 74454],
                    ['null', 66681],
                    ['IT', 31942],
                    ['US', 7766],
                    ['AT', 6272],
                    ['RU', 4478],
                    ['ES', 3120],
                    ['GR', 832],
                    ['DE', 796],
                    ['BE', 626],
                    ['LU', 362],
                    ['NO', 310],
                    ['EU', 296],
                    ['BR', 104],
                    ['RS', 74],
                    ['HU', 52],
                    ['SE', 32]
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
