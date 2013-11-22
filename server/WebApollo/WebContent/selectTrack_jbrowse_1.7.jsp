<%@page import="org.bbop.apollo.web.config.ServerConfiguration.DataAdapterConfiguration"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration"%>
<%@ page import="org.bbop.apollo.web.user.UserManager"%>
<%@ page import="org.bbop.apollo.web.user.Permission"%>
<%@ page import="org.bbop.apollo.web.track.TrackNameComparator"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Collections"%>

<%
ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));
if (!UserManager.getInstance().isInitialized()) {
	ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
	UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
}
String username = (String)session.getAttribute("username");
if (username == null) {
	out.println("You must first login");
	return;
}
Map<String, Integer> permissions = UserManager.getInstance().getPermissionsForUser(username);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.9/themes/base/jquery-ui.css"/>  
<!-- <link rel="stylesheet" type="text/css" href="jbrowse/plugins/WebApollo/jslib/jqueryui/themes/base/jquery.ui.theme.css" /> -->
<link rel="stylesheet" type="text/css" href="styles/selectTrack.css" /> 

<!-- trying to load SequenceSearch.css from both berkeleybop:jbrowse master branch and jbrowse_1.7 branch -->
<!-- <link rel="stylesheet" type="text/css" href="jbrowse/styles/search_sequence.css" /> --> <!-- master branch -->
<link rel="stylesheet" type="text/css" href="jbrowse/plugins/WebApollo/css/search_sequence.css" /> <!-- jbrowse_1.7 branch -->

<title>Select track</title>

<!-- <script type="text/javascript" src="jslib/jquery-1.7.1.min.js"></script> -->
<!-- <script type="text/javascript" src="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.min.js"></script> -->
<script type="text/javascript" src="jbrowse/src/dojo/dojo.js" data-dojo-config="isDebug: 1, async: 1"></script>

<!-- <script type="text/javascript" src="http://www.google.com/jsapi"></script> -->
<!-- trying to load SequenceSearch.js from both berkeleybop:jbrowse master branch and jbrowse_1.7 branch -->
<!-- <script type="text/javascript" src="jbrowse/js/SequenceSearch.js"></script> --> <!-- master branch -->
<!-- <script type="text/javascript" src="jbrowse/plugins/WebApollo/js/SequenceSearch.js"></script> --> <!-- jbrowse_1.7 branch --> 
<script type="text/javascript">

// google.load("dojo", "1.5");

define.amd.jQuery = true;
require({
           packages: [
	       { name: 'WebApollo', location: '../../plugins/WebApollo/js' }, 
	       { name: 'jquery', location: '../../plugins/WebApollo/jslib/jquery', main: 'jquery' }, 
               { name: 'jqueryui', location: '../../plugins/WebApollo/jslib/jqueryui' },
           ]
       }, 
       // 'dojo' is needed here, otherwise dojo.create() in SequenceSearch throws a method not found 'create' error ???
       ['dojo', 'jquery', 'WebApollo/SequenceSearch', 'jqueryui/dialog'], 
       function(dojo, $, SequenceSearch, dialog) {

console.log("callback of require() called");

// define.amd.jQuery = true;

$(function() {
	     console.log($("#data_adapter_dialog"));
	$("#data_adapter_dialog").dialog( { draggable: false, modal: true, autoOpen: false, resizable: false, closeOnEscape: false } );
	     console.log($("#search_sequences_dialog"));
	$("#search_sequences_dialog").dialog( { draggable: false, modal: true, autoOpen: false, resizable: false, closeOnEscape: false, width: "auto" } );
} );

write_data = function(adapter, track, options, successMessage) {
// function write_data(adapter, track, options, successMessage) {
	$("#data_adapter_dialog").dialog("option", "closeOnEscape", false);
	$(".ui-dialog-titlebar-close", this.parentNode).hide();
	
	var enableClose = function() {
		$("#data_adapter_dialog").dialog("option", "closeOnEscape", true);
		$(".ui-dialog-titlebar-close", this.parentNode).show();
	};
	
	$.ajax({
		url: "IOService?operation=write&adapter=" + adapter + "&track=" + track + (options.length ? "&" + options : ""),
		beforeSend: function() {
			$("#data_adapter_loading").show();
		},
		success: function(data, textStatus, jqXHR) {
			var msg = successMessage ? successMessage : data;
			$("#data_adapter_loading").hide();
			$("#data_adapter_message").html(msg);
			enableClose();
		},
		error: function(qXHR, textStatus, errorThrown) {
			$("#data_adapter_message").text("Error writing " + adapter);
			ok = false;
			enableClose();
		}
	});
	
	$("#data_adapter_dialog").dialog("open");
	$("#data_adapter_message").text("Writing " + adapter);
}

open_search_dialog = function() {
// function open_search_dialog() {
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
		window.open('jbrowse/?loc=' + id + ":" + fmin + "-" + fmax);
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
}
	console.log("end of require callback");
} );

