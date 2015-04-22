package org.bbop.apollo

import grails.transaction.Transactional
import groovy.json.JsonBuilder
import org.bbop.apollo.sequence.search.blast.TabDelimittedAlignment
import org.bbop.apollo.web.util.JSONUtil
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.tools.seq.search.blat.BlatCommandLine


@Transactional
class SequenceSearchService {

    def configWrapperService
    def serviceMethod() {

    }


    /*todo: use json builder instead of JSON objects
    def getSequenceSearchTools() {
        JsonBuilder json = new JsonBuilder ()
        JSONArray sequenceSearchToolsArray = new JSONArray()
        json.sequence_search_tools configWrapperService.getSequenceSearchTools(), { k,v ->
            k
        }
        println json.toString()
        return json.toString()
    }*/

    def getSequenceSearchTools() {
        JSONArray sequenceSearchToolsArray = new JSONArray();
        configWrapperService.getSequenceSearchTools().each { k, v ->
            sequenceSearchToolsArray.put(v.name);
        }
        return new JSONObject().put("sequence_search_tools", sequenceSearchToolsArray).toString();
    }
    def searchSequence(JSONObject input, String database) {

        String ret=input.get('search').get('key')
        JSONObject searchUtils=configWrapperService.getSequenceSearchTools().get(ret)
        searchUtils.put("database",database)

        def searcher=this.class.classLoader.loadClass( searchUtils.get('search_class'), true, false )?.newInstance()
        searcher.parseConfiguration(searchUtils)
        Collection<TabDelimittedAlignment> results=searcher.search('blat',
                input.get('search').residues,
                input.get('search').database_id)

        JsonBuilder json = new JsonBuilder ()
        json.matches results, { TabDelimittedAlignment result ->
            "identity" result.percentId
            "significance" result.eValue
            "subject"({
                "location" ({
                    "fmin" result.subjectStart
                    "fmax" result.subjectEnd
                    "strand" result.subjectStrand
                })
                "feature" ({
                    "uniquename" result.subjectId
                    "type"({
                        "name" "region"
                        "cv" ({
                            "name" "sequence"
                        })
                    })
                })
            })
            "query"({
                "location" ({
                    "fmin" result.queryStart
                    "fmax" result.queryEnd
                    "strand" result.queryStrand
                })
                "feature" ({
                    "uniquename" result.queryId
                    "type" ({
                        "name" "region"
                        "cv"({
                            "name" "sequence"
                        })
                    })
                })
            })

            rawscore result.bitscore
        }
        return json.toString()
    }


}
