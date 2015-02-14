package org.bbop.apollo;

//import *;
//import util.BioObjectUtil;
//import *;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import groovy.transform.CompileStatic


@CompileStatic
public class GFF3HandlerService {

    private File file;
    private boolean firstEntry;
    private PrintWriter out;
    private Mode mode;
    private Set<String> attributesToExport;

    def sequenceService
    def featureRelationshipService
    def featureService

    public enum Mode {
        READ,
        WRITE
    }
    
    public enum Format {
        TEXT,
        GZIP
    }
    
    public GFF3HandlerService(String path, Mode mode, Set<String> attributesToExport) throws IOException {
        this(path, mode, Format.TEXT, attributesToExport);
    }
    
    public GFF3HandlerService(String path, Mode mode, Format format, Set<String> attributesToExport) throws IOException {
        this.mode = mode;
        this.attributesToExport = attributesToExport;
        file = new File(path);
        file.createNewFile();
        firstEntry = true;
        if (mode == Mode.READ) {
            if (!file.canRead()) {
                throw new IOException("Cannot read GFF3 file: " + file.getAbsolutePath());
            }
        }
        if (mode == Mode.WRITE) {
            if (!file.canWrite()) {
                throw new IOException("Cannot write GFF3 to: " + file.getAbsolutePath());
            }
            switch (format) {
            case Format.TEXT:
                out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                break;
            case Format.GZIP:
                out = new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
            }
        }
    }
    
    public void close() {
        if (mode == Mode.READ) {
            //TODO
        }
        else if (mode == Mode.WRITE) { 
            out.close();
        }
    }
    
    public void writeFeatures(Collection<? extends Feature> features, String source) throws IOException {
        if (mode != Mode.WRITE) {
            throw new IOException("Cannot write to file in READ mode");
        }
        if (firstEntry) {
            out.println("##gff-version 3");
            firstEntry = false;
        }
        Map<Sequence, Collection<Feature>> featuresBySource = new HashMap<Sequence, Collection<Feature>>();
        for (Feature feature : features) {
            Sequence sourceFeature = feature.getFeatureLocation().sequence
            Collection<Feature> featureList = featuresBySource.get(sourceFeature);
            if (featureList == null) {
                featureList = new ArrayList<Feature>();
                featuresBySource.put(sourceFeature, featureList);
            }
            featureList.add(feature);
        }
        for (Map.Entry<Sequence, Collection<Feature>> entry : featuresBySource.entrySet()) {
            writeGroupDirectives(entry.getKey());
            for (Feature feature : entry.getValue()) {
                writeFeature(feature, source);
                writeFeatureGroupEnd();
            }
        }
    }
    
