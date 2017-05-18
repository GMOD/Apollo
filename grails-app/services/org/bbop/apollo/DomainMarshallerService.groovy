package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.bbop.apollo.gwt.shared.projection.Coordinate

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

        JSON.registerObjectMarshaller(ProjectionSequence) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['organism'] = it.organism
            returnArray['order'] = it.order
            returnArray['projectedOffset'] = it.projectedOffset
            returnArray['originalOffset'] = it.originalOffset
            returnArray['features'] = it.features?.join("::")
            return returnArray
        }

        JSON.registerObjectMarshaller(Coordinate) {
            def returnArray = [:]
            returnArray['min'] = it.min
            returnArray['max'] = it.max
            return returnArray
        }

        JSON.registerObjectMarshaller(Assemblage) {
            def returnArray = [:]
            returnArray['id'] = it?.id
            returnArray['payload'] = it?.payload ?: "{}"
            returnArray['name'] = it?.name
            returnArray['start'] = it?.start
            returnArray['end'] = it?.end
            returnArray['sequenceList'] = it?.sequenceList
            return returnArray
        }

        JSON.registerObjectMarshaller(UserOrganismPreference) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['organism'] = it.organism
            returnArray['currentOrganism'] = it.currentOrganism
            returnArray['nativeTrackList'] = it?.nativeTrackList
            returnArray['assemblage'] = it?.assemblage
            returnArray['startbp'] = it?.startbp
            returnArray['endbp'] = it?.endbp
            return returnArray
        }
    }
}
