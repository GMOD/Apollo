package org.gmod.gbol.simpleObject.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public abstract class FileHandler implements SimpleObjectIOInterface{
	
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
			System.err.println("Cannot write to file " + this.filePath);
		}
		
	}
	
	protected void openHandle(){
		try {
			this.fileReader = new FileReader(this.filePath);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open file handle to " + this.filePath);
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	protected void closeHandle(){
		try {
			this.fileReader.close();
		} catch (IOException e) {
			System.err.println("Unable to close file handle to " + this.filePath);
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
			System.err.println("ERROR: ChadoXML parse error: IOException");
			System.err.println(e.getMessage());
			System.exit(0);
		}
		
		return fileContents;
	}
	
	protected String getFilePath(){
		return this.filePath;
	}
	
	
	
}