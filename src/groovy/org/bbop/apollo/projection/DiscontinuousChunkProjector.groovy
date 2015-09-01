package org.bbop.apollo.projection

/**
 * Created by Nathan Dunn on 9/1/15.
 *
 * Responsible for handling chunks.
 *
 * TODO: refactor into the chunk manager service?
 */
class DiscontinuousChunkProjector {

    Integer getStartChunk(Integer chunkNumber, Integer chunkSize) {
        return chunkNumber * chunkSize
    }
    Integer getEndChunk(Integer chunkNumber, Integer chunkSize) {
        return (chunkNumber+1) * chunkSize
    }

    Integer getChunkForCoordinate(Integer coordinate, Integer chunkSize) {
        return coordinate  / chunkSize
    }

    String getSequenceForChunks(int startChunk, int endChunk,String parentFile,String sequenceName) {
        File file = new File(parentFile+"/"+sequenceName+"-"+startChunk+".txt")
        String inputText = file.text
        while(startChunk < endChunk){
            ++startChunk
            println "Adding other chunk file ! "
            File otherFile = new File(parentFile+"/"+sequenceName+"-"+startChunk+".txt")
            println "otherFile ${otherFile.absolutePath} ${otherFile.exists()} "
            inputText += otherFile.text
        }
        return inputText
    }
}
