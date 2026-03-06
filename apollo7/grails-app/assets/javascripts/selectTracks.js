//= require self
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
        headers: {
            "Content-Type":"application/x-www-form-urlencoded",
        },
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
