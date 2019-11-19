package org.bbop.apollo

import groovy.json.JsonSlurper
import org.bbop.apollo.go.GoAnnotation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.grails.plugins.metrics.groovy.Timed

import java.text.SimpleDateFormat

class GpadHandlerService {

    SimpleDateFormat gpadDateFormat = new SimpleDateFormat("YYYY-MM-dd")

    @Timed
    void writeFeaturesToText(String path, Collection<? extends Feature> features) throws IOException {
        WriteObject writeObject = new WriteObject()

        writeObject.mode = Mode.WRITE
        writeObject.file = new File(path)
        writeObject.format = Format.TEXT

        if (!writeObject.file.canWrite()) {
            throw new IOException("Cannot write GFF3 to: " + writeObject.file.getAbsolutePath());
        }


        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeObject.file, true)));
        writeObject.out = out
        out.println("##gpad-version 2")
        def goAnnotations = GoAnnotation.findAllByFeatureInList(features as List<Feature>)

        if (features) {
            writeObject.organismString = features.first().featureLocation.sequence.organism.commonName
        }

        for (GoAnnotation goAnnotation in goAnnotations) {
            writeGpadEntry(writeObject, goAnnotation)
        }


        out.flush()
        out.close()
    }

    def writeGpadEntry(WriteObject writeObject, GoAnnotation goAnnotation) {
        // 1	DB_Object_ID ::= ID		1	UniProtKB:P11678
        writeObject.out.write(goAnnotation.feature.name)
        writeObject.out.write("\t")
        //2	Negation ::= 'NOT'		0 or 1	NOT
        writeObject.out.write(goAnnotation.negate ? "NOT" : "")
        writeObject.out.write("\t")
        //3	Relation ::= OBO_ID	Relations Ontology	1	RO:0002263
        writeObject.out.write(goAnnotation.geneProductRelationshipRef)
        writeObject.out.write("\t")
        //4	Ontology_Class_ID ::= OBO_ID	Gene Ontology	1	GO:0050803
        writeObject.out.write(goAnnotation.goRef)
        writeObject.out.write("\t")
        //5	Reference ::= ID		1	PMID:30695063
//        writeObject.out.write(goAnnotation.notesArray)
        if (goAnnotation.notesArray) {
            JSONArray referenceArray = new JsonSlurper().parseText(goAnnotation.notesArray) as JSONArray
            List<String> referenceList = referenceArray.collect()
            writeObject.out.write(referenceList.join("|"))
        }
//        else{
//            writeObject.out.write("")
//        }
        writeObject.out.write("\t")
        //6	Evidence_type ::= OBO_ID	Evidence and Conclusion Ontology	1	ECO:0000315
        writeObject.out.write(goAnnotation.evidenceRef)
        writeObject.out.write("\t")
        //7	With_or_From ::= [ID] ('|' | ‘,’ ID)*		0 or greater	WB:WBVar00000510
        if (goAnnotation.withOrFromArray) {
            JSONArray withArray = new JsonSlurper().parseText(goAnnotation.withOrFromArray) as JSONArray
            List<String> withList = withArray.collect()
            writeObject.out.write(withList.join("|"))
        }
//        else{
//            writeObject.out.write("")
//        }
        writeObject.out.write("\t")
        //8	Interacting_taxon_ID ::= NCBITaxon:[Taxon_ID]		0 or greater	NCBITaxon:5476
        // TODO: add organism
        writeObject.out.write(writeObject.organismString)
        writeObject.out.write("\t")
        //9	Date ::= YYYY-MM-DD		1	2019-01-30
        writeObject.out.write(gpadDateFormat.format(goAnnotation.lastUpdated))
        writeObject.out.write("\t")
        //10	Assigned_by ::= Prefix		1	MGI
        writeObject.out.write("Apollo-${grails.util.Metadata.current['app.version']}")
        writeObject.out.write("\t")
        //11	Annotation_Extensions ::= [Extension_Conj] ('|' Extension_Conj)*		0 or greater	BFO:0000066
        writeObject.out.write("")
        writeObject.out.write("\t")
        //12	Annotation_Properties ::= [Property_Value_Pair] ('|' Property_Value_Pair)*		0 or greater	contributor=https://orcid.org/0000-0002-1478-7671


        String contributorString = null

        if(goAnnotation.owners){
            List<String> contributorArray = []
            goAnnotation.owners.each { User user ->
                contributorArray.add("contributor_name=${user.username}")
            }
            contributorString = contributorArray.join("|")
        }

        String noteString = null
        if(goAnnotation.notesArray){
            List<String> notesArray = []
            JSONArray notesJsonArray = new JsonSlurper().parseText(goAnnotation.notesArray) as JSONArray
            notesJsonArray.each {
                notesArray.add("annotation_note=${it}")
            }
            noteString = notesArray.join("|")
        }
        String tab12String = contributorString ?: ""
        if(tab12String && noteString){
            tab12String += "|"
            tab12String += noteString
        }
        writeObject.out.write(tab12String)

        writeObject.out.write("\t")

        writeObject.out.println()
    }

    String formatDate(Date date) {
        return gpadDateFormat.format(date)
    }

    static private String encodeString(String str) {
        return str ? str.replaceAll(",", "%2C").replaceAll("\n", "%0A").replaceAll("=", "%3D").replaceAll(";", "%3B").replaceAll("\t", "%09") : ""
    }


    enum Mode {
        READ,
        WRITE
    }

    enum Format {
        TEXT,
        GZIP
    }

    private class WriteObject {
        File file
        PrintWriter out
        Mode mode
        Format format
        String organismString
    }

}
