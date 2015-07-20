<%--
  Created by IntelliJ IDEA.
  User: nathandunn
  Date: 7/20/15
  Time: 6:55 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title></title>
</head>

<body>


<h3>Runtime Info</h3>
<table>
    <g:each in="${runtimeMapInstance}" var="data">
        <tr>
            <td>
                ${data.key}
            </td>
            <td>
                ${data.value}
            </td>
        </tr>
    </g:each>
</table>

<h3>Java Info</h3>
<table>
    <g:each in="${javaMapInstance}" var="data">
        <tr>
            <td>
                ${data.key}
            </td>
            <td>
                ${data.value}
            </td>
        </tr>
    </g:each>
</table>

<h3>Servlet Info</h3>
<table>
    <g:each in="${servletMapInstance}" var="data">
        <tr>
            <td>
                ${data.key}
            </td>
            <td>
                ${data.value}
            </td>
        </tr>
    </g:each>
</table>



</body>
</html>