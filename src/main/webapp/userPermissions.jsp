<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="org.bbop.apollo.web.config.ServerConfiguration"%>
<%@ page import="org.bbop.apollo.web.user.UserManager"%>
<%@ page import="org.bbop.apollo.web.user.Permission"%>
<%@ page import="org.bbop.apollo.web.user.UserAuthentication"%>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Iterator" %>

<%
    ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));
if (!UserManager.getInstance().isInitialized()) {
    ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
    UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
}
UserAuthentication userAuthentication = (UserAuthentication)Class.forName(serverConfig.getUserAuthenticationClass()).newInstance();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<!--
<link rel="stylesheet" type="text/css" href="styles/userPermissions.css" />
<link rel="stylesheet" type="text/css" href="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.css" />
<title>Edit Permissions</title>
<script type="text/javascript" src="jslib/jquery-1.7.1.min.js"></script>
<script type="text/javascript" src="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.min.js"></script>
-->

<script type="text/javascript">

function closeDialog() {
    $("#user_manager_dialog").dialog("close");
};

var updateUsernames = new Object();

var users = new Array();
<%
UserManager userManager = UserManager.getInstance();
List<String> usernames = new ArrayList<String>(userManager.getUserNames());
Collections.sort(usernames);
for (String username : usernames) {
    Map<String, Integer> permissions = userManager.getPermissionsForUser(username);
    Iterator<Integer> permissionIter = permissions.values().iterator();
    Integer permission = permissionIter.hasNext() ? permissionIter.next() : 0;
    boolean read = (permission & Permission.READ) != 0;
    boolean write = (permission & Permission.WRITE) != 0;
    boolean publish = (permission & Permission.PUBLISH) != 0;
    boolean manageUser = (permission & Permission.USER_MANAGER) != 0;
    out.println("var user = new Array();");
    out.println("users.push(user);");
    out.println(String.format("user.push('%s');", username));
    out.println(String.format("user.push('<input class=\"permission_checkbox\" permission=\"read\" user=\"%s\" type=\"checkbox\"" + (read ? " checked=\"checked\"" : "") + "></input>');", username));
    out.println(String.format("user.push('<input class=\"permission_checkbox\" permission=\"write\" user=\"%s\" type=\"checkbox\"" + (write ? " checked=\"checked\"" : "") + "></input>');", username));
    out.println(String.format("user.push('<input class=\"permission_checkbox\" permission=\"publish\" user=\"%s\" type=\"checkbox\"" + (publish ? " checked=\"checked\"" : "") + "></input>');", username));
    out.println(String.format("user.push('<input class=\"permission_checkbox\" permission=\"user_manager\" user=\"%s\" type=\"checkbox\"" + (manageUser ? " checked=\"checked\"" : "") + "></input>');", username));
    out.println(String.format("user.push('<a class=\"ui-icon ui-icon-close delete_user\" user=\"%s\"></a>');", username));

    /*
    out.println("<tr>");
    out.println("<td>" + username + "</td>");
    out.println("<td><input class='permission_checkbox read' type='checkbox'" + (read ? " checked='checked'" : "") + " id='" + username + "-read' onchange='update_user_list(\"" + username + "\")'></td>");
    out.println("<td><input class='permission_checkbox write' type='checkbox'" + (write ? " checked='checked'" : "") + " id='" + username + "-write' onchange='update_user_list(\"" + username + "\")'></td>");
    out.println("<td><input class='permission_checkbox publish' type='checkbox'" + (publish ? " checked='checked'" : "") + " id='" + username + "-publish' onchange='update_user_list(\"" + username + "\")'></td>");
    out.println("<td><input class='permission_checkbox user_manager' type='checkbox' " + (manageUser ? " checked='checked'" : "") + " id='" + username + "-user_manager' onchange='update_user_list(\"" + username + "\")'></td>");
    out.println("<td><a href='#' class='ui-icon ui-icon-close' onclick='delete_user(\"" + username + "\")'></a>");
    out.println("</tr>");
    */
}
%>

