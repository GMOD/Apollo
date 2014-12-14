<%@ page import="org.bbop.apollo.Organism" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">

    <g:set var="entityName" value="${message(code: 'sequence.label', default: 'Sequence')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>

    %{--<link rel="stylesheet" type="text/css" href="/jbrowse/plugins/WebApollo/"/>--}%
    %{--<link rel="stylesheet" type="text/css" href="css/selectTrack.css"/>--}%
    <asset:stylesheet src="selectTrack.css"/>
    %{--<asset:stylesheet src="search_sequence.css"/>--}%
    %{--<asset:stylesheet src="userPermissions.css"/>--}%
    %{--<asset:javascript src="vendor/jquery-1.11.1.min.js"/>--}%
    <asset:javascript src="selectTrack.js"/>
    %{--<script type="text/javascript" src="https://www.google.com/jsapi"></script>--}%
    %{--<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/dojo/1.5.1/dojo/dojo.xd.js"></script>--}%


    %{--<script src="js/jquery-ui-menubar/jquery.ui.all.css"></script>--}%
    %{--<script type="text/javascript" src="/js/jquery-ui-menubar/jquery-1.8.2.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery-1.8.2.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.core.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.widget.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.position.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.button.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.menu.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.menubar.js"></script>--}%
    %{--<script src="js/jquery-ui-menubar/jquery.ui.dialog.js"></script>--}%


    %{--<link rel="stylesheet" type="text/css" href="css/search_sequence.css"/>--}%
    %{--<link rel="stylesheet" type="text/css" href="css/userPermissions.css"/>--}%
    %{--<link rel="stylesheet" type="text/css" href="css/bootstrap.min.css"/>--}%
    %{--<link rel="stylesheet" type="text/css" href="css/bootstrap-glyphicons.css"/>--}%

    %{--<link rel="stylesheet" href="/jbrowse/plugins/WebApollo/jslib/jqueryui/themes/base/jquery.ui.all.css"/>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery-1.8.2.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.core.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.widget.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.position.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.button.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.menu.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.menubar.js"></script>--}%
    %{--<script src="jslib/jquery-ui-menubar/jquery.ui.dialog.js"></script>--}%
    <script>

        //    function cleanup_logo() {
        //        $("#logo").parent().css("padding", "0 0 0 0");
        //    }
        //    ;
    </script>
</head>

<body>
%{--<a href="#list-track" class="skip" tabindex="-1"><g:message code="default.link.skip.label"--}%
%{--default="Skip to content&hellip;"/></a>--}%

%{--<div class="row same-header">--}%

<div class="same-header btn-group btn-group-sm" role="group">
    

    <div class="btn-group" role="group">
        <a href="http://genomearchitect.org/" target="_blank">
            <img id="logo"
                 src="../images/ApolloLogo_100x36.png">
        </a>
    </div>

    %{--<div class="dropdown col-sm-1 ">--}%
    <div class="btn-group" role="group">
        <button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown"
                aria-expanded="true">
            File
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Export</a></li>
        </ul>
    </div>

    <div class="btn-group" role="group">
        <button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu2" data-toggle="dropdown"
                aria-expanded="true">
            View
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu2">
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Organism</a></li>
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Changes</a></li>
        </ul>
    </div>

    <div class="btn-group" role="group">
        <button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu3" data-toggle="dropdown"
                aria-expanded="true">
            Tools
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu3">
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Search sequence</a></li>
        </ul>
    </div>

    <div class="btn-group" role="group">
        <button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu4" data-toggle="dropdown"
                aria-expanded="true">
            Admin
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu4">
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Users</a></li>
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Groups</a></li>
        </ul>
    </div>

</div>

<div id="list-track" class="content scaffold-list" role="main">
    <h1><g:message code="default.list.label" args="[entityName]"/></h1>
    <g:select name="organism" from="${org.bbop.apollo.Organism.list()}"
              optionValue="commonName"/>
    <br/>
    <br/>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>

            %{--<th><g:message code="track.organism.label" default="Organism" /></th>--}%

            <g:sortableColumn property="name" title="${message(code: 'track.name.label', default: 'Name')}"/>
            <g:sortableColumn property="organism.name"
                              title="${message(code: 'track.name.label', default: 'Organism')}"/>

        </tr>
        </thead>
        <tbody>
        <g:each in="${sequenceInstanceList}" status="i" var="sequenceInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td><g:link action="show"
                            id="${sequenceInstance.id}">${fieldValue(bean: sequenceInstance, field: "name")}</g:link></td>
                <td>${sequenceInstance.organism.commonName}
                <g:link uri="">Browse</g:link>
                </td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${trackInstanceCount ?: 0}"/>
    </div>
</div>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

</body>
</html>
