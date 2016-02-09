package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.transaction.Transactional
import org.apache.commons.lang.RandomStringUtils
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.StandardTranslationTable
import org.bbop.apollo.sequence.Strand
import org.bbop.apollo.alteration.SequenceAlterationInContext
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import groovy.json.JsonSlurper

import java.util.zip.CRC32

@Transactional
class SequenceService {
    
    def configWrapperService
    def grailsApplication
    def featureService
    def transcriptService
    def requestHandlingService
    def exonService
    def cdsService
    def gff3HandlerService
    def overlapperService
    def sessionFactory


    List<FeatureLocation> getFeatureLocations(Sequence sequence){
        FeatureLocation.findAllBySequence(sequence)
    }

    /**
     * Get residues from sequence . . . could be multiple locations
     * @param feature
     * @return
     */
    String getResiduesFromFeature(Feature feature) {
        String returnResidues = ""
        def orderedFeatureLocations = feature.featureLocations.sort { it.fmin }
        for(FeatureLocation featureLocation in orderedFeatureLocations) {
            String residues = getResidueFromFeatureLocation(featureLocation)
            if(featureLocation.strand == Strand.NEGATIVE.value) {
                returnResidues += SequenceTranslationHandler.reverseComplementSequence(residues)
            }
            else returnResidues += residues
        }


        return returnResidues
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
        List<SequenceAlterationInContext> sequenceAlterationsInContextList = new ArrayList<SequenceAlterationInContext>()
        for (SequenceAlteration sequenceAlteration : sequenceAlterationList) {
            int alterationFmin = sequenceAlteration.fmin
            int alterationFmax = sequenceAlteration.fmax
            SequenceAlterationInContext sa = new SequenceAlterationInContext()
            if ((alterationFmin >= fmin && alterationFmax <= fmax) && (alterationFmax >= fmin && alterationFmax <= fmax)) {
                // alteration is within the generic feature
                sa.fmin = alterationFmin
                sa.fmax = alterationFmax
                if (sequenceAlteration instanceof Insertion) {
                    sa.instanceOf = Insertion.canonicalName
                }
                else if (sequenceAlteration instanceof Deletion) {
                    sa.instanceOf = Deletion.canonicalName
                }
                else if (sequenceAlteration instanceof Substitution) {
                    sa.instanceOf = Substitution.canonicalName
                }
                sa.type = 'within'
                sa.strand = sequenceAlteration.strand
                sa.name = sequenceAlteration.name + '-inContext'
                sa.originalAlterationUniqueName = sequenceAlteration.uniqueName
                sa.offset = sequenceAlteration.offset
                sa.alterationResidue = sequenceAlteration.alterationResidue
                sequenceAlterationsInContextList.add(sa)
            }
            else if ((alterationFmin >= fmin && alterationFmin <= fmax) && (alterationFmax >= fmin && alterationFmax >= fmax)) {
                // alteration starts in exon but ends in an intron
                int difference = alterationFmax - fmax
                sa.fmin = alterationFmin
                sa.fmax = Math.min(fmax,alterationFmax)
                if (sequenceAlteration instanceof Insertion) {
                    sa.instanceOf = Insertion.canonicalName
                }
                else if (sequenceAlteration instanceof Deletion) {
                    sa.instanceOf = Deletion.canonicalName
                }
                else if (sequenceAlteration instanceof Substitution) {
                    sa.instanceOf = Substitution.canonicalName
                }
                sa.type = 'exon-to-intron'
                sa.strand = sequenceAlteration.strand
                sa.name = sequenceAlteration.name + '-inContext'
                sa.originalAlterationUniqueName = sequenceAlteration.uniqueName
                sa.offset = sequenceAlteration.offset - difference
                sa.alterationResidue = sequenceAlteration.alterationResidue.substring(0, sequenceAlteration.alterationResidue.length() - difference)
                sequenceAlterationsInContextList.add(sa)
            }
            else if ((alterationFmin <= fmin && alterationFmin <= fmax) && (alterationFmax >= fmin && alterationFmax <= fmax)) {
                // alteration starts within intron but ends in an exon
                int difference = fmin - alterationFmin
                sa.fmin = Math.max(fmin, alterationFmin)
                sa.fmax = alterationFmax
                if (sequenceAlteration instanceof Insertion) {
                    sa.instanceOf = Insertion.canonicalName
                }
                else if (sequenceAlteration instanceof Deletion) {
                    sa.instanceOf = Deletion.canonicalName
                }
                else if (sequenceAlteration instanceof Substitution) {
                    sa.instanceOf = Substitution.canonicalName
                }
                sa.type = 'intron-to-exon'
                sa.strand = sequenceAlteration.strand
                sa.name = sequenceAlteration.name + '-inContext'
                sa.originalAlterationUniqueName = sequenceAlteration.uniqueName
                sa.offset = sequenceAlteration.offset - difference
                sa.alterationResidue = sequenceAlteration.alterationResidue.substring(difference, sequenceAlteration.alterationResidue.length())
                sequenceAlterationsInContextList.add(sa)
            }
        }
        int currentOffset = 0;
        // TODO: refactor with getResidues in FeatureService so we are calling a similar method
        for (SequenceAlterationInContext sequenceAlteration in sequenceAlterationsInContextList.sort() { a,b ->
                 a.fmin <=> b.fmin
        }){
            int localCoordinate = featureService.convertSourceCoordinateToLocalCoordinate(fmin,fmax,strand, sequenceAlteration.fmin);
            // Commented out since check for overlap is done beforehand
//            if(!overlapperService.overlaps(fmin,fmax,sequenceAlteration.fmin,sequenceAlteration.fmax)){
//                continue
//            }

            // TODO: is this correct?
            String sequenceAlterationResidues = sequenceAlteration.alterationResidue
            int alterationLength = sequenceAlteration.alterationResidue.length()
            if (strand == Strand.NEGATIVE) {
                sequenceAlterationResidues = SequenceTranslationHandler.reverseComplementSequence(sequenceAlterationResidues);
            }
            // Insertions
            if (sequenceAlteration.instanceOf == Insertion.canonicalName) {
                if (strand==Strand.NEGATIVE) {
                    ++localCoordinate;
                }
                residues.insert(localCoordinate + currentOffset, sequenceAlterationResidues);
                currentOffset += alterationLength;
            }
            // Deletions
            else if (sequenceAlteration.instanceOf == Deletion.canonicalName) {
                if (strand == Strand.NEGATIVE) {
                    residues.delete(localCoordinate + currentOffset - alterationLength + 1,
                            localCoordinate + currentOffset + 1);
                } else {
                    residues.delete(localCoordinate + currentOffset,
                            localCoordinate + currentOffset + alterationLength);
                }
                currentOffset -= alterationLength;
            }
            // Substitions
            else if (sequenceAlteration.instanceOf == Substitution.canonicalName) {
                int start = strand == Strand.NEGATIVE ? localCoordinate - (alterationLength - 1) : localCoordinate;
                residues.replace(start + currentOffset,
                        start + currentOffset + alterationLength,
                        sequenceAlterationResidues);
            }
        }

        return residues.toString()
    }

