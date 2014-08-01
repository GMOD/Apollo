package org.gmod.gbol.bioObject.io;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Comment;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.GenericFeatureProperty;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.FeatureSynonym;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

public class GFF3Handler {

    private File file;
    private boolean firstEntry;
    private PrintWriter out;
    private Mode mode;
    private Set<String> attributesToExport;
    
    public enum Mode {
        READ,
        WRITE
    }
    
    public enum Format {
        TEXT,
        GZIP
    }
    
    public GFF3Handler(String path, Mode mode, Set<String> attributesToExport) throws IOException {
        this(path, mode, Format.TEXT, attributesToExport);
    }
    
    public GFF3Handler(String path, Mode mode, Format format, Set<String> attributesToExport) throws IOException {
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
            case TEXT:
                out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                break;
            case GZIP:
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
    
    public void writeFeatures(Collection<? extends AbstractSingleLocationBioFeature> features, String source) throws IOException {
        if (mode != Mode.WRITE) {
            throw new IOException("Cannot write to file in READ mode");
        }
        if (firstEntry) {
            out.println("##gff-version 3");
            firstEntry = false;
        }
        Map<Feature, Collection<AbstractSingleLocationBioFeature>> featuresBySource = new HashMap<Feature, Collection<AbstractSingleLocationBioFeature>>();
        for (AbstractSingleLocationBioFeature feature : features) {
            Feature sourceFeature = feature.getFeatureLocation().getSourceFeature();
            Collection<AbstractSingleLocationBioFeature> featureList = featuresBySource.get(sourceFeature);
            if (featureList == null) {
                featureList = new ArrayList<AbstractSingleLocationBioFeature>();
                featuresBySource.put(sourceFeature, featureList);
            }
            featureList.add(feature);
        }
        for (Map.Entry<Feature, Collection<AbstractSingleLocationBioFeature>> entry : featuresBySource.entrySet()) {
            writeGroupDirectives(entry.getKey());
            for (AbstractSingleLocationBioFeature feature : entry.getValue()) {
                writeFeature(feature, source);
                writeFeatureGroupEnd();
            }
        }
    }
    
    public void writeFeatures(Iterator<? extends AbstractSingleLocationBioFeature> iterator, String source, boolean needDirectives) throws IOException {
        if (mode != Mode.WRITE) {
            throw new IOException("Cannot write to file in READ mode");
        }
        if (firstEntry) {
            out.println("##gff-version 3");
            firstEntry = false;
        }
        while (iterator.hasNext()) {
            AbstractSingleLocationBioFeature feature = iterator.next();
            if (needDirectives) {
                writeGroupDirectives(feature.getFeatureLocation().getSourceFeature());
                needDirectives = false;
            }
            writeFeature(feature, source);
            writeFeatureGroupEnd();
        }
    }
    
    private void writeGroupDirectives(Feature sourceFeature) {
        FeatureLocation loc = sourceFeature.getFeatureLocations().iterator().next();
        out.println(String.format("##sequence-region %s %d %d", sourceFeature.getUniqueName(), loc.getFmin() + 1, loc.getFmax()));
    }
    
    private void writeFeatureGroupEnd() {
        out.println("###");
    }
    
    private void writeFastaDirective() {
        out.println("##FASTA");
    }
    
    private void writeFeature(AbstractSingleLocationBioFeature feature, String source) {
        for (GFF3Entry entry : convertToEntry(feature, source)) {
            out.println(entry.toString());
        }
    }
    
    public void writeFasta(Collection<? extends Feature> features) {
        writeFastaDirective();
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
            writeFastaDirective();
        }
        String residues = null;
        if (useLocation) {
            FeatureLocation loc = feature.getFeatureLocations().iterator().next();
            residues = feature.getResidues(loc.getFmin(), loc.getFmax());
        }
        else {
            residues = feature.getResidues();
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
    
    private Collection<GFF3Entry> convertToEntry(AbstractSingleLocationBioFeature feature, String source) {
        List<GFF3Entry> gffEntries = new ArrayList<GFF3Entry>();
        convertToEntry(feature, source, gffEntries);
        return gffEntries;
    }
    
    private void convertToEntry(AbstractSingleLocationBioFeature feature, String source, Collection<GFF3Entry> gffEntries) {
        String []cvterm = feature.getType().split(":");
        String seqId = feature.getFeatureLocation().getSourceFeature().getUniqueName();
        String type = cvterm[1];
        int start = feature.getFmin() + 1;
        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax();
        String score = ".";
        String strand;
        if (feature.getStrand() == 1) {
            strand = "+";
        }
        else if (feature.getStrand() == -1) {
            strand = "-";
        }
        else {
            strand = ".";
        }
        String phase = ".";
        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase);
        entry.setAttributes(extractAttributes(feature));
        gffEntries.add(entry);
        for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
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
        String score = ".";
        String strand;
        if (cds.getStrand() == 1) {
            strand = "+";
        }
        else if (cds.getStrand() == -1) {
            strand = "-";
        }
        else {
            strand = ".";
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
        for (AbstractSingleLocationBioFeature child : cds.getChildren()) {
            convertToEntry(child, source, gffEntries);
        }
    }
    
    private Map<String, String> extractAttributes(AbstractBioFeature feature) {
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
        Iterator<GenericFeatureProperty> propertyIter = feature.getNonReservedProperties().iterator();
        if (attributesToExport.contains("attributes")) {
            if (propertyIter.hasNext()) {
                Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
                do {
                    GenericFeatureProperty prop = propertyIter.next();
                    StringBuilder props = properties.get(prop.getTag());
                    if (props == null) {
                        props = new StringBuilder();
                        properties.put(prop.getTag(), props);
                    }
                    else {
                        props.append(",");
                    }
                    props.append(prop.getValue());
                }
                while (propertyIter.hasNext());
                for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
                    attributes.put(encodeString(iter.getKey()), encodeString(iter.getValue().toString()));
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
