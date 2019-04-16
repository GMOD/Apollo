package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject

import java.security.MessageDigest
import java.sql.Timestamp

/**
 *
 * Chado Compliance Layers
 * Level 0: Relational schema - this basically means that the schema is adhered to
 * Level 1: Ontologies - this means that all features in the feature table are of a type represented in SO and
 * all feature relationships in feature_relationship table must be SO relationship types
 * Level 2: Graph - all features relationships between a feature of type X and Y must correspond to relationship of
 * that type in SO.
 *
 * Relevant Chado modules:
 * Chado General Module
 * Chado CV Module
 * Chado Organism Module
 * Chado Sequence Module
 * Chado Publication Module
 *
 */

@Transactional
class JbrowseHandlerService {

    def configWrapperService
    def sequenceService
    def featureRelationshipService
    def transcriptService
    def cdsService

    /**
     * Track only
     * @param organism
     * @return
     */
    def writeTrackOnly(Organism organism) {
        JSONObject returnObject = new JSONObject()
        println "writing track only"
        return returnObject
    }

    /**
     * Directory only
     * @param organism
     * @return
     */
    def writeJBrowseDirectory(Organism organism) {
        JSONObject returnObject = new JSONObject()
        println "writing track only"
        return returnObject
    }

    /**
     * Full JBrowse
     * @param organism
     * @return
     */
    def writeExportToThisOrganism(Organism organism) {
        JSONObject returnObject = new JSONObject()
        return returnObject
    }

}
