package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ExonService)
@Mock([Exon,FeatureLocation,MRNA,FeatureRelationship,Sequence])
class ExonServiceSpec extends Specification {

}
