package org.bbop.apollo.geneProduct

import grails.gorm.transactions.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.Gff3ConstantEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import java.text.SimpleDateFormat

@Transactional
class GeneProductService {

//    "2020-03-12 11:54:43.307"
  final String DATE_FORMAT_STRING = "YYYY-MM-DD hh:mm:ss.s"
  final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_STRING)

  /**
   * gene_product : "
   rank=1;
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
   date_created=2020-03-12 11:54:43.307
   "
   * @param geneProductString
   * @return
   */
  List<GeneProduct> convertGff3StringToGeneProducts(String geneProductInputString) {
    log.debug "input string: [${geneProductInputString}]"
    List<GeneProduct> geneProducts = []
    def geneProductStrings = (geneProductInputString.trim().split("rank=") as List).findAll { it.trim().size() > 0 }
    log.debug "gene product strings ${geneProductStrings.size()}: [${geneProductStrings}]"
    log.debug "joined ${geneProductStrings.join("|||||")}"
    for (String geneProductString in geneProductStrings) {
      def attributes = geneProductString.trim().split(";")
      GeneProduct geneProduct = new GeneProduct()
      for (String attribute in attributes) {
        if (attribute.contains("=")) {
          def (key, value) = attribute.split("=")
          switch (key) {
            case Gff3ConstantEnum.TERM.value:
              geneProduct.productName = value
              break
            case Gff3ConstantEnum.DB_XREF.value:
              geneProduct.reference = value
              break
            case Gff3ConstantEnum.EVIDENCE.value:
              geneProduct.evidenceRef = value
              break
            case Gff3ConstantEnum.ALTERNATE.value:
              geneProduct.alternate = value
              break
            case Gff3ConstantEnum.NOTE.value:
              geneProduct.notesArray = value
              break
            case Gff3ConstantEnum.BASED_ON.value:
              geneProduct.withOrFromArray = value
              break
            case Gff3ConstantEnum.LAST_UPDATED.value:
              geneProduct.lastUpdated = dateFormatter.parse(value)
              break
            case Gff3ConstantEnum.DATE_CREATED.value:
              geneProduct.dateCreated = dateFormatter.parse(value)
              break
          }
        }
      }
      geneProducts.add(geneProduct)
    }
    return geneProducts
  }


  String convertGeneProductsToGff3String(Collection<GeneProduct> geneProducts) {
    String productString = ""
    int rank = 1
    for (GeneProduct geneProduct in geneProducts) {
      if (productString.length() > 0) productString += ","
      productString += "${Gff3ConstantEnum.RANK.value}=${rank}"
      productString += ";${Gff3ConstantEnum.TERM.value}=${geneProduct.productName}"
      productString += ";${Gff3ConstantEnum.DB_XREF.value}=${geneProduct.reference}"
      productString += ";${Gff3ConstantEnum.EVIDENCE.value}=${geneProduct.evidenceRef}"
      productString += ";${Gff3ConstantEnum.ALTERNATE.value}=${geneProduct.alternate}"
      productString += ";${Gff3ConstantEnum.NOTE.value}=${geneProduct.notesArray}"
      productString += ";${Gff3ConstantEnum.BASED_ON.value}=${geneProduct.withOrFromArray}"
      productString += ";${Gff3ConstantEnum.LAST_UPDATED.value}=${geneProduct.lastUpdated}"
      productString += ";${Gff3ConstantEnum.DATE_CREATED.value}=${geneProduct.dateCreated}"
      ++rank
    }
    return productString
  }


  JSONObject convertAnnotationToJson(GeneProduct geneProduct) {
    JSONObject goObject = new JSONObject()
    if (geneProduct.getId()) {
      goObject.put("id", geneProduct.getId())
    }
    goObject.put("feature", geneProduct.feature.uniqueName)
    goObject.put("productName", geneProduct.productName)
    goObject.put("evidenceCode", geneProduct.evidenceRef)
    goObject.put("evidenceCodeLabel", geneProduct.evidenceRefLabel)
    goObject.put("alternate", geneProduct.alternate)
    goObject.put("withOrFrom", geneProduct.withOrFromArray)
    goObject.put("notes", geneProduct.notesArray)
    goObject.put("reference", geneProduct.reference)
    return goObject
  }

  JSONArray convertAnnotationsToJson(Collection<GeneProduct> geneProducts) {
    JSONArray annotations = new JSONArray()
//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
    for (GeneProduct geneProduct in geneProducts) {
      annotations.add(convertAnnotationToJson(geneProduct))
    }
    return annotations
  }

  JSONObject getAnnotations(Feature feature) {
    def geneProducts = GeneProduct.findAllByFeature(feature)
    JSONObject returnObject = new JSONObject()
    JSONArray annotations = convertAnnotationsToJson(geneProducts)
    returnObject.put("annotations", annotations)
    return returnObject
  }

  def deleteAnnotationFromFeature(Feature thisFeature) {
    GeneProduct.deleteAll(GeneProduct.executeQuery("select ga from GeneProduct  ga join ga.feature f where f = :feature", [feature: thisFeature]))
  }

  def deleteAnnotations(JSONArray featuresArray) {
    def featureUniqueNames = featuresArray.uniquename as List<String>
    List<Feature> features = Feature.findAllByUniqueNameInList(featureUniqueNames)
    for (Feature thisFeature in features) {
      deleteAnnotationFromFeature(thisFeature)
    }
  }

  def removeGeneProductsFromFeature(Feature feature) {
    def geneProducts = feature.geneProducts
    for (def annotation in geneProducts) {
      feature.removeFromGeneProducts(annotation)
    }
  }
}
