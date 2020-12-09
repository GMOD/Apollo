package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import groovy.json.JsonBuilder

@Transactional
class AnnotationEditorService {


  @NotTransactional
  String cleanJSONString(String inputString) {
    String outputString = new String(inputString)
    // remove leading string
    outputString = outputString.indexOf("\"") == 0 ? outputString.substring(1) : outputString
    outputString = outputString.lastIndexOf("\"") == outputString.length() - 1 ? outputString.substring(0, outputString.length() - 1) : outputString
    outputString = outputString.replaceAll("\\\\\"", "\"")
    outputString = outputString.replaceAll("\\\\\"", "'")
    return outputString
  }

  JsonBuilder recentAnnotations(Integer days, String statusFilter = null) {
    Date today = new Date()
    Date fromDate = today - days
    List updatedGenes
    if (statusFilter == null) {
      updatedGenes = Gene.findAllByLastUpdatedGreaterThan(fromDate)
    }
    else
    if (statusFilter.toLowerCase() == "none") {
      updatedGenes = Gene.findAllByLastUpdatedGreaterThanAndStatusIsNull(fromDate)
    } else {
      List<String> statusFilterStringList = statusFilter.split("\\|")
      List<Status> statusList = []
      for(def statusString in statusFilterStringList){
        Status status = Status.findByValue(statusString)
        if(status) {
          statusList.add(status)
        }
        else{
          throw new RuntimeException("Status ${statusString} not found.")
        }
      }
      updatedGenes = Gene.findAllByLastUpdatedGreaterThanAndStatusInList(fromDate, statusList)
    }


    Map geneToSpecies = [:]
    updatedGenes.each { gene ->
      geneToSpecies[gene.uniqueName] = gene.getFeatureLocation().sequence.organism.commonName
    }

    JsonBuilder toJson = new JsonBuilder()
    toJson(geneToSpecies)
    return toJson
  }
}
