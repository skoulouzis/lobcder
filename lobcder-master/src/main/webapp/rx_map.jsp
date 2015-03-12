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
                    ['country', 'rx_size (GB)'],
                    ['PL', 80.028284057],
                    ['IE', 0.0948638218],
                    ['GB', 460.9178578919],
                    ['FR', 5.4001894388],
                    ['NL', 166.963318604],
                    ['IT', 7.347307831],
                    ['RU', 0.0154984053],
                    ['US', 0.2494502896],
                    ['BE', 0.5169211179],
                    ['null', 0.0738442224],
                    ['GR', 0.1288743317],
                    ['LU', 1.5350251179],
                    ['DE', 6.4844905883]
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
