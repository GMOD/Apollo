<%@ page import="org.bbop.apollo.Organism" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'organism.label', default: 'Organism')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>

    <asset:javascript src="grails-angularjs.js"/>
    <asset:stylesheet src="grails-angularjs.css"/>

    %{--<asset:javascript src="jquery" />--}%
    <asset:javascript src="spring-websocket" />

    <script type="text/javascript">
        $(function() {
            var socket = new SockJS("${createLink(uri: '/stomp')}");
            var client = Stomp.over(socket);

            client.connect({}, function() {
                client.subscribe("/topic/hello", function(message) {
                    $("#helloDiv").append(JSON.parse(message.body));
                });
            });

            $("#helloButton").click(function() {
                client.send("/app/hello", {}, JSON.stringify("world"));
            });
        });
    </script>

</head>

<body>

<button id="helloButton">hello</button>
<div id="helloDiv"></div>

<a href="#list-organism" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                               default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="list-organism" class="content scaffold-list" role="main" ng-app="OrganismApp" ng-controller="OrganismController">

    <h1><g:message code="default.list.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>

            %{--<g:sortableColumn property="directory" title="${message(code: 'organism.directory.label', default: 'Directory')}" />--}%
            <g:sortableColumn property="commonName"
                              title="${message(code: 'organism.commonName.label', default: 'Common Name')}"/>

            <g:sortableColumn property="genus" title="${message(code: 'organism.genus.label', default: 'Genus')}"/>

            <th># Sequences</th>
            <th># Annotations</th>
            <th>Action</th>

            %{--<g:sortableColumn property="abbreviation" title="${message(code: 'organism.abbreviation.label', default: 'Abbreviation')}" />--}%

            %{--<g:sortableColumn property="comment" title="${message(code: 'organism.comment.label', default: 'Comment')}" />--}%


            %{--<g:sortableColumn property="species" title="${message(code: 'organism.species.label', default: 'Species')}" />--}%

        </tr>
        </thead>
        <tbody>
        <g:each in="${organismInstanceList}" status="i" var="organismInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                %{--<td><g:link action="show" id="${organismInstance.id}">${fieldValue(bean: organismInstance, field: "directory")}</g:link></td>--}%

                %{--<td><g:link action="show" id="${organismInstance.id}">${fieldValue(bean: organismInstance, field: "abbreviation")}</g:link></td>--}%

                %{--<td>${fieldValue(bean: organismInstance, field: "comment")}</td>--}%

                <td>${fieldValue(bean: organismInstance, field: "commonName")}
                ${organismInstance.abbreviation}
                </td>

                <td>${fieldValue(bean: organismInstance, field: "genus")} ${organismInstance.species}</td>

                <td>
                    ${organismInstance.sequences?.size()}
                </td>

                <td>
                    {{getRandomSpan()}}
                </td>

                <td>
                    <g:link action="show" id="${organismInstance.id}">Details</g:link>
                    %{--&nbsp;--}%
                    &bull;
                    %{--&nbsp;--}%
                    <g:link controller="annotator">Annotate</g:link>
                </td>

                %{--<td>${fieldValue(bean: organismInstance, field: "species")}</td>--}%

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${organismInstanceCount ?: 0}"/>
    </div>
</div>

<script type="text/javascript">

    //    angular.module('myModule', ['ui.bootstrap']);

    var as = angular.module('OrganismApp', []);

    as.controller('OrganismController', function ($scope, $rootScope, $http, $location) {

        // TODO: make a rest request instead for the # of features
        $scope.getRandomSpan = function(){
//            return Math.floor((Math.random()*1000)+1);
            return  42
        };
    });

</script>

</body>
</html>
