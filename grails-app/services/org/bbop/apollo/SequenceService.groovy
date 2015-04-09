package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.transaction.Transactional
import org.apache.commons.lang.RandomStringUtils
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.StandardTranslationTable
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

import java.util.zip.CRC32

@Transactional
class SequenceService {
    
    def configWrapperService
    def grailsApplication
    def featureService
    def transcriptService
    def exonService
    def cdsService
    def gff3HandlerService

    List<FeatureLocation> getFeatureLocations(Sequence sequence){
        FeatureLocation.findAllBySequence(sequence)
    }

    /**
     * Get residues from sequence . . . could be multiple locations
     * @param feature
     * @return
     */
    String getResiduesFromFeature(Feature feature ) {
        List<FeatureLocation> featureLocationList = FeatureLocation.createCriteria().list {
            eq("feature",feature)
            order("fmin","asc")
        }
        String returnResidue = ""

        for(FeatureLocation featureLocation in featureLocationList){
            returnResidue += getResidueFromFeatureLocation(featureLocation)
        }
        
        if(featureLocationList.first().strand==Strand.NEGATIVE.value){
            returnResidue = SequenceTranslationHandler.reverseComplementSequence(returnResidue)
        }

        return returnResidue
    }

    String getResidueFromFeatureLocation(FeatureLocation featureLocation) {
        return getResiduesFromSequence(featureLocation.sequence,featureLocation.fmin,featureLocation.fmax)
    }

    String getResiduesFromSequence(Sequence sequence, int fmin, int fmax) {
        StringBuilder sequenceString = new StringBuilder()

        int startChunkNumber = fmin / sequence.seqChunkSize;
        int endChunkNumber = (fmax - 1 ) / sequence.seqChunkSize;

        for(int i = startChunkNumber ; i<= endChunkNumber ; i++){
            SequenceChunk sequenceChunk = getSequenceChunkForChunk(sequence,i)
            sequenceString.append(sequenceChunk.residue)
        }

        // TODO: optimize
        //   SequenceChunk.findAllBySequenceAndChunkNumberGreaterThanAndChunkNumberLessThanEquals(sequence,startChunkNumber,endChunkNumber)["order":"chunkNumber"].collect(){ it -> it.residue}

        int startPosition = fmin - (startChunkNumber * sequence.seqChunkSize);

//        if(grails.util.Environment.current == grails.util.Environment.TEST){
//            return sequenceString
//        }

        return sequenceString.substring(startPosition,startPosition + (fmax-fmin))
    }

    SequenceChunk getSequenceChunkForChunk(Sequence sequence, int i) {
        SequenceChunk sequenceChunk = SequenceChunk.findBySequenceAndChunkNumber(sequence,i)
        if(!sequenceChunk){
            String residue = loadResidueForSequence(sequence,i)
            log.debug "RESIDUE load: ${residue?.size()}"
            sequenceChunk = new SequenceChunk(
                    sequence: sequence
                    ,chunkNumber: i
                    ,residue: residue
            ).save(flush:true)
        }
        log.debug "RESIDUE loaded from DB: ${sequenceChunk.residue?.size()}"
        return sequenceChunk
    }

    private static String generatorSampleDNA(int size){
        return RandomStringUtils.random(size,['A','T','C','G'] as char[])
    }
    

    String loadResidueForSequence(Sequence sequence, int chunkNumber) {
      
//        if(grails.util.Environment.current == grails.util.Environment.TEST){
//            return generatorSampleDNA(chunkNumber)
//        }
        
        String filePath = sequence.sequenceDirectory + "/" + sequence.seqChunkPrefix + chunkNumber + ".txt"

        return new File(filePath).getText().toUpperCase()
    }

    private String[] splitStringByNumberOfCharacters(String str, int numOfChars) {
        int numTokens = str.length() / numOfChars;
        if (str.length() % numOfChars != 0) {
            ++numTokens;
        }
        String []tokens = new String[numTokens];
        int idx = 0;
        for (int i = 0; i < numTokens; ) {
            tokens[i] = str.substring(idx, idx + numOfChars < str.length() ? idx + numOfChars : str.length());
            ++i
            idx += numOfChars
        }
        return tokens;
    }

