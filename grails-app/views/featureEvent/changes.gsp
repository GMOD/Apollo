<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Recent Changes</title>
</head>

<body>

<g:render template="../layouts/reportHeader"/>


<g:form id="customform" name="myForm" url="[action: 'changes', controller: 'featureEvent']">
    <div class="container-fluid" style="margin: 10px 0px 0px 20px; padding-bottom: 20px;">
        <g:if test="${flash.message}">
            <div class="message row col-sm-12" role="status">${flash.message}</div>
        </g:if>
        <div class="row col-sm-12">
            <div class="col-sm-3 form-group">
                <label for="ownerName">Owner:</label>
                <g:textField class="form-control" name="ownerName" maxlength="50" value="${ownerName}" placeholder="Owner"/><br/>
            </div>

            <div class="col-sm-4  form-group">
                <label for="featureType">Feature type:</label>
                <g:textField class="form-control" name="featureType" maxlength="50" value="${featureType}" placeholder="Feature Type"/> <br/>
            </div>

            <div class="col-sm-4  form-group">
                <label for="organismName">Organism:</label>
                <g:textField class="form-control" name="organismName" maxlength="50" value="${organismName}" placeholder="Organism"/><br/>
            </div>
        </div>

        <div class="row col-sm-12">
            <div class="form-group col-sm-4">
                <button class="col-sm-3 btn btn-primary" type="submit">
                    <span class="glyphicon glyphicon-search" aria-hidden="true"></span> Search
                </button>
            </div>
            %{--<input class="col-sm-1 btn btn-primary glyphicon" type="submit" value="Search"/>--}%
        </div>
        <g:hiddenField name="sort" value="${params.sort}"/>
        <g:hiddenField name="order" value="${params.order}"/>
    </div>
</g:form>


<div id="list-feature" class="content scaffold-list" role="main">

    <table>
        <thead>
        <tr>
            <g:sortableColumn property="lastUpdated" title="Last updated" params="${filters}"/>
            <g:sortableColumn property="organism" title="Organism" params="${filters}"/>
            <g:sortableColumn property="sequencename" title="Sequence name" params="${filters}"/>
            <g:sortableColumn property="name" title="Name" params="${filters}"/>
            <g:sortableColumn property="owners" title="Owner" params="${filters}"/>
            <g:sortableColumn property="cvTerm" title="Feature type" params="${filters}"/>
        </tr>
        </thead>
        <tbody>
        <g:each in="${features}" status="i" var="feature">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    <g:formatDate format="E dd-MMM-yy" date="${feature.lastUpdated}"/>
                </td>
                <td>
                    ${feature.featureLocation.sequence.organism.commonName}
                </td>
                <td>
                    ${feature.featureLocation.sequence.name}
                </td>
                <td>
                    <g:link target="_blank" controller="annotator" action="loadLink"
                            params="[loc: feature.featureLocation.sequence.name + ':' + feature.featureLocation.fmin + '..' + feature.featureLocation.fmax, organism: feature.featureLocation.sequence.organism.id]">
                        ${feature.name}
                    </g:link>
                </td>

                <td>
                    ${feature.owner?.username}
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
</div>
</body>
</html>
