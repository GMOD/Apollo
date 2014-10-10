<%@ page import="org.bbop.apollo.web.config.ServerConfiguration" %>
<%@ page import="org.bbop.apollo.web.datastore.JEDatabase" %>
<%@ page import="org.bbop.apollo.web.datastore.history.JEHistoryDatabase" %>
<%@ page import="org.bbop.apollo.web.datastore.history.Transaction" %>
<%@ page import="org.bbop.apollo.web.user.Permission" %>
<%@ page import="org.bbop.apollo.web.user.UserManager" %>
<%@ page import="org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature" %>
<%@ page import="org.gmod.gbol.bioObject.conf.BioObjectConfiguration" %>
<%@ page import="org.gmod.gbol.bioObject.util.BioObjectUtil" %>
<%@ page import="org.gmod.gbol.simpleObject.Feature" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.util.*" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>

<%
    ServerConfiguration serverConfig = new ServerConfiguration(getServletContext());
    InputStream gbolMappingStream = getServletContext().getResourceAsStream(serverConfig.getGBOLMappingFile());

    Set<String> allStatusList = new TreeSet<String>();

    for (ServerConfiguration.AnnotationInfoEditorConfiguration annotationInfoEditorConfiguration : serverConfig.getAnnotationInfoEditor().values()) {
        allStatusList.addAll(annotationInfoEditorConfiguration.getStatus());
    }

    BioObjectConfiguration bioObjectConfiguration = new BioObjectConfiguration(gbolMappingStream);
    if (!UserManager.getInstance().isInitialized()) {
        ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
        UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
    }
    String databaseDir = serverConfig.getDataStoreDirectory();
    String username = (String) session.getAttribute("username");
    Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);
%>

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

<!--
<link rel="stylesheet" type="text/css" href="styles/selectTrack.css" />
<link rel="stylesheet" type="text/css" href="styles/search_sequence.css" />
<link rel="stylesheet" type="text/css" href="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.css" />
<link rel="stylesheet" type="text/css" href="http://view.jqueryui.com/menubar/themes/base/jquery.ui.menubar.css" />
<script type="text/javascript" src="jslib/jquery-1.7.1.min.js"></script>
<script type="text/javascript" src="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.min.js"></script>


<script type="text/javascript" src="http://view.jqueryui.com/menubar/ui/jquery.ui.menubar.js"></script>
-->
<script type="text/javascript">


if (google) {
    google.load("dojo", "1.5");
}


<%
BufferedReader in = new java.io.BufferedReader(new InputStreamReader(application.getResourceAsStream(serverConfig.getTrackNameComparator())));
String line;
while ((line = in.readLine()) != null) {
    out.println(line);    
}
%>

var recent_changes = new Array();

<%!
/** Generate a record for a feature that includes the name, type, link to browser, and last modified date.
 *
 * @return String representation of the record in JSON format
 */
private String generateFeatureRecordJSON(AbstractSingleLocationBioFeature feature,ServerConfiguration.TrackConfiguration track, JEHistoryDatabase historyDataStore) {
    String builder="";
    long flank=Math.round((feature.getFmax()-feature.getFmin())*0.5);
    long left=Math.max(feature.getFmin()-flank,1);
    long right=Math.min(feature.getFmax()+flank,track.getSourceFeature().getSequenceLength()-1);


    Transaction t=historyDataStore.getCurrentTransactionForFeature(feature.getUniqueName());
    
    
    builder+=String.format("['<input type=\"checkbox\" class=\"track_select\" id=\"%s\"/>',", track.getName()+"<=>"+feature.getUniqueName());
    builder+=String.format("'%s',",track.getSourceFeature().getUniqueName());
    if(feature.getName()==null || feature.getName().trim().length()==0){
        builder+=String.format("'---- <a target=\"_blank\" href=\"jbrowse/?loc=%s:%d..%d\">%s</a> ----',",
            track.getSourceFeature().getUniqueName(), left, right, "unassigned");
    }
    else{
        builder+=String.format("'<a target=\"_blank\" href=\"jbrowse/?loc=%s:%d..%d\">%s</a>',",
            track.getSourceFeature().getUniqueName(), left, right, feature.getName());
    }
    builder+=String.format("'%s',", feature.getType().split(":")[1]);
    builder+=String.format("'%s',", feature.getTimeLastModified());
    builder+=String.format("'%s',", t!=null?t.getEditor():feature.getOwner().getOwner());
    builder+=String.format("'%s',", feature.getOwner().getOwner());
    builder+=String.format("'%s']", feature.getStatus()==null ? "" : feature.getStatus().getStatus());
    return builder;
}

