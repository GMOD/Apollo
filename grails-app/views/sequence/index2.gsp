<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
        pageEncoding="ISO-8859-1" %>
<%@ page import="javax.servlet.ServletContext; org.bbop.apollo.web.config.ServerConfiguration.DataAdapterConfiguration" %>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration.DataAdapterGroupConfiguration" %>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration" %>
<%@ page import="org.bbop.apollo.web.user.UserManager" %>
<%@ page import="org.bbop.apollo.web.user.Permission" %>
<%@ page import="org.bbop.apollo.web.track.TrackNameComparator" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.net.URL" %>
<%@ page import="org.apache.log4j.Logger" %>

%{--<%--}%
%{--Logger logger = Logger.getLogger(this.getClass().getName());--}%
%{--//ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));--}%
%{--javax.servlet.ServletContext servletContext = getServletContext();--}%
%{--logger.info("servlet context: "+servletContext);--}%
%{--ServerConfiguration serverConfig = new ServerConfiguration(servletContext);--}%
%{--if (!UserManager.getInstance().isInitialized()) {--}%
%{--ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();--}%
%{--UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());--}%
%{--}--}%
%{--String username = (String)session.getAttribute("username");--}%
%{--Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);--}%
%{--%>--}%

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta name="layout" content="oldlook">
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <!-- <link rel="stylesheet" type="text/css" href="jslib/DataTables-1.9.4/media/css/demo_table.css" /> -->

    <title>Select track</title>

    <asset:javascript src="jquery.js"/>

    <link rel="icon" type="image/x-icon" href="../images/webapollo_favicon.ico">
    <link rel="shortcut icon" type="image/x-icon" href="../images/webapollo_favicon.ico">

    <link rel="stylesheet" type="text/css" href="../css/selectTrack.css"/>
    %{--<link rel="stylesheet" type="text/css" href="../css/search_sequence.css"/>--}%
    %{--<link rel="stylesheet" type="text/css" href="../css/userPermissions.css"/>--}%
    <link rel="stylesheet" type="text/css" href="../js/jquery-ui-menubar/jquery.ui.all.css"/>
    %{--<link rel="stylesheet" type="text/css" href="../js/DataTables/css/demo_table.css"/>--}%

    <script src="../js/jquery-ui-menubar/jquery-1.8.2.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.core.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.widget.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.position.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.button.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.menu.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.menubar.js"></script>
    <script src="../js/jquery-ui-menubar/jquery.ui.dialog.js"></script>
    %{--<script type="text/javascript" src="https://www.google.com/jsapi"></script>--}%


    <script type="text/javascript" src="../js/DataTables/js/jquery.dataTables.js"></script>
    <script type="text/javascript" src="../js/DataTables-plugins/dataTablesPlugins.js"></script>

    %{--<script type="text/javascript" src="../js/SequenceSearch.js"></script>--}%

    <script type="text/javascript">

        var table;
        $(function () {
            $("#login_dialog").dialog({
                draggable: false,
                modal: true,
                autoOpen: false,
                resizable: false,
                closeOnEscape: false
            });
            $("#data_adapter_dialog").dialog({
                draggable: false,
                modal: true,
                autoOpen: false,
                resizable: false,
                closeOnEscape: false
            });
            $("#search_sequences_dialog").dialog({
                draggable: true,
                modal: true,
                autoOpen: false,
                resizable: false,
                closeOnEscape: false,
                width: "auto"
            });
//            table = $("#tracks").dataTable({
//                aaSorting: [[2, "asc"]],
//                aaData: tracks,
//                oLanguage: {
//                    sSearch: "Filter: "
//                },
//                aoColumns: [
//                    {bSortable: false, bSearchable: false},
//                    {sTitle: "Organism", bSortable: false},
//                    {sTitle: "Name", sType: "track-name"},
//                    {sTitle: "Length"}
//                ]
//            });
            $(".adapter_button").button({icons: {primary: "ui-icon-folder-collapsed"}});
            $("#checkbox_menu").menu({});
            $("#menu").menubar({
                        autoExpand: false,
                        select: function (event, ui) {
                            $(".ui-state-focus").removeClass("ui-state-focus");
                        },
                        position: {
                            within: $('#frame').add(window).first()
                        }
                    }
            );
            $("#checkbox_option").change(function () {
                update_checked(this.checked);
            });
            $("#check_all").click(function () {
                update_checked(true);
            });
            $("#check_none").click(function () {
                update_checked(false);
            });
            $("#check_displayed").click(function () {
                $(".track_select").prop("checked", true);
            });
            $(".track_select").click(function () {
                var allChecked = true;
                table.$(".track_select").each(function () {
                    if (!$(this).prop("checked")) {
                        allChecked = false;
                        return false;
                    }
                });
                $("#checkbox_option").prop("checked", allChecked);
            });
            $("#logout_item").click(function () {
                logout();
            });
            $(".data_adapter").click(function () {
                var tracks = new Array();
//                table.$(".track_select").each(function () {
//                    if ($(this).prop("checked")) {
//                        tracks.push($(this).attr("id"));
//                    }
//                });
                write_data($(this).text(), tracks, $(this).attr("_options"));
            });
            $("#search_sequence_item").click(function () {
                open_search_dialog();
            });
            $("#recent_changes").click(function () {
                window.location = "changes";
            });
            $("#user_manager_item").click(function () {
                open_user_manager_dialog();
            });
            $("#web_services_api").click(function () {
                window.open('web_services/web_service_api.html', '_blank');
            });
            $("#apollo_users_guide").click(function () {
                window.open('http://genomearchitect.org/web_apollo_user_guide', '_blank');
            });
            cleanup_user_item();
        });

        function cleanup_logo() {
            $("#logo").parent().css("padding", "0 0 0 0");
        }

        function cleanup_user_item() {
            $("#user_item").parent().attr("id", "user_item_menu");
        }

        function createListener() {
            $.ajax({
                url: "AnnotationChangeNotificationService?track=<%=username + "_" + session.getId()%>",
                success: function () {
                    createListener();
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    var status = jqXHR.status;
                    switch (status) {
                        case 0:
                            if (textStatus == "timeout") {
                                createListener();
                            }
                            break;
                        case 403:
                            alert("Logged out");
                            location.reload();
                            break;
                        case 502:
                            createListener();
                            break;
                        case 504:
                            createListener();
                            break;
                        default:
                            alert("Server connection error");
                            location.reload();
                            break;
                    }
                },
                timeout: 5 * 60 * 1000
            });
        }
        ;

        function write_data(adapter, tracks, options, successMessage) {
            $("#data_adapter_dialog").dialog("option", "closeOnEscape", false);
            $(".ui-dialog-titlebar-close", this.parentNode).hide();

            var enableClose = function () {
                $("#data_adapter_dialog").dialog("option", "closeOnEscape", true);
                $(".ui-dialog-titlebar-close", this.parentNode).show();
            };
            var message = "Writing " + adapter;
            if (tracks.length > 0) {
                var postData = {
                    operation: "write",
                    adapter: adapter,
                    tracks: tracks,
                    options: options
                };
                $.ajax({
                    type: "post",
                    data: JSON.stringify(postData),
                    url: "IOService",
                    beforeSend: function () {
                        $("#data_adapter_loading").show();
                    },
                    success: function (data, textStatus, jqXHR) {
                        var msg = successMessage ? successMessage : data;
                        $("#data_adapter_loading").hide();
                        $("#data_adapter_message").html(msg);
                        enableClose();
                    },
                    error: function (qXHR, textStatus, errorThrown) {
                        $("#data_adapter_message").text("Error writing " + adapter);
                        ok = false;
                        enableClose();
                    }
                });
            }
            else {
                $("#data_adapter_loading").hide();
                message = "No sequences selected";
                enableClose();
            }
            $("#data_adapter_dialog").dialog("open");
            $("#data_adapter_message").text(message);
        }
        ;

        function open_search_dialog() {
            var search = new SequenceSearch(".");
            var starts = new Object();
            search.setRedirectCallback(function (id, fmin, fmax) {
                var flank = Math.round((fmax - fmin) * 0.2);
                var url = 'jbrowse/?loc=' + id + ":" + (fmin - flank) + ".." + (fmax + flank) + "&highlight=" + id + ":" + (fmin + 1) + ".." + fmax;
                window.open(url);
            });
            search.setErrorCallback(function (response) {
                var error = eval('(' + response.responseText + ')');
                if (error && error.error) {
                    alert(error.error);
                }
            });
            var content = search.searchSequence(trackName, null, starts);
            if (content) {
                $("#search_sequences_dialog").show();
                $("#search_sequences_dialog").html(content);
                $("#search_sequences_dialog").dialog("open");
            }
        }
        ;

        function update_checked(checked) {
            $("#checkbox_option").prop("checked", checked);
            table.$(".track_select").prop("checked", checked);
        }
        ;

        function login() {
            var $login = $("#login_dialog");
            $login.dialog("option", "closeOnEscape", false);
            $login.dialog("option", "dialogClass", "login_dialog");
            $(".ui-dialog-titlebar-close", this.parentNode).hide();
            $login.load("Login");
            $login.dialog("open");
            $login.dialog("option", "width", "auto");
        }
        ;

        function logout() {
            $.ajax({
                type: "post",
                url: "Login?operation=logout",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                success: function (data, textStatus, jqXHR) {
                },
                error: function (qXHR, textStatus, errorThrown) {
                }
            });
        }
        ;

        function open_user_manager_dialog() {
            var $userManager = $("<div id='user_manager_dialog' title='Manage users'></div>");
            $userManager.dialog({
                draggable: false,
                modal: true,
                autoOpen: true,
                resizable: false,
                closeOnEscape: true,
                close: function () {
                    $(this).dialog("destroy").remove();
                },
                width: "70%"
            });
//            $userManager.load("userPermissions.jsp", null, function () {
//                $userManager.dialog('option', 'position', 'center');
//            });
            //$userManager.dialog("open");
        }

    </script>
