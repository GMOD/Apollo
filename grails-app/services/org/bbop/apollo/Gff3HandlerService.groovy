package org.bbop.apollo


import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand;
import java.io.*;
import java.util.*

//import groovy.transform.CompileStatic
//@CompileStatic
//@GrailsCompileStatic
public class Gff3HandlerService {

    def sequenceService
    def featureRelationshipService
    def transcriptService
    def exonService
    def featureService
    def overlapperService
    def featurePropertyService

    def attributesToExport=[
            FeatureStringEnum.NAME.value,
            FeatureStringEnum.SYMBOL.value,
            FeatureStringEnum.SYNONYMS.value,
            FeatureStringEnum.DESCRIPTION.value,
            FeatureStringEnum.STATUS.value,
            FeatureStringEnum.DBXREFS.value,
            FeatureStringEnum.OWNER.value,
            FeatureStringEnum.ATTRIBUTES.value,
            FeatureStringEnum.PUBMEDIDS.value,
            FeatureStringEnum.GOIDS.value,
            FeatureStringEnum.COMMENTS.value,
            FeatureStringEnum.DATE_CREATION.value,
            FeatureStringEnum.DATE_LAST_MODIFIED.value
    ]
    public void writeFeaturesToText(PrintWriter out, Collection<? extends Feature> features, String source, Boolean exportSequence = false, Collection<Sequence> sequences = null) throws IOException {

        if (!out.canWrite()) {
            throw new IOException("Cannot write GFF3");
        }

        out.println("##gff-version 3")
        writeFeatures(out, features, source)
        if(exportSequence) {
            writeFastaForReferenceSequences(out, sequences)
            writeFastaForSequenceAlterations(out, features)
        }
        out.flush()
        out.close()
    }


    public void writeFeatures(PrintWriter out, Collection<? extends Feature> features, String source) throws IOException {
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
        featuresBySource.sort{ it.key }
        for (Map.Entry<Sequence, Collection<Feature>> entry : featuresBySource.entrySet()) {
            writeGroupDirectives(out, entry.getKey());
            for (Feature feature : entry.getValue()) {
                writeFeature(out, feature, source);
                writeFeatureGroupEnd(out.out);
            }
        }
    }

