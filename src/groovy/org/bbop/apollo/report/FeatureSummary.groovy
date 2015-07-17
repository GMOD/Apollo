package org.bbop.apollo.report

/**
 * Created by nathandunn on 7/17/15.
 */
class FeatureSummary {
    int geneCount
    int transcriptCount
    int transposableElementCount
    int repeatRegionCount
    int exonCount

    Map<String,Integer> transcriptTypeCount
    Map<String,Integer> geneTypeCount

    float getExonsPerTranscript(){
        ((float) exonCount / (float) transcriptCount).round(2)
    }
}
