package org.bbop.apollo

import grails.transaction.Transactional
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.IOUtils
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

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
    List<String> decompress(File archiveFile, String path, String directoryName = null, boolean tempDir = false) {
        // decompressing
        if (archiveFile.name.contains(".zip")) {
            return decompressZipArchive(archiveFile, path, directoryName, tempDir)
        } else if (archiveFile.name.contains(".tar.gz") || archiveFile.name.contains(".tgz")) {
            return decompressTarArchive(archiveFile, path, directoryName, tempDir)
        } else if (archiveFile.name.contains(".gz")) {
            return decompressGzipArchive(archiveFile, path, directoryName, tempDir)
        } else {
            throw new IOException("Cannot detect format (either *.zip, *.tar.gz or *.tgz) for file: ${archiveFile.name}")
        }
        return null
    }

    String getArchiveRootDirectoryNameForTgzTrackList(File tarFile){
        TarArchiveInputStream tais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarFile)))
        TarArchiveEntry entry
        while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {
            if(entry.name.contains("trackList.json")){
                String foundPath = entry.name.substring(0,entry.name.length() - "trackList.json".length())
                log.debug "found path ${foundPath}"
                return foundPath
            }
        }
        log.debug "trackList.json not included in the archive"
        return null
    }

    String getArchiveRootDirectoryNameForZipTrackList(File zipFile){
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, new FileInputStream(zipFile))
        ZipArchiveEntry entry
        while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
            if(entry.name.contains("trackList.json")){
                return entry.name.substring(0,entry.name.length() - "trackList.json".length())
            }
        }
        log.debug "trackList.json not included in the archive"
        return null
    }

    /**
     * Decompress a zip archive to a folder specified by directoryName in the given path
     * @param zipFile
     * @param pathString
     * @param directoryName
     * @param tempDir
     * @return
     */
    List<String> decompressZipArchive(File zipFile, String pathString, String directoryName = null, boolean tempDir = false) {
        List<String> fileNames = []
        String archiveRootDirectoryName = getArchiveRootDirectoryNameForZipTrackList(zipFile)
        archiveRootDirectoryName = archiveRootDirectoryName != null ? archiveRootDirectoryName : ""
        String initialLocation = tempDir ? pathString + File.separator + "temp" : pathString
        log.debug "initial location: ${initialLocation} "
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, new FileInputStream(zipFile));
        ZipArchiveEntry entry


        Path destDir = Paths.get(pathString)
        String prefix = destDir.toString()

        while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
            try {
//                archiveRootDirectoryName = archiveRootDirectoryName ?: entry.getName()
                validateFileName(entry.getName(), archiveRootDirectoryName)
                // output should be output name minus the parent root directory
                String outputDirectoryName = entry.name
                if(outputDirectoryName.startsWith(archiveRootDirectoryName)){
                    log.debug "starts with ${outputDirectoryName} vs $archiveRootDirectoryName"
                    outputDirectoryName = outputDirectoryName.substring(archiveRootDirectoryName.length())
                    log.debug "final output ${outputDirectoryName}"
                }
                Path path = destDir.resolve(outputDirectoryName).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(path)
                }
                    // TODO: this doesn't quite work for zip.  Should use a tar.gz archive instead
