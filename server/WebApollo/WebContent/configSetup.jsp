<%@page import="org.gmod.gbol.bioObject.util.BioObjectUtil"%>
<%@page import="org.gmod.gbol.bioObject.conf.BioObjectConfiguration"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="org.bbop.apollo.web.config.ServerConfiguration.DataAdapterConfiguration"%>
<%@page import="org.bbop.apollo.web.config.ServerConfiguration.DataAdapterGroupConfiguration"%>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration"%>
<%@ page import="org.bbop.apollo.web.user.UserManager"%>
<%@ page import="org.bbop.apollo.web.user.Permission"%>

<%@ page import="java.util.Map"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Collection"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Collections"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.TreeSet"%>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.io.InputStream" %>

<%
ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));
InputStream gbolMappingStream = getServletContext().getResourceAsStream(serverConfig.getGBOLMappingFile());
BioObjectConfiguration bioObjectConfiguration = new BioObjectConfiguration(gbolMappingStream);
if (!UserManager.getInstance().isInitialized()) {
	ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
	UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
}
String databaseDir = serverConfig.getDataStoreDirectory();
String username = (String)session.getAttribute("username");
Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<!-- <link rel="stylesheet" type="text/css" href="jslib/DataTables-1.9.4/media/css/demo_table.css" /> -->

<title>Configuration setup</title>

<link rel="icon" type="image/x-icon" href="images/webapollo_favicon.ico">
<link rel="shortcut icon" type="image/x-icon" href="images/webapollo_favicon.ico">

<link rel="stylesheet" type="text/css" href="styles/recentChanges.css" />
<link rel="stylesheet" type="text/css" href="styles/search_sequence.css" />
<link rel="stylesheet" type="text/css" href="styles/userPermissions.css" />

<link rel="stylesheet" href="jslib/jquery-ui-menubar/jquery.ui.all.css" />
<script src="jslib/jquery-ui-menubar/jquery-1.8.2.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.core.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.widget.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.position.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.button.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.menu.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.menubar.js" type="text/javascript"></script>
<script src="jslib/jquery-ui-menubar/jquery.ui.dialog.js" type="text/javascript"></script>

<script type="text/javascript" src="jslib/DataTables/js/jquery.dataTables.js"></script>
<script type="text/javascript" src="jslib/DataTables-plugins/dataTablesPlugins.js"></script>
<script type="text/javascript" src="<%=new URL(request.getRequestURL().toString()).getProtocol()%>://www.google.com/jsapi"></script>

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

<%
BufferedReader in = new BufferedReader(new InputStreamReader(application.getResourceAsStream(serverConfig.getTrackNameComparator())));
String line;
while ((line = in.readLine()) != null) {
	out.println(line);	
}
%>

<%
Collection<ServerConfiguration.TrackConfiguration> tracks = serverConfig.getTracks().values();
boolean isAdmin = false;
if (username != null) {
	for (ServerConfiguration.TrackConfiguration track : tracks) {
		Integer permission = permissions.get(track.getName());
		if (permission == null) {
			permission = 0;
		}
		if ((permission & Permission.USER_MANAGER) == Permission.USER_MANAGER) {
			isAdmin = true;
		}
	}
}

%>


var recent_changes = new Array();


google.load("dojo", "1.5");
var table;
$(function() {
	$("#menu").menubar( {
		autoExpand: false,
		select: function(event, ui) {
			$(".ui-state-focus").removeClass("ui-state-focus");
		},
		position: {
        	within: $('#frame').add(window).first() }
		}
	);
<%
	if (username == null) {
		out.println("login();");
	}
%>
	$("#logout_item").click(function() {
		logout();
	});

	$("#select_tracks").click(function() {
		window.location="selectTrack.jsp";
	});
	$("#recent_changes").click(function() {
		window.location="recentChanges.jsp";
	});
	
	$("#search_sequence_item").click(function() {
		open_search_dialog();
	});
	$("#user_manager_item").click(function() {
		open_user_manager_dialog();
	});
	
	initialize_config_settings();

	cleanup_user_item();
} );


