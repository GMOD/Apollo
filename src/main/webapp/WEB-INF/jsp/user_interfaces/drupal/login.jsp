<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title>Login</title>
  <script>
    var context;
    
    $(document).ready(function() {
      
      // Disable the form elements until after trying a session login.
      $(".form-element" ).prop( "disabled", true);
      
      var pathname = location.pathname;
      context = /^\/([^\/]+)\//.exec(pathname)[1];
      
      // Test a Drupal login session.  If the session login doesn't
      // work then the login dialoge is shown
      sessionLogin();

      // Make the form buttons functional.
      buildDialogue();
    });
    
    /** 
     * Tests login using user provided credentials
     */
    function credLogin() {
      var username = $("#username").val();
      if (!username) {
        alert("Missing username");
        return;
      }
      var password = $("#password").val();
      var json = new Object();
      json.username = username;
      json.password = password;
      $.ajax({
        type: "post",
        url: "/" + context + "/Login?operation=login",
        processData: false,
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(json),
        success: function(data) {
          window.location.reload();
        },
        error: function(jqXHR, textStatus, errorThrown) {
          var error = $.parseJSON(jqXHR.responseText);
          setMessage(error.error);
        }
      });
    };
    
    /**
     * Tests login using Drupal session which is passed by cookie
     */
    function sessionLogin() {
      setMessage("Trying to login automatically...");

      var json = new Object();
      json.username = "__SESSION__";
      json.password = "__SESSION__";
      $.ajax({
        type: "post",
        url: "/" + context + "/Login?operation=login",
        processData: false,
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(json),
        success: function(data) {
          window.location.reload();
        },
        error: function(jqXHR, textStatus, errorThrown) {
          var error = $.parseJSON(jqXHR.responseText);
          setMessage("");
          $(".form-element" ).prop( "disabled", false);
        }
      });
    };
    
    function setMessage(message) {
      $("#message").text(message);
    };
    
    function buildDialogue() {
      $("head").append("<link rel='stylesheet' type='text/css' href='/" + context + "/styles/login.css'/>");
      $("#login_button").click(function() {
        credLogin();
      });
      $("#clear_button").click(function() {
        $(".input_field").val("");
      });
      $(".input_field").keypress(function(event) {
        var code = event.keyCode ? event.keyCode : event.which;
        if (code == $.ui.keyCode.ENTER) {
          credLogin();
        }
      });
      $("#username").focus();
    }
  
  </script>
</head>
<body>
  <div class="user_login">
    <span class="fieldname">User name</span>
    <input class="input_field form-element" type="text" id="username" />
  </div>
  <div class="user_login">
    <span class="fieldname">Password</span>
    <input class="input_field form-element" type="password" id="password" />
  </div>
  <div class="button_login">
    <button id="login_button" class="form-element">Login</button>
    <button id="clear_button" class="form-element">Clear</button></div>
  <div id="message"></div>
</body>
</html>