package org.bbop.apollo

import grails.test.mixin.TestFor
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(VariantAnnotationService)
class VariantAnnotationServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }


    @Ignore
    void "Convert a genomic coordinate to a local coordinate and back"() {

        when: "we have 3 source coordinates that fall on a transcript on the forward strand"
        // GB40841-RA
        int featureFmin = 1071210
        int featureFmax = 1071492

        int sourceCoordinate1 = 1071224
        int sourceCoordinate2 = 1071364
        int sourceCoordinate3 = 1071474

        then: "converting them to local coordinate, wrt. the transcript, should give the right values"
        int localCoordinate1 = service.convertSourceCoordinateToLocalCoordinate(featureFmin, featureFmax, 1, sourceCoordinate1)
        int localCoordinate2 = service.convertSourceCoordinateToLocalCoordinate(featureFmin, featureFmax, 1, sourceCoordinate2)
        int localCoordinate3 = service.convertSourceCoordinateToLocalCoordinate(featureFmin, featureFmax, 1, sourceCoordinate3)

        assert localCoordinate1 == 14
        assert localCoordinate2 == 154
        assert localCoordinate3 == 264

        then: "converting the local coordinates back to source coordinate should give the original source coordinate"
        int reconvertedSourceCoordinate1 = service.convertLocalCoordinateToSourceCoordinate(featureFmin, featureFmax, 1, localCoordinate1)
        int reconvertedSourceCoordinate2 = service.convertLocalCoordinateToSourceCoordinate(featureFmin, featureFmax, 1, localCoordinate2)
        int reconvertedSourceCoordinate3 = service.convertLocalCoordinateToSourceCoordinate(featureFmin, featureFmax, 1, localCoordinate3)

        assert reconvertedSourceCoordinate1 == sourceCoordinate1
        assert reconvertedSourceCoordinate2 == sourceCoordinate2
        assert reconvertedSourceCoordinate3 == sourceCoordinate3

        when: "we have 3 source coordinates that fall on a transcript on the reverse strand"
        // GB40736-RA
        int featureFmin2 = 938708
        int featureFmax2 = 939601

        int sourceCoordinate4 = 938724
        int sourceCoordinate5 = 938811
        int sourceCoordinate6 = 939591

        then: "converting them to local coordinate, wrt. the transcript, should give the right values"
        int localCoordinate4 = service.convertSourceCoordinateToLocalCoordinate(featureFmin2, featureFmax2, -1, sourceCoordinate4)
        int localCoordinate5 = service.convertSourceCoordinateToLocalCoordinate(featureFmin2, featureFmax2, -1, sourceCoordinate5)
        int localCoordinate6 = service.convertSourceCoordinateToLocalCoordinate(featureFmin2, featureFmax2, -1, sourceCoordinate6)

        assert localCoordinate4 == 876
        assert localCoordinate5 == 789
        assert localCoordinate6 == 9

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate4 = service.convertLocalCoordinateToSourceCoordinate(featureFmin2, featureFmax2, -1, localCoordinate4)
        int reconvertedSourceCoordinate5 = service.convertLocalCoordinateToSourceCoordinate(featureFmin2, featureFmax2, -1, localCoordinate5)
        int reconvertedSourceCoordinate6 = service.convertLocalCoordinateToSourceCoordinate(featureFmin2, featureFmax2, -1, localCoordinate6)

        assert reconvertedSourceCoordinate4 == sourceCoordinate4
        assert reconvertedSourceCoordinate5 == sourceCoordinate5
        assert reconvertedSourceCoordinate6 == sourceCoordinate6

    }

    @Ignore
    void "Converting genomic coordinate to cDNA local coordinate and back"() {

        when: "we have 3 source coordinates that fall on a transcript on the forward strand"
        // GB40829-RA
        def featureExonFminArray1 = [747699, 747822, 747946]
        def featureExonFmaxArray1 = [747760, 747894, 747966]

        int sourceCoordinate1 = 747749
        int sourceCoordinate2 = 747803
        int sourceCoordinate3 = 747849

        then: "converting them to local coordinate, wrt. the transcript exons, should give the right values"
        int localCoordinate1 = service.convertSourceCoordinateToLocalCoordinateForTranscript(featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate1)
        int localCoordinate2 = service.convertSourceCoordinateToLocalCoordinateForTranscript(featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate2)
        int localCoordinate3 = service.convertSourceCoordinateToLocalCoordinateForTranscript(featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate3)

        assert localCoordinate1 == 50
        assert localCoordinate2 == -1
        assert localCoordinate3 == 88

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate1 = service.convertLocalCoordinateToSourceCoordinateForTranscript(featureExonFminArray1, featureExonFmaxArray1, 1, localCoordinate1)
        int reconvertedSourceCoordinate3 = service.convertLocalCoordinateToSourceCoordinateForTranscript(featureExonFminArray1, featureExonFmaxArray1, 1, localCoordinate3)

        assert reconvertedSourceCoordinate1 == sourceCoordinate1
        assert reconvertedSourceCoordinate3 == sourceCoordinate3

        when: "we have 3 source coordinates that fall on a transcript on the reverse strand"
        // GB40743-RA
        def featureExonFminArray2 = [775035, 775344]
        def featureExonFmaxArray2 = [775185, 775413]

        int sourceCoordinate4 = 775399
        int sourceCoordinate5 = 775199
        int sourceCoordinate6 = 775083

        then: "converting them to local coordinate, wrt. the transcript exons, should give the right values"
        int localCoordinate4 = service.convertSourceCoordinateToLocalCoordinateForTranscript([775035, 775344], [775185, 775413], -1, sourceCoordinate4)
        int localCoordinate5 = service.convertSourceCoordinateToLocalCoordinateForTranscript([775035, 775344], [775185, 775413], -1, sourceCoordinate5)
        int localCoordinate6 = service.convertSourceCoordinateToLocalCoordinateForTranscript([775035, 775344], [775185, 775413], -1, sourceCoordinate6)

        assert localCoordinate4 == 13
        assert localCoordinate5 == -1
        assert localCoordinate6 == 170

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate4 = service.convertLocalCoordinateToSourceCoordinateForTranscript([775035, 775344], [775185, 775413], -1, localCoordinate4)
        int reconvertedSourceCoordinate6 = service.convertLocalCoordinateToSourceCoordinateForTranscript([775035, 775344], [775185, 775413], -1, localCoordinate6)

        assert reconvertedSourceCoordinate4 == sourceCoordinate4
        assert reconvertedSourceCoordinate6 == sourceCoordinate6

    }

    @Ignore
    void "Converting genomic coordinate to CDS local coordinate and back for transcripts that have UTRs"() {

        when: "we have 5 source coordinates that fall on a transcript on the forward strand"
        // GB40828-RA
        def featureExonFminArray1 = [734606, 734930, 735245]
        def featureExonFmaxArray1 = [734766, 735014, 735570]
        def featureCdsFmin = 734733
        def featureCdsFmax = 735446

        int sourceCoordinate1 = 734749
        int sourceCoordinate2 = 734899
        int sourceCoordinate3 = 735386
        int sourceCoordinate4 = 734674
        int sourceCoordinate5 = 735498

        then: "converting them to local coordinate, wrt. the transcript CDS, should give the right values"
        int localCoordinate1 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate1)
        int localCoordinate2 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate2)
        int localCoordinate3 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate3)
        int localCoordinate4 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate4)
        int localCoordinate5 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, sourceCoordinate5)

        assert localCoordinate1 == 16
        assert localCoordinate2 == -1
        assert localCoordinate3 == 258
        assert localCoordinate4 == -1
        assert localCoordinate5 == -1

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate1 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, localCoordinate1)
        int reconvertedSourceCoordinate3 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray1, featureExonFmaxArray1, 1, localCoordinate3)

        assert reconvertedSourceCoordinate1 == sourceCoordinate1
        assert reconvertedSourceCoordinate3 == sourceCoordinate3

        when: "we have 6 source coordinates that fall on a transcript on the forward strand"
        // GB40745-RA
        def featureExonFminArray2 = [731930, 732909]
        def featureExonFmaxArray2 = [732539, 733316]
        def featureCdsFmin2 = 732023
        def featureCdsFmax2 = 733182

        int sourceCoordinate6 = 732999
        int sourceCoordinate7 = 732875
        int sourceCoordinate8 = 732499
        int sourceCoordinate9 = 733224
        int sourceCoordinate10 = 731974
        int sourceCoordinate11 = 732085

        then: "converting them to local coordinate, wrt. the transcript CDS, should give the right values"
        int localCoordinate6 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [731930, 732909], [732539, 733316], -1, sourceCoordinate6)
        int localCoordinate8 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [731930, 732909], [732539, 733316], -1, sourceCoordinate8)
        int localCoordinate11 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [731930, 732909], [732539, 733316], -1, sourceCoordinate11)

        assert localCoordinate6 == 182
        assert localCoordinate8 == 312
        assert localCoordinate11 == 726

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate6 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [731930, 732909], [732539, 733316], -1, localCoordinate6)
        int reconvertedSourceCoordinate8 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [731930, 732909], [732539, 733316], -1, localCoordinate8)
        int reconvertedSourceCoordinate11 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [731930, 732909], [732539, 733316], -1, localCoordinate11)

        assert reconvertedSourceCoordinate6 == sourceCoordinate6
        assert reconvertedSourceCoordinate8 == sourceCoordinate8
        assert reconvertedSourceCoordinate11 == sourceCoordinate11
    }

    @Ignore
    void "Converting genomic coordinate to CDS local coordinate and back, for transcripts that have no UTRs"() {
        when: "we have 4 source coordinates that fall on a transcript on the forward strand"
        // GB40827-RA
        def featureExonFminArray = [729928, 730296]
        def featureExonFmaxArray = [730010, 730304]
        int featureCdsFmin = 729928
        int featureCdsFmax = 730304

        int sourceCoordinate1 = 729949
        int sourceCoordinate2 = 730009
        int sourceCoordinate3 = 730101
        int sourceCoordinate4 = 730301

        then: "converting them to local coordinate, wrt. the transcript CDS, should give the right values"
        int localCoordinate1a = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, sourceCoordinate1)
        int localCoordinate1b = service.convertSourceCoordinateToLocalCoordinateForTranscript(featureExonFminArray, featureExonFmaxArray, 1, sourceCoordinate1)
        assert localCoordinate1a == localCoordinate1b
        int localCoordinate2 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, sourceCoordinate2)
        int localCoordinate3 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, sourceCoordinate3)
        int localCoordinate4 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, sourceCoordinate4)

        assert localCoordinate1a == 21
        assert localCoordinate2 == 81
        assert localCoordinate3 == -1
        assert localCoordinate4 == 87

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate1a = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, localCoordinate1a)
        int reconvertedSourceCoordinate1b = service.convertLocalCoordinateToSourceCoordinateForTranscript(featureExonFminArray, featureExonFmaxArray, 1, localCoordinate1b)
        assert reconvertedSourceCoordinate1a == reconvertedSourceCoordinate1b
        int reconvertedSourceCoordinate2 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, localCoordinate2)
        int reconvertedSourceCoordinate4 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin, featureCdsFmax, featureExonFminArray, featureExonFmaxArray, 1, localCoordinate4)
        assert reconvertedSourceCoordinate2 == sourceCoordinate2
        assert reconvertedSourceCoordinate4 == sourceCoordinate4

        when: "we have 4 source coordinates that fall on a transcript on the reverse strand"
        // GB40739-RA
        def featureExonFminArray2 = [845782, 847144]
        def featureExonFmaxArray2 = [845798, 847278]
        def featureCdsFmin2 = 845782
        def featureCdsFmax2 = 847278

        int sourceCoordinate5 = 847249
        int sourceCoordinate6 = 847144
        int sourceCoordinate7 = 846800
        int sourceCoordinate8 = 845787

        then: "converting them to local coordinate, wrt. the transcript CDS, should give the right values"
        int localCoordinate5a = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, sourceCoordinate5)
        int localCoordinate5b = service.convertSourceCoordinateToLocalCoordinateForTranscript([845782, 847144], [845798, 847278], -1, sourceCoordinate5)
        assert localCoordinate5a == localCoordinate5b
        int localCoordinate6 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, sourceCoordinate6)
        int localCoordinate7 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, sourceCoordinate7)
        int localCoordinate8 = service.convertSourceCoordinateToLocalCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, sourceCoordinate8)

        assert localCoordinate5a == 28
        assert localCoordinate6 == 133
        assert localCoordinate7 == -1
        assert localCoordinate8 == 144

        then: "converting the local coordinates back to source coordinate should give the original source coordinates"
        int reconvertedSourceCoordinate5a = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, localCoordinate5a)
        int reconvertedSourceCoordinate5b = service.convertLocalCoordinateToSourceCoordinateForTranscript([845782, 847144], [845798, 847278], -1, localCoordinate5b)
        int reconvertedSourceCoordinate6 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, localCoordinate6)
        int reconvertedSourceCoordinate8 = service.convertLocalCoordinateToSourceCoordinateForCDS(featureCdsFmin2, featureCdsFmax2, [845782, 847144], [845798, 847278], -1, localCoordinate8)

        assert reconvertedSourceCoordinate5a == sourceCoordinate5
        assert reconvertedSourceCoordinate5b == sourceCoordinate5
        assert reconvertedSourceCoordinate6 == sourceCoordinate6
        assert reconvertedSourceCoordinate8 == sourceCoordinate8


    }
}
