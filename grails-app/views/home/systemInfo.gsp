<%--
  Created by IntelliJ IDEA.
  User: nathandunn
  Date: 7/20/15
  Time: 6:55 AM
--%>

<head>
    <meta name="layout" content="report">
    <title>System Info</title>

</head>

<body>

<g:render template="../layouts/reportHeader"/>


<h3>Metrics</h3>
<ul>
    <li>
    <g:link action="threads" controller="metrics">
        Threads
    </g:link>
    </li>
    <li>
    <g:link action="metrics" controller="metrics" params="[pretty:true]">
        Timings
    </g:link>
    </li>
</ul>

<h3>Runtime Info</h3>
<table>
    <tr>

    </tr>
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