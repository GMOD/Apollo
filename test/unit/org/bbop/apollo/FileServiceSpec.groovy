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

    def setup() {
    }

    def cleanup(){
        fileA.delete()
        fileB.delete()
        parentDir.delete()
    }

    void "handle tar.gz decompress"() {

        given: "a tar.gz file"
        File inputFile = new File(FINAL_DIRECTORY + "/no_symlinks.tgz" )
        println "input file ${inputFile} ${inputFile.exists()}"
        println "current working directory  ${new File(".").absolutePath}"
        assert inputFile.exists()
        assert !fileA.exists()
        assert !fileB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)
        println "fileNames ${fileNames.join(",")}"

        then: "we should have the right file"
        assert fileA.exists()
        assert fileB.exists()
        assert fileA.text == 'aaa\n'
        assert fileB.text == 'bbb\n'


    }

    void "handle symlinks"() {

        given: "a tar.gz file"
        File inputFile = new File(FINAL_DIRECTORY + "/symlinks.tgz" )
        println "current working directory  ${new File(".").absolutePath}"
        assert inputFile.exists()
        assert !fileA.exists()
        assert !fileB.exists()

        when: "we expand it"
        List<String> fileNames = service.decompressTarArchive(inputFile,FINAL_DIRECTORY)
        println "fileNames should have a symlink in it ${fileNames.join(",")}"

        then: "we should have the right file"
        assert fileB.exists()
        assert fileB.text == 'bbb - no symlink\n'
        assert !fileA.exists()
        assert Files.isSymbolicLink(Paths.get(fileA.absolutePath))


    }



}
