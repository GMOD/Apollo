package org.bbop.apollo.web.dataadapter.gff3;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.io.GFF3Handler;
import org.gmod.gbol.simpleObject.Feature;

public class Gff3IO {

	private GFF3Handler handler;

	public Gff3IO(String path, String source, Set<String> metaDataToExport) throws IOException {
		this(path, source, GFF3Handler.Format.TEXT, metaDataToExport);
	}
	
	public Gff3IO(String path, String source, GFF3Handler.Format format, Set<String> metaDataToExport) throws IOException {
		handler = new GFF3Handler(path, GFF3Handler.Mode.WRITE, format, metaDataToExport);
	}
	
	public void writeFeatures(Collection<? extends AbstractSingleLocationBioFeature> features, String source) throws IOException {
		handler.writeFeatures(features, source);
	}
	
	public void writeFeatures(Iterator<? extends AbstractSingleLocationBioFeature> iterator, String source, boolean needDirectives) throws IOException {
		handler.writeFeatures(iterator, source, needDirectives);
	}
	
	public void writeFasta(Collection<? extends Feature> features) {
		handler.writeFasta(features);
	}
	
	public void writeFasta(Collection<? extends Feature> features, boolean writeFastaDirective, boolean useLocation) {
		for (Feature feature : features) {
			handler.writeFasta(feature, writeFastaDirective, useLocation);
		}
	}
	
	public void writeFasta(Feature feature) {
		handler.writeFasta(feature);
	}
	
	public void close() {
		handler.close();
	}
	
}