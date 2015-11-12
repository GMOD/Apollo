package org.bbop.apollo.projection

import org.apache.commons.io.FileUtils

/**
 * Created by Nathan Dunn on 9/1/15.
 *
 * Responsible for handling chunks.
 *
 * TODO: refactor into the chunk manager service?
 */
@Singleton
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

    List<Integer> getChunksForPath(String parentFileString) {
        List<Integer> chunks = []
        File parentFile = new File(parentFileString)
        String[] extensions = [".txt"]

        FileUtils.listFiles(parentFile,extensions,false).each {
            Integer chunkNumber = getChunkNumberFromFileName(it.name)
            chunks << chunkNumber
            println "adding chunk ${chunkNumber} to ${chunks.size()} from ${it.name}"
        }
        return chunks
    }

    Integer getChunkNumberFromFileName(String fileName) {
        String suffix = fileName.split("-")[1]
        println "suffix ${suffix}"
        println "suf length ${suffix.length()} ${'.txt'.length()}"
        Integer endIndex = suffix.length() - 4
        println "endIndex ${endIndex}"
        Integer chunkNumber = Integer.parseInt(suffix.substring(0,endIndex))
        return chunkNumber
    }
}
