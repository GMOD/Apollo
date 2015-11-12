<%--
  Created by IntelliJ IDEA.
  User: nathandunn
  Date: 9/21/15
  Time: 9:10 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <asset:stylesheet src="application.css"/>
    <asset:javascript src="application.js"/>
    <asset:link rel="shortcut icon" href="webapollo_favicon.ico" type="image/x-icon"/>

    %{--<title>Track View Detail: ${track}:${sequence}:${name}</title>--}%
    <title>${track}:${sequence}:${name}</title>
    %{--<meta name="layout" content="main">--}%

    <script type='text/javascript'>

        function saveTextAsFile(sourceId,fileName) {
            var textToWrite = document.getElementById(sourceId).value;
            var textFileAsBlob = new Blob([textToWrite], {type:'text/plain'});
//                        var fileNameToSaveAs = document.getElementById("inputFileNameToSaveAs").value;
            var fileNameToSaveAs = fileName

            var downloadLink = document.createElement("a");
            downloadLink.download = fileNameToSaveAs;
            downloadLink.innerHTML = "Download File";
            if (window.webkitURL != null)
            {
                // Chrome allows the link to be clicked
                // without actually adding it to the DOM.
                downloadLink.href = window.webkitURL.createObjectURL(textFileAsBlob);
            }
            else
            {
                // Firefox requires the link to be added to the DOM
                // before it can be clicked.
                downloadLink.href = window.URL.createObjectURL(textFileAsBlob);
                downloadLink.onclick = destroyClickedElement;
                downloadLink.style.display = "none";
                document.body.appendChild(downloadLink);
            }

            downloadLink.click();
        }

    </script>
</head>

<body>

%{--http://localhost:8080/apollo/track/featureDetail?track=Official%20Gene%20Set%20v3.2&sequence=Group9.10&name=GB42731-RA--}%
<div>
    <h3>${track}:${sequence}:${name}</h3>

    <g:render template="dataTable" model="[data:data.trackDetails,offset:1]"/>

</div>

</body>
</html>