package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.projection.ProjectionSequence

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
            returnArray['offset'] = it.offset
            returnArray['originalOffset'] = it.originalOffset
            returnArray['features'] = it.features?.join("::")
            return returnArray
        }

        JSON.registerObjectMarshaller(Bookmark) {
            def returnArray = [:]
            returnArray['id'] = it?.id
            returnArray['projection'] = it?.projection ?: "NONE"
            returnArray['padding'] = it?.padding ?: 0
            returnArray['payload'] = it?.payload ?: "{}"
            returnArray['start'] = it?.start
            returnArray['end'] = it?.end
            returnArray['sequenceList'] = it?.sequenceList
            return returnArray
        }
    }
}
