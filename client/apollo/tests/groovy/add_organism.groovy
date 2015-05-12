@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

import groovyx.net.http.RESTClient

//body : [ firstName:'John', lastName:'Doe' ]
def client = new RESTClient( 'http://localhost:8080/' )
def resp = client.post(
        path: '/apollo/organism/addOrganism'
//, body : [ firstName:'John', lastName:'Doe' ]
)

assert resp.status == 200  // HTTP response code; 404 means not found, etc.
println resp.getData()
