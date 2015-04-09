package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import org.bbop.apollo.sequence.Strand;

//import *;
//import util.BioObjectUtil;
//import *;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import grails.compiler.GrailsCompileStatic

//import groovy.transform.CompileStatic
//
//
//@CompileStatic
//@GrailsCompileStatic
public class Gff3HandlerService {

    def sequenceService
    def featureRelationshipService
    def transcriptService
    def exonService
    def featureService
    def featurePropertyService


    // adding a default attribute list to export when defaultAttributesToExport is null
    private List<String> defaultAttributesExport = ["name"]
    
    public void writeFeaturesToText(String path,Collection<? extends Feature> features, String source) throws IOException {
        WriteObject writeObject = new WriteObject( )

        writeObject.mode = Mode.WRITE
        writeObject.file = new File(path)
        writeObject.format = Format.TEXT
        if(!writeObject.attributesToExport){
            writeObject.attributesToExport = defaultAttributesExport;
        }
        if (!writeObject.file.canWrite()) {
            throw new IOException("Cannot write GFF3 to: " + writeObject.file.getAbsolutePath());
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeObject.file, true)));
        writeObject.out = out
        writeFeatures(writeObject,features,source)
        out.close()
    }


    public void writeFeatures(WriteObject writeObject,Collection<? extends Feature> features, String source) throws IOException {
        Map<Sequence, Collection<Feature>> featuresBySource = new HashMap<Sequence, Collection<Feature>>();
        for (Feature feature : features) {
            Sequence sourceFeature = feature.getFeatureLocation().sequence
            Collection<Feature> featureList = featuresBySource.get(sourceFeature);
            if (!featureList) {
                featureList = new ArrayList<Feature>();
                featuresBySource.put(sourceFeature, featureList);
            }
            featureList.add(feature);
        }
        for (Map.Entry<Sequence, Collection<Feature>> entry : featuresBySource.entrySet()) {
            writeGroupDirectives(writeObject,entry.getKey());
            for (Feature feature : entry.getValue()) {
                writeFeature(writeObject,feature, source);
                writeFeatureGroupEnd(writeObject.out);
            }
        }
    }

    public void writeFeatures(WriteObject writeObject,Iterator<? extends Feature> iterator, String source, boolean needDirectives) throws IOException {
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            if (needDirectives) {
                writeGroupDirectives(writeObject,feature.getFeatureLocation().sequence)
                needDirectives = false;
            }
            writeFeature(writeObject,feature, source);
            writeFeatureGroupEnd(writeObject.out);
        }
    }

    static private void writeGroupDirectives(WriteObject writeObject,Sequence sourceFeature) {
        if (sourceFeature.getFeatureLocations().size() == 0) return;
        FeatureLocation loc = sourceFeature.getFeatureLocations().iterator().next();
        //writeObject.out.println(String.format("##sequence-region %s %d %d", sourceFeature.name, loc.getFmin() + 1, loc.getFmax()));
        writeObject.out.println(String.format("##sequence-region %s %d %d", sourceFeature.name, sourceFeature.start + 1, sourceFeature.end));
    }

    static private void writeFeatureGroupEnd(PrintWriter out) {
        out.println("###");
    }

    static private void writeEmptyFastaDirective(PrintWriter out) {
        out.println("##FASTA");
    }

    private void writeFeature(WriteObject writeObject,Feature feature, String source) {
        for (GFF3Entry entry : convertToEntry(writeObject,feature, source)) {
            writeObject.out.println(entry.toString());
        }
    }

    public void writeFasta(PrintWriter out,Collection<? extends Feature> features) {
        writeEmptyFastaDirective(out);
        for (Feature feature : features) {
            writeFasta(out,feature, false);
        }
    }

    public void writeFasta(PrintWriter out,Feature feature) {
        writeFasta(out,feature, true);
    }

    public void writeFasta(PrintWriter out,Feature feature, boolean writeFastaDirective) {
        writeFasta(out,feature, writeFastaDirective, true);
    }

    public void writeFasta(PrintWriter out,Feature feature, boolean writeFastaDirective, boolean useLocation) {
        int lineLength = 60;
        if (writeFastaDirective) {
            writeEmptyFastaDirective(out);
        }
        String residues = null;
        if (useLocation) {
            FeatureLocation loc = feature.getFeatureLocations().iterator().next();
            residues = sequenceService.getResidueFromFeatureLocation(loc)
        } else {
            residues = sequenceService.getResiduesFromFeature(feature)
        }
        if (residues != null) {
            out.println(">" + feature.getUniqueName());
            int idx = 0;
            while (idx < residues.length()) {
                out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())));
                idx += lineLength;
            }
        }
    }

    private Collection<GFF3Entry> convertToEntry(WriteObject writeObject,Feature feature, String source) {
        List<GFF3Entry> gffEntries = new ArrayList<GFF3Entry>();
        convertToEntry(writeObject,feature, source, gffEntries);
        return gffEntries;
    }

    private void convertToEntry(WriteObject writeObject,Feature feature, String source, Collection<GFF3Entry> gffEntries) {
        String[] cvterm = feature.cvTerm.split(":");
        String seqId = feature.getFeatureLocation().sequence.name
        String type = featureService.getCvTermFromFeature(feature);
        int start = feature.getFmin() + 1;
        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax();
        String score = ".";
        String strand;
        if (feature.getStrand() == Strand.POSITIVE.getValue()) {
            strand = Strand.POSITIVE.getDisplay()
        } else if (feature.getStrand() == Strand.POSITIVE.getValue()) {
            strand = Strand.POSITIVE.getDisplay()
        } else {
            strand = "."
        }
        String phase = ".";
        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase);
        entry.setAttributes(extractAttributes(writeObject,feature));
        gffEntries.add(entry);
        for (Feature child : featureRelationshipService.getChildren(feature)) {
            if (child instanceof CDS) {
                convertToEntry(writeObject, (CDS) child, source, gffEntries);
            } else {
                convertToEntry(writeObject, child, source, gffEntries);
            }
        }
    }

    private void convertToEntry(WriteObject writeObject,CDS cds, String source, Collection<GFF3Entry> gffEntries) {
        String seqId = cds.getFeatureLocation().sequence.name
        String type = cds.cvTerm
        String score = ".";
        String strand;
        if (cds.getStrand() == 1) {
            strand = "+";
        } else if (cds.getStrand() == -1) {
            strand = "-";
        } else {
            strand = ".";
        }
        //featureRelationshipService.getParentForFeature(cds,transcriptService.ontologyIds)
        Transcript transcript = transcriptService.getParentTranscriptForFeature(cds)

        List<Exon> exons = exonService.getSortedExons(transcript)
        int length = 0;
        for (Exon exon : exons) {
            if (!featureService.overlaps(exon, cds)) {
                continue;
            }
            int fmin = exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin();
            int fmax = exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax();
            String phase;
            if (length % 3 == 0) {
                phase = "0";
            } else if (length % 3 == 1) {
                phase = "2";
            } else {
                phase = "1";
            }
            length += fmax - fmin;
            GFF3Entry entry = new GFF3Entry(seqId, source, type, fmin + 1, fmax, score, strand, phase);
            entry.setAttributes(extractAttributes(writeObject,cds));
            gffEntries.add(entry);
        }
        for (Feature child : featureRelationshipService.getChildren(cds)) {
            convertToEntry(writeObject,child, source, gffEntries);
        }
    }

    private Map<String, String> extractAttributes(WriteObject writeObject, Feature feature) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FeatureStringEnum.EXPORT_ID.value, encodeString(feature.getUniqueName()));
        if (feature.getName() != null && writeObject.attributesToExport.contains(FeatureStringEnum.NAME.value)) {
            attributes.put(FeatureStringEnum.EXPORT_NAME.value, encodeString(feature.getName()));
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.SYNONYMS.value)) {
            Iterator<Synonym> synonymIter = feature.synonyms.iterator();
            if (synonymIter.hasNext()) {
                StringBuilder synonyms = new StringBuilder();
                synonyms.append(synonymIter.next().getName());
                while (synonymIter.hasNext()) {
                    synonyms.append(",");
                    synonyms.append(encodeString(synonymIter.next().getName()));
                }
                attributes.put(FeatureStringEnum.EXPORT_ALIAS.value, synonyms.toString());
            }
        }

        int count = 0;
        StringBuilder parents = new StringBuilder();
        if (feature.ontologyId == Gene.ontologyId) {
            println "${feature.name} is a gene and hence doesn't have a parent"
        }
        else {
            for (Feature parentFeature in featureRelationshipService.getParentForFeature(feature)) {
                count++;
                parents.append(encodeString(parentFeature.uniqueName));
                if (count > 1) {
                    parents.append(","); // highly unlikely scenario for a feature to have more than one parent
                }
                attributes.put(FeatureStringEnum.EXPORT_PARENT.value, parents.toString());
            }
        }
        
        //TODO: Target
        //TODO: Gap
        if (writeObject.attributesToExport.contains(FeatureStringEnum.COMMENTS.value)) {
            Iterator<Comment> commentIter = featurePropertyService.getComments(feature).iterator()
            if (commentIter.hasNext()) {
                StringBuilder comments = new StringBuilder();
                comments.append(encodeString(commentIter.next().value));
                while (commentIter.hasNext()) {
                    comments.append(",");
                    comments.append(encodeString(commentIter.next().value));
                }
                attributes.put(FeatureStringEnum.EXPORT_NOTE.value, comments.toString());
            }
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.DBXREFS.value)) {
            Iterator<DBXref> dbxrefIter = feature.featureDBXrefs.iterator();
            if (dbxrefIter.hasNext()) {
                StringBuilder dbxrefs = new StringBuilder();
                DBXref dbxref = dbxrefIter.next();
                dbxrefs.append(encodeString(dbxref.getDb().getName() + ":" + dbxref.getAccession()));
                while (dbxrefIter.hasNext()) {
                    dbxrefs.append(",");
                    dbxref = dbxrefIter.next();
                    dbxrefs.append(encodeString(dbxref.getDb().getName()) + ":" + encodeString(dbxref.getAccession()));
                }
                attributes.put(FeatureStringEnum.EXPORT_DBXREF.value, dbxrefs.toString());
            }
        }
        if (feature.getDescription() != null && writeObject.attributesToExport.contains(FeatureStringEnum.DESCRIPTION.value)) {
            attributes.put(FeatureStringEnum.DESCRIPTION.value, encodeString(feature.getDescription()));
        }
        if (feature.getStatus() != null && writeObject.attributesToExport.contains(FeatureStringEnum.STATUS.value)) {
            attributes.put(FeatureStringEnum.STATUS.value, encodeString(feature.getStatus().value));
        }
        if (feature.getSymbol() != null && writeObject.attributesToExport.contains(FeatureStringEnum.SYMBOL.value)) {
            attributes.put(FeatureStringEnum.SYMBOL.value, encodeString(feature.getSymbol()));
        }
        //TODO: Ontology_term
        //TODO: Is_circular
        Iterator<FeatureProperty> propertyIter = feature.featureProperties.iterator();
        if (writeObject.attributesToExport.contains(FeatureStringEnum.ATTRIBUTES.value)) {
            if (propertyIter.hasNext()) {
                Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
                while (propertyIter.hasNext()) {
                    FeatureProperty prop = propertyIter.next();
                    StringBuilder props = properties.get(prop.getTag());
                    if (props == null) {
                        props = new StringBuilder();
                        properties.put(prop.getTag(), props);
                    } else {
                        props.append(",");
                    }
                    props.append(encodeString(prop.getValue()));
                }
                for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
                    attributes.put(encodeString(iter.getKey()), iter.getValue().toString());
                }
            }
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.OWNER.value)) {
            attributes.put(FeatureStringEnum.OWNER.value, encodeString(feature.getOwner().username));
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_CREATION.value)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.dateCreated);
            attributes.put(FeatureStringEnum.DATE_CREATION.value, encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_LAST_MODIFIED.value)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.lastUpdated);
            attributes.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        return attributes;
    }

    static private String encodeString(String str) {
        
        return str.replaceAll(",", "%2C").replaceAll("=", "%3D").replaceAll(";", "%3B").replaceAll("\t", "%09");
    }


    public enum Mode {
        READ,
        WRITE
    }

    public enum Format {
        TEXT,
        GZIP
    }


    private class WriteObject {
        File file;
        PrintWriter out;
        Mode mode;
        Set<String> attributesToExport;
        Format format
    }

}
