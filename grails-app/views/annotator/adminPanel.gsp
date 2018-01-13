
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="embedded">
</head>

<body>


<div class="list-group">
    <g:each in="${links}" var="link">
        <g:if test="${highestRank && link.globalRole.rank <= highestRank}">
            <g:link class="list-group-item" target="_blank" uri="${link.link}">${link.label}</g:link>
        </g:if>
    </g:each>
</div>

</body>
</html>