</script>
</head>
<body>
<div id="search_sequences_dialog" title="Search sequences" style="display:none">
</div>
<div id="data_adapter_dialog" title="Data adapter">
	<!-- <div id="data_adapter_loading"><img src="images/loading.gif"/></div> -->
	<div id="data_adapter_loading"><img src="jbrowse/plugins/WebApollo/img/loading.gif"/></div> 
<div id="data_adapter_message"></div></div>
<table>
	<tr>
		<td>&nbsp;</td>
		<td><button id="search_all" onclick="open_search_dialog()">Search sequences</button></td>
		<td>&nbsp;</td>
<%
for (DataAdapterConfiguration conf : serverConfig.getDataAdapters()) {
	for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {
		Integer permission = permissions.get(track.getName());
		if (permission == null) {
			permission = 0;
		}
		if ((Permission.getValueForPermission(conf.getPermission()) & permission) != 0) {
			String options = conf.getOptions() != null ? conf.getOptions() : "";
			out.println("<td><button id='" + conf.getKey() + "_all' onclick='write_data(\"" + conf.getKey() + "\", \"all_tracks\", \"" + options +"\")'>All to " + conf.getKey() + "</button></td>");
			break;
		}
	}
}
%>
	</tr>
<%
TrackNameComparator trackNameComparator = (TrackNameComparator)Class.forName(serverConfig.getTrackNameComparatorClass()).newInstance();
List<ServerConfiguration.TrackConfiguration> tracks = new ArrayList<ServerConfiguration.TrackConfiguration>(serverConfig.getTracks().values());
Collections.sort(tracks, trackNameComparator);
for (ServerConfiguration.TrackConfiguration track : tracks) {
	Integer permission = permissions.get(track.getName());
	if (permission == null) {
		permission = 0;
	}
	if ((permission & Permission.READ) == Permission.READ) {
		out.println("<tr>");
		out.println("<td>" + track.getOrganism() + "</td>");
		out.println("<td>" + track.getSourceFeature().getUniqueName() + "</td>");
		out.println("<td><button type=\"button\" onClick=\"window.open('jbrowse/?loc=" + track.getSourceFeature().getUniqueName() + "')\">Edit</button></td>");
		for (DataAdapterConfiguration conf : serverConfig.getDataAdapters()) {
			if ((Permission.getValueForPermission(conf.getPermission()) & permission) != 0) {
				String options = conf.getOptions() != null ? conf.getOptions() : "";
				out.println("<td><button id='" + conf.getKey() + "_" + track.getSourceFeature().getUniqueName() + "' onclick='write_data(\"" + conf.getKey() + "\", \"" + track.getName() + "\", \"" + options +"\")'>" + conf.getKey() + "</button></td>");
			}
		}
		out.println("</tr>");
	}
}
%>

</table>
</body>
</html>