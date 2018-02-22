<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <title>${organism.commonName} Sequences</title>

    <script>
        function changeOrganism() {
            var name = $("#organism option:selected").val();
            window.location.href = "${createLink(action: 'report')}/" + name;
        }
    </script>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-track" class="form-group report-header content scaffold-list" role="main">
    <div class="row form-group">
        <div class="col-lg-4 lead">${organism.commonName} Sequences</div>

        <g:select id="organism" class="input-lg" name="organism"
                  from="${organisms}" optionValue="commonName" optionKey="id"
                  value="${organism.id}"
                  onchange=" changeOrganism(); "/>
    </div>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table class="reportTable">
        <thead>
        <tr>
            <g:sortableColumn property="name" title="Name" class="sortableColumn"/>
            <g:sortableColumn property="length" title="Length" class="sortableColumn"/>
            <th>Annotators</th>
            <th>Top Level Features</th>
            <th>Genes</th>
            <th>Transcripts</th>
            <th>Exons</th>
            <th>TE</th>
            <th>RR</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${sequenceInstanceList}" status="i" var="sequenceInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td>
                    %{--<g:link action="show"--}%
                            %{--id="${sequenceInstance.id}">${sequenceInstance.name}</g:link></td>--}%
                ${sequenceInstance.name}
                <td style="text-align: left;">
                    <g:formatNumber number="${sequenceInstance.length}" type="number"/>
                </td>
                <td style="max-width: 10%;display: block;padding-right: 10px;padding-left: 10px;">
                    <g:each in="${sequenceInstance.annotators}" var="annotator">
                        <a style="margin: 2px;padding: 2px;" href='<g:createLink action="report" controller="annotator" id="${annotator.id}">${annotator.username}</g:createLink>' class="btn btn-default">
                        ${annotator.username}
                        </a>
                    </g:each>
                </td>
                <td>
                    ${sequenceInstance.totalFeatureCount}
                </td>
                <td>${sequenceInstance.geneCount}</td>
                <td>
                    <g:if test="${sequenceInstance.transcriptCount}">
                        <div class="info-border">
                            Total
                            <span class="badge">${sequenceInstance.transcriptCount}</span>
                        </div>
                        <div class="info-border">
                            Protein encoding
                            <span class="badge"><g:formatNumber number="${sequenceInstance.proteinCodingTranscriptPercent}" type="percent"/></span>
                        </div>
                        <div class="info-border">
                            Exons / transcript
                            <span class="badge"><g:formatNumber number="${sequenceInstance.exonsPerTranscript}" type="number"/></span>
                        </div>

                        <g:each in="${sequenceInstance.transcriptTypeCount}" var="trans">
                            <div class="info-border">
                                ${trans.key}
                                <span class="badge">
                                    ${trans.value}
                                </span>
                            </div>
                        </g:each>
                    </g:if>
                    <g:else>0</g:else>
                </td>
                <td>${sequenceInstance.exonCount} </td>
                <td>${sequenceInstance.transposableElementCount}</td>
                <td>${sequenceInstance.repeatRegionCount}</td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${sequenceInstanceCount ?: 0}" params="[id:organism.id]"/>
   </div>
</div>

</body>
</html>
