package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.report.OrganismSummary
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.web.multipart.commons.CommonsMultipartFile

import javax.servlet.http.HttpServletResponse

import static org.springframework.http.HttpStatus.NOT_FOUND

@RestApi(name = "Organism Services", description = "Methods for managing organisms")
@Transactional(readOnly = true)
class OrganismController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def permissionService
    def requestHandlingService
    def preferenceService
    def organismService
    def reportService
    def configWrapperService
    def trackService


    @RestApiMethod(description = "Remove an organism", path = "/organism/deleteOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
            , @RestApiParam(name = "deleteData", type = "boolean", paramType = RestApiParamType.QUERY, description = "Delete organism's data directory (applicable only to those organisms with its data added via web services)")
    ])
    @Transactional
    def deleteOrganism() {
        /*
            Representative CURL query:

            curl http://localhost:8080/apollo/organism/deleteOrganism  \
            -F "organism=Amel" \
            -F "username=admin" \
            -F "password=admin" \
            -F "deleteData=true" \
            -X POST
         */
        try {
            JSONObject requestObject = permissionService.handleInput(request, params)
            log.debug "deleteOrganism ${requestObject}"
            if (permissionService.isUserAdmin(permissionService.getCurrentUser(requestObject))) {

                log.debug "organism ID: ${requestObject.organism}"
                Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.organism)

                if (organism) {
                    boolean addedViaWebServices = organism.addedViaWebServices
                    String organismDirectory = organism.directory
                    UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByOrganism(organism))
                    OrganismFilter.deleteAll(OrganismFilter.findAllByOrganism(organism))
                    organism.delete()

                    if (requestObject.containsKey("deleteData")) {
                        if (requestObject.getBoolean("deleteData")) {
                            // delete data
                            log.debug "deleteData is ${requestObject.deleteData}"
                            if (addedViaWebServices != null && addedViaWebServices) {
                                log.debug "organism ${organism.id} was added via web services; Can delete data directory"
                                File dataDirectory = new File(organismDirectory)
                                dataDirectory.deleteDir()
                            }
                            else {
                                log.error "organism ${organism.id} was not added via web services; Cannot delete data directory"
                                def warning = [warning: "Organism ${organism.id} deleted but cannot delete data directory since organism was not added via web services"]
                                render warning as JSON
                            }
                        }
                    }
                    log.info "Success deleting organism: ${requestObject.organism}"
                }
                else {
                    log.error "Organism ${requestObject.organism} not found"

                }
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
    @NotTransactional
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

    @RestApiMethod(description = "Adds an organism returning a JSON array of all organisms", path = "/organism/addOrganismWithSequence", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "directory", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for the organisms data directory (optional)")
            , @RestApiParam(name = "species", type = "string", paramType = RestApiParamType.QUERY, description = "species name")
            , @RestApiParam(name = "genus", type = "string", paramType = RestApiParamType.QUERY, description = "species genus")
            , @RestApiParam(name = "blatdb", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for a BLAT database (e.g. a .2bit file)")
            , @RestApiParam(name = "publicMode", type = "boolean", paramType = RestApiParamType.QUERY, description = "a flag for whether the organism appears as in the public genomes list")
            , @RestApiParam(name = "commonName", type = "string", paramType = RestApiParamType.QUERY, description = "a name used for the organism")
            , @RestApiParam(name = "nonDefaultTranslationTable", type = "string", paramType = RestApiParamType.QUERY, description = "non-default translation table")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "organism metadata")
            , @RestApiParam(name = "organismData", type = "file", paramType = RestApiParamType.QUERY, description = "ZIP compressed data directory")
    ])
    @Transactional
    def addOrganismWithSequence() {
        /*
            Representative CURL query:

            curl http://localhost:8080/apollo/organism/addOrganismWithSequence  \
                -F "commonName=Amel"
                -F "username=admin"
                -F "password=admin"
                -F "organismData=@/path/to/compressed-jbrowse-data.zip"
                -X POST
                -v
         */
        log.debug "addOrganismWithSequence with params: ${params.toString()}"
        JSONObject returnObject = new JSONObject()
        returnObject.put("status", new JSONArray())
        JSONObject requestObject = permissionService.handleInput(request, params)
        String clientToken = requestObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)

        if (!requestObject.containsKey("commonName")) {
            returnObject.getJSONArray("status").add("Error: Organism commonName not provided.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (!requestObject.containsKey("directory")) {
            if (request.getFile("organismData") == null) {
                returnObject.getJSONArray("status").add("Error: Neither 'directory' nor 'organismData' provided. /addOrganismWithSequence requires either one of them.")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }
        }

        String directoryName

        try {
            if (permissionService.isUserAdmin(permissionService.getCurrentUser(requestObject))) {
                log.debug "User is admin"
                def existingOrganism

                if (requestObject.containsKey("directory")) {
                    directoryName = requestObject.get("directory")
                    existingOrganism = Organism.findByCommonNameAndDirectory(requestObject.commonName, directoryName)
                }
                else {
                    // this assumes that commonName is unique, which is not desirable
                    existingOrganism = Organism.findByCommonName(requestObject.commonName)
                }

                if (existingOrganism == null) {
                    if (directoryName) {
                        log.debug "Directory provided"
                        // Note: If directory is provided, then the assumption is that the directory exists on the server filesystem
                        File directory = new File(directoryName)
                        if (directory.exists()) {
                            log.debug "Adding ${requestObject.commonName} with directory: ${directoryName}"
                            Organism organism = new Organism(
                                    commonName: requestObject.commonName,
                                    directory: directoryName,
                                    blatdb: requestObject.blatdb ?: "",
                                    genus: requestObject.genus ?: "",
                                    species: requestObject.species ?: "",
                                    metadata: requestObject.metadata ?: "",
                                    publicMode: requestObject.publicMode ?: false
                            ).save(failOnError: true, flush: true, insert: true)

                            log.debug "organism ${organism as JSON}"
                            preferenceService.setCurrentOrganism(permissionService.getCurrentUser(requestObject), organism, clientToken)
                            sequenceService.loadRefSeqs(organism)
                            findAllOrganisms()
                        }
                        else {
                            log.error "Directory ${directoryName} does not exist"
                            returnObject.getJSONArray("status").add("Error: Directory ${directoryName} does not exist.")
                        }

                    }
                    else {
                        log.debug "Directory not provided; using common data directory"
                        directoryName = configWrapperService.commonDataDirectory + File.separator + requestObject.commonName
                        File directory = new File(directoryName)
                        if (directory.exists()) {
                            log.error "directory ${directoryName} already exists"
                            returnObject.getJSONArray("status").add("Error: Directory ${directoryName} already exists.")
                            render returnObject
                            return
                        }
                        else {
                            if (directory.mkdirs()) {
                                log.debug "Successfully created directory ${directoryName}"
                                CommonsMultipartFile sequenceDataFile = request.getFile("organismData")
                                if (sequenceDataFile) {
                                    File zipFile = new File(sequenceDataFile.getOriginalFilename())
                                    sequenceDataFile.transferTo(zipFile)
                                    try {
                                        organismService.unzip(zipFile, configWrapperService.commonDataDirectory , requestObject.commonName, true)
                                    }
                                    catch (IOException e) {
                                        returnObject.getJSONArray("status").add(e.message)
                                        log.error e.printStackTrace()
                                    }
                                }

                                log.debug "Adding ${requestObject.commonName} with directory: ${directoryName}"
                                Organism organism = new Organism(
                                        commonName: requestObject.commonName,
                                        directory: directoryName,
                                        blatdb: requestObject.blatdb ?: "",
                                        genus: requestObject.genus ?: "",
                                        species: requestObject.species ?: "",
                                        metadata: requestObject.metadata ?: "",
                                        publicMode: requestObject.publicMode ?: false,
                                        addedViaWebServices: true
                                ).save(failOnError: true, flush: true, insert: true)

                                log.debug "organism ${organism as JSON}"
                                preferenceService.setCurrentOrganism(permissionService.getCurrentUser(requestObject), organism, clientToken)
                                sequenceService.loadRefSeqs(organism)
                                findAllOrganisms()
                            }
                            else {
                                log.error"Could not create ${directoryName}"
                                returnObject.getJSONArray("status").add("Error: Could not create ${directoryName}.")
                            }
                        }
                    }
                }
                else {
                    log.error "An organism with id: ${existingOrganism.id} already exists with the same commonName and directory"
                    returnObject.getJSONArray("status").add("Error: An organism with id: ${existingOrganism.id} already exists with the same commonName and directory.")
                }
            }
            else {
                log.error "username ${requestObject.username} is not authorized to add organisms"
                returnObject.getJSONArray("status").add("Error: username ${requestObject.username} is not authorized to add organisms")
            }
        } catch (Exception e) {
            log.error e.printStackTrace()
            returnObject.getJSONArray("error").add(e.message)
        }

        render returnObject as JSON
    }

    @RestApiMethod(description = "Adds a track to an existing organism returning a JSON array of all tracks for the current organism.", path = "/organism/addTrackToOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Organism id or commonName used to uniquely identify the organism")
            , @RestApiParam(name = "trackData", type = "string", paramType = RestApiParamType.QUERY, description = "ZIP compressed track data")
            , @RestApiParam(name = "trackConfig", type = "string", paramType = RestApiParamType.QUERY, description = "Track configuration (JBrowse JSON)")
    ])
    @Transactional
    def addTrackToOrganism() {
        /*
            Representative CURL query:

            curl http://localhost:8080/apollo/organism/addTrackToOrganism \
                -F "commonName=Amel"
                -F "username=admin"
                -F "password=admin"
                -F "trackData=@/path/to/compressed-track-data.zip"
                -F "trackConfig={'label': 'track_name', 'key': 'Track Name'}"
                -X POST
                -v
         */

        log.debug "addTrackToOrganism with params: ${params.toString()}"
        JSONObject returnObject = new JSONObject()
        returnObject.put("status", new JSONArray())
        JSONObject requestObject = permissionService.handleInput(request, params)

        try {
            if (!requestObject.containsKey("organism")) {
                returnObject.getJSONArray("status").add("/addTrackToOrganism requires 'organism'.")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            if (!requestObject.containsKey("trackData")) {
                returnObject.getJSONArray("status").add("/addTrackToOrganism requires 'trackData'.")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            if (!requestObject.containsKey("trackConfig")) {
                returnObject.getJSONArray("status").add("/addTrackToOrganism requires 'trackConfig'.")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            JSONObject trackConfigObject
            try {
                trackConfigObject = JSON.parse(params.get("trackConfig"))
            } catch (ConverterException ce) {
                returnObject.getJSONArray("status").add(ce.message)
                log.error ce.message
                render returnObject as JSON
                return
            }

            permissionService.checkPermissions(requestObject, PermissionEnum.ADMINISTRATE)
            log.debug "user ${requestObject.username} is admin"
            Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.organism)

            if (organism) {
                log.debug "Adding track to organism: ${organism.commonName}"
                String organismDirectoryName = organism.directory
                File organismDirectory = new File(organismDirectoryName)
                File commonDataDirectory = new File(configWrapperService.commonDataDirectory)

                CommonsMultipartFile trackDataFile = request.getFile("trackData")
                if (organismDirectory.getParentFile().getAbsolutePath() == commonDataDirectory.getAbsolutePath()) {
                    // organism data is in common data directory
                    log.debug "organism data is in common data directory"
                    if (trackDataFile) {
                        // TODO: have the user supply the urlTemplate
                        trackConfigObject.put("urlTemplate", "tracks/${trackConfigObject.label}/{refseq}/trackData.json")
                        File trackListJsonFile = new File(organism.directory + File.separator + "trackList.json")
                        JSONObject trackListObject = JSON.parse(trackListJsonFile.text)
                        JSONArray tracksArray = trackListObject.getJSONArray("tracks")
                        // check if track exists in trackList.json
                        if (trackService.findTrackFromArray(tracksArray, trackConfigObject.get("label")) == null) {
                            // add track config to trackList.json
                            tracksArray.add(trackConfigObject)
                            // unpack track data into organism directory
                            File zipFile = new File(trackDataFile.getOriginalFilename())
                            trackDataFile.transferTo(zipFile)
                            try {
                                // TODO: how to differentiate between which track data goes to tracks folder and which stays at directory root
                                // Perhaps, urlTemplate
                                organismService.unzip(zipFile, organismDirectoryName + File.separator + "tracks", trackConfigObject.get("label"), true)
                                // write to trackList.json
                                def trackListJsonWriter = trackListJsonFile.newWriter()
                                trackListJsonWriter << trackListObject.toString(4)
                                trackListJsonWriter.close()
                            }
                            catch (IOException e) {
                                returnObject.getJSONArray("status").add(e.message)
                                log.error e.printStackTrace()
                            }
                        }
                        else {
                            log.error "an entry for track ${trackConfigObject.get("label")} already exists in ${organism.directory}${File.separator}trackList.json"
                            returnObject.getJSONArray("status").add("an entry for track ${trackConfigObject.get("label")} already exists in ${organism.directory}${File.separator}trackList.json")
                        }
                    }
                }
                else {
                    // organism data is somewhere on the server where we don't want to modify anything
                    String newDirectoryName = configWrapperService.commonDataDirectory + File.separator + organism.commonName
                    File newDirectory = new File(newDirectoryName)
                    if (newDirectory.exists()) {
                        // suppl. organism directory present in common data directory
                        log.debug "suppl. organism directory ${newDirectoryName} present in common data directory"
                    }
                    else {
                        // make a new suppl. organism directory in common data directory
                        log.debug "creating suppl. organism directory ${newDirectoryName} present in common data directory"
                        newDirectory.mkdirs()

                        // write extendedTrackList.json
                        File extendedTrackListJsonFile = new File(newDirectoryName + File.separator + "extendedTrackList.json")
                        def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                        trackListJsonWriter << "{'tracks':[]}"
                        trackListJsonWriter.close()
                    }

                    if (trackDataFile) {
                        // TODO: urlTemplate
                        trackConfigObject.put("urlTemplate", "tracks/${trackConfigObject.label}/{refseq}/trackData.json")
                        File trackListJsonFile = new File(newDirectoryName + File.separator + "extendedTrackList.json")
                        JSONObject trackListObject = JSON.parse(trackListJsonFile.text)
                        trackConfigObject.put("addedViaWebServices", true)
                        JSONArray tracksArray = trackListObject.getJSONArray("tracks")
                        // check if track exists in trackList.json
                        if (trackService.findTrackFromArray(tracksArray, trackConfigObject.get("label")) == null) {
                            // add track config to trackList.json
                            tracksArray.add(trackConfigObject)
                            // unpack track data into organism directory
                            File zipFile = new File(trackDataFile.getOriginalFilename())
                            trackDataFile.transferTo(zipFile)
                            try {
                                // TODO: how to differentiate between which track data goes to tracks folder and which stays at directory root
                                organismService.unzip(zipFile, newDirectoryName + File.separator + "tracks", trackConfigObject.get("label"), true)
                                def trackListJsonWriter = trackListJsonFile.newWriter()
                                trackListJsonWriter << trackListObject.toString(4)
                                trackListJsonWriter.close()
                            }
                            catch (IOException e) {
                                returnObject.getJSONArray("status").add(e.message)
                                log.error e.printStackTrace()
                            }
                        }
                        else {
                            log.error "an entry for track ${trackConfigObject.get("label")} already exists in ${newDirectoryName}${File.separator}trackList.json"
                            returnObject.getJSONArray("status").add("an entry for track ${trackConfigObject.get("label")} already exists in ${organism.directory}${File.separator}trackList.json")
                        }
                    }

                }
            } else {
                returnObject.getJSONArray("status").add("Organism not found")
                log.error "Organism not found"
            }

        } catch (e) {
            returnObject.getJSONArray("status").add(e.message)
            log.error e.message
        }

        render returnObject as JSON
    }

    @RestApiMethod(description = "Deletes a track, previously added via web services, from an existing organism", path = "/organism/deleteTrackFromOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "An id or commonName used to uniquely identify the organism")
            , @RestApiParam(name = "trackLabel", type = "string", paramType = RestApiParamType.QUERY, description = "Track label corresponding to the track that is to be deleted")
    ])
    @Transactional
    def deleteTrackFromOrganism() {
        log.debug "deleteTrackFromOrganism with params: ${params}"
        JSONObject returnObject = new JSONObject()
        returnObject.put("error", new JSONArray())

        try {
            JSONObject requestObject = permissionService.handleInput(request, params)
            if (!requestObject.containsKey("id") && !requestObject.containsKey("commonName")) {
                returnObject.getJSONArray("error").add("Neither Organism ID nor commonName provided")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            if (!requestObject.containsKey("trackLabel")) {
                returnObject.getJSONArray("error").add("/deleteTrackFromOrganism requires 'trackLabel'")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            String trackLabel = requestObject.get("trackLabel")
            permissionService.checkPermissions(requestObject, PermissionEnum.ADMINISTRATE)
            log.debug "user ${requestObject.username} is admin"
            Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.organism)

            if (organism) {
                log.debug "organism ${organism}"
                File trackListJsonFile = new File(organism.trackList)
                JSONObject trackListObject = JSON.parse(trackListJsonFile.text) as JSONObject
                JSONObject trackObject = trackService.findTrackFromArray(trackListObject.getJSONArray("tracks"), trackLabel)

                if (trackObject == null) {
                    // track not found in trackList.json
                    log.debug "Track with label '${trackLabel}' not found; searching in extendedTrackList.json"
                    File extendedTrackListJsonFile = new File(configWrapperService.commonDataDirectory + File.separator + organism.commonName + File.separator + "/extendedTrackList.json")
                    if (extendedTrackListJsonFile.exists()) {
                        JSONObject extendedTrackListObject = JSON.parse(extendedTrackListJsonFile.text) as JSONObject
                        trackObject = trackService.findTrackFromArray(extendedTrackListObject.getJSONArray("tracks"), trackLabel)
                        if (trackObject == null) {
                            // track not found
                            log.error "Track with label '${trackLabel}' not found"
                            returnObject.getJSONArray("status").put("Error: Track with label '${trackLabel}' not found")
                        }
                        else {
                            log.debug "Track with label '${trackLabel}' found; removing from extendedTrackList.json"
                            extendedTrackListObject.getJSONArray("tracks").remove(trackObject)

                            File trackDir = new File(configWrapperService.commonDataDirectory + File.separator + organism.commonName + File.separator + "tracks" + File.separator + trackObject.label)
                            if (trackDir.exists()) {
                                log.debug "Deleting ${trackDir.getAbsolutePath()}"
                                if (trackDir.deleteDir()) {
                                    // updating extendedTrackList.json
                                    def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                    trackListJsonWriter << extendedTrackListObject.toString(4)
                                    trackListJsonWriter.close()
                                }
                            }
                            else {
                                log.error "${trackDir.getAbsolutePath()} directory not found"
                                // updating extendedTrackList.json
                                def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                trackListJsonWriter << extendedTrackListObject.toString(4)
                                trackListJsonWriter.close()
                            }
                            returnObject.put("track", trackObject)
                        }
                    }

                }
                else {
                    // track found in trackList.json
                    log.debug "track with label '${trackLabel}' found in trackList.json"
                    if (organism.addedViaWebServices) {
                        log.debug "organism was added via web services; thus can remove the track"
                        // track can be deleted since the organism and all subsequent tracks were added via web services
                        trackListObject.getJSONArray("tracks").remove(trackObject)
                        File trackDir = new File(organism.directory + File.separator + "tracks" + File.separator + trackObject.label)
                        if (trackDir.exists()) {
                            log.debug "Deleting ${trackDir.getAbsolutePath()}"
                            if (trackDir.deleteDir()) {
                                // updating trackList.json
                                def trackListJsonWriter = trackListJsonFile.newWriter()
                                trackListJsonWriter << trackListObject.toString(4)
                                trackListJsonWriter.close()
                            }
                        }
                        else {
                            log.error "${trackDir.getAbsolutePath()} directory not found"
                            // updating trackList.json
                            def trackListJsonWriter = trackListJsonFile.newWriter()
                            trackListJsonWriter << trackListObject.toString(4)
                            trackListJsonWriter.close()
                        }
                        returnObject.put("track", trackObject)
                    }
                    else {
                        // cannot delete track since its part of the main data directory
                        log.error "Track with label '${trackLabel}' found but is part of the main data directory and thus cannot be deleted."
                        returnObject.getJSONArray("status").put("Error: Track with label '${trackLabel}' found but is part of the main data directory and thus cannot be deleted.")
                    }
                }
            }
            else {
                log.error("Organism not found")
                returnObject.getJSONArray("status").add("Error: Organism not found")
            }
        } catch (Exception e) {
            log.error(e.message)
            returnObject.getJSONArray("status").add(e.message)
        }

        render returnObject as JSON
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
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "organism metadata")
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
                        , metadata: organismJson.metadata
                        , nonDefaultTranslationTable:  organismJson.nonDefaultTranslationTable ?: null
                        , publicMode: organismJson.publicMode
                )
                log.debug "organism ${organism as JSON}"

                if (checkOrganism(organism)) {
                    organism.save(failOnError: true, flush: true, insert: true)
                }
                preferenceService.setCurrentOrganism(permissionService.getCurrentUser(organismJson), organism, clientToken)

                // send file using:
//            curl \
                //  -F "userid=1" \
                //  -F "filecomment=This is an image file" \
                //  -F "sequenceData=@/home/user1/Desktop/jbrowse/sample/seq.zip" \
                //  localhost:8080/apollo
//                if (request.getFile("sequenceData)")) {
//
//                }

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
            , @RestApiParam(name = "nonDefaultTranslationTable", type = "string", paramType = RestApiParamType.QUERY, description = "non-default translation table")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "organism metadata")
    ])
    @Transactional
    def updateOrganismInfo() {
        log.debug "updating organism info ${params}"
        try {
            JSONObject organismJson = permissionService.handleInput(request, params)
            permissionService.checkPermissions(organismJson, PermissionEnum.ADMINISTRATE)
            Organism organism = Organism.findById(organismJson.id)
            if (organism) {
                log.debug "Updating organism info ${organismJson as JSON}"
                organism.commonName = organismJson.name
                organism.blatdb = organismJson.blatdb
                organism.species = organismJson.species
                organism.genus = organismJson.genus
                organism.metadata = organismJson.metadata
                organism.directory = organismJson.directory
                organism.publicMode = organismJson.publicMode
                organism.nonDefaultTranslationTable = organismJson.nonDefaultTranslationTable ?: null

                if (checkOrganism(organism)) {
                    organism.save(flush: true, insert: false, failOnError: true)
                } else {
                    throw new Exception("Bad organism directory: " + organism.directory)
                }
            } else {
                throw new Exception('organism not found')
            }
            render findAllOrganisms() as JSON
        }
        catch (e) {
            def error = [error: 'problem saving organism: ' + e]
            render error as JSON
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Update organism metadata", path = "/organism/updateOrganismMetadata", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "unique id of organism to change")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "organism metadata")
    ])
    @Transactional
    def updateOrganismMetadata() {
        log.debug "updating organism metadata ${params}"
        try {
            JSONObject organismJson = permissionService.handleInput(request, params)
            permissionService.checkPermissions(organismJson, PermissionEnum.ADMINISTRATE)
            Organism organism = Organism.findById(organismJson.id)
            if (organism) {
                log.debug "Updating organism metadata ${organismJson as JSON}"
                organism.metadata = organismJson.metadata
                organism.save(flush: true, insert: false, failOnError: true)
            } else {
                throw new Exception('Organism not found')
            }
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
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) if valid organism id or commonName is supplied show organism if user has permission")
    ])
    def findAllOrganisms() {
        try {
            JSONObject requestObject = permissionService.handleInput(request, params)
            List<Organism> organismList = []

            if (requestObject.organism) {
                log.debug "finding info for specific organism"
                Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.organism)
                if (!organism) {
                    render([error: "Organism not found"] as JSON)
                    return
                }
                List<PermissionEnum> permissionEnumList = permissionService.getOrganismPermissionsForUser(organism, permissionService.getCurrentUser(requestObject))
                if (permissionService.findHighestEnum(permissionEnumList)?.rank > PermissionEnum.NONE.rank) {
                    organismList.add(organism)
                }
            } else {
                log.debug "finding all info"
                if (permissionService.isAdmin()) {
                    organismList = Organism.all
                } else {
                    organismList = permissionService.getOrganismsForCurrentUser(requestObject)
                }
            }

            if (!organismList) {
                def error = [error: 'Not authorized for any organisms']
                render error as JSON
                return
            }

            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.getCurrentUser(requestObject), true, [max: 1, sort: "lastUpdated", order: "desc"])
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
                        nonDefaultTranslationTable : organism.nonDefaultTranslationTable,
                        metadata       : organism.metadata,
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
