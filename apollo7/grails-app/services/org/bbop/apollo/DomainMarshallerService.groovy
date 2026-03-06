package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.bbop.apollo.go.GoAnnotation
import org.bbop.apollo.preference.OrganismDTO
import org.bbop.apollo.preference.SequenceDTO
import org.bbop.apollo.preference.UserOrganismPreferenceDTO

@Transactional
class DomainMarshallerService {

    def registerObjects() {

        JSON.registerObjectMarshaller(User) {
            def returnArray = [:]
            returnArray['userId'] = it.id
            returnArray['username'] = it.username
            returnArray['firstName'] = it.firstName
            returnArray['lastName'] = it.lastName
            return returnArray
        }

        JSON.registerObjectMarshaller(Organism) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['commonName'] = it.commonName
            returnArray['genus'] = it?.genus
            returnArray['species'] = it?.species
            returnArray['directory'] = it.directory
            returnArray['metadata'] = it?.metadata
            returnArray['officialGeneSetTrack'] = it?.officialGeneSetTrack
          return returnArray
        }

        JSON.registerObjectMarshaller(OrganismDTO) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['commonName'] = it.commonName
            returnArray['directory'] = it.directory
            returnArray['annotationCount'] = it.annotationCount
            returnArray['variantEffectCount'] = it.variantEffectCount
            returnArray['officialGeneSetTrack'] = it?.officialGeneSetTrack
            return returnArray
        }

        JSON.registerObjectMarshaller(Sequence) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['length'] = it?.length
            returnArray['start'] = it?.start
            returnArray['end'] = it.end
            return returnArray
        }

      JSON.registerObjectMarshaller(GoAnnotation) {
        def returnArray = [:]
        returnArray['id'] = it.id
        returnArray['aspect'] = it.aspect
        returnArray['feature'] = it.feature.id
        returnArray['goRef'] = it.goRef
        returnArray['evidenceRef'] = it.evidenceRef
        returnArray['goRefLabel'] = it.goRefLabel
        returnArray['evidenceRefLabel'] = it.evidenceRefLabel
        returnArray['geneProductRelationshipRef'] = it.geneProductRelationshipRef
        returnArray['negate'] = it.negate
        returnArray['withOrFromArray'] = it.withOrFromArray
        returnArray['notesArray'] = it.notesArray
        returnArray['reference'] = it.reference
        returnArray['lastUpdated'] = it.lastUpdated
        returnArray['dateCreated'] = it.dateCreated
        return returnArray
      }

      JSON.registerObjectMarshaller(SequenceDTO) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['organism'] = it.organism
            returnArray['length'] = it?.length
            returnArray['start'] = it?.start
            returnArray['end'] = it.end
            return returnArray
        }

        JSON.registerObjectMarshaller(UserOrganismPreference) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['organism'] = it.organism
            returnArray['currentOrganism'] = it.currentOrganism
            returnArray['nativeTrackList'] = it?.nativeTrackList
            returnArray['sequence'] = it?.sequence
            returnArray['startbp'] = it?.startbp
            returnArray['endbp'] = it?.endbp
            return returnArray
        }

        JSON.registerObjectMarshaller(UserOrganismPreferenceDTO) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['organism'] = it.organism
            returnArray['currentOrganism'] = it.currentOrganism
            returnArray['nativeTrackList'] = it?.nativeTrackList
            returnArray['sequence'] = it?.sequence
            returnArray['startbp'] = it?.startbp
            returnArray['endbp'] = it?.endbp
            returnArray['clientToken'] = it?.clientToken
            returnArray['user'] = it?.user
            return returnArray
        }
    }
}
