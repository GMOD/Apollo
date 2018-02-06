package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
import org.bbop.apollo.report.SequenceSummary
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import javax.servlet.http.HttpServletResponse

import static org.springframework.http.HttpStatus.NOT_FOUND

@RestApi(name = "Sequence Services", description = "Methods for retrieving sequence data")
@Transactional(readOnly = true)
class SequenceController {


    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def featureService
    def requestHandlingService
    def transcriptService
    def permissionService
    def preferenceService
    def reportService

    def permissions() {}

    def beforeInterceptor = {
        if (params.action == "sequenceByName"
                || params.action == "sequenceByLocation"
        ) {
            response.setHeader("Access-Control-Allow-Origin", "*")
        }
    }

    @Transactional
    def setCurrentSequenceLocation(String name, Integer start, Integer end) {

        try {
            UserOrganismPreferenceDTO userOrganismPreference = preferenceService.setCurrentSequenceLocation(name, start, end, params[FeatureStringEnum.CLIENT_TOKEN.value].toString())
            if (params.suppressOutput) {
                render new JSONObject() as JSON
            } else {
                render userOrganismPreference.sequence as JSON
            }
        } catch (NumberFormatException e) {
            //  we can ignore this specific exception as null is an acceptable value for start / end
        }
        catch (Exception e) {
            def error = [error: e.message]
            log.error e.message
            render error as JSON
        }
    }

    @Transactional
    def setCurrentSequenceForNameAndOrganism(Organism organism) {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Sequence sequence = Sequence.findByNameAndOrganism(inputObject.sequenceName, organism)
        setCurrentSequence(sequence)
    }

    /**
     * ID is the organism ID
     * Sequence is the default sequence name
     *
     * If no sequence name is set, pull the preferences, otherwise just choose a random one.
     * @param id
     * @param sequenceName
     * @return
     */

