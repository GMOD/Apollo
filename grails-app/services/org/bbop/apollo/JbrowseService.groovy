package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional

@Transactional
class JbrowseService {

    @NotTransactional
    Boolean hasOverlappingDirectory(String path1, String path2) {
        String[] paths1 = path1.split("/")
        String[] paths2 = path2.split("/")

        log.debug "Comparing ${paths1[paths1.length-1]} to ${paths2[0]}"
        if(paths1[paths1.length-1]==paths2[0]){
            return true
        }
        return false
    }

    @NotTransactional
    String fixOverlappingPath(String path1, String path2) {
        String[] paths1 = path1.split("/")
        String[] paths2 = path2.split("/")
        List<String> finalPaths = new ArrayList<>()
        if(paths1[paths1.length-1]==paths2[0]){
            // add all but the last one
            for(p in paths1){
                if(p != paths2[0]){
                    finalPaths.add(p)
                }
            }
        }
        else{
            finalPaths.addAll(paths2)
        }
        finalPaths.addAll(paths2)
        return finalPaths.join("/")
    }

}
