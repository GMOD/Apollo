package org.gmod.gbol.bioObject.io;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Comment;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.GenericFeatureProperty;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.FeatureSynonym;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

public class FastaHandler {

	private File file;
	private PrintWriter out;
	private Mode mode;
	private int numResiduesPerLine;
	
	public enum Mode {
		READ,
		WRITE
	}
	
	public enum Format {
		TEXT,
		GZIP
	}
	
	public FastaHandler(String path, Mode mode) throws IOException {
		this(path, mode, Format.TEXT);
	}
	
	public FastaHandler(String path, Mode mode, Format format) throws IOException {
		numResiduesPerLine = 60;
		this.mode = mode;
		file = new File(path);
		file.createNewFile();
		if (mode == Mode.READ) {
			if (!file.canRead()) {
				throw new IOException("Cannot read FASTA file: " + file.getAbsolutePath());
			}
		}
		if (mode == Mode.WRITE) {
			if (!file.canWrite()) {
				throw new IOException("Cannot write FATA to: " + file.getAbsolutePath());
			}
			switch (format) {
			case TEXT:
				out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
				break;
			case GZIP:
				out = new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
			}
		}
	}
	
	public void close() {
		if (mode == Mode.READ) {
			//TODO
		}
		else if (mode == Mode.WRITE) { 
			out.close();
		}
	}
	
	public void writeFeatures(Collection<? extends AbstractSingleLocationBioFeature> features, String seqType) throws IOException {
		writeFeatures(features.iterator(), seqType);
	}
	
	public void writeFeatures(Iterator<? extends AbstractSingleLocationBioFeature> iterator, String seqType) throws IOException {
		if (mode != Mode.WRITE) {
			throw new IOException("Cannot write to file in READ mode");
		}
		while (iterator.hasNext()) {
			AbstractSingleLocationBioFeature feature = iterator.next();
			writeFeature(feature, seqType);
		}
	}
	
	public void writeFeature(AbstractSingleLocationBioFeature feature, String seqType) {
		out.println(String.format(">%s (%s) %d residues [%s]", feature.getUniqueName(), feature.getType(), feature.getResidues().length(), seqType));
		String seq = feature.getResidues();
		for (int i = 0; i < seq.length(); i += numResiduesPerLine) {
			int endIdx = i + numResiduesPerLine;
			out.println(seq.substring(i, endIdx > seq.length() ? seq.length() : endIdx));
		}
	}
	
}