var userTable;
$(function() {
    userTable = $("#users").dataTable({
        aaData: users,
        bAutoWidth: false,
        oLanguage: {
            sSearch: "Filter: "
        },
        aoColumns: [
            { sTitle: "User", sClass: "user_cell" },
            { sTitle: "Read", bSortable: false, bSearcheable: false },
            { sTitle: "Write", bSortable: false, bSearcheable: false },
            { sTitle: "Publish", bSortable: false, bSearcheable: false },
            { sTitle: "Manage users", bSortable: false },
            { bSortable: false }
        ]
    });
    $("#users_checkbox_menu").menu( { } );
    $(".permission_all").click(function() {
        update_checked(true, $(this).attr("permission"));
    });
    $(".permission_none").click(function() {
        update_checked(false, $(this).attr("permission"));
    });
    $(".permission_displayed").click(function() {
        $(".permission_checkbox[permission=" + $(this).attr("permission") + "]").attr("checked", true).change();
    });
    userTable.$(".delete_user").click(function() {
        delete_user($(this).attr("user"), $(this).parents("tr")[0]);
    });
    userTable.$(".permission_checkbox").change(function() {
        update_user_list($(this).attr("user"));
    });
});

function update_checked(checked, permission) {
    userTable.$(".permission_checkbox[permission='" + permission + "']").prop("checked", checked).change();
};

function toggleSelection(selector, checked) {
    $(selector).prop("checked", checked).change();
};

function update() {
    if (Object.keys(updateUsernames).length == 0) {
        return;
    }
    var permissionVals = {
            none: <%= Permission.NONE %>,
            read: <%= Permission.READ %>,
            write: <%= Permission.WRITE %>,
            publish: <%= Permission.PUBLISH %>,
            user_manager: <%= Permission.USER_MANAGER %>
    };
    var permissions = new Object();
    $.each(updateUsernames, function(username) {
        var permission = permissionVals["none"];
        userTable.$(".permission_checkbox[user='" + username + "']").each(function() {
            if ($(this).attr("checked")) {
                var p = $(this).attr("permission");
                permission |= permissionVals[p];
            }
        });
        permissions[username] = permission;
    });
    
    
    var json = new Object();
    json.operation = "set_permissions";
    json.permissions = permissions;
    disable_clickables(true);
    $.ajax({
        type: "post",
        url: "UserManagerService",
        processData: false,
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(json),
        success: function(response, status) {
            disable_clickables(false);
            updateUsernames = {};
        }
    });
};

function add_user_dialog() {
    var $dialog = $("<div></div>").load("<%= userAuthentication.getAddUserURL() %>", null, function() {
        $dialog.find("form").submit(function() {
            var username = get_user();
            if (userTable.$("a[user='" + username + "']").get(0)) {
                alert("User '" + username + "' already exists");
            }
            else {
                add_user();
                var row = userTable.fnAddDataAndDisplay( [ username,
                                '<input class="permission_checkbox" permission="read" user="' + username + '" type="checkbox"></input>',
                                '<input class="permission_checkbox" permission="write" user="' + username + '" type="checkbox"></input>',
                                '<input class="permission_checkbox" permission="publish" user="' + username + '" type="checkbox"></input>',
                                '<input class="permission_checkbox" permission="user_manager" user="' + username + '" type="checkbox"></input>',
                                '<a class="ui-icon ui-icon-close delete_user" user="' + username + '"></a>'
                                 ]);
                $(row.nTr).find(".delete_user").click(function() {
                    delete_user($(this).attr("user"), $(this).parents("tr")[0]);
                });
                $(row.nTr).find(".permission_checkbox").change(function() {
                    update_user_list($(this).attr("user"));
                });
                $(row.nTr).find("td").hide().fadeIn();
                //$(row.nTr).fadeIn();
                $dialog.dialog("close");
            }
            return false;
        });
    });
    $dialog.dialog({
        autoOpen: false,
        modal: true,
        close: function() {
            $(this).dialog("destroy").remove();
        },
        title: "Add User" });
    $dialog.dialog("open");
};

