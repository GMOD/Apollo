package org.bbop.apollo

import grails.converters.JSON
import org.apache.commons.collections.MultiMap
import org.apache.commons.collections.map.MultiValueMap
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class ProxyController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Proxy.list(params), model:[proxyInstanceCount: Proxy.count()]
    }

    def show(Proxy proxyInstance) {
        respond proxyInstance
    }

    def create() {
        respond new Proxy(params)
    }

    /**
     * What is input . . .
     * http://localhost:8080/apollo/proxy/request?request=http://golr.geneontology.org/solr/select?defType=edismax&qt=standard&indent=on&wt=json&rows=10&start=0&fl=*,score&facet=true&facet.mincount=1&facet.sort=count&json.nl=arrarr&facet.limit=25&fq=document_category:%22ontology_class%22&fq=source:(biological_process%20OR%20molecular_function%20OR%20cellular_component)&facet.field=annotation_class&facet.field=synonym&facet.field=alternate_id&q=gree*&qf=annotation_class%5E5&qf=annotation_class_label_searchable%5E5&qf=synonym_searchable%5E1&qf=alternate_id%5E1&json.wrf=jQuery1710751773979049176_1445036592600&_=1445036624006
     * http://golr.geneontology.org/solr/select
     * ?defType=edismax&
qt=standard&
indent=on&
wt=json&
rows=10&
start=0&
fl=*,score&
facet=true&
facet.mincount=1&
facet.sort=count&
json.nl=arrarr&
facet.limit=25&
fq=document_category:%22ontology_class%22&
fq=source:(biological_process%20OR%20molecular_function%20OR%20cellular_component)&
facet.field=annotation_class&
facet.field=synonym&
facet.field=alternate_id&
q=gree*&
qf=annotation_class%5E5&
qf=annotation_class_label_searchable%5E5&
qf=synonym_searchable%5E1&
qf=alternate_id%5E1&
json.wrf=jQuery1710751773979049176_1445036592600&
_=1445036624006

     // what is returned . .
     // MISSING "/solr/select"

     http://golr.geneontology.org
 ?json.wrf=jQuery17109697239506058395_1445105400207&
json=[nl:arrarr, wrf:jQuery17109697239506058395_1445105400207]&
facet=true&
indent=on&
facet.mincount=1&
facet.limit=25&
qf=[annotation_class^5, annotation_class_label_searchable^5, synonym_searchable^1, alternate_id^1]&
json.nl=arrarr&
wt=json&
defType=edismax&
rows=10&
fl=*,score&
start=0&
facet.sort=count&
q=red*&
_=1445105574330&
facet.field=[annotation_class, synonym, alternate_id]&
qt=standard&
fq=[document_category:"ontology_class", source:(biological_process OR molecular_function OR cellular_component)]&
protocol=http&
action=request&
controller=proxy&
returnType=solr&
url=golr.geneontology.org. Stacktrace follows:

     // what SHOULD be returned
     http://golr.geneontology.org/solr/select
 ?defType=edismax&
qt=standard&
indent=on&
wt=json&
rows=10&
start=0&
fl=*,score&
facet=true&
facet.mincount=1&
facet.sort=count&
json.nl=arrarr&
facet.limit=25&
fq=document_category:%22ontology_class%22&
fq=source:(biological_process%20OR%20molecular_function%20OR%20cellular_component)&
facet.field=annotation_class&
facet.field=synonym&
facet.field=alternate_id&
q=red*&
qf=annotation_class%5E5&
qf=annotation_class_label_searchable%5E5&
qf=synonym_searchable%5E1&
qf=alternate_id%5E1&
json.wrf=jQuery17101135514669585973_1445106124853&
_=1445106155048



     * @return
     */
    def request(String url){
        println "params: ${params}"
//        println "protocol: ${protocol}"
        println "requestUrl: ${url}"
//        println "returnType: ${returnType}"
        println "request URI: ${request.requestURI}"
//        http://localhost:8080/apollo/proxy/request/http%253A%252F%252Fgolr.geneontology.org%252Fsolr%252Fselect?defType=edismax&qt=standard&indent=on&wt=json&rows=10&start=0&fl=*,score&facet=true&facet.mincount=1&facet.sort=count&json.nl=arrarr&facet.limit=25&fq=document_category:%22ontology_class%22&fq=source:(biological_process%20OR%20molecular_function%20OR%20cellular_component)&facet.field=annotation_class&facet.field=synonym&facet.field=alternate_id&q=blood*&qf=annotation_class%5E5&qf=annotation_class_label_searchable%5E5&qf=synonym_searchable%5E1&qf=alternate_id%5E1&json.wrf=jQuery171043437046254985034_1445234103214&_=1445234283067

        String referenceUrl = URLDecoder.decode(url,"UTF-8")
        Proxy proxy = Proxy.findByReferenceUrl(referenceUrl)
        if(!proxy){
            log.error "No proxy found for ${referenceUrl}"
            response.status = 404
            return
        }
//        String targetUrl = proxy ? proxy.targetUrl : referenceUrl
//        request.parameterMap.each {
//            println "key: ${it.key} -> ${it.value}"
//        }

        String targetUrl = referenceUrl

        MultiMap multiMap =new MultiValueMap()

        params.eachWithIndex{ it, idx ->
            if(it.value instanceof String){
                multiMap.put(it.key,URLEncoder.encode(it.value,"UTF-8"))
            }
            else if(it.value instanceof HashMap){
                HashMap valueMap = it.value
                for(val in valueMap.values()){
                    multiMap.put(it.key,URLEncoder.encode(val,"UTF-8"))
                }
            }
        }

        int index = 0
        for(key in multiMap.keySet()){
            multiMap.getCollection(key).each {
                targetUrl += index++==0 ? "?" : "&"
                targetUrl += key+"="+it
            }
        }

        URL returnUrl = new URL(targetUrl)

//        println "return URL "+returnUrl
//        String returnText = url.text

        // TODO: make
//        if(returnType.equalsIgnoreCase("json")){
//            println "returning json"

//        response.setContentType("text/plain")
//        response.setCharacterEncoding("UTF-8")

//        response.outputStream << returnUrl.openStream()
//        render text: returnUrl.text
        forward(url:returnUrl)
//            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//            JSONObject returnObject = FormatUtil.convertFromXMLToJSON(docBuilder.parse(returnUrl.openStream()))
//            render returnObject as JSON
//        }
//        else{
//            println "returning else"
//            response.outputStream << returnUrl.openStream()
//        }
    }

    @Transactional
    def save(Proxy proxyInstance) {
        if (proxyInstance == null) {
            notFound()
            return
        }

        if (proxyInstance.hasErrors()) {
            respond proxyInstance.errors, view:'create'
            return
        }

        proxyInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'proxy.label', default: 'Proxy'), proxyInstance.id])
                redirect proxyInstance
            }
            '*' { respond proxyInstance, [status: CREATED] }
        }
    }

    def edit(Proxy proxyInstance) {
        respond proxyInstance
    }

    @Transactional
    def update(Proxy proxyInstance) {
        if (proxyInstance == null) {
            notFound()
            return
        }

        if (proxyInstance.hasErrors()) {
            respond proxyInstance.errors, view:'edit'
            return
        }

        proxyInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Proxy.label', default: 'Proxy'), proxyInstance.id])
                redirect proxyInstance
            }
            '*'{ respond proxyInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Proxy proxyInstance) {

        if (proxyInstance == null) {
            notFound()
            return
        }

        proxyInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Proxy.label', default: 'Proxy'), proxyInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'proxy.label', default: 'Proxy'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
