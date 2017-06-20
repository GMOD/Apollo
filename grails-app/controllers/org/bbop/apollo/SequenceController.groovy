package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.report.SequenceSummary
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import static org.springframework.http.HttpStatus.*

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
    def assemblageService

    def permissions() {  }


    @Transactional
    def setCurrentSequenceLocation(String name,Integer start, Integer end) {

        JSONObject inputObject = permissionService.handleInput(request,params)

        try {
            UserOrganismPreference userOrganismPreference = preferenceService.setCurrentSequenceLocation(name, start, end,inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            if(params.suppressOutput){
                render new JSONObject() as JSON
            }
            else{
                render userOrganismPreference.assemblage as JSON
            }
        } catch (NumberFormatException e) {
            //  we can ignore this specific exception as null is an acceptable value for start / end
        }
        catch (Exception e) {
            def error=[error: e.message]
            log.error e.message
            render error as JSON
        }
    }

    @Transactional
    def setCurrentSequenceForNameAndOrganism(Organism organism) {
        JSONObject inputObject = permissionService.handleInput(request,params)
        Sequence sequence = Sequence.findByNameAndOrganism(inputObject.sequenceName,organism)
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
        JSONObject inputObject = permissionService.handleInput(request,params)
        String token = inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        Organism organism = sequenceInstance.organism

        User currentUser = permissionService.currentUser
        preferenceService.setCurrentSequence(currentUser,sequenceInstance,token)

        Session session = SecurityUtils.subject.getSession(false)
        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, sequenceInstance.name)
//        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequenceInstance.name)
        Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequenceInstance)
        String assemblageString = assemblageService.convertAssemblageToJson(assemblage).toString()
//        JSONArray sequenceArray = (JSON.parse(assemblage.sequenceList)) as JSONArray
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, assemblage.name)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequenceInstance.organismId)

        JSONObject sequenceObject = new JSONObject()
        sequenceObject.put("id", sequenceInstance.id)
        sequenceObject.put("name", sequenceInstance.name)
        sequenceObject.put("length", sequenceInstance.length)
        sequenceObject.put("start", sequenceInstance.start)
        sequenceObject.put("end", sequenceInstance.end)
        UserOrganismPreference userOrganismPreference = preferenceService.getCurrentOrganismPreference(currentUser,sequenceInstance.name,token)
        sequenceObject.startBp = userOrganismPreference.startbp
        sequenceObject.endBp = userOrganismPreference.endbp

