package org.bbop.apollo

import grails.transaction.Transactional
import groovy.json.JsonBuilder
import org.bbop.apollo.sequence.search.blast.TabDelimittedAlignment
import org.codehaus.groovy.grails.web.json.JSONObject


@Transactional
class SequenceSearchService {

    def configWrapperService

    def searchSequence(JSONObject input, String database) {


        try {
            String ret=input.get('search').get('key')
            JSONObject searchUtils=configWrapperService.getSequenceSearchTools().get(ret)
            searchUtils.put("database",database)

            // dynamically allocate a search_class
            def searcher=this.class.classLoader.loadClass( searchUtils.get('search_class'))?.newInstance()

            // pass configuration
            searcher.parseConfiguration(searchUtils)

            Collection<TabDelimittedAlignment> results = searcher.search('searchid',
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
        catch(Exception e) {
            def obj=new JSONObject()
            obj.put("error",e.getMessage())
            return obj.toString()
        }
    }


}
