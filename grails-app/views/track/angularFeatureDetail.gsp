<%--
  Created by IntelliJ IDEA.
  User: nathandunn
  Date: 9/21/15
  Time: 9:10 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Track View Detail: ${track}:${sequence}:${name} </title>
    %{--<asset:javascript src="vendor/angular-1.3.17.min.js"/>--}%
    %{--<asset:javascript src="browser/view-detail.js"/>--}%
</head>

<body>

%{--http://localhost:8080/apollo/track/featureDetail?track=Official%20Gene%20Set%20v3.2&sequence=Group9.10&name=GB42731-RA--}%
<div ng-app="track-detail-app">
<h3>Track View Detail: ${track}:${sequence}:${name} </h3>

    <div ng-controller="TrackViewDetailController as ctrl"
    data-track="${track}" data-organism="${organism}" data-sequence="${sequence}" data-name="${name}"
    >

        <div ng-repeat="feature in ctrl.data">
          {{feature}}
        </div>




    </div>

</div>

</body>
</html>