package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.*

@RestApi(name = "Suggested Names Services", description = "Methods for managing suggested names")
@Transactional(readOnly = true)
class SuggestedNameController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService

    def beforeInterceptor = {
        if (!permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)) {
            forward action: "notAuthorized", controller: "annotator"
            return
        }
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def suggestedNames = SuggestedName.list(params)
        def organismFilterMap = [:]
        SuggestedNameOrganismFilter.findAllBySuggestedNameInList(suggestedNames).each() {
            List filterList = organismFilterMap.containsKey(it.suggestedName) ? organismFilterMap.get(it.suggestedName) : []
            filterList.add(it)
            organismFilterMap[it.suggestedName] = filterList
        }
        respond suggestedNames, model: [suggestedNameInstanceCount: SuggestedName.count(), organismFilters: organismFilterMap]
    }

    def show(SuggestedName suggestedNameInstance) {
        respond suggestedNameInstance, model: [organismFilters: SuggestedNameOrganismFilter.findAllBySuggestedName(suggestedNameInstance)]
    }

    def create() {
        respond new SuggestedName(params)
    }

    @Transactional
    def save(SuggestedName suggestedNameInstance) {
        if (suggestedNameInstance == null) {
            notFound()
            return
        }

        if (suggestedNameInstance.hasErrors()) {
            respond suggestedNameInstance.errors, view: 'create'
            return
        }


        suggestedNameInstance.save()

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new SuggestedNameOrganismFilter(
                    organism: organism,
                    suggestedName: suggestedNameInstance
            ).save()
        }

        suggestedNameInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'suggestedName.label', default: 'SuggestedName'), suggestedNameInstance.id])
                redirect suggestedNameInstance
            }
            '*' { respond suggestedNameInstance, [status: CREATED] }
        }
    }

    def edit(SuggestedName suggestedNameInstance) {
        respond suggestedNameInstance, model: [organismFilters: SuggestedNameOrganismFilter.findAllBySuggestedName(suggestedNameInstance)]
    }

    @Transactional
    def update(SuggestedName suggestedNameInstance) {
        if (suggestedNameInstance == null) {
            notFound()
            return
        }

        if (suggestedNameInstance.hasErrors()) {
            respond suggestedNameInstance.errors, view: 'edit'
            return
        }

        suggestedNameInstance.save()

        SuggestedNameOrganismFilter.deleteAll(SuggestedNameOrganismFilter.findAllBySuggestedName(suggestedNameInstance))

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new SuggestedNameOrganismFilter(
                    organism: organism,
                    suggestedName: suggestedNameInstance
            ).save()
        }

        suggestedNameInstance.save(flush: true)

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'SuggestedName.label', default: 'SuggestedName'), suggestedNameInstance.id])
                redirect suggestedNameInstance
            }
            '*' { respond suggestedNameInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(SuggestedName suggestedNameInstance) {

        if (suggestedNameInstance == null) {
            notFound()
            return
        }

        suggestedNameInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'SuggestedName.label', default: 'SuggestedName'), suggestedNameInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'suggestedName.label', default: 'SuggestedName'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }

    @RestApiMethod(description = "Create suggested name", path = "/suggestedName/createName", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Suggested name to add")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "Optional additional information")
    ]
    )
    @Transactional
    def createName() {
        JSONObject nameJson = permissionService.handleInput(request, params)
        try {
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(nameJson))) {
                if (!nameJson.name) {
                    throw new Exception('empty fields detected')
                }

                if (!nameJson.metadata) {
                    nameJson.metadata = ""
                }

                log.debug "Adding suggested name ${nameJson.name}"
                SuggestedName name = new SuggestedName(
                        name: nameJson.name,
                        metadata: nameJson.metadata
                ).save(flush: true)

                render name as JSON
            } else {
                def error = [error: 'not authorized to add SuggestedName']
                render error as JSON
                log.error(error.error)
            }
        } catch (e) {
            def error = [error: 'problem saving SuggestedName: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }


    @RestApiMethod(description = "Update suggested name", path = "/suggestedName/updateName", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Suggested name ID to update (or specify the old_name)")
            , @RestApiParam(name = "old_name", type = "string", paramType = RestApiParamType.QUERY, description = "Suggested name to update")
            , @RestApiParam(name = "new_name", type = "string", paramType = RestApiParamType.QUERY, description = "Suggested name to change to (the only editable option)")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "Optional additional information")
    ]
    )
    @Transactional
    def updateName() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            log.debug "Updating suggested name ${nameJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(nameJson))) {

                log.debug "Suggested name ID: ${nameJson.id}"
                SuggestedName name = SuggestedName.findById(nameJson.id) ?: SuggestedName.findByName(nameJson.old_name)

                if (!name) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to update the suggested name")
                    render jsonObject as JSON
                    return
                }

                name.name = nameJson.new_name

                if (nameJson.metadata) {
                    name.metadata = nameJson.metadata
                }

                name.save(flush: true)

                log.info "Success updating suggested name: ${name.id}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to edit suggested name']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem editing suggested name: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Remove a suggested name", path = "/suggestedName/deleteName", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Suggested name ID to remove (or specify the name)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Suggested name to delete")
    ])
    @Transactional
    def deleteName() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            log.debug "Deleting suggested name ${nameJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(nameJson))) {

                SuggestedName name = SuggestedName.findById(nameJson.id) ?: SuggestedName.findByName(nameJson.name)

                if (!name) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the suggested name")
                    render jsonObject as JSON
                    return
                }

                name.delete()

                log.info "Success deleting suggested name: ${nameJson}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to delete suggested name']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem deleting suggested name: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Returns a JSON array of all suggested names, or optionally, gets information about a specific suggested name", path = "/suggestedName/search", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "featureType", type = "string", paramType = RestApiParamType.QUERY, description = "Feature type")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Organism name")
            , @RestApiParam(name = "query", type = "string", paramType = RestApiParamType.QUERY, description = "Query value")
    ])
    @Transactional
    def search() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            log.debug "Showing suggested name ${nameJson}"
            String featureTypeString = nameJson.featureType
            println "feature type ${featureTypeString}"
            Organism organism = Organism.findByCommonName(nameJson.organism)
            println "organism ${organism}"