//                else if (entry.isUnixSymlink()) {S
//                    String dest = entry.name;
//                    Path destAbsPath = path.getParent().resolve(dest).normalize();
//                    if (!destAbsPath.normalize().toString().startsWith(prefix)) {
//                        log.info("Archive includes a symlink outside the current path $entry.name -> ${dest.toString()}")
////                        throw new RuntimeException("Archive includes an invalid symlink: " + entry.getName() + " -> " + dest);
//                    }
//                    Files.createSymbolicLink(path, Paths.get(dest));
//                    fileNames.add(destAbsPath.toString())
//                }
                else{
                    Files.createDirectories(path.getParent());
                    File outputFile = new File(initialLocation, outputDirectoryName)
                    if (outputFile.exists()) {
                        continue;
                    }

                    FileOutputStream fos = new FileOutputStream(outputFile)
                    IOUtils.copy(ais, fos)
                    fos.close()
                    fileNames.add(outputFile.absolutePath)
                }
            } catch (IOException e) {
                log.error("Problem decrompression file ${entry.name} vs ${archiveRootDirectoryName}", e)
            }
        }

        ais.close()

        if (tempDir) {
            // move files from temp directory to folder supplied via directoryName
            String unpackedArchiveLocation = initialLocation + File.separator + archiveRootDirectoryName
            String finalLocation = pathString + File.separator + directoryName
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
        return fileNames
    }

    /**
     * Decompress a tar.gz archive to a folder specified by directoryName in the given path
     * @param tarFile
     * @param pathString
     * @param directoryName
     * @param tempDir
     * @return
     */
    List<String> decompressTarArchive(File tarFile, String pathString, String directoryName = null, boolean tempDir = false) {
        List<String> fileNames = []
        String archiveRootDirectoryName = getArchiveRootDirectoryNameForTgzTrackList(tarFile)
        archiveRootDirectoryName = archiveRootDirectoryName!=null ? archiveRootDirectoryName : ""
        String initialLocation = tempDir ? pathString + File.separator + "temp" : pathString
        log.debug "initial location: ${initialLocation}"
        TarArchiveInputStream tais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarFile)))
        TarArchiveEntry entry
        Path destDir = Paths.get(pathString)
        String prefix = destDir.toString()

        while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {

            try {
                validateFileName(entry.name, archiveRootDirectoryName)
                if(!pathString.toString().startsWith(prefix)){
                    throw new IOException("Archive includes an invalid entry, ignoring: " + entry.name);
                }
                String outputDirectoryName = entry.name
                if(outputDirectoryName.startsWith(archiveRootDirectoryName)){
                    outputDirectoryName = outputDirectoryName.substring(archiveRootDirectoryName.length())
                }
                Path path = destDir.resolve(outputDirectoryName).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(path)
                }
                else if (entry.isSymbolicLink()) {
                    String dest = entry.getLinkName();
                    Path destAbsPath = path.getParent().resolve(dest).normalize();
                    if (!destAbsPath.normalize().toString().startsWith(prefix)) {
                        log.info("Archive includes a symlink outside the current path $entry.name -> ${dest.toString()}")
//                        throw new RuntimeException("Archive includes an invalid symlink: " + entry.getName() + " -> " + dest);
                    }
                    Files.createDirectories(path.getParent());
                    Files.createSymbolicLink(path, Paths.get(dest));
                    fileNames.add(destAbsPath.toString())
                }
                else{
                    Files.createDirectories(path.getParent());
                    File outputFile = new File(initialLocation, outputDirectoryName)
                    if (outputFile.exists()) {
                        continue;
                    }

                    FileOutputStream fos = new FileOutputStream(outputFile)
                    IOUtils.copy(tais, fos)
                    fos.close()
                    fileNames.add(outputFile.absolutePath)
                }
            } catch (IOException e) {
                log.error("Problem decrompression file ${entry.name} vs ${archiveRootDirectoryName}, ignoring: ${e.message}")
            }
        }

        tais.close()

        if (tempDir) {
            // move files from temp directory to folder supplied via directoryName
            String unpackedArchiveLocation = initialLocation + File.separator + archiveRootDirectoryName
            String finalLocation = pathString + File.separator + directoryName
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
        return fileNames
    }

    private void addFileToTar(TarArchiveOutputStream tOut, File file, String dir) throws IOException {
        String entry = dir + File.separator + file.name
        if (file.isFile() && allowableSuffix(file)) {
            TarArchiveEntry tarEntry = new TarArchiveEntry(file, entry)
            tOut.putArchiveEntry(tarEntry)
            FileInputStream fileInputStream = new FileInputStream(file)
            IOUtils.copy(fileInputStream, tOut)
            tOut.closeArchiveEntry()
        }
        else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTar(tOut, child, entry)
                }
            }
        }
        else{
            log.error(file.name + " is not supported");
        }
    }

    boolean allowableSuffix(File file) {
        return !file.absolutePath.startsWith(".")
    }

    def compressTarArchive(File outputTarFile, File inputDirectory, String base = "") throws IOException{

        FileOutputStream fileOutputStream
        TarArchiveOutputStream tarArchiveOutputStream
        try {
            fileOutputStream = new FileOutputStream(outputTarFile)
            tarArchiveOutputStream = new TarArchiveOutputStream(fileOutputStream)
            tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
            tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            tarArchiveOutputStream.setAddPaxHeadersForNonAsciiNames(true)

            addFileToTar(tarArchiveOutputStream, inputDirectory, ".")
        }
        catch (e) {
            log.error "${e}"
        }
    }

    def compressTarGzArchive(File outputTarFile, File inputDirectory, String base = "") throws IOException{

        FileOutputStream fileOutputStream
        TarArchiveOutputStream tarArchiveOutputStream
        GzipCompressorOutputStream gzipCompressorOutputStream
        try {
            fileOutputStream = new FileOutputStream(outputTarFile)
            gzipCompressorOutputStream = new GzipCompressorOutputStream(fileOutputStream)
            tarArchiveOutputStream = new TarArchiveOutputStream(gzipCompressorOutputStream)
            tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
            tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            tarArchiveOutputStream.setAddPaxHeadersForNonAsciiNames(true)

            addFileToTar(tarArchiveOutputStream, inputDirectory, ".")
        }
        catch (e) {
            log.error "${e}"
        }
    }

    List<String> decompressGzipArchive(File gzipFile, String path, String directoryName = null, boolean tempDir = false) {
        List<String> fileNames = []
        String initialLocation = tempDir ? path + File.separator + "temp" : path

        log.debug "initial location: ${initialLocation}"
        GzipCompressorInputStream tais = new GzipCompressorInputStream(new FileInputStream(gzipFile))
        String tempFileName = UUID.randomUUID().toString() + ".temp"

        new File(initialLocation).mkdirs()

        File outputFile = new File(initialLocation, tempFileName)
        assert outputFile.createNewFile()
        log.debug "${initialLocation} -> can write: ${outputFile.absolutePath} -> ${outputFile.exists()} -> ${outputFile.canWrite()}"
        try {
            FileOutputStream fos = new FileOutputStream(outputFile)
            IOUtils.copy(tais, fos)
            tais.close()
            fos.close()
            fileNames.add(outputFile.absolutePath)
        } catch (IOException e) {
            log.error("Problem decrompression file ${gzipFile} vs ${outputFile}", e)
        }
        return fileNames
    }

    def storeWithNewName(CommonsMultipartFile file, String path, String directoryName, String newName) {
        File pathFile = new File(path)
        if (!pathFile.exists()) {
            pathFile.mkdirs()
        }
        int suffixIndex = newName.indexOf(".")
        if (suffixIndex < 1) {
            throw new RuntimeException("Invalid filename, must have a suffix: [" + newName + "]")
        }
        String suffix = newName.substring(suffixIndex)
        String updatedName = directoryName.replaceAll(" ", "_") + suffix
        log.debug "newName ${newName}, directoryNaem ${directoryName}, updated name, ${updatedName}, suffix ${suffix}, path ${path}"
//        /opt/temporary/apollo/6503-nf_test3/raw || test2 || volvox-sorted.bam
//        /opt/temporary/apollo/6503-nf_test3/raw || test2 .bam
        String destinationFileName = path + File.separator + updatedName
        File destinationFile = new File(destinationFileName)
        try {
            file.transferTo(destinationFile)
        } catch (Exception e) {
            log.error e.message
        }
        return destinationFile
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
            log.debug "DONE transferringfile to ${destinationFileName.size()}"

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
    String validateFileName(String fileName, String intendedOutputDirectory) throws IOException {
        File file = new File(fileName)
        String canonicalPath = file.getCanonicalPath()
        File intendedOutputDirectoryFile = new File(intendedOutputDirectory)
        String canonicalIntendedOutputDirectoryPath = intendedOutputDirectoryFile.getCanonicalPath()

        if (canonicalPath.startsWith(canonicalIntendedOutputDirectoryPath) || canonicalPath == canonicalIntendedOutputDirectoryPath) {
            return canonicalPath
        } else {
            throw new IOException("File is outside extraction target directory. ${canonicalIntendedOutputDirectoryPath} vs ${canonicalPath}")
        }
    }
}
