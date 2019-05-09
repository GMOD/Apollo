package org.bbop.apollo.go

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.Feature
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

@Transactional(readOnly = true)
class GoAnnotationController {

//    static responseFormats = ['json', 'xml']
//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService
    def preferenceService
    def goAnnotationService

//    def index(Integer max) {
//        params.max = Math.min(max ?: 10, 100)
//        respond GoAnnotation.list(params), [status: OK]
//    }

    @RestApiMethod(description = "Load Go Annotations for gene", path = "/go", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "uniqueName", type = "Gene uniqueName", paramType = RestApiParamType.QUERY, description = "Gene name to query on")
    ]
    )
    def index() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        Feature feature = Feature.findByUniqueName(dataObject.uniqueName as String)
        if (feature) {
            JSONObject annotations =  goAnnotationService.getAnnotations(feature)
            // TODO: register with marshaller
            render annotations as JSON
        } else {
            render status: NOT_FOUND
        }
    }

    @RestApiMethod(description = "Save New Go Annotations for gene", path = "/go/save", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "gene", type = "string", paramType = RestApiParamType.QUERY, description = "Gene name to query on")
            , @RestApiParam(name = "goTerm", type = "string", paramType = RestApiParamType.QUERY, description = "GO CURIE")
            , @RestApiParam(name = "geneRelationship", type = "string", paramType = RestApiParamType.QUERY, description = "Gene relationship (RO) CURIE")
            , @RestApiParam(name = "evidenceCode", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) CURIE")
            , @RestApiParam(name = "negate", type = "boolean", paramType = RestApiParamType.QUERY, description = "Negate evidence (default false)")
            , @RestApiParam(name = "withOrFrom", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of with or from CURIE strings, e.g., {[\"UniProtKB:12312]]\"]}")
            , @RestApiParam(name = "references", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of reference CURIE strings, e.g., {[\"PMID:12312]]\"]}")
    ]
    )
    @Transactional
    def save() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        GoAnnotation goAnnotation = new GoAnnotation()
        Feature feature = Feature.findByUniqueName(dataObject.gene)
//        {"gene":"e35ea570-f700-41fb-b479-70aa812174ad",
        goAnnotation.feature = feature
//        "goTerm":"GO:0060841",
        goAnnotation.goRef = dataObject.goTerm
//        "geneRelationship":"RO:0002616",
        goAnnotation.geneProductRelationshipRef = dataObject.geneRelationship
//        "evidenceCode":"ECO:0000335",
        goAnnotation.evidenceRef = dataObject.evidenceCode
//        "negate":false,
        goAnnotation.negate = dataObject.negate ?: false
//        "withOrFrom":["withprefix:12312321"],
        goAnnotation.withOrFromArray = dataObject.withOrFrom
//        "references":["refprefix:44444444"]}
        goAnnotation.referenceArray = dataObject.references

        goAnnotation.save(flush: true, failOnError: true)

        JSONObject annotations =  goAnnotationService.getAnnotations(feature)
        render annotations as JSON
    }

    @Transactional
    def update() {
    }

    @Transactional
    def delete() {
        JSONObject dataObject = permissionService.handleInput(request, params)

        GoAnnotation goAnnotation = GoAnnotation.findById(dataObject.id)
        goAnnotation.delete(flush: true)
        Feature feature = Feature.findByUniqueName(dataObject.gene)


        JSONObject annotations =  goAnnotationService.getAnnotations(feature)
        render annotations as JSON
    }
}
