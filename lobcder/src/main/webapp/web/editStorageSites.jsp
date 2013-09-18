<%-- 
    Document   : editStorageSites
    Created on : Sep 17, 2013, 8:16:29 PM
    Author     : alogo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Edit storage site</title>
    </head>
    <body>
        
    <s:form action='editStorageSite'>

        <p><s:textfield name="editSite.firstName" label="First name" /> <br />
        <s:textfield name="editSite.lastName" label="Last name" /> </p>

    <s:hidden name="person.id" />
    <s:submit  value="Save" />

</s:form>
</body>
</html>
