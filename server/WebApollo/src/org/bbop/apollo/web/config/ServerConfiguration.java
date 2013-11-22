package org.bbop.apollo.web.config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ServerConfiguration {

	private String gbolMappingFile;
	private String dataStoreDirectory;
	private int defaultMinimumIntronSize;
	private int historySize;
	private String overlapperClass;
//	private String trackNameComparatorClass;
	private String trackNameComparator;
	private UserDatabaseConfiguration userDatabase;
	private String userAuthenticationClass;
	private Map<String, TrackConfiguration> tracks;
	private String cannedComments;
	private Collection<SequenceSearchToolConfiguration> sequenceSearchTools;
	private Map<String, DataAdapterGroupConfiguration> dataAdapters;
	private boolean useCDS;
	private boolean useMemoryStore;
	private Map<String, AnnotationInfoEditorConfiguration> annotationInfoEditors;
//	private Collection<AnnotationInfoEditorConfiguration> annotationInfoEditors;


	public ServerConfiguration(String xmlFileName) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		this(new FileInputStream(xmlFileName));
	}
	
	public ServerConfiguration(InputStream configuration) throws ParserConfigurationException, SAXException, IOException {
		defaultMinimumIntronSize = 1;
		historySize = 0;
		sequenceSearchTools = new ArrayList<SequenceSearchToolConfiguration>();
		dataAdapters = new HashMap<String, DataAdapterGroupConfiguration>();
		useCDS = false;
		useMemoryStore = true;
		annotationInfoEditors = new HashMap<String, AnnotationInfoEditorConfiguration>();
//		annotationInfoEditors = new ArrayList<AnnotationInfoEditorConfiguration>();
		init(configuration);
		configuration.close();
	}
	
	public String getGBOLMappingFile() {
		return gbolMappingFile;
	}
	
	public void setGBOLMappingFile(String gbolMappingFile) {
		this.gbolMappingFile = gbolMappingFile;
	}
	
	public String getDataStoreDirectory() {
		return dataStoreDirectory;
	}

	public void setDataStoreDirectory(String dataStoreDirectory) {
		this.dataStoreDirectory = dataStoreDirectory;
	}
	
	public int getDefaultMinimumIntronSize() {
		return defaultMinimumIntronSize;
	}
	
	public void setDefaultMinimumIntronSize(int defaultMinimumIntronSize) {
		this.defaultMinimumIntronSize = defaultMinimumIntronSize;
	}
	
	public int getHistorySize() {
		return historySize;
	}
	
	public void setHistorySize(int historySize) {
		this.historySize = historySize;
	}
	
	public String getOverlapperClass() {
		return overlapperClass;
	}

	public void setOverlapperClass(String overlapperClass) {
		this.overlapperClass = overlapperClass;
	}

	/*
	public String getTrackNameComparatorClass() {
		return trackNameComparatorClass;
	}

	public void setTrackNameComparatorClass(String trackNameComparatorClass) {
		this.trackNameComparatorClass = trackNameComparatorClass;
	}
	*/

	public String getTrackNameComparator() {
		return trackNameComparator;
	}

	public void setTrackNameComparator(String trackNameComparator) {
		this.trackNameComparator = trackNameComparator;
	}
	
	public UserDatabaseConfiguration getUserDatabase() {
		return userDatabase;
	}

	public void setUserDatabase(UserDatabaseConfiguration userDatabase) {
		this.userDatabase = userDatabase;
	}
	
	public String getUserAuthenticationClass() {
		return userAuthenticationClass;
	}

	public void setUserAuthenticationClass(String userAuthenticationClass) {
		this.userAuthenticationClass = userAuthenticationClass;
	}

	public Map<String, TrackConfiguration> getTracks() {
		return tracks;
	}

	public void setTracks(Map<String, TrackConfiguration> tracks) {
		this.tracks = tracks;
	}

	public String getCannedComments() {
		return cannedComments;
	}

	public void setCannedComments(String cannedComments) {
		this.cannedComments = cannedComments;
	}

	public Collection<SequenceSearchToolConfiguration> getSequenceSearchTools() {
		return sequenceSearchTools;
	}
	
	public void setSequenceSearchTool(Collection<SequenceSearchToolConfiguration> sequenceSearchTools) {
		this.sequenceSearchTools = sequenceSearchTools;
	}

	public Map<String, DataAdapterGroupConfiguration> getDataAdapters() {
		return dataAdapters;
	}
	
	public void setDataAdapters(Map<String, DataAdapterGroupConfiguration> dataAdapters) {
		this.dataAdapters = dataAdapters;
	}
	
	public boolean getUseCDS() {
		return useCDS;
	}

	public void setUseCDS(boolean useCDS) {
		this.useCDS = useCDS;
	}
	
	public boolean getUseMemoryStore() {
		return useMemoryStore;
	}

	public void setUseMemoryStore(boolean useMemoryStore) {
		this.useMemoryStore = useMemoryStore;
	}

	public Map<String, AnnotationInfoEditorConfiguration> getAnnotationInfoEditor() {
		return annotationInfoEditors;
	}
	
	public void setAnnotationInfoEditor(Map<String, AnnotationInfoEditorConfiguration> annotationInfoEditors) {
		this.annotationInfoEditors = annotationInfoEditors;
	}

	private void init(InputStream configuration) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(configuration);
		Node gbolMappingNode = doc.getElementsByTagName("gbol_mapping").item(0);
		if (gbolMappingNode != null) {
			gbolMappingFile = gbolMappingNode.getTextContent();
		}
		Node dataStoreDirectoryNode = doc.getElementsByTagName("datastore_directory").item(0);
		if (dataStoreDirectoryNode != null) {
			dataStoreDirectory = dataStoreDirectoryNode.getTextContent();
		}
		Node minimumIntronSizeNode = doc.getElementsByTagName("default_minimum_intron_size").item(0);
		if (minimumIntronSizeNode != null) {
			defaultMinimumIntronSize = Integer.parseInt(minimumIntronSizeNode.getTextContent());
		}
		Node historySizeNode = doc.getElementsByTagName("history_size").item(0);
		if (historySizeNode != null) {
			historySize = Integer.parseInt(historySizeNode.getTextContent());
		}
		Node overlapperClassNode = doc.getElementsByTagName("overlapper_class").item(0);
		if (overlapperClassNode != null) {
			overlapperClass = overlapperClassNode.getTextContent();
		}
		/*
		Node trackNameComparatorClassNode = doc.getElementsByTagName("track_name_comparator_class").item(0);
		if (trackNameComparatorClassNode != null) {
			trackNameComparatorClass = trackNameComparatorClassNode.getTextContent();
		}
		*/
		Node trackNameComparatorNode = doc.getElementsByTagName("track_name_comparator").item(0);
		if (trackNameComparatorNode != null) {
			trackNameComparator = trackNameComparatorNode.getTextContent();
		}
		Element userNode = (Element)doc.getElementsByTagName("user").item(0);
		if (userNode != null) {
			Element databaseNode = (Element)userNode.getElementsByTagName("database").item(0);
			if (databaseNode != null) {
				String driver = databaseNode.getElementsByTagName("driver").item(0).getTextContent();
				String url = databaseNode.getElementsByTagName("url").item(0).getTextContent();
				String userName = null;
				String password = null;
				Node userNameNode = databaseNode.getElementsByTagName("username").item(0);
				if (userNameNode != null) {
					userName = userNameNode.getTextContent();
				}
				Node passwordNode = databaseNode.getElementsByTagName("password").item(0);
				if (passwordNode != null) {
					password = passwordNode.getTextContent();
				}
				userDatabase = new UserDatabaseConfiguration(driver, url, userName, password);
			}
			Element authenticationClassNode = (Element)userNode.getElementsByTagName("authentication_class").item(0);
			if (authenticationClassNode != null) {
				userAuthenticationClass = authenticationClassNode.getTextContent();
			}
		}
		tracks = new HashMap<String, TrackConfiguration>();
		Element tracksNode = (Element)doc.getElementsByTagName("tracks").item(0);
		if (tracksNode != null) {
			NodeList tracksList = tracksNode.getElementsByTagName("track");
			if (tracksList != null) {
				for (int i = 0; i < tracksList.getLength(); ++i) {
					Element trackNode = (Element)tracksList.item(i);
					String name = null;
					Node nameNode = trackNode.getElementsByTagName("name").item(0);
					if (nameNode != null) {
						name = nameNode.getTextContent();
					}
					String organism = null;
					Node organismNode = trackNode.getElementsByTagName("organism").item(0);
					if (organismNode != null) {
						organism = organismNode.getTextContent();
					}
					
					String translationTable = null;
					Node translationTableNode = trackNode.getElementsByTagName("translation_table").item(0);
					if (translationTableNode != null) {
						translationTable = translationTableNode.getTextContent();
					}

					Node spliceSitesNode = tracksNode.getElementsByTagName("splice_sites").item(0);
					Set<String> spliceDonorSites = parseSpliceDonorSites((Element)spliceSitesNode);
					Set<String> spliceAcceptorSites = parseSpliceAcceptorSites((Element)spliceSitesNode);
					
					SourceFeatureConfiguration sourceFeature = null;
					Element sourceFeatureNode = (Element)trackNode.getElementsByTagName("source_feature").item(0);
					if (sourceFeatureNode != null) {
						String sequenceDirectory = sourceFeatureNode.getElementsByTagName("sequence_directory").item(0).getTextContent();
						int sequenceChunkSize = Integer.parseInt(sourceFeatureNode.getElementsByTagName("sequence_chunk_size").item(0).getTextContent());
						int sequenceLength = Integer.parseInt(sourceFeatureNode.getElementsByTagName("sequence_length").item(0).getTextContent());
						String uniqueName = sourceFeatureNode.getElementsByTagName("uniquename").item(0).getTextContent();
						String type = sourceFeatureNode.getElementsByTagName("type").item(0).getTextContent();
						int start = Integer.parseInt(sourceFeatureNode.getElementsByTagName("start").item(0).getTextContent());
						int end = Integer.parseInt(sourceFeatureNode.getElementsByTagName("end").item(0).getTextContent());
						String sequenceChunkPrefix = "";
						Node sequenceChunkPrefixNode = sourceFeatureNode.getElementsByTagName("sequence_chunk_prefix").item(0);
						if (sequenceChunkPrefixNode != null) {
							sequenceChunkPrefix = sequenceChunkPrefixNode.getTextContent();
						}
						sourceFeature = new SourceFeatureConfiguration(sequenceDirectory, sequenceChunkSize, sequenceChunkPrefix, sequenceLength, uniqueName, type, start, end);
					}
					tracks.put(name, new TrackConfiguration(name, organism, translationTable, sourceFeature, spliceDonorSites, spliceAcceptorSites));
				}
			}
			Node refSeqsNode = tracksNode.getElementsByTagName("refseqs").item(0);
			Node annotationTrackNameNode = tracksNode.getElementsByTagName("annotation_track_name").item(0);
			Node organismNode = tracksNode.getElementsByTagName("organism").item(0);
			Node sequenceTypeNode = tracksNode.getElementsByTagName("sequence_type").item(0);
			Node translationTableNode = tracksNode.getElementsByTagName("translation_table").item(0);
			Node spliceSitesNode = tracksNode.getElementsByTagName("splice_sites").item(0);
			if (refSeqsNode != null && annotationTrackNameNode != null && organismNode != null && sequenceTypeNode != null) {
				Set<String> spliceDonorSites = parseSpliceDonorSites((Element)spliceSitesNode);
				Set<String> spliceAcceptorSites = parseSpliceAcceptorSites((Element)spliceSitesNode);
				try {
					parseRefSeqs(refSeqsNode.getTextContent(), annotationTrackNameNode.getTextContent(), organismNode.getTextContent(), sequenceTypeNode.getTextContent(),
							translationTableNode != null ? translationTableNode.getTextContent() : null, spliceDonorSites, spliceAcceptorSites, tracks);
				}
				catch (Exception e) {
				    System.out.println("ERROR loading seq data:");
				    e.printStackTrace();
				}
			}
		}
		Element cannedCommentsNode = (Element)doc.getElementsByTagName("canned_comments").item(0);
		if (cannedCommentsNode != null) {
			cannedComments = cannedCommentsNode.getTextContent();
		}
		Element sequenceSearchToolsNode = (Element)doc.getElementsByTagName("sequence_search_tools").item(0);
		if (sequenceSearchToolsNode != null) {
			NodeList sequenceSearchToolsList = sequenceSearchToolsNode.getElementsByTagName("sequence_search_tool");
			for (int i = 0; i < sequenceSearchToolsList.getLength(); ++i) {
				Element sequenceSearchToolNode = (Element)sequenceSearchToolsList.item(i);
				String sequenceSearchToolKey = null;
				String sequenceSearchToolClass = null;
				String sequenceSearchToolConfig = null;
				Node sequenceSearchToolKeyNode = sequenceSearchToolNode.getElementsByTagName("key").item(0);
				if (sequenceSearchToolKeyNode != null) {
					sequenceSearchToolKey = sequenceSearchToolKeyNode.getTextContent();
				}
				Node sequenceSearchToolClassNode = sequenceSearchToolNode.getElementsByTagName("class").item(0);
				if (sequenceSearchToolClassNode != null) {
					sequenceSearchToolClass = sequenceSearchToolClassNode.getTextContent();
				}
				Node sequenceSearchToolConfigNode = sequenceSearchToolNode.getElementsByTagName("config").item(0);
				if (sequenceSearchToolConfigNode != null) {
					sequenceSearchToolConfig = sequenceSearchToolConfigNode.getTextContent();
				}
				if (sequenceSearchToolKey != null && sequenceSearchToolClass != null && sequenceSearchToolConfig != null) {
					sequenceSearchTools.add(new SequenceSearchToolConfiguration(sequenceSearchToolKey, sequenceSearchToolClass, sequenceSearchToolConfig));
				}
			}
		}
		Element dataAdaptersNode = (Element)doc.getElementsByTagName("data_adapters").item(0);
		if (dataAdaptersNode != null) {
//			NodeList dataAdaptersList = dataAdaptersNode.getElementsByTagName("data_adapter");
			NodeList dataAdaptersList = dataAdaptersNode.getChildNodes();
			for (int i = 0; i < dataAdaptersList.getLength(); ++i) {
				Node node = dataAdaptersList.item(i);
				if (!(node instanceof Element)) {
					continue;
				}
				Element dataAdapterNode = (Element)node;
				if (dataAdapterNode.getTagName().equals("data_adapter")) {
					DataAdapterConfiguration conf = processDataAdapterConfiguration(dataAdapterNode);
					DataAdapterGroupConfiguration dataAdapterGroup = dataAdapters.get(conf.getKey());
					if (dataAdapterGroup == null) {
						dataAdapterGroup = new DataAdapterGroupConfiguration(null, false, conf.getPermission());
						dataAdapters.put(conf.getKey(), dataAdapterGroup);
					}
					dataAdapterGroup.addDataAdapter(conf);
				}
				else if (dataAdapterNode.getTagName().equals("data_adapter_group")) {
					Node key = dataAdapterNode.getElementsByTagName("key").item(0);
					Node permission = dataAdapterNode.getElementsByTagName("permission").item(0);
					if (key == null || permission == null) {
						continue;
					}
					NodeList dataAdapterGroupList = dataAdapterNode.getElementsByTagName("data_adapter");
					if (dataAdapterGroupList == null) {
						continue;
					}
					DataAdapterGroupConfiguration dataAdapterGroup = dataAdapters.get(key.getTextContent());
					if (dataAdapterGroup == null) {
						dataAdapterGroup = new DataAdapterGroupConfiguration(key.getTextContent(), true, permission.getTextContent());
						dataAdapters.put(key.getTextContent(), dataAdapterGroup);
					}
					for (int j = 0; j < dataAdapterGroupList.getLength(); ++j) {
						dataAdapterGroup.addDataAdapter(processDataAdapterConfiguration((Element)dataAdapterGroupList.item(j)));
					}
				}
			}
		}
		Element useCDSNode = (Element)doc.getElementsByTagName("use_cds_for_new_transcripts").item(0);
		if (useCDSNode != null) {
			useCDS = Boolean.parseBoolean(useCDSNode.getTextContent());
		}
		Element useMemoryStoreNode = (Element)doc.getElementsByTagName("use_pure_memory_store").item(0);
		if (useMemoryStoreNode != null) {
			useMemoryStore = Boolean.parseBoolean(useMemoryStoreNode.getTextContent());
		}
		Element annotationInfoEditorNode = (Element)doc.getElementsByTagName("annotation_info_editor").item(0);
		if (annotationInfoEditorNode != null) {
			NodeList annotationInfoEditorGroups = annotationInfoEditorNode.getElementsByTagName("annotation_info_editor_group");
			for (int i = 0; i < annotationInfoEditorGroups.getLength(); ++i) {
				AnnotationInfoEditorConfiguration annotationInfoEditor = new AnnotationInfoEditorConfiguration();
//				annotationInfoEditors.add(annotationInfoEditor);
				Element annotationInfoEditorGroupNode = (Element)annotationInfoEditorGroups.item(i);
				Node statusNode = annotationInfoEditorGroupNode.getElementsByTagName("status").item(0);
				if (statusNode != null) {
					/*
					NodeList statusList = statusNode.getChildNodes();
					for (int i = 0; i < statusList.getLength(); ++i) {
						if (statusList.item(i) instanceof Element) {
							annotationInfoEditor.addStatus(statusList.item(i).getNodeName());
						}
					}
					*/
					NodeList statusList = ((Element)statusNode).getElementsByTagName("status_flag");
					for (int j = 0; j < statusList.getLength(); ++j) {
						annotationInfoEditor.addStatus(statusList.item(j).getTextContent());
					}
					/*
					for (String featureType : ((Element)statusNode).getAttribute("feature_type").split(",")) {
						annotationInfoEditor.addFeatureType("status", featureType);
					}
					*/
				}
				Node attributesNode = annotationInfoEditorGroupNode.getElementsByTagName("attributes").item(0);
				if (attributesNode != null) {
					annotationInfoEditor.setHasAttributes(true);
					/*
					for (String featureType : ((Element)attributesNode).getAttribute("feature_type").split(",")) {
						annotationInfoEditor.addFeatureType("attributes", featureType);
					}
					*/
				}
				Node dbxrefsNode = annotationInfoEditorGroupNode.getElementsByTagName("dbxrefs").item(0);
				if (dbxrefsNode != null) {
					annotationInfoEditor.setHasDbxrefs(true);
					/*
					for (String featureType : ((Element)dbxrefsNode).getAttribute("feature_type").split(",")) {
						annotationInfoEditor.addFeatureType("dbxrefs", featureType);
					}
					*/
				}
				Node pubmedIdsNode = annotationInfoEditorGroupNode.getElementsByTagName("pubmed_ids").item(0);
				if (pubmedIdsNode != null) {
					annotationInfoEditor.setHasPubmedIds(true);
					/*
					for (String featureType : ((Element)pubmedIdsNode).getAttribute("feature_type").split(",")) {
						annotationInfoEditor.addFeatureType("pubmedIds", featureType);
					}
					*/
				}
				Node goIdsNode = annotationInfoEditorGroupNode.getElementsByTagName("go_ids").item(0);
				if (goIdsNode != null) {
					annotationInfoEditor.setHasGoIds(true);
					/*
					for (String featureType : ((Element)goIdsNode).getAttribute("feature_type").split(",")) {
						annotationInfoEditor.addFeatureType("goIds", featureType);
					}
					*/
				}
				Node commentsNode = annotationInfoEditorGroupNode.getElementsByTagName("comments").item(0);
				if (commentsNode != null) {
					annotationInfoEditor.setHasComments(true);
					/*
					for (String featureType : ((Element)commentsNode).getAttribute("feature_type").split(",")) {
						annotationInfoEditor.addFeatureType("comments", featureType);
					}
					*/
				}
				if (annotationInfoEditorGroupNode.hasAttribute("feature_types")) {
					annotationInfoEditor.addFeatureTypes(annotationInfoEditorGroupNode.getAttribute("feature_types").split(","));
					for (String type : annotationInfoEditorGroupNode.getAttribute("feature_types").split(",")) {
						annotationInfoEditors.put(type, annotationInfoEditor);
					}
				}
				else {
					annotationInfoEditor.addFeatureTypes("default");
					annotationInfoEditors.put("default", annotationInfoEditor);
				}
			}
		}
	}
	
	private void parseRefSeqs(String refSeqsFileName, String annotationTrackName, String organism, String sequenceType, String translationTable, Set<String> spliceDonorSites, Set<String> spliceAcceptorSites, Map<String, TrackConfiguration> tracks) throws FileNotFoundException, IOException, JSONException {
		File refSeqsFile = new File(refSeqsFileName);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(refSeqsFile));
		JSONArray refSeqs = convertJBrowseJSON(in);
		for (int i = 0; i < refSeqs.length(); ++i) {
			JSONObject refSeq = refSeqs.getJSONObject(i);
			int length = refSeq.getInt("length");
			String name = refSeq.getString("name");
			String seqDir;
			String seqChunkPrefix = "";
			if (refSeq.has("seqDir")) {
				seqDir = refSeqsFile.getParent() + "/" + refSeq.getString("seqDir");
			}
			else {
				CRC32 crc = new CRC32();
				crc.update(name.getBytes());
				String hex = String.format("%08x", crc.getValue());
				String []dirs = splitStringByNumberOfCharacters(hex, 3);
				seqDir = String.format("%s/%s/%s/%s", refSeqsFile.getParent(), dirs[0], dirs[1], dirs[2]);
				seqChunkPrefix = name + "-";
			}
			int seqChunkSize = refSeq.getInt("seqChunkSize");
			int start = refSeq.getInt("start");
			int end = refSeq.getInt("end");
			SourceFeatureConfiguration sourceFeature = new SourceFeatureConfiguration(seqDir, seqChunkSize, seqChunkPrefix, length, name, sequenceType, start, end);
			
			TrackConfiguration c = new TrackConfiguration(annotationTrackName + "-" + name, organism, translationTable, sourceFeature, spliceDonorSites, spliceAcceptorSites);
			tracks.put(name, c);
//			tracks.put(name, new TrackConfiguration(annotationTrackName + "-" + name, organism, translationTable, sourceFeature));
		}
		in.close();
	}
	
	private String[] splitStringByNumberOfCharacters(String str, int numOfChars) {
		int numTokens = str.length() / numOfChars;
		if (str.length() % numOfChars != 0) {
			++numTokens;
		}
		String []tokens = new String[numTokens];
		int idx = 0;
		for (int i = 0; i < numTokens; ++i, idx += numOfChars) {
			tokens[i] = str.substring(idx, idx + numOfChars < str.length() ? idx + numOfChars : str.length());
		}
		return tokens;
	}
	
	private JSONArray convertJBrowseJSON(InputStream inputStream) throws IOException, JSONException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder buffer = new StringBuilder();
		String line;
