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
                  from="${org.bbop.apollo.Organism.listOrderByCommonName()}" optionValue="commonName" optionKey="id" value="${organism.id}"
        onchange=" changeOrganism(); "
        />
    </div>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>
            <g:sortableColumn property="name" title="Name"/>
            <g:sortableColumn property="length" title="Length"/>
        </tr>
        </thead>
        <tbody>
        <g:each in="${sequenceInstanceList}" status="i" var="sequenceInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td>
                    <g:link action="show"
                            id="${sequenceInstance.id}">${fieldValue(bean: sequenceInstance, field: "name")}</g:link></td>
                <td>
                    ${sequenceInstance.length}
                    %{--<g:link uri="">Browse</g:link>--}%
                </td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${sequenceInstanceCount ?: 0}"/>
    </div>
</div>

</body>
</html>
