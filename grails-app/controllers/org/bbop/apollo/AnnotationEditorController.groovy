package org.bbop.apollo

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

class AnnotationEditorController {

    def index() {
       println "bang "
    }



    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        println "got here! . . . "
        return "hello from controller, ${world}!"
    }

    @MessageMapping("/AnnotationEditorService")
    @SendTo("/topic/AnnotationEditorService")
    protected String annotationEditor(String inputString) {
        println " annotation editor service ${inputString}"
        return "annotationEditor ${inputString}!"
    }
}
