<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">


<!-- <link rel="stylesheet" type="text/css" href="jslib/DataTables-1.9.4/media/css/demo_table.css" /> -->

<title>Recent changes</title>

<link rel="icon" type="image/x-icon" href="images/webapollo_favicon.ico">
<link rel="shortcut icon" type="image/x-icon" href="images/webapollo_favicon.ico">

<link rel="stylesheet" type="text/css" href="styles/recentChanges.css"/>
<link rel="stylesheet" type="text/css" href="styles/search_sequence.css"/>
<link rel="stylesheet" type="text/css" href="styles/userPermissions.css"/>

<link rel="stylesheet" type="text/css" href="css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="css/bootstrap-glyphicons.css"/>

<link rel="stylesheet" href="jslib/jquery-ui-menubar/jquery.ui.all.css"/>
<script src="jslib/jquery-ui-menubar/jquery-1.8.2.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.core.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.widget.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.position.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.button.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.menu.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.menubar.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.dialog.js" type="text/javascript"></script>
<script src="https://www.google.com/jsapi" type="text/javascript"></script>


<script type="text/javascript" src="jslib/DataTables/js/jquery.dataTables.js"></script>
<script type="text/javascript" src="jslib/DataTables-plugins/dataTablesPlugins.js"></script>

<script type="text/javascript" src="js/SequenceSearch.js"></script>

<script type="text/javascript">


if (google) {
    google.load("dojo", "1.5");
}

<%--var recent_changes = new Array();--%>

<%--<c:forEach var="change" items="${changes}">--%>
<%--recent_changes.push(${change});--%>
<%--</c:forEach>--%>

//var table;
$(function () {
    $("#login_dialog").dialog({draggable: false, modal: true, autoOpen: false, resizable: false, closeOnEscape: false});
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
    $("#unassigned-group-filter").change(function () {
        if(this.checked){
            $("#group-filter").val("Unassigned");
        }
        else{
            $("#group-filter").val("");
        }
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
        $(".track_select").each(function () {
            if ($(this).prop("checked")) {
                tracks.push($(this).attr("id"));
            }
        });
        write_data($(this).text(), tracks, $(this).attr("_options"));
    });

    $("#select_tracks").click(function () {
        window.location = "selectTrack";
    });

    $("#genes").click(function () {
        window.location = "genes.jsp";
    });

    $("#search_sequence_item").click(function () {
        open_search_dialog();
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
            $("#offset").val(offset);
            $('#search-button').click();
        }

    });
    $("#next-page").click(function () {
        var offset = parseInt($("#offset").val());
//        if(offset!='0'){
        offset = offset + ${maximum};
        $("#offset").val(offset);
        $('#search-button').click();
//        }
    });
    $("#apollo_users_guide").click(function () {
        window.open('http://genomearchitect.org/web_apollo_user_guide', '_blank');
    });
    $("#delete_selected_item").click(function () {
        delete_selected_items();
    });

    <c:forEach var="status" items="${allStatusList}">
    $("#change_status_selected_item-${status.replaceAll(" ","_")}").click(function () {
        change_status_selected_items('${status}');
    });
    </c:forEach>
    cleanup_user_item();
});

function change_status_selected_items(updated_status) {
    var trackName = "";
    var tracks = new Array();
    $(".track_select").each(function () {
        if ($(this).prop("checked")) {
            var wholeId = $(this).attr("id");
            trackName = wholeId.split("<=>")[0];
            var featureId = wholeId.split("<=>")[1];
            tracks.push(featureId);
        }
    });
    var trackString = "\"features\": [";
    for (var i = 0; i < tracks.length; i++) {
        if (i > 0) {
            trackString += ',';
        }
        trackString += "{ \"uniquename\": \"" + tracks[i] + "\",\"status\":\"" + updated_status + "\" }";
    }
    trackString += ']';

//    var doDelete = confirm("Are you sure you want to update "+tracks.length+" annotations?");
//    if(doDelete){
    var postData = '{ "track": "' + trackName + '", ' + trackString + ', "operation": "set_status" }';
    $.ajax({
        type: "post",
        data: postData,
        url: "AnnotationEditorService",
        success: function (data, textStatus, jqXHR) {
            console.log('success');
            window.location = "recentChanges";

        },
        error: function (qXHR, textStatus, errorThrown) {
            console.log('error');
            alert('Error updating status: ' + errorThrown);
            ok = false;
        }
    });
//    }
}