    public void writeFeatures(Iterator<? extends Feature> iterator, String source, boolean needDirectives) throws IOException {
        if (mode != Mode.WRITE) {
            throw new IOException("Cannot write to file in READ mode");
        }
        if (firstEntry) {
            out.println("##gff-version 3");
            firstEntry = false;
        }
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            if (needDirectives) {
                writeGroupDirectives(feature.getFeatureLocation().sequence)
                needDirectives = false;
            }
            writeFeature(feature, source);
            writeFeatureGroupEnd();
        }
    }
    
    private void writeGroupDirectives(Sequence sourceFeature) {
        if(sourceFeature.getFeatureLocations().size()==0) return ;
        FeatureLocation loc = sourceFeature.getFeatureLocations().iterator().next();
        out.println(String.format("##sequence-region %s %d %d", sourceFeature.name, loc.getFmin() + 1, loc.getFmax()));
    }
    
    private void writeFeatureGroupEnd() {
        out.println("###");
    }
    
    private void writeEmptyFastaDirective() {
        out.println("##FASTA");
    }
    
    private void writeFeature(Feature feature, String source) {
        for (GFF3Entry entry : convertToEntry(feature, source)) {
            out.println(entry.toString());
        }
    }
    
    public void writeFasta(Collection<? extends Feature> features) {
        writeEmptyFastaDirective();
        for (Feature feature : features) {
            writeFasta(feature, false);
        }
    }
    
    public void writeFasta(Feature feature) {
        writeFasta(feature, true);
    }
    
    public void writeFasta(Feature feature, boolean writeFastaDirective) {
        writeFasta(feature, writeFastaDirective, true);
    }
    
    public void writeFasta(Feature feature, boolean writeFastaDirective, boolean useLocation) {
        int lineLength = 60;
        if (writeFastaDirective) {
            writeEmptyFastaDirective();
        }
        String residues = null;
        if (useLocation) {
            FeatureLocation loc = feature.getFeatureLocations().iterator().next();
            residues = sequenceService.getResidueFromFeatureLocation(loc)
        }
        else {
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
    
    private Collection<GFF3Entry> convertToEntry(Feature feature, String source) {
        List<GFF3Entry> gffEntries = new ArrayList<GFF3Entry>();
        convertToEntry(feature, source, gffEntries);
        return gffEntries;
    }
    
    private void convertToEntry(Feature feature, String source, Collection<GFF3Entry> gffEntries) {
        String []cvterm = feature.cvTerm.split(":");
        String seqId = feature.getFeatureLocation().sequence.name
        String type = cvterm[1];
        int start = feature.getFmin() + 1;
        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax();
        String score = "";
        String strand;
        if (feature.getStrand() == 1) {
            strand = "+";
        }
        else if (feature.getStrand() == -1) {
            strand = "-";
        }
        else {
            strand = "";
        }
        String phase = "";
        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase);
        entry.setAttributes(extractAttributes(feature));
        gffEntries.add(entry);
        for (Feature child : featureRelationshipService.getChildren(feature)) {
            if (child instanceof CDS) {
                convertToEntry((CDS)child, source, gffEntries);
            }
            else {
                convertToEntry(child, source, gffEntries);
            }
        }
    }
    
    private void convertToEntry(CDS cds, String source, Collection<GFF3Entry> gffEntries) {
        String []cvterm = cds.getType().split(":");
        String seqId = cds.getFeatureLocation().getSourceFeature().getUniqueName();
        String type = cvterm[1];
        String score = "";
        String strand;
        if (cds.getStrand() == 1) {
            strand = "+";
        }
        else if (cds.getStrand() == -1) {
            strand = "-";
        }
        else {
            strand = "";
        }
        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(cds.getTranscript().getExons());
        int length = 0;
        for (Exon exon : exons) {
            if (!exon.overlaps(cds)) {
                continue;
            }
            int fmin = exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin();
            int fmax = exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax();
            String phase;
            if (length % 3 == 0) {
                phase = "0";
            }
            else if (length % 3 == 1) {
                phase = "2";
            }
            else {
                phase = "1";
            }
            length += fmax - fmin;
            GFF3Entry entry = new GFF3Entry(seqId, source, type, fmin + 1, fmax, score, strand, phase);
            entry.setAttributes(extractAttributes(cds));
            gffEntries.add(entry);
        }
        for (Feature child : cds.getChildren()) {
            convertToEntry(child, source, gffEntries);
        }
    }
    
    private Map<String, String> extractAttributes(Feature feature) {
        SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
        Feature simpleFeature = (Feature)iterator.next();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("ID", encodeString(feature.getUniqueName()));
        if (feature.getName() != null && attributesToExport.contains("name")) {
            attributes.put("Name", encodeString(feature.getName()));
        }
        if (attributesToExport.contains("synonyms")) {
            Iterator<FeatureSynonym> synonymIter = feature.getSynonyms().iterator();
            if (synonymIter.hasNext()) {
                StringBuilder synonyms = new StringBuilder();
                synonyms.append(synonymIter.next().getSynonym().getName());
                while (synonymIter.hasNext()) {
                    synonyms.append(",");
                    synonyms.append(encodeString(synonymIter.next().getSynonym().getName()));
                }
                attributes.put("Alias", synonyms.toString());
            }
        }
        Iterator<FeatureRelationship> frIter = simpleFeature.getParentFeatureRelationships().iterator();
        if (frIter.hasNext()) {
            StringBuilder parents = new StringBuilder();
            parents.append(encodeString(frIter.next().getObjectFeature().getUniqueName()));
            while (frIter.hasNext()) {
                parents.append(",");
                parents.append(encodeString(frIter.next().getObjectFeature().getUniqueName()));
            }
            attributes.put("Parent", parents.toString());
        }
        //TODO: Target
        //TODO: Gap
        if (attributesToExport.contains("comments")) {
            Iterator<Comment> commentIter = feature.getComments().iterator();
            if (commentIter.hasNext()) {
                StringBuilder comments = new StringBuilder();
                comments.append(encodeString(commentIter.next().getComment()));
                while (commentIter.hasNext()) {
                    comments.append(",");
                    comments.append(encodeString(commentIter.next().getComment()));
                }
                attributes.put("Note", comments.toString());
            }
        }
        if (attributesToExport.contains("dbxrefs")) {
            Iterator<DBXref> dbxrefIter = feature.getNonPrimaryDBXrefs().iterator();
            if (dbxrefIter.hasNext()) {
                StringBuilder dbxrefs = new StringBuilder();
                DBXref dbxref = dbxrefIter.next();
                dbxrefs.append(encodeString(dbxref.getDb().getName() + ":" + dbxref.getAccession()));
                while (dbxrefIter.hasNext()) {
                    dbxrefs.append(",");
                    dbxref = dbxrefIter.next();
                    dbxrefs.append(encodeString(dbxref.getDb().getName()) + ":" + encodeString(dbxref.getAccession()));
                }
                attributes.put("Dbxref", dbxrefs.toString());
            }
        }
        if (feature.getDescription() != null && attributesToExport.contains("description")) {
            attributes.put("description", encodeString(feature.getDescription().getDescription()));
        }
        if (feature.getStatus() != null && attributesToExport.contains("status")) {
            attributes.put("status", encodeString(feature.getStatus().getStatus()));
        }
        if (feature.getSymbol() != null && attributesToExport.contains("symbol")) {
            attributes.put("symbol", encodeString(feature.getSymbol().getSymbol()));
        }
        //TODO: Ontology_term
        //TODO: Is_circular
        Iterator<FeatureProperty> propertyIter = feature.featureProperties.iterator();
        if (attributesToExport.contains("attributes")) {
            if (propertyIter.hasNext()) {
                Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
                while (propertyIter.hasNext()){
                    FeatureProperty prop = propertyIter.next();
                    StringBuilder props = properties.get(prop.getTag());
                    if (props == null) {
                        props = new StringBuilder();
                        properties.put(prop.getTag(), props);
                    }
                    else {
                        props.append(",");
                    }
                    props.append(encodeString(prop.getValue()));
                }
                for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
                    attributes.put(encodeString(iter.getKey()), iter.getValue().toString());
                }
            }
        }
        if (attributesToExport.contains("owner")) {
            attributes.put("owner", encodeString(feature.getOwner().getOwner()));
        }
        if (attributesToExport.contains("date_creation")) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.getTimeAccessioned());
            attributes.put("date_creation", encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        if (attributesToExport.contains("date_last_modified")) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.getTimeLastModified());
            attributes.put("date_last_modified", encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        return attributes;
    }
    
    private String encodeString(String str) {
        return str.replaceAll(",", "%2C").replaceAll("=", "%3D").replaceAll(";", "%3B").replaceAll("\t", "%09");
    }
    
    public class GFF3Entry {

        private String seqId;
        private String source;
        private String type;
        private int start;
        private int end;
        private String score;
        private String strand;
        private String phase;
        private Map<String, String> attributes;
        
        public GFF3Entry(String seqId, String source, String type, int start, int end, String score, String strand, String phase) {
            this.seqId = seqId;
            this.source = source;
            this.type = type;
            this.start = start;
            this.end = end;
            this.score = score;
            this.strand = strand;
            this.phase = phase;
            this.attributes = new HashMap<String, String>();
        }

        public String getSeqId() {
            return seqId;
        }

        public void setSeqId(String seqId) {
            this.seqId = seqId;
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public int getStart() {
            return start;
        }
        
        public void setStart(int start) {
            this.start = start;
        }
        
        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getStrand() {
            return strand;
        }

        public void setStrand(String strand) {
            this.strand = strand;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }
        
        public Map<String, String> getAttributes() {
            return attributes;
        }
        
        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
        
        public void addAttribute(String key, String value) {
            attributes.put(key, value);
        }
        
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(String.format("%s\t%s\t%s\t%d\t%d\t%s\t%s\t%s\t", getSeqId(), getSource(), getType(), getStart(), getEnd(), getScore(), getStrand(), getPhase()));
            Iterator<Map.Entry<String, String>> iter = attributes.entrySet().iterator();
            if (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                buf.append(entry.getKey());
                buf.append("=");
                buf.append(entry.getValue());
                while (iter.hasNext()) {
                    entry = iter.next();
                    buf.append(";");
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue());
                }
            }
            return buf.toString();
        }
    }
    
}
