package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class GeneProductNameController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService

    def index(Integer max) {
//        params.max = Math.min(max ?: 10, 100)
//        respond GeneProductName.list(params), model:[geneProductNameInstanceCount: GeneProductName.count()]
        params.max = Math.min(max ?: 10, 100)
        def geneProductNames = GeneProductName.list(params)
        def organismFilterMap = [:]
        GeneProductNameOrganismFilter.findAllByGeneProductNameInList(geneProductNames).each() {
            List filterList = organismFilterMap.containsKey(it.geneProductName) ? organismFilterMap.get(it.geneProductName) : []
            filterList.add(it)
            organismFilterMap[it.geneProductName] = filterList
        }
        respond geneProductNames, model: [geneProductNameInstanceCount: GeneProductName.count(), organismFilters: organismFilterMap]
    }

    def show(GeneProductName geneProductNameInstance) {
        respond geneProductNameInstance, model: [organismFilters: GeneProductNameOrganismFilter.findAllByGeneProductName(geneProductNameInstance)]
    }

    def create() {
        respond new GeneProductName(params)
    }

    @Transactional
    def save(GeneProductName geneProductNameInstance) {
        if (geneProductNameInstance == null) {
            notFound()
            return
        }

        if (geneProductNameInstance.hasErrors()) {
            respond geneProductNameInstance.errors, view: 'create'
            return
        }

        geneProductNameInstance.save()

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new GeneProductNameOrganismFilter(
                organism: organism,
                geneProductName: geneProductNameInstance
            ).save()
        }

        geneProductNameInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'geneProductName.label', default: 'GeneProductName'), geneProductNameInstance.id])
                redirect geneProductNameInstance
            }
            '*' { respond geneProductNameInstance, [status: CREATED] }
        }
    }

    def edit(GeneProductName geneProductNameInstance) {
        respond geneProductNameInstance, model: [organismFilters: GeneProductNameOrganismFilter.findAllByGeneProductName(geneProductNameInstance)]
    }

    @Transactional
    def update(GeneProductName geneProductNameInstance) {
        if (geneProductNameInstance == null) {
            notFound()
            return
        }

        if (geneProductNameInstance.hasErrors()) {
            respond geneProductNameInstance.errors, view: 'edit'
            return
        }

        geneProductNameInstance.save()

        GeneProductNameOrganismFilter.deleteAll(GeneProductNameOrganismFilter.findAllByGeneProductName(geneProductNameInstance))

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new GeneProductNameOrganismFilter(
                organism: organism,
                geneProductName: geneProductNameInstance
            ).save()
        }

        geneProductNameInstance.save(flush: true)


        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'GeneProductName.label', default: 'GeneProductName'), geneProductNameInstance.id])
                redirect geneProductNameInstance
            }
            '*' { respond geneProductNameInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(GeneProductName geneProductNameInstance) {

        if (geneProductNameInstance == null) {
            notFound()
            return
        }

        geneProductNameInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'GeneProductName.label', default: 'GeneProductName'), geneProductNameInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'geneProductName.label', default: 'GeneProductName'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }

    @RestApiMethod(description = "A comma-delimited list of gene product names", path = "/geneProduct/addGeneProductNames", verb = RestApiVerb.POST)
    @RestApiParams(params = [
        @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "names", type = "string", paramType = RestApiParamType.QUERY, description = "A comma-delimited list of gene product names to add")
        , @RestApiParam(name = "organisms", type = "string", paramType = RestApiParamType.QUERY, description = "(optional, default is none) List of organisms ids limit ALL entries to.  E.g., [3,5]")
    ])
    @Transactional
    def addGeneProductNames() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            println "Adding suggested gene product names ${nameJson}"
            if (!permissionService.hasGlobalPermissions(nameJson, GlobalPermissionEnum.ADMIN)) {
                println "DOES NOT have global permissions"
                render status: UNAUTHORIZED
                return
            }

            if (nameJson.names) {
                def organisms = []
                if (nameJson.organisms) {
                    organisms = Organism.findAllByIdInList(nameJson.organisms)
                }
                for (name in nameJson.names) {
                    GeneProductName geneProductName = GeneProductName.findOrSaveByName(name)
                    organisms.each {
                        GeneProductNameOrganismFilter.findOrSaveByGeneProductNameAndOrganism(geneProductName, it)
                    }
                }
                render nameJson.names as JSON
            } else {
                def error = [error: 'names not found']
                println(error.error)
                render error as JSON
            }
        }

        catch (
            Exception e
            ) {
            def error = [error: 'problem adding suggested names: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

}
