<%--
  Created by IntelliJ IDEA.
  User: nathandunn
  Date: 7/20/15
  Time: 6:55 AM
--%>

<head>
    <meta name="layout" content="report">
    <title>Performance Metrics</title>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div class="page-header" style="margin-left: 20px;">
    <div class="header"><h4>Total Hits <span class="label label-info">${countTotal}</span></h4></div>

    <div class="header"><h4>Overall Mean (s/count) <span class="label label-info"><g:formatNumber number="${meanTotal}"
                                                                                        maxFractionDigits="6"/>
</span></h4></div>
        <div class="header"><h4>Total time (s)<span class="label label-info"><g:formatNumber
                number="${totalTime}" maxFractionDigits="6"/>
        </span></h4></div>
        <g:link action="downloadReport"><i class="glyphicon glyphicon-download-alt glyphicon-th-large"></i></g:link>
    </div>


    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>
            <th>Class</th>
            <th>Method</th>
            <th>total %</th>
            <th>total (s)</th>
            <th>count</th>
            <th>mean (s)</th>
            <th>max</th>
            <th>min</th>
            <th>stdev</th>

        </tr>
        </thead>
        <tbody>
        <g:each in="${performanceMetricList}" status="i" var="metric">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>${metric.className}</td>
                <td>${metric.methodName}</td>
                <td><g:formatNumber number="${metric.totalPercent}" maxFractionDigits="2" type="percent"/></td>
                <td><g:formatNumber number="${metric.total}"/></td>
                <td>${metric.count}</td>
                <td><g:formatNumber number="${metric.mean}" minFractionDigits="2" maxFractionDigits="2"/></td>
                <td><g:formatNumber number="${metric.max}" minFractionDigits="2" maxFractionDigits="2"/></td>
                <td><g:formatNumber number="${metric.min}" minFractionDigits="2" maxFractionDigits="2"/></td>
                <td><g:formatNumber number="${metric.stddev}" minFractionDigits="2" maxFractionDigits="2"/></td>
            </tr>
        </g:each>
        </tbody>
    </table>

</body>
</html>