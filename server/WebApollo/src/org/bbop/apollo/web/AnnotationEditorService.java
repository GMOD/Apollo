package org.bbop.apollo.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bbop.apollo.config.Configuration;
import org.bbop.apollo.editor.AnnotationEditor;
import org.bbop.apollo.editor.session.AnnotationSession;
import org.bbop.apollo.editor.session.DataStore;
import org.bbop.apollo.editor.AnnotationEditor.AnnotationEditorException;
import org.bbop.apollo.tools.seq.search.SequenceSearchTool;
import org.bbop.apollo.tools.seq.search.SequenceSearchToolException;
import org.bbop.apollo.web.config.CannedComments;
import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.data.FeatureLazyResidues;
import org.bbop.apollo.web.data.FeatureSequenceChunkManager;
import org.bbop.apollo.web.datastore.AbstractDataStore;
import org.bbop.apollo.web.datastore.AbstractDataStoreManager;
import org.bbop.apollo.web.datastore.DataStoreChangeEvent;
import org.bbop.apollo.web.datastore.DataStoreChangeEvent.Operation;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.bbop.apollo.web.datastore.history.AbstractHistoryStore;
import org.bbop.apollo.web.datastore.history.AbstractHistoryStoreManager;
import org.bbop.apollo.web.datastore.history.JEHistoryDatabase;
import org.bbop.apollo.web.datastore.history.Transaction;
import org.bbop.apollo.web.datastore.history.TransactionList;
import org.bbop.apollo.web.datastore.session.JEDatabaseSessionHybridArrayDataStore;
import org.bbop.apollo.web.datastore.session.JEDatabaseSessionMemoryDataStore;
import org.bbop.apollo.web.name.FeatureNameAdapter;
import org.bbop.apollo.web.name.HttpSessionTimeStampNameAdapter;
import org.bbop.apollo.web.name.AbstractNameAdapter;
import org.bbop.apollo.web.name.PreDefinedNameAdapter;
import org.bbop.apollo.web.overlap.Overlapper;
import org.bbop.apollo.web.user.Permission;
import org.bbop.apollo.web.user.UserManager;
import org.bbop.apollo.web.util.JSONUtil;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Comment;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.FlankingRegion;
import org.gmod.gbol.bioObject.Frameshift;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.GenericFeatureProperty;
import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.NonCanonicalFivePrimeSpliceSite;
import org.gmod.gbol.bioObject.NonCanonicalThreePrimeSpliceSite;
import org.gmod.gbol.bioObject.Pseudogene;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureProperty;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.gmod.gbol.util.SequenceUtil;
import org.gmod.gbol.util.SequenceUtil.TranslationTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class AnnotationEditorService
 */
