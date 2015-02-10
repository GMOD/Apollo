package org.bbop.apollo

import grails.transaction.Transactional
import grails.util.Pair
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

import java.util.zip.CRC32

@Transactional
class SequenceService {

//    def configWrapperService

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


        return sequenceString.toString()
    }

    SequenceChunk getSequenceChunkForChunk(Sequence sequence, int i) {
        SequenceChunk sequenceChunk = SequenceChunk.findBySequenceAndChunkNumber(sequence,i)
        if(!sequenceChunk){
            String residue = loadResidueForSequence(sequence,i)
            println "RESIDUE load: ${residue?.size()}"
            sequenceChunk = new SequenceChunk(
                    sequence: sequence
                    ,chunkNumber: i
                    ,residue: residue
            ).save(flush:true)
        }
        println "RESIDUE loaded from DB: ${sequenceChunk.residue?.size()}"
        return sequenceChunk
    }

    String loadResidueForSequence(Sequence sequence, int chunkNumber) {
//        for (Pair<Integer, String> data : cache) {
//            if (data.getFirst().equals(chunkNumber)) {
//                return data.getSecond();
//            }
//        }
        String filePath = sequence.sequenceDirectory + "/" + sequence.seqChunkPrefix + chunkNumber + ".txt"
//        BufferedReader br = new BufferedReader(new FileReader(sequenceDirectory + "/" + chunkPrefix + chunkNumber + ".txt"));
//        BufferedReader br = new BufferedReader(new FileReader(filePath));

        return new File(filePath).getText().toUpperCase()
//        String line;
//        StringBuilder sb = new StringBuilder();
//        while ((line = br.readLine()) != null) {
//            sb.append(line.toUpperCase());
//        }
//        String sequence = sb.toString();
//        if (cache.size() >= cacheSize) {
//            cache.remove();
//        }
//        cache.add(new Pair<Integer, String>(chunkNumber, sequence));
//        br.close();
//        return sequence;
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
        println "loading refseq ${organism.refseqFile}"
        organism.valid = false ;
        organism.save(flush: true, failOnError: true,insert:false)

        File refSeqsFile = new File(organism.refseqFile);
        println " file exists ${refSeqsFile.exists()}"
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(refSeqsFile));
        JSONArray refSeqs = convertJBrowseJSON(bufferedInputStream);
        println "freseq length ${refSeqs.size()}"
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
                    ,dataDirectory: refSeqsFile.getParentFile().getParent()
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
}
