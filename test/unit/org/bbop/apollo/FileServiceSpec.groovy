package org.bbop.apollo

import grails.test.mixin.TestFor
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FileService)
class FileServiceSpec extends Specification {

    private final String FINAL_DIRECTORY = "test/unit/resources/archive_tests/"

    File parentDir = new File(FINAL_DIRECTORY+"data")
    File fileA = new File(FINAL_DIRECTORY+"data/a.txt")
    File fileB = new File(FINAL_DIRECTORY+"data/b.txt")
    File fileFlatA = new File(FINAL_DIRECTORY+"a.txt")
    File fileFlatB = new File(FINAL_DIRECTORY+"b.txt")

    // volvox data
    File seqDirFlat = new File(FINAL_DIRECTORY+"/seq")
    File seqDirFaFlat = new File(seqDirFlat.absolutePath+"/volvox.fa")
    File seqDirFaiFlat = new File(seqDirFlat.absolutePath+"/volvox.fa.fai")
    File trackListFlat = new File(FINAL_DIRECTORY+"/trackList.json")

    def setup() {
    }

    def cleanup(){
        parentDir.deleteDir()
        fileFlatA.delete()
        fileFlatB.delete()
        seqDirFlat.deleteDir()
        trackListFlat.delete()
    }

    void "handle tar.gz decompress directory"() {

        given: "a tar.gz file"
        File inputFile = new File(FINAL_DIRECTORY + "/no_symlinks.tgz" )
        assert inputFile.exists()
        assert !fileA.exists()
        assert !fileB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right file"
        assert fileA.exists()
        assert fileB.exists()
        assert fileA.text == 'aaa\n'
        assert fileB.text == 'bbb\n'


    }

    void "handle tar.gz symlinks directory"() {

        given: "a tar.gz file"
        File inputFile = new File(FINAL_DIRECTORY + "/symlinks.tgz" )
        assert inputFile.exists()
        assert !fileA.exists()
        assert !fileB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right file"
        assert fileB.exists()
        assert fileB.text == 'bbb - no symlink\n'
        assert !fileA.exists()
        assert Files.isSymbolicLink(Paths.get(fileA.absolutePath))


    }

    void "handle zip decompress directory"() {

        given: "a zip file"
        File inputFile = new File(FINAL_DIRECTORY + "/no_symlinks.zip" )
        assert inputFile.exists()
        assert !fileA.exists()
        assert !fileB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressZipArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right file"
        assert fileA.exists()
        assert fileB.exists()
        assert fileA.text == 'aaa\n'
        assert fileB.text == 'bbb\n'


    }

    void "handle tar.gz decompress flat"() {

        given: "a .tgz file"
        File inputFile = new File(FINAL_DIRECTORY + "/flat.tgz" )
        assert inputFile.exists()
        assert !fileFlatA.exists()
        assert !fileFlatB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right file"
        assert fileFlatA.exists()
        assert fileFlatB.exists()
        assert fileFlatA.text == 'aaa\n'
        assert fileFlatB.text == 'bbb\n'


    }


    void "handle tar.gz decompress flat symlink"() {

        given: "a .tgz file"
        File inputFile = new File(FINAL_DIRECTORY + "/flat_symlink.tgz" )
        assert inputFile.exists()
        assert !fileFlatA.exists()
        assert !fileFlatB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right file"
        assert fileFlatA.exists()
        assert fileFlatB.exists()
        assert fileFlatA.text == 'aaa\n'
        assert Files.isSymbolicLink(Paths.get(fileFlatB.absolutePath))


    }

    void "handle zip decompress flat"() {
        given: "a zip file"
        File inputFile = new File(FINAL_DIRECTORY + "/flat.zip" )
        assert inputFile.exists()
        assert !fileFlatA.exists()
        assert !fileFlatB.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressZipArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert fileFlatA.exists()
        assert fileFlatB.exists()
        assert fileFlatA.text == 'aaa\n'
        assert fileFlatB.text == 'bbb\n'
    }