function delete_selected_items() {
    var trackName = "";
    var tracks = new Array();
    $(".track_select").each(function () {
        if ($(this).prop("checked")) {
            var wholeId = $(this).attr("id");
            trackName = wholeId.split("<=>")[0];
            var featureId = wholeId.split("<=>")[1];
            tracks.push(featureId);
        }
    });
    var trackString = "\"features\": [";
    for (var i = 0; i < tracks.length; i++) {
        if (i > 0) {
            trackString += ',';
        }
        trackString += "{ \"uniquename\": \"" + tracks[i] + "\" }"
    }
    trackString += ']';

    var doDelete = confirm("Are you sure you want to delete " + tracks.length + " annotations?");
    if (doDelete) {
        var postData = '{ "track": "' + trackName + '", ' + trackString + ', "operation": "delete_feature" }';

        $.ajax({
            type: "post",
            data: postData,
            url: "AnnotationEditorService",
            success: function (data, textStatus, jqXHR) {
                console.log('success');
//                    alert('Deleted '+tracks.size() + ' successfully.');
                window.location = "recentChanges";

            },
            error: function (qXHR, textStatus, errorThrown) {
                console.log('error');
                alert('Error deleting: ' + errorThrown);
                ok = false;
            }
        });
    }
}

function cleanup_logo() {
    $("#logo").parent().css("padding", "0 0 0 0");
}

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
    <c:forEach var="track" items="${tracks}">
    var trackName = "${track.getName()}";
    </c:forEach>
    <%--<%--%>
    <%--for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {--%>
    <%--Integer permission = permissions.get(track.getName());--%>
    <%--if (permission == null) {--%>
    <%--permission = 0;--%>
    <%--}--%>
    <%--if ((permission & Permission.READ) == Permission.READ) {--%>
    <%--out.println("var trackName = '" + track.getName() + "'");--%>
    <%--break;--%>
    <%--}--%>

    <%--}--%>
    <%--%>--%>
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
        <li><a href="http://genomearchitect.org/" target="_blank"><img id="logo" src="images/ApolloLogo_100x36.png" onload="cleanup_logo()" alt=""/></a></li>
        <li><a id="file_item">File</a>
            <ul id="file_menu">
                <li><a id="export_menu">Export</a>
                    <ul>
                        <li><a class='none'>N/A</a></li>
                    </ul>
                </li>
            </ul>
        </li>

        <li><a id="view_item">View</a>
            <ul id="view_menu">
                <li><a id="select_tracks">Select tracks</a></li>
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
                    <li type='separator'></li>
                    <li><a id='delete_selected_item'>Delete selected</a></li>
                    <li><a>Change status of selected</a>
                        <ul>
                            <c:forEach var="status" items="${allStatusList}">
                                <li><a class='none'
                                       id="change_status_selected_item-${status.replaceAll(" ","_")}">${status}
                                </a></li>
                            </c:forEach>
                        </ul>
                    </li>
                </ul>
            </li>
        </c:if>

        <%--</ul>--%>
        <%--</li>--%>
        <c:if test="${username!=null}">
            <li><a id="user_item"><span class='usericon'></span>${username}
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
                </li>
                <li><a id="check_none">None</a></li>
            </ul>
        </li>
    </ul>
</div>
<div id="search_sequences_dialog" title="Search sequences" style="display: none"></div>
<!--
<div id="user_manager_dialog" title="Manage users" style="display:none"></div>
-->
<div id="data_adapter_dialog" title="Data adapter">
    <div id="data_adapter_loading"><img src="images/loading.gif" alt=""/></div>
    <div id="data_adapter_message"></div>
</div>
<div id="login_dialog" title="Login">
</div>

