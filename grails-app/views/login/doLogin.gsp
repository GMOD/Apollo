<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 3/16/15
  Time: 7:53 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <title>Login</title>
    <!--
<link rel="stylesheet" type="text/css" href="../styles/login.css" />
-->
    <!--
<script src="jslib/jquery-1.7.1.min.js" type="text/javascript"></script>
<script type="text/javascript" src="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.min.js"></script>
-->
    <script>
        var context;
        $(document).ready(function () {
            var pathname = location.pathname;
            context = /^\/([^\/]+)\//.exec(pathname)[1];
//            $("head").append("<link rel='stylesheet' type='text/css' href='/" + context + "/styles/login.css'/>");
            $("#login_button").click(function () {
                login();
            });
            $("#clear_button").click(function () {
//                $(".input_field").val("");
                $("#username").val("");
                $("#password").val("");
            });
            $(".input_field").keypress(function (event) {
                var code = event.keyCode ? event.keyCode : event.which;
                if (code == $.ui.keyCode.ENTER) {
                    login();
                }
            });
//            $("#username").focus();
        });

        function login() {
            var username = $("#username").val();
            if (!username) {
                alert("Missing username");
                return;
            }
            var password = $("#password").val();
            var remember_me = $("#remember_me").val();
            var json = new Object();
            json.username = username;
            json.password = password;
            json.rememberMe = remember_me;
            $.ajax({
                type: "post",
                url: "/" + context + "/Login?operation=login",
                processData: false,
                dataType: "json",
                contentType: "application/json",
                data: JSON.stringify(json),
                success: function (data) {
                    if(data.error){
                        setMessage(data.error);
                    }
                    else{
                        window.location.reload();
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    alert('error: '+jqXHR.responseText);
                    var error = $.parseJSON(jqXHR.responseText);
                    setMessage(error.error);
                }
            });
        }

        function setMessage(message) {
            $("#message").text(message);
        }

    </script>
</head>

<body>

<div class="input-group" style="margin-bottom: 5px;margin-top: 5px;">
    <input class="form-control" type="text" id="username" placeholder="Username" autofocus="autofocus"/>
</div>

<div class="input-group" style="margin-bottom: 5px">
    <input class="form-control" type="password" id="password" placeholder="Password"/>
</div>


%{--<div class="button_login">--}%
<button class="btn btn-primary" id="login_button">Login</button>
<button class="btn btn-default" id="clear_button" >Clear</button>
%{--<button class="btn btn-default" id="rememberme_button" >Remember Me</button>--}%
<div>
    Remember me
    <input type="checkbox" autocomplete="off" id="remember_me" checked>
</div>

%{--</div>--}%
<div id="message"></div>
</body>
</html>
