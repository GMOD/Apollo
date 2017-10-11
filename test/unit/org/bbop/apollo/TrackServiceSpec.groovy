package org.bbop.apollo

import grails.test.mixin.TestFor
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TrackService)
//@Mock([Sequence, FeatureLocation, Feature,MRNA])
class TrackServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {

        given: "an NCList object for the C. elegans gene flp-1"
        // http://localhost:8080/apollo/track/Caenorhabditis%20elegans/All%20Genes/IV:9144692..9146122.json?name=flp-1&ignoreCache=true
        // http://jbrowse.alliancegenome.org/jbrowse/index.html?data=data%2FCaenorhabditis%20elegans&tracks=All%20Genes&highlight=&lookupSymbol=flp-1&loc=IV%3A9143834..9146980
		String ncListString = ""
        String classesForTrackString = ""


        JSONArray ncListArray = new JSONArray(ncListString)
        JSONArray classesForTrackArray = new JSONArray(classesForTrackString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: 'Wormbase'
                , trackName:'All Genes'
                , sequenceName: 'IV'
        )

        when: "we filter it to an object"
        service.storeTrackData(sequenceDTO,classesForTrackArray)
        JSONArray renderedArray = service.convertAllNCListToObject(ncListArray, sequenceDTO)


        then: "we should get all of the surrounding data and the data itself"
        // we should see F23B2.5 / flp-q (selecte d= true)
           // Fb23B2.5a
            // Fb23B2.5b
            // Fb23B2.5c
        // we should see F23B2.4 (large and encompassing gene)
        assert renderedArray.size()==4

        // I think we ignore the small non-coding genes, though

    }

    // TODO: repeat the same for mouse, ignoring the outter gene
    // http://jbrowse.alliancegenome.org/jbrowse/index.html?data=data%2FMus%20musculus&tracks=All%20Genes&highlight=&lookupSymbol=Msx2&loc=13%3A53463164..53476782
}
