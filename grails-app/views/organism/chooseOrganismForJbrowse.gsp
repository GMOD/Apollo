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
<a href="#show-cannedComment" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                                    default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        %{--<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>--}%
        %{--<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>--}%
        %{--<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>--}%
    </ul>
</div>

<div id="show-cannedComment" class="content scaffold-show" role="main">
    <h1>
        Choose Organism for JBrowse
    </h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <ol class="property-list cannedComment">

        <li class="fieldcontain">
            <span id="comment-label" class="property-label">
                Choose Organism
            </span>

            <span class="property-value" aria-labelledby="comment-label">
                <ul>
                    <g:each in="${organisms}" var="organism">
                        <li>
                            <g:link uri="${params.urlString}&organism=${organism.id}">${organism.commonName}</g:link>
                            ${urlString}
                        </li>
                    %{--${organism} ${urlParams}--}%
                    </g:each>
                </ul>
            </span>

        </li>

</div>
</body>
</html>
