package org.bbop.apollo.provenance

import grails.gorm.transactions.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.Gff3ConstantEnum
import org.bbop.apollo.Provenance
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import java.text.SimpleDateFormat


@Transactional
class ProvenanceService {

  final String DATE_FORMAT_STRING = "YYYY-MM-DD hh:mm:ss.s"
  final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_STRING)

  /**
   gene_product : "rank=1;
   term=geneproduct2;
   db_xref=genereference2:11111;
   evidence=ECO:0000318;
   alternate=true;
   note=[];
   based_on=['genewith2:11111'];
   last_updated=2020-03-12 11:55:20.712;
   date_created=2020-03-12 11:55:20.712,rank=2;
   term=geneproduc1;
   db_xref=genereference:1111;
   evidence=ECO:0000250;
   alternate=true;
   note=[];
   based_on=['genewith1:2222','genewith1:1111'];
   last_updated=2020-03-12 11:54:43.307;
   date_created=2020-03-12 11:54:43.307"   "
   * @param geneProductString
   * @return
   */
  List<Provenance> convertGff3StringToProvenances(String provenanceInputString) {
    log.debug "input string: [${provenanceInputString}]"
    List<Provenance> provenances = []
    def provenanceStrings = (provenanceInputString.trim().split("rank=") as List).findAll { it.trim().size() > 0 }
    log.debug "gene product strings ${provenanceStrings.size()}: [${provenanceStrings}]"
    log.debug "joined ${provenanceStrings.join("|||||")}"
    for (String provenanceString in provenanceStrings) {
      def attributes = provenanceString.trim().split(";")
      Provenance provenance = new Provenance()
      for (String attribute in attributes) {
        if (attribute.contains("=")) {
          def (key, value) = attribute.split("=")
          switch (key) {
            case Gff3ConstantEnum.FIELD.value:
              provenance.field = value
              break
            case Gff3ConstantEnum.DB_XREF.value:
              provenance.reference = value
              break
            case Gff3ConstantEnum.EVIDENCE.value:
              provenance.evidenceRef = value
              break
            case Gff3ConstantEnum.NOTE.value:
              provenance.notesArray = value
              break
            case Gff3ConstantEnum.BASED_ON.value:
              provenance.withOrFromArray = value
              break
            case Gff3ConstantEnum.LAST_UPDATED.value:
              provenance.lastUpdated = dateFormatter.parse(value)
              break
            case Gff3ConstantEnum.DATE_CREATED.value:
              provenance.dateCreated = dateFormatter.parse(value)
              break
          }
        }
      }
      provenances.add(provenance)
    }
    return provenances
  }


  String convertProvenancesToGff3String(Collection<Provenance> provenances) {
    String productString = ""
    int rank = 1
    for (Provenance provenance in provenances) {
      if (productString.length() > 0) productString += ","
      productString += "${Gff3ConstantEnum.RANK.value}=${rank}"
      productString += ";${Gff3ConstantEnum.FIELD.value}=${provenance.field}"
      productString += ";${Gff3ConstantEnum.DB_XREF.value}=${provenance.reference}"
      productString += ";${Gff3ConstantEnum.EVIDENCE.value}=${provenance.evidenceRef}"
      productString += ";${Gff3ConstantEnum.NOTE.value}=${provenance.notesArray}"
      productString += ";${Gff3ConstantEnum.BASED_ON.value}=${provenance.withOrFromArray}"
      productString += ";${Gff3ConstantEnum.LAST_UPDATED.value}=${provenance.lastUpdated}"
      productString += ";${Gff3ConstantEnum.DATE_CREATED.value}=${provenance.dateCreated}"
      ++rank
    }
    return productString
  }

  JSONObject convertAnnotationToJson(Provenance provenance) {
    JSONObject goObject = new JSONObject()
    if (provenance.getId()) {
      goObject.put("id", provenance.getId())
    }
    goObject.put("feature", provenance.feature.uniqueName)
    goObject.put("field", provenance.field)
    goObject.put("evidenceCode", provenance.evidenceRef)
    goObject.put("evidenceCodeLabel", provenance.evidenceRefLabel)
    goObject.put("withOrFrom", provenance.withOrFromArray)
    goObject.put("notes", provenance.notesArray)
    goObject.put("reference", provenance.reference)
    return goObject
  }

  JSONArray convertAnnotationsToJson(Collection<Provenance> provenances) {
    JSONArray annotations = new JSONArray()
//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
    for (Provenance provenance in provenances) {
      annotations.add(convertAnnotationToJson(provenance))
    }
    return annotations
  }

  JSONObject getAnnotations(Feature feature) {
    def provenances = Provenance.findAllByFeature(feature)
    JSONObject returnObject = new JSONObject()
    JSONArray annotations = convertAnnotationsToJson(provenances)
    returnObject.put("annotations", annotations)
    return returnObject
  }

  def deleteAnnotationFromFeature(Feature thisFeature) {
    Provenance.deleteAll(Provenance.executeQuery("select ga from Provenance  ga join ga.feature f where f = :feature", [feature: thisFeature]))
  }

  def deleteAnnotations(JSONArray featuresArray) {
    def featureUniqueNames = featuresArray.uniquename as List<String>
    List<Feature> features = Feature.findAllByUniqueNameInList(featureUniqueNames)
    for (Feature thisFeature in features) {
      deleteAnnotationFromFeature(thisFeature)
    }
  }

  def removeProvenancesFromFeature(Feature feature) {
      def provenances = feature.provenances
      for(def annotation in provenances){
        feature.removeFromProvenances(annotation)
      }
  }
}
