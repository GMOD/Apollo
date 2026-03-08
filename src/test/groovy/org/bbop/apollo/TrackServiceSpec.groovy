package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.bbop.apollo.sequence.SequenceDTO
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import spock.lang.Specification

class TrackServiceSpec extends Specification implements ServiceUnitTest<TrackService>, DataTest {

    def setup() {
        service.trackMapperService = new TrackMapperService()
    }

    def cleanup() {
    }

    void "return embedded JSON from NCList"() {

        given: "an NCList object for the C. elegans gene flp-1"
        String ncListString = new File("src/test/resources/track-data/inputNcList.json").text
        String classesForTrackString = new File("src/test/resources/track-data/trackClasses.json").text

        JSONArray ncListArray = new JSONArray(ncListString)
        JSONArray classesForTrackArray = new JSONArray(classesForTrackString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: 'Caenorhabditis elegans'
                , trackName: 'All Genes'
                , sequenceName: 'IV'
        )

        when: "we filter it to an object"
        service.storeTrackData(sequenceDTO, classesForTrackArray)
        JSONArray renderedArray = service.convertAllNCListToObject(ncListArray, sequenceDTO)

        then: "we should get all of the surrounding data and the data itself"
        assert renderedArray.size() == 1
        assert renderedArray.getJSONObject(0).type == 'gene'

        when: 'we get the very first gene'
        JSONObject firstGeneObject = renderedArray.getJSONObject(0)
        JSONArray firstGeneChildren = firstGeneObject.children

        then: "we should see the next level"
        assert firstGeneChildren.size() == 4
        assert firstGeneChildren.getJSONObject(0).type == 'ncRNA'
        assert firstGeneChildren.getJSONObject(1).type == 'ncRNA'
        assert firstGeneChildren.getJSONObject(2).type == 'ncRNA'
        assert firstGeneChildren.getJSONArray(3)

        when: "we get the coding gene"
        JSONArray subArray = firstGeneChildren.getJSONArray(3)
        JSONObject codingGene = subArray.getJSONObject(0)
        JSONArray codingGeneChildren = codingGene.children

        then: "we should see 2 mrnas"
        assert codingGeneChildren.size() == 2
        assert codingGeneChildren.getJSONObject(0).type == 'mRNA'
        assert codingGeneChildren.getJSONObject(0).children.size() > 0
        assert codingGeneChildren.getJSONObject(1).type == 'mRNA'
        assert codingGeneChildren.getJSONObject(1).children.size() > 0
    }

    void "flatten nested mouse"() {

        given: "a nested JSON object"
        JSONArray inputArray = new JSONArray(new File("src/test/resources/track-data/mouseMsx2.json").text)

        when: "we flatten it"
        JSONArray renderedArray = service.flattenArray(inputArray, 'gene')

        then: "we should see an expected result"
        assert renderedArray.size() == 2

        when: "we get the two genes out"
        JSONObject gene1 = renderedArray.getJSONObject(0)
        JSONObject gene2 = renderedArray.getJSONObject(1)

        then: "we should see the appropriate structure"
        assert gene1.type == 'gene'
        assert gene1.name == 'Msx1os'
        assert gene1.children.size() == 1
        assert gene1.children[0].children.size() == 1
        assert gene1.children[0].type == 'ncRNA'

        assert gene2.type == 'gene'
        assert gene2.name == 'Msx1'
        assert gene2.children.size() == 3
        assert gene2.children[0].children.size() == 4
        assert gene2.children[0].type == 'mRNA'
    }

    void "flatten nested worm"() {

        given: "a nested JSON object"
        JSONArray inputArray = new JSONArray(new File("src/test/resources/track-data/wormGeneflp-1.json").text)

        when: "we flatten it"
        JSONArray renderedArray = service.flattenArray(inputArray, 'gene')

        then: "we should see an expected result"
        assert renderedArray.size() == 8
        for (obj in renderedArray) {
            assert obj.type == 'gene'
        }
    }
}
