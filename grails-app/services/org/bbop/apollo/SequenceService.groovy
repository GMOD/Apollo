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
    def overlapperService

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
        return getRawResiduesFromSequence(featureLocation.sequence,featureLocation.fmin,featureLocation.fmax)
    }


    String getGenomicResiduesFromSequenceWithAlterations(FlankingRegion flankingRegion) {
        return getGenomicResiduesFromSequenceWithAlterations(flankingRegion.sequence,flankingRegion.fmin,flankingRegion.fmax,flankingRegion.strand)
    }

    /**
     * Just meant for non-transcript genomic sequence
     * @param sequence
     * @param fmin
     * @param fmax
     * @param strand
     * @return
     */
    String getGenomicResiduesFromSequenceWithAlterations(Sequence sequence, int fmin, int fmax,Strand strand) {
        String residueString = getRawResiduesFromSequence(sequence,fmin,fmax)
        if(strand==Strand.NEGATIVE){
            residueString = SequenceTranslationHandler.reverseComplementSequence(residueString)
        }

        StringBuilder residues = new StringBuilder(residueString);
        List<SequenceAlteration> sequenceAlterationList = SequenceAlteration.executeQuery("select distinct sa from SequenceAlteration sa join sa.featureLocations fl join fl.sequence seq where seq.id = :seqId ",[seqId:sequence.id])
        int currentOffset = 0;
        // TODO: refactor with getResidues in FeatureService so we are calling a similar method
        for(SequenceAlteration sequenceAlteration in sequenceAlterationList){
            int localCoordinate = featureService.convertSourceCoordinateToLocalCoordinate(fmin,fmax,strand, sequenceAlteration.featureLocation.fmin);
            if(!overlapperService.overlaps(fmin,fmax,sequenceAlteration.featureLocation.fmin,sequenceAlteration.featureLocation.fmax)){
                continue
            }

            // TODO: is this correct?
            String sequenceAlterationResidues = sequenceAlteration.alterationResidue
            if (strand == Strand.NEGATIVE) {
                sequenceAlterationResidues = SequenceTranslationHandler.reverseComplementSequence(sequenceAlterationResidues);
            }
            // Insertions
            if (sequenceAlteration instanceof Insertion) {
                if (strand==Strand.NEGATIVE) {
                    ++localCoordinate;
                }
                residues.insert(localCoordinate + currentOffset, sequenceAlterationResidues);
                currentOffset += sequenceAlterationResidues.length();
            }
            // Deletions
            else if (sequenceAlteration instanceof Deletion) {
                if (strand == Strand.NEGATIVE) {
                    residues.delete(localCoordinate + currentOffset - sequenceAlteration.getLength() + 1,
                            localCoordinate + currentOffset + 1);
                } else {
                    residues.delete(localCoordinate + currentOffset,
                            localCoordinate + currentOffset + sequenceAlteration.getLength());
                }
                currentOffset -= sequenceAlterationResidues.length();
            }
            // Substitions
            else if (sequenceAlteration instanceof Substitution) {
                int start = strand == Strand.NEGATIVE ? localCoordinate - (sequenceAlteration.getLength() - 1) : localCoordinate;
                residues.replace(start + currentOffset,
                        start + currentOffset + sequenceAlteration.getLength(),
                        sequenceAlterationResidues);
            }
        }


        return residues.toString()
    }

    String getRawResiduesFromSequence(Sequence sequence, int fmin, int fmax) {
        StringBuilder sequenceString = new StringBuilder()

        int startChunkNumber = fmin / sequence.seqChunkSize;
        int endChunkNumber = (fmax - 1 ) / sequence.seqChunkSize;

        for(int i = startChunkNumber ; i<= endChunkNumber ; i++){
            SequenceChunk sequenceChunk = getSequenceChunkForChunk(sequence,i)
            sequenceString.append(sequenceChunk.residue)
        }


        int startPosition = fmin - (startChunkNumber * sequence.seqChunkSize);

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
        log.debug "refseq length ${refSeqs.size()}"
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
                    ,sequenceDirectory: seqDir
                    ,name: name
            ).save(failOnError: true)


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
    
    
    def getSequenceForFeature(Feature gbolFeature, String type, int flank = 0) {
        // Method returns the sequence for a single feature
        // Directly called for FASTA Export
        String featureResidues = null
        StandardTranslationTable standardTranslationTable = new StandardTranslationTable()
        
        if (type.equals(FeatureStringEnum.TYPE_PEPTIDE.value)) {
            if (gbolFeature instanceof Transcript && transcriptService.isProteinCoding((Transcript) gbolFeature)) {
                CDS cds = transcriptService.getCDS((Transcript) gbolFeature)
                String rawSequence = featureService.getResiduesWithAlterationsAndFrameshifts(cds)
                featureResidues = SequenceTranslationHandler.translateSequence(rawSequence, standardTranslationTable, true, cdsService.getStopCodonReadThrough(cds) != null)
                if (featureResidues.charAt(featureResidues.size() - 1) == StandardTranslationTable.STOP.charAt(0)) {
                    featureResidues = featureResidues.substring(0, featureResidues.size() - 1)
                }
                int idx;
                if ((idx = featureResidues.indexOf(StandardTranslationTable.STOP)) != -1) {
                    String codon = rawSequence.substring(idx * 3, idx * 3 + 3)
                    String aa = configWrapperService.getTranslationTable().getAlternateTranslationTable().get(codon)
                    if (aa != null) {
                        featureResidues = featureResidues.replace(StandardTranslationTable.STOP, aa)
                    }
                }
            } else if (gbolFeature instanceof Exon && transcriptService.isProteinCoding(exonService.getTranscript((Exon) gbolFeature))) {
                log.debug "Fetching peptide sequence for selected exon: ${gbolFeature}"
                String rawSequence = exonService.getCodingSequenceInPhase((Exon) gbolFeature, true)
                featureResidues = SequenceTranslationHandler.translateSequence(rawSequence, standardTranslationTable, true, cdsService.getStopCodonReadThrough(transcriptService.getCDS(exonService.getTranscript((Exon) gbolFeature))) != null)
                if (featureResidues.length()>0 && featureResidues.charAt(featureResidues.length() - 1) == StandardTranslationTable.STOP.charAt(0)) {
                    featureResidues = featureResidues.substring(0, featureResidues.length() - 1)
                }
                int idx
                if ((idx = featureResidues.indexOf(StandardTranslationTable.STOP)) != -1) {
                    String codon = rawSequence.substring(idx * 3, idx * 3 + 3)
                    String aa = configWrapperService.getTranslationTable().getAlternateTranslationTable().get(codon)
                    if (aa != null) {
                        featureResidues = featureResidues.replace(StandardTranslationTable.STOP, aa)
                    }
                }
            } else {
                featureResidues = ""
            }
        } else if (type.equals(FeatureStringEnum.TYPE_CDS.value)) {
            if (gbolFeature instanceof Transcript && transcriptService.isProteinCoding((Transcript) gbolFeature)) {
                featureResidues = featureService.getResiduesWithAlterationsAndFrameshifts(transcriptService.getCDS((Transcript) gbolFeature))
            } else if (gbolFeature instanceof Exon && transcriptService.isProteinCoding(exonService.getTranscript((Exon) gbolFeature))) {
                log.debug "Fetching CDS sequence for selected exon: ${gbolFeature}"
                featureResidues = exonService.getCodingSequenceInPhase((Exon) gbolFeature, false)
            } else {
                featureResidues = ""
            }

        } else if (type.equals(FeatureStringEnum.TYPE_CDNA.value)) {
            if (gbolFeature instanceof Transcript || gbolFeature instanceof Exon) {
                featureResidues = featureService.getResiduesWithAlterationsAndFrameshifts(gbolFeature)
            } else {
                featureResidues = ""
            }
        } else if (type.equals(FeatureStringEnum.TYPE_GENOMIC.value)) {

            int fmin = gbolFeature.getFmin() - flank
            int fmax = gbolFeature.getFmax() + flank

            if (flank > 0) {
                if (fmin < 0) {
                    fmin = 0
                }
                if (fmin < gbolFeature.getFeatureLocation().sequence.start) {
                    fmin = gbolFeature.getFeatureLocation().sequence.start
                }
                if (fmax > gbolFeature.getFeatureLocation().sequence.length) {
                    fmax = gbolFeature.getFeatureLocation().sequence.length
                }
                if (fmax > gbolFeature.getFeatureLocation().sequence.end) {
                    fmax = gbolFeature.getFeatureLocation().sequence.end
                }

            }
//            FlankingRegion genomicRegion = new FlankingRegion(
//                    name: gbolFeature.name
//                    , uniqueName: gbolFeature.uniqueName + "_flank"
//            ).save()
//            FeatureLocation genomicRegionLocation = new FeatureLocation(
//                    feature: genomicRegion
//                    , fmin: fmin
//                    , fmax: fmax
//                    , strand: gbolFeature.strand
//                    , sequence: gbolFeature.getFeatureLocation().sequence
//            ).save()
//            genomicRegion.addToFeatureLocations(genomicRegionLocation)
            // since we are saving the genomicFeature object, the backend database will have these entities
//            gbolFeature = genomicRegion
            //sequence = getResiduesFromFeature(gbolFeature)
//            featureResidues = featureService.getResiduesWithAlterationsAndFrameshifts(gbolFeature)
            featureResidues = getGenomicResiduesFromSequenceWithAlterations(gbolFeature.featureLocation.sequence,fmin,fmax,Strand.getStrandForValue(gbolFeature.strand))
        }
        return featureResidues
    }
    
    def getSequenceForFeatures(JSONObject inputObject, File outputFile=null) {
        // Method returns a JSONObject 
        // Suitable for 'get sequence' operation from AEC
        log.debug "input at getSequenceForFeature: ${inputObject}"
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        String type = inputObject.getString(FeatureStringEnum.TYPE.value)
        int flank
        if (inputObject.has('flank')) {
            flank = inputObject.getInt("flank")
            log.debug "flank from request object: ${flank}"
        } else {
            flank = 0
        }

        for (int i = 0; i < featuresArray.length(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i)
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature gbolFeature = Feature.findByUniqueName(uniqueName)
            String sequence = getSequenceForFeature(gbolFeature, type, flank)

            JSONObject outFeature = featureService.convertFeatureToJSON(gbolFeature)
            outFeature.put("residues", sequence)
            outFeature.put("uniquename", uniqueName)
            return outFeature
        }
    }
    
    def getGff3ForFeature(JSONObject inputObject, File outputFile) {
        List<Feature> featuresToWrite = new ArrayList<>();
        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        def sequenceAlterationTypes = [Insertion.class.canonicalName, Deletion.class.canonicalName, Substitution.class.canonicalName]
        for (int i = 0; i < features.length(); ++i) {
            JSONObject jsonFeature = features.getJSONObject(i);
            String uniqueName = jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value);
            Feature gbolFeature = Feature.findByUniqueName(uniqueName)
            gbolFeature = featureService.getTopLevelFeature(gbolFeature)
            featuresToWrite.add(gbolFeature);

            int fmin = gbolFeature.fmin
            int fmax = gbolFeature.fmax

            Sequence sequence = gbolFeature.featureLocation.sequence

            // TODO: does strand and alteration length matter here?
            List<Feature> listOfSequenceAlterations = Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl join fl.sequence s where s = :sequence and f.class in :sequenceTypes and fl.fmin >= :fmin and fl.fmax <= :fmax ", [sequence: sequence, sequenceTypes: sequenceAlterationTypes,fmin:fmin,fmax:fmax])
            featuresToWrite += listOfSequenceAlterations
        }
        gff3HandlerService.writeFeaturesToText(outputFile.absolutePath, featuresToWrite, grailsApplication.config.apollo.gff3.source as String)
    }
}