function delete_user(username, td) {
    var user = new Object();
    user.username = username;
    var json = new Object();
    json.operation = "delete_user";
    json.user = user;
    if (confirm("Are you sure you want to delete user '" + username + "'?")) {
        disable_clickables(true);
        $.ajax({
            type: "post",
            url: "UserManagerService",
            processData: false,
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(json),
            success: function(response, status) {
                //window.location.reload();
                var pos = userTable.fnGetPosition(td);
                userTable.fnDeleteRow(pos);
                disable_clickables(false);
            }
        });
    }
};

function update_user_list(username) {
    updateUsernames[username] = 1;
};

function disable_clickables(disabled) {
    $("button").prop("disabled", disabled);
    userTable.$("button").prop("disabled", disabled);
    /*
    if (disabled) {
        $("a").each(function() {
            $(this).fadeTo("fast", .5).removeAttr("href");
        });
    }
    else {
        $("a").each(function() {
            $(this).fadeTo("fast", 1).attr("href", "#");
        });
    }
    */
};

</script>
</head>
<body>
<%
if ((String)session.getAttribute("username") == null) {
    out.println("You must first login");
    out.println("</body>");
    out.println("</html>");
    return;
}
/*
UserManager userManager = UserManager.getInstance();
*/
boolean hasPermission = (userManager.getPermissionsForUser((String)session.getAttribute("username")).entrySet().iterator().next().getValue() & Permission.USER_MANAGER) != 0;
if (!hasPermission) {
    out.println("You do not have permission to modify users");
    out.println("</body>");
    out.println("</html>");
    return;
}
%>
<div id="users_checkbox_menu_div">
<ul id="users_checkbox_menu">
    <li><a><input type="checkbox" disabled="true"/></a>
    <ul>
        <li><a id="read">Read</a>
            <ul>
                <li><a class="permission_all" permission="read" id="read_check_all">All</a></li>
                <li><a class="permission_displayed" permission="read" id="read_check_displayed">Displayed</a>
                <li><a class="permission_none" permission="read" id="read_check_none">None</a></li>
            </ul>
        </li>
        <li><a id="write">Write</a>
            <ul>
                <li><a class="permission_all" permission="write" id="write_check_all">All</a></li>
                <li><a class="permission_displayed" permission="write" id="write_check_displayed">Displayed</a>
                <li><a class="permission_none" permission="write" id="write_check_none">None</a></li>
            </ul>
        </li>
        <li><a id="publish">Publish</a>
            <ul>
                <li><a class="permission_all" permission="publish" id="publish_check_all">All</a></li>
                <li><a class="permission_displayed" permission="publish" id="publish_check_displayed">Displayed</a>
                <li><a class="permission_none" permission="publish" id="publish_check_none">None</a></li>
            </ul>
        </li>
        <li><a id="user_manager">Manage users</a>
            <ul>
                <li><a class="permission_all" permission="user_manager" id="user_manager_check_all">All</a></li>
                <li><a class="permission_displayed" permission="user_manager" id="user_manager_check_displayed">Displayed</a>
                <li><a class="permission_none" permission="user_manager" id="user_manager_check_none">None</a></li>
            </ul>
        </li>
    </ul>
    </li>
</ul>
</div>

<div id="users_div">
<table id="users">
</table>
</div>
<div id="spacer_div"></div>
<div id="add_user_button_group">
<button id="add_user" onclick="add_user_dialog()">Add user</button>
</div>
<div id="update_button_group">
<button id="update" onclick="update()">Update</button>
<button id="cancel" onclick="closeDialog()">Cancel</button>
</div>
</body>
</html>
