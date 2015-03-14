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
                    ['country', 'tx_size (GB)'],
                    ['PL', 278.7808847539],
                    ['IE', 7.7027354185],
                    ['GB', 166.7116254754],
                    ['FR', 7.5398582416],
                    ['NL', 485.338929113],
                    ['IT', 1.2740683053],
                    ['RU', 12.7379883602],
                    ['US', 5.6576497583],
                    ['BE', 3.1047819695],
                    ['null', 0],
                    ['GR', 2.3485489013],
                    ['LU', 1.7711113077],
                    ['DE', 3.13054770231247E-005]
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
