<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 3/16/15
  Time: 7:53 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">

<head>

    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

    <!-- USWDS Design Systenm -->
    <link rel="stylesheet" href="uswds/css/uswds.min.css">
    <script src="uswds/js/uswds.min.js"></script>

    <!-- Source Sans Pro - Google Font -->
    <link rel="preconnect" href="https://fonts.gstatic.com">
    <link href="https://fonts.googleapis.com/css2?family=Source+Sans+Pro:wght@200;300;400;600;700&display=swap" rel="stylesheet">
    
    <title>Apollo Login</title>

    <style>
    body {
        font-family: 'Source Sans Pro', sans-serif !important; 
    }
    </style>

    <script>
        var context;
        $(document).ready(function () {
            var pathname = location.pathname;
            context = /^\/([^\/]+)\//.exec(pathname)[1];
            // $("head").append("<link rel='stylesheet' type='text/css' href='/" + context + "/styles/login.css'/>");
            $("#login_button").click(function () {
                login();
            });
            $("#clear_button").click(function () {
            // $(".input_field").val("");
                $("#username").val("");
                $("#password").val("");
            });
            $(".input_field").keypress(function (event) {
                var code = event.keyCode ? event.keyCode : event.which;
                if (code == $.ui.keyCode.ENTER) {
                    login();
                }
            });
            // $("#username").focus();
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

    <!-- Government Banner -->
    <section class="usa-banner" aria-label="Official government website">
        <div class="usa-accordion">
            <header class="usa-banner__header">
                <div class="usa-banner__inner">
                    <div class="grid-col-auto">
                        <img class="usa-banner__header-flag" src="/assets/img/us_flag_small.png" alt="U.S. flag">
                    </div>
                    <div class="grid-col-fill tablet:grid-col-auto">
                        <p class="usa-banner__header-text">An official website of the United States government</p>
                        <p class="usa-banner__header-action" aria-hidden="true">Here’s how you know</p>
                    </div>
                    <button class="usa-accordion__button usa-banner__button" aria-expanded="false"
                        aria-controls="gov-banner">
                        <span class="usa-banner__button-text">Here’s how you know</span>
                    </button>
                </div>
            </header>
            <div class="usa-banner__content usa-accordion__content" id="gov-banner">
                <div class="grid-row grid-gap-lg">
                    <div class="usa-banner__guidance tablet:grid-col-6">
                        <img class="usa-banner__icon usa-media-block__img" src="/assets/img/icon-dot-gov.svg" role="img"
                            alt="Dot gov">
                        <div class="usa-media-block__body">
                            <p>
                                <strong>
                                    Official websites use .gov
                                </strong>
                                <br />
                                A <strong>.gov</strong> website belongs to an official government organization in the United
                                States.

                            </p>
                        </div>
                    </div>
                    <div class="usa-banner__guidance tablet:grid-col-6">
                        <img class="usa-banner__icon usa-media-block__img" src="/assets/img/icon-https.svg" role="img"
                            alt="Https">
                        <div class="usa-media-block__body">
                            <p>
                                <strong>
                                    Secure .gov websites use HTTPS
                                </strong>
                                <br />
                                A <strong>lock</strong> (
                                <span class="icon-lock"><svg xmlns="http://www.w3.org/2000/svg" width="52" height="64"
                                        viewBox="0 0 52 64" class="usa-banner__lock-image" role="img"
                                        aria-labelledby="banner-lock-title banner-lock-description">
                                        <title id="banner-lock-title">Lock</title>
                                        <desc id="banner-lock-description">A locked padlock</desc>
                                        <path fill="#000000" fill-rule="evenodd"
                                            d="M26 0c10.493 0 19 8.507 19 19v9h3a4 4 0 0 1 4 4v28a4 4 0 0 1-4 4H4a4 4 0 0 1-4-4V32a4 4 0 0 1 4-4h3v-9C7 8.507 15.507 0 26 0zm0 8c-5.979 0-10.843 4.77-10.996 10.712L15 19v9h22v-9c0-6.075-4.925-11-11-11z" />
                                    </svg></span>
                                ) or <strong>https://</strong> means you’ve safely connected to the .gov website. Share
                                sensitive information only on official, secure websites.

                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- End Government Banner -->

    <div class="input-group" style="margin-bottom: 5px;margin-top: 5px;">
        <input class="form-control" type="text" id="username" placeholder="Username" autofocus="autofocus"/>
    </div>

    <div class="input-group" style="margin-bottom: 5px">
        <input class="form-control" type="password" id="password" placeholder="Password"/>
    </div>

    %{-- <div class="button_login"> --}%
    <button class="btn btn-primary" id="login_button">Login</button>
    <button class="btn btn-default" id="clear_button" >Clear</button>
    %{-- <button class="btn btn-default" id="rememberme_button" >Remember Me</button> --}%

    <div>
        Remember me
        <input type="checkbox" autocomplete="off" id="remember_me" checked>
    </div>

    %{--</div>--}%
    <div id="message"></div>

    <!-- Footer -->

</body>
</html>
