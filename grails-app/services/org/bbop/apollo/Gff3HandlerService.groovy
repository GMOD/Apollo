package org.bbop.apollo

import org.apache.commons.lang.WordUtils
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand
import org.grails.plugins.metrics.groovy.Timed
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.text.SimpleDateFormat

public class Gff3HandlerService {

    def sequenceService
    def featureRelationshipService
    def transcriptService
    def exonService
    def configWrapperService
    def requestHandlingService
    def featureService
    def overlapperService
    def featurePropertyService

    SimpleDateFormat gff3DateFormat = new SimpleDateFormat("YYYY-MM-dd")

    static final
    def unusedStandardAttributes = ["Alias", "Target", "Gap", "Derives_from", "Ontology_term", "Is_circular"];

    static final
    def topLevelFeatureList = [
            Gene.alternateCvTerm,
            Pseudogene.alternateCvTerm,
            RepeatRegion.alternateCvTerm,
            TransposableElement.alternateCvTerm
    ]

    static final
    def transcriptList = [
            Transcript.alternateCvTerm,
            MRNA.alternateCvTerm,
            TRNA.alternateCvTerm,
            SnRNA.alternateCvTerm,
            SnoRNA.alternateCvTerm,
            NcRNA.alternateCvTerm,
            RRNA.alternateCvTerm,
            MiRNA.alternateCvTerm
    ]

    static final
    def sequenceAlterationList = [
            Insertion.alternateCvTerm,
            Deletion.alternateCvTerm,
            Substitution.alternateCvTerm
    ]

