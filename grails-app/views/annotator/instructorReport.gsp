<%@ page import="org.bbop.apollo.gwt.shared.PermissionEnum; org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <export:resource />
    <title>Annotators</title>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-track" class="form-group report-header content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <g:each in="${userGroups}" var="userGroup">
        <div class="groupHeader">
            <p>Group: ${userGroup.name}</p>
        </div>
        <p><export:formats formats="['csv', 'excel', 'xml']" action="export" params="[userGroups: userGroup]"> </export:formats>
        </p>
        <g:set var="annotatorInstanceList" value="${annotatorGroupList.get(userGroup)}" />
        <table class="reportTable">
        <thead>
            <g:sortableColumn property="username" title="Username"/>
            <g:sortableColumn property="firstName" title="First Name"/>
            <g:sortableColumn property="lastName" title="Last Name"/>
            <th>Top Level Features</th>
            <th>Genes</th>
            <th>Transcripts</th>
            <th>Exons</th>
            <th>Transposable Elements</th>
            <th>Repeat Regions</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${annotatorInstanceList}" status="i" var="annotatorInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    <a style="margin: 2px;padding: 2px;" href='<g:createLink action="detail" controller="annotator"
                                                                             id="${annotatorInstance.annotator.id}">${annotatorInstance.username}</g:createLink>'
                       class="btn btn-default">
                        ${annotatorInstance.username}
                    </a>
                    <br/>
                    <perms:isUserAdmin user="${annotatorInstance.annotator}">
                        <span class="label label-default">Admin</span>
                    </perms:isUserAdmin>
                    <perms:isUserNotAdmin user="${annotatorInstance.annotator}">
                        <g:if test="${annotatorInstance.userOrganismPermissionList}">
                            <g:each in="${annotatorInstance.userOrganismPermissionList}" var="permission">
                                <g:if test="${permission.userOrganismPermission.permissionValues}">
                                    <span>
                                        ${permission.userOrganismPermission.organism.commonName}
                                        <g:each in="${permission.userOrganismPermission.permissionValues}" var="pValue">
                                            <span class="badge">
                                                ${pValue}
                                            </span>
                                        </g:each>
                                    </span>
                                    <br/>
                                </g:if>
                            </g:each>
                        </g:if>
                    </perms:isUserNotAdmin>
                </td>
                <td style="text-align: left;">
                    ${annotatorInstance.firstName}
                </td>
                <td>
                    ${annotatorInstance.lastName}
                </td>
                <td>
                    ${annotatorInstance.totalFeatureCount}
                </td>
                <td>${annotatorInstance.geneCount}</td>
                <td>
                    <g:if test="${annotatorInstance.transcriptCount}">
                        <div class="info-border">
                            Total
                            <span class="badge">${annotatorInstance.transcriptCount}</span>
                        </div>

                        <div class="info-border">
                            Protein encoding
                            <span class="badge"><g:formatNumber
                                    number="${annotatorInstance.proteinCodingTranscriptPercent}" type="percent"/></span>
                        </div>

                        <div class="info-border">
                            Exons / transcript
                            <span class="badge"><g:formatNumber number="${annotatorInstance.exonsPerTranscript}"
                                                                type="number"/></span>
                        </div>

                        <g:each in="${annotatorInstance.transcriptTypeCount}" var="trans">
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
                <td>${annotatorInstance.exonCount}</td>
                <td>${annotatorInstance.transposableElementCount}</td>
                <td>${annotatorInstance.repeatRegionCount}</td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </g:each>
    <div class="pagination">
        <g:paginate total="${annotatorInstanceCount ?: 0}"/>
    </div>
    <div>
        <p class="groupHeader">Export All Groups</p>
        <export:formats formats="['csv', 'excel', 'xml']" action="export" params="[userGroups: userGroupsIds]"> </export:formats>
    </div>
</div>

</body>
</html>
