<%@ page import="org.bbop.apollo.JBrowseMapper" %>

%{--${data}--}%


<div class="panel panel-primary">
    <div class="panel-heading">
        <h3 class="panel-title">
            Primary Data
        </h3>
    </div>

    <div class="panel-body">
        <table class="table">
            <g:if test="${JBrowseMapper.hasName(data)}">
                <tr>
                    <td class="info">
                        Name
                    </td>

                    <td>
                        ${JBrowseMapper.getName(data)}
                    </td>
                </tr>
            </g:if>

            <tr>
                <td class="col-md-1 info">
                    Type
                </td>

                <td>
                    ${JBrowseMapper.getType(data)}
                </td>
            </tr>

            <g:if test="${JBrowseMapper.hasScore(data)}">
                <tr>
                    <td class="col-md-1 info">
                        Score
                    </td>

                    <td>
                        ${JBrowseMapper.getScore(data)}
                    </td>
                </tr>
            </g:if>

            <tr>
                <td class="col-md-1 info">
                    Position
                </td>

                <td>
                    %{--${JBrowseMapper.getSequence(data)}:${JBrowseMapper.getStart(data)}..${JBrowseMapper.getEnd(data)} (${JBrowseMapper.getStrand(data) > 0 ? '+' : '-'} strand)--}%
                    ${JBrowseMapper.getPosition(data)}
                </td>
            </tr>

            <tr>
                <td class="col-md-1 info">
                    Length
                </td>

                <td>
                    ${JBrowseMapper.getLength(data)} bp
                </td>
            </tr>
        </table>


        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title">Attributes</h3>
            </div>

            <div class="panel-body">
                <table class="table">
                    <g:if test="${JBrowseMapper.getId(data) != null}">
                        <tr>
                            <td class="col-md-1 info">
                                Id
                            </td>

                            <td>
                                ${JBrowseMapper.getId(data)}
                            </td>
                        </tr>
                    </g:if>
                    <g:if test="${JBrowseMapper.getPhase(data) != null}">
                        <tr>
                            <td class="info">
                                Phase
                            </td>

                            <td>
                                ${JBrowseMapper.getPhase(data)}
                            </td>
                        </tr>
                    </g:if>
                    <tr>
                        <td class="col-md-1 info">
                            Seq_id
                        </td>

                        <td>
                            ${JBrowseMapper.getSequence(data)}
                        </td>
                    </tr>
                    <tr>
                        <td class="info">
                            Source
                        </td>

                        <td>
                            ${JBrowseMapper.getSource(data)}
                        </td>
                    </tr>
                </table>

                <div class="badge badge-alert">
                    %{--Region Sequence  <g:link action="download" controller="" class="fa fa-save">FASTA</g:link>--}%
                    Region Sequence <a class="fa fa-download" href="#" onclick="saveTextAsFile('fasta-${JBrowseMapper.getPosition(data)}','${JBrowseMapper.getPosition(data)}.fa')">FASTA</a>
                </div>
                <br/>
                <g:if test="${JBrowseMapper.getPosition(data)!=null}">
                    <textarea id="fasta-${JBrowseMapper.getPosition(data)}" cols="120">${JBrowseMapper.getSequenceString(data,sequenceString,start,end)}</textarea>
                </g:if>
        </div>

        <g:if test="${data.size() > 10}">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h4 class="panel-title">Subfeatures</h4>
                </div>

                <div class="panel-body">
                    <g:each in="${data[10]}" var="subfeature">
                        <g:render template="dataTable" model="[data: subfeature, offset: offset]"/>
                    </g:each>
                </div>
            </div>
        </g:if>

    </div>
</div>

