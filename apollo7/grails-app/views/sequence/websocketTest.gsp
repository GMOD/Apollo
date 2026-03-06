<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">

    <asset:javascript src="jquery"/>
    <asset:javascript src="spring-websocket"/>

    <script type="text/javascript">
        $(function () {
            var socket = new SockJS("${createLink(uri: '/stomp')}");
            var client = Stomp.over(socket);

            client.connect({}, function () {
                client.subscribe("/topic/hello", function (message) {
                    $("#helloDiv").append(JSON.parse(message.body));
                });
                client.subscribe("/topic/AnnotationEditorService", function (message) {
                    $("#helloDiv2").append(JSON.parse(message.body));
                });

                client.send("/app/AnnotationEditorService", {}, JSON.stringify("openconnectstring"))
            });

            $("#helloButton").click(function () {
                client.send("/app/hello", {}, JSON.stringify("world"));
            });


        });
    </script>

    <g:set var="entityName" value="${message(code: 'sequence.label', default: 'Sequence')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>
<button id="helloButton">hello</button>

<div id="helloDiv"></div>

<div id="helloDiv2"></div>

<a href="#list-track" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                            default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="list-track" class="content scaffold-list" role="main">
    <h1><g:message code="default.list.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>

            %{--<th><g:message code="track.organism.label" default="Organism" /></th>--}%

            <g:sortableColumn property="name" title="${message(code: 'track.name.label', default: 'Name')}"/>
            <g:sortableColumn property="organism.name" title="${message(code: 'track.name.label', default: 'Organism')}"/>

        </tr>
        </thead>
        <tbody>
        <g:each in="${sequenceInstanceList}" status="i" var="sequenceInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td><g:link action="show"
                            id="${sequenceInstance.id}">${fieldValue(bean: sequenceInstance, field: "name")}</g:link></td>
                <td>${sequenceInstance.organism.commonName}
                <g:link uri="">Browse</g:link>
                </td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${trackInstanceCount ?: 0}"/>
    </div>
</div>
</body>
</html>
