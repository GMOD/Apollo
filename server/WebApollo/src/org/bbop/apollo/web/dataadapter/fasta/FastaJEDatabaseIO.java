package org.bbop.apollo.web.dataadapter.fasta;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.bbop.apollo.editor.session.AnnotationSession;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.bbop.apollo.web.util.FeatureIterator;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.io.FastaHandler;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOException;
import org.gmod.gbol.util.SequenceUtil.TranslationTable;

public class FastaJEDatabaseIO extends FastaIO {

	private JEDatabase jeDatabase;
	
	public FastaJEDatabaseIO(String databaseDir, String path, String source, BioObjectConfiguration conf, TranslationTable translationTable) throws Exception {
		this(databaseDir, path, source, conf, true, FastaHandler.Format.GZIP, translationTable);
	}

	public FastaJEDatabaseIO(String databaseDir, String path, String source, BioObjectConfiguration conf, boolean readOnly, TranslationTable translationTable) throws Exception {
		this(databaseDir, path, source, conf, readOnly, FastaHandler.Format.GZIP, translationTable);
	}
	
	public FastaJEDatabaseIO(String databaseDir, String path, String seqType, BioObjectConfiguration conf, boolean readOnly, FastaHandler.Format format, TranslationTable translationTable) throws Exception {
		super(path, seqType, format, conf, null, translationTable);
		setJeDatabase(databaseDir, readOnly);
		session = new AnnotationSession();
		for (Iterator<Feature> iter = jeDatabase.getSequenceAlterationIterator(); iter.hasNext(); ) {
			session.addSequenceAlteration((SequenceAlteration)BioObjectUtil.createBioObject(iter.next(), conf));
		}
	}
	
	public void setJeDatabase(String databaseDir, boolean readOnly) {
		if (jeDatabase != null) {
			jeDatabase.close();
		}
		jeDatabase = new JEDatabase(databaseDir, readOnly);
	}

	public void writeFeatures(Feature sourceFeature, String seqType, Set<String> featureTypes, Set<String> metaDataToExport) throws SimpleObjectIOException, IOException {
		FeatureIterator featureIterator = new FeatureIterator(jeDatabase.getFeatureIterator(), sourceFeature, conf);
		writeFeatures(featureIterator, seqType, featureTypes, metaDataToExport);
	}
	
	@Override
	public void close() {
		super.close();
		if (jeDatabase != null) {
			jeDatabase.close();
		}
	}
	
}
