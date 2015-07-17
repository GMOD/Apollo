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
        thisFeatureSummaryInstance.geneCount = (int) Gene.executeQuery("select count(distinct g) from Gene g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()




        Map<String, Integer> transcriptMap = new TreeMap<>()
        Transcript.executeQuery("select distinct g from Transcript g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).each {
//            println "it: ${it}"
            String className = it.class.canonicalName.substring("org.bbop.apollo.".size())
            Integer count = transcriptMap.get(className) ?: 0
            transcriptMap.put(className, ++count)
        }
        thisFeatureSummaryInstance.transcriptTypeCount = transcriptMap
        if (transcriptMap) {
            thisFeatureSummaryInstance.transcriptCount = transcriptMap.values()?.sum()
        } else {
            thisFeatureSummaryInstance.transcriptCount = 0
        }

        thisFeatureSummaryInstance.transposableElementCount = (int) TransposableElement.executeQuery("select count(distinct g) from TransposableElement g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()
        thisFeatureSummaryInstance.repeatRegionCount = (int) RepeatRegion.executeQuery("select count(distinct g) from RepeatRegion g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()
        thisFeatureSummaryInstance.exonCount = (int) Exon.executeQuery("select count(distinct g) from Exon g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()
        return thisFeatureSummaryInstance
    }
}
