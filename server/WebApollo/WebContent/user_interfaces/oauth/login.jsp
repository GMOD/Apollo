<%@ page import="javax.xml.parsers.DocumentBuilder" %>
<%@ page import="javax.xml.parsers.DocumentBuilderFactory" %>
<%@ page import="javax.xml.parsers.ParserConfigurationException" %>
<%@ page import="java.io.FileInputStream" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="org.w3c.dom.Node" %>
<% 

String Provider_Name = null;
String Provider_Logo = null;
String Client_ID = null;
String Client_Secret = null;
String Auth_URL = null;
String Uname_Field = null;

// get the oauth settings from the configuration file 
String config_path = getServletContext().getRealPath("/") + "config/oauth.xml";

FileInputStream fstream = new FileInputStream(config_path);
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
DocumentBuilder db = dbf.newDocumentBuilder();
Document doc = db.parse(fstream);

Provider_Name  = doc.getElementsByTagName("provider_name").item(0).getFirstChild().getNodeValue();
Provider_Logo  = doc.getElementsByTagName("provider_logo").item(0).getFirstChild().getNodeValue();
Client_ID = doc.getElementsByTagName("client_id").item(0).getFirstChild().getNodeValue();
Client_Secret = doc.getElementsByTagName("client_secret").item(0).getFirstChild().getNodeValue();
Auth_URL = doc.getElementsByTagName("auth_url").item(0).getFirstChild().getNodeValue();
Uname_Field = doc.getElementsByTagName("uname_field").item(0).getFirstChild().getNodeValue();
%> 

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Login</title>
<script>
  var context;
  $(document).ready(function() {
    var pathname = location.pathname;
    context = /^\/([^\/]+)\//.exec(pathname)[1];
    $("#login_button").click(function() {
      
      login();
    });
    $("#sign_in_image").attr("src", "/" + context + "/images/sign_in_green.png");
  });
  
  function login() {
    var auth_url = "<%=Auth_URL%>?" +
       "scope=<%=Uname_Field%>&" +
       "response_type=code&" +
       "redirect_uri=" + encodeURIComponent("http://localhost:8080/WebApollo/Login?operation=login&forceRedirect=true") + "&" +
       "client_id=<%=Client_ID%>";
    window.location = auth_url;
  };
  
  function setMessage(message) {
      $("#message").text(message);
  };
  
</script>
</head>
<body>
<div>
Login with <b><%=Provider_Name%></b> 
<button id="login_button">
<%
if (Provider_Logo != null) {
	out.print("<img id=\"sign_in_image\" src=\"" + Provider_Logo + "\"><br>");
}
%>
</button>
<div id="message">
</div>
</div>
</body>
</html>