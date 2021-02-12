package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED

@RestApi(name = "Provenance Annotation", description = "Methods for managing provenance annotations")
@Transactional(readOnly = true)
class ProvenanceController {


  def permissionService
  def provenanceService
  def featureEventService
  def featureService

  @RestApiMethod(description = "Load Annotations for feature", path = "/provenance", verb = RestApiVerb.POST)
  @RestApiParams(params = [
    @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "uniqueName", type = "Feature uniqueName", paramType = RestApiParamType.QUERY, description = "Feature name to query on")
  ]
  )
  def index() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.READ)){
      render status : UNAUTHORIZED
      return
    }
    Feature feature = Feature.findByUniqueName(dataObject.uniqueName as String)
    if (feature) {
      JSONObject annotations = provenanceService.getAnnotations(feature)
      // TODO: register with marshaller
      render annotations as JSON
    } else {
      render status: NOT_FOUND
    }
  }

//        {"gene":"e35ea570-f700-41fb-b479-70aa812174ad",
//        "goTerm":"GO:0060841",
//        "geneRelationship":"RO:0002616",
//        "evidenceCode":"ECO:0000335",
//        "negate":false,
//        "withOrFrom":["withprefix:12312321"],
//        "references":["refprefix:44444444"]}
  @RestApiMethod(description = "Save New Go Annotations for feature", path = "/provenance/save", verb = RestApiVerb.POST)
  @RestApiParams(params = [
    @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
          , @RestApiParam(name = "feature", type = "string", paramType = RestApiParamType.QUERY, description = "Feature uniqueName to query on")
    , @RestApiParam(name = "field", type = "string", paramType = RestApiParamType.QUERY, description = "Field type to annotate ")
    , @RestApiParam(name = "evidenceCode", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) CURIE")
    , @RestApiParam(name = "evidenceCodeLabel", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) Label")
    , @RestApiParam(name = "withOrFrom", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of with or from CURIE strings, e.g., {[\"UniProtKB:12312]]\"]}")
      , @RestApiParam(name = "notes", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of notes  {[\"A simple note\"]}")
          , @RestApiParam(name = "references", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of reference CURIE strings, e.g., {[\"PMID:12312\"]}")
  ]
  )
  @Transactional
  def save() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
      render status : UNAUTHORIZED
      return
    }
    User user = permissionService.getCurrentUser(dataObject)
    Provenance provenance = new Provenance()
    Feature feature = Feature.findByUniqueName(dataObject.feature)

    JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

    provenance.feature = feature
    provenance.field = dataObject.field
    provenance.evidenceRef = dataObject.evidenceCode
    provenance.evidenceRefLabel = dataObject.evidenceCodeLabel
    provenance.withOrFromArray = dataObject.withOrFrom
    provenance.notesArray = dataObject.notes
    provenance.reference = dataObject.reference
    provenance.lastUpdated = new Date()
    provenance.dateCreated = new Date()
    provenance.addToOwners(user)
    feature.addToProvenances(provenance)
    provenance.save(flush: true, failOnError: true)

    JSONArray oldFeaturesJsonArray = new JSONArray()
    oldFeaturesJsonArray.add(originalFeatureJsonObject)
    JSONArray newFeaturesJsonArray = new JSONArray()
    JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
    newFeaturesJsonArray.add(currentFeatureJsonObject)

    featureEventService.addNewFeatureEvent(FeatureOperation.ADD_GO_ANNOTATION,
      feature.name,
      feature.uniqueName,
      dataObject,
      oldFeaturesJsonArray,
      newFeaturesJsonArray,
      user)

    JSONObject annotations = provenanceService.getAnnotations(feature)
    render annotations as JSON
  }

  @RestApiMethod(description = "Update existing annotations for feature", path = "/provenance/update", verb = RestApiVerb.POST)
  @RestApiParams(params = [
    @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "id", type = "string", paramType = RestApiParamType.QUERY, description = "GO Annotation ID to update (required)")
    , @RestApiParam(name = "feature", type = "string", paramType = RestApiParamType.QUERY, description = "uniqueName of feature to query on")
    , @RestApiParam(name = "field", type = "string", paramType = RestApiParamType.QUERY, description = "field type annotated")
    , @RestApiParam(name = "evidenceCode", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) CURIE")
    , @RestApiParam(name = "evidenceCodeLabel", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) Label")
    , @RestApiParam(name = "withOrFrom", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of with or from CURIE strings, e.g., {[\"UniProtKB:12312]]\"]}")
    , @RestApiParam(name = "notes", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of notes  {[\"A simple note\"]}")
    , @RestApiParam(name = "references", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of reference CURIE strings, e.g., {[\"PMID:12312\"]}")
  ]
  )
  @Transactional
  def update() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
      render status : UNAUTHORIZED
      return
    }
    User user = permissionService.getCurrentUser(dataObject)
    Feature feature = Feature.findByUniqueName(dataObject.feature)

    JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)


    Provenance provenance = Provenance.findById(dataObject.id)
    provenance.feature = feature
    provenance.field = dataObject.field
    provenance.evidenceRef = dataObject.evidenceCode
    provenance.evidenceRefLabel = dataObject.evidenceCodeLabel
    provenance.withOrFromArray = dataObject.withOrFrom
    provenance.notesArray = dataObject.notes
    provenance.reference = dataObject.reference
    provenance.lastUpdated = new Date()
    provenance.dateCreated = new Date()
    provenance.addToOwners(user)
    provenance.save(flush: true, failOnError: true, insert: false)

    JSONArray oldFeaturesJsonArray = new JSONArray()
    oldFeaturesJsonArray.add(originalFeatureJsonObject)
    JSONArray newFeaturesJsonArray = new JSONArray()
    JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
    newFeaturesJsonArray.add(currentFeatureJsonObject)

    featureEventService.addNewFeatureEvent(FeatureOperation.UPDATE_GO_ANNOTATION,
      feature.name,
      feature.uniqueName,
      dataObject,
      oldFeaturesJsonArray,
      newFeaturesJsonArray,
      user)

    JSONObject annotations = provenanceService.getAnnotations(feature)
    render annotations as JSON
  }

  @RestApiMethod(description = "Delete existing annotations for feature", path = "/provenance/delete", verb = RestApiVerb.POST)
  @RestApiParams(params = [
    @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
    , @RestApiParam(name = "id", type = "string", paramType = RestApiParamType.QUERY, description = "GO Annotation ID to delete (required)")
    , @RestApiParam(name = "uniqueName", type = "string", paramType = RestApiParamType.QUERY, description = "Feature uniqueName to remove feature from")
  ]
  )
  @Transactional
  def delete() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
      render status : UNAUTHORIZED
      return
    }
    User user = permissionService.getCurrentUser(dataObject)

    Feature feature = Feature.findByUniqueName(dataObject.feature)
    JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

    Provenance provenance = Provenance.findById(dataObject.id)
    feature.removeFromProvenances(provenance)
    provenance.delete(flush: true)

    JSONArray oldFeaturesJsonArray = new JSONArray()
    oldFeaturesJsonArray.add(originalFeatureJsonObject)
    JSONArray newFeaturesJsonArray = new JSONArray()
    JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
    newFeaturesJsonArray.add(currentFeatureJsonObject)

    featureEventService.addNewFeatureEvent(FeatureOperation.REMOVE_GO_ANNOTATION,
      feature.name,
      feature.uniqueName,
      dataObject,
      oldFeaturesJsonArray,
      newFeaturesJsonArray,
      user)

    JSONObject annotations = provenanceService.getAnnotations(feature)
    render annotations as JSON
  }
}
