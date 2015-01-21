package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

import java.util.zip.CRC32

@Transactional
class SequenceService {

//    def configWrapperService

    def getFeatureLocations(Sequence sequence){
        FeatureLocation.findAllBySequence(sequence)
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
}
