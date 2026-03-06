package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED

@Transactional(readOnly = true)
class ProvenanceController {


  def permissionService
  def provenanceService
  def featureEventService
  def featureService

  def index() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    try {
      permissionService.hasPermissions(dataObject,PermissionEnum.READ)
    } catch (e) {
      def error = [error: e.message]
      render error as JSON
    }
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
  @Transactional
  def save() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    try {
      permissionService.hasPermissions(dataObject,PermissionEnum.READ)
    } catch (e) {
      def error = [error: e.message]
      render error as JSON
    }
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

    featureEventService.addNewFeatureEvent(FeatureOperation.ADD_PROVENANCE,
      feature.name,
      feature.uniqueName,
      dataObject,
      oldFeaturesJsonArray,
      newFeaturesJsonArray,
      user)

    JSONObject annotations = provenanceService.getAnnotations(feature)
    render annotations as JSON
  }

  @Transactional
  def update() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    try {
      permissionService.hasPermissions(dataObject,PermissionEnum.READ)
    } catch (e) {
      def error = [error: e.message]
      render error as JSON
    }
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

    featureEventService.addNewFeatureEvent(FeatureOperation.UPDATE_PROVENANCE,
      feature.name,
      feature.uniqueName,
      dataObject,
      oldFeaturesJsonArray,
      newFeaturesJsonArray,
      user)

    JSONObject annotations = provenanceService.getAnnotations(feature)
    render annotations as JSON
  }

  @Transactional
  def delete() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    try {
      permissionService.hasPermissions(dataObject,PermissionEnum.READ)
    } catch (e) {
      def error = [error: e.message]
      render error as JSON
    }
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

    featureEventService.addNewFeatureEvent(FeatureOperation.REMOVE_PROVENANCE,
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
