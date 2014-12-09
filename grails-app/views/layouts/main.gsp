<!DOCTYPE html>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js"><!--<![endif]-->
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title><g:layoutTitle default="Web Apollo"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="apple-touch-icon" href="${assetPath(src: 'apple-touch-icon.png')}">
    <link rel="apple-touch-icon" sizes="114x114" href="${assetPath(src: 'apple-touch-icon-retina.png')}">
    <asset:stylesheet src="application.css"/>
    <asset:javascript src="application.js"/>
    <asset:link rel="shortcut icon" href="webapollo_favicon.ico" type="image/x-icon"/>
    %{--<link rel="shortcut icon" href="${assetPath(src: 'favicon.ico')}" type="image/x-icon">--}%
    <g:layoutHead/>
</head>

<body>

<div id="apolloLogo"><a href="http://genomearchitect.org">
    <asset:image src="ApolloLogo_100x36.png" alt="Web Apollo"/></a>
    %{--Genome Annotator--}%
    <nav:primary/>
</div>


%{--<g:include view="mainMenu"/>--}%
%{--<nav:secondary/>--}%

<g:layoutBody/>

<div class="footer" role="contentinfo"></div>

<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
</body>
</html>
