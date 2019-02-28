package org.bbop.apollo.track

import org.codehaus.groovy.grails.web.json.JSONObject

class TrackDefaults {

    static String getIndexedFastaConfig(String name){
        String inputString = "{" +
                "   'formatVersion' : 1," +
                "   'refSeqs' : 'seq/%filename%.fa.fai'," +
                "   'tracks' : [" +
                "      {" +
                "         'category' : 'Reference sequence'," +
                "         'faiUrlTemplate' : 'seq/%filename%.fa.fai'," +
                "         'key' : 'Reference sequence'," +
                "         'label' : 'DNA'," +
                "         'seqType' : 'dna'," +
                "         'storeClass' : 'JBrowse/Store/SeqFeature/IndexedFasta'," +
                "         'type' : 'SequenceTrack'," +
                "         'urlTemplate' : 'seq/%filename%.fa'," +
                "         'useAsRefSeqStore' : 1" +
                "      }" +
                "   ]" +
                "}"
        inputString = inputString.replaceAll('%filename%',name)
        return inputString
    }
}