    @Transactional
    def setCurrentSequence(Sequence sequenceInstance) {
        JSONObject inputObject = permissionService.handleInput(request, params)
        String token = inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        Organism organism = sequenceInstance.organism

        User currentUser = permissionService.currentUser
        UserOrganismPreferenceDTO userOrganismPreference = preferenceService.setCurrentSequence(currentUser, sequenceInstance, token)

        Session session = SecurityUtils.subject.getSession(false)
        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, sequenceInstance.name)
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequenceInstance.name)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequenceInstance.organismId)

        JSONObject sequenceObject = new JSONObject()
        sequenceObject.put("id", sequenceInstance.id)
        sequenceObject.put("name", sequenceInstance.name)
        sequenceObject.put("length", sequenceInstance.length)
        sequenceObject.put("start", sequenceInstance.start)
        sequenceObject.put("end", sequenceInstance.end)
        sequenceObject.startBp = userOrganismPreference.startbp
        sequenceObject.endBp = userOrganismPreference.endbp

        render sequenceObject as JSON
    }


    @Transactional
    def loadSequences(Organism organism) {
        if (!organism.sequences) {
            sequenceService.loadRefSeqs(organism)
        }

        JSONArray sequenceArray = new JSONArray()
        for (Sequence sequence in organism.sequences) {
            JSONObject jsonObject = new JSONObject()
            jsonObject.put("id", sequence.id)
            jsonObject.put("name", sequence.name)
            jsonObject.put("length", sequence.length)
            jsonObject.put("start", sequence.start)
            jsonObject.put("end", sequence.end)
            sequenceArray.put(jsonObject)
        }

        render sequenceArray as JSON
    }


    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'sequence.label', default: 'Sequence'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }


    @Transactional
    def lookupSequenceByName(String q, String clientToken) {
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        def sequences = Sequence.findAllByNameIlikeAndOrganism(q + "%", organism, ["sort": "name", "order": "asc", "max": 20]).collect() {
            it.name
        }
        render sequences as JSON
    }

    /**
     * @deprecated TODO: will be removed as standalone will likely not be supported in the future.
     * @return
     */
    def lookupSequenceByNameAndOrganism(String clientToken) {
        JSONObject j;
        for (k in params) {
            j = JSON.parse(k.key)
            break;
        }
        def organism
        if (!j.name || !j.organism) {
            organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        } else {
            organism = Organism.findById(j.organism)
        }
        def seqid = j.name
        def sequenceId = Sequence.findByNameAndOrganism(seqid, organism).id
        JSONObject jsonObject = new JSONObject()
        jsonObject.put(FeatureStringEnum.ID.value, sequenceId)
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.value, organism.id)
        render jsonObject as JSON
    }

    @Transactional
    def getSequences(String name, Integer start, Integer length, String sort, Boolean asc, Integer minFeatureLength, Integer maxFeatureLength, String clientToken) {
        try {
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)

            if (!organism) {
                render([] as JSON)
                return
            }
            def sequences = Sequence.createCriteria().list() {
                if (name) {
                    ilike('name', '%' + name + '%')
                }
                eq('organism', organism)
                gt('length', minFeatureLength ?: 0)
                lt('length', maxFeatureLength ?: Integer.MAX_VALUE)
                if (sort == "length") {
                    order('length', asc ? "asc" : "desc")
                }
                if (sort == "name") {
                    order('name', asc ? "asc" : "desc")
                }
            }
            def sequenceCounts = Feature.executeQuery("select fl.sequence.name, count(fl.sequence) from Feature f join f.featureLocations fl where fl.sequence.organism = :organism and fl.sequence.length < :maxFeatureLength and fl.sequence.length > :minFeatureLength and f.class in :viewableAnnotationList group by fl.sequence.name", [minFeatureLength: minFeatureLength ?: 0, maxFeatureLength: maxFeatureLength ?: Integer.MAX_VALUE, viewableAnnotationList: requestHandlingService.viewableAnnotationList, organism: organism])
            def map = [:]
            sequenceCounts.each {
                map[it[0]] = it[1]
            }
            def results = sequences.collect { s ->
                [id: s.id, length: s.length, start: s.start, end: s.end, count: map[s.name] ?: 0, name: s.name, sequenceCount: sequences.size()]
            }
            if (sort == "count") {
                results = results.sort { it.count }
                if (!asc) {
                    results = results.reverse()
                }
            }
            render results ? results[start..Math.min(start + length - 1, results.size() - 1)] as JSON : new JSONObject() as JSON
        }
        catch (PermissionException e) {
            def error = [error: "Error: " + e]
            render error as JSON
        }
    }

    /**
     * Permissions handled upstream
     * @param organism
     * @param max
     * @return
     */
    def report(Organism organism, Integer max) {
        organism = organism ?: Organism.first()
        params.max = Math.min(max ?: 20, 100)

        List<SequenceSummary> sequenceInstanceList = new ArrayList<>()
        List<Sequence> sequences = Sequence.findAllByOrganism(organism, params)

        sequences.each {
            sequenceInstanceList.add(reportService.generateSequenceSummary(it))
        }

        def organisms = permissionService.getOrganismsWithMinimumPermission(permissionService.currentUser,PermissionEnum.ADMINISTRATE)

        int sequenceInstanceCount = Sequence.countByOrganism(organism)
        render view: "report", model: [sequenceInstanceList: sequenceInstanceList, organisms: organisms, organism: organism, sequenceInstanceCount: sequenceInstanceCount]
    }

    @RestApiMethod(description = "Get sequence data within a range", path = "/sequence/<organism name>/<sequence name>:<fmin>..<fmax>?ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "sequenceName", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range(required)")
            , @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range (required)")
            , @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false).  Use cache for request if available.")
    ])
    @Transactional
    String sequenceByLocation(String organismString, String sequenceName, int fmin, int fmax) {

        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        Map paramMap = new TreeMap<>()

        if (!ignoreCache) {
            String responseString = sequenceService.checkCache(organismString, sequenceName, fmin, fmax,  paramMap)
            if (responseString) {
                render responseString
                return
            }
        }

        Organism organism = Organism.findByCommonName(organismString) ?: Organism.findById(organismString as Long)
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)

        Strand strand = params.strand ? Strand.getStrandForValue(params.strand as Integer) : Strand.POSITIVE
        String sequenceString = sequenceService.getGenomicResiduesFromSequenceWithAlterations(sequence, fmin, fmax, strand)
        sequenceService.cacheRequest(sequenceString, organismString, sequenceName, fmin, fmax,  paramMap)
        render sequenceString

    }

    @RestApiMethod(description = "Get sequence data as for a selected name", path = "/sequence/<organism name>/<sequence name>/<feature name>.<type>?ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID (required)")
            , @RestApiParam(name = "sequenceName", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name (required)")
            , @RestApiParam(name = "featureName", type = "string", paramType = RestApiParamType.QUERY, description = "The uniqueName (UUID) or given name of the feature (typically transcript) of the element to retrieve sequence from")
            , @RestApiParam(name = "type", type = "string", paramType = RestApiParamType.QUERY, description = "(default genomic) Return type: genomic, cds, cdna, peptide")
            , @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false).  Use cache for request if available.")
    ])
    @Transactional
    String sequenceByName(String organismString, String sequenceName, String featureName, String type) {

        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        Map paramMap = new TreeMap<>()
        paramMap.put("name", featureName)


        if (!ignoreCache) {
            String responseString = sequenceService.checkCache(organismString, sequenceName, featureName, type, paramMap)
            if (responseString) {
                render responseString
                return
            }
        }

        Feature feature = Feature.findByUniqueName(featureName)
        if (!feature) {
            def features = Feature.findAllByName(featureName)

            for (int i = 0; i < features.size() && !feature; i++) {
                Feature f = features.get(i)
                Sequence s = f.featureLocation.sequence
                if (f.featureLocation.sequence.name == sequenceName
                        && (s.organism.commonName == organismString || s.organism.id == organismString)
                ) {
                    feature = f
                }
            }
        }

        if (feature) {
            String sequenceString = sequenceService.getSequenceForFeature(feature, type)
            if(sequenceString?.trim()){
                render sequenceString
                sequenceService.cacheRequest(sequenceString, organismString, sequenceName, featureName, type, paramMap)
                return
            }
        }
        response.status = 404
    }

    @RestApiMethod(description = "Remove sequence cache for an organism and sequence", path = "/sequence/cache/clear/<organism name>/<sequence name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismName", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name (required)")
            , @RestApiParam(name = "sequenceName", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name (required)")
    ])
    @Transactional
    def clearSequenceCache(String organismName, String sequenceName) {
        if (!checkPermission(organismName)) return
        int removed = SequenceCache.countByOrganismNameAndSequenceName(organismName, sequenceName)
        SequenceCache.deleteAll(SequenceCache.findAllByOrganismNameAndSequenceName(organismName, sequenceName))
        render new JSONObject(removed: removed) as JSON
    }

    @RestApiMethod(description = "Remove sequence cache for an organism", path = "/sequence/cache/clear/<organism name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismName", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name (required) or 'ALL' if admin")
    ])
    @Transactional
    def clearOrganismCache(String organismName) {
        if (organismName.toLowerCase().equals("all") && permissionService.isAdmin()) {
            log.info "Deleting cache for all organisms"
            JSONArray jsonArray = new JSONArray()
            Organism.all.each { organism ->
                int removed = SequenceCache.countByOrganismName(organism.commonName)
                SequenceCache.deleteAll(SequenceCache.findAllByOrganismName(organism.commonName))
                JSONObject jsonObject = new JSONObject(name: organism.commonName, removed: removed) as JSONObject
                jsonArray.add(jsonObject)
            }

            render jsonArray as JSON
        } else {
            log.info "Deleting cache for ${organismName}"
            if (!checkPermission(organismName)) return
            int removed = SequenceCache.countByOrganismName(organismName)
            SequenceCache.deleteAll(SequenceCache.findAllByOrganismName(organismName))
            render new JSONObject(removed: removed) as JSON
        }

    }

    def checkPermission(String organismString) {
        Organism organism = preferenceService.getOrganismForToken(organismString)
        if (organism.publicMode || permissionService.checkPermissions(PermissionEnum.READ)) {
            return true
        } else {
            // not accessible to the public
            response.status = HttpServletResponse.SC_FORBIDDEN
            render ""
            return false
        }

    }
}