    @Timed
    public void writeFeaturesToText(String path, Collection<? extends Feature> features, String source, Boolean exportSequence = false, Collection<Sequence> sequences = null) throws IOException {
        WriteObject writeObject = new WriteObject()

        writeObject.mode = Mode.WRITE
        writeObject.file = new File(path)
        writeObject.format = Format.TEXT

        // TODO: use specified metadata?
        writeObject.attributesToExport.add(FeatureStringEnum.NAME.value);
        writeObject.attributesToExport.add(FeatureStringEnum.SYMBOL.value);
        writeObject.attributesToExport.add(FeatureStringEnum.SYNONYMS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DESCRIPTION.value);
        writeObject.attributesToExport.add(FeatureStringEnum.STATUS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DBXREFS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.OWNER.value);
        writeObject.attributesToExport.add(FeatureStringEnum.ATTRIBUTES.value);
        writeObject.attributesToExport.add(FeatureStringEnum.PUBMEDIDS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.GOIDS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.COMMENTS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DATE_CREATION.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DATE_LAST_MODIFIED.value);

        if (!writeObject.file.canWrite()) {
            throw new IOException("Cannot write GFF3 to: " + writeObject.file.getAbsolutePath());
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeObject.file, true)));
        writeObject.out = out
        out.println("##gff-version 3")
        writeFeatures(writeObject, features, source)
        if (exportSequence) {
            writeFastaForReferenceSequences(writeObject, sequences)
            writeFastaForSequenceAlterations(writeObject, features)
        }
        out.flush()
        out.close()
    }

    def writeFeaturesToText(String path, def assemblageToFeaturesMap, String source, Boolean exportSequence = false) throws IOException {
        WriteObject writeObject = new WriteObject()
        writeObject.mode = Mode.WRITE
        writeObject.file = new File(path)
        writeObject.format = Format.TEXT

        // TODO: use specified metadata?
        writeObject.attributesToExport.add(FeatureStringEnum.NAME.value);
        writeObject.attributesToExport.add(FeatureStringEnum.SYMBOL.value);
        writeObject.attributesToExport.add(FeatureStringEnum.SYNONYMS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DESCRIPTION.value);
        writeObject.attributesToExport.add(FeatureStringEnum.STATUS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DBXREFS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.OWNER.value);
        writeObject.attributesToExport.add(FeatureStringEnum.ATTRIBUTES.value);
        writeObject.attributesToExport.add(FeatureStringEnum.PUBMEDIDS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.GOIDS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.COMMENTS.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DATE_CREATION.value);
        writeObject.attributesToExport.add(FeatureStringEnum.DATE_LAST_MODIFIED.value);

        if (!writeObject.file.canWrite()) {
            throw new IOException("Cannot write GFF3 to: " + writeObject.file.getAbsolutePath());
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeObject.file, true)));
        writeObject.out = out
        out.println("##gff-version 3")
        writeFeatures(writeObject, assemblageToFeaturesMap, source)
        if (exportSequence) {
            writeFastaForReferenceSequences(writeObject, assemblages)
            writeFastaForSequenceAlterations(writeObject, assemblageToFeaturesMap)
        }
        out.flush()
        out.close()
    }

    @Timed
    public void writeFeatures(WriteObject writeObject, Collection<? extends Feature> features, String source) throws IOException {
        Map<Sequence, Collection<Feature>> featuresBySource = new HashMap<Sequence, Collection<Feature>>();
        for (Feature feature : features) {
            feature.featureLocations.each { featureLocation ->
                Sequence sourceFeature = featureLocation.sequence
                Collection<Feature> featureList = featuresBySource.get(sourceFeature);
                if (!featureList) {
                    featureList = new ArrayList<Feature>();
                    featuresBySource.put(sourceFeature, featureList);
                }
                featureList.add(feature);
            }
        }
        featuresBySource.sort { it.key }
        for (Map.Entry<Sequence, Collection<Feature>> entry : featuresBySource.entrySet()) {
            writeGroupDirectives(writeObject, entry.getKey());
            for (Feature feature : entry.getValue()) {
                writeFeature(writeObject, feature, source);
                writeFeatureGroupEnd(writeObject.out);
            }
        }
    }

    def writeFeatures(WriteObject writeObject, def assemblageToFeaturesMap, String source) throws IOException {
        // at this point all features have been projected onto an assemblage
        assemblageToFeaturesMap.sort { it.key }
        assemblageToFeaturesMap.each {
            // processing each assemblage in assemblageToFeaturesMap
            writeGroupDirectives(writeObject, it.key)
            for (int i = 0; i < it.value.length(); i++) {
                JSONObject jsonFeature = it.value.get(i)
                writeFeature(writeObject, jsonFeature, source)
                writeFeatureGroupEnd(writeObject.out)
            }
        }
    }

