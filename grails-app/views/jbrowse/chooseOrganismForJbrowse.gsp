<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 6/4/15
  Time: 2:37 PM
--%>

<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Choose JBrowse Organism</title>
</head>

<body>

<div>
    <ul>
        <li>
            <a class="btn btn-default" href="${createLink(uri: '/')}">
                <g:img src="ApolloLogo_100x36.png"/>
            </a>
        </li>
    </ul>
</div>

<g:if test="${flash.message}">
    <div class="message" role="status">${flash.message}</div>
</g:if>


<div style="margin-left: 20px;">
    <h3>
        Choose Organism to View
    </h3>

    <div class="btn-group-vertical" role="group" style="width: 300px;">
            <g:each in="${organisms}" var="organism">
                <a href="${createLink(uri:'')}/${organism.id}/jbrowse/index.html" type="button" class="btn btn-default">${organism.commonName}</a>
            </g:each>
    </div>

</div>

</body>
</html>
