<!-- The DOCTYPE declaration above will set the     -->
<!-- browser's rendering engine into                -->
<!-- "Standards Mode". Replacing this declaration   -->
<!-- with a "Quirks Mode" doctype is not supported. -->

<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>

    <meta name="layout" content="annotator2">
    %{--<meta name="layout" content="main"/>--}%
    <title>Annotator</title>

    <asset:javascript src="spring-websocket"/>

    <script type="text/javascript" language="javascript" src="annotator.nocache.js"></script>
    <script>
        var Options = {
            rootUrl: '${applicationContext.servletContext.getContextPath()}'
            , showFrame: '${params.showFrame  && params.showFrame == 'true' ? 'true' : 'false' }'
        };

        //     $(function() {
        var socket = new SockJS("${createLink(uri: '/stomp')}");
        var client = Stomp.over(socket);

        client.connect({}, function () {
            client.subscribe("/topic/AnnotationNotification", function (message) {
                window.reloadAnnotations();
            });
            client.subscribe("/topic/JBrowseTrackList",function(message){
                console.log('index2::recieved the track list: '+message.body);
                var trackList ;
                trackList = JSON.parse(message.body);
                console.log('index::the track list!: '+trackList.length);
                var returnPayload = {tracks:trackList};
                window.loadTracks(JSON.stringify(returnPayload));
            })

        });



        var sendTrackUpdate = function (track) {
            console.log('publishing track update: ' + track);
            client.send("/topic/TrackList", {}, track);
            console.log('PUBLSISHED track update: ' + track);
        };

        var requestTracks = function () {
//            console.log('getting all tracks: '+track);
            var commandObject = {};
            commandObject.command = "list";

            console.log('connecting . . ');
            console.log('index:PUBLSISHING track list: ' + commandObject);
            console.log('index:PUBLSISHED track list: ' + commandObject);
            client.send("/topic/TrackList", {}, JSON.stringify(commandObject));
            console.log('index:PUBLSISHED track list: ' + commandObject);
        };
    </script>
</head>

<body style="background-color: white;">

<div id="someIframe">Test</div>
%{--<div id="annotator" style="background-color: white;"></div>--}%

<!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
<noscript>
    <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
    </div>
</noscript>

</body>
</html>
