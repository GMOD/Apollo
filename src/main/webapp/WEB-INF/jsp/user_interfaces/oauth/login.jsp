<%@ page import="javax.xml.parsers.DocumentBuilder" %>
<%@ page import="javax.xml.parsers.DocumentBuilderFactory" %>
<%@ page import="javax.xml.parsers.ParserConfigurationException" %>
<%@ page import="java.io.FileInputStream" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="org.w3c.dom.Element" %>
<%@ page import="org.w3c.dom.Node" %>
<%@ page import="org.w3c.dom.NodeList" %>
<%@ page import="org.w3c.dom.NamedNodeMap" %>
<%@ page import="java.net.URL" %> <% 

// get the oauth settings from the configuration file 
String config_path = getServletContext().getRealPath("/") + "config/oauth.xml";
FileInputStream fstream = new FileInputStream(config_path);
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
DocumentBuilder db = dbf.newDocumentBuilder();
Document dom = db.parse(fstream);
Element docEle = dom.getDocumentElement();

// iterate through all of the providers that are enbaled and create a link for each one
String auth_buttons = "";
NodeList providers = docEle.getElementsByTagName("provider");
for (int i = 0; i < providers.getLength(); i++) {
    if (providers.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element provider = (Element) providers.item(i);

        String provider_id = provider.getAttribute("id");
        String enable = provider.getAttribute("enable");

        // if this provider is enabled then create the authentication link
        if (enable.equals("TRUE")) {
            String provider_name  = provider.getElementsByTagName("provider_name").item(0).getFirstChild().getNodeValue();
            String provider_logo  = provider.getElementsByTagName("provider_logo").item(0).getFirstChild().getNodeValue();
            String client_id      = provider.getElementsByTagName("client_id").item(0).getFirstChild().getNodeValue();
            String client_secret  = provider.getElementsByTagName("client_secret").item(0).getFirstChild().getNodeValue();
            String auth_url       = provider.getElementsByTagName("auth_url").item(0).getFirstChild().getNodeValue();
            String uname_field    = provider.getElementsByTagName("uname_field").item(0).getFirstChild().getNodeValue();


            auth_buttons += "<button " +
                                "id=\"" + provider_id + "-login-button\" " +
                                "style=\"margin-left: auto; margin-right: auto; display: block; min-width: 100px; min-height: 30px\" " +
                                "alt=\"" + provider_name + "\" " + 
                              "title=\"Login with " + provider_name + "\" " +
                                "onClick=\"login('" + auth_url + "', '" + uname_field + "', '" + client_id +"')\">";

            if (provider_logo != null) {
                auth_buttons += "<img style=\"max-width: 14em\" id=\"sign_in_image\" src=\"" + provider_logo + "\" border=\"0\">";
            }
            else {
                auth_buttons += "Login With " + provider_name;
            }
            auth_buttons += "</button>";
        }
    }
} %> 
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Login</title>
<script>
  function login(auth_url, uname_field, client_id) {
    var redirect_url = auth_url + "?" +
       "scope=" + uname_field + "&" +
       "response_type=code" + "&" +
       "state=na" + "&" +
       "redirect_uri=" + encodeURIComponent("http://localhost:8080/apollo/Login?operation=login") + "&" +
       "client_id=" + client_id;
    window.location = redirect_url;
  };
</script>
</head>
<body>
  <div>
    <% out.print(auth_buttons); %>
  </div>
</body>
</html>
