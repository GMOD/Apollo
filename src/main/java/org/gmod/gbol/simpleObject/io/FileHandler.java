package org.gmod.gbol.simpleObject.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public abstract class FileHandler implements SimpleObjectIOInterface{

    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    private String filePath;
    private FileReader fileReader;
    
    public FileHandler(String filePath) throws IOException{
        this.filePath = filePath;
        File f = new File(this.filePath);
        if (!f.exists()){
            throw new FileNotFoundException("File " + this.filePath + " not found.");
        }
        if (!f.canRead()){
            throw new IOException("Cannot read file " + this.filePath);
        }
        if (!f.isFile()){
            throw new IOException(this.filePath + " is an unsupported file type.");
        }
        if (!f.canWrite()){
            logger.error("Cannot write to file " + this.filePath);
        }
        
    }
    
    protected void openHandle(){
        try {
            this.fileReader = new FileReader(this.filePath);
        } catch (FileNotFoundException e) {
            logger.error("Unable to open file handle to " + this.filePath);
            e.printStackTrace();
            System.exit(-1);
        }
        
    }
    
    protected void closeHandle(){
        try {
            this.fileReader.close();
        } catch (IOException e) {
            logger.error("Unable to close file handle to " + this.filePath);
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    protected StringBuilder readFileContents(){
        StringBuilder fileContents = new StringBuilder("");
        try {
            while (this.fileReader.ready()) {
                char[] buf = new char[1024];
                this.fileReader.read(buf);
                fileContents.append(String.valueOf(buf));
            }
        } catch (java.io.IOException e) {
            logger.error("ERROR: ChadoXML parse error: IOException");
            logger.error(e.getMessage());
            System.exit(0);
        }
        
        return fileContents;
    }
    
    protected String getFilePath(){
        return this.filePath;
    }
    
    
    
}
