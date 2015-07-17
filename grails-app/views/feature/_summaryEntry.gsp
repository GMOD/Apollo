<div class="row input-group col-md-offset-0 col-md-4">
    <ul class="list-group">
        <li class="list-group-item list-group-item-info">
            ${organism ? organism.commonName : "ALL"}
        </li>
        <li class="list-group-item">
            Genes <span class="badge">${summaryData.geneCount}</span>
        </li>
        <li class="list-group-item">
            Transcripts <span class="badge">${summaryData.transcriptCount}</span>
        </li>
        <li class="list-group-item">
            Transposable Elements <span class="badge">${summaryData.transposableElementCount}</span>
        </li>
        <li class="list-group-item">
            Repeat Regions <span class="badge">${summaryData.repeatRegionCount}</span>
        </li>
        <li class="list-group-item">
            Exons <span class="badge">${summaryData.exonCount}</span>
        </li>
    </ul>
</div>
