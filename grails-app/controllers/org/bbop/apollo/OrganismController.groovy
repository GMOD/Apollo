package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.report.OrganismSummary
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.NOT_FOUND

@RestApi(name = "Organism Services", description = "Methods for managing organims")
@Transactional(readOnly = true)
class OrganismController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def permissionService
    def requestHandlingService
    def preferenceService
    def organismService
    def reportService


    @RestApiMethod(description = "Remove an organism", path = "/organism/deleteOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "json", paramType = RestApiParamType.QUERY, description = "Pass an Organism JSON object with an 'id' that corresponds to the id to delete")
    ])
    @Transactional
    def deleteOrganism() {
        try {
            JSONObject organismJson = permissionService.handleInput(request, params)
            log.debug "deleteOrganism ${organismJson}"
            if (permissionService.isUserAdmin(permissionService.getCurrentUser(organismJson))) {

                log.debug "organism ID: ${organismJson.id}"
                Organism organism = Organism.findById(organismJson.id as Long)
                if (!organism) {
                    organism = Organism.findByCommonName(organismJson.organism)
                }
                if (organism) {
                    UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByOrganism(organism))
                    organism.delete()
                }
                log.info "Success deleting organism: ${organismJson.id}"
                render findAllOrganisms()
            } else {
                def error = [error: 'not authorized to delete organism']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem deleting organism: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Remove features from an organism", path = "/organism/deleteOrganismFeatures", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "json", paramType = RestApiParamType.QUERY, description = "An organism json object that has an 'id' or 'commonName' parameter that corresponds to an organism.")
    ])
    @Transactional
    def deleteOrganismFeatures() {
        JSONObject organismJson = permissionService.handleInput(request, params)
        if (organismJson.username == "" || organismJson.organism == "" || organismJson.password == "") {
            def error = ['error': 'Empty fields in request JSON']
            render error as JSON
            log.error(error.error)
            return
        }
        try {
            if (!permissionService.hasPermissions(organismJson, PermissionEnum.ADMINISTRATE)) {
                def error = [error: 'not authorized to delete all features from organism']
                log.error(error.error)
                render error as JSON
                return
            }

            Organism organism = Organism.findByCommonName(organismJson.organism)

            if (!organism) {
                organism = Organism.findById(organismJson.organism)
            }

            if (!organism) {
                throw new Exception("Can not find organism for ${organismJson.organism} to remove features of")
            }

            organismService.deleteAllFeaturesForOrganism(organism)
            render [:] as JSON
        }
        catch (e) {
            def error = [error: 'problem removing organism features for organism: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Adds an organism returning a JSON array of all organisms", path = "/organism/addOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "directory", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for the organisms data directory (required)")
            , @RestApiParam(name = "species", type = "string", paramType = RestApiParamType.QUERY, description = "species name")
            , @RestApiParam(name = "genus", type = "string", paramType = RestApiParamType.QUERY, description = "species genus")
            , @RestApiParam(name = "blatdb", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for a BLAT database (e.g. a .2bit file)")
            , @RestApiParam(name = "publicMode", type = "boolean", paramType = RestApiParamType.QUERY, description = "a flag for whether the organism appears as in the public genomes list")
            , @RestApiParam(name = "commonName", type = "string", paramType = RestApiParamType.QUERY, description = "a name used for the organism")
    ])
    @Transactional
    def addOrganism() {
        JSONObject organismJson = permissionService.handleInput(request, params)
        String clientToken = organismJson.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        try {
            if (permissionService.isUserAdmin(permissionService.getCurrentUser(organismJson))) {
                if (organismJson.get("commonName") == "" || organismJson.get("directory") == "") {
                    throw new Exception('empty fields detected')
                }

                log.debug "Adding ${organismJson.publicMode}"
                Organism organism = new Organism(
                        commonName: organismJson.commonName
                        , directory: organismJson.directory
                        , blatdb: organismJson.blatdb
                        , species: organismJson.species
                        , genus: organismJson.genus
                        , publicMode: organismJson.publicMode
                )
                log.debug "organism ${organism as JSON}"

                if (checkOrganism(organism)) {
                    organism.save(failOnError: true, flush: true, insert: true)
                }
                preferenceService.setCurrentOrganism(permissionService.getCurrentUser(organismJson), organism, clientToken)
                sequenceService.loadRefSeqs(organism)
                render findAllOrganisms()
            } else {
                def error = [error: 'not authorized to add organism']
                render error as JSON
                log.error(error.error)
            }
        } catch (e) {
            def error = [error: 'problem saving organism: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Finds sequences for a given organism and returns a JSON object including the username, organism and a JSONArray of sequences", path = "/organism/getSequencesForOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Common name or ID for the organism")
    ])
    def getSequencesForOrganism() {
        JSONObject organismJson = permissionService.handleInput(request, params)
        if (organismJson.username == "" || organismJson.organism == "" || organismJson.password == "") {
            render(['error': 'Empty fields in request JSON'] as JSON)
            return
        }

        List<Sequence> sequenceList

        Organism organism = Organism.findByCommonName(organismJson.organism)
        if (!organism) {
            organism = Organism.findById(organismJson.organism)
        }
        if (!organism) {
            def error = ['error': 'Organism not found ' + organismJson.organism]
            render error as JSON
            log.error(error.error)
            return
        }


        if (permissionService.findHighestOrganismPermissionForUser(organism, permissionService.getCurrentUser(organismJson)).rank >= PermissionEnum.EXPORT.rank) {
            def c = Sequence.createCriteria()
            sequenceList = c.list {
                eq('organism', organism)
            }
            log.debug "Sequence list fetched at getSequencesForOrganism: ${sequenceList}"
        } else {
            def error = ['error': 'Username ' + organismJson.username + ' does not have export permissions for organism ' + organismJson.organism]
            render error as JSON
            log.error(error.error)
            return
        }

        render([username: organismJson.username, organism: organismJson.organism, sequences: sequenceList] as JSON)
    }

    private boolean checkOrganism(Organism organism) {
        File directory = new File(organism.directory)
        File trackListFile = new File(organism.getTrackList())
        File refSeqFile = new File(organism.getRefseqFile())

        if (!directory.exists() || !directory.isDirectory()) {
            organism.valid = false
            throw new Exception("Invalid directory specified: " + directory.absolutePath)
        } else if (!trackListFile.exists()) {
            organism.valid = false
            throw new Exception("Track file does not exists: " + trackListFile.absolutePath)
        } else if (!refSeqFile.exists()) {
            organism.valid = false
            throw new Exception("Reference sequence file does not exists: " + refSeqFile.absolutePath)
        } else {
            organism.valid = true
        }
        return organism.valid
    }


    @RestApiMethod(description = "Adds an organism returning a JSON array of all organisms", path = "/organism/updateOrganismInfo", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "unique id of organism to change")
            , @RestApiParam(name = "directory", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for the organisms data directory (required)")
            , @RestApiParam(name = "species", type = "string", paramType = RestApiParamType.QUERY, description = "species name")
            , @RestApiParam(name = "genus", type = "string", paramType = RestApiParamType.QUERY, description = "species genus")
            , @RestApiParam(name = "blatdb", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for a BLAT database (e.g. a .2bit file)")
            , @RestApiParam(name = "publicMode", type = "boolean", paramType = RestApiParamType.QUERY, description = "a flag for whether the organism appears as in the public genomes list")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "a common name used for the organism")
    ])
    @Transactional
    def updateOrganismInfo() {
        log.debug "updating organism info ${params}"
        try {
            JSONObject organismJson = permissionService.handleInput(request, params)
            permissionService.checkPermissions(organismJson, PermissionEnum.ADMINISTRATE)
            Organism organism = Organism.findById(organismJson.id)
            if (organism) {
                log.debug "Adding public mode ${organismJson.publicMode}"
                organism.commonName = organismJson.name
                organism.blatdb = organismJson.blatdb
                organism.species = organismJson.species
                organism.genus = organismJson.genus
                organism.directory = organismJson.directory
                organism.publicMode = organismJson.publicMode

                if (checkOrganism(organism)) {
                    organism.save(flush: true, insert: false, failOnError: true)
                } else {
                    throw new Exception("Bad organism directory: " + organism.directory)
                }
            } else {
                throw new Exception('organism not found')
            }
//            render findAllOrganisms()
            render new JSONObject() as JSON
        }
        catch (e) {
            def error = [error: 'problem saving organism: ' + e]
            render error as JSON
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Returns a JSON array of all organisms, or optionally, gets information about a specific organism", path = "/organism/findAllOrganisms", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY)
    ])
    def findAllOrganisms() {
        try {
//            JSONObject organismJson = request.JSON ?: JSON.parse(params.data.toString()) as JSONObject
            JSONObject organismJson = permissionService.handleInput(request, params)
            List<Organism> organismList = []

            if (organismJson.organism) {
                log.debug "finding info for specific organism"
                Organism organism = Organism.findByCommonName(organismJson.organism)
                if (!organism) organism = Organism.findById(organismJson.organism)
                if (!organism) render([error: "Organism not found"] as JSON)
                List<PermissionEnum> permissionEnumList = permissionService.getOrganismPermissionsForUser(organism, permissionService.getCurrentUser(organismJson))
                if (permissionService.findHighestEnum(permissionEnumList)?.rank > PermissionEnum.NONE.rank) {
                    organismList.add(organism)
                }
            } else {
                log.debug "finding all info"
                if (permissionService.isAdmin()) {
                    organismList = Organism.all
                } else {
                    organismList = permissionService.getOrganismsForCurrentUser(organismJson)
                }
            }

            if (!organismList) {
                def error = [error: 'Not authorized for any organisms']
                render error as JSON
                return
            }

            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.getCurrentUser(organismJson), true, [max: 1, sort: "lastUpdated", order: "desc"])
            Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null

            JSONArray jsonArray = new JSONArray()
            for (Organism organism in organismList) {

                def c = Feature.createCriteria()

                def list = c.list {
                    featureLocations {
                        sequence {
                            eq('organism', organism)
                        }
                    }
                    'in'('class', requestHandlingService.viewableAnnotationList)
                }
                log.debug "${list}"
                Integer annotationCount = list.size()
                Integer sequenceCount = Sequence.countByOrganism(organism)

                JSONObject jsonObject = [
                        id             : organism.id,
                        commonName     : organism.commonName,
                        blatdb         : organism.blatdb,
                        directory      : organism.directory,
                        annotationCount: annotationCount,
                        sequences      : sequenceCount,
                        genus          : organism.genus,
                        species        : organism.species,
                        valid          : organism.valid,
                        publicMode     : organism.publicMode,
                        currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false
                ] as JSONObject
                jsonArray.add(jsonObject)
            }
            render jsonArray as JSON
        }
        catch (Exception e) {
            e.printStackTrace()
            def error = [error: e.message]
            render error as JSON
        }
    }

    /**
     * Permissions handled upstream
     * @return
     */
    def report() {
        Map<Organism, OrganismSummary> organismSummaryListInstance = new TreeMap<>(new Comparator<Organism>() {
            @Override
            int compare(Organism o1, Organism o2) {
                return o1.commonName <=> o2.commonName
            }
        })

        // global version
        OrganismSummary organismSummaryInstance = reportService.generateAllFeatureSummary()


        Organism.listOrderByCommonName().each { organism ->
            OrganismSummary thisOrganismSummaryInstance = reportService.generateOrganismSummary(organism)
            organismSummaryListInstance.put(organism, thisOrganismSummaryInstance)
        }


        respond organismSummaryInstance, model: [organismSummaries: organismSummaryListInstance]
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'organism.label', default: 'Organism'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }

}
