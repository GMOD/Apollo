<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <!-- <link rel="stylesheet" type="text/css" href="jslib/DataTables-1.9.4/media/css/demo_table.css" /> -->

    <title>Select track</title>

    <link rel="icon" type="image/x-icon" href="images/webapollo_favicon.ico">
    <link rel="shortcut icon" type="image/x-icon" href="images/webapollo_favicon.ico">

    <link rel="stylesheet" type="text/css" href="styles/selectTrack.css"/>
    <link rel="stylesheet" type="text/css" href="styles/search_sequence.css"/>
    <link rel="stylesheet" type="text/css" href="styles/userPermissions.css"/>
    <link rel="stylesheet" type="text/css" href="css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="css/bootstrap-glyphicons.css"/>

    <link rel="stylesheet" href="jslib/jquery-ui-menubar/jquery.ui.all.css"/>
    <script src="jslib/jquery-ui-menubar/jquery-1.8.2.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.core.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.widget.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.position.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.button.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.menu.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.menubar.js"></script>
    <script src="jslib/jquery-ui-menubar/jquery.ui.dialog.js"></script>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript" src="jslib/DataTables/js/jquery.dataTables.js"></script>
    <script type="text/javascript" src="jslib/DataTables-plugins/dataTablesPlugins.js"></script>


    <script type="text/javascript" src="js/SequenceSearch.js"></script>

    <script type="text/javascript">

        var tracks = new Array();
        if (!!google) {
            google.load("dojo", "1.5");
        }
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
            });
            $("#checkbox_option").change(function () {
                update_checked(this.checked);
            });
            $("#check_all").click(function () {
                update_checked(true);
                $('#all_selected').show();
            });
            $("#check_none").click(function () {
                update_checked(false);
                $('#all_selected').hide();
            });
            $("#check_displayed").click(function () {
                $(".track_select").prop("checked", true);
                $('#all_selected').hide();
            });
            $(".track_select").click(function () {
                var allChecked = true;
                $(".track_select").each(function () {
                    if (!$(this).prop("checked")) {
                        allChecked = false;
                        return false;
                    }
                });
                $("#checkbox_option").prop("checked", allChecked);
            });
            <c:if test="${username==null}">
            login();
            </c:if>
            <c:if test="${username!=null}">
            createListener();
            </c:if>
            $("#logout_item").click(function () {
                logout();
            });
            $(".data_adapter").click(function () {
                var tracks = new Array();
                if ($('#all_selected').is(":visible")) {
                    tracks.push('all_tracks');
                }
                else {
                    $(".track_select").each(function () {
                        if ($(this).prop("checked")) {
                            tracks.push($(this).attr("id"));
                        }
                    });
                }
                for (var t in tracks) {
                    console.log(tracks[t]);
                }
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
            $("#previous-page").click(function () {
                var offset = parseInt($("#offset").val());
                if (offset != 0) {
                    offset = offset - ${maximum};
                    if (offset < 0) offset = 0;
                    $("#offset").val(offset);
                    $('#search-button').click();
                }

            });
            $("#next-page").click(function () {
                var offset = parseInt($("#offset").val());
                offset = offset + ${maximum};
                $("#offset").val(offset);
                $('.search-button').click();
            });
            $("#apollo_users_guide").click(function () {
                window.open('http://genomearchitect.org/web_apollo_user_guide', '_blank');
            });
            cleanup_user_item();
        });

        function cleanup_logo() {
            $("#logo").parent().css("padding", "0 0 0 0");
        }
        ;

        function cleanup_user_item() {
            $("#user_item").parent().attr("id", "user_item_menu");
        }
        ;

        function createListener() {
            $.ajax({
                url: "AnnotationChangeNotificationService?track=${username}_${sessionScope.get("id")}",
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
            if(tracks[0]=="all_tracks") {
                var postData = {
                    operation: "write",
                    adapter: adapter,
                    options: options,
                    tracks: "all_tracks"
                };
                url = $.param(postData);
                console.log(url);
                $.ajax({
                    type: "get",
                    url: "IOService?"+url,
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
            else if (tracks.length > 0) {
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
            <c:forEach var="track" items="${tracks}">
            var trackName = "${track.getName()}";
            </c:forEach>
            var search = new SequenceSearch(".");
            var starts = new Object();
            <c:forEach var="track" items="${tracks}">
            starts['${track.getSourceFeature().getUniqueName()}'] = ${track.getSourceFeature().getStart()};
            </c:forEach>
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
            $(".track_select").prop("checked", checked);
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
                    "Content-Type": "application/x-www-form-urlencoded",
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
            $userManager.load("userPermissions.jsp", null, function () {
                $userManager.dialog('option', 'position', 'center');
            });
            //$userManager.dialog("open");
        }

    </script>
</head>
<body>
<div id="header">
    <ul id="menu">
        <li><a href="http://genomearchitect.org/" target="_blank"><img id="logo" src="images/ApolloLogo_100x36.png"
                                                                       onload="cleanup_logo()" alt=""/></a></li>
        <li><a id="file_item">File</a>
            <ul id="file_menu">
                <li><a id="export_menu">Export</a>
                    <ul>
                        <c:forEach var="adapterGroup" items="${dataAdapters}">
                            <c:if test="${adapterGroup.isGroup()}">
                                <li><a>Group: ${adapterGroup.getKey()}</a>
                                <ul>
                            </c:if>
                            <c:forEach var="adapter" items="${adapterGroup.dataAdapters}">
                                <li>
                                    <a class='data_adapter' _options='${adapter.getOptions()}'>${adapter.getKey()}</a>
                                </li>
                            </c:forEach>
                            <c:if test="${adapterGroup.isGroup()}">
                                </ul></li>
                            </c:if>
                        </c:forEach>
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
        <c:if test="${isAdmin}">

            <li><a id="admin_item">Admin</a>
                <ul id="admin_menu">
                    <li><a id='user_manager_item'>Manage users</a></li>
                </ul>
            </li>
        </c:if>
        <c:if test="${username!=null}">
            <li><a id="user_item"><span class='usericon'></span> ${username}
            </a>
                <ul id="user_menu">
                    <li><a id="logout_item">Logout</a></li>
                </ul>
            </li>
        </c:if>
        <li><a id="help_item">Help</a>
            <ul id="help_menu">
                <li><a id='web_services_api'>Web Services API</a></li>
                <li><a id='apollo_users_guide'>Apollo User's Guide</a></li>
                <li>
                    <jsp:include page="version.jsp"></jsp:include>
                </li>
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
<div id="all_selected" style="display: none;" class="label-info label">All Selected</div>
<div id="search_sequences_dialog" title="Search sequences" style="display:none"></div>
<div id="data_adapter_dialog" title="Data adapter">
    <div id="data_adapter_loading"><img src="images/loading.gif"/></div>
    <div id="data_adapter_message"></div>
</div>
<div id="login_dialog" title="Login">
</div>
<form action="sequences" method="get">
    <div class="row">
        <div class="col-2">&nbsp;&nbsp;Showing&nbsp;${trackViews.size()} of ${trackCount}<br/><em>
            &nbsp;&nbsp;${organism}</em></div>
        <input type="button" class="btn btn-mini col-1 ${!hasPrevious? 'disabled' :''}" href="#" id="previous-page"
               value="&larr; Previous">
        <input type="text" class="col-1" name="offset" id="offset" value="${offset==null ? '0' : offset}">
        <input type="button" class="btn btn-mini col-1 ${!hasNext ? 'disabled' :''}" href="#" id="next-page"
               value="Next &rarr;">
        <input type="submit" value="Search" class="search-button col-offset-1 btn ui-icon-search btn-default col-1">
        <a href="selectTrack.jsp" class="col-offset-2 col-2 btn-mini btn-default btn-link">Older Track Select Page<br>(smaller
            data only)</a>
    </div>
    <table class="table">
        <thead>
        <tr>
            <td>
                <div class="col-2-lg">Show</div>
                <select name="maximum" class="col-2-lg">
                <option ${maximum=='10' ? 'selected=true' : ''}>10</option>
                <option ${maximum=='25' ? 'selected=true' : ''}>25</option>
                <option ${maximum=='100' ? 'selected=true' : ''}>100</option>
                <option ${maximum=='1000' ? 'selected=true' : ''}>1000</option>
            </select>
                <input type="submit" value="Go" class="search-button col-offset-1 btn btn-small ui-icon-search btn-default col-2-lg">

            </td>
            <%--<th><input type="text" name="organism" value="${organism}"/></th>--%>
            <th><input type="text" name="name" value="${name}"></th>
            <th><input type="text" name="minLength" value="${minLength}">-<input type="text" name="maxLength"
                                                                                 value="${maxLength}"></th>
        </tr>
        <tr>
            <th></th>
            <th>Name</th>
            <th>Length</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="track" items="${trackViews}" varStatus="iter">
            <tr>
                <c:forEach var="col" items="${track}" varStatus="innerIter">
                    <td>
                        ${col}
                    </td>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</form>
</body>
</html>

