package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.report.AnnotatorSummary
import org.bbop.apollo.report.OrganismSummary
import org.bbop.apollo.report.SequenceSummary

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


    def generateSequenceSummary(Sequence sequence){
        SequenceSummary sequenceSummary = new SequenceSummary()
        sequenceSummary.sequence = sequence
        sequenceSummary.geneCount = (int) Gene.executeQuery("select count(g) from Gene g join g.featureLocations fl join fl.sequence s where s = :sequence ",[sequence:sequence]).iterator().next()
        sequenceSummary.transposableElementCount = (int) TransposableElement.executeQuery("select count(g) from TransposableElement  g join g.featureLocations fl join fl.sequence s where s = :sequence ",[sequence:sequence]).iterator().next()
        sequenceSummary.repeatRegionCount = (int) RepeatRegion.executeQuery("select count(g) from RepeatRegion  g join g.featureLocations fl join fl.sequence s where s = :sequence ",[sequence:sequence]).iterator().next()
        sequenceSummary.exonCount = (int) Exon.executeQuery("select count(g) from Exon g join g.featureLocations fl join fl.sequence s where s = :sequence ",[sequence:sequence]).iterator().next()
        sequenceSummary.annotators = User.executeQuery("select distinct annotator from Feature g join g.featureLocations fl join fl.sequence s join g.owners annotator where s = :sequence ",[sequence:sequence])


        Map<String, Integer> transcriptMap = new TreeMap<>()
        Transcript.executeQuery("select distinct g from Transcript g join g.featureLocations fl join fl.sequence s  where s = :sequence", [sequence: sequence]).each {
//            println "it: ${it}"
            String className = it.class.canonicalName.substring("org.bbop.apollo.".size())
            Integer count = transcriptMap.get(className) ?: 0
            transcriptMap.put(className, ++count)
        }
        sequenceSummary.transcriptTypeCount = transcriptMap
        if (transcriptMap) {
            sequenceSummary.transcriptCount = transcriptMap.values()?.sum()
        } else {
            sequenceSummary.transcriptCount = 0
        }


        return sequenceSummary
    }

    AnnotatorSummary generateAnnotatorSummary(User owner) {
        AnnotatorSummary annotatorSummary = new AnnotatorSummary()
        annotatorSummary.annotator = owner
        annotatorSummary.geneCount = (int) Gene.executeQuery("select count(distinct g) from Gene g join g.owners owner where owner = :owner ",[owner:owner]).iterator().next()
        annotatorSummary.transposableElementCount = (int) TransposableElement.executeQuery("select count(distinct g) from TransposableElement g join g.owners owner where owner = :owner",[owner: owner]).iterator().next()
        annotatorSummary.repeatRegionCount = (int) TransposableElement.executeQuery("select count(distinct g) from RepeatRegion g join g.owners owner where owner = :owner",[owner: owner]).iterator().next()
        annotatorSummary.exonCount = (int) TransposableElement.executeQuery("select count(distinct g) from Exon g join g.childFeatureRelationships child join child.parentFeature.owners owner where owner = :owner",[owner: owner]).iterator().next()


        Map<String, Integer> transcriptMap = new TreeMap<>()
        Transcript.executeQuery("select distinct g from Transcript g join g.owners owner where owner = :owner ", [owner: owner]).each {
//            println "it: ${it}"
            String className = it.class.canonicalName.substring("org.bbop.apollo.".size())
            Integer count = transcriptMap.get(className) ?: 0
            transcriptMap.put(className, ++count)
        }
        annotatorSummary.transcriptTypeCount = transcriptMap
        if (transcriptMap) {
            annotatorSummary.transcriptCount = transcriptMap.values()?.sum()
        } else {
            annotatorSummary.transcriptCount = 0
        }


        return annotatorSummary
    }
}
