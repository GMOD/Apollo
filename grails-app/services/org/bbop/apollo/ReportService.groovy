package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.report.OrganismSummary

@Transactional
class ReportService {

    def generateAllFeatureSummary() {
        OrganismSummary thisFeatureSummaryInstance = new OrganismSummary()
        thisFeatureSummaryInstance.geneCount = Gene.count


        Map<String, Integer> transcriptMap = new TreeMap<>()
        Transcript.executeQuery("select distinct g from Transcript g ").each {
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

        thisFeatureSummaryInstance.transcriptCount = Transcript.count
        thisFeatureSummaryInstance.transposableElementCount = TransposableElement.count
        thisFeatureSummaryInstance.repeatRegionCount = RepeatRegion.count
        thisFeatureSummaryInstance.exonCount = Exon.count
        thisFeatureSummaryInstance.sequenceCount = Sequence.count



        return thisFeatureSummaryInstance
    }

    def generateOrganismSummary(Organism organism) {
        OrganismSummary thisFeatureSummaryInstance = new OrganismSummary()
        thisFeatureSummaryInstance.geneCount = (int) Gene.executeQuery("select count(distinct g) from Gene g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()

        thisFeatureSummaryInstance.annotators = User.executeQuery("select distinct own from Feature g join g.featureLocations fl join fl.sequence s join s.organism o join g.owners own where o = :organism", [organism: organism])


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
        thisFeatureSummaryInstance.sequenceCount = Sequence.countByOrganism(organism)
        thisFeatureSummaryInstance.organismId = organism.id

        thisFeatureSummaryInstance.transposableElementCount = (int) TransposableElement.executeQuery("select count(distinct g) from TransposableElement g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()
        thisFeatureSummaryInstance.repeatRegionCount = (int) RepeatRegion.executeQuery("select count(distinct g) from RepeatRegion g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()
        thisFeatureSummaryInstance.exonCount = (int) Exon.executeQuery("select count(distinct g) from Exon g join g.featureLocations fl join fl.sequence s join s.organism o where o = :organism", [organism: organism]).iterator().next()
        return thisFeatureSummaryInstance
    }

}
