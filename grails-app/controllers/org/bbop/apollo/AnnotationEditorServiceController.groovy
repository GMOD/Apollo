package org.bbop.apollo

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

class AnnotationEditorServiceController {

    def index() {
       println "bang "
    }



    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        println "got here! . . . "
        return "hello from controller, ${world}!"
    }
}
