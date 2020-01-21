package org.bbop.apollo

import groovy.json.JsonSlurper
import org.bbop.apollo.go.GoAnnotation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.grails.plugins.metrics.groovy.Timed

import java.text.SimpleDateFormat

/**
 * Spec: https://github.com/geneontology/go-annotation/blob/master/specs/gpad-gpi-2-0.md
 */
class GpiHandlerService {

    def configWrapperService

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
            writeGpiEntry(writeObject, goAnnotation)
        }


        out.flush()
        out.close()
    }

    def writeGpiEntry(WriteObject writeObject, GoAnnotation goAnnotation) {
        // 1	DB_Object_ID ::= ID		1	UniProtKB:P11678
        writeObject.out.write(goAnnotation.feature.name)
        writeObject.out.write("\t")
//      2	DB_Object_Symbol ::= xxxx		1	AMOT
        writeObject.out.write(goAnnotation.feature.symbol ?: goAnnotation.feature.name)
        writeObject.out.write("\t")
//      3	DB_Object_Name ::= xxxx		0 or greater	Angiomotin
        writeObject.out.write(goAnnotation.feature.name)
        writeObject.out.write("\t")
//      4	DB_Object_Synonyms ::= [Label] ('|' Label)*		0 or greater	AMOT|KIAA1071
        writeObject.out.write(goAnnotation.feature?.synonyms?.name?.join("|"))
        writeObject.out.write("\t")
//      5	DB_Object_Type ::= OBO_ID	Sequence Ontology	1	SO:0000104
        writeObject.out.write(goAnnotation.feature.ontologyId)
        writeObject.out.write("\t")
//      6	DB_Object_Taxon ::= NCBITaxon:[Taxon_ID]		1	NCBITaxon:9606
      // TODO: add organism
        writeObject.out.write(writeObject.organismString)
        writeObject.out.write("\t")
//      7	Parent_ObjectID ::= [ID] ('|' ID)*		1
        writeObject.out.write("\t")
        writeObject.out.write("\t")
        // 8	DB_Xrefs ::= [ID] ('|' ID)*		0 or greater
        // TODO: add organism


        def dBXrefArray = []
        goAnnotation.feature.featureDBXrefs.each {
          dBXrefArray.add(it.db.description+":"+it.accession)
        }
        writeObject.out.write(dBXrefArray.join("|"))
//        writeObject.out.write(goAnnotation.feature?.featureDBXrefs?.generateReference().join("|"))
        writeObject.out.write("\t")
//      9	Gene_Product_Properties ::= [Property_Value_Pair] ('|' Property_Value_Pair)*		0 or greater	db_subset=Swiss-Prot
        writeObject.out.write("\t")
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
