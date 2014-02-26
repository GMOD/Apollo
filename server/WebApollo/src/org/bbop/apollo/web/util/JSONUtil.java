package org.bbop.apollo.web.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;

import org.bbop.apollo.web.name.AbstractNameAdapter;
import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.DB;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureDBXref;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureProperty;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtil {
	
	public static JSONObject convertBioFeatureToJSON(AbstractBioFeature feature) {
		SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
		return convertFeatureToJSON((Feature)iterator.next());
	}

	public static Feature convertJSONToFeature(JSONObject jsonFeature, BioObjectConfiguration configuration, Organism organism) {
		return convertJSONToFeature(jsonFeature, configuration, organism, null);
	}

	public static Feature convertJSONToFeature(JSONObject jsonFeature, BioObjectConfiguration configuration, Feature sourceFeature) {
		return convertJSONToFeature(jsonFeature, configuration, sourceFeature != null ? sourceFeature.getOrganism() : null, sourceFeature);
	}

	public static Feature convertJSONToFeature(JSONObject jsonFeature, BioObjectConfiguration configuration, Feature sourceFeature, AbstractNameAdapter nameAdapter) {
		return convertJSONToFeature(jsonFeature, configuration, sourceFeature != null ? sourceFeature.getOrganism() : null, sourceFeature, nameAdapter);
	}

	public static Feature convertJSONToFeature(JSONObject jsonFeature, BioObjectConfiguration configuration, Organism organism, Feature sourceFeature) {
		return convertJSONToFeature(jsonFeature, configuration, organism, sourceFeature, null);
	}
	
	public static Feature convertJSONToFeature(JSONObject jsonFeature, BioObjectConfiguration configuration, Organism organism, Feature sourceFeature, AbstractNameAdapter nameAdapter) {
		Feature gsolFeature = new Feature();
		try {
			gsolFeature.setOrganism(organism);
			JSONObject type = jsonFeature.getJSONObject("type");
			gsolFeature.setType(convertJSONToCVTerm(type));
			if (jsonFeature.has("uniquename")) {
				gsolFeature.setUniqueName(jsonFeature.getString("uniquename"));
			}
			else if (nameAdapter != null) {
				gsolFeature.setUniqueName(nameAdapter.generateUniqueName());
			}
			if (jsonFeature.has("name")) {
				gsolFeature.setName(jsonFeature.getString("name"));
			}
			if (jsonFeature.has("residues")) {
				gsolFeature.setResidues(jsonFeature.getString("residues"));
			}
			if (jsonFeature.has("location")) {
				JSONObject jsonLocation = jsonFeature.getJSONObject("location");
				gsolFeature.addFeatureLocation(convertJSONToFeatureLocation(jsonLocation, sourceFeature));
			}
			if (jsonFeature.has("children")) {
				JSONArray children = jsonFeature.getJSONArray("children");
				for (int i = 0; i < children.length(); ++i) {
					Feature child = convertJSONToFeature(children.getJSONObject(i), configuration, sourceFeature, nameAdapter);
					FeatureRelationship fr = new FeatureRelationship();
					fr.setObjectFeature(gsolFeature);
					fr.setSubjectFeature(child);
					fr.setType(configuration.getDefaultCVTermForClass("PartOf"));
					child.getParentFeatureRelationships().add(fr);
					gsolFeature.getChildFeatureRelationships().add(fr);
				}
			}
			if (jsonFeature.has("timeaccessioned")) {
				gsolFeature.setTimeAccessioned(new Date(jsonFeature.getInt("timeaccessioned")));
			}
			else {
				gsolFeature.setTimeAccessioned(new Date());
			}
			if (jsonFeature.has("timelastmodified")) {
				gsolFeature.setTimeLastModified(new Date(jsonFeature.getInt("timelastmodified")));
			}
			else {
				gsolFeature.setTimeLastModified(new Date());
			}
			if (jsonFeature.has("properties")) {
				JSONArray properties = jsonFeature.getJSONArray("properties");
				for (int i = 0; i < properties.length(); ++i) { 
					JSONObject property = properties.getJSONObject(i);
					JSONObject propertyType = property.getJSONObject("type");
					FeatureProperty gsolProperty = new FeatureProperty();
					gsolProperty.setType(new CVTerm(propertyType.getString("name"), new CV(propertyType.getJSONObject("cv").getString("name"))));
					gsolProperty.setValue(property.getString("value"));
					gsolProperty.setFeature(gsolFeature);
					int rank = 0;
					for (FeatureProperty fp : gsolFeature.getFeatureProperties()) {
						if (fp.getType().equals(gsolProperty.getType())) {
							if (fp.getRank() > rank) {
								rank = fp.getRank();
							}
						}
					}
					gsolProperty.setRank(rank + 1);
					gsolFeature.getFeatureProperties().add(gsolProperty);
				}
			}
			if (jsonFeature.has("dbxrefs")) {
				JSONArray dbxrefs = jsonFeature.getJSONArray("dbxrefs");
				for (int i = 0; i < dbxrefs.length(); ++i) {
					JSONObject dbxref = dbxrefs.getJSONObject(i);
					JSONObject db = dbxref.getJSONObject("db");
					gsolFeature.addFeatureDBXref(new DB(db.getString("name")), dbxref.getString("accession"));
				}
			}
		}
		catch (JSONException e) {
			return null;
		}
		return gsolFeature;
	}
	
	public static FeatureProperty convertJSONToFeatureProperty(JSONObject jsonFeatureProperty) {
		FeatureProperty gsolFeatureProperty = new FeatureProperty();
		try {
			gsolFeatureProperty.setType(convertJSONToCVTerm(jsonFeatureProperty.getJSONObject("type")));
			gsolFeatureProperty.setValue(jsonFeatureProperty.getString("value"));
		}
		catch (JSONException e) {
			return null;
		}
		return gsolFeatureProperty;
	}
	
	public static JSONObject convertFeatureToJSON(Feature gsolFeature) {
		return convertFeatureToJSON(gsolFeature, true);
	}
	
	public static JSONObject convertFeatureToJSON(Feature gsolFeature, boolean includeSequence) {
		JSONObject jsonFeature = new JSONObject();
		try {
			jsonFeature.put("type", convertCVTermToJSON(gsolFeature.getType()));
			jsonFeature.put("uniquename", gsolFeature.getUniqueName());
			if (gsolFeature.getName() != null) {
				jsonFeature.put("name", gsolFeature.getName());
			}
			Collection<FeatureRelationship> childrenRelationships = gsolFeature.getChildFeatureRelationships();
			if (childrenRelationships.size() > 0) {
				JSONArray children = new JSONArray();
				jsonFeature.put("children", children);
				for (FeatureRelationship fr : childrenRelationships) {
					children.put(convertFeatureToJSON(fr.getSubjectFeature()));
				}
			}
			Collection<FeatureRelationship> parentRelationships = gsolFeature.getParentFeatureRelationships();
			if (parentRelationships.size() == 1) {
				Feature parent = parentRelationships.iterator().next().getObjectFeature();
				jsonFeature.put("parent_id", parent.getUniqueName());
				jsonFeature.put("parent_type", JSONUtil.convertCVTermToJSON(parent.getType()));
			}
			Collection<FeatureLocation> featureLocations = gsolFeature.getFeatureLocations();
			if (featureLocations.size() > 0) {
				FeatureLocation gsolFeatureLocation = featureLocations.iterator().next();
				if (gsolFeatureLocation != null) {
					jsonFeature.put("location", convertFeatureLocationToJSON(gsolFeatureLocation));
				}
			}
			if (includeSequence && gsolFeature.getResidues() != null) {
				jsonFeature.put("residues", gsolFeature.getResidues());
			}
			Collection<FeatureProperty> gsolFeatureProperties = gsolFeature.getFeatureProperties();
			if (gsolFeatureProperties.size() > 0) {
				JSONArray properties = new JSONArray();
				jsonFeature.put("properties", properties);
				for (FeatureProperty property : gsolFeatureProperties) {
					JSONObject jsonProperty = new JSONObject();
					jsonProperty.put("type", convertCVTermToJSON(property.getType()));
					jsonProperty.put("value", property.getValue());
					properties.put(jsonProperty);
				}
			}
			Collection<FeatureDBXref> gsolFeatureDbxrefs = gsolFeature.getFeatureDBXrefs();
			if (gsolFeatureDbxrefs.size() > 0) {
				JSONArray dbxrefs = new JSONArray();
				jsonFeature.put("dbxrefs", dbxrefs);
				for (FeatureDBXref gsolDbxref : gsolFeatureDbxrefs) {
					JSONObject dbxref = new JSONObject();
					dbxref.put("accession", gsolDbxref.getDbxref().getAccession());
					dbxref.put("db", new JSONObject().put("name", gsolDbxref.getDbxref().getDb().getName()));
					dbxrefs.put(dbxref);
				}
			}
			Date timeLastModified = gsolFeature.getTimeLastModified() != null ? gsolFeature.getTimeLastModified() : gsolFeature.getTimeAccessioned();
			if (timeLastModified != null) {
				jsonFeature.put("date_last_modified", timeLastModified.getTime());
			}
		}
		catch (JSONException e) {
			return null;
		}
		return jsonFeature;
	}
	
	public static JSONObject convertCVTermToJSON(CVTerm gsolCVTerm) throws JSONException {
		JSONObject jsonCVTerm = new JSONObject();
		JSONObject jsonCV = new JSONObject();
		jsonCVTerm.put("cv", jsonCV);
		jsonCV.put("name", gsolCVTerm.getCv().getName());
		jsonCVTerm.put("name", gsolCVTerm.getName());
		return jsonCVTerm;
	}
	
	public static JSONObject convertCVTermToJSON(String cvterm) throws JSONException {
		String[] tokens = cvterm.split(":");
		return convertCVTermToJSON(new CVTerm(tokens[1], new CV(tokens[0])));
	}
	
	public static CVTerm convertJSONToCVTerm(JSONObject jsonCVTerm) throws JSONException {
		return new CVTerm(jsonCVTerm.getString("name"), new CV(jsonCVTerm.getJSONObject("cv").getString("name")));
	}
	
	public static FeatureLocation convertJSONToFeatureLocation(JSONObject jsonLocation, Feature sourceFeature) throws JSONException {
		FeatureLocation gsolLocation = new FeatureLocation();
		gsolLocation.setFmin(jsonLocation.getInt("fmin"));
		gsolLocation.setFmax(jsonLocation.getInt("fmax"));
		gsolLocation.setStrand(jsonLocation.getInt("strand"));
		gsolLocation.setSourceFeature(sourceFeature);
		return gsolLocation;
	}
	
	public static JSONObject convertFeatureLocationToJSON(FeatureLocation gsolFeatureLocation) throws JSONException {
		JSONObject jsonFeatureLocation = new JSONObject();
		jsonFeatureLocation.put("fmin", gsolFeatureLocation.getFmin());
		jsonFeatureLocation.put("fmax", gsolFeatureLocation.getFmax());
		if (gsolFeatureLocation.isIsFminPartial()) {
			jsonFeatureLocation.put("is_fmin_partial", true);
		}
		if (gsolFeatureLocation.isIsFmaxPartial()) {
			jsonFeatureLocation.put("is_fmax_partial", true);
		}
		jsonFeatureLocation.put("strand", gsolFeatureLocation.getStrand());
		return jsonFeatureLocation;
	}
	
	public static JSONObject convertMatchToJSON(Match match) throws JSONException {
		JSONObject jsonMatch = new JSONObject();
		jsonMatch.put("rawscore", match.getRawScore());
		jsonMatch.put("normscore", match.getNormalizedScore());
		jsonMatch.put("significance", match.getSignificance());
		jsonMatch.put("identity", match.getIdentity());
		FeatureLocation queryLoc = match.getQueryFeatureLocation();
		if (queryLoc != null) {
			JSONObject jsonQuery = convertFeatureToJSON(queryLoc.getSourceFeature());
			JSONObject jsonQueryLoc = convertFeatureLocationToJSON(queryLoc);
			JSONObject query = new JSONObject();
			query.put("location", jsonQueryLoc);
			query.put("feature", jsonQuery);
			jsonMatch.put("query", query);
		}
		FeatureLocation subjectLoc = match.getSubjectFeatureLocation();
		if (subjectLoc != null) {
			JSONObject jsonSubject = convertFeatureToJSON(subjectLoc.getSourceFeature());
			JSONObject jsonSubjectLoc = convertFeatureLocationToJSON(subjectLoc);
			JSONObject subject = new JSONObject();
			subject.put("location", jsonSubjectLoc);
			subject.put("feature", jsonSubject);
			jsonMatch.put("subject", subject);
		}
		return jsonMatch;
	}
	
	public static JSONObject convertInputStreamToJSON(InputStream inputStream) throws IOException, JSONException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder buffer = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		return new JSONObject(buffer.toString());
	}

}
