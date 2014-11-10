package org.bbop.apollo



import grails.test.mixin.*
import spock.lang.*

@TestFor(GeneController)
@Mock(Gene)
class GeneControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        params["name"] = 'someValidName'
    }

    void "Test the index action returns the correct model"() {

        when:"The index action is executed"
            controller.index()

        then:"The model is correct"
            !model.geneInstanceList
            model.geneInstanceCount == 0
    }

    void "Test the create action returns the correct model"() {
        when:"The create action is executed"
            controller.create()

        then:"The model is correctly created"
            model.geneInstance!= null
    }

    void "Test the save action correctly persists an instance"() {

        when:"The save action is executed with an invalid instance"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'POST'
            def gene = new Gene()
            gene.validate()
            controller.save(gene)

        then:"The create view is rendered again with the correct model"
            model.geneInstance!= null
            view == 'create'

        when:"The save action is executed with a valid instance"
            response.reset()
            populateValidParams(params)
            gene = new Gene(params)

            controller.save(gene)

        then:"A redirect is issued to the show action"
            response.redirectedUrl == '/gene/show/1'
            controller.flash.message != null
            Gene.count() == 1
    }

    void "Test that the show action returns the correct model"() {
        when:"The show action is executed with a null domain"
            controller.show(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the show action"
            populateValidParams(params)
            def gene = new Gene(params)
            controller.show(gene)

        then:"A model is populated containing the domain instance"
            model.geneInstance == gene
    }

    void "Test that the edit action returns the correct model"() {
        when:"The edit action is executed with a null domain"
            controller.edit(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the edit action"
            populateValidParams(params)
            def gene = new Gene(params)
            controller.edit(gene)

        then:"A model is populated containing the domain instance"
            model.geneInstance == gene
    }

    void "Test the update action performs an update on a valid domain instance"() {
        when:"Update is called for a domain instance that doesn't exist"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'PUT'
            controller.update(null)

        then:"A 404 error is returned"
            response.redirectedUrl == '/gene/index'
            flash.message != null


        when:"An invalid domain instance is passed to the update action"
            response.reset()
            def gene = new Gene()
            gene.validate()
            controller.update(gene)

        then:"The edit view is rendered again with the invalid instance"
            view == 'edit'
            model.geneInstance == gene

        when:"A valid domain instance is passed to the update action"
            response.reset()
            populateValidParams(params)
            gene = new Gene(params).save(flush: true)
            controller.update(gene)

        then:"A redirect is issues to the show action"
            response.redirectedUrl == "/gene/show/$gene.id"
            flash.message != null
    }

    void "Test that the delete action deletes an instance if it exists"() {
        when:"The delete action is called for a null instance"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'DELETE'
            controller.delete(null)

        then:"A 404 is returned"
            response.redirectedUrl == '/gene/index'
            flash.message != null

        when:"A domain instance is created"
            response.reset()
            populateValidParams(params)
            def gene = new Gene(params).save(flush: true)

        then:"It exists"
            Gene.count() == 1

        when:"The domain instance is passed to the delete action"
            controller.delete(gene)

        then:"The instance is deleted"
            Gene.count() == 0
            response.redirectedUrl == '/gene/index'
            flash.message != null
    }
}
