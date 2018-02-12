<%@ page import="org.bbop.apollo.gwt.shared.PermissionEnum; org.bbop.apollo.Feature" %>

<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <title>Annotators</title>

</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-track" class="form-group report-header content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <g:each in="${userGroups}" var="userGroup">
        Group: ${userGroup.name}
        <g:set var="annotatorsInstance" value="${annotatorGroupList.get(userGroup)}" />
            <g:set var="annotators" value="${userGroup.users}" />
            <g:set var="organisms" value="${permissionService.getOrganismsForGroup(userGroup)}" />
            <g:set var="organismsNum" value="${organisms.size()}" />
            <table>
                <thead>
                <tr>
            <g:sortableColumn property="username" title="Username"/>
            <g:sortableColumn property="firstName" title="First Name"/>
            <g:sortableColumn property="lastName" title="Last Name"/>
            <th>Organism</th>
            <th>Top Level Features</th>
            <th>Genes</th>
            <th>Transcripts</th>
            <th>Exons</th>
            <th>TE</th>
            <th>RR</th>
            </tr>
            </thead>
            <tbody>


            <g:each in="${annotators}" var="annotator" status="i">
                <g:set var="annotatorInstance" value="${annotatorsInstance.get(annotator)}" />
                <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td rowspan="${organismsNum}">
                    <a style="margin: 2px;padding: 2px;" href='<g:createLink action="detail" controller="annotator"
                                                                             id="${annotator.id}">${annotator.username}</g:createLink>'
                       class="btn btn-default">
                        ${annotator.username}
                    </a>
                    <br/>
                    <perms:isUserAdmin user="${annotator}">
                        <span class="label label-default">Admin</span>
                    </perms:isUserAdmin>
                    <perms:isUserNotAdmin user="${annotator}">
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
                <td style="text-align: left;" rowspan="${organismsNum}">
                    ${annotator.firstName}
                </td>
                <td rowspan="${organismsNum}">
                    ${annotator.lastName}
                </td>
                    <g:set var="num" value="${1}" />
                    <g:each in="${organisms}" var="organism">
                        <g:if test="${num++ > 1}">
                        <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                        </g:if>
                        <td>
                            ${organism.commonName}
                        </td>
                        <g:set var="annotatorOrganismInstance" value="${annotatorInstance.get(organism)}" />
                            <td>
                                ${annotatorOrganismInstance.totalFeatureCount}
                            </td>
                            <td>${annotatorOrganismInstance.geneCount}</td>
                            <td>
                                <g:if test="${annotatorOrganismInstance.transcriptCount}">
                                    <div class="info-border">
                                        Total
                                        <span class="badge">${annotatorOrganismInstance.transcriptCount}</span>
                                    </div>

                                    <div class="info-border">
                                        Protein encoding
                                        <span class="badge"><g:formatNumber
                                                number="${annotatorOrganismInstance.proteinCodingTranscriptPercent}" type="percent"/></span>
                                    </div>

                                    <div class="info-border">
                                        Exons / transcript
                                        <span class="badge"><g:formatNumber number="${annotatorOrganismInstance.exonsPerTranscript}"
                                                                            type="number"/></span>
                                    </div>

                                    <g:each in="${annotatorOrganismInstance.transcriptTypeCount}" var="trans">
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
                            <td>${annotatorOrganismInstance.exonCount}</td>
                            <td>${annotatorOrganismInstance.transposableElementCount}</td>
                            <td>${annotatorOrganismInstance.repeatRegionCount}</td>
                        <g:if test="${num > 1}">
                        </tr>
                        </g:if>

                </g:each>
               </tr>
            </g:each>
            </tbody>
    </table>
    </g:each>
    <div class="pagination">
        <g:paginate total="${annotatorInstanceCount ?: 0}"/>
    </div>
</div>

</body>
</html>
