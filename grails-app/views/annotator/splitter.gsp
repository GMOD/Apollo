<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 12/3/14
  Time: 4:42 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html ng-app="AnnotatorApplication">
<head>
    %{--<meta name="layout" content="main"/>--}%
    <title>Annotator</title>

    <asset:javascript src="annotator/controllers/Annotator.js"/>
    <asset:stylesheet src="annotator.css"/>
</head>

<body>

<div class="wrap">
    <div class="resizable resizable1" ng-controller="AnnotatorController">
        <div>
            ASDFASDFADSFADFSDF ASDF ASDF ASDF
        </div>
        %{--<button class="btn btn-default btn-lg" ng-click="pingSelf()">Ping</button>--}%
        %{--<tabset>--}%
        %{--<tab heading="Static title">Static content</tab>--}%
        %{--<tab ng-repeat="tab in tabs" heading="{{tab.title}}">--}%
        %{--<div>--}%
        %{--<ui-layout options="{flow : 'row'}">--}%
        %{--<div>A {{tab.title}}</div>--}%

        %{--<div>B {{tab.content}}</div>--}%
        %{--</ui-layout>--}%
        %{--</div>--}%
        %{--</tab>--}%
        %{--<tab select="alertMe()">--}%
        %{--<tab-heading>--}%
        %{--<i class="glyphicon glyphicon-bell"></i> Alert!--}%
        %{--</tab-heading>--}%
        %{--I've got an HTML heading, and a select callback. Pretty cool!--}%
        %{--</tab>--}%
        %{--</tabset>--}%

    </div>

    <div class="resizable resizable2"></div>
</div>

</body>

</html>