    private JSONArray convertJBrowseJSON(InputStream inputStream) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder buffer = new StringBuilder();
        String line;
//        reader.readLine();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return new JSONArray(buffer.toString());
    }

    def loadRefSeqs(Organism organism ) {
        log.info "loading refseq ${organism.refseqFile}"
        organism.valid = false ;
        organism.save(flush: true, failOnError: true,insert:false)

        File refSeqsFile = new File(organism.refseqFile);
        log.info " file exists ${refSeqsFile.exists()}"
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(refSeqsFile));
        JSONArray refSeqs = convertJBrowseJSON(bufferedInputStream);
        log.debug "freseq length ${refSeqs.size()}"
        // delete all sequence for the organism
        Sequence.deleteAll(Sequence.findAllByOrganism(organism))
        for (int i = 0; i < refSeqs.length(); ++i) {
            JSONObject refSeq = refSeqs.getJSONObject(i);
            int length = refSeq.getInt("length");
            String name = refSeq.getString("name");
            String seqDir;
            String seqChunkPrefix = "";
            if (refSeq.has("seqDir")) {
                seqDir = refSeqsFile.getParent() + "/" + refSeq.getString("seqDir");
            }
            else {
                CRC32 crc = new CRC32();
                crc.update(name.getBytes());
                String hex = String.format("%08x", crc.getValue());
                String []dirs = splitStringByNumberOfCharacters(hex, 3);
                seqDir = String.format("%s/%s/%s/%s", refSeqsFile.getParent(), dirs[0], dirs[1], dirs[2]);
                seqChunkPrefix = name + "-";
            }
            int seqChunkSize = refSeq.getInt("seqChunkSize");
            int start = refSeq.getInt("start");
            int end = refSeq.getInt("end");



            Sequence sequence = new Sequence(
                   organism: organism
                   ,length: length
                    ,refSeqFile: organism.refseqFile
                    ,seqChunkPrefix: seqChunkPrefix
                    ,seqChunkSize: seqChunkSize
                    ,start: start
                    ,end: end
//                    ,dataDirectory: refSeqsFile.getParentFile().getParent()
                    ,sequenceDirectory: seqDir
                    ,name: name
            ).save(failOnError: true)


//            SourceFeatureConfiguration sourceFeature = new SourceFeatureConfiguration(seqDir, seqChunkSize, seqChunkPrefix, length, name, sequenceType, start, end);
//
//            TrackConfiguration c = new TrackConfiguration(annotationTrackName + "-" + name, organism, translationTable, sourceFeature, spliceDonorSites, spliceAcceptorSites);
//            tracks.put(name, c);
//            tracks.put(name, new TrackConfiguration(annotationTrackName + "-" + name, organism, translationTable, sourceFeature));
        }

        organism.valid = true
        organism.save(flush: true,insert:false,failOnError: true)

        bufferedInputStream.close();
    }

    def setResiduesForFeature(SequenceAlteration sequenceAlteration, String residue) {
        sequenceAlteration.alterationResidue = residue
    }

    def setResiduesForFeatureFromLocation(Deletion deletion) {
        FeatureLocation featureLocation = deletion.featureLocation
        deletion.alterationResidue = getResidueFromFeatureLocation(featureLocation)
    }
    
    def getSequenceForFeature(JSONObject inputObject, File outputFile=null) {
        println "===> input at getSequenceForFeature: ${inputObject}"
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String type = inputObject.getString(FeatureStringEnum.TYPE.value)
        StandardTranslationTable standardTranslationTable = new StandardTranslationTable()

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature gbolFeature = Feature.findByUniqueName(uniqueName)
            String sequence = null
            if (type.equals(FeatureStringEnum.TYPE_PEPTIDE.value)) {
                if (gbolFeature instanceof Transcript && transcriptService.isProteinCoding((Transcript) gbolFeature)) {
                    CDS cds = transcriptService.getCDS((Transcript) gbolFeature)
                    String rawSequence = featureService.getResiduesWithAlterationsAndFrameshifts(cds)
                    sequence = SequenceTranslationHandler.translateSequence(rawSequence, standardTranslationTable, true, cdsService.getStopCodonReadThrough(cds) != null)
                    if (sequence.charAt(sequence.size() - 1) == StandardTranslationTable.STOP.charAt(0)) {
                        sequence = sequence.substring(0, sequence.size() - 1)
                    }
                    int idx;
                    if ((idx = sequence.indexOf(StandardTranslationTable.STOP)) != -1) {
                        String codon = rawSequence.substring(idx * 3, idx * 3 + 3)
                        String aa = configWrapperService.getTranslationTable().getAlternateTranslationTable().get(codon)
                        if (aa != null) {
                            sequence = sequence.replace(StandardTranslationTable.STOP, aa)
                        }
                    }
                } else if (gbolFeature instanceof Exon && transcriptService.isProteinCoding(exonService.getTranscript((Exon) gbolFeature))) {
                    println "===> trying to fetch PEPTIDE sequence of selected exon: ${gbolFeature}"
                    String rawSequence = exonService.getCodingSequenceInPhase((Exon) gbolFeature, true)
                    sequence = SequenceTranslationHandler.translateSequence(rawSequence, standardTranslationTable, true, cdsService.getStopCodonReadThrough(transcriptService.getCDS(exonService.getTranscript((Exon) gbolFeature))) != null)
                    if (sequence.charAt(sequence.length() - 1) == StandardTranslationTable.STOP.charAt(0)) {
                        sequence = sequence.substring(0, sequence.length() - 1)
                    }
                    int idx
                    if ((idx = sequence.indexOf(StandardTranslationTable.STOP)) != -1) {
                        String codon = rawSequence.substring(idx * 3, idx * 3 + 3)
                        String aa = configWrapperService.getTranslationTable().getAlternateTranslationTable().get(codon)
                        if (aa != null) {
                            sequence = sequence.replace(StandardTranslationTable.STOP, aa)
                        }
                    }
                } else {
                    sequence = ""
                }
            } else if (type.equals(FeatureStringEnum.TYPE_CDS.value)) {
                if (gbolFeature instanceof Transcript && transcriptService.isProteinCoding((Transcript) gbolFeature)) {
                    sequence = featureService.getResiduesWithAlterationsAndFrameshifts(transcriptService.getCDS((Transcript) gbolFeature))
                } else if (gbolFeature instanceof Exon && transcriptService.isProteinCoding(exonService.getTranscript((Exon) gbolFeature))) {
                    println "trying to fetch CDS sequence of selected exon: ${gbolFeature}"
                    sequence = exonService.getCodingSequenceInPhase((Exon) gbolFeature, false)
                } else {
                    sequence = ""
                }

            } else if (type.equals(FeatureStringEnum.TYPE_CDNA.value)) {
                if (gbolFeature instanceof Transcript || gbolFeature instanceof Exon) {
                    sequence = featureService.getResiduesWithAlterationsAndFrameshifts(gbolFeature)
                } else {
                    sequence = ""
                }
            } else if (type.equals(FeatureStringEnum.TYPE_GENOMIC.value)) {
                int flank
                if (inputObject.has('flank')) {
                    flank = inputObject.getInt("flank")
                    println "FLANK from request object: ${flank}"
                } else {
                    flank = 0
                }

                if (flank > 0) {
                    int fmin = gbolFeature.getFmin() - flank
                    if (fmin < 0) {
                        fmin = 0
                    }
                    if (fmin < gbolFeature.getFeatureLocation().sequence.start) {
                        fmin = gbolFeature.getFeatureLocation().sequence.start
                    }
                    int fmax = gbolFeature.getFmax() + flank
                    if (fmax > gbolFeature.getFeatureLocation().sequence.length) {
                        fmax = gbolFeature.getFeatureLocation().sequence.length
                    }
                    if (fmax > gbolFeature.getFeatureLocation().sequence.end) {
                        fmax = gbolFeature.getFeatureLocation().sequence.end
                    }

                    FlankingRegion genomicRegion = new FlankingRegion(
                            name: gbolFeature.name
                            , uniqueName: gbolFeature.uniqueName + "_flank"
                    ).save()
                    FeatureLocation genomicRegionLocation = new FeatureLocation(
                            feature: genomicRegion
                            , fmin: fmin // fmin with the flank
                            , fmax: fmax // fmax with the flank
                            , strand: gbolFeature.strand
                            , sequence: gbolFeature.getFeatureLocation().sequence
                    ).save()
                    genomicRegion.addToFeatureLocations(genomicRegionLocation)
                    // since we are saving the genomicFeature object, the backend database will have these entities
                    gbolFeature = genomicRegion
                }
                sequence = getResiduesFromFeature(gbolFeature)
            }
            JSONObject outFeature = featureService.convertFeatureToJSON(gbolFeature)
            outFeature.put("residues", sequence)
            outFeature.put("uniquename", uniqueName)
            return outFeature
        }
    }
    
    def getGff3ForFeature(JSONObject inputObject, File outputFile) {
        // File tempFile = File.createTempFile("feature", ".gff3");
        // TODO: use specified metadata?
        Set<String> metaDataToExport = new HashSet<>();
        metaDataToExport.add(FeatureStringEnum.NAME.value);
        metaDataToExport.add(FeatureStringEnum.SYMBOL.value);
        metaDataToExport.add(FeatureStringEnum.DESCRIPTION.value);
        metaDataToExport.add(FeatureStringEnum.STATUS.value);
        metaDataToExport.add(FeatureStringEnum.DBXREFS.value);
        metaDataToExport.add(FeatureStringEnum.ATTRIBUTES.value);
        metaDataToExport.add(FeatureStringEnum.PUBMEDIDS.value);
        metaDataToExport.add(FeatureStringEnum.GOIDS.value);
        metaDataToExport.add(FeatureStringEnum.COMMENTS.value);
        
        List<Feature> featuresToWrite = new ArrayList<>();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            String uniqueName = jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value);
            Feature gbolFeature = Feature.findByUniqueName(uniqueName)
            gbolFeature = featureService.getTopLevelFeature(gbolFeature)
            featuresToWrite.add(gbolFeature);
        }
        gff3HandlerService.writeFeaturesToText(outputFile.absolutePath, featuresToWrite, grailsApplication.config.apollo.gff3.source as String)
    }
}
