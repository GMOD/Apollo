package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.GeneProductName
import org.bbop.apollo.User
import org.bbop.apollo.geneProduct.GeneProduct
import org.bbop.apollo.geneProduct.GeneProductService
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED

@Transactional(readOnly = true)
class GeneProductController {


    PermissionService permissionService
    GeneProductService geneProductService
    FeatureEventService featureEventService
    FeatureService featureService
    def search() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            log.debug "Showing gene product names ${nameJson}"
            Organism organism = nameJson.organism ? (Organism.findByCommonName(nameJson.organism) ?: Organism.findById(nameJson.organism)) : null

            List<GeneProductName> geneProductNameList = new ArrayList<>()
            List<String> geneProductNamesFiltered = new ArrayList<>()
            geneProductNameList.addAll(GeneProductName.executeQuery("select cc from GeneProductName cc where cc.name like :query", [query: nameJson.query + "%"]))
            // if there are organism filters for these canned comments for this organism, then apply them
            // TODO: somehow it is breaking this for organisms
            List<GeneProductNameOrganismFilter> geneProductNameOrganismFilters = GeneProductNameOrganismFilter.findAllByGeneProductNameInList(geneProductNameList)
            if (geneProductNameOrganismFilters) {
                GeneProductNameOrganismFilter.findAllByOrganismAndGeneProductNameInList(organism, geneProductNameList).each {
                    geneProductNamesFiltered.add(it.geneProductName.name)
                    geneProductNameList.remove(it.geneProductName)
                }
            }
            // otherwise ignore them
            else {
                geneProductNameList.each {
                    geneProductNamesFiltered.add(it.name)
                }
            }
            render geneProductNamesFiltered as JSON
        } catch (Exception e) {
            def error = [error: 'problem finding gene product names for : ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @Transactional
    def index() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.READ)){
            render status : UNAUTHORIZED
            return
        }
        Feature feature = Feature.findByUniqueName(dataObject.uniqueName as String)
        if (feature) {
            JSONObject annotations = geneProductService.getAnnotations(feature)
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
        if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
            render status : UNAUTHORIZED
            return
        }
        User user = permissionService.getCurrentUser(dataObject)
        GeneProduct geneProduct = new GeneProduct()
        Feature feature = Feature.findByUniqueName(dataObject.feature)

        JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

        geneProduct.feature = feature
        geneProduct.productName = dataObject.productName
        geneProduct.evidenceRef = dataObject.evidenceCode
        geneProduct.evidenceRefLabel = dataObject.evidenceCodeLabel
        geneProduct.alternate = dataObject.alternate ?: false
        geneProduct.withOrFromArray = dataObject.withOrFrom
        geneProduct.notesArray = dataObject.notes
        geneProduct.reference = dataObject.reference
        geneProduct.lastUpdated = new Date()
        geneProduct.dateCreated = new Date()
        geneProduct.addToOwners(user)
        feature.addToGeneProducts(geneProduct)
        geneProduct.save(flush: true, failOnError: true)
        GeneProductName.findOrSaveByName(geneProduct.productName)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        JSONArray newFeaturesJsonArray = new JSONArray()
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
        newFeaturesJsonArray.add(currentFeatureJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.ADD_GENE_PRODUCT,
            feature.name,
            feature.uniqueName,
            dataObject,
            oldFeaturesJsonArray,
            newFeaturesJsonArray,
            user)

        JSONObject annotations = geneProductService.getAnnotations(feature)
        render annotations as JSON
    }

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


        GeneProduct geneProduct = GeneProduct.findById(dataObject.id)
        geneProduct.feature = feature
        geneProduct.productName = dataObject.productName
        geneProduct.evidenceRef = dataObject.evidenceCode
        geneProduct.evidenceRefLabel = dataObject.evidenceCodeLabel
        geneProduct.alternate = dataObject.alternate ?: false
        geneProduct.withOrFromArray = dataObject.withOrFrom
        geneProduct.notesArray = dataObject.notes
        geneProduct.reference = dataObject.reference
        geneProduct.lastUpdated = new Date()
        geneProduct.dateCreated = new Date()
        geneProduct.addToOwners(user)
        GeneProductName.findOrSaveByName(geneProduct.productName)
        geneProduct.save(flush: true, failOnError: true, insert: false)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        JSONArray newFeaturesJsonArray = new JSONArray()
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
        newFeaturesJsonArray.add(currentFeatureJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.UPDATE_GENE_PRODUCT,
            feature.name,
            feature.uniqueName,
            dataObject,
            oldFeaturesJsonArray,
            newFeaturesJsonArray,
            user)

        JSONObject annotations = geneProductService.getAnnotations(feature)
        render annotations as JSON
    }

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

        GeneProduct geneProduct = GeneProduct.findById(dataObject.id)
        feature.removeFromGeneProducts(geneProduct)
        geneProduct.delete(flush: true)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        JSONArray newFeaturesJsonArray = new JSONArray()
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
        newFeaturesJsonArray.add(currentFeatureJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.REMOVE_GENE_PRODUCT,
            feature.name,
            feature.uniqueName,
            dataObject,
            oldFeaturesJsonArray,
            newFeaturesJsonArray,
            user)

        JSONObject annotations = geneProductService.getAnnotations(feature)
        render annotations as JSON
    }

}
