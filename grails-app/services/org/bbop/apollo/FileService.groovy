package org.bbop.apollo

import grails.transaction.Transactional
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.FileSystemException

@Transactional
class FileService {

    /**
     * Decompress an archive to a folder specified by directoryName in the given path
     * @param archiveFile
     * @param path
     * @param directoryName
     * @param tempDir
     * @return
     */
    def decompress(File archiveFile, String path, String directoryName = null, boolean tempDir = false) {
        // decompressing
        if (archiveFile.name.contains(".zip")) {
            decompressZipArchive(archiveFile, path, directoryName, tempDir)
        }
        else if (archiveFile.name.contains(".tar.gz") || archiveFile.name.contains(".tgz")) {
            decompressTarArchive(archiveFile, path, directoryName, tempDir)
        }
        else {
            throw new IOException("Cannot detect format (either *.zip, *.tar.gz or *.tgz) for file: ${archiveFile.name}")
        }
    }

    /**
     * Decompress a zip archive to a folder specified by directoryName in the given path
     * @param zipFile
     * @param path
     * @param directoryName
     * @param tempDir
     * @return
     */
    def decompressZipArchive(File zipFile, String path, String directoryName = null, boolean tempDir = false) {
        String archiveRootDirectoryName
        boolean atArchiveRoot = true
        String initialLocation = tempDir ? path + File.separator + "temp" : path
        log.debug "initial location: ${initialLocation}"
        final InputStream is = new FileInputStream(zipFile);
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, is);
        ZipArchiveEntry entry = null

        while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
            if (atArchiveRoot) {
                archiveRootDirectoryName = entry.getName()
                atArchiveRoot = false
            }

            validateFileName(entry.getName(), archiveRootDirectoryName)
            if (entry.isDirectory()) {
                File dir = new File(initialLocation, entry.getName());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                continue;
            }

            File outputFile = new File(initialLocation, entry.getName());

            if (outputFile.isDirectory()) {
                continue;
            }

            if (outputFile.exists()) {
                continue;
            }

            OutputStream os = new FileOutputStream(outputFile);
            IOUtils.copy(ais, os);
            os.close();
        }

        ais.close()
        is.close()

        if (tempDir) {
            // move files from temp directory to folder supplied via directoryName
            String unpackedArchiveLocation = initialLocation + File.separator + archiveRootDirectoryName
            String finalLocation = path + File.separator + directoryName
            File finalLocationFile = new File(finalLocation)
            if (finalLocationFile.mkdir()) log.debug "${finalLocation} directory created"
            try {
                Files.move(new File(unpackedArchiveLocation).toPath(), finalLocationFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                log.debug "files moved from ${unpackedArchiveLocation} to ${finalLocation}"
            } catch (FileSystemException fse) {
                log.error fse.message
            }

            // delete temp folder
            new File(initialLocation).deleteDir()
        }
    }

    /**
     * Decompress a tar.gz archive to a folder specified by directoryName in the given path
     * @param tarFile
     * @param path
     * @param directoryName
     * @param tempDir
     * @return
     */
    def decompressTarArchive(File tarFile, String path, String directoryName = null, boolean tempDir = false) {
        boolean atArchiveRoot = true
        String archiveRootDirectoryName
        String initialLocation = tempDir ? path + File.separator + "temp" : path
        log.debug "initial location: ${initialLocation}"
        TarArchiveInputStream tais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarFile)))
        TarArchiveEntry entry = null

        while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {
            if (atArchiveRoot) {
                archiveRootDirectoryName = entry.getName()
                atArchiveRoot = false
            }

            validateFileName(entry.getName(), archiveRootDirectoryName)
            if (entry.isDirectory()) {
                File dir = new File(initialLocation, entry.getName())
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                continue;
            }

            File outputFile = new File(initialLocation, entry.getName())

            if (outputFile.isDirectory()) {
                continue;
            }

            if (outputFile.exists()) {
                continue;
            }

            FileOutputStream fos = new FileOutputStream(outputFile);
            IOUtils.copy(tais, fos);
            fos.close();
        }

        if (tempDir) {
            // move files from temp directory to folder supplied via directoryName
            String unpackedArchiveLocation = initialLocation + File.separator + archiveRootDirectoryName
            String finalLocation = path + File.separator + directoryName
            File finalLocationFile = new File(finalLocation)
            if (finalLocationFile.mkdir()) log.debug "${finalLocation} directory created"
            try {
                Files.move(new File(unpackedArchiveLocation).toPath(), finalLocationFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                log.debug "files moved from ${unpackedArchiveLocation} to ${finalLocation}"
            } catch (FileSystemException fse) {
                log.error fse.message
            }

            // delete temp folder
            new File(initialLocation).deleteDir()
        }
    }


    def store(CommonsMultipartFile file, String path, String directoryName = null, boolean tempDir = false) {
        File pathFile = new File(path)
        if (!pathFile.exists()) {
            pathFile.mkdirs()
        }
        String destinationFileName = directoryName ?
                path + File.separator + directoryName + File.separator + file.getOriginalFilename() :
                path + File.separator + file.getOriginalFilename()

        File destinationFile = new File(destinationFileName)
        try {
            log.debug "transferring track file to ${destinationFileName}"
            file.transferTo(destinationFile)
        } catch (Exception e) {
            log.error e.message
        }
    }

    /**
     * Validate that a given file falls within its intended output directory
     * @param fileName
     * @param intendedOutputDirectory
     * @return
     * @throws IOException
     */
    def validateFileName(String fileName, String intendedOutputDirectory) throws IOException {
        File file = new File(fileName)
        String canonicalPath = file.getCanonicalPath()
        File intendedOutputDirectoryFile = new File(intendedOutputDirectory)
        String canonicalIntendedOutputDirectoryPath = intendedOutputDirectoryFile.getCanonicalPath()
        if (canonicalPath.startsWith(canonicalIntendedOutputDirectoryPath)) {
            return canonicalPath
        } else {
            throw new IOException("File is outside extraction target directory.")
        }
    }
}
