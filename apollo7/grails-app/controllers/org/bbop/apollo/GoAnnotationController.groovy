package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.User
import org.bbop.apollo.go.GoAnnotation
import org.bbop.apollo.go.GoAnnotationService
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED

@Transactional(readOnly = true)
class GoAnnotationController {


  PermissionService permissionService
  GoAnnotationService goAnnotationService
  FeatureEventService featureEventService
  FeatureService featureService
  def index() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    try {
      permissionService.hasPermissions(dataObject,PermissionEnum.READ)
    } catch (e) {
      def error = [error: e.message]
      render error as JSON
      return
    }
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject,GlobalPermissionEnum.USER,PermissionEnum.READ)){
      render status : UNAUTHORIZED
      return
    }
    Feature feature = Feature.findByUniqueName(dataObject.uniqueName as String)
    if (feature) {
      JSONObject annotations = goAnnotationService.getAnnotations(feature)
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
//        "reference":"refprefix:44444444"
  @Transactional
  def save() {
    JSONObject dataObject = permissionService.handleInput(request, params)
    try {
      permissionService.hasPermissions(dataObject,PermissionEnum.READ)
    } catch (e) {
      def error = [error: e.message]
      render error as JSON
      return
    }
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject,GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
      render status : UNAUTHORIZED
      return
    }
    User user = permissionService.getCurrentUser(dataObject)
    GoAnnotation goAnnotation = new GoAnnotation()
    Feature feature = Feature.findByUniqueName(dataObject.feature)

    JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

    goAnnotation.feature = feature
    goAnnotation.aspect = dataObject.aspect
    goAnnotation.goRef = dataObject.goTerm
    goAnnotation.geneProductRelationshipRef = dataObject.geneRelationship
    goAnnotation.evidenceRef = dataObject.evidenceCode
    goAnnotation.goRefLabel = dataObject.goTermLabel
    goAnnotation.evidenceRefLabel = dataObject.evidenceCodeLabel
    goAnnotation.negate = dataObject.negate ?: false
    goAnnotation.withOrFromArray = dataObject.withOrFrom
    goAnnotation.notesArray = dataObject.notes
    goAnnotation.reference = dataObject.reference
    goAnnotation.lastUpdated = new Date()
    goAnnotation.dateCreated = new Date()
    goAnnotation.addToOwners(user)
    feature.addToGoAnnotations(goAnnotation)
    goAnnotation.save(flush: true, failOnError: true)

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

    JSONObject annotations = goAnnotationService.getAnnotations(feature)
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
      return
    }
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject,GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
      render status : UNAUTHORIZED
      return
    }
    User user = permissionService.getCurrentUser(dataObject)
    Feature feature = Feature.findByUniqueName(dataObject.feature)

    JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)


    GoAnnotation goAnnotation = GoAnnotation.findById(dataObject.id)
    goAnnotation.aspect = dataObject.aspect
    goAnnotation.goRef = dataObject.goTerm
    goAnnotation.geneProductRelationshipRef = dataObject.geneRelationship
    goAnnotation.evidenceRef = dataObject.evidenceCode
    goAnnotation.goRefLabel = dataObject.goTermLabel
    goAnnotation.evidenceRefLabel = dataObject.evidenceCodeLabel
    goAnnotation.negate = dataObject.negate ?: false
    goAnnotation.withOrFromArray = dataObject.withOrFrom
    goAnnotation.notesArray = dataObject.notes
    goAnnotation.reference = dataObject.reference
    goAnnotation.lastUpdated = new Date()
    goAnnotation.addToOwners(user)
    goAnnotation.save(flush: true, failOnError: true, insert: false)

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

    JSONObject annotations = goAnnotationService.getAnnotations(feature)
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
      return
    }
    if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject,GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
      render status : UNAUTHORIZED
      return
    }
    User user = permissionService.getCurrentUser(dataObject)

    Feature feature = Feature.findByUniqueName(dataObject.feature)
    JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

    GoAnnotation goAnnotation = GoAnnotation.findById(dataObject.id)
    feature.removeFromGoAnnotations(goAnnotation)
    goAnnotation.delete(flush: true)

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

    JSONObject annotations = goAnnotationService.getAnnotations(feature)
    render annotations as JSON
  }
}
