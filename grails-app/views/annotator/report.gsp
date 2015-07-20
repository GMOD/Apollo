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
    <table>
        <thead>
        <tr>
            <g:sortableColumn property="username" title="Username"/>
            <g:sortableColumn property="firstName" title="First Name"/>
            <g:sortableColumn property="lastName" title="Last Name"/>
            <th>Top Level Features</th>
            <th>Genes</th>
            <th>Transcripts</th>
            <th>Exons</th>
            <th>TE</th>
            <th>RR</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${annotatorInstanceList}" status="i" var="annotatorInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    ${annotatorInstance.username}
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
                        <button>
                            Total
                            <span class="badge">${annotatorInstance.transcriptCount}</span>
                        </button>
                        <button>
                            Protein encoding
                            <span class="badge"><g:formatNumber
                                    number="${annotatorInstance.proteinCodingTranscriptPercent}" type="percent"/></span>
                        </button>
                        <button>
                            Exons / transcript
                            <span class="badge"><g:formatNumber number="${annotatorInstance.exonsPerTranscript}"
                                                                type="number"/></span>
                        </button>

                        <g:each in="${annotatorInstance.transcriptTypeCount}" var="trans">
                            <button>
                                ${trans.key}
                                <span class="badge">
                                    ${trans.value}
                                </span>
                            </button>
                        </g:each>
                    </g:if>
                    <g:else>0</g:else>
                </td>
                <td>${annotatorInstance.exonCount}</td>
                <td>${annotatorInstance.transposableElementCount}</td>
                <td>${annotatorInstance.repeatRegionCount}</td>
            </tr>
            <g:each in="${annotatorInstance.userOrganismPermissionList}" var="userOrganismPermission">
                <tr>
                    <td></td>
                    <td>
                        <g:link action="report" id="${userOrganismPermission.userOrganismPermission.organism.id}" controller="sequence">
                            ${userOrganismPermission.userOrganismPermission.organism.commonName}
                        </g:link>
                    </td>
                    <td>
                        <g:if test="${userOrganismPermission.userOrganismPermission.permissions.toLowerCase().contains(org.bbop.apollo.gwt.shared.PermissionEnum.ADMINISTRATE.display)}">
                            <i class="glyphicon glyphicon-cog" title="Administrate privileges"></i>
                        </g:if>
                        <g:if test="${userOrganismPermission.userOrganismPermission.permissions.toLowerCase().contains(org.bbop.apollo.gwt.shared.PermissionEnum.WRITE.display)}">
                            <i class="glyphicon glyphicon-edit" title="Edit privileges"></i>
                        </g:if>
                        <g:if test="${userOrganismPermission.userOrganismPermission.permissions.toLowerCase().contains(org.bbop.apollo.gwt.shared.PermissionEnum.EXPORT.display)}">
                            <i class="glyphicon glyphicon-download-alt" title="Export privileges"></i>
                        </g:if>
                        <g:if test="${userOrganismPermission.userOrganismPermission.permissions.toLowerCase().contains(org.bbop.apollo.gwt.shared.PermissionEnum.READ.display)}">
                            <i class="glyphicon glyphicon-search"  title="Read privileges"></i>
                        </g:if>
                    </td>
                    <td>
                        ${userOrganismPermission.totalFeatureCount}
                    </td>
                    <td>${userOrganismPermission.geneCount}</td>
                    <td>
                        <g:if test="${userOrganismPermission.transcriptCount}">
                            <button>
                                Total
                                <span class="badge">${userOrganismPermission.transcriptCount}</span>
                            </button>
                            <button>
                                Protein encoding
                                <span class="badge"><g:formatNumber number="${userOrganismPermission.proteinCodingTranscriptPercent}" type="percent"/></span>
                            </button>
                            <button>
                                Exons / transcript
                                <span class="badge"><g:formatNumber number="${userOrganismPermission.exonsPerTranscript}" type="number"/></span>
                            </button>

                            <g:each in="${userOrganismPermission.transcriptTypeCount}" var="trans">
                                <button>
                                    ${trans.key}
                                    <span class="badge">
                                        ${trans.value}
                                    </span>
                                </button>
                            </g:each>
                        </g:if>
                        <g:else>0</g:else>
                    </td>
                    <td>${userOrganismPermission.exonCount} </td>
                    <td>${userOrganismPermission.transposableElementCount}</td>
                    <td>${userOrganismPermission.repeatRegionCount}</td>
                </tr>
            </g:each>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${annotatorInstanceCount ?: 0}"/>
    </div>
</div>

</body>
</html>
