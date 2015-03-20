<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration"%>
<%@ page import="org.bbop.apollo.web.user.UserManager"%>
<%@ page import="org.bbop.apollo.web.user.Permission"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Add User</title>
<!--
<script type="text/javascript" src="jslib/jquery-1.7.1.min.js"></script>
<script type="text/javascript" src="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.min.js"></script>
-->
<script type="text/javascript">

$(document).ready(function() {
    /*
    $(".input_field").keypress(function(event) {
        var code = event.keyCode ? event.keyCode : event.which;
        if (code == $.ui.keyCode.ENTER) {
            $("#user_form").submit();
        }
    });
    */
    $("#username").focus();
    $("#user_form").submit(function(event) {
        event.preventDefault();
    });
    
});

function clear_fields() {
    $(".input_field").each(function() {
        $(this).val("");
    });
};

function get_user() {
    return $("#username").val();
};

function add_user() {
    var username = get_user();
    if (username.length == 0) {
        alert("No username entered");
        return;
    }
    var password = $("#password").val();
    var user = new Object();
    user.username = username;
    user.password = password;
    user.encrypted = true;
    var json = new Object();
    json.operation = "add_user";
    json.user = user;
    /*
    $("button").prop("disabled", true);
    $("input").prop("disabled", true);
    */
    $.ajax({
        type: "post",
        url: "UserManagerService",
        processData: false,
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(json),
        success: function(response, status) {
            /*
            $("button").prop("disabled", false);
            $("input").prop("disabled", false);
            */
//            window.location.reload();
        }
    });
    return username;
};

</script>
</head>
<body>
<%
    ServerConfiguration serverConfig = new ServerConfiguration(getServletContext());
if (!UserManager.getInstance().isInitialized()) {
    ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
    UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
}
if ((String)session.getAttribute("username") == null) {
    out.println("You must first login");
    out.println("</body>");
    out.println("</html>");
    return;
}
UserManager userManager = UserManager.getInstance();
boolean hasPermission = (userManager.getPermissionsForUser((String)session.getAttribute("username")).entrySet().iterator().next().getValue() & Permission.USER_MANAGER) != 0;
if (!hasPermission) {
    out.println("You do not have permission to add users");
    out.println("</body>");
    out.println("</html>");
    return;
}
%>
<div>
    <form id="user_form">
        <div class="user_login"><span class="fieldname">User name</span><input class="input_field" type="text" id="username" /></div>
        <div class="user_login"><span class="fieldname">Password</span><input class="input_field" type="password" id="password" /></div>
        <div class="button_add_user"><input id="add_user_button" type="submit" value="Add user" onclick=""/><input type="reset" id="clear_button" value="Clear" /></div>
    </form>
<!-- 
        <div class="user_login"><span class="fieldname">User name</span><input class="input_field" type="text" id="username" /></div>
        <div class="user_login"><span class="fieldname">Password</span><input class="input_field" type="password" id="password" /></div>
        <div class="button_add_user"><button id="add_user_button" onclick="add_user()">Add user</button><button id="clear_button" onclick="clear_fields()">Clear</button></div>
-->
</div>
</body>
</html>
