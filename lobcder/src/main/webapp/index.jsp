<%-- 
    Document   : index
    Created on : Sep 16, 2013, 3:05:12 PM
    Author     : alogo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>
            Welcome to the Large OBject Cloud Data storagE fedeRation (LOBCDER) service.
        </title>
        <link rel="stylesheet" type="text/css" href="style.css">
    </head>
    <body>
        <header id="header">
            <img id="logo-left" src="https://masterinterface.vph-share.eu/static/logo-web.png">
            <img src="http://www.how-to-draw-funny-cartoons.com/image-files/cartoon-lobster-7.gif"?w=100&h=100" width="100" height="100">
            <div id="page-title-wrapper">
                <div id="page-title">
                    <span class="title">
                        Welcome to the Large OBject Cloud Data storagE fedeRation (LOBCDER) service.
                    </span>
                </div>
            </div>
        </header>

        <div id="content">
            <div class="container">
                <div class="row">
                    <div class="span2">
                        <ul id="main-nav" class="nav nav-tabs nav-stacked">
                            <li class="active" id="navhome">
                                <a href="index.jsp">Home</a>
                            </li>
                            <li class="list-parent">
                                <a href="web/statistics.jsp">
                                    <i class="icon-globe icon-white">
                                    </i>
                                    Statistics
                                </a>
                            </li>

                            <li class="list-parent">
                                <a href="manager.jsp">LOBCDER Manager</a>
                            </li>

                        </ul>
                    </div>

                    <div id="page-content" class="span9">
                        <p style="text-align: justify; max-width: 90%;">
                            LOBCDER is a storage federation service that aims 
                            to ensure reliable, managed access to distributed 
                            scientific data stored in various storage frameworks 
                            and providers.
                        </p>

                        <p style="text-align: justify; max-width: 90%;">
                            Nowadays, data-intensive scientific research needs 
                            advanced storage capabilities that enable efficient 
                            data shearing. This is of great importance for many 
                            scientific domains such as the domain Virtual 
                            Physiological Human. LOBCDER is solution that 
                            federates a variety of data infrastructures ranging 
                            from simple file servers such as SFTP to more 
                            sophisticated systems used in the cloud or the grid. 
                            LOBCDER follows a client-centric approach that 
                            loosely couples with available data storage 
                            resources. This way we are able to utilize 
                            heterogeneous storage resources, reduce the 
                            complexity of using multiple storage resources and 
                            avoid vendor lock-in in the case of cloud storage. 
                        </p>
                        <br>
                        <br>
                        <br>
                        <br>
                    </div>
                </div>
            </div>
        </div> 
        <footer>
            <div id="bottom">
                <span>
                    footer
                </span>
                <span>
            </div>
        </footer>
    </body>
</html>
