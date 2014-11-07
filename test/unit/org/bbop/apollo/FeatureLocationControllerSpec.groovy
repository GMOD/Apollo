package org.bbop.apollo



import grails.test.mixin.*
import spock.lang.*

@TestFor(FeatureLocationController)
@Mock([FeatureLocation,Sequence,Feature])
class FeatureLocationControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        Sequence sequence = new Sequence(
//                organism: organism
                length: 3
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "asdf"
                ,seqChunkSize: 3
                ,start: 5
                ,end: 8
                ,dataDirectory: "adsfads"
                ,sequenceDirectory: "asdfadsf"
                ,name: "chromosome7"
        ).save(failOnError: true)

        Feature feature = new Feature(
                name: "abc123"
        ).save(failOnError: true)

        params["feature"] = feature
        params["fmin"] = 12
        params["fmax"] = 33
        params["sequence"] = sequence


    }

    void "Test the index action returns the correct model"() {

        when:"The index action is executed"
            controller.index()

        then:"The model is correct"
            !model.featureLocationInstanceList
            model.featureLocationInstanceCount == 0
    }

    void "Test the create action returns the correct model"() {
        when:"The create action is executed"
            controller.create()

        then:"The model is correctly created"
            model.featureLocationInstance!= null
    }

    void "Test the save action correctly persists an instance"() {

        when:"The save action is executed with an invalid instance"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'POST'
            def featureLocation = new FeatureLocation()
            featureLocation.validate()
            controller.save(featureLocation)

        then:"The create view is rendered again with the correct model"
            model.featureLocationInstance!= null
            view == 'create'

        when:"The save action is executed with a valid instance"
            response.reset()
            populateValidParams(params)
            featureLocation = new FeatureLocation(params)

            controller.save(featureLocation)

        then:"A redirect is issued to the show action"
            response.redirectedUrl == '/featureLocation/show/1'
            controller.flash.message != null
            FeatureLocation.count() == 1
    }

    void "Test that the show action returns the correct model"() {
        when:"The show action is executed with a null domain"
            controller.show(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the show action"
            populateValidParams(params)
            def featureLocation = new FeatureLocation(params)
            controller.show(featureLocation)

        then:"A model is populated containing the domain instance"
            model.featureLocationInstance == featureLocation
    }

    void "Test that the edit action returns the correct model"() {
        when:"The edit action is executed with a null domain"
            controller.edit(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the edit action"
            populateValidParams(params)
            def featureLocation = new FeatureLocation(params)
            controller.edit(featureLocation)

        then:"A model is populated containing the domain instance"
            model.featureLocationInstance == featureLocation
    }

    void "Test the update action performs an update on a valid domain instance"() {
        when:"Update is called for a domain instance that doesn't exist"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'PUT'
            controller.update(null)

        then:"A 404 error is returned"
            response.redirectedUrl == '/featureLocation/index'
            flash.message != null


        when:"An invalid domain instance is passed to the update action"
            response.reset()
            def featureLocation = new FeatureLocation()
            featureLocation.validate()
            controller.update(featureLocation)

        then:"The edit view is rendered again with the invalid instance"
            view == 'edit'
            model.featureLocationInstance == featureLocation

        when:"A valid domain instance is passed to the update action"
            response.reset()
            populateValidParams(params)
            featureLocation = new FeatureLocation(params).save(flush: true)
            controller.update(featureLocation)

        then:"A redirect is issues to the show action"
            response.redirectedUrl == "/featureLocation/show/$featureLocation.id"
            flash.message != null
    }

    void "Test that the delete action deletes an instance if it exists"() {
        when:"The delete action is called for a null instance"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'DELETE'
            controller.delete(null)

        then:"A 404 is returned"
            response.redirectedUrl == '/featureLocation/index'
            flash.message != null

        when:"A domain instance is created"
            response.reset()
            populateValidParams(params)
            def featureLocation = new FeatureLocation(params).save(flush: true)

        then:"It exists"
            FeatureLocation.count() == 1

        when:"The domain instance is passed to the delete action"
            controller.delete(featureLocation)

        then:"The instance is deleted"
            FeatureLocation.count() == 0
            response.redirectedUrl == '/featureLocation/index'
            flash.message != null
    }
}