<form action="recentChanges" method="get">
<div class="row form-group">
    <div class="col-3"><h4>&nbsp;&nbsp;Scanned &nbsp;${tracks.size()} of ${trackCount} tracks</h4></div>
    <input type="button" class="btn btn-mini col-1" href="#" id="previous-page" value="&larr; Previous">
    <input type="text" class="col-1" name="offset" id="offset" value="${offset==null ? '0' : offset}">
    <input type="button" class="btn btn-mini col-1" href="#" id="next-page" value="Next &rarr;">
    <%--<input type="submit" value="Search" class="btn ui-icon-search btn-default col-1">--%>
    <input type="submit" id="search-button" value="Search" class=" col-offset-1 btn ui-icon-search btn-default col-1">
    <a href="recentChanges.jsp" class="col-offset-4 btn-mini btn-default btn-link">Older Recent Changes (smaller data only)</a>
</div>
<table class="table">
    <thead>
    <tr>
        <td>
            Show&nbsp;<select name="maximum">
            <option ${maximum=='10' ? 'selected=true' : ''}>10</option>
            <option ${maximum=='25' ? 'selected=true' : ''}>25</option>
            <option ${maximum=='100' ? 'selected=true' : ''}>100</option>
            <option ${maximum=='1000' ? 'selected=true' : ''}>1000</option>
        </select>

        </td>
        <th>
            <select name="track">
                <option value="">All</option>
                <c:forEach var="trackFilter" items="${allTrackNames}">
                    <c:set var="trackLabel" value="${trackFilter.replaceAll('Annotations-','')}"/>
                    <option ${track.contains(trackLabel)?'selected':''} value="${trackLabel}">${trackLabel.length()>23 ? trackLabel.substring(0,20).concat("...") : trackLabel}</option>
                </c:forEach>
            </select>
        </th>
        <th><input type="text" id="group-filter" name="group" value="${group}"/>
            <br/>
            Unassigned
        <input type="checkbox" id="unassigned-group-filter" name="unassigned-group" ${group eq 'Unassigned' ? 'checked' : ''}>
        </th>
        <th>
            <select name="type">
                <option value="">All</option>
                <c:forEach var="typeIter" items="${types}">
                    <option  ${typeIter==type ? 'selected' : ''}>${typeIter}</option>
                </c:forEach>
            </select>
        </th>
        <th>
            Days
            <select name="days_filter_logic">
                <option >-----</option>
                <option ${days_filter_logic=='Before' ? 'selected' : ''}>Before</option>
                <option ${days_filter_logic=='After' ? 'selected' : ''}>After</option>
                <%--<option ${days_filter_logic=='Equals' ? 'selected' : ''}>Equals</option>--%>
            </select>
            <input type="text" name="days_filter" value="${days_filter}">
        </th>
        <%--<th><input type="text" name="editor"></th>--%>
        <th><input type="text" name="owner" value="${owner}"></th>
        <th>
            <select name="status">
                <option value="">All</option>
                <option>None</option>
                <c:forEach var="statusIter" items="${allStatusList}">
                    <option ${statusIter==status ? 'selected' : ''}>${statusIter}</option>
                </c:forEach>
            </select>
        </th>
    </tr>
    <tr>
        <th>Select</th>
        <th>Track</th>
        <th>Group</th>
        <th>Type</th>
        <th>Date</th>
        <%--<th>Editor</th>--%>
        <th>Owner</th>
        <th>Status</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="change" items="${changes}" varStatus="iter">
    <tr>
            <c:forTokens var="col" items="${change.replaceAll('\\\\[|\\\\]','')}" delims="," varStatus="innerIter">
        <td>
                <c:if test="${innerIter.first}">${iter.count + offset}</c:if>
                ${col.replaceAll("'","")}
                        </td>
                        </c:forTokens>
                        </tr>
            </c:forEach>

    </tbody>
</table>
</form>

<%--<div id="recent_changes_div">--%>
<%--<table id="recent_changes"></table>--%>
<%--</div>--%>
</body>
</html>