//    @Timed
//    public void writeFeatures(WriteObject writeObject, Iterator<? extends Feature> iterator, String source, boolean needDirectives) throws IOException {
//        while (iterator.hasNext()) {
//            Feature feature = iterator.next();
//            if (needDirectives) {
//                writeGroupDirectives(writeObject, feature.featureLocation.sequence)
//                needDirectives = false;
//            }
//            writeFeature(writeObject, feature, source);
//            writeFeatureGroupEnd(writeObject.out);
//        }
//    }

    static private void writeGroupDirectives(WriteObject writeObject, Sequence sourceFeature) {
        if (sourceFeature.featureLocations?.size() == 0) return;
        writeObject.out.println(String.format("##sequence-region %s %d %d", sourceFeature.name, sourceFeature.start + 1, sourceFeature.end));
    }

    static private void writeGroupDirectives(WriteObject writeObject, Assemblage assemblage) {
        JSONArray sequenceListArray = JSON.parse(assemblage.sequenceList) as JSONArray
        String formattedAssemblageName = formatAssemblageName(sequenceListArray)
        writeObject.out.println("##sequence-region assemblage ${formattedAssemblageName}")
    }

    static private void writeFeatureGroupEnd(PrintWriter out) {
        out.println("###");
    }

    static private void writeEmptyFastaDirective(PrintWriter out) {
        out.println("##FASTA");
    }

    private void writeFeature(WriteObject writeObject, Feature feature, String source) {
        for (GFF3Entry entry : convertToEntry(writeObject, feature, source)) {
            writeObject.out.println(entry.toString());
        }
    }

    private def writeFeature(WriteObject writeObject, JSONObject jsonFeature, String source) {
        for (GFF3Entry entry : convertToEntry(writeObject, jsonFeature, source)) {
            writeObject.out.println(entry.toString());
        }
    }

    public void writeFasta(WriteObject writeObject, Collection<? extends Feature> features) {
        writeEmptyFastaDirective(writeObject.out);
        for (Feature feature : features) {
            writeFasta(writeObject.out, feature, false);
        }
    }

    def writeFasta(WriteObject writeObject, JSONArray jsonFeatures) {
        writeEmptyFastaDirective(writeObject.out)
        for (JSONObject jsonFeature : jsonFeatures) {
            writeFasta(writeObject.out, jsonFeature, false)
        }
    }

    public void writeFasta(PrintWriter out, Feature feature) {
        writeFasta(out, feature, true);
    }

    def writeFasta(PrintWriter out, JSONObject jsonFeature) {
        writeFasta(out, jsonFeature, true)
    }

    public void writeFasta(PrintWriter out, Feature feature, boolean writeFastaDirective) {
        writeFasta(out, feature, writeFastaDirective, true);
    }

    def writeFasta(PrintWriter out, JSONObject jsonFeature, boolean writeFastaDirective) {
        writeFasta(out, jsonFeature, writeFastaDirective, true)
    }

    public void writeFasta(PrintWriter out, Feature feature, boolean writeFastaDirective, boolean useLocation) {
        int lineLength = 60;
        if (writeFastaDirective) {
            writeEmptyFastaDirective(out);
        }
        String residues = null;
        if (useLocation) {
            residues = sequenceService.getResidueFromFeatureLocation(feature.featureLocation)
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

    def writeFasta(PrintWriter out, JSONObject jsonFeature, boolean writeFastaDirective, boolean useLocation) {
        int lineLength = 60;
        if (writeFastaDirective) {
            writeEmptyFastaDirective(out);
        }
        String residues = null;
        // TODO: fetch residues corresponding to featureLocation on the assemblage
    }

    public void writeFastaForReferenceSequences(WriteObject writeObject, Collection<Sequence> sequences) {
        for (Sequence sequence : sequences) {
            writeFastaForReferenceSequence(writeObject, sequence)
        }
    }

    def writeFastaForAssemblages(WriteObject writeObject, def assemblages) {
        for (Assemblage assemblage : assemblages) {
            writeFastaForAssemblage(writeObject, assemblage)
        }
    }

    public void writeFastaForReferenceSequence(WriteObject writeObject, Sequence sequence) {
        int lineLength = 60;
        String residues = null
        writeEmptyFastaDirective(writeObject.out);
        residues = sequenceService.getRawResiduesFromSequence(sequence, 0, sequence.length)
        if (residues != null) {
            writeObject.out.println(">" + sequence.name);
            int idx = 0;
            while (idx < residues.length()) {
                writeObject.out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())))
                idx += lineLength
            }
        }
    }

    def writeFastaForAssemblage(WriteObject writeObject, Assemblage assemblage) {
        // TODO: write FASTA for an entire assemblage
    }

    public void writeFastaForSequenceAlterations(WriteObject writeObject, Collection<? extends Feature> features) {
        for (Feature feature : features) {
            if (feature instanceof SequenceAlteration) {
                writeFastaForSequenceAlteration(writeObject, feature)
            }
        }
    }

    def writeFastaForSequenceAlterations(WriteObject writeObject, JSONArray jsonFeatures) {
        for (JSONObject jsonFeature : jsonFeatures) {
            if (getType(jsonFeature) in sequenceAlterationList) {
                writeFastaForSequenceAlteration(writeObject, jsonFeature)
            }
        }
    }

    public void writeFastaForSequenceAlteration(WriteObject writeObject, SequenceAlteration sequenceAlteration) {
        int lineLength = 60;
        String residues = null
        residues = sequenceAlteration.getAlterationResidue()
        if (residues != null) {
            writeObject.out.println(">" + sequenceAlteration.name)
            int idx = 0;
            while (idx < residues.length()) {
                writeObject.out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())))
                idx += lineLength
            }
        }
    }

    def writeFastaForSequenceAlteration(WriteObject writeObject, JSONObject sequenceAlteration) {
        int lineLength = 60;
        String residues = null
        residues = sequenceAlteration.get(FeatureStringEnum.RESIDUES.value)
        if (residues != null) {
            writeObject.out.println(">" + sequenceAlteration.get(FeatureStringEnum.NAME.value))
            int idx = 0;
            while (idx < residues.length()) {
                writeObject.out.println(residues.substring(idx, Math.min(idx + lineLength, residues.length())))
                idx += lineLength
            }
        }
    }

    private Collection<GFF3Entry> convertToEntry(WriteObject writeObject, Feature feature, String source) {
        List<GFF3Entry> gffEntries = new ArrayList<GFF3Entry>();
        convertToEntry(writeObject, feature, source, gffEntries);
        return gffEntries;
    }

    private def convertToEntry(WriteObject writeObject, JSONObject jsonFeature, String source) {
        def gffEntries = []
        processCdsForJsonFeature(jsonFeature)
        convertToEntry(writeObject, jsonFeature, source, gffEntries)
        return gffEntries
    }

    @Timed
    private def processCdsForJsonFeature(JSONObject jsonFeature) {
        if (getType(jsonFeature) in topLevelFeatureList) {
            if (jsonFeature.has(FeatureStringEnum.CHILDREN.value) && getType(jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value).getJSONObject(0)) == MRNA.alternateCvTerm) {
                // is a protein coding feature and thus has a CDS that needs to be processed
                JSONArray exons = new JSONArray()
                JSONObject cdsJsonFeature = new JSONObject()
                for (JSONObject mrnaJsonFeature : jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                    for (JSONObject childJsonFeature : mrnaJsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                        if (getType(childJsonFeature) == Exon.alternateCvTerm) {
                            exons.add(childJsonFeature)
                        }
                        else if (getType(childJsonFeature) == CDS.alternateCvTerm) {
                            cdsJsonFeature = childJsonFeature
                        }
                    }
                    boolean hasChildren = cdsJsonFeature.has(FeatureStringEnum.CHILDREN.value)
                    JSONArray cdsChildFeaturesJsonArray
                    if (hasChildren) {
                        // extracting cds child features
                        cdsChildFeaturesJsonArray = cdsJsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                    }

                    JSONObject cdsLocation = cdsJsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value)
                    int length = 0
                    JSONArray cdsPartsJsonArray = new JSONArray()
                    for (JSONObject exon : exons) {
                        JSONObject exonLocation = exon.getJSONObject(FeatureStringEnum.LOCATION.value)
                        JSONObject cdsPartObject = JSON.parse(cdsJsonFeature.toString()) as JSONObject
                        cdsPartObject.remove(FeatureStringEnum.CHILDREN.value)
                        JSONObject cdsPartLocation = new JSONObject()

                        long exonFmin = exonLocation.get(FeatureStringEnum.FMIN.value)
                        long exonFmax = exonLocation.get(FeatureStringEnum.FMAX.value)
                        long cdsFmin = cdsLocation.get(FeatureStringEnum.FMIN.value)
                        long cdsFmax = cdsLocation.get(FeatureStringEnum.FMAX.value)

                        if (!overlapperService.overlaps(exonFmin, exonFmax, cdsFmin, cdsFmax)) {
                            continue
                        }
                        long cdsPartFmin = exonFmin < cdsFmin ? cdsFmin : exonFmin
                        long cdsPartFmax = exonFmax > cdsFmax ? cdsFmax : exonFmax
                        String phase
                        if (length % 3 == 0) {
                            phase = "0"
                        }
                        else if (length % 3 == 1) {
                            phase = "2"
                        }
                        else {
                            phase = "1"
                        }
                        length += cdsPartFmax - cdsPartFmin
                        cdsPartLocation.put(FeatureStringEnum.FMIN.value, cdsPartFmin)
                        cdsPartLocation.put(FeatureStringEnum.FMAX.value, cdsPartFmax)
                        cdsPartLocation.put(FeatureStringEnum.STRAND.value, cdsLocation.get(FeatureStringEnum.STRAND.value))
                        cdsPartLocation.put(FeatureStringEnum.SEQUENCE.value, cdsLocation.get(FeatureStringEnum.SEQUENCE.value))
                        cdsPartObject.put(FeatureStringEnum.LOCATION.value, cdsPartLocation)
                        cdsPartObject.put(FeatureStringEnum.PHASE.value, phase)
                        cdsPartsJsonArray.add(cdsPartObject)
                    }
                    if (hasChildren) {
                        // adding child features to the last CDS part
                        cdsPartsJsonArray.last().put(FeatureStringEnum.CHILDREN.value, cdsChildFeaturesJsonArray)
                    }
                    JSONArray newChildJsonFeaturesArray = new JSONArray()
                    newChildJsonFeaturesArray.addAll(exons)
                    newChildJsonFeaturesArray.addAll(cdsPartsJsonArray)
                    mrnaJsonFeature.put(FeatureStringEnum.CHILDREN.value, newChildJsonFeaturesArray)
                }
            }
        }
    }

    @Timed
    private void convertToEntry(WriteObject writeObject, Feature feature, String source, Collection<GFF3Entry> gffEntries) {

        //log.debug "converting feature to ${feature.name} entry of # of entries ${gffEntries.size()}"
        feature.featureLocations.each { featureLocation ->
            String seqId = featureLocation.sequence.name
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
            entry.setAttributes(extractAttributes(writeObject, feature));
            gffEntries.add(entry);
            if (featureService.typeHasChildren(feature)) {
                for (Feature child : featureRelationshipService.getChildren(feature)) {
                    if (child instanceof CDS) {
                        convertToEntry(writeObject, (CDS) child, source, gffEntries);
                    } else {
                        convertToEntry(writeObject, child, source, gffEntries);
                    }
                }
            }
        }
    }

    private def convertToEntry(WriteObject writeObject, JSONObject jsonFeature, String source, def gffEntries) {
        JSONObject location = jsonFeature.getJSONObject(FeatureStringEnum.LOCATION.value)
        String sequenceListString = location.getString(FeatureStringEnum.SEQUENCE.value)
        String type = jsonFeature.getJSONObject(FeatureStringEnum.TYPE.value).get(FeatureStringEnum.NAME.value)
        int fmin = location.getLong(FeatureStringEnum.FMIN.value)
        int fmax = location.getLong(FeatureStringEnum.FMAX.value)
        long start = fmin + 1
        long end = fmax == fmin ? fmax + 1 : fmax
        String score = ".";
        String strand
        int strandValue = location.getInt(FeatureStringEnum.STRAND.value)
        if (strandValue == Strand.POSITIVE.value) {
            strand = Strand.POSITIVE.display
        }
        else if (strandValue == Strand.NEGATIVE.value) {
            strand = Strand.NEGATIVE.display
        }
        else {
            strand = Strand.NONE.display
        }
        String phase = "."
        if (jsonFeature.has(FeatureStringEnum.PHASE.value)) {
            phase = jsonFeature.get(FeatureStringEnum.PHASE.value)
        }
        String seqIdSequenceList = generateSequenceNameFromSequenceList(JSON.parse(sequenceListString) as JSONArray)
        GFF3Entry entry = new GFF3Entry(seqIdSequenceList, source, type, start, end, score, strand, phase)
        entry.setAttributes(extractAttributes(writeObject, jsonFeature))
        gffEntries.add(entry)
        if (jsonFeature.has(FeatureStringEnum.CHILDREN.value)) {
            for (JSONObject childJsonFeature : jsonFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                convertToEntry(writeObject, childJsonFeature, source, gffEntries)
            }
        }
    }

    @Timed
    private void convertToEntry(WriteObject writeObject, CDS cds, String source, Collection<GFF3Entry> gffEntries) {
        //log.debug "converting CDS to ${cds.name} entry of # of entries ${gffEntries.size()}"

        cds.featureLocations.each { featureLocation ->
            String seqId = featureLocation.sequence.name
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
            Transcript transcript = transcriptService.getParentTranscriptForFeature(cds)

            List<Exon> exons = transcriptService.getSortedExons(transcript, true)
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
                entry.setAttributes(extractAttributes(writeObject, cds));
                gffEntries.add(entry);
            }

            for (Feature child : featureRelationshipService.getChildren(cds)) {
                convertToEntry(writeObject, child, source, gffEntries);
            }
        }

    }

    @Timed
    private Map<String, String> extractAttributes(WriteObject writeObject, Feature feature) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FeatureStringEnum.EXPORT_ID.value, encodeString(feature.getUniqueName()));
        if (feature.getName() != null && !isBlank(feature.getName()) && writeObject.attributesToExport.contains(FeatureStringEnum.NAME.value)) {
            attributes.put(FeatureStringEnum.EXPORT_NAME.value, encodeString(feature.getName()));
        }
        if (!(feature.class.name in requestHandlingService.viewableAnnotationList + requestHandlingService.viewableAlterations)) {
            def parent = featureRelationshipService.getParentForFeature(feature)
            attributes.put(FeatureStringEnum.EXPORT_PARENT.value, encodeString(parent.uniqueName));
        }
        if (configWrapperService.exportSubFeatureAttrs() || feature.class.name in requestHandlingService.viewableAnnotationList + requestHandlingService.viewableAnnotationTranscriptList + requestHandlingService.viewableAlterations) {
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
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DESCRIPTION.value) && feature.getDescription() != null && !isBlank(feature.getDescription())) {

                attributes.put(FeatureStringEnum.DESCRIPTION.value, encodeString(feature.getDescription()));
            }
            if (writeObject.attributesToExport.contains(FeatureStringEnum.STATUS.value) && feature.getStatus() != null) {
                attributes.put(FeatureStringEnum.STATUS.value, encodeString(feature.getStatus().value));
            }
            if (writeObject.attributesToExport.contains(FeatureStringEnum.SYMBOL.value) && feature.getSymbol() != null && !isBlank(feature.getSymbol())) {
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
                        if (prop instanceof Comment) {
                            // ignoring 'comment' as they are already processed earlier
                            continue
                        }
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
                        if (iter.getKey() in unusedStandardAttributes) {
                            attributes.put(encodeString(WordUtils.capitalizeFully(iter.getKey())), iter.getValue().toString());
                        } else {
                            attributes.put(encodeString(WordUtils.uncapitalize(iter.getKey())), iter.getValue().toString());
                        }
                    }
                }
            }
            if (writeObject.attributesToExport.contains(FeatureStringEnum.OWNER.value) && feature.getOwner()) {
                attributes.put(FeatureStringEnum.OWNER.value.toLowerCase(), encodeString(feature.getOwner().username));
            }
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_CREATION.value)) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feature.dateCreated);
                attributes.put(FeatureStringEnum.DATE_CREATION.value, encodeString(formatDate(calendar.time)));
            }
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_LAST_MODIFIED.value)) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feature.lastUpdated);
                attributes.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, encodeString(formatDate(calendar.time)));
            }


            if (feature.class.name in [Insertion.class.name, Substitution.class.name]) {
                attributes.put(FeatureStringEnum.RESIDUES.value, feature.alterationResidue)
            }
        }
        return attributes;
    }

    private def extractAttributes(WriteObject writeObject, JSONObject jsonFeature, String parentUniqueName = null) {
        def attributes = [:]
        String type = jsonFeature.getJSONObject(FeatureStringEnum.TYPE.value).get(FeatureStringEnum.NAME.value)
        attributes.put(FeatureStringEnum.EXPORT_ID.value, encodeString(jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)))
        if (jsonFeature.get(FeatureStringEnum.NAME.value) != null && writeObject.attributesToExport.contains(FeatureStringEnum.NAME.value)) {
            attributes.put(FeatureStringEnum.EXPORT_NAME.value, encodeString(jsonFeature.get(FeatureStringEnum.NAME.value)))
        }
        if (!(type in topLevelFeatureList)) {
            if (parentUniqueName) {
                attributes.put(FeatureStringEnum.EXPORT_PARENT.value, encodeString(parentUniqueName));
            }
        }

        if (configWrapperService.exportSubFeatureAttrs() || type in topLevelFeatureList + transcriptList + sequenceAlterationList) {
            // symbol
            if (writeObject.attributesToExport.contains(FeatureStringEnum.SYMBOL.value) && jsonFeature.has(FeatureStringEnum.SYMBOL.value)) {
                attributes.put(FeatureStringEnum.SYMBOL.value, encodeString(jsonFeature.get(FeatureStringEnum.SYMBOL.value)))
            }

            // description
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DESCRIPTION.value) && jsonFeature.has(FeatureStringEnum.DESCRIPTION.value)) {
                attributes.put(FeatureStringEnum.DESCRIPTION.value, encodeString(jsonFeature.get(FeatureStringEnum.DESCRIPTION.value)))
            }

            // status
            if (writeObject.attributesToExport.contains(FeatureStringEnum.STATUS.value) && jsonFeature.has(FeatureStringEnum.STATUS.value)) {
                attributes.put(FeatureStringEnum.STATUS.value, encodeString(jsonFeature.get(FeatureStringEnum.STATUS.value)))
            }

            // DBXrefs
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DBXREFS.value) && jsonFeature.has(FeatureStringEnum.DBXREFS.value)) {
                JSONArray featureDbxrefsArray = jsonFeature.getJSONArray(FeatureStringEnum.DBXREFS.value)
                def dbxrefArray = []
                for (int i = 0; i < featureDbxrefsArray.length(); i++) {
                    JSONObject dbxrefObject = featureDbxrefsArray.getJSONObject(i)
                    String db = dbxrefObject.getJSONObject(FeatureStringEnum.DB.value).get(FeatureStringEnum.NAME.value)
                    String accession = dbxrefObject.get(FeatureStringEnum.ACCESSION.value)
                    dbxrefArray.add(encodeString(db) + ":" + encodeString(accession))
                }
                attributes.put(FeatureStringEnum.EXPORT_DBXREF.value, dbxrefArray.sort(true).join(","))
            }

            // feature properties
            if (jsonFeature.has(FeatureStringEnum.PROPERTIES.value)) {
                JSONArray featurePropertiesArray = jsonFeature.getJSONArray(FeatureStringEnum.PROPERTIES.value)
                def attributesMap = [:]
                def commentsArray = []
                for (int i = 0; i < featurePropertiesArray.length(); i++) {
                    JSONObject featurePropertyObject = featurePropertiesArray.getJSONObject(i)
                    String tag = featurePropertyObject.get(FeatureStringEnum.NAME.value)
                    String value = featurePropertyObject.get(FeatureStringEnum.VALUE.value)
                    if (tag == FeatureStringEnum.COMMENT.value) {
                        commentsArray.add(encodeString(value))
                    }
                    else {
                        if (attributesMap.containsKey(tag)) {
                            attributesMap.get(tag).add(encodeString(value))
                        }
                        else {
                            attributesMap.put(tag, [encodeString(value)])
                        }
                    }
                }

                attributesMap.each {
                    if (it.key in unusedStandardAttributes) {
                        attributes.put(encodeString(WordUtils.capitalizeFully(it.key)), it.value.join(","))
                    }
                    else {
                        attributes.put(encodeString(WordUtils.uncapitalize(it.key)), it.value.join(","))
                    }
                }

                // Comments
                if (writeObject.attributesToExport.contains(FeatureStringEnum.COMMENTS.value)) {
                    attributes.put(FeatureStringEnum.EXPORT_NOTE.value, commentsArray.join(","))
                }
            }

            // Owner
            if (writeObject.attributesToExport.contains(FeatureStringEnum.OWNER.value) && jsonFeature.has(FeatureStringEnum.OWNER.value.toLowerCase())) {
                attributes.put(FeatureStringEnum.OWNER.value.toLowerCase(), encodeString(jsonFeature.get(FeatureStringEnum.OWNER.value.toLowerCase())))
            }

            // date creation
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_CREATION.value)) {
                Date date = new Date(jsonFeature.getLong(FeatureStringEnum.DATE_CREATION.value));
                attributes.put(FeatureStringEnum.DATE_CREATION.value, encodeString(formatDate(date)))
            }
            // date last modified
            if (writeObject.attributesToExport.contains(FeatureStringEnum.DATE_LAST_MODIFIED.value)) {
                Date date = new Date(jsonFeature.getLong(FeatureStringEnum.DATE_LAST_MODIFIED.value))
                attributes.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, encodeString(formatDate(date)))
            }

            if (writeObject.attributesToExport.contains(FeatureStringEnum.SYNONYMS.value)) {
                // TODO
            }

            // TODO: Target
            // TODO: Gap

            if (type in [Insertion.alternateCvTerm, Substitution.alternateCvTerm]) {
                attributes.put(FeatureStringEnum.RESIDUES.value, jsonFeature.get(FeatureStringEnum.RESIDUES.value))
            }
        }
        return attributes
    }

    static private def formatAssemblageName(JSONArray sequenceListArray) {
        def sequenceNames = []
        for (int i = 0; i < sequenceListArray.size(); i++) {
            JSONObject sequenceObject = sequenceListArray.getJSONObject(i)
            String sequenceName = sequenceObject.get(FeatureStringEnum.NAME.value)
            sequenceName += ":"
            sequenceObject.get(FeatureStringEnum.REVERSE.value) ? (sequenceName += "REVERSE") : (sequenceName += "REFERENCE")
            sequenceName += ":"
            sequenceName += sequenceObject.get(FeatureStringEnum.START.value) + 1 + "-" + sequenceObject.get(FeatureStringEnum.END.value)
            sequenceNames.add(sequenceName)
        }
        return sequenceNames.join(" ")
    }

    private def generateSequenceNameFromSequenceList(JSONArray sequenceListArray) {
        def sequenceNames = []
        for (int i = 0; i < sequenceListArray.size(); i++) {
            JSONObject sequenceObject = sequenceListArray.getJSONObject(i)
            sequenceNames.add(sequenceObject.get(FeatureStringEnum.NAME.value))
        }
        return sequenceNames.join("-")
    }

    private def getType(JSONObject jsonFeature) {
        String type = null
        if (jsonFeature.has(FeatureStringEnum.TYPE.value)) {
            type = jsonFeature.getJSONObject(FeatureStringEnum.TYPE.value).get(FeatureStringEnum.NAME.value)
        }
        return type
    }

    String formatDate(Date date) {
        return gff3DateFormat.format(date)
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
        } else {
            return false
        }
    }

    private class WriteObject {
        File file;
        PrintWriter out;
        Mode mode;
        Set<String> attributesToExport = new HashSet<>();
        Format format;
    }

}
