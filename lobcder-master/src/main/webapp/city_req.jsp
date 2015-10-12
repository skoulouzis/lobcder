<html>
    <head>
        <script type='text/javascript' src='https://www.google.com/jsapi'></script>
        <script type='text/javascript'>
            google.load('visualization', '1', {'packages': ['geochart']});
            google.setOnLoadCallback(drawMarkersMap);

            function drawMarkersMap() {
                var data = google.visualization.arrayToDataTable([
                    ['City', 'Requests'],
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


//                var data = google.visualization.arrayToDataTable([
//                    ['City', 'Population', 'Area'],
////                    ['Rome', 2761477, 1285.31],
////                    ['Milan', 1324110, 181.76],
////                    ['Naples', 959574, 117.27],
////                    ['Turin', 907563, 130.17],
////                    ['Palermo', 655875, 158.9],
////                    ['Genoa', 607906, 243.60],
////                    ['Bologna', 380181, 140.7],
////                    ['Florence', 371282, 102.41],
////                    ['Fiumicino', 67370, 213.44],
////                    ['Anzio', 52192, 43.43],
//                    ['Ciampino', 1, 1]
//                ]);

                //      var options = {
                //        region: 'IT',
                //        displayMode: 'markers',
                //        colorAxis: {colors: ['green', 'blue']}
                //      };
                //
                //                var options = {
                //                    colorAxis: {colors: ['#B2B2FF', '#FF0000']} // orange to blue
                //                };


//                var options = {};
//                options['region'] = '154';
//                 options['colors'] = ['green', 'blue'];
//                options['dataMode'] = 'markers';

                var options = {
                    displayMode: 'markers',
                    sizeAxis: {minValue: 0, maxValue: 100},
                    colorAxis: {colors: ['#e7711c', '#4374e0']},
                    region: '150'

                };
                options['backgroundColor'] = '#98AFC7';
                options['datalessRegionColor'] = '#E5E5E5';




                var chart = new google.visualization.GeoChart(document.getElementById('chart_div'));
                chart.draw(data, options);
            }
            ;
        </script>
    </head>
    <body>
        <div id="chart_div" style="width: 900px; height: 500px;"></div>
    </body>
</html>