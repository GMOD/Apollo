<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 5/12/15
  Time: 12:03 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="embedded">
</head>

<body>

%{--<ul>--}%
%{--<g:each in="${links}" var="link">--}%
%{--<li>--}%
%{--<g:link target="_blank" uri="${link.link}">${link.label}</g:link>--}%
%{--</li>--}%
%{--</g:each>--}%
%{--</ul>--}%

<div class="list-group">
    <g:each in="${links}" var="link">
        <g:link class="list-group-item" target="_blank" uri="${link.link}">${link.label}</g:link>
    </g:each>
</div>

</body>
</html>