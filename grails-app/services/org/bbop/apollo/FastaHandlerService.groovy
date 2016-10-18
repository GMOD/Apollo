package org.bbop.apollo

import org.bbop.apollo.sequence.Strand
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;


public class FastaHandlerService {

    private File file;
    private PrintWriter out;
    private Mode mode;
    public final static int NUM_RESIDUES_PER_LINE = 60;

    def sequenceService
    def transcriptService
    def featurePropertyService

    public enum Mode {
        READ,
        WRITE
    }
    
    public enum Format {
        TEXT,
        GZIP
    }
    

    public void close() {
        if (mode == Mode.READ) {
            //TODO
        }
        else if (mode == Mode.WRITE) { 
            out.close();
        }
    }
    
    public void writeFeatures(Collection<Feature> features, String seqType, Set<String> metaDataToExport, String path, Mode mode, Format format,String region = null ) throws IOException {
        this.mode = mode
        file = new File(path)
        file.createNewFile()
        if(mode == Mode.READ) {
            if (!file.canRead()) {
                throw new IOException("Cannot read FASTA file: " + file.getAbsolutePath())
            }
        }
        if(mode == Mode.WRITE) {
            if(!file.canWrite()) {
                throw new IOException("Cannot write FASTA to file: " + file.getAbsolutePath())
            }
            switch(format) {
                case Format.TEXT:
                    out = new PrintWriter(new BufferedWriter(new FileWriter(file)))
                    break
                case Format.GZIP:
                    out = new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))
                    break
            }
            
        }
        writeFeatures(features.iterator(), seqType, metaDataToExport,region);
        out.flush()
        out.close()
    }
    
    public void writeFeatures(Iterator<? extends Feature> iterator, String seqType, Set<String> metaDataToExport,String region) throws IOException {
        if (mode != Mode.WRITE) {
            throw new IOException("Cannot write to file in READ mode");
        }
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            if(feature.class.name in [Gene.class.name, Pseudogene.class.name]) {
                def transcriptList = transcriptService.getTranscripts(feature)
                for (Transcript transcript in transcriptList) {
                    writeFeature(transcript, seqType, metaDataToExport);
                }
            }
            else {
                writeFeature(feature, seqType, metaDataToExport)
            }
        }
    }
    
    public void writeFeature(Feature feature, String seqType, Set<String> metaDataToExport) {
        String seq = sequenceService.getSequenceForFeature(feature, seqType, 0)
        int featureLength = seq.length()
        if (featureLength == 0) {
            // no sequence returned by getSequenceForFeature()
            log.debug " export for ${seqType.toUpperCase()} resulted in a sequence length 0 for ${feature.uniqueName} of type ${feature.class.canonicalName}"
            return
        }

        String strand

        if (feature.getStrand() == Strand.POSITIVE.getValue()) {
            strand = Strand.POSITIVE.getDisplay()
        } else if (feature.getStrand() == Strand.NEGATIVE.getValue()) {
            strand = Strand.NEGATIVE.getDisplay()
        } else {
            strand = "."
        }
        //int featureLength = sequenceService.getResiduesFromFeature(feature).length()
        String defline = String.format(">%s (%s) %d residues [%s:%d-%d %s strand] [%s]", feature.getUniqueName(), feature.cvTerm, featureLength, feature.getFeatureLocation().getSequence().name, feature.fmin + 1, feature.fmax, strand, seqType);
        if (!metaDataToExport.isEmpty()) {
            boolean first = true;
            if (metaDataToExport.contains("name") && feature.getName() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "name=" + feature.getName();
            }
            if (metaDataToExport.contains("symbol") && feature.getSymbol() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "symbol=" + feature.symbol
            }
            if (metaDataToExport.contains("description") && feature.getDescription() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "description=" + feature.description
            }
            if (metaDataToExport.contains("status") && feature.getStatus() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "status=" + feature.status.value
            }
            if (metaDataToExport.contains("dbxrefs")) {
                Iterator<DBXref> dbxrefIter = feature.featureDBXrefs.iterator();
                if (dbxrefIter.hasNext()) {
                    if (!first) {
                        defline += ";";
                    }
                    else {
                        defline += " ";
                    }
                    first = false;
                    StringBuilder dbxrefs = new StringBuilder();
                    DBXref dbxref = dbxrefIter.next();
                    dbxrefs.append(dbxref.getDb().getName() + ":" + dbxref.getAccession());
                    while (dbxrefIter.hasNext()) {
                        dbxrefs.append(",");
                        dbxref = dbxrefIter.next();
                        dbxrefs.append(dbxref.getDb().getName() + ":" + dbxref.getAccession());
                    }
                    defline += "dbxrefs=" + dbxrefs.toString();
                }
            }
            if (metaDataToExport.contains("attributes")) {
                Iterator<FeatureProperty> propertyIter = feature.featureProperties.iterator();
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
                        props.append(prop.getValue());
                    }
//                    while (propertyIter.hasNext());
                    if (!first) {
                        defline += ";";
                    }
                    else {
                        defline += " ";
                    }
                    first = false;
                    for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
                        defline += iter.getKey() + "=" + iter.getValue().toString();
                    }
                }
            }
            // TODO: set comment
            if (metaDataToExport.contains(FeatureStringEnum.COMMENTS.value)) {
                Iterator<Comment> commentIter = featurePropertyService.getComments(feature).iterator()
                if (commentIter.hasNext()) {
                    StringBuilder comments = new StringBuilder();
                    comments.append(commentIter.next().value);
                    while (commentIter.hasNext()) {
                        comments.append(",");
                        comments.append(commentIter.next().value);
                    }
                    if (!first) {
                        defline += ";";
                    }
                    else {
                        defline += " ";
                    }
                    first = false;
                    defline += FeatureStringEnum.COMMENTS.value+"=" + comments.toString();
                }
            }
            // TODO: set owner
            if (metaDataToExport.contains(FeatureStringEnum.OWNER.value) && feature.getOwner() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += FeatureStringEnum.OWNER.value+"=" + feature.getOwner().getOwner();
            }
            if (metaDataToExport.contains("date_creation") && feature.dateCreated != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feature.dateCreated)
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "date_last_modified=" + String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }
            if (metaDataToExport.contains("date_last_modified") && feature.lastUpdated != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feature.lastUpdated);
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "date_last_modified=" + String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }
        }
        out.println(defline);
//        String seq = sequenceService.getResiduesFromFeature(feature)
        // TODO: use splitter
        for (int i = 0; i < seq.length(); i += NUM_RESIDUES_PER_LINE) {
            int endIdx = i + NUM_RESIDUES_PER_LINE;
            out.println(seq.substring(i, endIdx > seq.length() ? seq.length() : endIdx));
        }
    }
    
}
