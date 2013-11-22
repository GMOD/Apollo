package org.bbop.apollo.web.dataadapter.chado;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.dataadapter.DataAdapter;
import org.bbop.apollo.web.user.UserManager;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Servlet implementation class ChadoIOService
 */
public class ChadoDataAdapter extends DataAdapter {
	private String dataStoreDirectory;
	private Map<String, String> trackToOrganism;
	private Map<String, ServerConfiguration.SourceFeatureConfiguration> trackToSourceFeature;
	private String hibernateConfig;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ChadoDataAdapter() {
        trackToOrganism = new HashMap<String, String>();
        trackToSourceFeature = new HashMap<String, ServerConfiguration.SourceFeatureConfiguration>();
    }

    @Override
	public void init(ServerConfiguration serverConfiguration, String configPath, String basePath) throws DataAdapterException {
		try {
			Document doc = getXMLDocument(basePath, configPath);
			Node hibernateConfigNode = doc.getElementsByTagName("hibernate_config").item(0);
			if (hibernateConfigNode == null) {
				throw new DataAdapterException("Configuration missing required 'hibernate_config' element");
			}
			hibernateConfig = basePath + "/" + hibernateConfigNode.getTextContent();
			dataStoreDirectory = serverConfiguration.getDataStoreDirectory();
			for (ServerConfiguration.TrackConfiguration track : serverConfiguration.getTracks().values()) {
				trackToOrganism.put(track.getName(), track.getOrganism());
				trackToSourceFeature.put(track.getName(), track.getSourceFeature());
				if (!UserManager.getInstance().isInitialized()) {
					ServerConfiguration.UserDatabaseConfiguration userDatabase = serverConfiguration.getUserDatabase();
					UserManager.getInstance().initialize(userDatabase.getDriver(), userDatabase.getURL(), userDatabase.getUserName(), userDatabase.getPassword());
				}
			}
		} catch (Exception e) {
			throw new DataAdapterException(e.getMessage());
		}
	}

    @Override
	public void write(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException {
		execute(tracks, parameters, response, false);
    }
	
	private void printFeature(Feature feature, PrintWriter out, boolean isTopLevel) {
		if (isTopLevel) {
			out.println("<table>");
		}
		FeatureLocation loc = feature.getFeatureLocations().iterator().next();
		out.println("<tr>");
		out.println("<td class=\"uniquename\">" + feature.getUniqueName() + "</td>");
		out.println("<td>" + feature.getType() + "</td>");
		out.println("<td>" + loc.getFmin() + "</td>");
		out.println("<td>" + loc.getFmax() + "</td>");
		out.println("<td>" + loc.getStrand() + "</td>");
		out.println("</tr>");
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			printFeature(fr.getSubjectFeature(), out, false);
		}
		if (isTopLevel) {
			out.println("</table>");
		}
	}
	
	private Feature getSourceFeature(String track, ChadoJEDatabaseIO chadoIO) {
		ServerConfiguration.SourceFeatureConfiguration sourceFeature = trackToSourceFeature.get(track);
        String [] type = sourceFeature.getType().split(":");
		return chadoIO.getFeature(new CVTerm(type[1], new CV(type[0])), sourceFeature.getUniqueName());
	}
	
	public class ChadoIOServiceException extends Exception {
		
		private static final long serialVersionUID = 1L;

		public ChadoIOServiceException(String message) {
			super(message);
		}
		
	}

	@Override
	public void read(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException {
		execute(tracks, parameters, response, true);
	}

	private void execute(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response, boolean read) throws IOException {
		try {
			String displayFeaturesParameter = getParameter(parameters, "display_features");
			boolean displayFeatures = displayFeaturesParameter != null ? Boolean.parseBoolean(displayFeaturesParameter) : true;
			PrintWriter out = response.getWriter();
            response.setContentType("text/html");
			out.println("<html>");
			if (displayFeatures) {
				out.println("<head><link rel=\"stylesheet\" type=\"text/css\" href=\"styles/chado.css\" /></head>");
			}
			out.println("<body>");
			for (String track : tracks) {
				File dataPath = new File(dataStoreDirectory + "/" + track);
				if (!dataPath.exists()) {
					continue;
				}
				ChadoJEDatabaseIO chadoIO = new ChadoJEDatabaseIO(dataStoreDirectory + "/" + track, hibernateConfig, false);
				String [] organism = trackToOrganism.get(track).split("\\s+");
				chadoIO.setOrganism(organism[0], organism[1]);
				Feature sourceFeature = getSourceFeature(track, chadoIO);
				if (!read) {
					chadoIO.writeFeatures(sourceFeature);
				}
				if (displayFeatures) {
					out.println("<div>" + track + "</div>");
					for (Iterator<? extends Feature> iter = chadoIO.getFeatures(sourceFeature, true); iter.hasNext();) {
						Feature feature = iter.next();
						if (feature.getParentFeatureRelationships().size() == 0) {
							printFeature(feature, out, true);
						}
					}
				}
				chadoIO.close();
			}
			if (!displayFeatures) {
				out.println("Done writing to Chado");
			}
			out.println("</body>");
			out.println("</html>");
		}
    	catch (Exception e) {
    		e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error with operation to database");
    	}
	}
	
}
