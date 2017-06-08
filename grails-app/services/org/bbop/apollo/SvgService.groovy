package org.bbop.apollo

import grails.transaction.Transactional
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.ext.awt.geom.Polygon2D
import org.apache.batik.svggen.SVGGraphics2D
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document

import java.awt.*

@Transactional(readOnly = true)
class SvgService {

    final Integer GLOBAL_HEIGHT = 30
    final Integer GLOBAL_WIDTH = 500
    final Integer TOP_PADDING = 10
    final Integer LEFT_PADDING = 10

    void paint(Graphics2D g2d) {
        g2d.setPaint(Color.red);
        g2d.fill(new Rectangle(10, 10, 100, 100));
    }

    def renderSVGFromJSONArray(JSONArray jsonArray) {
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setClip(0,0,GLOBAL_WIDTH,GLOBAL_HEIGHT)

        generateFeatures(svgGenerator, jsonArray)

        StringWriter stringWriter = new StringWriter()
        svgGenerator.stream(stringWriter, true, false)
        stringWriter.close()
        println "should be returning ${stringWriter.toString()}"
        return stringWriter.toString()
    }

    def generateFeatures(SVGGraphics2D svgGraphics2D, JSONArray jsonArray) {
        if (!jsonArray) return

        long globalFmin = jsonArray.first().fmin
        long globalFmax = jsonArray.last().fmax
        for (JSONObject jsonObject in jsonArray) {
            generateFeature(svgGraphics2D, jsonObject, globalFmin, globalFmax)
        }
    }

    def generateFeature(SVGGraphics2D svgGraphics2D, JSONObject jsonObject, long globalFmin, long globalFmax) {
        // assume type is mRNA for type
        int globalWidth = globalFmax - globalFmin
        int internalFmin = GLOBAL_WIDTH * ((jsonObject.fmin - globalFmin) / globalWidth)
        int internalFmax = GLOBAL_WIDTH * ((jsonObject.fmax - globalFmin) / globalWidth)
        int height = GLOBAL_HEIGHT / 2.0

        // this will go away once we start working with introns
        svgGraphics2D.setStroke(new BasicStroke(2.0))
        svgGraphics2D.setPaint(Color.black)
        svgGraphics2D.drawLine(internalFmin, height, internalFmax, height)

        // draw an arrow to do with strand now
        if(jsonObject.strand==-1){
            println "doing negative strand ${jsonObject.strand}"
            Polygon2D shape = new Polygon2D()
            shape.addPoint(0,height)
            shape.addPoint(10,(int) GLOBAL_HEIGHT * 3 / 4)
            shape.addPoint(10,(int) 0 + (GLOBAL_HEIGHT / 4) )
            svgGraphics2D.draw(shape)
            svgGraphics2D.fill(shape)
        }
        else
        if(jsonObject.strand==1){
            println "doing positive strand ${jsonObject.strand}"
            Polygon2D shape = new Polygon2D()
            // TODO: not sure why those numbers work, why we need substract 200?
            shape.addPoint(internalFmax-200,height)
            shape.addPoint(internalFmax-210,(int) GLOBAL_HEIGHT * 3 / 4)
            shape.addPoint(internalFmax-210,(int) 0 + (GLOBAL_HEIGHT / 4) )
            svgGraphics2D.draw(shape)
            svgGraphics2D.fill(shape)
        }


    }

}
