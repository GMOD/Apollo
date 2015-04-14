package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.web.util.JSONUtil
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.tools.seq.search.blat.BlatCommandLine


@Transactional
class SequenceSearchService {

    def configWrapperService
    def serviceMethod() {

    }

    def searchSequence(JSONObject input, String database) {
        String ret=input.get('search').get('key')
        JSONObject searchUtils=configWrapperService.getSequenceSearchTools().get(ret)
        searchUtils.put("database",database)
        def searcher=this.class.classLoader.loadClass( searchUtils.get('search_class'), true, false )?.newInstance()
        searcher.parseConfiguration(searchUtils)
        Collection<Match> results=searcher.search('test', input.get('search').get('residues'), input.get('search').get('database_id'))
        JSONArray a=new JSONArray()
        for(i in results) {
            a.put(i.toString())
        }


        return a.toString()
    }


}
