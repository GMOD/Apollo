



<h5>${organism ? organism.commonName : "All" }</h5>
<ol class="property-list feature">
    <li class="fieldcontain">
        <span class="property-label">Gene Count</span>
        <span class="property-value" aria-labelledby="name-label">
            ${summaryData.geneCount}
        </span>
    </li>
    <li class="fieldcontain">
        <span class="property-label">Transcript Count</span>
        <span class="property-value" aria-labelledby="name-label">
            ${summaryData.transcriptCount}
        </span>
    </li>
    <li class="fieldcontain">
        <span class="property-label">Transposable Element Count</span>
        <span class="property-value" aria-labelledby="name-label">
            ${summaryData.transposableElementCount}
        </span>
    </li>
    <li class="fieldcontain">
        <span class="property-label">Repeat Region Count</span>
        <span class="property-value" aria-labelledby="name-label">
            ${summaryData.repeatRegionCount}
        </span>
    </li>
    <li class="fieldcontain">
        <span class="property-label">Exon Count</span>
        <span class="property-value" aria-labelledby="name-label">
            ${summaryData.exonCount}
        </span>
    </li>
</ol>
