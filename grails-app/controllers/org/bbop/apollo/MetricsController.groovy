package org.bbop.apollo

import grails.converters.JSON
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class MetricsController {

    def metrics() {
        JSONObject result = new JSONObject()
        result.put("version", "4.0.0")
        result.put("timers", new JSONObject())
        result.put("counters", new JSONObject())
        result.put("gauges", new JSONObject())
        result.put("histograms", new JSONObject())
        result.put("meters", new JSONObject())
        render result as JSON
    }

    def threads() {
        JSONArray result = new JSONArray()
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            JSONObject threadInfo = new JSONObject()
            threadInfo.put("name", thread.name)
            threadInfo.put("state", thread.state.toString())
            threadInfo.put("daemon", thread.daemon)
            result.add(threadInfo)
        }
        render result as JSON
    }
}
