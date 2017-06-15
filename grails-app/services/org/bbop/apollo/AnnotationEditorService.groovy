package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional

@Transactional
class AnnotationEditorService {


    @NotTransactional
    String cleanJSONString(String inputString){
        String outputString = new String(inputString)
        // remove leading string
        outputString = outputString.indexOf("\"")==0 ? outputString.substring(1) : outputString
        outputString = outputString.lastIndexOf("\"")==outputString.length()-1 ? outputString.substring(0,outputString.length()-1) : outputString
//        outputString = outputString.replaceAll("/\\\\/","")
        outputString = outputString.replaceAll("\\\\\"","\"")
        return outputString
    }
}
