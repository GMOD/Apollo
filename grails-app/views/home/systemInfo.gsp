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



<div class="row col-md-offset-1">

    <h3>Metrics</h3>
    <ul class="list-unstyled">
        <li>
            <g:link action="threads" controller="metrics">
                Threads
            </g:link>
        </li>
        <li>
            <g:link action="metrics" controller="metrics" params="[pretty: true]">
                Timings
            </g:link>
        </li>
    </ul>

    <div class="row col-md-5">
        <h3>Runtime Info</h3>
        <table class="table">
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
    </div>

    <div class="row col-md-10">
        <h3>Java Info</h3>
        <table class="table">
            <g:each in="${javaMapInstance.sort{it.key}}" var="data">
                <g:if test="${!data.key.toLowerCase().contains("password")}">
                    <tr>
                        <td>
                            ${data.key}
                        </td>
                        <td>
                            ${data.value}
                        </td>
                    </tr>
                </g:if>
            </g:each>
        </table>
    </div>

    <div class="row col-md-10">
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
    </div>
</div>

</body>
</html>
