package org.bbop.apollo

import grails.test.mixin.TestFor
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TrackService)
//@Mock([Sequence, FeatureLocation, Feature,MRNA])
class TrackServiceSpec extends Specification {

    def setup() {
        service.trackMapperService = new TrackMapperService()
    }

    def cleanup() {
    }

    void "test something"() {

        given: "an NCList object for the C. elegans gene flp-1"
        // http://jbrowse.alliancegenome.org/jbrowse/index.html?data=data%2FMus%20musculus&tracks=All%20Genes&highlight=&lookupSymbol=Msx2&loc=13%3A53463164..53476782
//        http://localhost:8080/apollo/track/Mus%20musculus/All%20Genes/13:53466884..53473074.json?name=Msx2&ignoreCache=true
		String ncListString = new File("test/integration/resources/track-data/inputArray.json").text
        String classesForTrackString = new File("test/integration/resources/track-data/trackClasses.json").text


        println "converting: ${ncListString}"
        JSONArray ncListArray = new JSONArray(ncListString)
        println ncListArray.size()
        JSONArray classesForTrackArray = new JSONArray(classesForTrackString)
        println classesForTrackArray.size()
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: 'Caenorhabditis elegans'
                , trackName:'All Genes'
                , sequenceName: 'IV'
        )

        when: "we filter it to an object"
        service.storeTrackData(sequenceDTO,classesForTrackArray)
        JSONArray renderedArray = service.convertAllNCListToObject(ncListArray, sequenceDTO)


        then: "we should get all of the surrounding data and the data itself"
        // we should see 3 encompassing non-coding transcripts:  Gm33763 (MGI:5592922), XR_873404.1, XR_873405.1, XR_873406.1
        // we should see 2 coding transcripts with select=true, Msx2 (MGI:97169), ENSMUST00000021922, NM_013601.2
        assert renderedArray.size()==1
        assert renderedArray.getJSONObject(0).type=='gene'

        when: 'we get the very first gene'
        JSONObject firstGeneObject = renderedArray.getJSONObject(0)
        JSONArray firstGeneChildren = firstGeneObject.children

        then: "we should see the next level"
        assert firstGeneChildren.size()==4
        assert firstGeneChildren.getJSONObject(0).type=='ncRNA'
        assert firstGeneChildren.getJSONObject(1).type=='ncRNA'
        assert firstGeneChildren.getJSONObject(2).type=='ncRNA'
        assert firstGeneChildren.getJSONObject(3).type=='gene'

        when: "we get the coding gene"
        JSONObject codingGene = firstGeneChildren.getJSONObject(3)
        JSONArray codingGeneChildren = codingGene.children

        then: "we should see 2 mrnas"
        assert codingGeneChildren.size()==2
        assert codingGeneChildren.getJSONObject(0).type=='mRNA'
        assert codingGeneChildren.getJSONObject(0).children.size()>0
        assert codingGeneChildren.getJSONObject(1).type=='mRNA'
        assert codingGeneChildren.getJSONObject(1).children.size()>0


    }

    // TODO: repeat the same for mouse, ignoring the outter gene
}