//            def names = SuggestedName.findAllByNameIlike(nameJson.query + "%")
//            // if name has a feature type it must match
//            def filteredNames = names.findAll{ name ->
//                boolean match = true
//                if(name.featureTypes){
//                    match = match && name.featureTypes.contains(featureType)
//                }
//                if(name.org){
//                    match = match && name.featureTypes.contains(featureType)
//                }
//            }
            List<SuggestedName> suggestedNameList = new ArrayList<>()
            List<SuggestedName> suggestedNamesFiltered= new ArrayList<>()
            if (featureTypeString) {
                FeatureType featureType = FeatureType.findByName(featureTypeString)
                suggestedNameList.addAll(SuggestedName.executeQuery("select cc from SuggestedName cc join cc.featureTypes ft where ft in (:featureType) and cc.name like :query", [featureType: [featureType],query:nameJson.query + "%"]))
            }
            suggestedNameList.addAll(SuggestedName.executeQuery("select cc from SuggestedName cc where cc.featureTypes is empty and cc.name like :query",[query:nameJson.query + "%"]))

            // if there are organism filters for these canned comments for this organism, then apply them
            // TODO: somehow it is breaking this for organisms
            List<SuggestedNameOrganismFilter> suggestedNameOrganismFilters = SuggestedNameOrganismFilter.findAllBySuggestedNameInList(suggestedNameList)
            if (suggestedNameOrganismFilters) {
                SuggestedNameOrganismFilter.findAllByOrganismAndSuggestedNameInList(organism, suggestedNameList).each {
                    suggestedNamesFiltered.add(it.suggestedName)
                    suggestedNameList.remove(it.suggestedName)
                }
                suggestedNameList.each {
                    suggestedNamesFiltered.add(it)
                }
            }
            // otherwise ignore them
            else {
                suggestedNameList.each {
                    suggestedNamesFiltered.add(it)
                }
            }

            render suggestedNamesFiltered as JSON
        }
        catch (Exception e) {
            def error = [error: 'problem showing suggested names: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Returns a JSON array of all suggested names, or optionally, gets information about a specific suggested name", path = "/suggestedName/showName", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Name ID to show (or specify a name)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Name to show")
    ])
    @Transactional
    def showName() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            log.debug "Showing suggested name ${nameJson}"
            if (!permissionService.hasGlobalPermissions(nameJson, GlobalPermissionEnum.ADMIN)) {
                render status: UNAUTHORIZED
                return
            }

            if (nameJson.id || nameJson.name) {
                SuggestedName name = SuggestedName.findById(nameJson.id) ?: SuggestedName.findByName(nameJson.name)

                if (!name) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the suggested names")
                    render jsonObject as JSON
                    return
                }

                log.info "Success showing name: ${nameJson}"
                render name as JSON
            } else {
                def names = SuggestedName.all

                log.info "Success showing all suggested names"
                render names as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem showing suggested names: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "A comma-delimited list of names", path = "/suggestedName/addNames", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
//            , @RestApiParam(name = "names", type = "string", paramType = RestApiParamType.QUERY, description = "A comma-delimited list of names to add, with organisms, and types {names:[ {name:'name1':organisms:['bee','cow'],types:['gene','ncRNA']}}")
            , @RestApiParam(name = "names", type = "string", paramType = RestApiParamType.QUERY, description = "A comma-delimited list of names to add")
    ])
    @Transactional
    def addNames() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            println "Adding suggested names ${nameJson}"
            if (!permissionService.hasGlobalPermissions(nameJson, GlobalPermissionEnum.ADMIN)) {
                println "DOES NOT have global permissions"
                render status: UNAUTHORIZED
                return
            }

            if (nameJson.names) {
                for (name in nameJson.names) {
                    SuggestedName.findOrSaveByName(name)
                }
                render  nameJson.names as JSON
            } else {
                def error = [error: 'names not found']
                println(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem adding suggested names: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }
}
