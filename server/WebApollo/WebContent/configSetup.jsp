<%@page import="org.gmod.gbol.bioObject.util.BioObjectUtil"%>
<%@page import="org.gmod.gbol.bioObject.conf.BioObjectConfiguration"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="org.bbop.apollo.web.config.ServerConfiguration.DataAdapterConfiguration"%>
<%@page import="org.bbop.apollo.web.config.ServerConfiguration.DataAdapterGroupConfiguration"%>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration"%>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration.UserDatabaseConfiguration"%>
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








<style>
label:nth-child(even) {
   background-color: #ddd;
   display:block;
}


</style>

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


//google.load("dojo", "1.5");
var table;
var tracks_configuration_list={};

$(function() {
	
	initialize_config_settings();
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

	cleanup_user_item();
} );


function initialize_config_settings() {
	
	$("#datastore_directory").attr("value","<% out.print(serverConfig.getDataStoreDirectory()); %>");
	$("#mapping_file").attr("value","<% out.print(serverConfig.getGBOLMappingFile()); %>");
	$("#minimum_intron_size").attr("value","<% out.print(serverConfig.getDefaultMinimumIntronSize()); %>");
	$("#history_size").attr("value","<% out.print(serverConfig.getHistorySize()); %>");
	$("#use_cds_for_new_transcripts").attr("value","<% out.print(serverConfig.getUseCDS()); %>");
	$("#track_name_comparator").attr("value","<% out.print(serverConfig.getTrackNameComparator()); %>");
	$("#overlapper_class").attr("value","<% out.print(serverConfig.getOverlapperClass()); %>");




	$("#database_username").attr("value","<% out.print(serverConfig.getUserDatabase().getUserName()); %>");
	$("#database_password").attr("value","<% out.print(serverConfig.getUserDatabase().getPassword()); %>");
	$("#database_driver").attr("value","<% out.print(serverConfig.getUserDatabase().getDriver()); %>");
	$("#database_url").attr("value","<% out.print(serverConfig.getUserDatabase().getURL()); %>");

	<%
	//Get tracklist configuration
	Map<String,ServerConfiguration.TrackConfiguration> trackmap=serverConfig.getTracks();
	for(String trackname : trackmap.keySet()) {
		ServerConfiguration.TrackConfiguration track=trackmap.get(trackname);
		out.println("tracks_configuration_list[\""+track.getName()+
		                  "\"]={ \"organism\": \""+track.getOrganism()+"\","+ 
		                        "\"translation_table\": \""+track.getTranslationTable()+"\","+
		                        "\"sequence_cvterm\": \""+track.getSourceFeature().getType()+"\","+
		                        "\"refseqs_json\": \""+track.getSourceFeature().getSequenceDirectory()+"\","+
		                        "\"donor_site\": \""+track.getSpliceDonorSites()+"\","+
		                        "\"acceptor_site\": \""+track.getSpliceAcceptorSites()+"\""+
		
				"};");
		
	}
	%>
	
	
	var init_refseqs_config=function(){
		var track_selection=$("#track_options_list").attr("value");
		$("#organism_name").attr("value",tracks_configuration_list[track_selection].organism);
		$("#refseqs_json").attr("value",tracks_configuration_list[track_selection].refseqs_json);
		$("#sequence_cvterm").attr("value",tracks_configuration_list[track_selection].sequence_cvterm);
		$("#acceptor_site").attr("value",tracks_configuration_list[track_selection].acceptor_site);
		$("#donor_site").attr("value",tracks_configuration_list[track_selection].donor_site);
		$("#translation_table").attr("value",tracks_configuration_list[track_selection].translation_table);
	}
	
	$("#track_options_list").click(init_refseqs_config);
	init_refseqs_config();
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

<%

if ("POST".equalsIgnoreCase(request.getMethod())) {
	// Form was submitted.
	out.println("<h3>Settings saved</h3>");
	out.println(request.getParameter("mapping_file"));
	out.println(request.getParameter("datastore_directory"));
	out.println(request.getParameter("minimum_intron_size"));
	
	out.println(serverConfig.getDataStoreDirectory());
	out.println(serverConfig.getDefaultMinimumIntronSize());
	serverConfig.setDataStoreDirectory(request.getParameter("datastore_directory"));
	serverConfig.setGBOLMappingFile(request.getParameter("mapping_file"));
	serverConfig.setDefaultMinimumIntronSize(Integer.parseInt(request.getParameter("minimum_intron_size")));
	serverConfig.setHistorySize(Integer.parseInt(request.getParameter("history_size")));
	serverConfig.setOverlapperClass(request.getParameter("overlapper_class"));
	serverConfig.setUseCDS(Boolean.parseBoolean(request.getParameter("use_cds_for_new_transcripts")));
	serverConfig.setTrackNameComparator(request.getParameter("track_name_comparator"));
	
	ServerConfiguration.UserDatabaseConfiguration udc=
			serverConfig.new UserDatabaseConfiguration(
			request.getParameter("database_driver"),
			request.getParameter("database_url"),
			request.getParameter("database_username"),
			request.getParameter("database_password"));
	
	serverConfig.setUserDatabase(udc);
	
}
%>

<p>Basic configuration</p>


<form id="webapollo_config_form" accept-charset=utf-8 method="POST">
<fieldset class="formLayout">

<label>GBOL Mapping file:
<input type="text" id="mapping_file" name="mapping_file"></input> </label>

<label>Annotations data directory:
<input type="text" id="datastore_directory" name="datastore_directory"></input> </label>

<label>Minimum intron size:
<input type="text" id="minimum_intron_size" name="minimum_intron_size"></input> </label>


<label>History tracking size:
<input type="text" id="history_size" name="history_size"></input> </label>


<label>Overlapper class:
<input type="text" id="overlapper_class" name="track_name_comparator"></input> </label>

<label>Track name comparator:
<input type="text" id="track_name_comparator" name="track_name_comparator"></input> </label>

<label>Use existing CDS when creating new transcript:
<input type="text" id="use_cds_for_new_transcripts" name="use_cds_for_new_transcripts"></input> </label>


</fieldset>
<br />
<br />

<p>User database configuration</p>
<fieldset class="formLayout">

<label>Username:
<input type="text" id="database_username" name="database_username"></input> </label>

<label>Password:
<input type="password" id="database_password" name="database_password"></input> </label>

<label>Driver:
<input type="text" id="database_driver" name="database_driver"></input> </label>

<label>URL:
<input type="text" id="database_url" name="database_url"></input> </label>

</fieldset>


<p>Annotation track configuration</p>

<select id="track_options_list">
<%
//Output tracknames

for(String trackname : trackmap.keySet()) {
	ServerConfiguration.TrackConfiguration track=trackmap.get(trackname);
	out.println("<option>"+track.getName()+"</option>");
	
}
%>
</select>
<fieldset class="formLayout">

<label>Refseqs.json location:
<input type="text" id="refseqs_json" name="refseqs_json"></input> </label>

<label>Annotation database prefix:
<input type="text" id="database_prefix" name="database_prefix"></input> </label>

<label>Organism name:
<input type="text" id="organism_name" name="organism_name"></input> </label>

<label>Sequence CV term:
<input type="text" id="sequence_cvterm" name="sequence_cvterm"></input> </label>


<label>Translation table:
<input type="text" id="translation_table" name="translation_table"></input> </label>


<label>Splice acceptor site:
<input type="text" id="acceptor_site" name="acceptor_site"></input> </label>

<label>Splice donor site:
<input type="text" id="donor_site" name="donor_site"></input> </label>
</fieldset>


<br />
<input type="submit" value="Submit"></input>
</form>

</body>
</html>
