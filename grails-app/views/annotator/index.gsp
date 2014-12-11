<!-- The DOCTYPE declaration above will set the     -->
<!-- browser's rendering engine into                -->
<!-- "Standards Mode". Replacing this declaration   -->
<!-- with a "Quirks Mode" doctype is not supported. -->

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    %{--<meta name="layout" content="main"/>--}%
    <title>Annotator</title>

    <asset:javascript src="annotator/controllers/Annotator.js"/>
    <asset:stylesheet src="annotator.css"/>

  <!--                                           -->
  <!-- Any title is fine                         -->
  <!--                                           -->
  <title>Web Application Starter Project</title>

  <!--                                           -->
  <!-- This script loads your compiled module.   -->
  <!-- If you add any GWT meta tags, they must   -->
  <!-- be added before this line.                -->
  <!--                                           -->
  <script type="text/javascript" language="javascript" src="annotator.nocache.js"></script>
</head>

<!--                                           -->
<!-- The body can have arbitrary html, or      -->
<!-- you can leave the body empty if you want  -->
<!-- to create a completely dynamic UI.        -->
<!--                                           -->
<body>

<div id="apolloLogo" style="width: 100%">
  %{--<a href="http://genomearchitect.org">--}%
  %{--<asset:image src="ApolloLogo_100x36.png" alt="Web Apollo"/></a>--}%
  %{--Genome Annotator--}%
  %{--<nav:primary class="nav primary small-menu"/>--}%
  %{--<nav:primary/>--}%
  <ul class="nav nav-pills header1" >
    <li role="presentation" class="">
      <a href="http://genomearchitect.org">
        <asset:image src="ApolloLogo_100x36.png" alt="Web Apollo"/></a>
    </li>

    <li role="presentation" class="menu-item">
      <g:link action="list" controller="organism">Organisms</g:link>
    </li>
    <li role="presentation" class="active menu-item">
      <g:link action="index" controller="sequence">Sequences</g:link>
    </li>
    <li role="presentation" class=" menu-item">
      <g:link action="index" controller="annotator">Annotate</g:link>
    </li>
    <li role="presentation" class=" menu-item">
      <g:link action="permissions" controller="user">Permissions</g:link>
    </li>
  </ul>

</div>

<div id="annotator"></div>

<!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
<noscript>
  <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
    Your web browser must have JavaScript enabled
    in order for this application to display correctly.
  </div>
</noscript>

%{--<h1>Web Application Starter Project</h1>--}%

%{--<table align="center">--}%
  %{--<tr>--}%
    %{--<td colspan="2" style="font-weight:bold;">Please enter your name:</td>--}%
  %{--</tr>--}%
  %{--<tr>--}%
    %{--<td id="nameFieldContainer"></td>--}%
    %{--<td id="sendButtonContainer"></td>--}%
  %{--</tr>--}%
  %{--<tr>--}%
    %{--<td id="feedbackLabelContainer"></td>--}%
    %{--<td colspan="1" style="color:red;" id="errorLabelContainer"></td>--}%
  %{--</tr>--}%
%{--</table>--}%
</body>
</html>
