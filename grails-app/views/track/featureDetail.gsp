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

    <title>Track View Detail: ${track}:${sequence}:${name}</title>
    %{--<meta name="layout" content="main">--}%
</head>

<body>

%{--http://localhost:8080/apollo/track/featureDetail?track=Official%20Gene%20Set%20v3.2&sequence=Group9.10&name=GB42731-RA--}%
<div>
    <h3>Track View Detail: ${track}:${sequence}:${name}</h3>


    <h4>Primary Data</h4>

    <div class="responsive-table">
        <table class="table">
            <tr>
                <td class="info">
                    Name
                </td>

                <td >
                    ${data.name}
                </td>
            </tr>

            <tr>
                <td class="col-md-1 info">
                    Type
                </td>

                <td >
                    ${data.type}
                </td>
            </tr>

            <tr >
                <td class="col-md-1 info">
                    Score
                </td>

                <td >
                    ${data.score}
                </td>
            </tr>

            <tr >
                <td class="col-md-1 info">
                    Position
                </td>

                <td >
                    ${data.seq}:${data.start}..${data.end} (${data.strand > 0 ? '-' : '+'} strand)
                </td>
            </tr>

            <tr >
                <td class="col-md-1 info">
                    Length
                </td>

                <td >
                    ${data.end - data.start} bp
                </td>
            </tr>
        </table>
    </div>

</div>

</body>
</html>