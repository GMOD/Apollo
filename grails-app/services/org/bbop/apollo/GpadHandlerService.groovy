package org.bbop.apollo

import groovy.json.JsonSlurper
import org.bbop.apollo.go.GoAnnotation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.grails.plugins.metrics.groovy.Timed

import java.text.SimpleDateFormat

class GpadHandlerService {

//    def sequenceService
//    def featureRelationshipService
//    def transcriptService
//    def exonService
//    def configWrapperService
//    def requestHandlingService
//    def featureService
//    def overlapperService
//    def featurePropertyService
//    def goAnnotationService

    SimpleDateFormat gpadDateFormat = new SimpleDateFormat("YYYY-MM-dd")

//    static final def unusedStandardAttributes = ["Alias", "Target", "Gap", "Derives_from", "Ontology_term", "Is_circular"];
    @Timed
    void writeFeaturesToText(String path, Collection<? extends Feature> features, String source, Boolean exportSequence = false, Collection<Sequence> sequences = null) throws IOException {
        WriteObject writeObject = new WriteObject()

        writeObject.mode = Mode.WRITE
        writeObject.file = new File(path)
        writeObject.format = Format.TEXT


//
//        // TODO: use specified metadata?
//        writeObject.attributesToExport.add(FeatureStringEnum.NAME.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.SYMBOL.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.SYNONYMS.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.DESCRIPTION.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.STATUS.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.DBXREFS.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.OWNER.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.ATTRIBUTES.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.PUBMEDIDS.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.GOIDS.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.COMMENTS.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.DATE_CREATION.value);
//        writeObject.attributesToExport.add(FeatureStringEnum.DATE_LAST_MODIFIED.value);
//
        if (!writeObject.file.canWrite()) {
            throw new IOException("Cannot write GFF3 to: " + writeObject.file.getAbsolutePath());
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeObject.file, true)));
        writeObject.out = out
        out.println("##gpad-version 2")
//        writeFeatures(writeObject, features, source)

//        def uniqueNames = features.uniqueName

        def goAnnotations = GoAnnotation.findAllByFeatureInList(features as List<Feature>)

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
//        writeObject.out.write(goAnnotation.referenceArray)
        if(goAnnotation.referenceArray){
            JSONArray referenceArray = new JsonSlurper().parseText(goAnnotation.referenceArray) as JSONArray
            List<String> referenceList = referenceArray.collect( )
            writeObject.out.write(referenceList.join(","))
        }
//        else{
//            writeObject.out.write("")
//        }
        writeObject.out.write("\t")
        //6	Evidence_type ::= OBO_ID	Evidence and Conclusion Ontology	1	ECO:0000315
        writeObject.out.write(goAnnotation.evidenceRef)
        writeObject.out.write("\t")
        //7	With_or_From ::= [ID] ('|' | ‘,’ ID)*		0 or greater	WB:WBVar00000510
        if(goAnnotation.withOrFromArray){
            JSONArray withArray = new JsonSlurper().parseText(goAnnotation.withOrFromArray) as JSONArray
            List<String> withList = withArray.collect( )
            writeObject.out.write(withList.join(","))
        }
//        else{
//            writeObject.out.write("")
//        }
        writeObject.out.write("\t")
        //8	Interacting_taxon_ID ::= NCBITaxon:[Taxon_ID]		0 or greater	NCBITaxon:5476
        // TODO: add organism
        writeObject.out.write("taxon or organism name")
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
        // TODO: add owners . . contriburor  -
        writeObject.out.write("")
        writeObject.out.write("\t")


        writeObject.out.println()

    }
