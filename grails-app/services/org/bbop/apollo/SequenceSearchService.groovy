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

    def searchSequence(JSONObject input) {
        String ret=input.get('search').get('key')
        Map<String,String> searchUtils=configWrapperService.getSequenceSearchTools().get(ret)
        println "test1 "+searchUtils.get('name')
        def searcher=this.class.classLoader.loadClass( searchUtils.get('search_class'), true, false )?.newInstance()
        println "test3 "+searcher.getClass().getName()
        Collection<Match> results=searcher.search("test", input.get('search').get('residues'), "test")
        searcher.setBlatBin(searchUtils.get('exe'))
        searcher.setTmpDir(searchUtils.get('tmp_dir'))
        searcher.database(searchUtils.get('tmp_dir'))
        JSONArray a=new JSONArray()
        for(i in results) {
            a.put(i.toString())
        }


        return r.toString()
    }


}
