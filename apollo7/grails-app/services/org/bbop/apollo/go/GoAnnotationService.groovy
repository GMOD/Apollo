package org.bbop.apollo.go

import grails.gorm.transactions.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.Gff3ConstantEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import java.text.SimpleDateFormat

@Transactional
class GoAnnotationService {

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
     date_created=2020-03-12 11:54:43.307"     * @param geneProductString
     * @return
     */
    List<GoAnnotation> convertGff3StringToGoAnnotations(String goAnnotationInputString) {
        log.debug "input string: [${goAnnotationInputString}]"
        List<GoAnnotation> goAnnotations = []
        def goAnnotationStrings = (goAnnotationInputString.trim().split("rank=") as List).findAll { it.trim().size() > 0 }
        log.debug "gene product strings ${goAnnotationStrings.size()}: [${goAnnotationStrings}]"
        log.debug "joined ${goAnnotationStrings.join("|||||")}"
        for (String goAnnotationString in goAnnotationStrings) {
            def attributes = goAnnotationString.trim().split(";")
            GoAnnotation goAnnotation = new GoAnnotation()
            for (String attribute in attributes) {
                if (attribute.contains("=")) {
                    def (key, value) = attribute.split("=")
                    switch (key) {
                        case Gff3ConstantEnum.ASPECT.value:
                            goAnnotation.aspect = value
                            break
                        case Gff3ConstantEnum.TERM.value:
                            goAnnotation.goRef = value
                            break
                        case Gff3ConstantEnum.DB_XREF.value:
                            goAnnotation.reference = value
                            break
                        case Gff3ConstantEnum.EVIDENCE.value:
                            goAnnotation.evidenceRef = value
                            break
                        case Gff3ConstantEnum.GENE_PRODUCT_RELATIONSHIP.value:
                            goAnnotation.geneProductRelationshipRef = value
                            break
                        case Gff3ConstantEnum.NEGATE.value:
                            goAnnotation.negate = value
                            break
                        case Gff3ConstantEnum.NOTE.value:
                            goAnnotation.notesArray = value
                            break
                        case Gff3ConstantEnum.BASED_ON.value:
                            goAnnotation.withOrFromArray = value
                            break
                        case Gff3ConstantEnum.LAST_UPDATED.value:
                            goAnnotation.lastUpdated = dateFormatter.parse(value)
                            break
                        case Gff3ConstantEnum.DATE_CREATED.value:
                            goAnnotation.dateCreated = dateFormatter.parse(value)
                            break
                    }
                }
            }
            goAnnotations.add(goAnnotation)
        }
        return goAnnotations
    }


    String convertGoAnnotationsToGff3String(Collection<GoAnnotation> goAnnotations) {
        String productString = ""
        int rank = 1
        for (GoAnnotation goAnnotation in goAnnotations) {
            if (productString.length() > 0) productString += ","
            productString += "${Gff3ConstantEnum.RANK.value}=${rank}"
            productString += ";${Gff3ConstantEnum.ASPECT.value}=${goAnnotation.aspect}"
            productString += ";${Gff3ConstantEnum.TERM.value}=${goAnnotation.goRef}"
            productString += ";${Gff3ConstantEnum.DB_XREF.value}=${goAnnotation.reference}"
            productString += ";${Gff3ConstantEnum.EVIDENCE.value}=${goAnnotation.evidenceRef}"
            productString += ";${Gff3ConstantEnum.GENE_PRODUCT_RELATIONSHIP.value}=${goAnnotation.geneProductRelationshipRef}"
            productString += ";${Gff3ConstantEnum.NEGATE.value}=${goAnnotation.negate}"
            productString += ";${Gff3ConstantEnum.NOTE.value}=${goAnnotation.notesArray}"
            productString += ";${Gff3ConstantEnum.BASED_ON.value}=${goAnnotation.withOrFromArray}"
            productString += ";${Gff3ConstantEnum.LAST_UPDATED.value}=${goAnnotation.lastUpdated}"
            productString += ";${Gff3ConstantEnum.DATE_CREATED.value}=${goAnnotation.dateCreated}"
            ++rank
        }
        return productString
    }

    JSONObject convertAnnotationToJson(GoAnnotation goAnnotation) {
        JSONObject goObject = new JSONObject()
        if (goAnnotation.getId()) {
            goObject.put("id", goAnnotation.getId())
        }
        goObject.put("feature", goAnnotation.feature.uniqueName)
        goObject.put("aspect", goAnnotation.aspect)
        goObject.put("goTerm", goAnnotation.goRef)
        goObject.put("goTermLabel", goAnnotation.goRefLabel)
        goObject.put("geneRelationship", goAnnotation.geneProductRelationshipRef)
        goObject.put("evidenceCode", goAnnotation.evidenceRef)
        goObject.put("evidenceCodeLabel", goAnnotation.evidenceRefLabel)
        goObject.put("negate", goAnnotation.negate)
        goObject.put("withOrFrom", goAnnotation.withOrFromArray)
        goObject.put("notes", goAnnotation.notesArray)
        goObject.put("reference", goAnnotation.reference)
        return goObject
    }

    JSONArray convertAnnotationsToJson(Collection<GoAnnotation> goAnnotations) {
        JSONArray annotations = new JSONArray()
//                {
//                    "gene" : "e35ea570-f700-41fb-b479-70aa812174ad" , "goTerm" : "GO:0014731PHENOTYPE" ,
//                    "geneRelationship" : "RO:0002326" , "evidenceCode" : "ECO:0000361" , "negate" : false , "withOrFrom"
//                    : ["asef:123123"] , "references" : ["ref:asdfasdf21"]
//                }
        for (GoAnnotation goAnnotation in goAnnotations) {
            annotations.add(convertAnnotationToJson(goAnnotation))
        }
        return annotations
    }

    JSONObject getAnnotations(Feature feature) {
        def goAnnotations = GoAnnotation.findAllByFeature(feature)
        JSONObject returnObject = new JSONObject()
        JSONArray annotations = convertAnnotationsToJson(goAnnotations)
        returnObject.put("annotations", annotations)
        return returnObject
    }

    def deleteAnnotationFromFeature(Feature thisFeature) {
        GoAnnotation.deleteAll(GoAnnotation.executeQuery("select ga from GoAnnotation ga join ga.feature f where f = :feature", [feature: thisFeature]))
    }

    def deleteAnnotations(JSONArray featuresArray) {
        def featureUniqueNames = featuresArray.uniquename as List<String>
        List<Feature> features = Feature.findAllByUniqueNameInList(featureUniqueNames)
        for (Feature thisFeature in features) {
            deleteAnnotationFromFeature(thisFeature)
        }
    }

    def removeGoAnnotationsFromFeature(Feature feature) {
        def goAnnotations = feature.goAnnotations
        for (def annotation in goAnnotations) {
            feature.removeFromGoAnnotations(annotation)
        }
    }
}