@WebServlet("/AnnotationEditorService")
public class AnnotationEditorService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Set<String> operationsNotRequiringLogin;
	private static Set<String> operationsNotRequiringMutexes;

	private BioObjectConfiguration bioObjectConfiguration;
	private Map<String, AnnotationEditor> trackToEditor;
	private Map<String, Date> trackToLastAccess;
	private String dataStoreDirectory;
	private Map<String, FeatureLazyResidues> trackToSourceFeature;
	private Map<String, TranslationTable> trackToTranslationTable;
	private int defaultMinimumIntronSize;
	private int historySize;
	private Overlapper overlapper;
	private CannedComments cannedComments;
	private Map<String, SequenceSearchTool> sequenceSearchTools;
	private Collection<String> sequenceSearchToolsKeys;
	private Map<String, ServerConfiguration.DataAdapterGroupConfiguration> dataAdapters;
	private boolean useCDS;
	private boolean useMemoryStore;
	private Map<String, ServerConfiguration.AnnotationInfoEditorConfiguration> annotationInfoEditorConfigurations;
	private Thread cleanupThread;

	static {
		operationsNotRequiringMutexes = new HashSet<String>(Arrays.asList("get_user_permission", "search_sequence", "get_sequence_search_tools",
				"get_data_adapters", "get_annotation_info_editor_configuration", "get_canned_comments"));
		operationsNotRequiringLogin = new HashSet<String>(Arrays.asList("get_translation_table"));
	}
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AnnotationEditorService() {
        super();
    }

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			ServerConfiguration serverConfig = new ServerConfiguration(getServletContext().getResourceAsStream("/config/config.xml"));
			InputStream gbolMappingStream = getServletContext().getResourceAsStream(serverConfig.getGBOLMappingFile());
			bioObjectConfiguration = new BioObjectConfiguration(gbolMappingStream);
			gbolMappingStream.close();
			trackToEditor = new HashMap<String, AnnotationEditor>();
			dataStoreDirectory = serverConfig.getDataStoreDirectory();
			trackToSourceFeature = new HashMap<String, FeatureLazyResidues>();
			trackToLastAccess = new ConcurrentHashMap<String, Date>();
			trackToTranslationTable = new HashMap<String, TranslationTable>();
			Map<String, SequenceUtil.TranslationTable> ttables = new HashMap<String, SequenceUtil.TranslationTable>();
			for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {
				FeatureSequenceChunkManager chunkManager = FeatureSequenceChunkManager.getInstance(track.getName());
				chunkManager.setSequenceDirectory(track.getSourceFeature().getSequenceDirectory());
				chunkManager.setChunkSize(track.getSourceFeature().getSequenceChunkSize());
				chunkManager.setChunkPrefix(track.getSourceFeature().getSequenceChunkPrefix());
				chunkManager.setSequenceLength(track.getSourceFeature().getEnd());
				FeatureLazyResidues sourceFeature = new FeatureLazyResidues(track.getName());
                sourceFeature.setUniqueName(track.getSourceFeature().getUniqueName());
                sourceFeature.setFmin(track.getSourceFeature().getStart());
                sourceFeature.setFmax(track.getSourceFeature().getEnd());
                String [] type = track.getSourceFeature().getType().split(":");
                sourceFeature.setType(new CVTerm(type[1], new CV(type[0])));
				trackToSourceFeature.put(track.getName(), sourceFeature);
				if (track.getTranslationTable() != null) {
					TranslationTable ttable;
					if (ttables.containsKey(track.getTranslationTable())) {
						ttable = ttables.get(track.getTranslationTable());
					}
					else {
						ttable = SequenceUtil.getDefaultTranslationTable().cloneTable();
						BufferedReader reader = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream(track.getTranslationTable())));
						String line;
						while ((line = reader.readLine()) != null) {
							String []tokens = line.split("\t");
							String codon = tokens[0].toUpperCase();
							String aa = tokens[1].toUpperCase();
							ttable.getTranslationTable().put(codon, aa);
							if (aa.equals(TranslationTable.STOP)) {
								ttable.getStopCodons().add(codon);
								if (tokens.length == 3) {
									ttable.getAlternateTranslationTable().put(codon, tokens[2]);
								}
								else {
									ttable.getAlternateTranslationTable().remove(codon);
								}
							}
							else {
								ttable.getStopCodons().remove(codon);
								ttable.getAlternateTranslationTable().remove(codon);
							}
							if (tokens.length == 3) {
								if (tokens[2].equals("start")) {
									ttable.getStartCodons().add(codon);
								}
							}
							else {
								ttable.getStartCodons().remove(codon);
							}
						}
					}
					ttables.put(track.getTranslationTable(), ttable);
					trackToTranslationTable.put(track.getName(), ttable);
				}
				for (String donor : track.getSpliceDonorSites()) {
					SequenceUtil.addSpliceDonorSite(donor);
				}
				for (String acceptor : track.getSpliceAcceptorSites()) {
					SequenceUtil.addSpliceAcceptorSite(acceptor);
				}
			}
			defaultMinimumIntronSize = serverConfig.getDefaultMinimumIntronSize();
			historySize = serverConfig.getHistorySize();
			if (!UserManager.getInstance().isInitialized()) {
				ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfig.getUserDatabase();
				UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
			}
			if (serverConfig.getOverlapperClass() != null) {
				overlapper = (Overlapper)Class.forName(serverConfig.getOverlapperClass()).newInstance();
			}
			if (serverConfig.getCannedComments() != null) {
				cannedComments = new CannedComments((InputStream)getServletContext().getResourceAsStream(serverConfig.getCannedComments()));
			}
			sequenceSearchTools = new HashMap<String, SequenceSearchTool>();
			sequenceSearchToolsKeys = new ArrayList<String>();
			for (ServerConfiguration.SequenceSearchToolConfiguration conf : serverConfig.getSequenceSearchTools()) {
				SequenceSearchTool sequenceSearchTool = (SequenceSearchTool)Class.forName(conf.getClassName()).newInstance();
				sequenceSearchTool.parseConfiguration((InputStream)getServletContext().getResourceAsStream(conf.getConfigFilename()));
				sequenceSearchTool.setBioObjectConfiguration(bioObjectConfiguration);
				sequenceSearchTools.put(conf.getKey(), sequenceSearchTool);
				sequenceSearchToolsKeys.add(conf.getKey());
			}
			dataAdapters = serverConfig.getDataAdapters();
			useCDS = serverConfig.getUseCDS();
			useMemoryStore = serverConfig.getUseMemoryStore();
			annotationInfoEditorConfigurations = serverConfig.getAnnotationInfoEditor();
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
		
		cleanupThread = new Thread(new Runnable() {
			public void run() {
				
				final long SLEEP_TIME = 60 * 60 * 1000; //1 hour
				final long LAST_ACCESS_AGE = 30 * 60 * 1000; //30 minutes
				
				while (true) {
					try {
						boolean runGC = false;
						Thread.sleep(SLEEP_TIME);
						for (Map.Entry<String, Date> entry : trackToLastAccess.entrySet()) {
							long lastAccessedTime = entry.getValue().getTime();
							long currentTime = new Date().getTime();
							long difference = (currentTime - lastAccessedTime);
							if (difference >= LAST_ACCESS_AGE) {
								cleanup(entry.getKey());
								runGC = true;
							}
							if (runGC) {
								System.gc();
							}
						}
					}
					catch (Exception e) {
					}
				}
			}
		});
		cleanupThread.start();
		
	}

	@Override
	public void destroy() {
		for (AbstractDataStore dataStore : AbstractDataStoreManager.getInstance().getDataStores()) {
			dataStore.close();
		}
		for (AbstractHistoryStore historyStore : AbstractHistoryStoreManager.getInstance().getHistoryStores()) {
			historyStore.close();
		}
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		try {
			JSONObject json = JSONUtil.convertInputStreamToJSON(request.getInputStream());
			String operation = json.getString("operation");
			String track = json.getString("track");
			
			SessionData sessionData = getSessionData(track);
			AnnotationEditor editor = sessionData.getEditor();
			
			response.setContentType("application/json");
			/*
			BufferedWriter out;
			String encoding = request.getHeader("Accept-Encoding");
			boolean compress = false;
			if (encoding != null && encoding.contains("gzip")) {
				out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(response.getOutputStream())));
				compress = true;
//				out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
			}
			else {
				out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
			}
			*/
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
			
			if (!operationsNotRequiringLogin.contains(operation)) {
				if (session == null) {
					throw new AnnotationEditorServiceException("You must first login before editing");
				}
				String username = (String)session.getAttribute("username");
				if (username == null) {
					throw new AnnotationEditorServiceException("You must first login before editing");
				}
				Map<String, Integer> permissions = (Map<String, Integer>)session.getAttribute("permissions");
				int permission = Permission.NONE;
				if (!permissions.containsKey(track)) {
					permission = getUserPermission(track, username);
					permissions.put(track, permission);
				}
				else {
					permission = permissions.get(track);
				}
				if ((permission & Permission.READ) == 0) {
					throw new AnnotationEditorServiceException("You do not have read permissions");
				}
				boolean updateDataStore = true;
				if (json.has("update_datastore") && !json.getBoolean("update_datastore")) {
					updateDataStore = false;
				}
				AbstractDataStore dataStore = updateDataStore ? sessionData.getDataStore() : null;
				AbstractHistoryStore historyStore = updateDataStore ? sessionData.getHistoryStore() : null;
				
				if (!operationsNotRequiringMutexes.contains(operation)) {
					
					// start of operations that need mutexes
					synchronized (editor) {

						// get_organism
						if (operation.equals("get_organism")) {
							getOrganism(editor, out);
						}

						// set_organism
						else if (operation.equals("set_organism")) {
							setOrganism(editor, json.getJSONObject("organism"), out);
						}

						// get_source_feature
						else if (operation.equals("get_source_feature")) {
							getSourceFeature(editor, track, out);
						}

						// set_source_feature
						else if (operation.equals("set_source_feature")) {
							setSourceFeature(editor, json.getJSONArray("features").getJSONObject(0), out);
						}

						// get_name
						else if (operation.equals("get_name")) {
							getName(editor, session, json.getJSONArray("features"), track, out);
						}

						// set_name
						else if (operation.equals("set_name")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setName(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// add_feature
						else if (operation.equals("add_feature")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addFeature(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// delete_feature
						else if (operation.equals("delete_feature")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteFeature(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// get_features
						else if (operation.equals("get_features")) {
							getFeatures(editor, out);
						}

						// add_transcript
						else if (operation.equals("add_transcript")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addTranscript(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// duplicate_transcript
						else if (operation.equals("duplicate_transcript")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							duplicateTranscript(editor, dataStore, json.getJSONArray("features").getJSONObject(0), track, out);
						}

						// merge_transcripts
						else if (operation.equals("merge_transcripts")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							JSONArray transcripts = json.getJSONArray("features");
							mergeTranscripts(editor, dataStore, historyStore, transcripts.getJSONObject(0), transcripts.getJSONObject(1), track, username, out);
						}

						// set_translation_start
						else if (operation.equals("set_translation_start")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setTranslationStart(editor, dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}

						// set_translation_end
						else if (operation.equals("set_translation_end")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setTranslationEnd(editor, dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}

						// set_translation_ends
						else if (operation.equals("set_translation_ends")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setTranslationEnds(editor, dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}

						// set_longest_orf
						else if (operation.equals("set_longest_orf")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setLongestORF(editor, dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}

						// add_exon
						else if (operation.equals("add_exon")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addExon(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// delete_exon
						else if (operation.equals("delete_exon")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteExon(editor, dataStore, json.getJSONArray("features"), track, out);
						}

						// merge_exons
						else if (operation.equals("merge_exons")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							mergeExons(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// split_exons
						else if (operation.equals("split_exon")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							splitExon(editor, session, new HttpSessionTimeStampNameAdapter(session, editor.getSession()), dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}

						// make_intron
						else if (operation.equals("make_intron")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							makeIntron(editor, session, new HttpSessionTimeStampNameAdapter(session, editor.getSession()), dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}

						// split_transcript
						else if (operation.equals("split_transcript")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							HttpSessionTimeStampNameAdapter nameAdapter = new HttpSessionTimeStampNameAdapter(session, editor.getSession());
							splitTranscript(editor, session, nameAdapter, nameAdapter, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// add_sequence_alteration
						else if (operation.equals("add_sequence_alteration")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addSequenceAlteration(editor, session, dataStore, json.getJSONArray("features"), track, out);
						}

						// delete_sequence_alteration
						else if (operation.equals("delete_sequence_alteration")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteSequenceAlteration(editor, dataStore, json.getJSONArray("features"), track, out);
						}

						// get_sequence_alterations
						else if (operation.equals("get_sequence_alterations")) {
							getSequenceAlterations(editor, out);
						}

						// get_residues_with_alterations
						else if (operation.equals("get_residues_with_alterations")) {
							getResiduesWithAlterations(editor, json.getJSONArray("features"), out);
						}

						// add_frameshift
						else if (operation.equals("add_frameshift")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addFrameshift(editor, dataStore, json.getJSONArray("features"), track, out);
						}

						// delete_frameshift
						else if (operation.equals("delete_frameshift")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteFrameshift(editor, dataStore, json.getJSONArray("features"), track, out);
						}

						// get_residues_with_frameshifts
						else if (operation.equals("get_residues_with_frameshifts")) {
							getResiduesWithFrameshifts(editor, json.getJSONArray("features"), out);
						}

						// get_residues_with_alterations_and_frameshifts
						else if (operation.equals("get_residues_with_alterations_and_frameshifts")) {
							getResiduesWithAlterationsAndFrameshifts(editor, json.getJSONArray("features"), out);
						}

						// set_exon_boundaries
						else if (operation.equals("set_exon_boundaries")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setExonBoundaries(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// set_boundaries
						else if (operation.equals("set_boundaries")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setBoundaries(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}
						
						// set_to_downstream_donor
						else if (operation.equals("set_to_downstream_donor")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setToDownstreamDonor(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// set_to_upstream_donor
						else if (operation.equals("set_to_upstream_donor")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setToUpstreamDonor(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// set_to_downstream_acceptor
						else if (operation.equals("set_to_downstream_acceptor")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setToDownstreamAcceptor(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// set_to_upstream_acceptor
						else if (operation.equals("set_to_upstream_acceptor")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setToUpstreamAcceptor(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}
						
						// lock_feature
						else if (operation.equals("lock_feature")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							lockFeature(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, permission, out);
						}

						// unlock_feature
						else if (operation.equals("unlock_feature")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							unlockFeature(editor, dataStore, historyStore, json.getJSONArray("features"), track, username, permission, out);
						}

						// undo
						else if (operation.equals("undo")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							undo(editor, session, dataStore, historyStore, json, track, out, json.has("count") ? json.getInt("count") : 1);
						}

						// redo
						else if (operation.equals("redo")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							redo(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, username, out, json.has("count") ? json.getInt("count") : 1);
						}

						// get_information
						else if (operation.equals("get_information")) {
							getInformation(editor, json.getJSONArray("features"), out);
						}

						// get_sequence
						else if (operation.equals("get_sequence")) {
							getSequence(editor, json.getJSONArray("features"), json.getString("type"), json.has("flank") ? json.getInt("flank") : 0, out);
						}

						// flip_strand
						else if (operation.equals("flip_strand")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							flipStrand(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, username, out);
						}

						// get_comments
						else if (operation.equals("get_comments")) {
							getComments(editor, session, json.getJSONArray("features"), track, out);
						}

						// add_comments
						else if (operation.equals("add_comments")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addComments(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// delete_comments
						else if (operation.equals("delete_comments")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteComments(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// update_comments
						else if (operation.equals("update_comments")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							updateComments(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// get_description
						else if (operation.equals("get_description")) {
							getDescription(editor, session, json.getJSONArray("features"), track, out);
						}

						// set_description
						else if (operation.equals("set_description")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setDescription(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// get_status
						else if (operation.equals("get_status")) {
							getStatus(editor, session, json.getJSONArray("features"), track, out);
						}

						// set_status
						else if (operation.equals("set_status")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setStatus(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// delete_status
						else if (operation.equals("delete_status")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteStatus(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// get_symbol
						else if (operation.equals("get_symbol")) {
							getSymbol(editor, session, json.getJSONArray("features"), track, out);
						}

						// set_symbol
						else if (operation.equals("set_symbol")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							setSymbol(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// get_non_primary_dbxrefs
						else if (operation.equals("get_non_primary_dbxrefs")) {
							getNonPrimaryDBXrefs(editor, session, json.getJSONArray("features"), track, out);
						}

						// add_non_primary_dbxrefs
						else if (operation.equals("add_non_primary_dbxrefs")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addNonPrimaryDBXrefs(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// delete_non_primary_dbxrefs
						else if (operation.equals("delete_non_primary_dbxrefs")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteNonPrimaryDBXrefs(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// update_non_primary_dbxrefs
						else if (operation.equals("update_non_primary_dbxrefs")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							updateNonPrimaryDBXrefs(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// get_non_reserved_properties
						else if (operation.equals("get_non_reserved_properties")) {
							getNonReservedProperties(editor, session, json.getJSONArray("features"), track, out);
						}

						// add_non_reserved_properties
						else if (operation.equals("add_non_reserved_properties")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							addNonReservedProperties(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// delete_non_reserved_properties
						else if (operation.equals("delete_non_reserved_properties")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							deleteNonReservedProperties(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}

						// update_non_reserved_properties
						else if (operation.equals("update_non_reserved_properties")) {
							if ((permission & Permission.WRITE) == 0) {
								throw new AnnotationEditorServiceException("You do not have editing permissions");
							}
							updateNonReservedProperties(editor, session, dataStore, historyStore, json.getJSONArray("features"), track, out);
						}
						
						// get_annotation_info_editor_data
						else if (operation.equals("get_annotation_info_editor_data")) {
							getAnnotationInfoEditorData(editor, session, json.getJSONArray("features"), track, out);
						}

						// get_history_for_features
						else if (operation.equals("get_history_for_features")) {
							getHistoryForFeatures(editor, historyStore, json.getJSONArray("features"), out);
						}

						// set_readthrough_stop_codon
						else if (operation.equals("set_readthrough_stop_codon")) {
							setReadthroughStopCodon(editor, dataStore, historyStore, json.getJSONArray("features").getJSONObject(0), track, username, out);
						}
						
						// release_resources
						else if (operation.equals("release_resources")) {
							releaseResources(track, out);
						}
						
					}
					// end of operations needing mutexes
				}

				// start of operations not needing mutexes
				else {
					
					// get_user_permissions
					if (operation.equals("get_user_permission")) {
						getUserPermission(permission, track, (String)session.getAttribute("username"), out);
					}

					// search_sequence
					else if (operation.equals("search_sequence")) {
						searchSequence(editor, session, json.getJSONObject("search"), out);
					}

					// get_sequence_search_tools
					else if (operation.equals("get_sequence_search_tools")) {
						getSequenceSearchTools(out);
					}

					// get_data_adapters
					else if (operation.equals("get_data_adapters")) {
						getDataAdapters(out);
					}

					// get_annotation_info_editor_configuration
					else if (operation.equals("get_annotation_info_editor_configuration")) {
						getAnnotationInfoEditorConfiguration(out);
					}

					// get_canned_comments
					else if (operation.equals("get_canned_comments")) {
						getCannedComments(editor, json.getJSONArray("features"), track, out);
					}
					
				}
				// end of operations not needing mutexes

			}

			// start of operations not needing login
			else {

				//get_translation_table
				if (operation.equals("get_translation_table")) {
					getTranslationTable(editor, track, out);
				}

			}
			// end of operations not needing login
			
			/*
			if (compress) {
				response.addHeader("Content-encoding", "gzip");
				out.close();
			}
			else {
				out.flush();
			}
			*/
			out.flush();
			
		} catch (JSONException e) {
			try {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
//			throw new ServletException(e);
		} catch (AnnotationEditorException e) {
			try {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
		}
			catch (JSONException e2) {
			}
//			throw new ServletException(e);
		} catch (AnnotationEditorServiceException e) {
			try {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
//			throw new ServletException(e);
		} catch (SQLException e) {
			try {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (SequenceSearchToolException e) {
			try {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		}
	}
	
	private JSONObject createJSONFeatureContainer(JSONObject ... features) throws JSONException {
		JSONObject jsonFeatureContainer = new JSONObject();
		JSONArray jsonFeatures = new JSONArray();
		jsonFeatureContainer.put("features", jsonFeatures);
		for (JSONObject feature : features) {
			jsonFeatures.put(feature);
		}
		return jsonFeatureContainer;
	}
	
	private void getOrganism(AnnotationEditor editor, BufferedWriter out) throws JSONException, IOException {
		JSONObject organism = new JSONObject();
		if (editor.getSession().getOrganism() == null) {
			return;
		}
		organism.put("genus", editor.getSession().getOrganism().getGenus());
		organism.put("species", editor.getSession().getOrganism().getSpecies());
		out.write(organism.toString());
	}
	
	private void setOrganism(AnnotationEditor editor, JSONObject organism, BufferedWriter out) throws JSONException, IOException {
		editor.getSession().setOrganism(new Organism(organism.getString("genus"), organism.getString("species")));
		getOrganism(editor, out);
	}
	
	private void getSourceFeature(AnnotationEditor editor, String track, BufferedWriter out) throws JSONException, IOException {
		Feature gsolFeature = trackToSourceFeature.get(track);
		JSONObject featureContainer = createJSONFeatureContainer();
		if (gsolFeature != null) {
			FeatureLazyResidues sourceFeature = trackToSourceFeature.get(track);
			JSONObject jsonFeature = JSONUtil.convertFeatureToJSON(sourceFeature, false);
			jsonFeature.put("fmin", sourceFeature.getFmin());
			jsonFeature.put("fmax", sourceFeature.getFmax());
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void setSourceFeature(AnnotationEditor editor, JSONObject sourceFeature, BufferedWriter out) throws JSONException, IOException {
		Feature gsolSourceFeature = JSONUtil.convertJSONToFeature(sourceFeature, bioObjectConfiguration, editor.getSession().getOrganism());
		editor.getSession().setSourceFeature(gsolSourceFeature);
		out.write(createJSONFeatureContainer(JSONUtil.convertFeatureToJSON(gsolSourceFeature)).toString());
	}
	
	
	private void getName(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			jsonFeature.put("type", JSONUtil.convertCVTermToJSON(feature.getType()));
			if (feature.getName() != null) {
				jsonFeature.put("name", feature.getName());
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void setName(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			editor.setName(feature, jsonFeature.getString("name"));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
					updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
				}
				else {
					if (!(feature instanceof Gene)) {
						updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
					}
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
		if (updateFeatureContainer.getJSONArray("features").length() > 0) {
			fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
		}
	}

	private void addFeature(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		addFeature(editor, session, dataStore, historyStore, features, track, username, out, true);
	}
	
	private void addFeature(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out, boolean fireUpdateChange) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			Feature gsolFeature = JSONUtil.convertJSONToFeature(features.getJSONObject(i), bioObjectConfiguration, trackToSourceFeature.get(track), new HttpSessionTimeStampNameAdapter(session, editor.getSession()));
			updateNewGsolFeatureAttributes(gsolFeature, trackToSourceFeature.get(track));
			AbstractSingleLocationBioFeature gbolFeature = (AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(gsolFeature, bioObjectConfiguration);
			if (gbolFeature.getFmin() < 0 || gbolFeature.getFmax() < 0) {
				throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
			}
			setOwner(gbolFeature, (String)session.getAttribute("username"));
			editor.addFeature(gbolFeature);
			if (gbolFeature instanceof Gene) {
				for (Transcript transcript : ((Gene)gbolFeature).getTranscripts()) {
					if (!((Gene)gbolFeature).isPseudogene() && transcript.isProteinCoding()) {
						if (!useCDS || transcript.getCDS() == null) {
							calculateCDS(editor, transcript);
						}
					}
					else {
						if (transcript.getCDS() != null) {
							transcript.deleteCDS();
						}
					}
					findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
					updateTranscriptAttributes(transcript);
					featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
				}
			}
			else {
				featureContainer.getJSONArray("features").put(JSONUtil.convertFeatureToJSON(gsolFeature));
			}
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, gbolFeature, track);
			}
			if (historyStore != null) {
				if (gbolFeature instanceof Gene) {
					for (Transcript transcript : ((Gene)gbolFeature).getTranscripts()) {
						Transaction transaction = new Transaction(Transaction.Operation.ADD_FEATURE, transcript.getUniqueName(), username);
						transaction.addNewFeature(transcript);
						writeHistoryToStore(historyStore, transaction);
					}
				}
				else {
					Transaction transaction = new Transaction(Transaction.Operation.ADD_FEATURE, gbolFeature.getUniqueName(), username);
					transaction.addNewFeature(gbolFeature);
					writeHistoryToStore(historyStore, transaction);
				}
			}
		}
		if (out != null) {
			out.write(featureContainer.toString());
		}
		if (fireUpdateChange) {
			fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.ADD);
		}
	}

	private void deleteFeature(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		deleteFeature(editor, dataStore, historyStore, features, track, username, out, true);
	}
	
	private void deleteFeature(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out, boolean fireUpdateChange) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		Map<String, List<AbstractSingleLocationBioFeature>> modifiedFeaturesUniqueNames = new HashMap<String, List<AbstractSingleLocationBioFeature>>();
		
		boolean isUpdateOperation = false;
		for (int i = 0; i < features.length(); ++i) {
			AbstractSingleLocationBioFeature feature = getFeature(editor, features.getJSONObject(i));
			if (feature != null) {
				isUpdateOperation = deleteFeature(editor, feature, modifiedFeaturesUniqueNames);
			}
		}
		for (Map.Entry<String, List<AbstractSingleLocationBioFeature>> entry : modifiedFeaturesUniqueNames.entrySet()) {
			String uniqueName = entry.getKey();
			List<AbstractSingleLocationBioFeature> deletedFeatures = entry.getValue();
			AbstractSingleLocationBioFeature feature = editor.getSession().getFeatureByUniqueName(uniqueName);
			SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
			Feature gsolFeature = (Feature)iterator.next();
			if (!isUpdateOperation) {
				featureContainer.getJSONArray("features").put(new JSONObject().put("uniquename", uniqueName));

				if (feature instanceof Transcript) {
					Transcript transcript = (Transcript)feature;
					Gene gene = transcript.getGene();
					editor.deleteTranscript(transcript.getGene(), transcript);
					if (gene.getTranscripts().size() == 0) {
						editor.deleteFeature(gene);
					}
					if (dataStore != null) {
						if (gene.getTranscripts().size() > 0) {
							dataStore.writeFeature(gene);
						}
						else {
							dataStore.deleteFeature(gene);
						}
					}
					
//					editor.getSession().endTransactionForFeature(gene);
					
				}
				else {
					editor.deleteFeature(feature);
					if (dataStore != null) {
						dataStore.deleteFeature(gsolFeature);
					}
					
//					editor.getSession().endTransactionForFeature(feature);
					
				}
				
				if (historyStore != null) {
//					Transaction transaction = new Transaction(operation, feature.getUniqueName());
//					transaction.setAttribute("feature", feature);
//					transaction.addOldFeature(feature);
//					writeHistoryToStore(historyStore, transaction);
					List<String> toBeDeleted = new ArrayList<String>();
					toBeDeleted.add(feature.getUniqueName());
					while (!toBeDeleted.isEmpty()) {
						String id = toBeDeleted.remove(toBeDeleted.size() - 1);
						for (Transaction t : historyStore.getTransactionListForFeature(id)) {
							if (t.getOperation().equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
								if (editor.getSession().getFeatureByUniqueName(t.getOldFeatures().get(1).getUniqueName()) == null) {
									toBeDeleted.add(t.getOldFeatures().get(1).getUniqueName());
								}
							}
						}
						historyStore.deleteHistoryForFeature(id);
					}
				}
			}
			else {
				Transaction.Operation operation;
				if (feature instanceof Transcript) {
					Transcript transcript = (Transcript)feature;
					calculateCDS(editor, transcript);
					findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
					updateTranscriptAttributes(transcript);
					operation = Transaction.Operation.DELETE_EXON;
					if (dataStore != null) {
						dataStore.writeFeature(transcript.getGene());
					}
					
//					editor.getSession().endTransactionForFeature(transcript);
					
				}
				else {
					operation = Transaction.Operation.DELETE_FEATURE;
					if (dataStore != null) {
						dataStore.writeFeature(gsolFeature);
					}
					
//					editor.getSession().endTransactionForFeature(feature);
					
				}
				if (historyStore != null) {
					Transaction transaction = new Transaction(operation, feature.getUniqueName(), username);
//					transaction.setAttribute("feature", feature);
//					transaction.setAttribute("children", deletedFeatures);
					transaction.getOldFeatures().addAll(deletedFeatures);
					if (operation == Transaction.Operation.DELETE_EXON) {
						transaction.addNewFeature(feature);
					}
					writeHistoryToStore(historyStore, transaction);
				}
				featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(editor.getSession().getFeatureByUniqueName(uniqueName)));
			}
		}
		if (fireUpdateChange) {
			if (!isUpdateOperation) {
				fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.DELETE);
			}
			else {
				fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
			}
		}
		if (out != null) {
			out.write(createJSONFeatureContainer().toString());
		}
		
		editor.getSession().endTransactionForAllFeatures();
		
	}

	private boolean deleteFeature(AnnotationEditor editor, AbstractSingleLocationBioFeature feature, Map<String, List<AbstractSingleLocationBioFeature>> modifiedFeaturesUniqueNames) throws JSONException {
		if (feature instanceof Exon) {
			Exon exon = (Exon)feature;
			Transcript transcript = (Transcript)editor.getSession().getFeatureByUniqueName(exon.getTranscript().getUniqueName());

			if (!transcript.getGene().isPseudogene() && transcript.isProteinCoding()) {
				CDS cds = transcript.getCDS();
				if (editor.isManuallySetTranslationStart(cds)) {
					int cdsStart = cds.getStrand() == -1 ? cds.getFmax() : cds.getFmin();
					if (cdsStart >= exon.getFmin() && cdsStart <= exon.getFmax()) {
						editor.setManuallySetTranslationStart(cds, false);
					}
				}
			}

			editor.deleteExon(transcript, exon);
			List<AbstractSingleLocationBioFeature> deletedFeatures = modifiedFeaturesUniqueNames.get(transcript.getUniqueName());
			if (deletedFeatures == null) {
				deletedFeatures = new ArrayList<AbstractSingleLocationBioFeature>();
				modifiedFeaturesUniqueNames.put(transcript.getUniqueName(), deletedFeatures);
			}
			deletedFeatures.add(exon);
			return transcript.getNumberOfExons() > 0;
		}
		else {
			List<AbstractSingleLocationBioFeature> deletedFeatures = modifiedFeaturesUniqueNames.get(feature.getUniqueName());
			if (deletedFeatures == null) {
				deletedFeatures = new ArrayList<AbstractSingleLocationBioFeature>();
				modifiedFeaturesUniqueNames.put(feature.getUniqueName(), deletedFeatures);
			}
			deletedFeatures.add(feature);
			return false;
		}
	}
	
	private void getFeatures(AnnotationEditor editor, BufferedWriter out) throws JSONException, IOException {
		JSONObject jsonFeatureContainer = createJSONFeatureContainer();
		JSONArray jsonFeatures = jsonFeatureContainer.getJSONArray("features");
		for (AbstractSingleLocationBioFeature gbolFeature : editor.getSession().getFeatures()) {
			if (gbolFeature instanceof Gene) {
				for (Transcript transcript : ((Gene)gbolFeature).getTranscripts()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
				}
			}
			else {
				jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(gbolFeature));
			}
		}
		out.write(jsonFeatureContainer.toString());
	}

	private void addTranscript(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		addTranscript(editor, session, dataStore, historyStore, features, track, username, out, true);
	}
	
	private void addTranscript(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out, boolean fireUpdateChange) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		AbstractNameAdapter nameAdapter = new HttpSessionTimeStampNameAdapter(session, editor.getSession());
//		StringBuilder errors = new StringBuilder();;
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonTranscript = features.getJSONObject(i);
//			FeatureLazyResidues sourceFeature = trackToSourceFeature.get(track);
//			JSONObject location = jsonTranscript.getJSONObject("location");
//			if (location.getInt("fmin") < sourceFeature.getFmin() || location.getInt("fmax") > sourceFeature.getFmax()) {
//				errors.append(String.format("Invalid location for transcript: %d, %d, %d [%s]\n", location.getInt("fmin"), location.getInt("fmax"), location.getInt("strand"), sourceFeature.getUniqueName()));
//			}
//			else {
			try {
				Transcript transcript = addTranscript(editor, session, jsonTranscript, track, nameAdapter, false);
				featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
				if (dataStore != null) {
					writeFeatureToStore(editor, dataStore, transcript.getGene(), track);
				}
				if (historyStore != null) {
					Transaction transaction = new Transaction(Transaction.Operation.ADD_TRANSCRIPT, transcript.getUniqueName(), username);
					transaction.addNewFeature(transcript);
					writeHistoryToStore(historyStore, transaction);
				}
			}
			catch (Exception e) {
				deleteFeature(editor, dataStore, historyStore, featureContainer.getJSONArray("features"), track, username, out, false);
				e.printStackTrace();
				throw new AnnotationEditorServiceException("Error writing transcript: " + e.getMessage(), e);
			}
		}
		if (fireUpdateChange) {
			fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.ADD);
		}
//		if (errors.length() > 0) {
//			throw new AnnotationEditorServiceException(errors.toString());
//		}
//		else {
			if (out != null) {
				out.write(featureContainer.toString());
			}
//		}
	}

	private void duplicateTranscript(AnnotationEditor editor, AbstractDataStore dataStore, JSONObject jsonTranscript, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		editor.duplicateTranscript(transcript);
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
	}
	
	private void mergeTranscripts(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonTranscript1, JSONObject jsonTranscript2, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript1 = (Transcript)getFeature(editor, jsonTranscript1);
		Transcript transcript2 = (Transcript)getFeature(editor, jsonTranscript2);
		// cannot merge transcripts from different strands
		if (!transcript1.getStrand().equals(transcript2.getStrand())) {
			throw new AnnotationEditorServiceException("You cannot merge transcripts on opposite strands");
		}
		Gene gene2 = transcript2.getGene();
		Transcript oldTranscript1 = cloneTranscript(transcript1, true);
		Transcript oldTranscript2 = cloneTranscript(transcript2, true);
		editor.mergeTranscripts(transcript1, transcript2);
		calculateCDS(editor, transcript1);
		findNonCanonicalAcceptorDonorSpliceSites(editor, transcript1);
		updateTranscriptAttributes(transcript1);
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		JSONObject deleteFeatureContainer = createJSONFeatureContainer();
		if (historyStore != null) {
			Transaction transaction1 = new Transaction(Transaction.Operation.MERGE_TRANSCRIPTS, transcript1.getUniqueName(), username);
			transaction1.addOldFeature(oldTranscript1);
			transaction1.addOldFeature(oldTranscript2);
			transaction1.addNewFeature(transcript1);
			historyStore.addTransaction(transaction1);
			
			Transaction transaction2 = new Transaction(Transaction.Operation.MERGE_TRANSCRIPTS, transcript2.getUniqueName(), username);
			transaction2.addOldFeature(oldTranscript1);
			transaction2.addOldFeature(oldTranscript2);
			transaction2.addNewFeature(transcript1);
			historyStore.addTransaction(transaction2);
			
			/*
			if (gene2.getTranscripts().size() == 0) {
				historyStore.deleteHistoryForFeature(transcript2.getUniqueName());
			}
			*/
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript1), track);
			if (!transcript1.getGene().equals(gene2)) {
				deleteFeatureFromStore(dataStore, gene2);
			}
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript1))).toString());
		for (Transcript transcript : transcript1.getGene().getTranscripts()) {
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
		}
		deleteFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript2));
		fireDataStoreChange(new DataStoreChangeEvent(this, updateFeatureContainer, track, DataStoreChangeEvent.Operation.UPDATE),
				new DataStoreChangeEvent(this, deleteFeatureContainer, track, DataStoreChangeEvent.Operation.DELETE));
	}
	
	private void flipStrand(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		AbstractNameAdapter nameAdapter = new HttpSessionTimeStampNameAdapter(session, editor.getSession());
		for (int i = 0; i < features.length(); ++i) {
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, features.getJSONObject(i));
			if (feature instanceof Transcript) {
				feature = flipTranscriptStrand(editor, session, dataStore, historyStore, (Transcript)feature, track, username, nameAdapter);
			}
			else {
				flipFeatureStrand(editor, session, dataStore, historyStore, feature, track, username);
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}

	private void getComments(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray comments = new JSONArray();
			jsonFeature.put("comments", comments);
			for (Comment comment : feature.getComments()) {
				comments.put(comment.getComment());
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void getCannedComments(AnnotationEditor editor, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray comments = new JSONArray();
			jsonFeature.put("comments", comments);
			Collection<String> cannedCommentsForType = cannedComments.getCannedCommentsForType(feature.getType());
			if (cannedCommentsForType != null) {
				for (String comment : cannedComments.getCannedCommentsForType(feature.getType())) {
					jsonFeature.put("comments", comments);
					comments.put(comment);
				}
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void addComments(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray comments = jsonFeature.getJSONArray("comments");
			for (int j = 0; j < comments.length(); ++j) {
				String comment = comments.getString(j);
				editor.addComment(feature, comment);
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}

	private void deleteComments(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray comments = jsonFeature.getJSONArray("comments");
			for (int j = 0; j < comments.length(); ++j) {
				String comment = comments.getString(j);
				editor.deleteComment(feature, comment);
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}

	private void updateComments(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray oldComments = jsonFeature.getJSONArray("old_comments");
			JSONArray newComments = jsonFeature.getJSONArray("new_comments");
			for (int j = 0; j < oldComments.length(); ++j) {
				String oldComment = oldComments.getString(i);
				String newComment = newComments.getString(i);
				editor.updateComment(feature, oldComment, newComment);
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}
	
	private void getDescription(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			if (feature.getDescription() != null) {
				jsonFeature.put("description", feature.getDescription().getDescription());
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void setDescription(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			String description = jsonFeature.getString("description");
			editor.setDescription(feature, description);
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}

	private void getSymbol(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			if (feature.getSymbol() != null) {
				jsonFeature.put("symbol", feature.getSymbol().getSymbol());
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void setSymbol(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			String symbol = jsonFeature.getString("symbol");
			editor.setSymbol(feature, symbol);
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}
	
	private void getStatus(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			if (feature.getStatus() != null) {
				jsonFeature.put("status", feature.getStatus().getStatus());
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void setStatus(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			String status = jsonFeature.getString("status");
			editor.setStatus(feature, status);
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}
	
	private void deleteStatus(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			editor.deleteStatus(feature);
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}
	
	private void getNonPrimaryDBXrefs(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray dbxrefs = new JSONArray();
			jsonFeature.put("dbxrefs", dbxrefs);
			for (DBXref dbxref : feature.getNonPrimaryDBXrefs()) {
				JSONObject jsonDbxref = new JSONObject();
				jsonDbxref.put("db", dbxref.getDb().getName());
				jsonDbxref.put("accession", dbxref.getAccession());
				dbxrefs.put(jsonDbxref);
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}

	private void addNonPrimaryDBXrefs(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray dbxrefs = jsonFeature.getJSONArray("dbxrefs");
			for (int j = 0; j < dbxrefs.length(); ++j) {
				JSONObject dbxref = dbxrefs.getJSONObject(i);
				editor.addNonPrimaryDBXref(feature, dbxref.getString("db"), dbxref.getString("accession"));
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}	

	private void deleteNonPrimaryDBXrefs(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray dbxrefs = jsonFeature.getJSONArray("dbxrefs");
			for (int j = 0; j < dbxrefs.length(); ++j) {
				JSONObject dbxref = dbxrefs.getJSONObject(j);
				editor.deleteNonPrimaryDBXref(feature, dbxref.getString("db"), dbxref.getString("accession"));
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}

	private void updateNonPrimaryDBXrefs(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray oldDbxrefs = jsonFeature.getJSONArray("old_dbxrefs");
			JSONArray newDbxrefs = jsonFeature.getJSONArray("new_dbxrefs");
			for (int j = 0; j < oldDbxrefs.length(); ++j) {
				JSONObject oldDbxref = oldDbxrefs.getJSONObject(i);
				JSONObject newDbxref = newDbxrefs.getJSONObject(i);
				editor.updateNonPrimaryDBXref(feature, oldDbxref.getString("db"), oldDbxref.getString("accession"), newDbxref.getString("db"), newDbxref.getString("accession"));
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}
	
	private void getNonReservedProperties(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray properties = new JSONArray();
			jsonFeature.put("non_reserved_properties", properties);
			for (GenericFeatureProperty property : feature.getNonReservedProperties()) {
				JSONObject jsonProperty = new JSONObject();
				jsonProperty.put("tag", property.getTag());
				jsonProperty.put("value", property.getValue());
				properties.put(jsonProperty);
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}

	private void addNonReservedProperties(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray properties = jsonFeature.getJSONArray("non_reserved_properties");
			for (int j = 0; j < properties.length(); ++j) {
				JSONObject property = properties.getJSONObject(i);
				editor.addNonReservedProperty(feature, property.getString("tag"), property.getString("value"));
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}	

	private void deleteNonReservedProperties(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray properties = jsonFeature.getJSONArray("non_reserved_properties");
			for (int j = 0; j < properties.length(); ++j) {
				JSONObject property = properties.getJSONObject(j);
				editor.deleteNonReservedProperty(feature, property.getString("tag"), property.getString("value"));
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}

	private void updateNonReservedProperties(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			JSONArray oldProperties = jsonFeature.getJSONArray("old_non_reserved_properties");
			JSONArray newProperties = jsonFeature.getJSONArray("new_non_reserved_properties");
			for (int j = 0; j < oldProperties.length(); ++j) {
				JSONObject oldProperty = oldProperties.getJSONObject(i);
				JSONObject newProperty = newProperties.getJSONObject(i);
				editor.updateNonReservedProperty(feature, oldProperty.getString("tag"), oldProperty.getString("value"), newProperty.getString("tag"), newProperty.getString("value"));
			}
			updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		if (out != null) {
			out.write(updateFeatureContainer.toString());
		}
//		fireDataStoreChange(updateFeatureContainer, track, Operation.UPDATE);
	}
	
	private void getAnnotationInfoEditorData(AnnotationEditor editor, HttpSession session, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)getFeature(editor, jsonFeature);
			jsonFeature.put("type", JSONUtil.convertCVTermToJSON(feature.getType()));
			jsonFeature.put("name", feature.getName());
			if (feature.getSymbol() != null) {
				jsonFeature.put("symbol", feature.getSymbol().getSymbol());
			}
			if (feature.getDescription() != null) {
				jsonFeature.put("description", feature.getDescription().getDescription());
			}
			if (feature.getTimeAccessioned() != null) {
				jsonFeature.put("date_creation", feature.getTimeAccessioned().getTime());
			}
			if (feature.getTimeLastModified() != null) {
				jsonFeature.put("date_last_modified", feature.getTimeLastModified().getTime());
			}
			ServerConfiguration.AnnotationInfoEditorConfiguration conf = annotationInfoEditorConfigurations.get(feature.getType());
			if (conf == null) {
				conf = annotationInfoEditorConfigurations.get("default");
			}
			if (conf.hasStatus() && feature.getStatus() != null) {
				jsonFeature.put("status", feature.getStatus().getStatus());
			}
			if (conf.hasAttributes()) {
				JSONArray properties = new JSONArray();
				jsonFeature.put("non_reserved_properties", properties);
				for (GenericFeatureProperty property : feature.getNonReservedProperties()) {
					JSONObject jsonProperty = new JSONObject();
					jsonProperty.put("tag", property.getTag());
					jsonProperty.put("value", property.getValue());
					properties.put(jsonProperty);
				}
			}
			if (conf.hasDbxrefs() || conf.hasPubmedIds() || conf.hasGoIds()) {
				JSONArray dbxrefs = new JSONArray();
				jsonFeature.put("dbxrefs", dbxrefs);
				for (DBXref dbxref : feature.getNonPrimaryDBXrefs()) {
					JSONObject jsonDbxref = new JSONObject();
					jsonDbxref.put("db", dbxref.getDb().getName());
					jsonDbxref.put("accession", dbxref.getAccession());
					dbxrefs.put(jsonDbxref);
				}
			}
			if (conf.hasComments()) {
				JSONArray comments = new JSONArray();
				jsonFeature.put("comments", comments);
				for (Comment comment : feature.getComments()) {
					comments.put(comment.getComment());
				}
				JSONArray cannedComments = new JSONArray();
				jsonFeature.put("canned_comments", cannedComments);
				Collection<String> cc = this.cannedComments.getCannedCommentsForType(feature.getType());
				if (cc != null) {
					for (String comment : cc) {
						cannedComments.put(comment);
					}
				}
			}
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void searchSequence(AnnotationEditor editor, HttpSession session, JSONObject search, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException, SequenceSearchToolException {
		if (sequenceSearchTools.size() == 0) {
			throw new AnnotationEditorServiceException("No search tools configured");
		}
		SequenceSearchTool sequenceSearchTool = sequenceSearchTools.get(search.getString("key"));
		if (sequenceSearchTool == null) {
			throw new AnnotationEditorServiceException("No search tool configured for key: " + search.getString("key"));
		}
		JSONObject matchContainer = new JSONObject().put("matches", new JSONArray());
		String query = search.getString("residues");
		String databaseId = search.has("database_id") ? search.getString("database_id") : null;
		
		for (Match match : sequenceSearchTool.search(session.getId(), query, databaseId)) {
			matchContainer.getJSONArray("matches").put(JSONUtil.convertMatchToJSON(match));
		}
		out.write(matchContainer.toString());
	}
	
	private void getSequenceSearchTools(BufferedWriter out) throws JSONException, IOException {
		JSONArray sequenceSearchToolsArray = new JSONArray();
		JSONObject sequenceSearchToolsContainer = new JSONObject().put("sequence_search_tools", sequenceSearchToolsArray);
		for (String key : sequenceSearchToolsKeys) {
			sequenceSearchToolsArray.put(key);
		}
		out.write(sequenceSearchToolsContainer.toString());
	}
	
	private void releaseResources(String track, BufferedWriter out) throws JSONException, IOException {
		cleanup(track);
		System.gc();
//		printMemoryUsage();
		out.write(new JSONObject().put("released_resources", "track").toString());
	}

	private void getHistoryForFeatures(AnnotationEditor editor, AbstractHistoryStore historyStore, JSONArray features, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
        JSONObject historyContainer = createJSONFeatureContainer();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (int i = 0; i < features.length(); ++i) {
                JSONObject jsonFeature = features.getJSONObject(i);
                AbstractSingleLocationBioFeature gbolFeature = getFeature(editor, jsonFeature);
                JSONArray history = new JSONArray();
                jsonFeature.put("history", history);
                TransactionList transactionList = historyStore.getTransactionListForFeature(jsonFeature.getString("uniquename"));
                for (int j = 0; j < transactionList.size(); ++j) {
                        Transaction transaction = transactionList.get(j);
                        JSONObject historyItem = new JSONObject();
                        historyItem.put("operation", transaction.getOperation().toString());
                        historyItem.put("editor", transaction.getEditor());
                        historyItem.put("date", dateFormat.format(transaction.getDate()));
                        if (j == transactionList.getCurrentIndex()) {
                                historyItem.put("current", true);
                        }
                        JSONArray historyFeatures = new JSONArray();
                        historyItem.put("features", historyFeatures);
                        for (AbstractSingleLocationBioFeature f : transaction.getNewFeatures()) {
                        	if (transaction.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT)) {
                        		if (gbolFeature.overlaps(f)) {
//                            	if (f.getUniqueName().equals(jsonFeature.getString("uniquename"))) {
                            		historyFeatures.put(JSONUtil.convertBioFeatureToJSON(f));
                            	}
                        	}
                        	else {
                        		historyFeatures.put(JSONUtil.convertBioFeatureToJSON(f));
                        	}
                        }
                        history.put(historyItem);
                }
                historyContainer.getJSONArray("features").put(jsonFeature);
        }
        out.write(historyContainer.toString());
	}	
	
	private Transcript flipTranscriptStrand(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, Transcript oldTranscript, String track, String username, AbstractNameAdapter nameAdapter) throws JSONException, AnnotationEditorServiceException {
		boolean isPseudogene = oldTranscript.getGene().isPseudogene();
		Gene oldGene = oldTranscript.getGene();
		editor.deleteTranscript(oldGene, oldTranscript);
		if (oldGene.getTranscripts().size() == 0) {
			editor.deleteFeature(oldGene);
		}
		editor.flipStrand(oldTranscript);
		Transcript newTranscript = addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(oldTranscript), track, nameAdapter, isPseudogene);
		if (dataStore != null) {
			if (oldGene.getTranscripts().size() == 0) {
				deleteFeatureFromStore(dataStore, oldGene);
			}
			else {
				writeFeatureToStore(editor, dataStore, oldGene, track);
			}
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(newTranscript), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.FLIP_STRAND, newTranscript.getUniqueName(), username);
			transaction.addNewFeature(newTranscript);
			writeHistoryToStore(historyStore, transaction);
		}
		return newTranscript;

	}

	private AbstractSingleLocationBioFeature flipFeatureStrand(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, AbstractSingleLocationBioFeature feature, String track, String username) throws AnnotationEditorServiceException {
		editor.flipStrand(feature);
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, feature, track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.FLIP_STRAND, feature.getUniqueName(), username);
			transaction.addNewFeature(feature);
			writeHistoryToStore(historyStore, transaction);
		}
		return feature;
	}
	
	private void getDataAdapters(BufferedWriter out) throws JSONException, IOException {
		JSONArray dataAdaptersArray = new JSONArray();
		JSONObject dataAdaptersContainer = new JSONObject().put("data_adapters", dataAdaptersArray);
		for (Map.Entry<String, ServerConfiguration.DataAdapterGroupConfiguration> entry : dataAdapters.entrySet()) {
			if (!entry.getValue().isGroup()) {
				for (ServerConfiguration.DataAdapterConfiguration adapter : entry.getValue().getDataAdapters()) {
					JSONObject dataAdapter = new JSONObject();
					dataAdaptersArray.put(dataAdapter);
					dataAdapter.put("key", adapter.getKey());
					dataAdapter.put("permission", Permission.getValueForPermission(adapter.getPermission()));
					dataAdapter.put("options", adapter.getOptions());
				}
			}
			else {
				JSONObject dataAdapterGroup = new JSONObject();
				dataAdaptersArray.put(dataAdapterGroup);
				dataAdapterGroup.put("key", entry.getValue().getKey());
				dataAdapterGroup.put("permission", Permission.getValueForPermission(entry.getValue().getPermission()));
				JSONArray dataAdapterGroupArray = new JSONArray();
				dataAdapterGroup.put("data_adapters", dataAdapterGroupArray);
				for (ServerConfiguration.DataAdapterConfiguration adapter : entry.getValue().getDataAdapters()) {
					JSONObject dataAdapter = new JSONObject();
					dataAdapterGroupArray.put(dataAdapter);
					dataAdapter.put("key", adapter.getKey());
					dataAdapter.put("permission", Permission.getValueForPermission(adapter.getPermission()));
					dataAdapter.put("options", adapter.getOptions());
				}
			}
		}
		out.write(dataAdaptersContainer.toString());
	}

	private void getAnnotationInfoEditorConfiguration(BufferedWriter out) throws JSONException, IOException {
		JSONObject annotationInfoEditorConfigContainer = new JSONObject();
		JSONArray annotationInfoEditorConfigs = new JSONArray();
		annotationInfoEditorConfigContainer.put("annotation_info_editor_configs", annotationInfoEditorConfigs);
		for (ServerConfiguration.AnnotationInfoEditorConfiguration annotationInfoEditorConfiguration : annotationInfoEditorConfigurations.values()) {
			JSONObject annotationInfoEditorConfig = new JSONObject();
			annotationInfoEditorConfigs.put(annotationInfoEditorConfig);
			if (annotationInfoEditorConfiguration.hasStatus()) {
				for (String status : annotationInfoEditorConfiguration.getStatus()) {
					annotationInfoEditorConfig.append("status", status);
				}
			}
			if (annotationInfoEditorConfiguration.hasDbxrefs()) {
				annotationInfoEditorConfig.put("hasDbxrefs", true);
			}
			if (annotationInfoEditorConfiguration.hasAttributes()) {
				annotationInfoEditorConfig.put("hasAttributes", true);
			}
			if (annotationInfoEditorConfiguration.hasPubmedIds()) {
				annotationInfoEditorConfig.put("hasPubmedIds", true);
			}
			if (annotationInfoEditorConfiguration.hasGoIds()) {
				annotationInfoEditorConfig.put("hasGoIds", true);
			}
			if (annotationInfoEditorConfiguration.hasComments()) {
				annotationInfoEditorConfig.put("hasComments", true);
			}
			JSONArray supportedTypes = new JSONArray();
			annotationInfoEditorConfig.put("supported_types", supportedTypes);
			for (String supportedType : annotationInfoEditorConfiguration.getSupportedFeatureTypes()) {
				supportedTypes.put(supportedType);
			}
		}
		out.write(annotationInfoEditorConfigContainer.toString());
	}
	
	private void getTranslationTable(AnnotationEditor editor, String track, BufferedWriter out) throws JSONException, IOException {
		SequenceUtil.TranslationTable translationTable = editor.getConfiguration().getTranslationTable();
		JSONObject ttable = new JSONObject();
		for (Map.Entry<String, String> t : translationTable.getTranslationTable().entrySet()) {
			ttable.put(t.getKey(), t.getValue());
		}
		out.write(new JSONObject().put("translation_table", ttable).toString());
	}
	
	private void setTranslationStart(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonTranscript, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		Transcript oldTranscript = cloneTranscript(transcript);
		boolean setStart = jsonTranscript.has("location");
		if (!setStart) {
			editor.setManuallySetTranslationStart(transcript.getCDS(), false);
			calculateCDS(editor, transcript);
		}
		else {
			JSONObject jsonCDSLocation = jsonTranscript.getJSONObject("location");
			editor.setTranslationStart(transcript, jsonCDSLocation.getInt("fmin"), true);
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(setStart ? Transaction.Operation.SET_TRANSLATION_START : Transaction.Operation.UNSET_TRANSLATION_START, transcript.getUniqueName(), username);
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(transcript));
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void setTranslationEnd(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonTranscript, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		Transcript oldTranscript = cloneTranscript(transcript);
		boolean setEnd = jsonTranscript.has("location");
		if (!setEnd) {
			editor.setManuallySetTranslationEnd(transcript.getCDS(), false);
			calculateCDS(editor, transcript);
		}
		else {
			JSONObject jsonCDSLocation = jsonTranscript.getJSONObject("location");
			editor.setTranslationEnd(transcript, jsonCDSLocation.getInt("fmax"));
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(setEnd ? Transaction.Operation.SET_TRANSLATION_END : Transaction.Operation.UNSET_TRANSLATION_END, transcript.getUniqueName(), username);
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(transcript));
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	/*
	private void setTranslationEnd(AnnotationEditor editor, AbstractDataStore dataStore, JSONObject jsonTranscript, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		JSONObject jsonCDSLocation = jsonTranscript.getJSONArray("children").getJSONObject(0).getJSONObject("location");
		editor.setTranslationEnd(transcript, jsonCDSLocation.getInt("fmax"));
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
	}
	*/

	private void setTranslationEnds(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonTranscript, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		Transcript oldTranscript = cloneTranscript(transcript);
		JSONObject jsonCDSLocation = jsonTranscript.getJSONObject("location");
		editor.setTranslationEnds(transcript, jsonCDSLocation.getInt("fmin"), jsonCDSLocation.getInt("fmax"), jsonTranscript.has("manually_set_start") ? jsonTranscript.getBoolean("manually_set_start") : false, jsonTranscript.has("manually_set_end") ? jsonTranscript.getBoolean("manually_set_end") : false);
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.SET_TRANSLATION_ENDS, transcript.getUniqueName(), username);
//			transaction.addOldFeature(oldTranscript.getCDS());
//			transaction.addNewFeature(transcript.getCDS());
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(transcript));
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void setLongestORF(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonTranscript, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		Transcript oldTranscript = cloneTranscript(transcript);
		setLongestORF(editor, transcript);
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.SET_LONGEST_ORF, transcript.getUniqueName(), username);
//			transaction.addOldFeature(oldTranscript.getCDS());
//			transaction.addNewFeature(transcript.getCDS());
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(transcript));
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void calculateCDS(AnnotationEditor editor, Transcript transcript) {
		if (transcript.isProteinCoding() && (transcript.getGene() == null || !transcript.getGene().isPseudogene())) {
			calculateCDS(editor, transcript, transcript.getCDS() != null ? transcript.getCDS().getStopCodonReadThrough() != null : false);
		}
	}
	
	private void calculateCDS(AnnotationEditor editor, Transcript transcript, boolean readThroughStopCodon) {
		editor.calculateCDS(transcript, readThroughStopCodon);
		setTimesForCDS(transcript);
	}

	private void setLongestORF(AnnotationEditor editor, Transcript transcript) {
		editor.setLongestORF(transcript);
		setTimesForCDS(transcript);
	}
	
	private void setTimesForCDS(Transcript transcript) {
		if (transcript.getCDS() != null) {
			CDS cds = transcript.getCDS();
			if (cds.getTimeAccessioned() == null) {
				cds.setTimeAccessioned(new Date());
			}
			cds.setTimeLastModified(new Date());
		}
	}
	
	private void findNonCanonicalAcceptorDonorSpliceSites(AnnotationEditor editor, Transcript transcript) {
		editor.findNonCanonicalAcceptorDonorSpliceSites(transcript);
		for (NonCanonicalFivePrimeSpliceSite spliceSite : transcript.getNonCanonicalFivePrimeSpliceSites()) {
			if (spliceSite.getTimeAccessioned() == null) {
				spliceSite.setTimeAccessioned(new Date());
			}
			spliceSite.setTimeLastModified(new Date());
			spliceSite.setOwner(transcript.getOwner());
		}
		for (NonCanonicalThreePrimeSpliceSite spliceSite : transcript.getNonCanonicalThreePrimeSpliceSites()) {
			if (spliceSite.getTimeAccessioned() == null) {
				spliceSite.setTimeAccessioned(new Date());
			}
			spliceSite.setTimeLastModified(new Date());
			spliceSite.setOwner(transcript.getOwner());
		}
	}
	
	private void addExon(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws IOException, JSONException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, features.getJSONObject(0));
		Transaction transaction = null;
		if (historyStore != null) {
			transaction = new Transaction(Transaction.Operation.ADD_EXON, transcript.getUniqueName(), username);
			transaction.addOldFeature(cloneTranscript(transcript));
		}
		for (int i = 1; i < features.length(); ++i) {
			JSONObject jsonExon = features.getJSONObject(i);
			Feature gsolExon = JSONUtil.convertJSONToFeature(jsonExon, bioObjectConfiguration, trackToSourceFeature.get(track), new HttpSessionTimeStampNameAdapter(session, editor.getSession()));
			updateNewGsolFeatureAttributes(gsolExon, trackToSourceFeature.get(track));
			Exon gbolExon = (Exon)BioObjectUtil.createBioObject(gsolExon, bioObjectConfiguration);
			if (gbolExon.getFmin() < 0 || gbolExon.getFmax() < 0) {
				throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
			}
			editor.addExon(transcript, gbolExon);
			calculateCDS(editor, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		if (historyStore != null) {
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(transcript));
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private void deleteExon(AnnotationEditor editor, AbstractDataStore dataStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, features.getJSONObject(0));
		for (int i = 1; i < features.length(); ++i) {
			Exon exon = (Exon)getFeature(editor, features.getJSONObject(i));
			editor.deleteExon(transcript, exon);
		}		
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
	}
	
	private void mergeExons(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorException, AnnotationEditorServiceException {
		Exon exon1 = (Exon)getFeature(editor, features.getJSONObject(0));
		Exon exon2 = (Exon)getFeature(editor, features.getJSONObject(1));
		Transcript transcript = exon1.getTranscript();
		Transcript oldTranscript = cloneTranscript(transcript);
		editor.mergeExons(exon1, exon2);
		calculateCDS(editor, exon1.getTranscript());
		findNonCanonicalAcceptorDonorSpliceSites(editor, exon1.getTranscript());
		updateTranscriptAttributes(exon1.getTranscript());
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(exon1.getTranscript()), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.MERGE_EXONS, transcript.getUniqueName(), username);
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(exon1.getTranscript()));
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private void splitExon(AnnotationEditor editor, HttpSession session, AbstractNameAdapter nameAdapter, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonExon, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Exon exon = (Exon)getFeature(editor, jsonExon);
		Transcript oldTranscript = cloneTranscript(exon.getTranscript());
		JSONObject exonLocation = jsonExon.getJSONObject("location");
		Exon splitExon = editor.splitExon(exon, exonLocation.getInt("fmax"), exonLocation.getInt("fmin"), nameAdapter.generateUniqueName());
		updateNewGbolFeatureAttributes(splitExon, trackToSourceFeature.get(track));
		calculateCDS(editor, exon.getTranscript());
		findNonCanonicalAcceptorDonorSpliceSites(editor, exon.getTranscript());
		updateTranscriptAttributes(exon.getTranscript());
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(exon.getTranscript()), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.SPLIT_EXON, exon.getTranscript().getUniqueName(), username);
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(exon.getTranscript());
			writeHistoryToStore(historyStore, transaction);
		}
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(exon.getTranscript()));
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private void makeIntron(AnnotationEditor editor, HttpSession session, AbstractNameAdapter nameAdapter, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonExon, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Exon exon = (Exon)getFeature(editor, jsonExon);
		Transcript oldTranscript = cloneTranscript(exon.getTranscript());
		JSONObject exonLocation = jsonExon.getJSONObject("location");
		Exon splitExon = editor.makeIntron(exon, exonLocation.getInt("fmin"), defaultMinimumIntronSize, nameAdapter.generateUniqueName());
		if (splitExon == null) {
			out.write(new JSONObject().put("alert", "Unable to find canonical splice sites.").toString());
			return;
		}
		updateNewGbolFeatureAttributes(splitExon, trackToSourceFeature.get(track));
		calculateCDS(editor, exon.getTranscript());
		findNonCanonicalAcceptorDonorSpliceSites(editor, exon.getTranscript());
		updateTranscriptAttributes(exon.getTranscript());
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(exon.getTranscript()), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(Transaction.Operation.SPLIT_EXON, exon.getTranscript().getUniqueName(), username);
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(exon.getTranscript());
			writeHistoryToStore(historyStore, transaction);
		}
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(exon.getTranscript()));
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void splitTranscript(AnnotationEditor editor, HttpSession session, AbstractNameAdapter transcriptNameAdapter, AbstractNameAdapter geneNameAdapter, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		splitTranscript(editor, session, transcriptNameAdapter, geneNameAdapter, dataStore, historyStore, features, track, username, out, true, false);
	}
	
	private void splitTranscript(AnnotationEditor editor, HttpSession session, AbstractNameAdapter transcriptNameAdapter, AbstractNameAdapter geneNameAdapter, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out, boolean addHistoryForOriginalTranscript, boolean redo) throws JSONException, IOException, AnnotationEditorServiceException {
		Exon exon1 = (Exon)getFeature(editor, features.getJSONObject(0));
		Exon exon2 = (Exon)getFeature(editor, features.getJSONObject(1));
		Transcript oldTranscript = cloneTranscript(exon1.getTranscript(), true);
		Transcript splitTranscript = editor.splitTranscript(exon1.getTranscript(), exon1, exon2, transcriptNameAdapter.generateUniqueName());
		updateNewGbolFeatureAttributes(splitTranscript, trackToSourceFeature.get(track));
		calculateCDS(editor, exon1.getTranscript());
		calculateCDS(editor, exon2.getTranscript());
		findNonCanonicalAcceptorDonorSpliceSites(editor, exon1.getTranscript());
		findNonCanonicalAcceptorDonorSpliceSites(editor, exon2.getTranscript());
		updateTranscriptAttributes(exon1.getTranscript());
		updateTranscriptAttributes(exon2.getTranscript());
		exon2.getTranscript().setOwner(exon1.getTranscript().getOwner());
		Gene gene1 = exon1.getTranscript().getGene();
		
		if (gene1 != null) {

			/*
			boolean overlaps = false;
			for (Transcript t : gene1.getTranscripts()) {
				if (t.equals(splitTranscript)) {
					continue;
				}
				if (overlapper.overlaps(t, splitTranscript)) {
					overlaps = true;
					break;
				}
			}
			if (!overlaps) {
				editor.deleteTranscript(gene1, splitTranscript);
				splitTranscript = addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(splitTranscript), track, new HttpSessionTimeStampNameAdapter(session, editor.getSession()), gene1.isPseudogene());
//				addTranscript(editor, session, dataStore, null, new JSONArray().put(JSONUtil.convertBioFeatureToJSON(splitTranscript)), track, username, null, false);
			}
			*/
			
			Set<Transcript> gene1Transcripts = new HashSet<Transcript>();
			Set<Transcript> gene2Transcripts = new HashSet<Transcript>();
			List<Transcript> transcripts = BioObjectUtil.createSortedFeatureListByLocation(gene1.getTranscripts(), false);
			gene1Transcripts.add(transcripts.get(0));

			for (int i = 0; i < transcripts.size() - 1; ++i) {
				Transcript t1 = transcripts.get(i);
				for (int j = i + 1; j < transcripts.size(); ++j) {
					Transcript t2 = transcripts.get(j);
					if (gene1Transcripts.contains(t2) || gene2Transcripts.contains(t2)) {
						continue;
					}
					if (t1.getFmin() < splitTranscript.getFmin()) {
						if (overlapper.overlaps(t1, t2)) {
							gene1Transcripts.add(t2);
						}
						else {
							gene2Transcripts.add(t2);
						}
					}
					else {
						gene2Transcripts.add(t2);
					}
				}
				if (t1.getFmin() > splitTranscript.getFmin()) {
					break;
				}
			}
			/*
			for (Transcript t : transcripts) {
				if (t.getFmin() < splitTranscript.getFmin()) {
					if (overlapper.overlaps(t, splitTranscript)) {
						gene2Transcripts.add(t);
					}
					else {
						gene1Transcripts.add(t);
					}
				}
				else {
					gene2Transcripts.add(t);
				}
			}
			*/
			for (Transcript t : gene2Transcripts) {
				editor.deleteTranscript(gene1, t);
//				Transcript tmp = addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(t), track, new HttpSessionTimeStampNameAdapter(session, editor.getSession()));
			}
			
			editor.getSession().indexFeature(gene1);
			
			for (Transcript t : gene2Transcripts) {
				if (!t.equals(splitTranscript)) {
					addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(t), track, geneNameAdapter, gene1.isPseudogene());
				}
			}
			splitTranscript = addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(splitTranscript), track, geneNameAdapter, gene1.isPseudogene());
			/*
			if (gene2Transcripts.size() > 0) {
				/*
				Gene gene2 = cloneGene(gene1);
				gene2.setUniqueName(new HttpSessionTimeStampNameAdapter(session, editor.getSession()).generateUniqueName());
				gene2.setFmin(splitTranscript.getFmin());
				gene2.setFmax(splitTranscript.getFmax());
				for (Transcript t : gene2Transcripts) {
					gene2.addTranscript(t);
				}
				editor.addFeature(gene2);
			}
			*/
			
		}
		if (historyStore != null) {
			TransactionList oldTransactions = historyStore.getTransactionListForFeature(oldTranscript.getUniqueName());
			if (addHistoryForOriginalTranscript) {
				Transaction transaction1 = new Transaction(Transaction.Operation.SPLIT_TRANSCRIPT, exon1.getTranscript().getUniqueName(), username);
				transaction1.addOldFeature(oldTranscript);
				transaction1.addNewFeature(exon1.getTranscript());
				transaction1.addNewFeature(splitTranscript);
				historyStore.addTransaction(transaction1);
			}
//			for (Transaction transaction : historyStore.getTransactionListForFeature(oldTranscript.getUniqueName())) {
			for (int i = 0; i < oldTransactions.size(); ++i) {
				Transaction transaction = oldTransactions.get(i);
				if (!redo) {
					if (i == historyStore.getCurrentIndexForFeature(oldTranscript.getUniqueName())) {
						break;
					}
				}
				/*
				if (transaction.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT) && i == oldTransactions.size() - 1) {
					break;
				}
				*/
				transaction.setFeatureUniqueName(splitTranscript.getUniqueName());
				writeHistoryToStore(historyStore, transaction);
			}
			if (addHistoryForOriginalTranscript) {
				Transaction transaction2 = new Transaction(Transaction.Operation.SPLIT_TRANSCRIPT, exon2.getTranscript().getUniqueName(), username);
				transaction2.addOldFeature(oldTranscript);
				transaction2.addNewFeature(exon1.getTranscript());
				transaction2.addNewFeature(splitTranscript);
				historyStore.addTransaction(transaction2);
			}
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(exon1.getTranscript()), track);
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(splitTranscript), track);
			/*
			if (!getTopLevelFeatureForTranscript(exon1.getTranscript()).equals(getTopLevelFeatureForTranscript(exon2.getTranscript()))) {
				writeFeatureToStore(dataStore, getTopLevelFeatureForTranscript(exon2.getTranscript()), track);
			}
			*/
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(exon1.getTranscript()))).toString());

		JSONObject updateContainer = createJSONFeatureContainer();
		JSONObject addContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(splitTranscript));
		for (Transcript t : exon1.getTranscript().getGene().getTranscripts()) {
			updateContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(t));
		}
		for (Transcript t : splitTranscript.getGene().getTranscripts()) {
			if (!t.getUniqueName().equals(splitTranscript.getUniqueName())) {
				updateContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(t));
			}
		}
		
		/*
		JSONObject updateContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(exon1.getTranscript()));
		JSONObject addContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(splitTranscript));
		*/

		fireDataStoreChange(new DataStoreChangeEvent(this, updateContainer, track, DataStoreChangeEvent.Operation.UPDATE), new DataStoreChangeEvent(this, addContainer, track, DataStoreChangeEvent.Operation.ADD));
	}
	
	private void addSequenceAlteration(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		JSONObject addFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			Feature gsolFeature = JSONUtil.convertJSONToFeature(features.getJSONObject(i), bioObjectConfiguration, trackToSourceFeature.get(track), new HttpSessionTimeStampNameAdapter(session, editor.getSession()));
			updateNewGsolFeatureAttributes(gsolFeature, trackToSourceFeature.get(track));
			SequenceAlteration sequenceAlteration = (SequenceAlteration)BioObjectUtil.createBioObject(gsolFeature, bioObjectConfiguration);
			if (sequenceAlteration.getFmin() < 0 || sequenceAlteration.getFmax() < 0) {
				throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
			}

			setOwner(sequenceAlteration, (String)session.getAttribute("username"));
			editor.addSequenceAlteration(sequenceAlteration);
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, sequenceAlteration, track);
			}
			for (AbstractSingleLocationBioFeature feature : editor.getSession().getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
				if (feature instanceof Gene) {
					for (Transcript transcript : ((Gene)feature).getTranscripts()) {
						editor.setLongestORF(transcript);
						findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
						updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
					}
					if (dataStore != null) {
						writeFeatureToStore(editor, dataStore, feature, track);
					}
				}
			}
			addFeatureContainer.getJSONArray("features").put(JSONUtil.convertFeatureToJSON(gsolFeature));
		}
		fireDataStoreChange(new DataStoreChangeEvent(this, addFeatureContainer, track, DataStoreChangeEvent.Operation.ADD, true), new DataStoreChangeEvent(this, updateFeatureContainer, track, DataStoreChangeEvent.Operation.UPDATE));
		out.write(addFeatureContainer.toString());
	}

	private void deleteSequenceAlteration(AnnotationEditor editor, AbstractDataStore dataStore, JSONArray features, String track, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		JSONObject updateFeatureContainer = createJSONFeatureContainer();
		JSONObject deleteFeatureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			SequenceAlteration sequenceAlteration = (SequenceAlteration)getFeature(editor, features.getJSONObject(i));
			editor.deleteSequenceAlteration(sequenceAlteration);
			if (dataStore != null) {
				deleteFeatureFromStore(dataStore, sequenceAlteration);
				//						SimpleObjectIteratorInterface iterator = sequenceAlteration.getWriteableSimpleObjects(bioObjectConfiguration);
				//						Feature gsolFeature = (Feature)iterator.next();
				//						removeSourceFromFeature(gsolFeature);
				//						dataStore.deleteSequenceAlteration(gsolFeature);
			}
			for (AbstractSingleLocationBioFeature feature : editor.getSession().getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
				if (feature instanceof Gene) {
					for (Transcript transcript : ((Gene)feature).getTranscripts()) {
						editor.setLongestORF(transcript);
						findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
						updateFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
					}
					if (dataStore != null) {
						writeFeatureToStore(editor, dataStore, feature, track);
					}
				}
			}
			deleteFeatureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(sequenceAlteration));
		}
		fireDataStoreChange(new DataStoreChangeEvent(this, deleteFeatureContainer, track, DataStoreChangeEvent.Operation.DELETE, true), new DataStoreChangeEvent(this, updateFeatureContainer, track, DataStoreChangeEvent.Operation.UPDATE));
		out.write(createJSONFeatureContainer().toString());
	}

	private void getSequenceAlterations(AnnotationEditor editor, BufferedWriter out) throws JSONException, IOException {
		JSONObject jsonFeatureContainer = createJSONFeatureContainer();
		JSONArray jsonFeatures = jsonFeatureContainer.getJSONArray("features");
		for (SequenceAlteration alteration : editor.getSession().getSequenceAlterations()) {
			jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(alteration));
		}
		out.write(jsonFeatureContainer.toString());
	}
	
	private void getResiduesWithAlterations(AnnotationEditor editor, JSONArray features, BufferedWriter out) throws IOException, JSONException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			AbstractSingleLocationBioFeature gbolFeature = getFeature(editor, features.getJSONObject(i));
			JSONObject jsonFeature = new JSONObject();
			jsonFeature.put("uniquename", features.getJSONObject(i).getString("uniquename"));
			jsonFeature.put("residues", editor.getSession().getResiduesWithAlterations(gbolFeature));
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void addFrameshift(AnnotationEditor editor, AbstractDataStore dataStore, JSONArray features, String track, BufferedWriter out) throws IOException, JSONException, AnnotationEditorServiceException {
		JSONObject jsonTranscript = features.getJSONObject(0);
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		JSONArray jsonProperties = jsonTranscript.getJSONArray("properties");
		for (int i = 0; i < jsonProperties.length(); ++i) {
			JSONObject jsonProperty = jsonProperties.getJSONObject(i);
			FeatureProperty gsolProperty = JSONUtil.convertJSONToFeatureProperty(jsonProperty);
			editor.addFrameshift(transcript, (Frameshift)BioObjectUtil.createBioObject(gsolProperty, bioObjectConfiguration));
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
	}

	private void deleteFrameshift(AnnotationEditor editor, AbstractDataStore dataStore, JSONArray features, String track, BufferedWriter out) throws IOException, JSONException, AnnotationEditorServiceException {
		JSONObject jsonTranscript = features.getJSONObject(0);
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		JSONArray jsonProperties = jsonTranscript.getJSONArray("properties");
		for (int i = 0; i < jsonProperties.length(); ++i) {
			JSONObject jsonProperty = jsonProperties.getJSONObject(i);
			FeatureProperty gsolProperty = JSONUtil.convertJSONToFeatureProperty(jsonProperty);
			editor.deleteFrameshift(transcript, (Frameshift)BioObjectUtil.createBioObject(gsolProperty, bioObjectConfiguration));
		}
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		out.write(createJSONFeatureContainer().toString());
	}
	
	private void getResiduesWithFrameshifts(AnnotationEditor editor, JSONArray features, BufferedWriter out) throws IOException, JSONException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			AbstractSingleLocationBioFeature gbolFeature = getFeature(editor, features.getJSONObject(i));
			JSONObject jsonFeature = new JSONObject();
			jsonFeature.put("uniquename", features.getJSONObject(i).getString("uniquename"));
			jsonFeature.put("residues", editor.getSession().getResiduesWithFrameshifts(gbolFeature));
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}

	private void getResiduesWithAlterationsAndFrameshifts(AnnotationEditor editor, JSONArray features, BufferedWriter out) throws IOException, JSONException, AnnotationEditorServiceException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			AbstractSingleLocationBioFeature gbolFeature = getFeature(editor, features.getJSONObject(i));
			JSONObject jsonFeature = new JSONObject();
			jsonFeature.put("uniquename", features.getJSONObject(i).getString("uniquename"));
			jsonFeature.put("residues", editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature));
			featureContainer.getJSONArray("features").put(jsonFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private void setExonBoundaries(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			if (!jsonFeature.has("location")) {
				continue;
			}
			JSONObject jsonLocation = jsonFeature.getJSONObject("location");
			int fmin = jsonLocation.getInt("fmin");
			int fmax = jsonLocation.getInt("fmax");
			if (fmin < 0 || fmax < 0) {
				throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
			}
			Exon exon = (Exon)editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			Transcript transcript = exon.getTranscript();
			Transcript oldTranscript = cloneTranscript(transcript);
			if (transcript.getFmin().equals(exon.getFmin())) {
				transcript.setFmin(fmin);
			}
			if (transcript.getFmax().equals(exon.getFmax())) {
				transcript.setFmax(fmax);
			}
			editor.setExonBoundaries(exon, fmin, fmax);
			calculateCDS(editor, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
			}
			if (historyStore != null) {
				Transaction transaction = new Transaction(Transaction.Operation.SET_EXON_BOUNDARIES, transcript.getUniqueName(), username);
				transaction.addOldFeature(oldTranscript);
				transaction.addNewFeature(transcript);
				writeHistoryToStore(historyStore, transaction);
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private void setBoundaries(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			if (!jsonFeature.has("location")) {
				continue;
			}
			JSONObject jsonLocation = jsonFeature.getJSONObject("location");
			int fmin = jsonLocation.getInt("fmin");
			int fmax = jsonLocation.getInt("fmax");
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			AbstractSingleLocationBioFeature oldFeature = cloneFeature(feature);
			editor.setBoundaries(feature, fmin, fmax);
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, feature, track);
			}
			if (historyStore != null) {
				Transaction transaction = new Transaction(Transaction.Operation.SET_BOUNDARIES, feature.getUniqueName(), username);
				transaction.addOldFeature(oldFeature);
				transaction.addNewFeature(feature);
				writeHistoryToStore(historyStore, transaction);
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private void setToDownstreamDonor(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException, AnnotationEditorException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			Exon exon = (Exon)editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			Transcript transcript = exon.getTranscript();
			Transcript oldTranscript = cloneTranscript(transcript);
			editor.setToDownstreamDonor(exon);
			calculateCDS(editor, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
			}
			if (historyStore != null) {
				Transaction transaction = new Transaction(Transaction.Operation.SET_EXON_BOUNDARIES, transcript.getUniqueName(), username);
				transaction.addOldFeature(oldTranscript);
				transaction.addNewFeature(transcript);
				writeHistoryToStore(historyStore, transaction);
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void setToUpstreamDonor(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException, AnnotationEditorException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			Exon exon = (Exon)editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			Transcript transcript = exon.getTranscript();
			Transcript oldTranscript = cloneTranscript(transcript);
			editor.setToUpstreamDonor(exon);
			calculateCDS(editor, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
			}
			if (historyStore != null) {
				Transaction transaction = new Transaction(Transaction.Operation.SET_EXON_BOUNDARIES, transcript.getUniqueName(), username);
				transaction.addOldFeature(oldTranscript);
				transaction.addNewFeature(transcript);
				writeHistoryToStore(historyStore, transaction);
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void setToDownstreamAcceptor(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException, AnnotationEditorException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			Exon exon = (Exon)editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			Transcript transcript = exon.getTranscript();
			Transcript oldTranscript = cloneTranscript(transcript);
			editor.setToDownstreamAcceptor(exon);
			calculateCDS(editor, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
			}
			if (historyStore != null) {
				Transaction transaction = new Transaction(Transaction.Operation.SET_EXON_BOUNDARIES, transcript.getUniqueName(), username);
				transaction.addOldFeature(oldTranscript);
				transaction.addNewFeature(transcript);
				writeHistoryToStore(historyStore, transaction);
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void setToUpstreamAcceptor(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException, AnnotationEditorException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			Exon exon = (Exon)editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			Transcript transcript = exon.getTranscript();
			Transcript oldTranscript = cloneTranscript(transcript);
			editor.setToUpstreamAcceptor(exon);
			calculateCDS(editor, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(transcript));
			if (dataStore != null) {
				writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
			}
			if (historyStore != null) {
				Transaction transaction = new Transaction(Transaction.Operation.SET_EXON_BOUNDARIES, transcript.getUniqueName(), username);
				transaction.addOldFeature(oldTranscript);
				transaction.addNewFeature(transcript);
				writeHistoryToStore(historyStore, transaction);
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void lockFeature(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, int permission, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException, AnnotationEditorException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			if (!feature.getOwner().getOwner().equals(username) && (permission & Permission.ADMIN) == 0) {
				throw new AnnotationEditorServiceException("Cannot lock someone else's annotation");
			}
			feature.addNonReservedProperty("locked", "true");
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}

	private void unlockFeature(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, int permission, BufferedWriter out) throws JSONException, AnnotationEditorServiceException, IOException, AnnotationEditorException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			AbstractSingleLocationBioFeature feature = editor.getSession().getFeatureByUniqueName(jsonFeature.getString("uniquename"));
			if (!feature.getOwner().getOwner().equals(username) && (permission & Permission.ADMIN) == 0) {
				throw new AnnotationEditorServiceException("Cannot unlock someone else's annotation");
			}
			feature.deleteNonReservedProperty("locked", "true");
			featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(feature));
			if (dataStore != null) {
				if (feature instanceof Transcript) {
					writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript((Transcript)feature), track);
				}
				else {
					writeFeatureToStore(editor, dataStore, feature, track);
				}
			}
		}
		out.write(featureContainer.toString());
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private boolean isLockedFeature(AbstractSingleLocationBioFeature feature) {
		for (GenericFeatureProperty fp : feature.getNonReservedProperties()) {
			if (fp.getTag().equals("locked") && fp.getValue().equals("true")) {
				return true;
			}
		}
		return false;
	}

	private void undo(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject json, String track, BufferedWriter out, int count) throws JSONException, IOException, AnnotationEditorServiceException, AnnotationEditorException {
		JSONArray features = json.getJSONArray("features");
		for (int i = 0; i< features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			String uniqueName = jsonFeature.getString("uniquename");
			for (int j = 1; j < count; ++j) {
				historyStore.setToPreviousTransactionForFeature(uniqueName);
			}
			Transaction transaction = historyStore.getCurrentTransactionForFeature(uniqueName);
			if (transaction == null) {
				return;
			}
			historyStore.setToPreviousTransactionForFeature(uniqueName);
			if (transaction.getOperation().equals(Transaction.Operation.ADD_FEATURE)) {
				if (!json.has("confirm") || !json.getBoolean("confirm")) {
					historyStore.setToNextTransactionForFeature(uniqueName);
					out.write(new JSONObject().put("confirm", "Undo of adding a feature will permanently delete the feature.  Continue?").toString());
					return;
				}
				JSONArray jsonFeatures = new JSONArray();
				jsonFeatures.put(new JSONObject().put("uniquename", uniqueName));
				deleteFeature(editor, dataStore, historyStore, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.DELETE_FEATURE)) {
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(feature));
				}
				addFeature(editor, session, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.ADD_TRANSCRIPT)) {
				if (!json.has("confirm") || !json.getBoolean("confirm")) {
					historyStore.setToNextTransactionForFeature(uniqueName);
					out.write(new JSONObject().put("confirm", "Undo of adding a transcript will permanently delete the transcript.  Continue?").toString());
					return;
				}
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature transcript : transaction.getNewFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON((transcript)));
				}
				deleteFeature(editor, dataStore, historyStore, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.DELETE_TRANSCRIPT)) {
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature transcript : transaction.getOldFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
				}
				addTranscript(editor, session, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.ADD_EXON)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.DELETE_EXON)) {
				Transcript transcript = (Transcript)editor.getSession().getFeatureByUniqueName(transaction.getFeatureUniqueName());
				JSONArray jsonFeatures = new JSONArray();
				jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
				for (AbstractSingleLocationBioFeature exon : transaction.getOldFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(exon));
				}
				addExon(editor, session, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.MERGE_EXONS)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SPLIT_EXON)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_EXON_BOUNDARIES)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_EXON_BOUNDARIES)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_BOUNDARIES)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getOldFeatures()) {
					setBoundaries(editor, dataStore, null, new JSONArray().put(JSONUtil.convertBioFeatureToJSON(feature)), track, null, out);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
				Transcript oldTranscript1 = (Transcript)transaction.getOldFeatures().get(0);
				Transcript oldTranscript2 = (Transcript)transaction.getOldFeatures().get(1);
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				if (uniqueName.equals(oldTranscript1.getUniqueName())) {
					if (!historyStore.getTransactionListForFeature(oldTranscript2.getUniqueName()).equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
						historyStore.setToPreviousTransactionForFeature(oldTranscript2.getUniqueName());
					}
				}
				else {
					if (!historyStore.getTransactionListForFeature(oldTranscript1.getUniqueName()).equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
						historyStore.setToPreviousTransactionForFeature(oldTranscript1.getUniqueName());
					}
				}
				/*
				Exon leftExon = null;
				Exon rightExon = null;
				for (Exon exon : transcript1.getExons()) {
					if (leftExon == null || exon.getFmin() > leftExon.getFmin()) {
						leftExon = exon;
					}
				}
				for (Exon exon : transcript2.getExons()) {
					if (rightExon == null || exon.getFmin() < rightExon.getFmin()) {
						rightExon = exon;
					}
				}
				JSONArray jsonFeatures = new JSONArray();
				jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(leftExon));
				jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(rightExon));
				splitTranscript(editor, session, new PreDefinedNameAdapter(transcript2.getUniqueName()), dataStore, null, jsonFeatures, track, null, out);
				*/
				
				/*
				Gene newGene = (Gene)getFeature(editor, newTranscript.getGene().getUniqueName());
				editor.deleteTranscript(newGene, newTranscript);
				if (newGene.getTranscripts().size() == 0) {
					editor.deleteFeature(newGene);
				}
				Gene oldGene1 = (Gene)editor.getSession().getFeatureByUniqueName(oldTranscript1.getGene().getUniqueName());
				if (oldGene1 == null) {
					editor.addFeature(oldTranscript1.getGene());
					oldGene1 = oldTranscript1.getGene();
				}
				else {
					editor.addTranscript(oldGene1, oldTranscript1);
				}
				Gene oldGene2 = (Gene)editor.getSession().getFeatureByUniqueName(oldTranscript2.getGene().getUniqueName());
				if (oldGene2 == null) {
					editor.addFeature(oldTranscript2.getGene());
					oldGene2 = oldTranscript2.getGene();
				}
				else {
					editor.addTranscript(oldGene2, oldTranscript2);
				}
				if (dataStore != null) {
					dataStore.deleteFeature(newGene);
					dataStore.writeFeature(oldGene1);
					dataStore.writeFeature(oldGene2);
				}
				JSONObject deletedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(newTranscript));
				JSONObject addedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(oldTranscript1), JSONUtil.convertBioFeatureToJSON(oldTranscript2));
				*/

				Gene newGene = null;
				Gene gene1 = (Gene)editor.getSession().getFeatureByUniqueName(oldTranscript1.getGene().getUniqueName());
				Gene gene2 = (Gene)editor.getSession().getFeatureByUniqueName(oldTranscript2.getGene().getUniqueName());
				if (gene1 == null) {
					gene1 = oldTranscript1.getGene();
					newGene = gene1;
					if (historyStore.getTransactionListForFeature(oldTranscript1.getUniqueName()).isEmpty()) {
						for (Transaction t : historyStore.getTransactionListForFeature(oldTranscript2.getUniqueName())) {
							Transaction newT = new Transaction(t);
							newT.setFeatureUniqueName(oldTranscript1.getUniqueName());
							historyStore.addTransaction(newT);
						}
						for (int j = historyStore.getHistorySizeForFeature(oldTranscript2.getUniqueName()) - 1; j > historyStore.getCurrentIndexForFeature(oldTranscript2.getUniqueName()); --j) {
							historyStore.setToPreviousTransactionForFeature(oldTranscript1.getUniqueName());
						}
					}
				}
				else if (gene2 == null) {
					gene2 = oldTranscript2.getGene();
					newGene = gene2;
					if (historyStore.getTransactionListForFeature(oldTranscript2.getUniqueName()).isEmpty()) {
						for (Transaction t : historyStore.getTransactionListForFeature(oldTranscript1.getUniqueName())) {
							Transaction newT = new Transaction(t);
							newT.setFeatureUniqueName(oldTranscript2.getUniqueName());
							historyStore.addTransaction(newT);
						}
						for (int j = historyStore.getHistorySizeForFeature(oldTranscript1.getUniqueName()) - 1; j > historyStore.getCurrentIndexForFeature(oldTranscript1.getUniqueName()); --j) {
							historyStore.setToPreviousTransactionForFeature(oldTranscript2.getUniqueName());
						}
					}
				}
				Set<String> oldTranscriptNames1 = new HashSet<String>();
				Set<String> oldTranscriptNames2 = new HashSet<String>();
				for (Transcript t : oldTranscript1.getGene().getTranscripts()) {
					oldTranscriptNames1.add(t.getUniqueName());
				}
				for (Transcript t : oldTranscript2.getGene().getTranscripts()) {
					oldTranscriptNames2.add(t.getUniqueName());
				}
				editor.deleteTranscript(gene1, newTranscript);
				editor.addTranscript(gene1, oldTranscript1);
				editor.addTranscript(gene2, oldTranscript2);
				if (!gene1.getUniqueName().equals(gene2.getUniqueName())) {
					List<Transcript> toBeMoved = new ArrayList<Transcript>();
					for (Transcript t : gene1.getTranscripts()) {
						if (!t.getUniqueName().equals(newTranscript.getUniqueName())) {
							if (!oldTranscriptNames1.contains(t.getUniqueName()) && oldTranscriptNames2.contains(t.getUniqueName())) {
								toBeMoved.add(t);
							}
						}
					}
					for (Transcript t : toBeMoved) {
						editor.deleteTranscript(gene1, t);
						editor.deleteTranscript(gene2, t);
						editor.addTranscript(gene2, t);
					}
					List<Transcript> toBeDeleted = new ArrayList<Transcript>();
					for (Transcript t : gene2.getTranscripts()) {
						if (gene1.getTranscripts().contains(t)) {
							toBeDeleted.add(t);
						}
					}
					for (Transcript t : toBeDeleted) {
						editor.deleteTranscript(gene2, t);
					}
					editor.addFeature(newGene != null ? newGene : gene2);
				}
				
				/*
				editor.deleteFeature(newTranscript.getGene());
				editor.addFeature(oldTranscript1.getGene());
				if (!oldTranscript1.getGene().getUniqueName().equals(oldTranscript2.getGene().getUniqueName())) {
					editor.addFeature(oldTranscript2.getGene());
				}
				*/
				if (dataStore != null) {
					dataStore.writeFeature(gene1);

//					editor.getSession().endTransactionForFeature(gene1);
					
					if (!gene1.getUniqueName().equals(gene2.getUniqueName())) {
						dataStore.writeFeature(gene2);
					}

//					editor.getSession().endTransactionForFeature(gene2);
					
					/*
					dataStore.deleteFeature(newTranscript.getGene());
					dataStore.writeFeature(oldTranscript1.getGene());
					if (!oldTranscript1.getGene().getUniqueName().equals(oldTranscript2.getGene().getUniqueName())) {
						dataStore.writeFeature(oldTranscript2.getGene());
					}
					*/
				}
				JSONObject deletedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(newTranscript));
				JSONObject addedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(oldTranscript1), JSONUtil.convertBioFeatureToJSON(oldTranscript2));
				JSONObject updatedFeatures = createJSONFeatureContainer();
				for (Transcript t : oldTranscript1.getGene().getTranscripts()) {
					if (!t.getUniqueName().equals(oldTranscript1.getUniqueName())) {
						updatedFeatures.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(t));
					}
				}
				if (!gene1.getUniqueName().equals(gene2.getUniqueName())) {
					for (Transcript t : gene2.getTranscripts()) {
						if (!t.getUniqueName().equals(oldTranscript1.getUniqueName())) {
							updatedFeatures.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(t));
						}
					}
				}
				
				fireDataStoreChange(new DataStoreChangeEvent(this, deletedFeatures, track, Operation.DELETE), new DataStoreChangeEvent(this, addedFeatures, track, Operation.ADD), new DataStoreChangeEvent(this, updatedFeatures, track, Operation.UPDATE));
				
				editor.getSession().endTransactionForAllFeatures();
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT)) {
				if (!json.has("confirm") || !json.getBoolean("confirm")) {
					historyStore.setToNextTransactionForFeature(uniqueName);
					out.write(new JSONObject().put("confirm", "Undo of a split will lose modifications and redo information for the remerged features done after the split.  Continue?").toString());
					return;
				}
				Transcript transcript1 = (Transcript)transaction.getNewFeatures().get(0);
				Transcript transcript2 = (Transcript)transaction.getNewFeatures().get(1);
				Transcript oldTranscript = (Transcript)transaction.getOldFeatures().get(0);
				Gene gene1 = (Gene)editor.getSession().getFeatureByUniqueName(transcript1.getGene().getUniqueName());
				Gene gene2 = transcript1.getGene().getUniqueName().equals(transcript2.getGene().getUniqueName()) ? gene1 : (Gene)editor.getSession().getFeatureByUniqueName(transcript2.getGene().getUniqueName());
				if (gene2 == null) {
					gene2 = gene1;
				}
				if (historyStore.getHistorySizeForFeature(oldTranscript.getUniqueName()) > 0 && !uniqueName.equals(oldTranscript.getUniqueName())) {
					/*
					Transaction oldTransaction = null;
					do {
						oldTransaction = historyStore.peekTransactionForFeature(oldTranscript.getUniqueName());
						if (!oldTransaction.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT)) {
							historyStore.popTransactionForFeature(oldTranscript.getUniqueName());
						}
						else {
							historyStore.setToPreviousTransactionForFeature(oldTranscript.getUniqueName());
							break;
						}
					} while (oldTransaction != null);
					*/
					while (historyStore.getCurrentIndexForFeature(oldTranscript.getUniqueName()) < historyStore.getHistorySizeForFeature(oldTranscript.getUniqueName()) - 1) {
						historyStore.popTransactionForFeature(oldTranscript.getUniqueName());
					}
					historyStore.setToPreviousTransactionForFeature(oldTranscript.getUniqueName());
				}
				else if (gene1 == null) {
					boolean found = false;
					for (Transaction t : historyStore.getTransactionListForFeature(transcript2.getUniqueName())) {
						if (found && !t.getOldFeatures().get(0).getUniqueName().equals(oldTranscript.getUniqueName())) {
							break;
						}
					/*
					TransactionList transactions = historyStore.getTransactionListForFeature(transcript2.getUniqueName());
					for (int j = 0; i < transactions.size() - 1; ++i) {
						Transaction t = transactions.get(i);
						*/
						t.setFeatureUniqueName(oldTranscript.getUniqueName());
						historyStore.addTransaction(t);
						/*
						if (i == historyStore.getCurrentIndexForFeature(transcript2.getUniqueName()) + 1) {
							break;
						}
						*/
						if (!t.getOldFeatures().isEmpty() && t.getOldFeatures().get(0).getUniqueName().equals(oldTranscript.getUniqueName())) {
							found = true;
						}
					}
					if (historyStore.getCurrentIndexForFeature(oldTranscript.getUniqueName()) == historyStore.getHistorySizeForFeature(oldTranscript.getUniqueName()) - 1) {
						historyStore.setToPreviousTransactionForFeature(oldTranscript.getUniqueName());
					}
				}
				else {
					while (historyStore.getHistorySizeForFeature(oldTranscript.getUniqueName()) > historyStore.getCurrentIndexForFeature(oldTranscript.getUniqueName()) + 2) {
						historyStore.popTransactionForFeature(oldTranscript.getUniqueName());
					}
					/*
					while (historyStore.getHistorySizeForFeature(transcript2.getUniqueName()) > historyStore.getCurrentIndexForFeature(transcript2.getUniqueName()) + 1) {
						historyStore.popTransactionForFeature(transcript2.getUniqueName());
					}
					*/
				}
				if (gene1 == null) {
					gene1 = oldTranscript.getGene();
					editor.addFeature(oldTranscript.getGene());
				}
				editor.deleteTranscript(gene1, transcript1);
				Set<String> toBeDeleted = new java.util.HashSet<String>();
				
				for (AbstractSingleLocationBioFeature feature : editor.getSession().getOverlappingFeatures(oldTranscript.getFeatureLocation(), false)) {
					if (feature instanceof Gene) {
						Gene gene = (Gene)feature;
						for (Transcript transcript : gene.getTranscripts()) {
							for (Transaction t : historyStore.getTransactionListForFeature(transcript.getUniqueName())) {
								if (t.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT)) {
									if (t.getOldFeatures().get(0).getUniqueName().equals(oldTranscript.getUniqueName())) {
										toBeDeleted.add(transcript.getUniqueName());
										break;
									}
								}
							}
						}
					}

//					else {
//						editor.getSession().endTransactionForFeature(feature);
//					}

				}
				for (String tu : toBeDeleted) {
					Transcript t = (Transcript)getFeature(editor, tu);
					Gene gene = t.getGene().getUniqueName().equals(gene1.getUniqueName()) ? gene1 : t.getGene().getUniqueName().equals(gene2.getUniqueName()) ? gene2 : t.getGene();
					editor.deleteTranscript(gene, t);
					historyStore.deleteHistoryForFeature(t.getUniqueName());
					if (gene.getTranscripts().size() == 0) {
						editor.getSession().deleteFeature(gene);
						dataStore.deleteFeature(gene);
					}
					else {
						dataStore.writeFeature(gene);
					}

//					editor.getSession().endTransactionForFeature(gene);
					
				}
				
				Set<Transcript> toBeMoved = new HashSet<Transcript>();
				if (!gene1.equals(gene2)) {
					for (Transcript t : gene2.getTranscripts()) {
						if (oldTranscript.overlaps(t)) {
							toBeMoved.add(t);
						}
					}
				}
				for (Transcript t : toBeMoved) {
					editor.deleteTranscript(gene2, t);
				}
				/*
				if (gene2.getTranscripts().size() == 0) {
					editor.deleteFeature(gene2);
				}
				*/
				editor.addTranscript(gene1, oldTranscript);
				for (Transcript t : toBeMoved) {
					Transcript tmp = addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(t), track, new HttpSessionTimeStampNameAdapter(session, editor.getSession()), gene1.isPseudogene());
				}
				
				/*
				List<Transcript> toBeProcessed = new ArrayList<Transcript>();
				toBeProcessed.add(transcript2);
				while (!toBeProcessed.isEmpty()) {
					Transcript t1 = toBeProcessed.remove(toBeProcessed.size() - 1);
					boolean valid = false;
					for (Transaction t : historyStore.getTransactionListForFeature(t1.getUniqueName())) {
						if (t.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT)) {
							if (t.getOldFeatures().get(0).getUniqueName().equals(oldTranscript.getUniqueName())) {
								toBeDeleted.add(t1.getUniqueName());
								valid = true;
							}
							else if (valid) {
								for (AbstractSingleLocationBioFeature feat : t.getNewFeatures()) {
									if (!toBeDeleted.contains(feat.getUniqueName())) {
										toBeProcessed.add((Transcript)feat);
									}
								}
							}
						}
					}
				}
				for (String tu : toBeDeleted) {
					Transcript t = (Transcript)getFeature(editor, tu);
					Gene gene = t.getGene();
					editor.deleteTranscript(gene, t);
					historyStore.deleteHistoryForFeature(t.getUniqueName());
					if (gene.getTranscripts().size() == 0) {
						dataStore.deleteFeature(gene);
					}
					else {
						dataStore.writeFeature(gene);
					}
				}
				*/
				
				if (dataStore != null) {
					dataStore.writeFeature(gene1);

//					editor.getSession().endTransactionForFeature(gene1);
					
					if (gene2.getTranscripts().size() > 0) {
						dataStore.writeFeature(gene2);
					}
					else {
						dataStore.deleteFeature(gene2);
					}

//					editor.getSession().endTransactionForFeature(gene2);
					

				}
				List<DataStoreChangeEvent> events = new ArrayList<DataStoreChangeEvent>();
				for (String deletedTranscript : toBeDeleted) {
					events.add(new DataStoreChangeEvent(this, createJSONFeatureContainer(new JSONObject().put("uniquename", deletedTranscript)), track, DataStoreChangeEvent.Operation.DELETE));
				}
				events.add(new DataStoreChangeEvent(this, createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(oldTranscript)), track, DataStoreChangeEvent.Operation.UPDATE));
				fireDataStoreChange(events.toArray(new DataStoreChangeEvent[0]));
				out.write(new JSONObject().toString());
				
				editor.getSession().endTransactionForAllFeatures();

			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_LONGEST_ORF)) {
				Transcript oldTranscript = (Transcript)transaction.getOldFeatures().get(0);
				CDS oldCDS = oldTranscript.getCDS();
//				CDS oldCDS = (CDS)transaction.getOldFeatures().get(0);
				int fmin = oldCDS.getStrand().equals(-1) ? oldCDS.getFmax() - 1 : oldCDS.getFmin();
				JSONObject jsonLocation = new JSONObject();
				/*
				jsonLocation.put("fmin", fmin);
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationStart(editor, dataStore, null, jsonTranscript, track, null, out);
				*/
				jsonLocation.put("fmin", oldCDS.getFmin());
				jsonLocation.put("fmax", oldCDS.getFmax());
				jsonLocation.put("strand", oldCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_TRANSLATION_START)) {
				Transcript oldTranscript = (Transcript)transaction.getOldFeatures().get(0);
				CDS oldCDS = oldTranscript.getCDS();
//				CDS oldCDS = (CDS)transaction.getOldFeatures().get(0);
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", oldCDS.getFmin());
				jsonLocation.put("fmax", oldCDS.getFmax());
				jsonLocation.put("strand", oldCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.UNSET_TRANSLATION_START)) {
				Transcript oldTranscript = (Transcript)transaction.getOldFeatures().get(0);
				CDS oldCDS = oldTranscript.getCDS();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", oldCDS.getFmin());
				jsonLocation.put("fmax", oldCDS.getFmax());
				jsonLocation.put("strand", oldCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation).put("manually_set_start", true);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_TRANSLATION_END)) {
				Transcript oldTranscript = (Transcript)transaction.getOldFeatures().get(0);
				CDS oldCDS = oldTranscript.getCDS();
//				CDS oldCDS = (CDS)transaction.getOldFeatures().get(0);
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", oldCDS.getFmin());
				jsonLocation.put("fmax", oldCDS.getFmax());
				jsonLocation.put("strand", oldCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.UNSET_TRANSLATION_END)) {
				Transcript oldTranscript = (Transcript)transaction.getOldFeatures().get(0);
				CDS oldCDS = oldTranscript.getCDS();
//				CDS oldCDS = (CDS)transaction.getOldFeatures().get(0);
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", oldCDS.getFmin());
				jsonLocation.put("fmax", oldCDS.getFmax());
				jsonLocation.put("strand", oldCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation).put("manually_set_end", true);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.FLIP_STRAND)) {
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(feature));
				}
				flipStrand(editor, session, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_READTHROUGH_STOP_CODON)) {
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("readthrough_stop_codon", false);
				setReadthroughStopCodon(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.UNSET_READTHROUGH_STOP_CODON)) {
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("readthrough_stop_codon", true);
				setReadthroughStopCodon(editor, dataStore, null, jsonTranscript, track, null, out);
			}
		}
	}
	
	private void redo(AnnotationEditor editor, HttpSession session, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONArray features, String track, String username, BufferedWriter out, int count) throws JSONException, IOException, AnnotationEditorServiceException, AnnotationEditorException {
		for (int i = 0; i< features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			String uniqueName = jsonFeature.getString("uniquename");
			if (historyStore.getCurrentIndexForFeature(uniqueName) + (count - 1) >= historyStore.getHistorySizeForFeature(uniqueName) - 1) {
				continue;
			}
			for (int j = 0; j < count; ++j) {
				historyStore.setToNextTransactionForFeature(uniqueName);
			}
			Transaction transaction = historyStore.getCurrentTransactionForFeature(uniqueName);
			if (transaction == null) {
				return;
			}
			if (transaction.getOperation().equals(Transaction.Operation.ADD_FEATURE)) {
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(feature));
				}
				addFeature(editor, session, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.DELETE_FEATURE)) {
				JSONArray jsonFeatures = new JSONArray();
				jsonFeatures.put(new JSONObject().put("uniquename", uniqueName));
				deleteFeature(editor, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.ADD_EXON)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.DELETE_EXON)) {
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature exon : transaction.getOldFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON((exon)));
				}
				deleteFeature(editor, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.MERGE_EXONS)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SPLIT_EXON)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_EXON_BOUNDARIES)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					updateTranscript(editor, (Transcript)feature, dataStore, track);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_BOUNDARIES)) {
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					setBoundaries(editor, dataStore, null, new JSONArray().put(JSONUtil.convertBioFeatureToJSON(feature)), track, null, out);
				}
			}
			else if (transaction.getOperation().equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
				Transcript oldTranscript1 = (Transcript)transaction.getOldFeatures().get(0);
				Transcript oldTranscript2 = (Transcript)transaction.getOldFeatures().get(1);
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);

				if (uniqueName.equals(oldTranscript1.getUniqueName())) {
					historyStore.setToNextTransactionForFeature(oldTranscript2.getUniqueName());
				}
				else {
					historyStore.setToNextTransactionForFeature(oldTranscript1.getUniqueName());
				}
				Transcript t1 = (Transcript)editor.getSession().getFeatureByUniqueName(oldTranscript1.getUniqueName());
				Transcript t2 = (Transcript)editor.getSession().getFeatureByUniqueName(oldTranscript2.getUniqueName());
				if (t1 != null && t2 != null) {
					mergeTranscripts(editor, dataStore, null, (JSONUtil.convertBioFeatureToJSON(oldTranscript1)), (JSONUtil.convertBioFeatureToJSON(oldTranscript2)), track, null, out);
				}
				else {
					Transcript transcript = t1 != null ? t1 : t2;
					Gene gene = transcript.getGene();
					editor.deleteTranscript(gene, transcript);
//					Gene gene = t1.getGene();
//					editor.deleteTranscript(gene, t1);
					editor.addTranscript(gene, newTranscript);
					JSONObject updatedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(newTranscript));
					DataStoreChangeEvent updateEvent = new DataStoreChangeEvent(this, updatedFeatures, track, DataStoreChangeEvent.Operation.UPDATE);
					if (t1 != null) {
						fireDataStoreChange(updateEvent);
					}
					else {
						for (Transaction t : historyStore.getTransactionListForFeature(t2.getUniqueName())) {
							t.setFeatureUniqueName(newTranscript.getUniqueName());
							historyStore.addTransaction(t);
						}
//						historyStore.deleteHistoryForFeature(t2.getUniqueName());
						fireDataStoreChange(updateEvent, new DataStoreChangeEvent(this, createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(t2)), track, DataStoreChangeEvent.Operation.DELETE));
					}
					out.write(createJSONFeatureContainer(updatedFeatures).toString());
					if (dataStore != null) {
						dataStore.writeFeature(gene);
					}					
					editor.getSession().endTransactionForAllFeatures();
				}
				/*
				editor.deleteFeature(oldTranscript1.getGene());
				if (!oldTranscript1.getGene().getUniqueName().equals(oldTranscript2.getGene().getUniqueName())) {
					editor.deleteFeature(oldTranscript2.getGene());
				}
				editor.addFeature(newTranscript.getGene());
				if (dataStore != null) {
					dataStore.deleteFeature(oldTranscript1.getGene());
					if (!oldTranscript1.getGene().getUniqueName().equals(oldTranscript2.getGene().getUniqueName())) {
						dataStore.deleteFeature(oldTranscript2.getGene());
					}
					dataStore.writeFeature(newTranscript.getGene());
				}
				JSONObject addedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(newTranscript));
				JSONObject deletedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(oldTranscript1), JSONUtil.convertBioFeatureToJSON(oldTranscript2));
				JSONObject updatedFeatures = createJSONFeatureContainer();
				for (Transcript t : newTranscript.getGene().getTranscripts()) {
					if (!t.getUniqueName().equals(oldTranscript1.getUniqueName()) && !t.getUniqueName().equals(oldTranscript2.getUniqueName())) {
						updatedFeatures.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(t));
					}
				}
				fireDataStoreChange(new DataStoreChangeEvent(this, deletedFeatures, track, Operation.DELETE), new DataStoreChangeEvent(this, addedFeatures, track, Operation.ADD), new DataStoreChangeEvent(this, updatedFeatures, track, Operation.UPDATE));
				*/
				
				/*
				Gene oldGene1 = (Gene)editor.getSession().getFeatureByUniqueName(oldTranscript1.getGene().getUniqueName());
				if (oldGene1 != null) {
					editor.deleteTranscript(oldGene1, oldTranscript1);
					if (oldGene1.getTranscripts().size() == 0) {
						editor.deleteFeature(oldGene1);
					}
				}
				else {
					for (Transaction t : historyStore.getTransactionListForFeature(oldTranscript2.getUniqueName())) {
						t.setFeatureUniqueName(oldTranscript1.getUniqueName());
						historyStore.addTransaction(t);
					}
					oldGene1 = oldTranscript1.getGene();
				}
				Gene oldGene2 = (Gene)editor.getSession().getFeatureByUniqueName(oldTranscript2.getGene().getUniqueName());
				if (oldGene2 != null) {
					editor.deleteTranscript(oldGene2, oldTranscript2);
					if (oldGene2.getTranscripts().size() == 0) {
						editor.deleteFeature(oldGene2);
					}
				}
				else {
					for (Transaction t : historyStore.getTransactionListForFeature(oldTranscript1.getUniqueName())) {
						t.setFeatureUniqueName(oldTranscript2.getUniqueName());
						historyStore.addTransaction(t);
					}
					oldGene2 = oldTranscript2.getGene();
				}
				Gene newGene = (Gene)editor.getSession().getFeatureByUniqueName(newTranscript.getGene().getUniqueName());
				if (newGene == null) {
					editor.addFeature(newTranscript.getGene());
				}
				else {
					editor.addTranscript(newGene, newTranscript);
				}
				if (dataStore != null) {
					dataStore.deleteFeature(oldGene1);
					dataStore.deleteFeature(oldGene2);
					dataStore.writeFeature(getTopLevelFeatureForTranscript(newTranscript));
				}
				JSONObject addedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(newTranscript));
				JSONObject deletedFeatures = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(oldTranscript1), JSONUtil.convertBioFeatureToJSON(oldTranscript2));
				fireDataStoreChange(new DataStoreChangeEvent(this, deletedFeatures, track, Operation.DELETE), new DataStoreChangeEvent(this, addedFeatures, track, Operation.ADD));
				*/
				
				/*
				AbstractSingleLocationBioFeature transcript1 = transaction.getOldFeatures().get(0);
				AbstractSingleLocationBioFeature transcript2 = transaction.getOldFeatures().get(1);
				if (editor.getSession().getFeatureByUniqueName(transcript1.getUniqueName()) == null) {
//					editor.addFeature(((Transcript)transcript1).getGene());
					addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(transcript1), track, new PreDefinedNameAdapter(transcript1.getUniqueName()));
					for (Transaction t : historyStore.getTransactionListForFeature(transcript2.getUniqueName())) {
						t.setFeatureUniqueName(transcript1.getUniqueName());
						historyStore.addTransaction(t);
					}
					/ *
					Transaction addTransaction = new Transaction(Transaction.Operation.ADD_TRANSCRIPT, transcript1.getUniqueName(), username);
					addTransaction.addNewFeature(transcript1);
					historyStore.addTransaction(addTransaction);
					transaction.setFeatureUniqueName(transcript1.getUniqueName());
					writeHistoryToStore(historyStore, transaction);
					* /
				}
				else if (uniqueName.equals(transcript1.getUniqueName())) {
					if (!historyStore.getTransactionListForFeature(transcript2.getUniqueName()).equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
						historyStore.setToNextTransactionForFeature(transcript2.getUniqueName());
					}
				}
				if (editor.getSession().getFeatureByUniqueName(transcript2.getUniqueName()) == null) {
//					editor.addFeature(((Transcript)transcript2).getGene());
					addTranscript(editor, session, JSONUtil.convertBioFeatureToJSON(transcript2), track, new PreDefinedNameAdapter(transcript2.getUniqueName()));
					Transaction addTransaction = new Transaction(Transaction.Operation.ADD_TRANSCRIPT, transcript2.getUniqueName(), username);
					addTransaction.addNewFeature(transcript2);
					historyStore.addTransaction(addTransaction);
					transaction.setFeatureUniqueName(transcript2.getUniqueName());
					writeHistoryToStore(historyStore, transaction);
				}
				else if (uniqueName.equals(transcript2.getUniqueName())) {
					if (!historyStore.getTransactionListForFeature(transcript1.getUniqueName()).equals(Transaction.Operation.MERGE_TRANSCRIPTS)) {
						historyStore.setToNextTransactionForFeature(transcript1.getUniqueName());
					}
				}

				mergeTranscripts(editor, dataStore, null, (JSONUtil.convertBioFeatureToJSON(transcript1)), (JSONUtil.convertBioFeatureToJSON(transcript2)), track, null, out);
				*/
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SPLIT_TRANSCRIPT)) {
				Transcript transcript1 = (Transcript)transaction.getNewFeatures().get(0);
				Transcript transcript2 = (Transcript)transaction.getNewFeatures().get(1);
				Exon leftExon = null;
				Exon rightExon = null;
				for (Exon exon : transcript1.getExons()) {
					if (leftExon == null || exon.getFmin() > leftExon.getFmin()) {
						leftExon = exon;
					}
				}
				for (Exon exon : transcript2.getExons()) {
					if (rightExon == null || exon.getFmin() < rightExon.getFmin()) {
						rightExon = exon;
					}
				}
				JSONArray jsonFeatures = new JSONArray();
				jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(leftExon));
				jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(rightExon));
//				splitTranscript(editor, session, new PreDefinedNameAdapter(transcript2.getUniqueName()), new PreDefinedNameAdapter(transcript2.getGene().getUniqueName()), dataStore, historyStore, jsonFeatures, track, transaction.getEditor(), out, false, true);
	
				AbstractNameAdapter geneNameAdapter = editor.getSession().getFeatureByUniqueName(transcript2.getGene().getUniqueName()) != null ? new HttpSessionTimeStampNameAdapter(session, editor.getSession()) : new PreDefinedNameAdapter(transcript2.getGene().getUniqueName());
				splitTranscript(editor, session, new PreDefinedNameAdapter(transcript2.getUniqueName()), geneNameAdapter, dataStore, historyStore, jsonFeatures, track, transaction.getEditor(), out, false, true);

			}
			/*
			else if (transaction.getOperation().equals(Transaction.Operation.SET_TRANSLATION_START)) {
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				CDS newCDS = newTranscript.getCDS();
//				CDS newCDS = (CDS)transaction.getNewFeatures().get(0);
				int fmin = newCDS.getStrand().equals(-1) ? newCDS.getFmax() - 1 : newCDS.getFmin();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", fmin);
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationStart(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_TRANSLATION_END)) {
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				CDS newCDS = newTranscript.getCDS();
//				CDS newCDS = (CDS)transaction.getNewFeatures().get(0);
				int fmax = newCDS.getStrand().equals(-1) ? newCDS.getFmin() - 1 : newCDS.getFmax();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmax", fmax);
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationEnd(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			*/
			else if (transaction.getOperation().equals(Transaction.Operation.SET_TRANSLATION_START)) {
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				CDS newCDS = newTranscript.getCDS();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", newCDS.getFmin());
				jsonLocation.put("fmax", newCDS.getFmax());
				jsonLocation.put("strand", newCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation).put("manually_set_start", true);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.UNSET_TRANSLATION_START)) {
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				CDS newCDS = newTranscript.getCDS();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", newCDS.getFmin());
				jsonLocation.put("fmax", newCDS.getFmax());
				jsonLocation.put("strand", newCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_TRANSLATION_END)) {
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				CDS newCDS = newTranscript.getCDS();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", newCDS.getFmin());
				jsonLocation.put("fmax", newCDS.getFmax());
				jsonLocation.put("strand", newCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation).put("manually_set_end", true);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.UNSET_TRANSLATION_END)) {
				Transcript newTranscript = (Transcript)transaction.getNewFeatures().get(0);
				CDS newCDS = newTranscript.getCDS();
				JSONObject jsonLocation = new JSONObject();
				jsonLocation.put("fmin", newCDS.getFmin());
				jsonLocation.put("fmax", newCDS.getFmax());
				jsonLocation.put("strand", newCDS.getStrand());
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("location", jsonLocation);
				setTranslationEnds(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_LONGEST_ORF)) {
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName);
				setLongestORF(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.FLIP_STRAND)) {
				JSONArray jsonFeatures = new JSONArray();
				for (AbstractSingleLocationBioFeature feature : transaction.getNewFeatures()) {
					jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(feature));
				}
				flipStrand(editor, session, dataStore, null, jsonFeatures, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.SET_READTHROUGH_STOP_CODON)) {
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("readthrough_stop_codon", true);
				setReadthroughStopCodon(editor, dataStore, null, jsonTranscript, track, null, out);
			}
			else if (transaction.getOperation().equals(Transaction.Operation.UNSET_READTHROUGH_STOP_CODON)) {
				JSONObject jsonTranscript = new JSONObject().put("uniquename", uniqueName).put("readthrough_stop_codon", false);
				setReadthroughStopCodon(editor, dataStore, null, jsonTranscript, track, null, out);
			}
		}
	}
	
	private int getUserPermission(String track, String username) throws SQLException {
		return UserManager.getInstance().getTrackPermissionForUser(track, username);
	}
	
	private void getUserPermission(int permission, String track, String username, BufferedWriter out) throws IOException, JSONException {
		out.write(new JSONObject().put("permission", permission).put("username", username).toString());
	}
	
	private void getInformation(AnnotationEditor editor, JSONArray features, BufferedWriter out) throws JSONException, IOException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			String uniqueName = jsonFeature.getString("uniquename");
			AbstractSingleLocationBioFeature gbolFeature = editor.getSession().getFeatureByUniqueName(uniqueName);
			Date timeAccessioned = gbolFeature.getTimeAccessioned();
			String owner = gbolFeature.getOwner().getOwner();
			JSONObject info = new JSONObject();
			info.put("uniquename", uniqueName).put("time_accessioned", timeAccessioned.toString()).put("owner", owner);
			String parentIds = "";
			for (AbstractSingleLocationBioFeature feature : gbolFeature.getParents()) {
				if (parentIds.length() > 0) {
					parentIds += ", ";
				}
				parentIds += feature.getUniqueName();
			}
			if (parentIds.length() > 0) {
				info.put("parent_ids", parentIds);
			}
			featureContainer.getJSONArray("features").put(info);
		}
		out.write(featureContainer.toString());
	}
	
	private void getSequence(AnnotationEditor editor, JSONArray features, String type, int flank, BufferedWriter out) throws JSONException, IOException {
		JSONObject featureContainer = createJSONFeatureContainer();
		for (int i = 0; i < features.length(); ++i) {
			JSONObject jsonFeature = features.getJSONObject(i);
			String uniqueName = jsonFeature.getString("uniquename");
			AbstractSingleLocationBioFeature gbolFeature = editor.getSession().getFeatureByUniqueName(uniqueName);
			String sequence = null;
			if (type.equals("peptide")) {
				if (gbolFeature instanceof Transcript && ((Transcript)gbolFeature).isProteinCoding()) {
					String rawSequence = editor.getSession().getResiduesWithAlterationsAndFrameshifts(((Transcript)gbolFeature).getCDS());
					sequence = SequenceUtil.translateSequence(rawSequence, editor.getConfiguration().getTranslationTable(), true, ((Transcript)gbolFeature).getCDS().getStopCodonReadThrough() != null);
					if (sequence.charAt(sequence.length() - 1) == TranslationTable.STOP.charAt(0)) {
						sequence = sequence.substring(0, sequence.length() - 1);
					}
					int idx;
					if ((idx = sequence.indexOf(TranslationTable.STOP)) != -1) {
						String codon = rawSequence.substring(idx * 3, idx * 3 + 3);
						String aa = editor.getConfiguration().getTranslationTable().getAlternateTranslationTable().get(codon);
						if (aa != null) {
							sequence = sequence.replace(TranslationTable.STOP, aa);
						}
					}
				}
				else if (gbolFeature instanceof Exon && ((Exon)gbolFeature).getTranscript().isProteinCoding()) {
					String rawSequence = getCodingSequenceInPhase(editor, (Exon)gbolFeature, true);
					sequence = SequenceUtil.translateSequence(rawSequence, editor.getConfiguration().getTranslationTable(), true, ((Exon)gbolFeature).getTranscript().getCDS().getStopCodonReadThrough() != null);
					if (sequence.charAt(sequence.length() - 1) == TranslationTable.STOP.charAt(0)) {
						sequence = sequence.substring(0, sequence.length() - 1);
					}
					int idx;
					if ((idx = sequence.indexOf(TranslationTable.STOP)) != -1) {
						String codon = rawSequence.substring(idx * 3, idx * 3 + 3);
						String aa = editor.getConfiguration().getTranslationTable().getAlternateTranslationTable().get(codon);
						if (aa != null) {
							sequence = sequence.replace(TranslationTable.STOP, aa);
						}
					}
				}
				else {
//					sequence = SequenceUtil.translateSequence(editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature), editor.getConfiguration().getTranslationTable());
					sequence = "";
				}
				
			}
			else if (type.equals("cdna")) {
				if (gbolFeature instanceof Transcript || gbolFeature instanceof Exon) {
					sequence = editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature);
				}
				else {
					sequence = "";
				}
			}
			else if (type.equals("cds")) {
				if (gbolFeature instanceof Transcript && ((Transcript)gbolFeature).isProteinCoding()) {
					sequence = editor.getSession().getResiduesWithAlterationsAndFrameshifts(((Transcript)gbolFeature).getCDS());
				}
				else if (gbolFeature instanceof Exon && ((Exon)gbolFeature).getTranscript().isProteinCoding()) {
					sequence = getCodingSequenceInPhase(editor, (Exon)gbolFeature, false);
				}
				else {
//					sequence = editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature);
					sequence = "";
				}
			}
			else if (type.equals("genomic")) {
				AbstractSingleLocationBioFeature genomicFeature = new AbstractSingleLocationBioFeature((Feature)((SimpleObjectIteratorInterface)gbolFeature.getWriteableSimpleObjects(bioObjectConfiguration)).next(), bioObjectConfiguration) { };
				FeatureLazyResidues sourceFeature = (FeatureLazyResidues)gbolFeature.getFeatureLocation().getSourceFeature();
				genomicFeature.getFeatureLocation().setSourceFeature(sourceFeature);
				if (flank > 0) {
					int fmin = genomicFeature.getFmin() - flank;
//					if (fmin < 0) {
//						fmin = 0;
//					}
					if (fmin < sourceFeature.getFmin()) {
						fmin = sourceFeature.getFmin();
					}
					int fmax = genomicFeature.getFmax() + flank;
//					if (fmax > genomicFeature.getFeatureLocation().getSourceFeature().getSequenceLength()) {
//						fmax = genomicFeature.getFeatureLocation().getSourceFeature().getSequenceLength();
//					}
					if (fmax > sourceFeature.getFmax()) {
						fmax = sourceFeature.getFmax();
					}
					genomicFeature.setFmin(fmin);
					genomicFeature.setFmax(fmax);
				}
				gbolFeature = genomicFeature;
				sequence = editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature);
			}
			JSONObject outFeature = JSONUtil.convertBioFeatureToJSON(gbolFeature);
			outFeature.put("residues", sequence);
			outFeature.put("uniquename", uniqueName);
			outFeature.put("residues", sequence);
			featureContainer.getJSONArray("features").put(outFeature);
		}
		out.write(featureContainer.toString());
	}
	
	private AbstractSingleLocationBioFeature getFeature(AnnotationEditor editor, JSONObject jsonFeature) throws JSONException, AnnotationEditorServiceException {
		try {
			String uniqueName = jsonFeature.getString("uniquename");
			return getFeature(editor, uniqueName);
		}
		catch (JSONException e) {
			throw new JSONException("JSON feature object lacks 'uniquename' field");
		}
	}
	
	private AbstractSingleLocationBioFeature getFeature(AnnotationEditor editor, String uniqueName) throws AnnotationEditorServiceException {
		AbstractSingleLocationBioFeature feature = editor.getSession().getFeatureByUniqueName(uniqueName);
		if (feature == null) {
			throw new AnnotationEditorServiceException("Feature with unique name " + uniqueName + " not found");
		}
		return feature;
	}
	
	private void updateNewGsolFeatureAttributes(Feature gsolFeature, Feature sourceFeature) {
		gsolFeature.setIsAnalysis(false);
		gsolFeature.setIsObsolete(false);
		gsolFeature.setTimeAccessioned(new Date()); //new Timestamp(new Date().getTime()));
		gsolFeature.setTimeLastModified(new Date()); //new Timestamp(new Date().getTime()));
		if (sourceFeature != null) {
			gsolFeature.getFeatureLocations().iterator().next().setSourceFeature(sourceFeature);
		}
		for (FeatureRelationship fr : gsolFeature.getChildFeatureRelationships()) {
			updateNewGsolFeatureAttributes(fr.getSubjectFeature(), sourceFeature);
		}
	}

	private void updateNewGbolFeatureAttributes(AbstractSingleLocationBioFeature gbolFeature, Feature sourceFeature) {
		gbolFeature.setIsAnalysis(false);
		gbolFeature.setIsObsolete(false);
		gbolFeature.setTimeAccessioned(new Date()); //new Timestamp(new Date().getTime()));
		gbolFeature.setTimeLastModified(new Date()); //new Timestamp(new Date().getTime()));
		if (sourceFeature != null) {
			gbolFeature.getFeatureLocations().iterator().next().setSourceFeature(sourceFeature);
		}
		for (AbstractSingleLocationBioFeature child : gbolFeature.getChildren()) {
			updateNewGbolFeatureAttributes(child, sourceFeature);
		}
	}
	
	private void updateTranscriptAttributes(Transcript transcript) {
		AbstractNameAdapter nameAdapter = new FeatureNameAdapter();
		if (transcript.getName() == null) {
			transcript.setName(nameAdapter.generateName(transcript));
		}
	}
	
	private SessionData getSessionData(String track) {
        SessionData sessionData;
        AnnotationEditor editor = trackToEditor.get(track);
        if (editor == null) {
        	AbstractDataStore dataStore = new JEDatabase(getStorageFile(track).getAbsolutePath());
        	DataStore sessionDataStore = useMemoryStore ?
        			new JEDatabaseSessionMemoryDataStore((JEDatabase)dataStore, trackToSourceFeature.get(track), bioObjectConfiguration) :
        			new JEDatabaseSessionHybridArrayDataStore((JEDatabase)dataStore, bioObjectConfiguration, trackToSourceFeature.get(track));
        	editor = new AnnotationEditor(new AnnotationSession(sessionDataStore), new Configuration());
        	editor.getConfiguration().setTranslationTable(trackToTranslationTable.get(track));
        	trackToEditor.put(track, editor);
        	
        	//              AbstractDataStore dataStore = new JEDatabase(getStorageFile(track).getAbsolutePath());

        	/*
        	if (useMemoryStore) {
        		java.util.Iterator<Feature> iter = dataStore.getFeatureIterator();
        		while (iter.hasNext()) {
        			Feature feature = iter.next();
        			addSourceToFeature(feature, trackToSourceFeature.get(track));
        			editor.getSession().addFeature((AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(feature, bioObjectConfiguration));
        		}
        		iter = dataStore.getSequenceAlterationIterator();
        		while (iter.hasNext()) {
        			Feature sequenceAlteration = iter.next();
        			addSourceToFeature(sequenceAlteration, trackToSourceFeature.get(track));
        			editor.getSession().addSequenceAlteration((SequenceAlteration)BioObjectUtil.createBioObject(sequenceAlteration, bioObjectConfiguration));
        		}
        	}
        	*/

        	AbstractDataStoreManager.getInstance().addDataStore(track, dataStore);

        	AbstractHistoryStore historyStore = new JEHistoryDatabase(getStorageFile(track + "_history").getAbsolutePath(), false, historySize);
        	AbstractHistoryStoreManager.getInstance().addHistoryStore(track, historyStore);

        	sessionData = new SessionData(editor, dataStore, historyStore);
        }
        else {
        	sessionData = new SessionData(editor, AbstractDataStoreManager.getInstance().getDataStore(track), AbstractHistoryStoreManager.getInstance().getHistoryStore(track));
        }
//      if (session.getAttribute("uniquenameCounter") == null) {
//              session.setAttribute("uniquenameCounter", getUniquenameCounter(session.getId(), editor.getSession().getFeatures()));
//              int count = getFeatureCount(editor.getSession().getFeatures());
//              count += getFeatureCount(editor.getSession().getSequenceAlterations());
//              session.setAttribute("uniquenameCounter", count);
//      }
        
        trackToLastAccess.put(track, new Date());
        
//        printMemoryUsage();
        
        return sessionData;
	}

	private void checkValidity(AbstractSingleLocationBioFeature feature) throws AnnotationEditorServiceException {
		if (feature instanceof Transcript && ((Transcript)feature).getGene() == null) {
			throw new AnnotationEditorServiceException("Transcript does not have a gene parent: " + feature.getUniqueName());
		}
	}
	
	private void writeHistoryToStore(AbstractHistoryStore historyStore, Transaction transaction) {
		synchronized (historyStore) {
			historyStore.addTransaction(transaction);
		}
	}
	
	private void writeFeatureToStore(AnnotationEditor editor, AbstractDataStore dataStore, AbstractSingleLocationBioFeature feature, String track) throws AnnotationEditorServiceException {
		checkValidity(feature);
		SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
		Feature gsolFeature = (Feature)iterator.next();
		removeSourceFromFeature(gsolFeature);
		synchronized (dataStore) {
			if (feature instanceof SequenceAlteration) {
				dataStore.writeSequenceAlteration(gsolFeature);
			}
			else {
				dataStore.writeFeature(gsolFeature);
			}

//			editor.getSession().endTransactionForFeature(feature);
			
		}
		addSourceToFeature(gsolFeature, trackToSourceFeature.get(track));
		
		editor.getSession().endTransactionForAllFeatures();
	}
	
	private void deleteFeatureFromStore(AbstractDataStore dataStore, AbstractSingleLocationBioFeature feature) {
		SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
		Feature gsolFeature = (Feature)iterator.next();
		if (feature instanceof SequenceAlteration) {
			dataStore.deleteSequenceAlteration(gsolFeature);
		}
		else {
			dataStore.deleteFeature(gsolFeature);
		}
	}
	
	private AbstractSingleLocationBioFeature getTopLevelFeatureForTranscript(Transcript transcript) {
		if (transcript.getGene() != null) {
			return transcript.getGene();
		}
		return transcript;
	}
	
	private void removeSourceFromFeature(Feature feature) {
		if (feature.getFeatureLocations().size() > 0) {
			feature.getFeatureLocations().iterator().next().setSourceFeature(null);
		}
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			removeSourceFromFeature(fr.getSubjectFeature());
		}
	}
	
	private void addSourceToFeature(Feature feature, Feature sourceFeature) {
		if (feature.getFeatureLocations().size() > 0) {
			feature.getFeatureLocations().iterator().next().setSourceFeature(sourceFeature);
		}
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			addSourceToFeature(fr.getSubjectFeature(), sourceFeature);
		}
	}
	
//	private int getFeatureCount(Collection<? extends AbstractSingleLocationBioFeature> features) {
//		int numFeatures = 0;
//		for (AbstractSingleLocationBioFeature feature : features) {
//			numFeatures += getFeatureCount(feature, 0);
//		}
//		return numFeatures;
//	}
//
//	private int getFeatureCount(AbstractSingleLocationBioFeature feature, int count) {
//		++count;
//		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
//			count = getFeatureCount(child, count);
//		}
//		return count;
//	}

	private void fireDataStoreChange(JSONObject features, String track, DataStoreChangeEvent.Operation operation) {
		AbstractDataStoreManager.getInstance().fireDataStoreChange(new DataStoreChangeEvent(this, features, track, operation));
	}

	private void fireDataStoreChange(DataStoreChangeEvent ... events) {
		AbstractDataStoreManager.getInstance().fireDataStoreChange(events);
	}
	
	private int getUniquenameCounter(String sessionId, Collection<? extends AbstractSingleLocationBioFeature> features) {
		int count = 0;
		for (AbstractSingleLocationBioFeature feature : features) {
			count = getUniquenameCounter(sessionId, feature, 0);
		}
		return count;
	}
	
	private int getUniquenameCounter(String sessionId, AbstractSingleLocationBioFeature feature, int count) {
		if (feature.getUniqueName().startsWith(sessionId)) {
			int featureNum = Integer.parseInt(feature.getUniqueName().substring(sessionId.length() + 1));
			if (featureNum > count) {
				count = featureNum;
			}
		}
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			count = getUniquenameCounter(sessionId, child, count);
		}
		return count;

	}
	
	private File getStorageFile(String filename) {
		File dir = new File(dataStoreDirectory + "/" + filename);
		return dir;
	}
	
	private AbstractSingleLocationBioFeature cloneFeature(AbstractSingleLocationBioFeature feature) {
		return feature.cloneFeature(feature.getUniqueName());
	}
	
	private Gene cloneGene(Gene gene) {
		Gene copy = gene.isPseudogene() ? new Pseudogene((Pseudogene)gene, gene.getUniqueName()) : new Gene(gene, gene.getUniqueName());
		copy.setName(gene.getName());
		if (copy.getSymbol() != null) {
			copy.setSymbol(gene.getSymbol().getSymbol());
		}
		if (copy.getDescription() != null) {
			copy.setDescription(gene.getDescription().getDescription());
		}
		for (Comment comment : gene.getComments()) {
			copy.addComment(comment);
		}
		for (DBXref dbxref : gene.getNonPrimaryDBXrefs()) {
			copy.addNonPrimaryDBXref(dbxref);
		}
		for (GenericFeatureProperty featureProp : gene.getNonReservedProperties()) {
			copy.addNonReservedProperty(featureProp.getTag(), featureProp.getValue());
		}
		if (gene.getStatus() != null) {
			copy.setStatus(gene.getStatus().getStatus());
		}
		setOwner(copy, gene.getOwner().getOwner());
		copy.setTimeAccessioned(gene.getTimeAccessioned());
		copy.setTimeLastModified(gene.getTimeLastModified());
		return copy;
	}

	private Transcript cloneTranscript(Transcript transcript) {
		return cloneTranscript(transcript, false);
	}
	
	private Transcript cloneTranscript(Transcript transcript, boolean cloneGene) {
		Transcript copy = (Transcript)transcript.cloneFeature(transcript.getUniqueName());
		copy.setName(transcript.getName());
		if (copy.getSymbol() != null) {
			copy.setSymbol(transcript.getSymbol().getSymbol());
		}
		if (copy.getDescription() != null) {
			copy.setDescription(transcript.getDescription().getDescription());
		}
		for (Exon exon : transcript.getExons()) {
			copy.addExon(cloneExon(exon));
		}
		for (NonCanonicalFivePrimeSpliceSite spliceSite : transcript.getNonCanonicalFivePrimeSpliceSites()) {
			copy.addNonCanonicalFivePrimeSpliceSite(spliceSite);
		}
		for (NonCanonicalThreePrimeSpliceSite spliceSite : transcript.getNonCanonicalThreePrimeSpliceSites()) {
			copy.addNonCanonicalThreePrimeSpliceSite(spliceSite);
		}
		for (Comment comment : transcript.getComments()) {
			copy.addComment(comment);
		}
		for (DBXref dbxref : transcript.getNonPrimaryDBXrefs()) {
			copy.addNonPrimaryDBXref(dbxref);
		}
		for (GenericFeatureProperty featureProp : transcript.getNonReservedProperties()) {
			copy.addNonReservedProperty(featureProp.getTag(), featureProp.getValue());
		}
		if (transcript.getStatus() != null) {
			copy.setStatus(transcript.getStatus().getStatus());
		}
		if (cloneGene) {
			Gene gene = cloneGene(transcript.getGene());
			for (Transcript t : transcript.getGene().getTranscripts()) {
				if (!t.getUniqueName().equals(transcript.getUniqueName())) {
					gene.addTranscript(cloneTranscript(t));
				}
			}
			copy.setGene(gene);
		}
		if (transcript.getCDS() != null) {
			copy.setCDS(cloneCDS(transcript.getCDS()));
		}
		setOwner(copy, transcript.getOwner().getOwner());
		copy.setTimeAccessioned(transcript.getTimeAccessioned());
		copy.setTimeLastModified(transcript.getTimeLastModified());
		return copy;
	}
	
	private Exon cloneExon(Exon exon) {
		return new Exon(exon, exon.getUniqueName());
	}
	
	private CDS cloneCDS(CDS cds) {
		CDS copy = new CDS(cds, cds.getUniqueName());
		for (Comment comment : cds.getComments()) {
			copy.addComment(comment);
		}
		return copy;
	}
	
	private NonCanonicalFivePrimeSpliceSite cloneNonCanonicalFivePrimeSpliceSite(NonCanonicalFivePrimeSpliceSite spliceSite) {
		return new NonCanonicalFivePrimeSpliceSite(spliceSite, spliceSite.getUniqueName());
	}

	private NonCanonicalThreePrimeSpliceSite cloneNonCanonicalThreePrimeSpliceSite(NonCanonicalFivePrimeSpliceSite spliceSite) {
		return new NonCanonicalThreePrimeSpliceSite(spliceSite, spliceSite.getUniqueName());
	}

	private void setOwner(AbstractSingleLocationBioFeature feature, String owner) {
		feature.setOwner(owner);
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			setOwner(child, owner);
		}
	}
	
	private void sendError(HttpServletResponse response, int statusCode, String message) {
		try {
			response.getOutputStream().print(message);
			response.setStatus(statusCode);
//			response.sendError(statusCode, new JSONObject().put("error", message).toString());
		}
		catch (Exception e) {
		}
	}
	
	private void sendConfirm(HttpServletResponse response, String message) {
		try {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("confirm", message).toString());
		}
		catch (Exception e) {
		}
	}
	
	private void updateTranscript(AnnotationEditor editor, Transcript updatedTranscript, AbstractDataStore dataStore, String track) throws AnnotationEditorServiceException, JSONException {
		Transcript originalTranscript = (Transcript)getFeature(editor, updatedTranscript.getUniqueName());
		Gene gene = originalTranscript.getGene();
//		editor.calculateCDS(updatedTranscript);
		editor.deleteTranscript(gene, originalTranscript);
		editor.addTranscript(gene, updatedTranscript);
		JSONObject featureContainer = createJSONFeatureContainer();
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(updatedTranscript), track);
		}
		featureContainer.getJSONArray("features").put(JSONUtil.convertBioFeatureToJSON(updatedTranscript));
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private Transcript addTranscript(AnnotationEditor editor, HttpSession session, JSONObject jsonTranscript, String track, AbstractNameAdapter nameAdapter, boolean isPseudogene) throws JSONException, AnnotationEditorServiceException {
		Gene gene = jsonTranscript.has("parent_id") ? (Gene)editor.getSession().getFeatureByUniqueName(jsonTranscript.getString("parent_id")) : null;
		Transcript transcript = null;
		if (gene != null) {
			Feature gsolTranscript = JSONUtil.convertJSONToFeature(jsonTranscript, bioObjectConfiguration, trackToSourceFeature.get(track), nameAdapter);
			transcript = (Transcript)BioObjectUtil.createBioObject(gsolTranscript, bioObjectConfiguration);
			if (transcript.getFmin() < 0 || transcript.getFmax() < 0) {
				throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
			}

			setOwner(transcript, (String)session.getAttribute("username"));
			if (!useCDS || transcript.getCDS() == null) {
				calculateCDS(editor, transcript);
			}
			editor.addTranscript(gene, transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
			updateTranscriptAttributes(transcript);
		}
		else {
			Collection<AbstractSingleLocationBioFeature> overlappingFeatures = editor.getSession().getOverlappingFeatures(JSONUtil.convertJSONToFeatureLocation(jsonTranscript.getJSONObject("location"), trackToSourceFeature.get(track)));
			for (AbstractSingleLocationBioFeature feature : overlappingFeatures) {
				if (feature instanceof Gene && !((Gene)feature).isPseudogene() && overlapper != null) {
					Gene tmpGene = (Gene)feature;
					Feature gsolTranscript = JSONUtil.convertJSONToFeature(jsonTranscript, bioObjectConfiguration, trackToSourceFeature.get(track), nameAdapter);
					updateNewGsolFeatureAttributes(gsolTranscript, trackToSourceFeature.get(track));
					Transcript tmpTranscript = (Transcript)BioObjectUtil.createBioObject(gsolTranscript, bioObjectConfiguration);
					if (tmpTranscript.getFmin() < 0 || tmpTranscript.getFmax() < 0) {
						throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
					}
					setOwner(tmpTranscript, (String)session.getAttribute("username"));
					if (!useCDS || tmpTranscript.getCDS() == null) {
						calculateCDS(editor, tmpTranscript);
					}
					updateTranscriptAttributes(tmpTranscript);
					if (overlapper.overlaps(tmpTranscript, tmpGene)) {
						transcript = tmpTranscript;
						gene = tmpGene;
						editor.addTranscript(gene, transcript);
						findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
						break;
					}
					else {
//						editor.getSession().endTransactionForFeature(feature);
					}
				}
				else {
//					editor.getSession().endTransactionForFeature(feature);
				}
			}
		}
		if (gene == null) {
			JSONObject jsonGene = new JSONObject();
			jsonGene.put("children", new JSONArray().put(jsonTranscript));
			jsonGene.put("location", jsonTranscript.getJSONObject("location"));
			jsonGene.put("type", JSONUtil.convertCVTermToJSON(bioObjectConfiguration.getDefaultCVTermForClass(isPseudogene ? "Pseudogene" : "Gene")));
			Feature gsolGene = JSONUtil.convertJSONToFeature(jsonGene, bioObjectConfiguration, trackToSourceFeature.get(track), nameAdapter);
			updateNewGsolFeatureAttributes(gsolGene, trackToSourceFeature.get(track));
			gene = (Gene)BioObjectUtil.createBioObject(gsolGene, bioObjectConfiguration);
			if (gene.getFmin() < 0 || gene.getFmax() < 0) {
				throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
			}
			setOwner(gene, (String)session.getAttribute("username"));
			transcript = gene.getTranscripts().iterator().next();
			if (!useCDS || transcript.getCDS() == null) {
				calculateCDS(editor, transcript);
			}
			editor.addFeature(gene);
			updateTranscriptAttributes(transcript);
			findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
		}
		return transcript;
	}
	
	private String getCodingSequenceInPhase(AnnotationEditor editor, Exon exon, boolean removePartialCodons) {
		Transcript transcript = exon.getTranscript();
		CDS cds = transcript.getCDS();
		if (cds == null || !exon.overlaps(cds)) {
			return "";
		}
		int length = 0;
		FlankingRegion flankingRegion = new FlankingRegion(null, null, false, false, null, exon.getConfiguration());
		flankingRegion.setFeatureLocation(new FeatureLocation());
		flankingRegion.getFeatureLocation().setSourceFeature(exon.getFeatureLocation().getSourceFeature());
		flankingRegion.setStrand(exon.getStrand());
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), true);
		for (Exon e : exons) {
			if (e.equals(exon)) {
				break;
			}
			if (!e.overlaps(cds)) {
				continue;
			}
			int fmin = e.getFmin() < cds.getFmin() ? cds.getFmin() : e.getFmin();
			int fmax = e.getFmax() > cds.getFmax() ? cds.getFmax() : e.getFmax();
			flankingRegion.setFmin(fmin);
			flankingRegion.setFmax(fmax);
			length += editor.getSession().getResiduesWithAlterationsAndFrameshifts(flankingRegion).length();
		}
		flankingRegion.setFmin(exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin());
		flankingRegion.setFmax(exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax());
		String residues = editor.getSession().getResiduesWithAlterationsAndFrameshifts(flankingRegion);
		if (removePartialCodons) {
			int phase = length % 3 == 0 ? 0 : 3 - (length % 3);
			residues = residues.substring(phase);
			residues = residues.substring(0, residues.length() - (residues.length() % 3));
		}
		return residues;
	}
	
	private void setReadthroughStopCodon(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore, JSONObject jsonTranscript, String track, String username, BufferedWriter out) throws JSONException, IOException, AnnotationEditorServiceException {
		Transcript transcript = (Transcript)getFeature(editor, jsonTranscript);
		Transcript oldTranscript = cloneTranscript(transcript);
		boolean readThroughStopCodon = jsonTranscript.getBoolean("readthrough_stop_codon");
		calculateCDS(editor, transcript, readThroughStopCodon);
		if (dataStore != null) {
			writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(transcript), track);
		}
		if (historyStore != null) {
			Transaction transaction = new Transaction(readThroughStopCodon ? Transaction.Operation.SET_READTHROUGH_STOP_CODON : Transaction.Operation.UNSET_READTHROUGH_STOP_CODON, transcript.getUniqueName(), username);
//			transaction.addOldFeature(oldTranscript.getCDS());
//			transaction.addNewFeature(transcript.getCDS());
			transaction.addOldFeature(oldTranscript);
			transaction.addNewFeature(transcript);
			writeHistoryToStore(historyStore, transaction);
		}
		out.write(createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(getTopLevelFeatureForTranscript(transcript))).toString());
		JSONObject featureContainer = createJSONFeatureContainer(JSONUtil.convertBioFeatureToJSON(transcript));
		fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.UPDATE);
	}
	
	private class AnnotationEditorServiceException extends Exception {

		private static final long serialVersionUID = 1L;

		/** Constructor.
		 * 
		 * @param message - String describing the error
		 */
		public AnnotationEditorServiceException(String message) {
			super(message);
		}
		
		public AnnotationEditorServiceException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}

	private class SessionData {

		private AnnotationEditor editor;
		private AbstractDataStore dataStore;
		private AbstractHistoryStore historyStore;
		
		public SessionData(AnnotationEditor editor, AbstractDataStore dataStore, AbstractHistoryStore historyStore) {
			this.editor = editor;
			this.dataStore = dataStore;
			this.historyStore = historyStore;
		}

		public AnnotationEditor getEditor() {
			return editor;
		}

		public AbstractDataStore getDataStore() {
			return dataStore;
		}

		public AbstractHistoryStore getHistoryStore() {
			return historyStore;
		}

	}

	private void cleanup(String track) {
		AnnotationEditor editor = trackToEditor.get(track);
		synchronized (editor) {
			trackToEditor.remove(track);
			AbstractDataStoreManager.getInstance().closeDataStore(track);
			AbstractHistoryStoreManager.getInstance().closeHistoryStore(track);
			trackToLastAccess.remove(track);
		}
	}
	
	private void printMemoryUsage() {
		 Runtime runtime = Runtime.getRuntime();
		 java.text.NumberFormat format = java.text.NumberFormat.getInstance();
		 StringBuilder sb = new StringBuilder();
		 long maxMemory = runtime.maxMemory();
		 long allocatedMemory = runtime.totalMemory();
		 long freeMemory = runtime.freeMemory();

		 sb.append("free memory: " + format.format(freeMemory / 1024) + "\n");
		 sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
		 sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
		 sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");
		 System.out.println(sb.toString());
	}

}
