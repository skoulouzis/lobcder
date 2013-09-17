<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
        <title>Manage </title>
    </head>
    <body>
        <h3>Manage Storage Sites.</h3>

        <!--        <ol>
        <s:iterator value="sites">
            <display:column property='propertyName' 
            <li>
            <td> Id: <s:property value="storageSiteId" />  </td>
            <td>  URL: <s:property value="resourceURI" /></td>
            <td>  quota size: <s:property value="quotaSize" /></td>
            <td> quota num: <s:property value="quotaNum" /></td>
            <td>  current size: <s:property value="currentSize" /></td>
            <td> current num: <s:property value="currentNum" /></td>
            <td> user name: <s:property value="credential.storageSiteUsername" /></td>
            <td>  password: <s:property value="credential.storageSitePassword" /></td>
            <td>  is encrypted: <s:property value="encrypt" /></td>
            </li>
        </s:iterator>
    </ol>   -->




        <s:url action="deleteStorageSites" var="deleteUrl">
            <s:param name="storageSiteId" value="storageSiteId"/>
        </s:url>


        <table border="1">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>URL</th>
                    <th>Quota Size</th>
                    <th>Quota Num.</th>
                    <th>Current Size</th>
                    <th>current Num.</th>
                    <th>User Name</th>
                    <th>Password:</th>
                    <th>Is encrypted</th>
                    <th>__-</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <s:iterator value="sites">
                        <s:url action="web/editStorageSites" var="editUrl">
                            <s:param name="storageSiteId" value="storageSiteId"/>
                        </s:url>
                        
                        <td><s:property value="storageSiteId" /></td>
                        <td><s:property value="resourceURI" /></td>
                        <td><s:property value="quotaSize" /></td>
                        <td><s:property value="quotaNum" /></td>
                        <td><s:property value="currentSize" /></td>
                        <td><s:property value="currentNum" /></td>
                        <td><s:property value="credential.storageSiteUsername" /></td>
                        <td><s:property value="credential.storageSitePassword" /></td>
                        <td><s:property value="encrypt" /></td>
                        <td><a href="<s:property value='#editUrl' />" >Edit</a></td>
                    </s:iterator>



                </tr>
            </tbody>
        </table>



        <p><a href="../index.jsp">Home</a>.</p>

    </body>
</html>