<%@ page import="org.bbop.apollo.User" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'user.label', default: 'User')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>
<a href="#list-user" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                           default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="list-user" class="content scaffold-list col-lg-offset-1" role="main">
    %{--<h1><g:message code="default.list.label" args="[entityName]"/></h1>--}%
    <h1>Sequence Permissions</h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>

    <div class="col-lg-6 col-lg-offset-1">
        <table>
            <th>Sequence</th>
            <th>Admin</th>
            <th>Write</th>
            <th>Export</th>
            <th>Read</th>

        <g:include view="sequence/singleUser.gsp"/>
        <g:include view="sequence/singleUser.gsp"/>
        <g:include view="sequence/singleUser.gsp"/>
        <g:include view="sequence/singleUser.gsp"/>
        <g:include view="sequence/singleUser.gsp"/>
        </table>
    </div>

    <div class="pagination">
        <g:paginate total="${userInstanceCount ?: 0}"/>
    </div>
</div>
</body>
</html>
