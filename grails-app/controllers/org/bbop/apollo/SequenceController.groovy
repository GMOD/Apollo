package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.report.SequenceSummary
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class SequenceController {


    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def featureService
    def transcriptService
    def permissionService
    def preferenceService
    def reportService
    def bookmarkService

    def permissions() {  }


    @Transactional
    def setCurrentSequenceLocation(String name,Integer start, Integer end) {

        try {
            UserOrganismPreference userOrganismPreference = preferenceService.setCurrentSequenceLocation(name, start, end)
            if(params.suppressOutput){
                render new JSONObject() as JSON
            }
            else{
                render userOrganismPreference.bookmark as JSON
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
        log.debug "setting default sequences: ${params}"
        Organism organism = sequenceInstance.organism

        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser, organism)

        Bookmark bookmark = bookmarkService.generateBookmarkForSequence(sequenceInstance)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , organism: organism
                    , bookmark: bookmark
                    , currentOrganism: true
            ).save(insert: true, flush: true, failOnError: true)
        } else {
            userOrganismPreference.bookmark = bookmark
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, failOnError: true)
        }
        preferenceService.setOtherCurrentOrganismsFalse(userOrganismPreference, currentUser)

        Session session = SecurityUtils.subject.getSession(false)
        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, sequenceInstance.name)
//        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequenceInstance.name)
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, bookmark.sequenceList.toString())
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequenceInstance.organismId)


        render userOrganismPreference.bookmark.sequenceList
    }


    @Transactional
    def loadSequences(Organism organism) {
        println "Loading sequences ${organism.commonName}"
        if (!organism.sequences) {
            sequenceService.loadRefSeqs(organism)
        }

        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser, organism)
        if (userOrganismPreference?.bookmark) {
            userOrganismPreference.currentOrganism = true
            request.session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, userOrganismPreference.bookmark.sequenceList.toString())
            userOrganismPreference.save(flush: true)
        } else {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: Bookmark.findByOrganism(organism)
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


    def lookupSequenceByName(String q) {
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
        def sequences = Sequence.findAllByNameIlikeAndOrganism(q + "%", organism, ["sort": "name", "order": "asc", "max": 20]).collect() {
            it.name
        }
        render sequences as JSON
    }
    def lookupSequenceByNameAndOrganism() {
        JSONObject j;
        for(k in params) {
            j=JSON.parse(k.key)
            break;
        }
        def organism
        if(!j.name||!j.organism) {
            organism = preferenceService.getCurrentOrganismForCurrentUser()
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
    def getSequences(String name, Integer start, Integer length, String sort, Boolean asc, Integer minFeatureLength, Integer maxFeatureLength) {
        try {
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
            minFeatureLength = minFeatureLength ?: 0
            maxFeatureLength = maxFeatureLength ?: Integer.MAX_VALUE
            List<Sequence> sequences
            def sequenceCount = Sequence.countByOrganismAndNameIlikeAndLengthGreaterThanEqualsAndLengthLessThanEquals(organism, "%${name}%", minFeatureLength, maxFeatureLength )
            sequences = Sequence.findAllByOrganismAndNameIlikeAndLengthGreaterThanEqualsAndLengthLessThanEquals(organism, "%${name}%", minFeatureLength, maxFeatureLength, [offset: start, max: length, sort: sort, order: asc ? "asc" : "desc"])
            JSONArray returnSequences = JSON.parse( (sequences as JSON).toString()) as JSONArray

            for(int i = 0 ; i < returnSequences.size() ; i++){
                returnSequences.getJSONObject(i).put("sequenceCount",sequenceCount)
            }

            render returnSequences as JSON
        }
        catch(PermissionException e) {
            def error=[error: "Error: "+e]
            render error as JSON
        }
    }

    def report(Organism organism,Integer max) {
        if (!permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)) {
            redirect(uri: "/auth/unauthorized")
            return
        }
        organism = organism ?: Organism.first()
        params.max = Math.min(max ?: 20, 100)

        List<SequenceSummary> sequenceInstanceList = new ArrayList<>()
        List<Sequence> sequences = Sequence.findAllByOrganism(organism,params)

        sequences.each {
            sequenceInstanceList.add(reportService.generateSequenceSummary(it))
        }
        println "sequence summary list size: ${sequenceInstanceList.size()}"

        int sequenceInstanceCount = Sequence.countByOrganism(organism)
        render view:"report", model:[sequenceInstanceList:sequenceInstanceList,organism:organism,sequenceInstanceCount:sequenceInstanceCount]
    }


}
