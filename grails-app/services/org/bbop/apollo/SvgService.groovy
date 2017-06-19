package org.bbop.apollo

import grails.transaction.Transactional
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.ext.awt.geom.Polygon2D
import org.apache.batik.svggen.SVGGraphics2D
import org.bbop.apollo.track.RenderObject
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document
import org.w3c.dom.Element

import java.awt.*

@Transactional(readOnly = true)
class SvgService {

    final Integer GLOBAL_HEIGHT = 30
    final Integer GLOBAL_WIDTH = 400

    def renderSVGFromJSONArray(JSONArray jsonArray) {
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation()

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);


        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        int maxIsoForms = getMaxIsoForms(jsonArray)
        log.debug "max isoformas: ${maxIsoForms}"
        int canvasHeight = maxIsoForms*GLOBAL_HEIGHT
        log.debug "canvas height: ${canvasHeight}"
        svgGenerator.setSVGCanvasSize(new Dimension(500,canvasHeight))

        generateFeatures(svgGenerator, jsonArray)

        StringWriter stringWriter = new StringWriter()
        svgGenerator.stream(stringWriter, true, false)
        stringWriter.close()
        log.debug "should be returning ${stringWriter.toString()}"
        return stringWriter.toString()
    }

    int getMaxIsoForms(JSONArray jsonArray) {
        int maxIsoForms = 1
        for(def childArray in jsonArray){
            maxIsoForms = childArray?.children?.size() > maxIsoForms ? childArray?.children?.size() : maxIsoForms
        }
        return maxIsoForms
    }

    def generateFeatures(SVGGraphics2D svgGraphics2D, JSONArray jsonArray) {
        if (!jsonArray) return

        RenderObject renderObject = new RenderObject(
               globalFmin:jsonArray.first().fmin,
               globalFmax:jsonArray.first().fmax,
        )

        // Renders in the X-axis, multiple feature roots
        log.debug "# of feature regions: ${jsonArray?.size()}"
        for (JSONObject jsonObject in jsonArray) {
            generateFeature(svgGraphics2D, jsonObject, renderObject)
        }
    }

    def generateFeature(SVGGraphics2D svgGraphics2D, JSONObject jsonObject, RenderObject renderObject) {
        // assume type is mRNA for type
        int globalWidth = renderObject.globalWidth
        int internalFmin = GLOBAL_WIDTH * ((jsonObject.fmin - renderObject.globalFmin) / globalWidth) ?: 1
        int internalFmax = GLOBAL_WIDTH * ((jsonObject.fmax - renderObject.globalFmin) / globalWidth)-1
        int height = GLOBAL_HEIGHT / 2.0

        // this will go away once we start working with introns
        svgGraphics2D.setStroke(new BasicStroke(2))
        svgGraphics2D.setPaint(Color.black)
        svgGraphics2D.drawLine(internalFmin, height, internalFmax, height)

        int stepHeight = 0
        // render multiple isofrms
        log.debug "# of isoforms: ${jsonObject.children?.size()}"
        for(JSONArray children in jsonObject.children){
            // Renders in the Y-axis
            renderChildren(svgGraphics2D,children,renderObject,stepHeight)
            stepHeight += GLOBAL_HEIGHT
        }

    }

    def drawStrand(SVGGraphics2D svgGraphics2D, JSONObject jsonObject,RenderObject renderObject) {
        svgGraphics2D.setColor(Color.BLACK)
        int globalWidth = renderObject.globalWidth
//        int internalFmin = GLOBAL_WIDTH * ((jsonObject.fmin - renderObject.globalFmin) / globalWidth)
        int internalFmax = GLOBAL_WIDTH * ((jsonObject.fmax - renderObject.globalFmin) / globalWidth)
        int height = GLOBAL_HEIGHT / 2.0
        // draw an arrow to do with strand now
        if(jsonObject.strand==-1){
            Polygon2D shape = new Polygon2D()
            shape.addPoint(2,height)
            shape.addPoint(12,(int) GLOBAL_HEIGHT * 3 / 4)
            shape.addPoint(12,(int) 0 + (GLOBAL_HEIGHT / 4) )
            svgGraphics2D.draw(shape)
            svgGraphics2D.fill(shape)
        }
        else
        if(jsonObject.strand==1){
            Polygon2D shape = new Polygon2D()
            // TODO: not sure why those numbers work, why we need substract 200?
            shape.addPoint(internalFmax,height)
            shape.addPoint(internalFmax-10,(int) GLOBAL_HEIGHT * 3 / 4)
            shape.addPoint(internalFmax-10,(int) 0 + (GLOBAL_HEIGHT / 4) )
            svgGraphics2D.draw(shape)
            svgGraphics2D.fill(shape)
        }

    }

    def renderChildren(SVGGraphics2D svgGraphics2D, JSONArray children, RenderObject renderObject, int stepHeight) {
        int globalWidth = renderObject.globalWidth

        // sorted by FMIN and the type (CDS to exon)
        for(JSONObject childObject in children.sort(){ a,b ->
            if(a.type != b.type ){
                if(a.type == "exon"){
                  return -1
                }
                else{
                    return a.type <=> b.type
                }
            }
            return a.fmin <=> b.fmin
        }){
            drawStrand(svgGraphics2D,childObject,renderObject)
            String type = childObject.type
            Integer phase = childObject.phase  // if a CDS
            int internalFmin = GLOBAL_WIDTH * ((childObject.fmin - renderObject.globalFmin) / globalWidth) ?: 1
            int internalFmax = GLOBAL_WIDTH * ((childObject.fmax - renderObject.globalFmin) / globalWidth)
            int height = GLOBAL_HEIGHT / 2.0
//            svgGraphics2D.drawRect(internalFmin,stepHeight,(internalFmax-internalFmin),GLOBAL_HEIGHT)
            if(type=="CDS"){
                Polygon2D shape = new Polygon2D()
                shape.addPoint(internalFmin,stepHeight+10)
                shape.addPoint(internalFmax,stepHeight+10)
                shape.addPoint(internalFmax,stepHeight+GLOBAL_HEIGHT-10)
                shape.addPoint(internalFmin,stepHeight+GLOBAL_HEIGHT-10)
                switch (phase){
                    case 0:
                        svgGraphics2D.setColor(Color.RED)
                        break
                    case 1:
                        svgGraphics2D.setColor(Color.BLUE)
                        break
                    case 2:
                        svgGraphics2D.setColor(Color.GREEN)
                        break
                }
                svgGraphics2D.fill(shape)
            }
            else
            if(type=="exon"){
                Polygon2D shape = new Polygon2D()
                svgGraphics2D.setColor(Color.BLACK)
                shape.addPoint(internalFmin,stepHeight+10)
                shape.addPoint(internalFmax,stepHeight+10)
                shape.addPoint(internalFmax,stepHeight+GLOBAL_HEIGHT-10)
                shape.addPoint(internalFmin,stepHeight+GLOBAL_HEIGHT-10)
                svgGraphics2D.draw(shape)
                svgGraphics2D.setColor(Color.WHITE)
                svgGraphics2D.fill(shape)
            }
        }
    }
}