//        render userOrganismPreference.assemblage.sequenceList
//        render sequenceObject as JSON
        render assemblageString
    }


    @Transactional
    def loadSequences(Organism organism) {
        if (!organism.sequences) {
            sequenceService.loadRefSeqs(organism)
        }

        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser, organism)
        if (userOrganismPreference?.assemblage) {
            userOrganismPreference.currentOrganism = true
            request.session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, userOrganismPreference.assemblage.sequenceList.toString())
            userOrganismPreference.save(flush: true)
        } else {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , organism: organism
                    , currentOrganism: true
                    , assemblage: Assemblage.findByOrganism(organism)
            ).save(insert: true, flush: true)
        }
        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false where pref.id != :prefId ", [prefId: userOrganismPreference.id])

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
    def lookupSequenceByName(String q,String clientToken) {
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
        for(k in params) {
            j=JSON.parse(k.key)
            break;
        }
        def organism
        if(!j.name||!j.organism) {
            organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        else {
            organism=Organism.findById(j.organism)
        }
        def seqid=j.name
        def sequenceId = Sequence.findByNameAndOrganism(seqid,organism).id
        JSONObject jsonObject = new JSONObject()
        jsonObject.put(FeatureStringEnum.ID.value,sequenceId)
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.value,organism.id)
        render jsonObject as JSON
    }

    @Transactional
    def getSequences(String name, Integer start, Integer length, String sort, Boolean asc, Integer minFeatureLength, Integer maxFeatureLength,String clientToken) {
        try {
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)

            if(!organism) {
                render ([] as JSON)
                return
            }
            def sequences = Sequence.createCriteria().list() {
                if(name) {
                    ilike('name', '%'+name+'%')
                }
                eq('organism',organism)
                gt('length',minFeatureLength ?: 0)
                lt('length',maxFeatureLength ?: Integer.MAX_VALUE)
                if(sort=="length") {
                    order('length',asc?"asc":"desc")
                }
                if(sort=="name") {
                    order('name', asc?"asc":"desc")
                }
            }
            def sequenceCounts = Feature.executeQuery("select fl.sequence.name, count(fl.sequence) from Feature f join f.featureLocations fl where fl.sequence.organism = :organism and fl.sequence.length < :maxFeatureLength and fl.sequence.length > :minFeatureLength and f.class in :viewableAnnotationList group by fl.sequence.name", [minFeatureLength: minFeatureLength ?: 0, maxFeatureLength: maxFeatureLength ?: Integer.MAX_VALUE, viewableAnnotationList: requestHandlingService.viewableAnnotationList, organism: organism])
            def map = [:]
            sequenceCounts.each {
                map[it[0]] = it[1]
            }
            def results = sequences.collect { s ->
                [id: s.id, length: s.length, start: s.start, end: s.end, count: map[s.name]?:0, name: s.name, sequenceCount: sequences.size()]
            } 
            if(sort=="count") {
                results = results.sort { it.count }
                if(!asc) {
                    results = results.reverse()
                }
            }
            render results ? results[start..Math.min(start+length-1,results.size()-1)] as JSON: new JSONObject() as JSON
        }
        catch(PermissionException e) {
            def error=[error: "Error: "+e]
            render error as JSON
        }
    }

    /**
     * Permissions handled upstream
     * @param organism
     * @param max
     * @return
     */
    def report(Organism organism,Integer max) {
        organism = organism ?: Organism.first()
        params.max = Math.min(max ?: 20, 100)

        List<SequenceSummary> sequenceInstanceList = new ArrayList<>()
        List<Sequence> sequences = Sequence.findAllByOrganism(organism,params)

        sequences.each {
            sequenceInstanceList.add(reportService.generateSequenceSummary(it))
        }

        int sequenceInstanceCount = Sequence.countByOrganism(organism)
        render view:"report", model:[sequenceInstanceList:sequenceInstanceList,organism:organism,sequenceInstanceCount:sequenceInstanceCount]
    }

    /**
     * GET (base)/features/(refseq_name)?start=234&end=5678
     * http://gmod.org/wiki/JBrowse_Configuration_Guide#JBrowse_REST_Feature_Store_API
     *
     *
     * {
*     "features": [
*    { "start": 123, "end": 456 }', // minimal
*    { "start": 123, "end": 456, "score": 42 }, // required
*    {"seq": "gattacagattaca", "start": 0, "end": 14}, // seq
     *
*    { "type": "mRNA", "start": 5975, "end": 9744, "score": 0.84, "strand": 1,
*        "name": "au9.g1002.t1", "uniqueID": "globallyUniqueString3",
*        "subfeatures": [
     *                 { "type": "five_prime_UTR", "start": 5975, "end": 6109, "score": 0.98, "strand": 1 },
     *
     *
     */
    def features(){
        println "features params: ${params}"
//        println "id: ${id}"
//        println "start: ${start}"
//        println "end: ${end}"

        JSONObject features1 = new JSONObject(start:123,end:456,name:"region1",type:"MRNA",label:"first label",Id:"abc123",unique_name:"def567")
        JSONObject features2 = new JSONObject(start:789,end:1012,name:"region2")

        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer(features1,features2)



        render jsonObject
    }


    def regionFeatureDensities(){
        println "regionFeatureDensities params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    def statsGlobal(){
        println "stats global params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    def statsRegion(){
        println "stats region params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }
}
