<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="oldlook">

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

%{--<div class="row">--}%

<nav class="navbar navbar-custom" role="navigation">
    <div class="container-fluid">

        <div class="navbar-header">
            <a class="navbar-brand" href="http://genomearchitect.org/" target="_blank">
                <img style="padding: 0;margin: -18px; height:53px;" id="logo"  src="../images/ApolloLogo_100x36.png">
            </a>
        </div>

        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
            <ul class="nav navbar-nav">
                %{--<li class="active"><a href="#">Link <span class="sr-only">(current)</span></a></li>--}%
                %{--<li><a href="#">Link</a></li>--}%
                %{--<button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown"--}%
                        %{--aria-expanded="true">--}%
                    %{--File--}%
                    %{--<span class="caret"></span>--}%
                %{--</button>--}%
                %{--<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">--}%
                    %{--<li role="presentation"><a role="menuitem" tabindex="-1" href="#">Export</a></li>--}%
                %{--</ul>--}%

                <li class="dropdown">
                    <a href="#" class="dropdown-toggle header-header" data-toggle="dropdown" role="button"
                       aria-expanded="false">File <span class="caret"></span></a>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="#">Export</a></li>
                    </ul>
                </li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle header-header" data-toggle="dropdown" role="button"
                       aria-expanded="false">View<span class="caret"></span></a>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="#">Organism</a></li>
                        <li><a href="#">Changes</a></li>
                    </ul>
                </li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle header-header" data-toggle="dropdown" role="button"
                       aria-expanded="false">Permissions<span class="caret"></span></a>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="#">Users</a></li>
                        <li><a href="#">Groups</a></li>
                    </ul>
                </li>
            </ul>
            %{--<form class="navbar-form navbar-left" role="search">--}%
                %{--<div class="form-group">--}%
                    %{--<input type="text" class="form-control" placeholder="Search">--}%
                %{--</div>--}%
                %{--<button type="submit" class="btn btn-default">Submit</button>--}%
            %{--</form>--}%
            <ul class="nav navbar-nav navbar-right">
                <li><a href="#" class="header-header">Login</a></li>
            </ul>
        </div><!-- /.navbar-collapse -->

        %{--<div class="btn-group" role="group">--}%
            %{--<button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu3" data-toggle="dropdown"--}%
                    %{--aria-expanded="true">--}%
                %{--Tools--}%
                %{--<span class="caret"></span>--}%
            %{--</button>--}%
            %{--<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu3">--}%
                %{--<li role="presentation"><a role="menuitem" tabindex="-1" href="#">Search sequence</a></li>--}%
            %{--</ul>--}%
        %{--</div>--}%

        %{--<div class="btn-group" role="group">--}%
            %{--<button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu4" data-toggle="dropdown"--}%
                    %{--aria-expanded="true">--}%
                %{--Admin--}%
                %{--<span class="caret"></span>--}%
            %{--</button>--}%
            %{--<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu4">--}%
                %{--<li role="presentation"><a role="menuitem" tabindex="-1" href="#">Users</a></li>--}%
                %{--<li role="presentation"><a role="menuitem" tabindex="-1" href="#">Groups</a></li>--}%
            %{--</ul>--}%
        %{--</div>--}%

    </div>
</nav>

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