</head>

<body>
<div id="header">
    <ul id="menu">
        <li><a href="http://genomearchitect.org/" target="_blank"><img id="logo"
                                                                       src="../images/ApolloLogo_100x36.png"
                                                                       onload="cleanup_logo()"/></a></li>
        <li><a id="file_item">File</a>
            <ul id="file_menu">
                <li><a id="export_menu">Export</a>
                    <ul>
                    </ul>
                </li>
            </ul>
        </li>

        <li><a id="view_item">View</a>
            <ul id="view_menu">
                <li><a id="recent_changes">Changes</a></li>
            </ul>
        </li>

        <li><a id="tools_item">Tools</a>
            <ul id="tools_menu">
                <li><a id="search_sequence_item">Search sequence</a></li>
            </ul>
        </li>
        %{--<%--}%
        %{--if (isAdmin) {--}%
        %{--%>--}%

        <li><a id="admin_item">Admin</a>
            <ul id="admin_menu">
                <li><a id='user_manager_item'>Manage users</a></li>
            </ul>
        </li>
        %{--<%--}%
        %{--}--}%
        %{--if (username != null) {--}%
        %{--%>--}%
        <li><a id="user_item"><span class='usericon'></span> <%=params.username%></a>
            <ul id="user_menu">
                <li><a id="logout_item">Logout</a></li>
            </ul>
        </li>
        %{--<%--}%
        %{--}--}%
        %{--%>--}%
        <li><a id="help_item">Help</a>
            <ul id="help_menu">
                <li><a id='web_services_api'>Web Services API</a></li>
                <li><a id='apollo_users_guide'>Apollo User's Guide</a></li>
                %{--<li><jsp:include page="version.jsp"></jsp:include></li>--}%
            </ul>
        </li>
    </ul>
</div>

<div id="checkbox_menu_div">
    <ul id="checkbox_menu">
        <li><a><input type="checkbox" id="checkbox_option"/>Select</a>
            <ul>
                <li><a id="check_all">All</a></li>
                <li><a id="check_displayed">Displayed</a>
                <li><a id="check_none">None</a></li>
            </ul>
        </li>
    </ul>
</div>

<div id="search_sequences_dialog" title="Search sequences" style="display:none"></div>
<!--
<div id="user_manager_dialog" title="Manage users" style="display:none"></div>
-->
<div id="data_adapter_dialog" title="Data adapter">
    <div id="data_adapter_loading"><img src="../images/loading.gif"/></div>

    <div id="data_adapter_message"></div>
</div>


<a href="sequences" class="col-offset-4 btn-mini btn-default btn-link">Default Track Select</a>

<div id="login_dialog" title="Login">
</div>

<div id="tracks_div">
    <table id="tracks"></table>
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

</body>
</html>
