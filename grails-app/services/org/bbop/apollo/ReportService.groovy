package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.report.FeatureSummary

@Transactional
class ReportService {

    def generateAllFeatureSummary() {
        FeatureSummary featureSummaryInstance = new FeatureSummary()
        featureSummaryInstance.geneCount = Gene.count
        featureSummaryInstance.transcriptCount = Transcript.count
        featureSummaryInstance.transposableElementCount = TransposableElement.count
        featureSummaryInstance.repeatRegionCount = RepeatRegion.count
        featureSummaryInstance.exonCount = Exon.count
        return featureSummaryInstance
    }

    def generateFeatureSummary(Organism organism) {
        FeatureSummary thisFeatureSummaryInstance = new FeatureSummary()
        thisFeatureSummaryInstance.geneCount = (int) Gene.executeQuery("select count(distinct g) from Gene g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism",[organism:organism]).iterator().next()
        thisFeatureSummaryInstance.transcriptCount = (int) Transcript.executeQuery("select count(distinct g) from Transcript g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism",[organism:organism]).iterator().next()
        thisFeatureSummaryInstance.transposableElementCount = (int) TransposableElement.executeQuery("select count(distinct g) from TransposableElement g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism",[organism:organism]).iterator().next()
        thisFeatureSummaryInstance.repeatRegionCount = (int) RepeatRegion.executeQuery("select count(distinct g) from RepeatRegion g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism",[organism:organism]).iterator().next()
        thisFeatureSummaryInstance.exonCount = (int) Exon.executeQuery("select count(distinct g) from Exon g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism",[organism:organism]).iterator().next()
        return thisFeatureSummaryInstance
    }
}