    public void writeFeatures(PrintWriter out, Iterator<? extends Feature> iterator, String source, boolean needDirectives) throws IOException {
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            if (needDirectives) {
                writeGroupDirectives(out, feature.getFeatureLocation().sequence)
                needDirectives = false;
            }
            writeFeature(out, feature, source);
            writeFeatureGroupEnd(out.out);
        }
    }

    static private void writeGroupDirectives(PrintWriter out, Sequence sourceFeature) {
        if (sourceFeature.getFeatureLocations().size() == 0) return;
        out.out.println(String.format("##sequence-region %s %d %d", sourceFeature.name, sourceFeature.start + 1, sourceFeature.end));
    }

    static private void writeFeatureGroupEnd(PrintWriter out) {
        out.println("###");
    }

    static private void writeEmptyFastaDirective(PrintWriter out) {
        out.println("##FASTA");
    }

    public void writeFasta(PrintWriter out, Collection<? extends Feature> features) {
        writeEmptyFastaDirective(out.out);
        for (Feature feature : features) {
            writeFasta(out.out, feature, false);
        }
    }

    public void writeFasta(PrintWriter out, Feature feature) {
        writeFasta(out, feature, true);
    }

    public void writeFasta(PrintWriter out, Feature feature, boolean writeFastaDirective) {
        writeFasta(out, feature, writeFastaDirective, true);
    }

    public void writeFasta(PrintWriter out, Feature feature, boolean writeFastaDirective, boolean useLocation) {
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
    
    public void writeFastaForReferenceSequences(PrintWriter out, Collection<Sequence> sequences) {
        for (Sequence sequence : sequences) {
            writeFastaForReferenceSequence(out, sequence)
        }
    }
    
    public void writeFastaForReferenceSequence(PrintWriter out, Sequence sequence) {
        int lineLength = 60;
        String residues = null
        def sequenceTypes = [Insertion.class.canonicalName, Deletion.class.canonicalName, Substitution.class.canonicalName]
        writeEmptyFastaDirective(out.out);
        residues = sequenceService.getRawResiduesFromSequence(sequence, 0, sequence.length)
        if (residues != null) {
            out.out.println(">" + sequence.name);
            int idx = 0;
            while(idx < residues.length()) {
                out.out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())))
                idx += lineLength
            }
        }
    }
    
    public void writeFastaForSequenceAlterations(PrintWriter out, Collection<? extends Feature> features) {
        for (Feature feature : features) {
            if (feature instanceof SequenceAlteration) {
                writeFastaForSequenceAlteration(out, feature)
            }
        }
    }
    
    public void writeFastaForSequenceAlteration(PrintWriter out, SequenceAlteration sequenceAlteration) {
        int lineLength = 60;
        String residues = null
        residues = sequenceAlteration.getAlterationResidue()
        if(residues != null) {
            out.out.println(">" + sequenceAlteration.name)
            int idx = 0;
            while(idx < residues.length()) {
                out.out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())))
                idx += lineLength
            }
        }
    }

    public void convertToEntry(Feature feature, List<Feature> exons, List<Feature> children, String source, List<GFF3Entry> gffEntries) {

        log.debug "converting feature to ${feature.name} entry of # of entries ${gffEntries.size()}"

        String seqId = feature.featureLocation.sequence.name
        String type = featureService.getCvTermFromFeature(feature);
        int start = feature.getFmin() + 1;
        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax();
        String score = ".";
        String strand;
        if (feature.getStrand() == Strand.POSITIVE.getValue()) {
            strand = Strand.POSITIVE.getDisplay()
        } else if (feature.getStrand() == Strand.NEGATIVE.getValue()) {
            strand = Strand.NEGATIVE.getDisplay()
        } else {
            strand = "."
        }
        String phase = ".";
        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase);
        //entry.setAttributes(extractAttributes(feature));
        gffEntries.add(entry);
        for(Feature child_feature : children) {
            if(child_feature instanceof CDS) convertToEntry(child_feature,exons,source,gffEntries)
            else convertToEntry(child_feature,source,gffEntries)
        }
        for(Feature exon : exons) {
            convertToEntry(exon,source,gffEntries)
        }
    }

    public void convertToEntry(CDS cds, List<Feature> exons, String source, List<GFF3Entry> gffEntries) {

        log.debug "converting CDS to ${cds.name} entry of # of entries ${gffEntries.size()}"

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

        int length = 0;
        for (Exon exon : exons) {
            if (!overlapperService.overlaps(exon, cds)) {
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
            //entry.setAttributes(extractAttributes(cds));
            gffEntries.add(entry);
        }
    }
    public void convertToEntry(Feature feature, String source, List<GFF3Entry> gffEntries) {

        log.debug "converting feature to ${feature.name} entry of # of entries ${gffEntries.size()}"
        String seqId = feature.getFeatureLocation().sequence.name
        String type = feature.cvTerm
        String score = ".";
        String strand;
        if (feature.getStrand() == 1) {
            strand = "+";
        } else if (feature.getStrand() == -1) {
            strand = "-";
        } else {
            strand = ".";
        }
        int start = feature.getFmin() + 1;
        int end = feature.getFmax().equals(feature.getFmin()) ? feature.getFmax() + 1 : feature.getFmax()
        String phase="."
        GFF3Entry entry = new GFF3Entry(seqId, source, type, start, end, score, strand, phase)
        //entry.setAttributes(extractAttributes(feature))
        gffEntries.add(entry);
    }
    private Map<String, String> extractAttributes(Feature feature) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FeatureStringEnum.EXPORT_ID.value, encodeString(feature.getUniqueName()));
        if (feature.getName() != null && !isBlank(feature.getName()) && attributesToExport.contains(FeatureStringEnum.NAME.value)) {
            attributes.put(FeatureStringEnum.EXPORT_NAME.value, encodeString(feature.getName()));
        }
        if (attributesToExport.contains(FeatureStringEnum.SYNONYMS.value)) {
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
            log.debug "${feature.name} is a gene and hence doesn't have a parent"
        } else {
            for (Feature parentFeature in featureRelationshipService.getParentForFeature(feature)) {
                count++;
                parents.append(encodeString(parentFeature.uniqueName));
                if (count > 1) {
                    parents.append(","); // highly unlikely scenario for a feature to have more than one parent
                }
                attributes.put(FeatureStringEnum.EXPORT_PARENT.value, parents.toString());
            }
        }

        if (attributesToExport.contains(FeatureStringEnum.COMMENTS.value)) {
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
        if (attributesToExport.contains(FeatureStringEnum.DBXREFS.value)) {
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
        if (feature.getDescription() != null && !isBlank(feature.getDescription()) && attributesToExport.contains(FeatureStringEnum.DESCRIPTION.value)) {
            
            attributes.put(FeatureStringEnum.DESCRIPTION.value, encodeString(feature.getDescription()));
        }
        if (feature.getStatus() != null && attributesToExport.contains(FeatureStringEnum.STATUS.value)) {
            attributes.put(FeatureStringEnum.STATUS.value, encodeString(feature.getStatus().value));
        }
        if (feature.getSymbol() != null && !isBlank(feature.getSymbol()) && attributesToExport.contains(FeatureStringEnum.SYMBOL.value)) {
            attributes.put(FeatureStringEnum.SYMBOL.value, encodeString(feature.getSymbol()));
        }
        Iterator<FeatureProperty> propertyIter = feature.featureProperties.iterator();
        if (attributesToExport.contains(FeatureStringEnum.ATTRIBUTES.value)) {
            if (propertyIter.hasNext()) {
                Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
                while (propertyIter.hasNext()) {
                    FeatureProperty prop = propertyIter.next();
                    StringBuilder props = properties.get(prop.getTag());
                    if (props == null) {
                        if (prop.getTag() == null) {
                            // tag is null for generic properties
                            continue
                        }
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

        if (feature.getOwner() && attributesToExport.contains(FeatureStringEnum.OWNER.value)) {
            attributes.put(FeatureStringEnum.OWNER.value, encodeString(feature.getOwner().username));
        }
        if (attributesToExport.contains(FeatureStringEnum.DATE_CREATION.value)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.dateCreated);
            attributes.put(FeatureStringEnum.DATE_CREATION.value, encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        if (attributesToExport.contains(FeatureStringEnum.DATE_LAST_MODIFIED.value)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(feature.lastUpdated);
            attributes.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, encodeString(String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        }
        return attributes;
    }

    static private String encodeString(String str) {
        return str ? str.replaceAll(",", "%2C").replaceAll("=", "%3D").replaceAll(";", "%3B").replaceAll("\t", "%09") : ""
    }


    public enum Mode {
        READ,
        WRITE
    }

    public enum Format {
        TEXT,
        GZIP
    }
    
    private boolean isBlank(String attributeValue) {
        if (attributeValue == "") {
            return true
        }
        else {
            return false
        }
    }
}