//		reader.readLine();
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		return new JSONArray(buffer.toString());
	}
	
	private Set<String> parseSpliceDonorSites(Element spliceSitesNode) {
		Set<String> donorSites = new HashSet<String>();
		if (spliceSitesNode != null) {
			NodeList donorSiteList = spliceSitesNode.getElementsByTagName("donor_site");
			for (int i = 0; i < donorSiteList.getLength(); ++i) {
				donorSites.add(donorSiteList.item(i).getTextContent());
			}
		}
		else {
			donorSites.add("GT");
		}
		return donorSites;
	}

	private Set<String> parseSpliceAcceptorSites(Element spliceSitesNode) {
		Set<String> acceptorSites = new HashSet<String>();
		if (spliceSitesNode != null) {
			NodeList acceptorSiteList = spliceSitesNode.getElementsByTagName("acceptor_site");
			for (int i = 0; i < acceptorSiteList.getLength(); ++i) {
				acceptorSites.add(acceptorSiteList.item(i).getTextContent());
			}
		}
		else {
			acceptorSites.add("AG");
		}
		return acceptorSites;
	}
	
	private DataAdapterConfiguration processDataAdapterConfiguration(Element dataAdapterNode) {
		String dataAdapterKey = null;
		String dataAdapterClass = null;
		String dataAdapterPermission = null;
		String dataAdapterConfig = null;
		String dataAdapterOptions = null;
		Node dataAdapterKeyNode = dataAdapterNode.getElementsByTagName("key").item(0);
		if (dataAdapterKeyNode != null) {
			dataAdapterKey = dataAdapterKeyNode.getTextContent();
		}
		Node dataAdapterClassNode = dataAdapterNode.getElementsByTagName("class").item(0);
		if (dataAdapterClassNode != null) {
			dataAdapterClass = dataAdapterClassNode.getTextContent();
		}
		Node dataAdapterPermissionNode = dataAdapterNode.getElementsByTagName("permission").item(0);
		if (dataAdapterPermissionNode != null) {
			dataAdapterPermission = dataAdapterPermissionNode.getTextContent();
		}
		Node dataAdapterConfigNode = dataAdapterNode.getElementsByTagName("config").item(0);
		if (dataAdapterConfigNode != null) {
			dataAdapterConfig = dataAdapterConfigNode.getTextContent();
		}
		Node dataAdapterOptionsNode = dataAdapterNode.getElementsByTagName("options").item(0);
		if (dataAdapterOptionsNode != null) {
			dataAdapterOptions = dataAdapterOptionsNode.getTextContent();
		}
		return new DataAdapterConfiguration(dataAdapterKey, dataAdapterClass, dataAdapterPermission, dataAdapterConfig, dataAdapterOptions);
	}

	public class SourceFeatureConfiguration {
		
		private String sequenceDirectory;
		private int sequenceChunkSize;
		private String sequenceChunkPrefix;
		private int sequenceLength;
		private String uniqueName;
		private String type;
		private int start;
		private int end;

		public SourceFeatureConfiguration(String sequenceDirectory, int sequenceChunkSize, String sequenceChunkPrefix, int sequenceLength, String uniqueName, String type, int start, int end) {
			this.sequenceDirectory = sequenceDirectory;
			this.sequenceChunkSize = sequenceChunkSize;
			this.sequenceChunkPrefix = sequenceChunkPrefix;
			this.sequenceLength = sequenceLength;
			this.uniqueName = uniqueName;
			this.type = type;
			this.start = start;
			this.end = end;
		}

		public String getSequenceDirectory() {
			return sequenceDirectory;
		}
		
		public int getSequenceChunkSize() {
			return sequenceChunkSize;
		}
		
		public String getSequenceChunkPrefix() {
			return sequenceChunkPrefix;
		}
		
		public int getSequenceLength() {
			return sequenceLength;
		}
		
		public String getUniqueName() {
			return uniqueName;
		}

		public String getType() {
			return type;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}

	}
	
	public class UserDatabaseConfiguration {
		
		private String driver;
		private String url;
		private String userName;
		private String password;

		public UserDatabaseConfiguration(String driver, String url, String userName, String password) {
			this.driver = driver;
			this.url = url;
			this.userName = userName;
			this.password = password;
		}

		public String getDriver() {
			return driver;
		}

		public String getURL() {
			return url;
		}

		public String getUserName() {
			return userName;
		}

		public String getPassword() {
			return password;
		}
		
	}
	
	public class TrackConfiguration {
		
		private String name;
		private String organism;
		private SourceFeatureConfiguration sourceFeature;
		private String translationTable;
		private Set<String> spliceDonorSites;
		private Set<String> spliceAcceptorSites;

		public TrackConfiguration(String name, String organism, String translationTable, SourceFeatureConfiguration sourceFeature, Set<String> spliceDonorSites, Set<String> spliceAcceptorSites) {
			this.name = name;
			this.organism = organism;
			this.sourceFeature = sourceFeature;
			this.translationTable = translationTable;
			this.spliceDonorSites = spliceDonorSites;
			this.spliceAcceptorSites = spliceAcceptorSites;
		}

		public String getName() {
			return name;
		}

		public String getOrganism() {
			return organism;
		}

		public SourceFeatureConfiguration getSourceFeature() {
			return sourceFeature;
		}
		
		public String getTranslationTable() {
			return translationTable;
		}
		
		public Set<String> getSpliceDonorSites() {
			return spliceDonorSites;
		}
		
		public Set<String> getSpliceAcceptorSites() {
			return spliceAcceptorSites;
		}

	}
	
	public class SequenceSearchToolConfiguration {
		
		private String key;
		private String className;
		private String configFileName;
		
		public SequenceSearchToolConfiguration(String key, String className, String configFile) {
			this.key = key;
			this.className = className;
			this.configFileName = configFile;
		}
		
		public String getKey() {
			return key;
		}
		
		public String getClassName() {
			return className;
		}
		
		public String getConfigFilename() {
			return configFileName;
		}
	}
	
	public class DataAdapterGroupConfiguration {
		
		private Collection<DataAdapterConfiguration> dataAdapters;
		private boolean isGroup;
		private String key;
		private String permission;
		
		public DataAdapterGroupConfiguration(String key, boolean isGroup, String permission) {
			this.isGroup = isGroup;
			this.key = key;
			this.permission = permission;
			dataAdapters = new ArrayList<DataAdapterConfiguration>();
		}
		
		public Collection<DataAdapterConfiguration> getDataAdapters() {
			return dataAdapters;
		}
		
		public boolean isGroup() {
			return isGroup;
		}
		
		public String getKey() {
			return key;
		}
		
		public String getPermission() {
			return permission;
		}
		
		public void addDataAdapter(DataAdapterConfiguration dataAdapter) {
			dataAdapters.add(dataAdapter);
		}
		
	}
	
	public class DataAdapterConfiguration {
		
		private String key;
		private String className;
		private String permission;
		private String configFileName;
		private String options;
		
		public DataAdapterConfiguration(String key, String className, String permission, String configFile, String options) {
			this.key = key;
			this.className = className;
			this.permission = permission;
			this.configFileName = configFile;
			this.options = options;
		}
		
		public String getKey() {
			return key;
		}
		
		public String getClassName() {
			return className;
		}
		
		public String getPermission() {
			return permission;
		}
		
		public String getConfigFileName() {
			return configFileName;
		}

		public String getOptions() {
			return options;
		}

	}
	
	public class AnnotationInfoEditorConfiguration {
		
		private List<String> status;
		private boolean hasDbxrefs;
		private boolean hasAttributes;
		private boolean hasPubmedIds;
		private boolean hasGoIds;
		private boolean hasComments;
//		private Map<String, Set<String>> supportedFeatureTypes;
		private Collection<String> supportedFeatureTypes;
		
		public AnnotationInfoEditorConfiguration() {
			status = new ArrayList<String>();
//			supportedFeatureTypes = new HashMap<String, Set<String>>();
			supportedFeatureTypes = new ArrayList<String>();
		}
		
		public List<String> getStatus() {
			return status;
		}
		
		public void addStatus(String status) {
			this.status.add(status);
		}
		
		public boolean hasStatus() {
			return status.size() > 0;
		}
		
		public void setHasDbxrefs(boolean hasDbxrefs) {
			this.hasDbxrefs = hasDbxrefs;
		}
		
		public boolean hasDbxrefs() {
			return hasDbxrefs;
		}

		public void setHasAttributes(boolean hasAttributes) {
			this.hasAttributes = hasAttributes;
		}
		
		public boolean hasAttributes() {
			return hasAttributes;
		}

		public void setHasPubmedIds(boolean hasPubmedIds) {
			this.hasPubmedIds = hasPubmedIds;
		}
		
		public boolean hasPubmedIds() {
			return hasPubmedIds;
		}

		public void setHasGoIds(boolean hasGoIds) {
			this.hasGoIds = hasGoIds;
		}
		
		public boolean hasGoIds() {
			return hasGoIds;
		}

		public void setHasComments(boolean hasComments) {
			this.hasComments = hasComments;
		}
		
		public boolean hasComments() {
			return hasComments;
		}
		
		public void addFeatureTypes(String ... types) {
			for (String type : types) {
				supportedFeatureTypes.add(type);
			}
		}
		
		public Collection<String> getSupportedFeatureTypes() {
			return supportedFeatureTypes;
		}
		
		/*
		public void addFeatureType(String type, String featureType) {
			Set<String> featureTypes = supportedFeatureTypes.get(type);
			if (featureTypes == null) {
				featureTypes = new HashSet<String>();
				supportedFeatureTypes.put(type, featureTypes);
			}
			featureTypes.add(featureType);
		}

		public boolean isSupported(String type, String featureType) {
			Set<String> featureTypes = supportedFeatureTypes.get(type);
			return featureTypes == null ? true : featureTypes.contains(featureType);
		}
		*/
		
	}
	
}
