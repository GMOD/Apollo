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
    <div class="page-header">

        <h3>Annotation Details: <span class="label label-default"> ${annotatorInstance.firstName} ${annotatorInstance.lastName}
                (${annotatorInstance.username})</span></h3>
    </div>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <div>
        <i class="glyphicon glyphicon-cog" title="Administrate privileges"></i><span class="legend-icon">ADMINISTRATE</span>
        <i class="glyphicon glyphicon-edit" title="Edit privileges"></i><span class="legend-icon">WRITE</span>
        <i class="glyphicon glyphicon-download-alt" title="Export privileges"></i><span class="legend-icon">EXPORT</span>
        <i class="glyphicon glyphicon-search" title="Read privileges"></i><span class="legend-icon">READ</span>
    </div>

    <h3>Total</h3>
    <table>
        <thead>
        <tr>
            <th>Top Level Features</th>
            <th>Genes</th>
            <th>Transcripts</th>
            <th>Exons</th>
            <th>Transposable Elements</th>
            <th>Repeat Regions</th>
        </tr>
        </thead>
        <tbody>
        <tr>
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
        </tbody>
    </table>

    <h3>Organism Breakdown</h3>
    <table>
        <thead>
        <th>Organism</th>
        <th>Permissions</th>
        <th>Top Level Features</th>
        <th>Genes</th>
        <th>Transcripts</th>
        <th>Exons</th>
        <th>Transposable Elements</th>
        <th>Repeat Regions</th>
        <th>Last Updated</th>
        </thead>
        <tbody>
        <g:each in="${annotatorInstance.userOrganismPermissionList}" var="userOrganismPermission">
            <tr>
                <td>
                    <g:link action="report" id="${userOrganismPermission.userOrganismPermission.organism.id}"
                            controller="sequence">
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
                        <i class="glyphicon glyphicon-search" title="Read privileges"></i>
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
                            <span class="badge"><g:formatNumber
                                    number="${userOrganismPermission.proteinCodingTranscriptPercent}"
                                    type="percent"/></span>
                        </button>
                        <button>
                            Exons / transcript
                            <span class="badge"><g:formatNumber number="${userOrganismPermission.exonsPerTranscript}"
                                                                type="number"/></span>
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
                <td>${userOrganismPermission.exonCount}</td>
                <td>${userOrganismPermission.transposableElementCount}</td>
                <td>${userOrganismPermission.repeatRegionCount}</td>
                <td>
                <g:formatDate format="dd-MMM-yy HH:mm (E z)" date="${userOrganismPermission.lastUpdated}"/>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>

</div>

</body>
</html>