/** Generate a list of records for a feature that may include subfeatures
*
* @return non-empty ArrayList of Strings with records in JSON format
*/
private ArrayList<String> generateFeatureRecord(AbstractSingleLocationBioFeature feature, ServerConfiguration.TrackConfiguration track, JEHistoryDatabase historyDataStore) {
    ArrayList<String> builder=new ArrayList<String>();
    String type=feature.getType().split(":")[1];


    for (AbstractSingleLocationBioFeature subfeature : feature.getChildren()) {
        builder.add(generateFeatureRecordJSON(subfeature,track, historyDataStore));
    }
    builder.add(generateFeatureRecordJSON(feature,track, historyDataStore));
    return builder;
}

%>

<%
int maximum = 100 ;
int count  =0 ;
Collection<ServerConfiguration.TrackConfiguration> tracks = serverConfig.getTracks().values();
System.out.println("# of tracks");
boolean isAdmin = false;
if (username != null) {
    for (ServerConfiguration.TrackConfiguration track : tracks) {
        Integer permission = permissions.get(track.getName());
        System.out.println("count ["+count+"] / maximum ["+maximum +"]");
        if (permission == null && count < maximum) {
            permission = 0;
        }
        if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
            isAdmin = true;
        }
        if ((permission & Permission.READ) == Permission.READ) {
            Collection<Feature> features = new ArrayList<Feature>();
            Collection<Feature> sequence_alterations = new ArrayList<Feature>();
            String my_database = databaseDir + "/"+ track.getName();

            //check that database exists
            File database = new File(my_database);
            if (!database.exists()) {
                continue;
            }
            System.out.println("database exists: "+my_database );
            File databaseHistory = new File(my_database+"_history");
            System.out.println("database histry exists: "+databaseHistory.exists());
            // load database
            JEDatabase dataStore = new JEDatabase(my_database,false);


            try {
            JEHistoryDatabase historyDataStore = new JEHistoryDatabase(my_database+"_history",false,0);

            dataStore.readFeatures(features);
            Iterator<Feature> featureIterator = features.iterator();
            while(featureIterator.hasNext() && count < maximum ) {
                Feature feature = featureIterator.next();
                // use list of records to get objects that have subfeatures
                AbstractSingleLocationBioFeature gbolFeature=(AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(feature, bioObjectConfiguration);
                ArrayList<String> record = generateFeatureRecord(gbolFeature, track, historyDataStore);
                for (String s : record) {
                    out.println("eecent_changes.push(" + s + ");\n");
                    ++count ;
                }
            }
            dataStore.readSequenceAlterations(sequence_alterations);
            featureIterator = sequence_alterations.iterator();
            while (featureIterator.hasNext() && count < maximum) {
                Feature feature = featureIterator.next();
                // use list of records to get objects that have subfeatures
                AbstractSingleLocationBioFeature gbolFeature=(AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(feature, bioObjectConfiguration);
                ArrayList<String> record = generateFeatureRecord(gbolFeature, track, historyDataStore);
                for (String s : record) {
                    out.println("recent_changes.push(" + s + ");\n");
                    ++count ;
                }
            }} catch (Exception e) {
            System.err.println("Unable to read database history: "+my_database+"_history:\n"+e);
}
        }
    }
}

%>


var table;
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
    table = $("#recent_changes").dataTable({
        aaData: recent_changes,
        aaSorting: [[4, "desc"]],
        oLanguage: {
            sSearch: "Filter: "
        },
        aoColumns: [
            {bSortable: false, bSearchable: false},
            {sTitle: "Track", bSortable: true},
            {sTitle: "Feature name", bSortable: true},
            {sTitle: "Feature type", bSortable: true},
            {sTitle: "Last modified", bSortable: true},
            {sTitle: "Editor", bSortable: true},
            <%
            if(allStatusList.size()>0){
            %>
            {sTitle: "Owner", bSortable: true},
            {sTitle: "Status", bSortable: true}
            <%
            }
            else{
            %>
            {sTitle: "Owner", bSortable: true}
            <%
        }
        %>

        ]
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
    <%
        if (username == null) {
            out.println("login();");
        }
        else {
            out.println("createListener();");
        }
    %>
    $("#logout_item").click(function () {
        logout();
    });
    $(".data_adapter").click(function () {
        var tracks = new Array();
        table.$(".track_select").each(function () {
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
    $("#apollo_users_guide").click(function () {
        window.open('http://genomearchitect.org/web_apollo_user_guide', '_blank');
    });
    $("#delete_selected_item").click(function () {
        delete_selected_items();
    });

    <%
for(String status : allStatusList){
%>
    $("#change_status_selected_item-<%=status.replaceAll(" ","_")%>").click(function () {
        change_status_selected_items('<%=status%>');
    });

    <%
    }
    %>

    cleanup_user_item();
});

function change_status_selected_items(updated_status) {
    var trackName = "";
    var tracks = new Array();
    table.$(".track_select").each(function () {
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
;

function delete_selected_items() {
    var trackName = "";
    var tracks = new Array();
    table.$(".track_select").each(function () {
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
;

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
    <%
        for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {
            Integer permission = permissions.get(track.getName());
            if (permission == null) {
                permission = 0;
            }
            if ((permission & Permission.READ) == Permission.READ) {
                out.println("var trackName = '" + track.getName() + "'");
                break;
            }

        }
    %>
    var search = new SequenceSearch(".");
    var starts = new Object();
    <%
        for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {
            out.println(String.format("starts['%s'] = %d;", track.getSourceFeature().getUniqueName(), track.getSourceFeature().getStart()));
        }
    %>
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
                        <%
                            /*
                                for (DataAdapterGroupConfiguration groupConf : serverConfig.getDataAdapters().values()) {
                                    if (groupConf.isGroup()) {
                                        out.println(String.format("\t\t\t\t\t<li><a>%s</a>", groupConf.getKey()));
                                        out.println("<ul>");
                                        for (DataAdapterConfiguration conf : groupConf.getDataAdapters()) {
                                            out.println(String.format("\t\t\t\t\t\t<li><a class='data_adapter' _options='%s'>%s</a></li>", conf.getOptions(), conf.getKey()));
                                        }
                                        out.println("</ul></li>");
                                    }
                                    else {
                                        for (DataAdapterConfiguration conf : groupConf.getDataAdapters()) {
                                            out.println(String.format("\t\t\t\t\t<li><a class='data_adapter' _options='%s'>%s</a></li>", conf.getOptions(), conf.getKey()));
                                        }
                                    }
                                }

                                for (Map.Entry<String, String> dataAdapter : dataAdapters.entrySet()) {
                                    out.println(String.format("\t\t\t\t\t<li><a class='data_adapter' _options='%s'>%s</a></li>", dataAdapter.getValue(), dataAdapter.getKey()));
                                }
                            */
                        %>
                    </ul>
                </li>
            </ul>
        </li>

        <li><a id="view_item">View</a>
            <ul id="view_menu">
                <li><a id="select_tracks">Select tracks</a></li>
                <%--<li><a id="genes">Genes</a></li>--%>
            </ul>
        </li>

        <li><a id="tools_item">Tools</a>
            <ul id="tools_menu">
                <li><a id="search_sequence_item">Search sequence</a></li>
            </ul>
        </li>


        <%
            if (isAdmin) {
        %>
        <li><a id="admin_item">Admin</a>
            <ul id="admin_menu">
                <li><a id='user_manager_item'>Manage users</a></li>
                <li type='separator'></li>
                <li><a id='delete_selected_item'>Delete selected</a></li>
                <% if (allStatusList.size() > 0) {
                %>
                <li><a>Change status of selected</a>
                    <ul>
                            <%
                for(String status : allStatusList){
                    %>
                        <li><a class='none' id="change_status_selected_item-<%=status.replaceAll(" ","_")%>"><%=status%>
                        </a></li>
                            <%
                }
            %>
                </li>
            </ul>
        </li>
        <%
            }
        %>

    </ul>
    </li>
    <%
        }
        if (username != null) {
    %>
    <li><a id="user_item"><span class='usericon'></span><%=username%>
    </a>
        <ul id="user_menu">
            <li><a id="logout_item">Logout</a></li>
        </ul>
    </li>
    <%
        }
    %>
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
<a href="recentChanges" class="col-offset-4 btn-mini btn-default btn-link">Default Recent Changes</a>
<div id="login_dialog" title="Login">
</div>
<div id="recent_changes_div">
    <table id="recent_changes"></table>
</div>
</body>
</html>