function initialize_config_settings() {
	$("#datastore_directory").attr("value","<% out.print(serverConfig.getDataStoreDirectory()); %>");
	$('#webapollo_config_form').submit(function(){
		serverConfig.setDataStoreDirectory($("#datastore_directory").text());
	});
	

	$("#user_database").attr("value","<% out.print(serverConfig.getUserDatabase()); %>");
}




function cleanup_logo() {
	$("#logo").parent().css("padding", "0 0 0 0");
};

function cleanup_user_item() {
	$("#user_item").parent().attr("id", "user_item_menu");
};



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
	search.setRedirectCallback(function(id, fmin, fmax) {
		 var flank = Math.round((fmax - fmin) * 0.2);
		 var url = 'jbrowse/?loc=' + id + ":" + (fmin-flank) + ".." + (fmax+flank)+"&highlight="+id+":"+(fmin+1) + ".." + fmax;
		 window.open(url);
	});
	search.setErrorCallback(function(response) {
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
};

function update_checked(checked) {
	$("#checkbox_option").prop("checked", checked);
	table.$(".track_select").prop("checked", checked);
};

function login() {
	var $login = $("#login_dialog");
	$login.dialog("option", "closeOnEscape", false);
	$login.dialog("option", "dialogClass", "login_dialog");
	$(".ui-dialog-titlebar-close", this.parentNode).hide();
	$login.load("Login");
	$login.dialog("open");
	$login.dialog("option", "width", "auto");
};

function logout() {
	$.ajax({
		type: "post",
		url: "Login?operation=logout",
		success: function(data, textStatus, jqXHR) {
		},
		error: function(qXHR, textStatus, errorThrown) {
		}
	});
};

function open_user_manager_dialog() {
	var $userManager = $("<div id='user_manager_dialog' title='Manage users'></div>");
	$userManager.dialog( {
		draggable: false,
		modal: true,
		autoOpen: true,
		resizable: false,
		closeOnEscape: true,
		close: function() {
			$(this).dialog("destroy").remove();
		},
		width: "70%"
		} );
	$userManager.load("userPermissions.jsp", null, function() {
		$userManager.dialog('option', 'position', 'center');
	});
	//$userManager.dialog("open");
}

</script>
</head>
<body>
<div id="header">
<ul id="menu">
	<li><a><img id="logo" src="images/ApolloLogo_100x36.png" onload="cleanup_logo()" alt=""/></a></li>
	<li><a id="file_item">File</a>
		<ul id="file_menu">
			<li><a id="export_menu">Export</a>
			<ul><li><a class='none'>N/A</a></li>

			</ul>
			</li>
		</ul>
	</li>
	
	<li><a id="view_item">View</a>
		<ul id="view_menu">
			<li><a id="select_tracks">Select tracks</a></li>
			<li><a id="recent_changes">Recent changes</a></li>
		</ul>
	</li>
	
	<li><a id="tools_item">Tools</a>
		<ul id="tools_menu">
			<li><a id="search_sequence_item">Search sequence</a></li>
		</ul>
	</li>
<%
	if (isAdmin) {
		out.println("<li><a id=\"admin_item\">Admin</a>");
		out.println("<ul id=\"tools_menu\">");
		out.println("\t<li><a id='user_manager_item'>Manage users</a></li>");
		out.println("</ul>");
		out.println("</li>");
	}
	if (username != null) {
		out.println("<li><a id=\"user_item\"><span class='usericon'></span>" + username + "</a>");
		out.println("<ul id=\"user_menu\">");
		out.println("\t<li><a id=\"logout_item\">Logout</a></li>");
		out.println("</ul>");
		out.println("</li>");
	}
%>
</ul>
</div>




<div id="login_dialog" title="Login">
</div>

<form id="webapollo_config_form" >
<label>Annotations data store directory: </label>
<input type="text" value="test" id="datastore_directory"></input>
<br />

<label>User database: </label>
<input type="text" value="test" id="user_database"></input>
<br />


<input type="submit" value="Submit"></input>
</form>

</body>
</html>
