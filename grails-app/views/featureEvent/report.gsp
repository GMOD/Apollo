<%@ page import="org.bbop.apollo.RequestHandlingService; org.bbop.apollo.Organism; org.bbop.apollo.User; org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Recent Changes</title>

    <script>
        function doSearch(){
            document.getElementById("customform").submit();
        }
    </script>
</head>

<body>

<g:render template="../layouts/reportHeader"/>


<g:form id="customform" name="myForm" url="[action: 'report', controller: 'featureEvent']">
    <div class="container-fluid" style="margin: 10px 0px 0px 20px; padding-bottom: 20px;">
        <g:if test="${flash.message}">
            <div class="message row col-sm-12" role="status">${flash.message}</div>
        </g:if>
        <div class="row col-sm-12">
            <div class="col-sm-2 form-group">
                %{--<label for="ownerName">Owner:</label>--}%
                <g:select name='ownerName' value="${ownerName}"
                          noSelection="${[null: 'Select User ...']}"
                          from='${User.listOrderByUsername()}'
                          optionKey="username" optionValue="username" onchange="doSearch();"/>
            </div>

            <div class="col-sm-2  form-group">
                %{--<label for="featureType">Feature type:</label>--}%
                <g:select name='featureType' value="${featureType}"
                          noSelection="${[null: 'Select Feature Type...']}"
                          from='${featureTypes}' onchange="doSearch();"
                />
            </div>

            <div class="col-sm-2  form-group">
                <g:select name='organismName' value="${organismName}"
                          noSelection="${[null: 'Select Organism ...']}"
                          from='${organisms}'
                          optionKey="commonName" optionValue="commonName" onchange="doSearch();"/>
            </div>
            <div class="col-sm-2  form-group">
                <g:textField class="form-control input-sm" name="sequenceName" maxlength="50" value="${sequenceName}" placeholder="Sequence Name" />
            </div>
        </div>
        <div class="row col-sm-12">
            <div class="col-sm-2">
                <strong>Last Updated</strong>
            </div>
            <div class="col-sm-4  form-group">
                After:
                <g:datePicker name="afterDate" value="${afterDate}" precision="day" relativeYears="[-20..20]"/>
            </div>
            <div class="col-sm-4  form-group">
                Before:
                <g:datePicker name="beforeDate" value="${beforeDate}" precision="day" relativeYears="[-20..20]"/>
            </div>
        </div>
        <div class="row col-sm-12">
            <div class="col-sm-2">
                <strong>Date Created</strong>
            </div>
            <div class="col-sm-4  form-group">
                After:
                <g:datePicker name="dateCreatedAfterDate" value="${dateCreatedAfterDate}" precision="day" relativeYears="[-20..20]"/>
            </div>
            <div class="col-sm-4  form-group">
                Before:
                <g:datePicker name="dateCreatedBeforeDate" value="${dateCreatedBeforeDate}" precision="day" relativeYears="[-20..20]"/>
            </div>
        </div>

        <div class="row col-sm-12">
            <div class="form-group col-sm-4">
                <button class="col-sm-3 btn btn-primary" type="submit">
                    <span class="glyphicon glyphicon-search" aria-hidden="true"></span> Search
                </button>
            </div>
        </div>
        <g:hiddenField name="sort" value="${params.sort}"/>
        <g:hiddenField name="order" value="${params.order}"/>
    </div>
</g:form>


<div id="list-feature" class="content scaffold-list" role="main">

    <table>
        <thead>
        <tr>
            <g:sortableColumn property="lastUpdated" title="Last update" params="${filters}"/>
            <g:sortableColumn property="dateCreated" title="Created" params="${filters}"/>
            <g:sortableColumn property="organism" title="Organism" params="${filters}"/>
            <g:sortableColumn property="sequencename" title="Sequence name" params="${filters}"/>
            <g:sortableColumn property="name" title="Name" params="${filters}"/>
            <g:sortableColumn property="owners" title="Owner" params="${filters}"/>
            <g:sortableColumn property="cvTerm" title="Feature type" params="${filters}"/>
        </tr>
        </thead>
        <tbody>
        <g:each in="${features}" status="i" var="feature">
            <g:set var="sequence" value="${feature.featureLocation.sequence}"/>
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    <g:formatDate format="dd-MMM-yy HH:mm (E z)" date="${feature.lastUpdated}"/>
                    (v ${feature.version})
                </td>
                <td>
                    <g:formatDate format="dd-MMM-yy HH:mm (E z)" date="${feature.dateCreated}"/>
                </td>
                <td>
                    <g:link target="_blank" controller="annotator" action="loadLink"
                            params="[organism: sequence.organism.id]">
                        ${sequence.organism.commonName}
                    </g:link>
                </td>
                <td>
                %{--${feature.featureLocation.sequence.name}--}%
                    <g:set var="sequence" value="${feature.featureLocation.sequence}"/>
                    <g:link target="_blank" controller="annotator" action="loadLink"
                            params="[loc: sequence.name + ':' + sequence.start + '..' + sequence.end, organism: sequence.organism.id]">
                        ${sequence.name}</g:link>
                    <g:link target="_blank" controller="sequence" action="report" id="${sequence.organism.id}">
                        <div class="glyphicon glyphicon-list-alt">
                        </div>
                    </g:link>
                </td>
                <td>
                    <g:link target="_blank" controller="annotator" action="loadLink"
                            params="[loc: feature.featureLocation.sequence.name + ':' + feature.featureLocation.fmin + '..' + feature.featureLocation.fmax, organism: feature.featureLocation.sequence.organism.id]">
                        ${feature.name}
                    </g:link>
                </td>

                <td>
                    <g:link target="_blank" controller="annotator" action="detail" id="${feature.owner?.id}">
                        ${feature.owner?.username}
                    </g:link>
                </td>
                <td>
                    ${feature.cvTerm}
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${featureCount ?: 0}" params="${params}"/>
    </div>
    <div class="col-sm-4">
        <div class="btn btn-info">
            Results <div class="badge badge-important">
            <g:formatNumber number="${featureCount}" type="number"/>
        </div>
        </div>
    </div>
</div>
</body>
</html>