    void "handle zip trackList.json decompress flat"() {
        given: "a zip file"
        File inputFile = new File(FINAL_DIRECTORY + "/volvox_flat.zip" )
        assert inputFile.exists()
        assert !seqDirFlat.exists()
        assert !trackListFlat.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressZipArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert trackListFlat.exists()
        assert !trackListFlat.text.empty

        assert seqDirFlat.exists()
        assert seqDirFaFlat.exists() && !seqDirFaFlat.text.empty
        assert seqDirFaiFlat.exists() && !seqDirFaiFlat.text.empty
    }

    void "handle zip trackList.json decompress with_directory"() {
        given: "a zip file"
        File inputFile = new File(FINAL_DIRECTORY + "/volvox.zip" )
        assert inputFile.exists()
        assert !seqDirFlat.exists()
        assert !trackListFlat.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressZipArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert trackListFlat.exists()
        assert !trackListFlat.text.empty

        assert seqDirFlat.exists()
        assert seqDirFaFlat.exists() && !seqDirFaFlat.text.empty
        assert seqDirFaiFlat.exists() && !seqDirFaiFlat.text.empty
    }

    void "handle .tgz trackList.json decompress flat"() {
        given: "a zip file"
        File inputFile = new File(FINAL_DIRECTORY + "/volvox_flat.tgz" )
        assert inputFile.exists()
        assert !seqDirFlat.exists()
        assert !trackListFlat.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert trackListFlat.exists()
        assert !trackListFlat.text.empty

        assert seqDirFlat.exists()
        assert seqDirFaFlat.exists() && !seqDirFaFlat.text.empty
        assert seqDirFaiFlat.exists() && !seqDirFaiFlat.text.empty
    }


    void "handle .tgz trackList.json decompress with_directory"() {
        given: "a tgz file"
        File inputFile = new File(FINAL_DIRECTORY + "/volvox.tgz" )
        assert inputFile.exists()
        assert !seqDirFlat.exists()
        assert !trackListFlat.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert trackListFlat.exists()
        assert !trackListFlat.text.empty

        assert seqDirFlat.exists()
        assert seqDirFaFlat.exists() && !seqDirFaFlat.text.empty
        assert seqDirFaiFlat.exists() && !seqDirFaiFlat.text.empty
    }

    void "handle .tgz trackList.json decompress flat symlink"() {
        given: "a tgz file"
        File inputFile = new File(FINAL_DIRECTORY + "/volvox_symlink_flat.tgz" )
        assert inputFile.exists()
        assert !seqDirFlat.exists()
        assert !trackListFlat.exists()
        assert !fileFlatA.exists()
        assert !fileFlatB.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert trackListFlat.exists()
        assert !trackListFlat.text.empty

        assert seqDirFlat.exists()
        assert seqDirFaFlat.exists() && !seqDirFaFlat.text.empty
        assert seqDirFaiFlat.exists() && !seqDirFaiFlat.text.empty

        assert fileFlatA.exists()
        assert Files.isSymbolicLink(Paths.get(fileFlatA.absolutePath))
        assert fileFlatB.exists()
        assert !Files.isSymbolicLink(Paths.get(fileFlatB.absolutePath))
    }

    void "handle .tgz trackList.json symlink decompress with_directory"() {
        given: "a tgz file"
        File inputFile = new File(FINAL_DIRECTORY + "/volvox_symlink.tgz" )
        assert inputFile.exists()
        assert !seqDirFlat.exists()
        assert !trackListFlat.exists()

        when: "we expand it"
        List<String> fileFlatNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)

        then: "we should have the right fileFlat"
        assert trackListFlat.exists()
        assert !trackListFlat.text.empty

        assert seqDirFlat.exists()
        assert seqDirFaFlat.exists() && !seqDirFaFlat.text.empty
        assert seqDirFaiFlat.exists() && !seqDirFaiFlat.text.empty

        assert fileFlatA.exists()
        assert Files.isSymbolicLink(Paths.get(fileFlatA.absolutePath))
        assert fileFlatB.exists()
        assert !Files.isSymbolicLink(Paths.get(fileFlatB.absolutePath))
    }
}