//    @Timed
//    private void convertToEntry(WriteObject writeObject, Feature feature, String source, Collection<GFF3Entry> gffEntries) {
//
//        //log.debug "converting feature to ${feature.name} entry of # of entries ${gffEntries.size()}"
//
//        String seqId = feature.featureLocation.sequence.name
//        String type = featureService.getCvTermFromFeature(feature);
//        int start = feature.getFmin() + 1;
//        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax();
//        String score = ".";
//        String strand;
//        if (feature.getStrand() == Strand.POSITIVE.getValue()) {
//            strand = Strand.POSITIVE.getDisplay()
//        } else if (feature.getStrand() == Strand.NEGATIVE.getValue()) {
//            strand = Strand.NEGATIVE.getDisplay()
//        } else {
//            strand = "."
//        }
//        String phase = ".";
//        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase);
//        entry.setAttributes(extractAttributes(writeObject, feature));
//        gffEntries.add(entry);
//        if(featureService.typeHasChildren(feature)){
//            for (Feature child : featureRelationshipService.getChildren(feature)) {
//                if (child instanceof CDS) {
//                    convertToEntry(writeObject, (CDS) child, source, gffEntries);
//                } else {
//                    convertToEntry(writeObject, child, source, gffEntries);
//                }
//            }
//        }
//    }
//
//    @Timed
//    private void convertToEntry(WriteObject writeObject, CDS cds, String source, Collection<GFF3Entry> gffEntries) {
//        //log.debug "converting CDS to ${cds.name} entry of # of entries ${gffEntries.size()}"
//
//        String seqId = cds.featureLocation.sequence.name
//        String type = cds.cvTerm
//        String score = ".";
//        String strand;
//        if (cds.getStrand() == 1) {
//            strand = "+";
//        } else if (cds.getStrand() == -1) {
//            strand = "-";
//        } else {
//            strand = ".";
//        }
//        Transcript transcript = transcriptService.getParentTranscriptForFeature(cds)
//
//        List<Exon> exons = transcriptService.getSortedExons(transcript,true)
//        int length = 0;
//        for (Exon exon : exons) {
//            if (!overlapperService.overlaps(exon, cds)) {
//                continue;
//            }
//            int fmin = exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin();
//            int fmax = exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax();
//            String phase;
//            if (length % 3 == 0) {
//                phase = "0";
//            } else if (length % 3 == 1) {
//                phase = "2";
//            } else {
//                phase = "1";
//            }
//            length += fmax - fmin;
//            GFF3Entry entry = new GFF3Entry(seqId, source, type, fmin + 1, fmax, score, strand, phase);
//            entry.setAttributes(extractAttributes(writeObject, cds));
//            gffEntries.add(entry);
//        }
//        for (Feature child : featureRelationshipService.getChildren(cds)) {
//            convertToEntry(writeObject, child, source, gffEntries);
//        }
//    }
//
//    @Timed
//    private Map<String, String> extractAttributes(WriteObject writeObject, Feature feature) {
//        Map<String, String> attributes = new HashMap<String, String>();
//        attributes.put(FeatureStringEnum.EXPORT_ID.value, encodeString(feature.getUniqueName()));
//        if (feature.getName() != null && !isBlank(feature.getName()) && writeObject.attributesToExport.contains(FeatureStringEnum.NAME.value)) {
//            attributes.put(FeatureStringEnum.EXPORT_NAME.value, encodeString(feature.getName()));
//        }
//        if (!(feature.class.name in requestHandlingService.viewableAnnotationList+requestHandlingService.viewableAlterations)) {
//            def parent= featureRelationshipService.getParentForFeature(feature)
//            attributes.put(FeatureStringEnum.EXPORT_PARENT.value, encodeString(parent.uniqueName));
//        }
//        if(configWrapperService.exportSubFeatureAttrs() || feature.class.name in requestHandlingService.viewableAnnotationList+requestHandlingService.viewableAnnotationTranscriptList+requestHandlingService.viewableAlterations) {
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.SYNONYMS.value)) {
//                Iterator<Synonym> synonymIter = feature.synonyms.iterator();
//                if (synonymIter.hasNext()) {
//                    StringBuilder synonyms = new StringBuilder();
//                    synonyms.append(synonymIter.next().getName());
//                    while (synonymIter.hasNext()) {
//                        synonyms.append(",");
//                        synonyms.append(encodeString(synonymIter.next().getName()));
//                    }
//                    attributes.put(FeatureStringEnum.EXPORT_ALIAS.value, synonyms.toString());
//                }
//            }
//
//
//            //TODO: Target
//            //TODO: Gap
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.COMMENTS.value)) {
//                Iterator<Comment> commentIter = featurePropertyService.getComments(feature).iterator()
//                if (commentIter.hasNext()) {
//                    StringBuilder comments = new StringBuilder();
//                    comments.append(encodeString(commentIter.next().value));
//                    while (commentIter.hasNext()) {
//                        comments.append(",");
//                        comments.append(encodeString(commentIter.next().value));
//                    }
//                    attributes.put(FeatureStringEnum.EXPORT_NOTE.value, comments.toString());
//                }
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.DBXREFS.value)) {
//                Iterator<DBXref> dbxrefIter = feature.featureDBXrefs.iterator();
//                if (dbxrefIter.hasNext()) {
//                    StringBuilder dbxrefs = new StringBuilder();
//                    DBXref dbxref = dbxrefIter.next();
//                    dbxrefs.append(encodeString(dbxref.getDb().getName() + ":" + dbxref.getAccession()));
//                    while (dbxrefIter.hasNext()) {
//                        dbxrefs.append(",");
//                        dbxref = dbxrefIter.next();
//                        dbxrefs.append(encodeString(dbxref.getDb().getName()) + ":" + encodeString(dbxref.getAccession()));
//                    }
//                    attributes.put(FeatureStringEnum.EXPORT_DBXREF.value, dbxrefs.toString());
//                }
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.DESCRIPTION.value) && feature.getDescription() != null && !isBlank(feature.getDescription())) {
//
//                attributes.put(FeatureStringEnum.DESCRIPTION.value, encodeString(feature.getDescription()));
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.STATUS.value) && feature.getStatus() != null) {
//                attributes.put(FeatureStringEnum.STATUS.value, encodeString(feature.getStatus().value));
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.SYMBOL.value) && feature.getSymbol() != null && !isBlank(feature.getSymbol())) {
//                attributes.put(FeatureStringEnum.SYMBOL.value, encodeString(feature.getSymbol()));
//            }
//            //TODO: Ontology_term
//            //TODO: Is_circular
//            Iterator<FeatureProperty> propertyIter = feature.featureProperties.iterator();
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.ATTRIBUTES.value)) {
//                if (propertyIter.hasNext()) {
//                    Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
//                    while (propertyIter.hasNext()) {
//                        FeatureProperty prop = propertyIter.next();
//                        if (prop instanceof Comment) {
//                            // ignoring 'comment' as they are already processed earlier
//                            continue
//                        }
//                        StringBuilder props = properties.get(prop.getTag());
//                        if (props == null) {
//                            if (prop.getTag() == null) {
//                                // tag is null for generic properties
//                                continue
//                            }
//                            props = new StringBuilder();
//                            properties.put(prop.getTag(), props);
//                        } else {
//                            props.append(",");
//                        }
//                        props.append(encodeString(prop.getValue()));
//                    }
//                    for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
//                        if (iter.getKey() in unusedStandardAttributes) {
//                            attributes.put(encodeString(WordUtils.capitalizeFully(iter.getKey())), iter.getValue().toString());
//                        }
//                        else {
//                            attributes.put(encodeString(WordUtils.uncapitalize(iter.getKey())), iter.getValue().toString());
//                        }
//                    }
//                }
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.OWNER.value) && feature.getOwner()) {
//                String ownersString = feature.owners.collect{ owner ->
//                    encodeString(owner.username)
//                }.join(",")
//                // Note: how to do this using history directly, but only the top-level visible object gets annotated (e.g., the mRNA)
//                // also, this is a separate query to the history table for each GFF3, so very slow
////                def owners = FeatureEvent.findAllByUniqueName(feature.uniqueName).editor.unique()
////                String ownersString = owners.collect{ owner ->
////                    encodeString(owner.username)
////                }.join(",")
//                attributes.put(FeatureStringEnum.OWNER.value.toLowerCase(), ownersString);
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_CREATION.value)) {
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(feature.dateCreated);
//                attributes.put(FeatureStringEnum.DATE_CREATION.value, encodeString(formatDate(calendar.time)));
//            }
//            if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_LAST_MODIFIED.value)) {
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(feature.lastUpdated);
//                attributes.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, encodeString(formatDate(calendar.time)));
//            }
//
//
//            if(feature.class.name in [InsertionArtifact.class.name, SubstitutionArtifact.class.name]) {
//                attributes.put(FeatureStringEnum.RESIDUES.value, feature.alterationResidue)
//            }
//        }
//        return attributes;
//    }

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

//    private boolean isBlank(String attributeValue) {
//        if (attributeValue == "") {
//            return true
//        }
//        else {
//            return false
//        }
//    }

    private class WriteObject {
        File file;
        PrintWriter out;
        Mode mode;
        Set<String> attributesToExport = new HashSet<>();
        Format format;
    }

}