    String getRawResiduesFromSequence(Sequence sequence, int fmin) {
        return getRawResiduesFromSequence(sequence,fmin,sequence.length)
    }

    String getRawResiduesFromSequence(Sequence sequence, int fmin, int fmax) {
        StringBuilder sequenceString = new StringBuilder()

        int startChunkNumber = fmin / sequence.seqChunkSize;
        int endChunkNumber = (fmax - 1 ) / sequence.seqChunkSize;

        
        for(int i = startChunkNumber ; i<= endChunkNumber ; i++){
            sequenceString.append(loadResidueForSequence(sequence,i))
        }

        int startPosition = fmin - (startChunkNumber * sequence.seqChunkSize);

        return sequenceString.substring(startPosition,startPosition + (fmax-fmin))
    }

    String loadResidueForSequence(Sequence sequence, int chunkNumber) {
        CRC32 crc = new CRC32();
        crc.update(sequence.name.getBytes());
        String hex = String.format("%08x", crc.getValue());
        String []dirs = splitStringByNumberOfCharacters(hex, 3);
        String seqDir = String.format("%s/seq/%s/%s/%s", sequence.organism.directory, dirs[0], dirs[1], dirs[2]);
        String filePath = seqDir+ "/"+ sequence.name + "-" + chunkNumber + ".txt"

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


    def loadRefSeqs(Organism organism) {
        log.info "loading refseq ${organism.refseqFile}"
        organism.valid = false ;
        organism.save(flush: true, failOnError: true,insert:false)

        File refSeqsFile = new File(organism.refseqFile);
        if(refSeqsFile.exists()) {
            def refSeqs=refSeqsFile.withReader { r ->
                new JsonSlurper().parse( r )
            }

            Sequence.deleteAll(Sequence.findAllByOrganism(organism))
            refSeqs.each { refSeq ->
                int length;
                if(refSeq.length) {
                    length = refSeq.length
                }
                else {
                    //workaround for jbrowse refSeqs that have no length element
                    length = refSeq.end-refSeq.start
                }
                Sequence sequence = new Sequence(
                        organism: organism
                        ,length: length
                        ,seqChunkSize: refSeq.seqChunkSize
                        ,start: refSeq.start
                        ,end: refSeq.end
                        ,name: refSeq.name
                ).save(failOnError: true)
            }

            organism.valid = true
            organism.save(flush: true,insert:false,failOnError: true)

        }
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
                Boolean readThroughStop = false
                if (cdsService.getStopCodonReadThrough(cds).size() > 0) {
                    readThroughStop = true
                }
                String rawSequence = featureService.getResiduesWithAlterationsAndFrameshifts(cds)
                featureResidues = SequenceTranslationHandler.translateSequence(rawSequence, standardTranslationTable, true, readThroughStop)
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
                Boolean readThroughStop = false
                if (cdsService.getStopCodonReadThrough(transcriptService.getCDS(exonService.getTranscript((Exon) gbolFeature))).size() > 0) {
                    readThroughStop = true
                }
                featureResidues = SequenceTranslationHandler.translateSequence(rawSequence, standardTranslationTable, true, readThroughStop)
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
            List<Feature> listOfSequenceAlterations = Feature.executeQuery("select distinct f from Feature f join f.featureLocations fl join fl.sequence s where s = :sequence and f.class in :sequenceTypes and fl.fmin >= :fmin and fl.fmax <= :fmax ", [sequence: sequence, sequenceTypes: requestHandlingService.viewableAlterations,fmin:fmin,fmax:fmax])
            featuresToWrite += listOfSequenceAlterations
        }
        gff3HandlerService.writeFeaturesToText(outputFile.absolutePath, featuresToWrite, grailsApplication.config.apollo.gff3.source as String)
    }

    /**
     * /opt/apollo/honeybee/data/seq/50e/b7a/08/{"padding":0, "projection":"None", "referenceTrack":"Official Gene Set v3.2", "sequenceList":[{"name":"Group1.1"}], "label":"Group1.1"}:-1..-1-7.txt
     * @param inputSequence
     * @return  return from first { to last }
     */
    String getSequencePathName(String inputSequence) {
        return inputSequence.substring(inputSequence.indexOf("{"),inputSequence.lastIndexOf("}")+1)
    }

    String getSequencePrefixPath(String inputFileName) {
        return inputFileName.substring(0,inputFileName.indexOf("{"))
    }

    String getChunkSuffix(String inputFileName) {
        return inputFileName.substring(inputFileName.lastIndexOf("-"))
    }
}
