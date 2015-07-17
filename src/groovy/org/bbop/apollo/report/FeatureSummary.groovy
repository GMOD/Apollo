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

    Map<String, Integer> transcriptTypeCount
    Map<String, Integer> geneTypeCount

    int getTotalFeatureCount() {
        transcriptCount + repeatRegionCount + transposableElementCount
    }

    float getProteinCodingFeaturePercent() {
        totalFeatureCount && transcriptTypeCount ? ((float) transcriptTypeCount.get("MRNA") / (float) totalFeatureCount).round(2) : 0
    }

    float getProteinCodingTranscriptPercent() {
        transcriptCount && transcriptTypeCount? ((float) transcriptTypeCount.get("MRNA") / (float) transcriptCount).round(2) : 0
    }

    float getExonsPerTranscript() {
        ((float) exonCount / (float) transcriptCount).round(2)
    }
}
