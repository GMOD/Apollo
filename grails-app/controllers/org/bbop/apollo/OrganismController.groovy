package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import htsjdk.samtools.reference.FastaSequenceIndexCreator
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.gwt.shared.track.SequenceTypeEnum
import org.bbop.apollo.gwt.shared.track.TrackTypeEnum
import org.bbop.apollo.report.OrganismSummary
import org.bbop.apollo.track.TrackDefaults
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
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

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
    def fileService


    @RestApiMethod(description = "Remove an organism", path = "/organism/deleteOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "json", paramType = RestApiParamType.QUERY, description = "Pass an Organism JSON object with an 'id' that corresponds to the organism to be removed")
    ])
    @Transactional
    def deleteOrganism() {

        try {
            JSONObject organismJson = permissionService.handleInput(request, params)
            log.debug "deleteOrganism ${organismJson}"
            //if (permissionService.isUserBetterOrEqualRank(currentUser, GlobalPermissionEnum.INSTRUCTOR)){
            log.debug "organism ID: ${organismJson.id} vs ${organismJson.organism}"
            Organism organism = Organism.findById(organismJson.id as Long) ?: Organism.findByCommonName(organismJson.organism)
            if (!organism) {
                def error = [error: "Organism ${organismJson.organism} not found"]
                log.error(error.error)
                render error as JSON
                return
            }
            // to support webservice, get current user from session or input object
            def currentUser = permissionService.getCurrentUser(organismJson)
            String creatorMetaData = organism.getMetaData(FeatureStringEnum.CREATOR.value)
            // only allow global admin or organism creator or organism administrative to delete the organism
            if (!permissionService.hasGlobalPermissions(organismJson, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.checkPermissions(organismJson, organism, PermissionEnum.ADMINISTRATE)) {
                def error = [error: 'not authorized to delete organism']
                log.error(error.error)
                render error as JSON
                return
            }

            UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByOrganism(organism))
            OrganismFilter.deleteAll(OrganismFilter.findAllByOrganism(organism))
            organism.delete()
            log.info "Success deleting organism: ${organismJson.organism}"

            if (organism.directory.startsWith(trackService.commonDataDirectory)) {
                log.info "Directoy is part of the common data directory ${trackService.commonDataDirectory}, so deleting ${organism.directory}"
                File directoryToRemove = new File(organism.directory)
                assert directoryToRemove.deleteDir()
            }

            render findAllOrganisms()

        }
        catch (Exception e) {
            def error = [error: 'problem deleting organism: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Delete an organism along with its data directory and returns a JSON object containing properties of the deleted organism", path = "/organism/deleteOrganismWithSequence", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
    ])
    @Transactional
    def deleteOrganismWithSequence() {

        JSONObject requestObject = permissionService.handleInput(request, params)
        JSONObject responseObject = new JSONObject()
        log.debug "deleteOrganism ${requestObject}"

        try {
            //if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(requestObject))) {
            // use hasGolbalPermssions instead, which can validate the authentication
            if (permissionService.hasGlobalPermissions(requestObject, GlobalPermissionEnum.ADMIN)) {
                Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.organism)
                if (organism) {
                    boolean dataAddedViaWebServices = organism.dataAddedViaWebServices == null ? false : organism.dataAddedViaWebServices
                    String organismDirectory = organism.directory
                    def organismAsJSON = organism as JSON
                    UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByOrganism(organism))
                    OrganismFilter.deleteAll(OrganismFilter.findAllByOrganism(organism))
                    organism.delete()

                    if (dataAddedViaWebServices) {
                        log.debug "organism ${organism.id} was added via web services;"
                        File dataDirectory = new File(organismDirectory)
                        if (dataDirectory.deleteDir()) {
                            log.info "dataDirectory: ${organismDirectory} deleted successfully."
                        } else {
                            log.error "Could not delete data directory: ${organismDirectory}."
                            responseObject.put("warn", "Could not delete data directory: ${organismDirectory}")
                        }
                    } else {
                        log.warn "organism ${organism.id} was not added via web services; Organism deleted but cannot delete data directory ${organismDirectory}"
                        responseObject.put("warn", "Organism ${organism.id} was not added via web services; Organism deleted but cannot delete data directory ${organismDirectory}.")
                        File extendedDataDirectory = trackService.getExtendedDataDirectory(organism)
                        if (extendedDataDirectory.exists()) {
                            log.info "Extended data directory found: ${extendedDataDirectory.absolutePath}"
                            if (extendedDataDirectory.deleteDir()) {
                                log.info "extended data directory found and deleted"
                            } else {
                                log.error "Extended data directory found but could not be deleted"
                                responseObject.put("warn", responseObject.get("warn") + " Extended data directory found but could not be deleted.")
                            }
                        }
                    }
                    //render organismAsJSON
                    responseObject.put("organism", JSON.parse(organismAsJSON.toString()) as JSONObject)
                    log.info "Success deleting organism: ${requestObject.organism}"
                } else {
                    log.error "Organism: ${requestObject.organism} not found"
                    responseObject.put("error", "Organism: ${requestObject.organism} not found.")
                }
            } else {
                log.error "username not authorized to delete organism"
                responseObject.put("error", "username not authorized to delete organism.")
            }
        } catch (Exception e) {
            log.error(e.message)
            responseObject.put("error", e.message)
        }

        render responseObject as JSON
    }

    @RestApiMethod(description = "Remove features from an organism", path = "/organism/deleteOrganismFeatures", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism.")
            , @RestApiParam(name = "sequences", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Comma-delimited sequence names on that organism if only certain sequences should be deleted.")
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

            if (organismJson.sequences) {
                List<String> sequenceNames = organismJson.sequences.toString().split(",")
                List<Sequence> sequences = Sequence.findAllByOrganismAndNameInList(organism, sequenceNames)
                organismService.deleteAllFeaturesForSequences(sequences)
            } else {
                organismService.deleteAllFeaturesForOrganism(organism)
            }

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
            , @RestApiParam(name = "species", type = "string", paramType = RestApiParamType.QUERY, description = "species name")
            , @RestApiParam(name = "genus", type = "string", paramType = RestApiParamType.QUERY, description = "species genus")
            , @RestApiParam(name = "blatdb", type = "string", paramType = RestApiParamType.QUERY, description = "filesystem path for a BLAT database (e.g. a .2bit file)")
            , @RestApiParam(name = "publicMode", type = "boolean", paramType = RestApiParamType.QUERY, description = "a flag for whether the organism appears as in the public genomes list")
            , @RestApiParam(name = "commonName", type = "string", paramType = RestApiParamType.QUERY, description = "commonName for an organism")
            , @RestApiParam(name = "nonDefaultTranslationTable", type = "string", paramType = RestApiParamType.QUERY, description = "non-default translation table")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "organism metadata")
            , @RestApiParam(name = "organismData", type = "file", paramType = RestApiParamType.QUERY, description = "zip or tar.gz compressed data directory")
            , @RestApiParam(name = "sequenceData", type = "file", paramType = RestApiParamType.QUERY, description = "FASTA file (optionally compressed) to automatically upload with")
    ])
    @Transactional
    def addOrganismWithSequence() {


        JSONObject returnObject = new JSONObject()
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.info "adding organismwith SEQUENDE ${requestObject as String}"
        String clientToken = requestObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        CommonsMultipartFile organismDataFile = request.getFile(FeatureStringEnum.ORGANISM_DATA.value)
        CommonsMultipartFile sequenceDataFile = request.getFile(FeatureStringEnum.SEQUENCE_DATA.value)

        if (!requestObject.containsKey(FeatureStringEnum.ORGANISM_NAME.value)) {
            returnObject.put("error", "/addOrganismWithSequence requires '${FeatureStringEnum.ORGANISM_NAME.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (organismDataFile == null && sequenceDataFile == null) {
            returnObject.put("error", "/addOrganismWithSequence requires '${FeatureStringEnum.ORGANISM_DATA.value}' or ${FeatureStringEnum.SEQUENCE_DATA.value}.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (organismDataFile != null && sequenceDataFile != null) {
            returnObject.put("error", "/addOrganismWithSequence requires ONLY one (not both) of '${FeatureStringEnum.ORGANISM_DATA.value}' or ${FeatureStringEnum.SEQUENCE_DATA.value}.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        try {
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(requestObject))) {
                String organismName = requestObject.get(FeatureStringEnum.ORGANISM_NAME.value)
                def organism = new Organism(
                        commonName: organismName,
                        directory: trackService.commonDataDirectory,
                        blatdb: requestObject.blatdb ?: "",
                        genus: requestObject.genus ?: "",
                        obsolete: false,
                        valid: true,
                        species: requestObject.species ?: "",
                        metadata: requestObject.metadata ?: "",
                        publicMode: requestObject.publicMode ?: false,
                        nonDefaultTranslationTable: requestObject.nonDefaultTranslationTable ?: null,
                        dataAddedViaWebServices: true
                ).save(failOnError: true, flush: true, insert: true)
                def currentUser = permissionService.currentUser
                organism.addMetaData("creator", currentUser.id.toString())
                File directory = trackService.getExtendedDataDirectory(organism)

                if (directory.mkdirs() && directory.setWritable(true)) {

                    if (organismDataFile) {
                        log.debug "Successfully created directory ${directory.absolutePath}"
                        File archiveFile = new File(organismDataFile.getOriginalFilename())
                        organismDataFile.transferTo(archiveFile)
                        try {
                            fileService.decompress(archiveFile, directory.absolutePath, null, false)
                            log.debug "Adding ${organismName} with directory: ${directory.absolutePath}"
                            organism.directory = directory.absolutePath
                            organism.save()
                            sequenceService.loadRefSeqs(organism)
                            preferenceService.setCurrentOrganism(permissionService.getCurrentUser(requestObject), organism, clientToken)
                            findAllOrganisms()
                        }
                        catch (IOException e) {
                            log.error e.printStackTrace()
                            returnObject.put("error", e.message)
                            organism.delete()
                            render returnObject
                            return
                        }
                    } else if (sequenceDataFile) {

                        SequenceTypeEnum sequenceTypeEnum = SequenceTypeEnum.getSequenceTypeForFile(sequenceDataFile.getOriginalFilename())
                        if (sequenceTypeEnum == null) {
                            returnObject.put("error", "Bad file input: " + sequenceDataFile.originalFilename)
                            render returnObject
                            return
                        }

                        // TODO: put this in a temp directory? ? ?
                        try {
                            File rawDirectory = new File(directory.absolutePath + "/seq")
                            assert rawDirectory.mkdir()
                            assert rawDirectory.setWritable(true)
                            File archiveFile = new File(rawDirectory.absolutePath + File.separator + organismName + "." + sequenceTypeEnum.suffix)
                            sequenceDataFile.transferTo(archiveFile)

                            organism.directory = directory.absolutePath
                            organism.save()

                            // decompress if need be
                            if (sequenceTypeEnum.compression != null) {
                                List<String> fileNames = fileService.decompress(archiveFile, rawDirectory.absolutePath)
                                // move the filenames to the same original name, let's assume there is one
                                File oldFile = new File(fileNames[0])
                                assert oldFile.exists()
                                assert oldFile.absolutePath.endsWith(".fa")
                                File newFile = new File(rawDirectory.absolutePath + File.separator + organismName + ".fa")
                                oldFile.renameTo(newFile)
                            }

                            String trackListJson = TrackDefaults.getIndexedFastaConfig(organismName)
                            File trackListFile = new File(directory.absolutePath + File.separator + "trackList.json")
                            trackListFile.write(trackListJson)

                            // create an index
                            Path path = FileSystems.getDefault().getPath(rawDirectory.absolutePath + File.separator + organismName + ".fa")
                            FastaSequenceIndexCreator.create(path, true)

                            sequenceService.loadRefSeqs(organism)
                            preferenceService.setCurrentOrganism(permissionService.getCurrentUser(requestObject), organism, clientToken)
                            findAllOrganisms()
                        }
                        catch (IOException e) {
                            log.error e.printStackTrace()
                            returnObject.put("error", e.message)
                            organism.delete()
                        }

                    } else {
                        throw new RuntimeException("Not sure how we got here ")
                    }
                } else {
                    log.error "Could not create ${directory.absolutePath}"
                    returnObject.put("error", "Could not create ${directory.absolutePath}.")
                    organism.delete()
                }
            } else {
                log.error "username ${requestObject.get(FeatureStringEnum.USERNAME.value)} is not authorized to add organisms"
                returnObject.put("error", "username ${requestObject.get(FeatureStringEnum.USERNAME.value)} is not authorized to add organisms.")
            }
        }
        catch (e) {
            log.error e.printStackTrace()
            returnObject.put("error", e.message)
        }

        render returnObject as JSON
    }

    @RestApiMethod(description = "Removes an added track from an existing organism returning a JSON object containing all tracks for the current organism.", path = "/organism/removeTrackFromOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
            , @RestApiParam(name = "trackLabel", type = "string", paramType = RestApiParamType.QUERY, description = "Name of track")
    ])
    @Transactional
    def removeTrackFromOrganism() {
        JSONObject returnObject = new JSONObject()
        JSONObject requestObject = permissionService.handleInput(request, params)
        log.info "removing track from organism with ${requestObject}"

        if (!requestObject.containsKey(FeatureStringEnum.ORGANISM.value)) {
            returnObject.put("error", "/removeTrackFromOrganism requires '${FeatureStringEnum.ORGANISM.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (!requestObject.containsKey(FeatureStringEnum.TRACK_LABEL.value)) {
            returnObject.put("error", "/removeTrackFromOrganism requires '${FeatureStringEnum.TRACK_LABEL.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        try {
            permissionService.checkPermissions(requestObject, PermissionEnum.ADMINISTRATE)
            Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.get(FeatureStringEnum.ORGANISM.value)?.id)
            // find in the extended track list and remove
            File extendedDirectory = trackService.getExtendedDataDirectory(organism)
            if (!extendedDirectory.exists()) {
                returnObject.put("error", "No temporary directory found to remove tracks from ${extendedDirectory.absolutePath}")
                render returnObject as JSON
                return
            }
            File extendedTrackListJsonFile
            if (new File(extendedDirectory.absolutePath + File.separator + TrackService.EXTENDED_TRACKLIST).exists()) {
                extendedTrackListJsonFile = new File(extendedDirectory.absolutePath + File.separator + TrackService.EXTENDED_TRACKLIST)
            } else {
                if (organism.directory.contains(trackService.commonDataDirectory)) {
                    extendedTrackListJsonFile = new File(organism.directory + File.separator + trackService.TRACKLIST)
                } else {
                    throw new RuntimeException("Can not delete tracks from a non-temporary directory: ${extendedTrackListJsonFile.absolutePath}")
                }
            }

            JSONObject extendedTrackListObject = JSON.parse(extendedTrackListJsonFile.text)
            JSONArray extendedTracksArray = extendedTrackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)

            String trackLabel = requestObject.getString(FeatureStringEnum.TRACK_LABEL.value)
            JSONObject trackObject = trackService.findTrackFromArray(extendedTracksArray, trackLabel)
            extendedTracksArray = trackService.removeTrackFromArray(extendedTracksArray, trackLabel)
            extendedTrackListObject.put(FeatureStringEnum.TRACKS.value, extendedTracksArray)
            extendedTrackListJsonFile.write(extendedTrackListObject.toString())

            TrackTypeEnum trackTypeEnum = TrackTypeEnum.valueOf(trackObject.apollo.type)
            // delete any files with the patterns of key.suffix and key.suffixIndex

            for (def suffix in trackTypeEnum.suffix) {
                File fileToDelete = new File(extendedDirectory.absolutePath + File.separator + "raw/" + trackObject.label.replaceAll(" ", "_") + ".${suffix}")
                if (fileToDelete.exists()) {
                    assert fileToDelete.delete()
                }
            }
            for (def suffix in trackTypeEnum.suffixIndex) {
                File fileToDelete = new File(extendedDirectory.absolutePath + File.separator + "raw/" + trackObject.label.replaceAll(" ", "_") + ".${suffix}")
                if (fileToDelete.exists()) {
                    assert fileToDelete.delete()
                }
            }
            render returnObject as JSON
        } catch (Exception ce) {
            log.error ce.message
            returnObject.put("error", ce.message)
            render returnObject as JSON
            return
        }

    }


    @RestApiMethod(description = "Adds a track to an existing organism returning a JSON object containing all tracks for the current organism.", path = "/organism/addTrackToOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
            , @RestApiParam(name = "trackData", type = "string", paramType = RestApiParamType.QUERY, description = "zip or tar.gz compressed track data")
            , @RestApiParam(name = "trackFile", type = "string", paramType = RestApiParamType.QUERY, description = "track file (*.bam, *.vcf, *.bw, *gff)")
            , @RestApiParam(name = "trackFileIndex", type = "string", paramType = RestApiParamType.QUERY, description = "index (*.bai, *.tbi)")
            , @RestApiParam(name = "trackConfig", type = "string", paramType = RestApiParamType.QUERY, description = "Track configuration (JBrowse JSON)")
    ])
    @Transactional
    def addTrackToOrganism() {

        JSONObject returnObject = new JSONObject()
        JSONObject requestObject = permissionService.handleInput(request, params)
        String pathToJBrowseBinaries = servletContext.getRealPath("/jbrowse/bin")
        log.debug "path to JBrowse binaries ${pathToJBrowseBinaries}"

        if (!requestObject.containsKey(FeatureStringEnum.ORGANISM.value)) {
            returnObject.put("error", "/addTrackToOrganism requires '${FeatureStringEnum.ORGANISM.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (requestObject.containsKey(FeatureStringEnum.TRACK_DATA.value) && requestObject.containsKey("trackFile")) {
            returnObject.put("error", "Both 'trackData' and 'trackFile' specified; /addTrackToOrganism requires either '${FeatureStringEnum.TRACK_DATA.value}' or 'trackFile'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (!requestObject.containsKey(FeatureStringEnum.TRACK_DATA.value) && !requestObject.containsKey("trackFile")) {
            returnObject.put("error", "/addTrackToOrganism requires either '${FeatureStringEnum.TRACK_DATA.value}' or 'trackFile'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (!requestObject.containsKey(FeatureStringEnum.TRACK_CONFIG.value)) {
            returnObject.put("error", "/addTrackToOrganism requires '${FeatureStringEnum.TRACK_CONFIG.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        JSONObject trackConfigObject
        try {
            trackConfigObject = JSON.parse(params.get(FeatureStringEnum.TRACK_CONFIG.value) as String) as JSONObject
        } catch (ConverterException ce) {
            log.error ce.message
            returnObject.put("error", ce.message)
            render returnObject as JSON
            return
        }

        if (!trackConfigObject.containsKey(FeatureStringEnum.LABEL.value) || !trackConfigObject.containsKey(FeatureStringEnum.URL_TEMPLATE.value)) {
            log.error "trackConfig requires '${FeatureStringEnum.LABEL.value}' and '${FeatureStringEnum.URL_TEMPLATE.value}'"
            returnObject.put("error", "trackConfig requires '${FeatureStringEnum.LABEL.value}' and '${FeatureStringEnum.URL_TEMPLATE.value}'.")
            render returnObject as JSON
            return
        }

        try {
            permissionService.checkPermissions(requestObject, PermissionEnum.ADMINISTRATE)
//            log.debug "user ${requestObject.get(FeatureStringEnum.USERNAME.value)} is admin"
            Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.get(FeatureStringEnum.ORGANISM.value))

            if (organism) {
                log.debug "Adding track to organism: ${organism.commonName}"
                String organismDirectoryName = organism.directory
                File organismDirectory = new File(organismDirectoryName)
                File commonDataDirectory = new File(trackService.commonDataDirectory)

                CommonsMultipartFile trackDataFile = request.getFile(FeatureStringEnum.TRACK_DATA.value)
                CommonsMultipartFile trackFile = request.getFile(FeatureStringEnum.TRACK_FILE.value)
                CommonsMultipartFile trackFileIndex = request.getFile(FeatureStringEnum.TRACK_FILE_INDEX.value)

                if (organismDirectory.getParentFile().getCanonicalPath() == commonDataDirectory.getCanonicalPath()) {
                    // organism data is in common data directory
                    log.info "organism data is in common data directory"
                    File trackListJsonFile = new File(organism.directory + File.separator + trackService.TRACKLIST)
                    JSONObject trackListObject = JSON.parse(trackListJsonFile.text)
                    JSONArray tracksArray = trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)

                    if (trackDataFile) {
                        // check if track exists in trackList.json
                        if (trackService.findTrackFromArray(tracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value)) == null) {
                            // add track config to trackList.json
                            tracksArray.add(trackConfigObject)
                            // unpack track data into organism directory
                            File archiveFile = new File(trackDataFile.getOriginalFilename())
                            trackDataFile.transferTo(archiveFile)
                            try {
                                String urlTemplate = trackConfigObject.get(FeatureStringEnum.URL_TEMPLATE.value)
                                String trackDirectoryName = urlTemplate.split("/").first()
                                String path = organismDirectoryName + File.separator + trackDirectoryName
                                fileService.decompress(archiveFile, path, trackConfigObject.get(FeatureStringEnum.LABEL.value), true)

                                // write to trackList.json
                                def trackListJsonWriter = trackListJsonFile.newWriter()
                                trackListJsonWriter << trackListObject.toString(4)
                                trackListJsonWriter.close()
                                returnObject.put(FeatureStringEnum.TRACKS.value, tracksArray)
                            }
                            catch (IOException e) {
                                log.error e.printStackTrace()
                                returnObject.put("error", e.message)
                            }
                        } else {
                            log.error "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}"
                            returnObject.put("error", "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}.")
                        }
                    } else {
                        // trackDataFile is null; use data from trackFile and trackFileIndex, if available
                        if (trackFile) {
                            if (trackService.findTrackFromArray(tracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value)) == null) {
                                // add track config to trackList.json
                                tracksArray.add(trackConfigObject)
                                try {
                                    String urlTemplate = trackConfigObject.get(FeatureStringEnum.URL_TEMPLATE.value)
                                    String trackDirectoryName = urlTemplate.split("/").first()
                                    String path = organismDirectoryName + File.separator + trackDirectoryName
//                                    fileService.store(trackFile, path)
                                    TrackTypeEnum trackTypeEnum = org.bbop.apollo.gwt.shared.track.TrackTypeEnum.valueOf(trackConfigObject.apollo.type)
                                    String newFileName = trackTypeEnum ? trackConfigObject.key + "." + trackTypeEnum.suffix[0] : trackFile.originalFilename
                                    File destinationFile = fileService.storeWithNewName(trackFile, path, trackConfigObject.key, newFileName)
                                    if (trackFileIndex.originalFilename) {
                                        String newFileNameIndex = trackTypeEnum ? trackConfigObject.key + "." + trackTypeEnum.suffixIndex[0] : trackFileIndex.originalFilename
//                                        fileService.store(trackFileIndex, path)
                                        fileService.storeWithNewName(trackFileIndex, path, trackConfigObject.key, newFileNameIndex)
                                    }

                                    if (trackTypeEnum == TrackTypeEnum.GFF3_JSON || trackTypeEnum == TrackTypeEnum.GFF3_JSON_CANVAS) {
                                        trackService.generateJSONForGff3(destinationFile, organismDirectoryName, pathToJBrowseBinaries,trackConfigObject.apollo.topType)
                                    }

                                    // write to trackList.json
                                    def trackListJsonWriter = trackListJsonFile.newWriter()
                                    trackListJsonWriter << trackListObject.toString(4)
                                    trackListJsonWriter.close()
                                    returnObject.put(FeatureStringEnum.TRACKS.value, tracksArray)
                                }
                                catch (IOException e) {
                                    log.error e.printStackTrace()
                                    returnObject.put("error", e.message)
                                }
                            } else {
                                log.error "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}"
                                returnObject.put("error", "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}.")
                            }
                        }
                    }
                } else {
                    // organism data is somewhere on the server where we don't want to modify anything
                    File trackListJsonFile = new File(organism.directory + File.separator + trackService.TRACKLIST)
                    JSONObject trackListObject = JSON.parse(trackListJsonFile.text)
                    JSONArray tracksArray = trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)
                    if (trackService.findTrackFromArray(tracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value)) != null) {
                        log.error "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}"
                        returnObject.put("error", "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}.")
                    } else {
                        File extendedDirectory = trackService.getExtendedDataDirectory(organism)
                        if (!extendedDirectory.exists()) {
                            // make a new extended organism directory in common data directory
                            if (extendedDirectory.mkdirs() && extendedDirectory.setWritable(true)) {
                                // write extendedTrackList.json
                                File extendedTrackListJsonFile = trackService.getExtendedTrackList(organism)
                                def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                trackListJsonWriter << "{'${FeatureStringEnum.TRACKS.value}':[]}"
                                trackListJsonWriter.close()
                            } else {
                                log.error "Cannot create directory ${extendedDirectory.absolutePath}"
                                returnObject.put("error", "Cannot create directory ${extendedDirectory.absolutePath}.")
                            }
                        }

                        if (trackDataFile) {
                            File extendedTrackListJsonFile = trackService.getExtendedTrackList(organism)
                            JSONObject extendedTrackListObject = JSON.parse(extendedTrackListJsonFile.text)
                            JSONArray extendedTracksArray = extendedTrackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)
                            // check if track exists in extendedTrackList.json
                            if (trackService.findTrackFromArray(extendedTracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value)) != null) {
                                log.error "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${extendedDirectory.absolutePath}/${trackService.EXTENDED_TRACKLIST}"
                                returnObject.put("error", "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${extendedDirectory.absolutePath}/${trackService.EXTENDED_TRACKLIST}.")
                            } else {
                                // add track config to extendedTrackList.json
                                extendedTracksArray.add(trackConfigObject)
                                // unpack track data into organism directory
                                File archiveFile = new File(trackDataFile.getOriginalFilename())
                                trackDataFile.transferTo(archiveFile)
                                try {
                                    String urlTemplate = trackConfigObject.get(FeatureStringEnum.URL_TEMPLATE.value)
                                    String trackDirectoryName = urlTemplate.split("/").first()
                                    String path = extendedDirectory.absolutePath + File.separator + trackDirectoryName
                                    fileService.decompress(archiveFile, path, trackConfigObject.get(FeatureStringEnum.LABEL.value), true)

                                    // write to trackList.json
                                    def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                    trackListJsonWriter << extendedTrackListObject.toString(4)
                                    trackListJsonWriter.close()
                                    returnObject.put(FeatureStringEnum.TRACKS.value, tracksArray + extendedTracksArray)
                                }
                                catch (IOException e) {
                                    log.error e.printStackTrace()
                                    returnObject.put("error", e.message)
                                }
                            }
                        } else {
                            if (trackFile) {
                                if (trackService.findTrackFromArray(tracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value)) == null) {
                                    // add track config to trackList.json
                                    File extendedTrackListJsonFile = trackService.getExtendedTrackList(organism)
                                    if (!extendedTrackListJsonFile.exists()) {
                                        def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                        trackListJsonWriter << "{'${FeatureStringEnum.TRACKS.value}':[]}"
                                        trackListJsonWriter.close()
                                    } else {
                                        log.info "FILE EXISTS, so nothing to do ${extendedTrackListJsonFile.text}"
                                    }
                                    JSONObject extendedTrackListObject = JSON.parse(extendedTrackListJsonFile.text)
                                    JSONArray extendedTracksArray = extendedTrackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)
                                    if (trackService.findTrackFromArray(extendedTracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value)) != null) {
                                        log.error "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}"
                                        returnObject.put("error", "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}.")
                                    } else {
                                        try {
                                            String path = extendedDirectory.absolutePath + File.separator + "raw"
                                            TrackTypeEnum trackTypeEnum = org.bbop.apollo.gwt.shared.track.TrackTypeEnum.valueOf(trackConfigObject.apollo.type)

                                            // TODO: if the suffix is 0 does not end with gzip, then we need to run it through the decrompressor
                                            String newFileName = trackTypeEnum ? trackConfigObject.key + "." + trackTypeEnum.suffix[0] : trackFile.originalFilename
//                                            File destinationFile
//                                            if( (trackTypeEnum == TrackTypeEnum.GFF3_JSON || trackTypeEnum == TrackTypeEnum.GFF3_JSON_CANVAS) && trackFile.originalFilename.endsWith(".gz")){
//                                                File archiveFile = new File(trackFile.originalFilename)
//                                                trackFile.transferTo(archiveFile)
////                                                String outputFilePath = fileService.decompress(archiveFile, path, trackConfigObject.get(FeatureStringEnum.LABEL.value), true)[0]
//                                                String outputFilePath = fileService.decompressGzipArchive(archiveFile, path, null,false)[0]
//                                                destinationFile = new File(outputFilePath)
////                                                destinationFile = fileService.storeWithNewName(outputFile, path, trackConfigObject.key, newFileName)
//                                            }
//                                            else{
//                                            destinationFile = fileService.storeWithNewName(trackFile, path, trackConfigObject.key, newFileName)
//                                            }

                                            File destinationFile = fileService.storeWithNewName(trackFile, path, trackConfigObject.key, newFileName)
                                            if (trackFileIndex.getOriginalFilename()) {
                                                String newFileNameIndex = trackTypeEnum ? trackConfigObject.key + "." + trackTypeEnum.suffixIndex[0] : trackFileIndex.originalFilename
                                                fileService.storeWithNewName(trackFileIndex, path, trackConfigObject.key, newFileNameIndex)
                                            }

                                            if (trackTypeEnum == TrackTypeEnum.GFF3_JSON || trackTypeEnum == TrackTypeEnum.GFF3_JSON_CANVAS) {
                                                trackService.generateJSONForGff3(destinationFile, extendedDirectory.absolutePath, pathToJBrowseBinaries,trackConfigObject.apollo.topType)
                                            }

                                            extendedTracksArray.add(trackConfigObject)
                                            extendedTrackListObject.put(FeatureStringEnum.TRACKS.value, extendedTracksArray)

                                            // write to trackList.json
                                            def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                            trackListJsonWriter << extendedTrackListObject.toString(4)
                                            trackListJsonWriter.close()
                                            returnObject.put(FeatureStringEnum.TRACKS.value, tracksArray)
                                        }
                                        catch (IOException e) {
                                            log.error e.printStackTrace()
                                            returnObject.put("error", e.message)
                                        }
                                    }
                                    log.debug "trackJsonWriter: -> ${extendedTrackListJsonFile.absolutePath}, ${extendedTrackListJsonFile.text}"
                                } else {
                                    log.error "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}"
                                    returnObject.put("error", "an entry for track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' already exists in ${organism.directory}/${TRACKLIST}.")
                                }
                            }
                        }
                    }
                }
            } else {
                log.error "Organism not found"
                returnObject.put("error", "Organism not found.")
            }

        } catch (e) {
            log.error e.message
            returnObject.put("error", e.message)
        }

        render returnObject as JSON
    }

    @RestApiMethod(description = "Deletes a track from an existing organism and returns a JSON object of the deleted track's configuration", path = "/organism/deleteTrackFromOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
            , @RestApiParam(name = "trackLabel", type = "string", paramType = RestApiParamType.QUERY, description = "Track label corresponding to the track that is to be deleted")
    ])
    @Transactional
    def deleteTrackFromOrganism() {

        JSONObject returnObject = new JSONObject()

        try {
            JSONObject requestObject = permissionService.handleInput(request, params)
            if (!requestObject.containsKey(FeatureStringEnum.ORGANISM.value)) {
                returnObject.put("error", "/deleteTrackFromOrganism requires '${FeatureStringEnum.ORGANISM.value}'.")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            if (!requestObject.containsKey(FeatureStringEnum.TRACK_LABEL.value)) {
                returnObject.put("error", "/deleteTrackFromOrganism requires '${FeatureStringEnum.TRACK_LABEL.value}'.")
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                render returnObject as JSON
                return
            }

            String trackLabel = requestObject.get(FeatureStringEnum.TRACK_LABEL.value)
            permissionService.checkPermissions(requestObject, PermissionEnum.ADMINISTRATE)
            log.debug "user ${requestObject.get(FeatureStringEnum.USERNAME.value)} is admin"
            Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.get(FeatureStringEnum.ORGANISM.value))

            if (organism) {
                log.debug "organism ${organism}"
                File trackListJsonFile = new File(organism.trackList)
                JSONObject trackListObject = JSON.parse(trackListJsonFile.text) as JSONObject
                JSONObject trackObject = trackService.findTrackFromArray(trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value), trackLabel)

                if (trackObject == null) {
                    // track not found in trackList.json
                    log.debug "Track with label '${trackLabel}' not found; searching in extendedTrackList.json"
                    File extendedTrackListJsonFile = trackService.getExtendedTrackList(organism)
                    if (extendedTrackListJsonFile.exists()) {
                        JSONObject extendedTrackListObject = JSON.parse(extendedTrackListJsonFile.text) as JSONObject
                        trackObject = trackService.findTrackFromArray(extendedTrackListObject.getJSONArray(FeatureStringEnum.TRACKS.value), trackLabel)
                        if (trackObject == null) {
                            // track not found
                            log.error "Track with label '${trackLabel}' not found"
                            returnObject.put("error", "Track with label '${trackLabel}' not found.")
                        } else {
                            log.debug "Track with label '${trackLabel}' found; removing from extendedTrackList.json"
                            extendedTrackListObject.getJSONArray(FeatureStringEnum.TRACKS.value).remove(trackObject)
                            String urlTemplate = trackObject.get(FeatureStringEnum.URL_TEMPLATE.value)
                            String trackDirectory = urlTemplate.split("/").first()
                            File commonDirectory = trackService.getExtendedDataDirectory(organism)
                            File trackDir = new File(commonDirectory.absolutePath + File.separator + trackDirectory + File.separator + trackObject.get(FeatureStringEnum.LABEL.value))
                            if (trackDir.exists()) {
                                log.debug "Deleting ${trackDir.getAbsolutePath()}"
                                if (trackDir.deleteDir()) {
                                    // updating extendedTrackList.json
                                    def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                    trackListJsonWriter << extendedTrackListObject.toString(4)
                                    trackListJsonWriter.close()
                                }
                            } else {
                                log.error "track directory ${trackDir.getAbsolutePath()} not found"
                                returnObject.put("error", "Track with label '${trackLabel}' removed from config but track directory not found.")
                                // updating extendedTrackList.json
                                def trackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                trackListJsonWriter << extendedTrackListObject.toString(4)
                                trackListJsonWriter.close()
                            }
                            returnObject.put("track", trackObject)
                        }
                    } else {
                        log.error "Track with label '${trackLabel}' not found"
                        returnObject.put("error", "Track with label '${trackLabel}' not found.")
                    }
                } else {
                    // track found in trackList.json
                    log.debug "track with label '${trackLabel}' found in trackList.json"
                    if (organism.dataAddedViaWebServices) {
                        log.debug "organism data was added via web services; thus can remove the track"
                        // track can be deleted since the organism and all subsequent tracks were added via web services
                        trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value).remove(trackObject)
                        String urlTemplate = trackObject.get(FeatureStringEnum.URL_TEMPLATE.value)
                        String trackDirectory = urlTemplate.split("/").first()
                        File trackDir = new File(organism.directory + File.separator + trackDirectory + File.separator + trackObject.get(FeatureStringEnum.LABEL.value))
                        if (trackDir.exists()) {
                            log.debug "Deleting ${trackDir.getAbsolutePath()}"
                            if (trackDir.deleteDir()) {
                                // updating trackList.json
                                def trackListJsonWriter = trackListJsonFile.newWriter()
                                trackListJsonWriter << trackListObject.toString(4)
                                trackListJsonWriter.close()
                            }
                        } else {
                            log.error "track directory ${trackDir.getAbsolutePath()} not found"
                            returnObject.put("error", "Track with label '${trackLabel}' removed from config but track directory not found.")
                            // updating trackList.json
                            def trackListJsonWriter = trackListJsonFile.newWriter()
                            trackListJsonWriter << trackListObject.toString(4)
                            trackListJsonWriter.close()
                        }
                        returnObject.put("track", trackObject)
                    } else {
                        // cannot delete track since its part of the main data directory
                        log.error "Track with label '${trackLabel}' found but is part of the main data directory and cannot be deleted."
                        returnObject.put("error", "Track with label '${trackLabel}' found but is part of the main data directory and cannot be deleted.")
                    }
                }
            } else {
                log.error("Organism not found")
                returnObject.put("error", "Organism not found.")
            }
        } catch (Exception e) {
            log.error(e.message)
            returnObject.put("error", e.message)
        }

        render returnObject as JSON
    }

    @RestApiMethod(description = "Update a track in an existing organism returning a JSON object containing old and new track configurations", path = "/organism/updateTrackForOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
            , @RestApiParam(name = "trackConfig", type = "string", paramType = RestApiParamType.QUERY, description = "Track configuration (JBrowse JSON)")
    ])
    @Transactional
    def updateTrackForOrganism() {

        JSONObject returnObject = new JSONObject()
        JSONObject requestObject = permissionService.handleInput(request, params)


        if (!requestObject.containsKey(FeatureStringEnum.ORGANISM.value)) {
            returnObject.put("error", "/updateTrackForOrganism requires '${FeatureStringEnum.ORGANISM.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        if (!requestObject.containsKey(FeatureStringEnum.TRACK_CONFIG.value)) {
            returnObject.put("error", "/updateTrackForOrganism requires '${FeatureStringEnum.TRACK_CONFIG.value}'.")
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            render returnObject as JSON
            return
        }

        JSONObject trackConfigObject
        try {
            trackConfigObject = JSON.parse(params.get(FeatureStringEnum.TRACK_CONFIG.value)) as JSONObject
        } catch (ConverterException ce) {
            log.error ce.message
            returnObject.put("error", ce.message)
            render returnObject as JSON
            return
        }

        if (!trackConfigObject.containsKey(FeatureStringEnum.LABEL.value) || !trackConfigObject.containsKey(FeatureStringEnum.URL_TEMPLATE.value)) {
            log.error "trackConfig requires both '${FeatureStringEnum.LABEL.value}' and '${FeatureStringEnum.URL_TEMPLATE.value}'."
            returnObject.put("error", "trackConfig requires both '${FeatureStringEnum.LABEL.value}' and '${FeatureStringEnum.URL_TEMPLATE.value}'.")
            render returnObject as JSON
            return
        }

        try {
            permissionService.checkPermissions(requestObject, PermissionEnum.ADMINISTRATE)
            log.debug "user ${requestObject.get(FeatureStringEnum.USERNAME.value)} is admin"
            Organism organism = preferenceService.getOrganismForTokenInDB(requestObject.get(FeatureStringEnum.ORGANISM.value))

            if (organism) {
                String organismDirectoryName = organism.directory
                File organismDirectory = new File(organismDirectoryName)
                File commonDataDirectory = new File(trackService.commonDataDirectory)

                if (organismDirectory.getParentFile().getAbsolutePath() == commonDataDirectory.getAbsolutePath()) {
                    // organism data is in common data directory
                    log.debug "organism data is in common data directory"
                    File trackListJsonFile = new File(organism.directory + File.separator + trackService.TRACKLIST)
                    JSONObject trackListObject = JSON.parse(trackListJsonFile.text)
                    JSONArray tracksArray = trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)
                    // check if track exists in trackList.json
                    JSONObject trackObject = trackService.findTrackFromArray(tracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value))
                    if (trackObject == null) {
                        log.error "Cannot find track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}'"
                        returnObject.put("error", "Cannot find track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}'.")
                    } else {
                        // replaces track config
                        tracksArray.remove(trackObject)
                        tracksArray.add(trackConfigObject)

                        // write to trackList.json
                        def trackListJsonWriter = trackListJsonFile.newWriter()
                        trackListJsonWriter << trackListObject.toString(4)
                        trackListJsonWriter.close()

                        returnObject.put("oldTrackConfig", trackObject)
                        returnObject.put("newTrackConfig", trackConfigObject)
                    }
                } else {
                    // organism data is somewhere on the server where we don't want to modify anything
                    log.debug "organism data is somewhere on the FS"
                    File trackListJsonFile = new File(organism.directory + File.separator + trackService.TRACKLIST)
                    JSONObject trackListObject = JSON.parse(trackListJsonFile.text)
                    JSONArray tracksArray = trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)
                    // check if track exists in trackList.json
                    JSONObject trackObject = trackService.findTrackFromArray(tracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value))
                    if (trackObject != null) {
                        // cannot update track config
                        log.error "Track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' found but is part of the main data directory and cannot be updated."
                        returnObject.put("error", "Track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}' found but is part of the main data directory and cannot be updated.")
                    } else {
                        File extendedDirectory = trackService.getExtendedDataDirectory(organism)
                        if (extendedDirectory.exists()) {
                            // extended organism directory present in common data directory
                            log.debug "extended organism directory ${extendedDirectory.absolutePath} present in common data directory"
                            File extendedTrackListJsonFile = trackService.getExtendedTrackList(organism)
                            JSONObject extendedTrackListObject = JSON.parse(extendedTrackListJsonFile.text)
                            JSONArray extendedTracksArray = extendedTrackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)
                            // check if track exists in extendedTrackList.json
                            trackObject = trackService.findTrackFromArray(extendedTracksArray, trackConfigObject.get(FeatureStringEnum.LABEL.value))
                            if (trackObject == null) {
                                log.error "Cannot find track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}'"
                                returnObject.put("error", "Cannot find track with label '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}'.")
                            } else {
                                // replaces track config
                                extendedTracksArray.remove(trackObject)
                                extendedTracksArray.add(trackConfigObject)

                                // write to trackList.json
                                def extendedTrackListJsonWriter = extendedTrackListJsonFile.newWriter()
                                extendedTrackListJsonWriter << extendedTrackListObject.toString(4)
                                extendedTrackListJsonWriter.close()

                                returnObject.put("oldTrackConfig", trackObject)
                                returnObject.put("newTrackConfig", trackConfigObject)
                            }
                        } else {
                            log.error "Extended organism directory does not exist; Cannot find track with '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}'."
                            returnObject.put("error", "Cannot find track with '${trackConfigObject.get(FeatureStringEnum.LABEL.value)}'.")
                        }
                    }
                }
            } else {
                log.error "Organism not found"
                returnObject.put("error", "Organism not found.")
            }
        } catch (Exception e) {
            log.error e.message
            returnObject.put("error", e.message)
        }

        render returnObject as JSON
    }

    @RestApiMethod(description = "Adds an organism returning a JSON array of all organisms", path = "/organism/addOrganism", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "directory", type = "string", paramType = RestApiParamType.QUERY, description = "Filesystem path for the organisms data directory (required)")
            , @RestApiParam(name = "commonName", type = "string", paramType = RestApiParamType.QUERY, description = "A name used for the organism")
            , @RestApiParam(name = "species", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Species name")
            , @RestApiParam(name = "genus", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Species genus")
            , @RestApiParam(name = "blatdb", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Filesystem path for a BLAT database (e.g. a .2bit file)")
            , @RestApiParam(name = "publicMode", type = "boolean", paramType = RestApiParamType.QUERY, description = "(optional) A flag for whether the organism appears as in the public genomes list (default false)")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) Organism metadata")
            , @RestApiParam(name = "returnAllOrganisms", type = "boolean", paramType = RestApiParamType.QUERY, description = "(optional) Return all organisms (true / false) (default true)")
    ])
    @Transactional
    def addOrganism() {
        JSONObject organismJson = permissionService.handleInput(request, params)
        String clientToken = organismJson.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        try {
            // use permissionService.hasGlobalPermissions to check both authentication and authorization
            if (!permissionService.hasGlobalPermissions(organismJson, GlobalPermissionEnum.INSTRUCTOR)) {
                def error = [error: 'not authorized to add organism']
                render error as JSON
                log.error(error.error)
                return
            }

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
                    , obsolete: false
                    , metadata: organismJson.metadata ? organismJson.metadata.toString() : null
                    , nonDefaultTranslationTable: organismJson.nonDefaultTranslationTable ?: null
                    , publicMode: organismJson.publicMode ?: false
            )
            log.debug "organism ${organism as JSON}"
            // to support webservice, get current user from session or input object
            def currentUser = permissionService.getCurrentUser(organismJson)
            // allow specify the metadata creator through webservice, if not specified, take current user as the creator
            if (!organism.getMetaData(FeatureStringEnum.CREATOR.value)) {
                log.debug "creator does not exist, set current user as the creator"
                organism.addMetaData(FeatureStringEnum.CREATOR.value, currentUser.id as String)
            }

            if (checkOrganism(organism)) {
                organism.save(failOnError: true, flush: true, insert: true)
            }
            def user = permissionService.currentUser
            def userOrganismPermission = UserOrganismPermission.findByUserAndOrganism(user, organism)
            if (!userOrganismPermission) {
                log.debug "creating new permissions! "
                userOrganismPermission = new UserOrganismPermission(
                        user: user
                        , organism: organism
                        , permissions: "[]"
                ).save(insert: true)
                log.debug "created new permissions! "
            }

            JSONArray permissionsArray = new JSONArray()
            permissionsArray.add(PermissionEnum.ADMINISTRATE.name())
            userOrganismPermission.permissions = permissionsArray.toString()
            userOrganismPermission.save(flush: true)

            // send file using:
//            curl \
            //  -F "userid=1" \
            //  -F "filecomment=This is an image file" \
            //  -F "sequenceData=@/home/user1/Desktop/jbrowse/sample/seq.zip" \
            //  localhost:8080/apollo
//                if (request.getFile("sequenceData)")) {
//
//                }W
            sequenceService.loadRefSeqs(organism)

            preferenceService.setCurrentOrganism(permissionService.getCurrentUser(organismJson), organism, clientToken)
            Boolean returnAllOrganisms = organismJson.returnAllOrganisms ? Boolean.valueOf(organismJson.returnAllOrganisms) : true

            render returnAllOrganisms ? findAllOrganisms() : new JSONArray()


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
        }


        if (organism.genomeFasta) {
            File genomeFastaFile = new File(organism.genomeFastaFileName)
            File genomeFastaIndexFile = new File(organism.genomeFastaIndexFileName)
            if (!genomeFastaFile.exists()) {
                organism.valid = false
                throw new Exception("Invalid fasta file : " + genomeFastaFile.absolutePath)
            }
            if (!genomeFastaIndexFile.exists()) {
                organism.valid = false
                throw new Exception("Invalid index fasta file : " + genomeFastaIndexFile.absolutePath)
            }
        } else if (!refSeqFile.exists()) {
            organism.valid = false
            throw new Exception("Reference sequence file does not exists: " + refSeqFile.absolutePath)
        }

        organism.valid = true
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
        try {
            JSONObject organismJson = permissionService.handleInput(request, params)
            permissionService.checkPermissions(organismJson, PermissionEnum.ADMINISTRATE)
            Organism organism = Organism.findById(organismJson.id)
            Boolean madeObsolete
            if (organism) {
                log.debug "Updating organism info ${organismJson as JSON}"
                organism.commonName = organismJson.name
                organism.blatdb = organismJson.blatdb ?: null
                organism.species = organismJson.species ?: null
                organism.genus = organismJson.genus ?: null
                //if the organismJson.metadata is null, remain the old metadata
                organism.metadata = organismJson.metadata ? organismJson.metadata.toString() : organism.metadata
                organism.directory = organismJson.directory
                organism.publicMode = organismJson.publicMode ?: false
                madeObsolete = !organism.obsolete && organismJson.obsolete
                organism.obsolete = organismJson.obsolete ?: false
                organism.nonDefaultTranslationTable = organismJson.nonDefaultTranslationTable ?: null
                if (checkOrganism(organism)) {
                    if (madeObsolete) {
                        // TODO: remove all organism permissions
                        permissionService.removeAllPermissions(organism)
                    }
                    organism.save(flush: true, insert: false, failOnError: true)
                } else {
                    throw new Exception("Bad organism directory: " + organism.directory)
                }

                if (organism.genomeFasta) {
                    // update location of genome fasta
                    sequenceService.updateGenomeFasta(organism)
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

    @RestApiMethod(description = "Get creator metadata for organism, returns userId as String", path = "/organism/getOrganismCreator", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "ID or commonName that can be used to uniquely identify an organism")
    ])
    def getOrganismCreator() {
        JSONObject organismJson = permissionService.handleInput(request, params)
        if (!permissionService.hasGlobalPermissions(organismJson, GlobalPermissionEnum.ADMIN)) {
            def error = [error: 'not authorized to view the metadata']
            log.error(error.error)
            render error as JSON
            return
        }
        Organism organism = preferenceService.getOrganismForTokenInDB(organismJson.organism)
        if (!organism) {
            def error = [error: 'The organism does not exist']
            log.error(error.error)
            render error as JSON
            return
        }
        JSONObject metaData = new JSONObject()
        metaData.creator = organism.getMetaData(FeatureStringEnum.CREATOR.value)
        render metaData as JSON

    }

    @RestApiMethod(description = "Returns a JSON array of all organisms, or optionally, gets information about a specific organism", path = "/organism/findAllOrganisms", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "(optional) ID or commonName that can be used to uniquely identify an organism")
    ])
    def findAllOrganisms() {
        try {
            JSONObject requestObject = permissionService.handleInput(request, params)
            Boolean showPublicOnly = requestObject.showPublicOnly ? Boolean.valueOf(requestObject.showPublicOnly) : false
            Boolean showObsolete = requestObject.showObsolete ? Boolean.valueOf(requestObject.showObsolete) : false
            List<Organism> organismList = []
            if (requestObject.organism) {
                log.debug "finding info for specific organism"
                Organism organism = Organism.findByCommonName(requestObject.organism)
                if (!organism) organism = Organism.findById(requestObject.organism)
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
                //if (permissionService.isAdmin()) {
                if (permissionService.hasGlobalPermissions(requestObject, GlobalPermissionEnum.ADMIN)) {
                    organismList = showObsolete ? Organism.all : Organism.findAllByObsolete(false)
                } else {
                    organismList = permissionService.getOrganismsForCurrentUser(requestObject).filter() { o -> !o.obsolete }
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
                        id                        : organism.id,
                        commonName                : organism.commonName,
                        blatdb                    : organism.blatdb,
                        directory                 : organism.directory,
                        annotationCount           : annotationCount,
                        sequences                 : sequenceCount,
                        genus                     : organism.genus,
                        species                   : organism.species,
                        valid                     : organism.valid,
                        publicMode                : organism.publicMode,
                        obsolete                  : organism.obsolete,
                        nonDefaultTranslationTable: organism.nonDefaultTranslationTable,
                        metadata                  : organism.metadata,
                        currentOrganism           : defaultOrganismId != null ? organism.id == defaultOrganismId : false
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
        OrganismSummary organismSummaryInstance = permissionService.currentUser.roles.first().rank == GlobalPermissionEnum.ADMIN.rank ? reportService.generateAllFeatureSummary() : new OrganismSummary()
//        OrganismSummary organismSummaryInstance = reportService.generateAllFeatureSummary()

//        def organismPermissions = permissionService.getOrganismsWithPermission(permissionService.currentUser)
        def organisms = permissionService.getOrganismsWithMinimumPermission(permissionService.currentUser, PermissionEnum.ADMINISTRATE)


        organisms.each { organism ->
            OrganismSummary thisOrganismSummaryInstance = reportService.generateOrganismSummary(organism)
            organismSummaryListInstance.put(organism, thisOrganismSummaryInstance)
        }


        respond organismSummaryInstance, model: [organismSummaries: organismSummaryListInstance, isSuperAdmin: permissionService.isAdmin()]
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
