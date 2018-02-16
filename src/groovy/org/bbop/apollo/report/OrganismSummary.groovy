package org.bbop.apollo.report

import org.bbop.apollo.User

/**
 * Created by nathandunn on 7/17/15.
 */
class OrganismSummary {
    int geneCount
    int transcriptCount
    int transposableElementCount
    int repeatRegionCount
    int exonCount
    int sequenceCount
    long organismId
    List<User> annotators
    Date lastUpdated

    Map<String, Integer> transcriptTypeCount
    Map<String, Integer> geneTypeCount

    int getTotalFeatureCount() {
        transcriptCount + repeatRegionCount + transposableElementCount
    }

    float getProteinCodingFeaturePercent() {
        totalFeatureCount && transcriptTypeCount.containsKey("MRNA") ? ((float) transcriptTypeCount.get("MRNA") / (float) totalFeatureCount).round(2) : 0
    }

    float getProteinCodingTranscriptPercent() {
        transcriptCount && transcriptTypeCount.containsKey("MRNA")? ((float) transcriptTypeCount.get("MRNA") / (float) transcriptCount).round(2) : 0
    }

    float getExonsPerTranscript() {
        ((float) exonCount / (float) transcriptCount).round(2)
    }
}
