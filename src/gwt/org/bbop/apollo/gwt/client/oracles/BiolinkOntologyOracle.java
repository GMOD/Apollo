package org.bbop.apollo.gwt.client.oracles;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.ibm.icu.impl.BOCU;
import org.bbop.apollo.gwt.client.go.GoEvidenceCode;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ndunn on 4/24/15.
 */
public class BiolinkOntologyOracle extends MultiWordSuggestOracle {

    private final Integer ROWS = 20;
    private final String FINAL_URL = "http://api.geneontology.org/api/search/entity/autocomplete/";
    public final static String ECO_BASE = "http://www.evidenceontology.org/term/";
    public final static String GO_BASE = "http://amigo.geneontology.org/amigo/term/";
    public final static String RO_BASE = "http://www.ontobee.org/ontology/RO?iri=http://purl.obolibrary.org/obo/";

    private String prefix;
    private String baseUrl;
    private String category = null;
    private JSONArray preferredSuggestions = new JSONArray();
    private Boolean usePreferredSuggestions = true ;

    public BiolinkOntologyOracle() {
        this(BiolinkLookup.ECO, ECO_BASE);
    }

    public BiolinkOntologyOracle(BiolinkLookup biolinkLookup) {
        this(biolinkLookup,null);
    }

    public BiolinkOntologyOracle(BiolinkLookup biolinkLookup, String baseUrl) {
        super();
        this.prefix = biolinkLookup.name();
        if(baseUrl!=null){
            this.baseUrl = baseUrl;
        }
        else{
            switch (biolinkLookup){
                case ECO:
                    this.baseUrl = ECO_BASE;
                    for(GoEvidenceCode goEvidenceCode : GoEvidenceCode.values()){
                        addPreferredSuggestion(goEvidenceCode.name() , goEvidenceCode.getDescription(),goEvidenceCode.getCurie());
                    }
                    break;
                case GO:
                    this.baseUrl = GO_BASE;
                    break;
                case RO:
                    this.baseUrl = RO_BASE;
                    break;
                default:
                    this.baseUrl = null ;
            }
        }
    }

    public void addPreferredSuggestion(String name, String label, String id) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", new JSONString(name));
        jsonObject.put("label", new JSONString(label));
        jsonObject.put("id", new JSONString(id));
        preferredSuggestions.set(preferredSuggestions.size(), jsonObject);
    }


    @Override
    public void requestSuggestions(final Request suggestRequest, final Callback suggestCallback) {
        String url = FINAL_URL + suggestRequest.getQuery() + "?rows=" + ROWS;
        if (prefix != null) {
            url += "&prefix=" + prefix;
        }
        if (category != null) {
            url += "&category=" + category;
        }

        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);

        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(com.google.gwt.http.client.Request request, com.google.gwt.http.client.Response response) {
                    JSONArray jsonArray = JSONParser.parseStrict(response.getText()).isObject().get("docs").isArray();
                    List<Suggestion> suggestionList = new ArrayList<>();
                    Set<String> ids = new HashSet<>();


                    /*
                      handle preferred suggestions
                      **/
                    for (int i = 0; i < preferredSuggestions.size(); i++) {
                        final JSONObject jsonObject = preferredSuggestions.get(i).isObject();
                        final String id = jsonObject.get("id").isString().stringValue();
                        ids.add(id);
                        Suggestion suggestion = new Suggestion() {
                            @Override
                            public String getDisplayString() {
                                String displayString = jsonObject.get("label").isString().stringValue();
                                displayString = displayString.replaceAll(suggestRequest.getQuery(), "<em>" + suggestRequest.getQuery() + "</em>");
                                displayString += " (" + id + ") ";
                                displayString = "<div style='font-weight:boldest;font-size:larger;'>" + displayString + "</div>";
                                return displayString;
                            }

                            @Override
                            public String getReplacementString() {
                                return id;
                            }
                        };
                        suggestionList.add(suggestion);
                    }


                    for (int i = 0; i < jsonArray.size(); i++) {
                        final JSONObject jsonObject = jsonArray.get(i).isObject();
                        final String id = jsonObject.get("id").isString().stringValue();

                        if (!ids.contains(id)) {
                            Suggestion suggestion = new Suggestion() {
                                @Override
                                public String getDisplayString() {
                                    String displayString = jsonObject.get("label").isArray().get(0).isString().stringValue();
                                    displayString = displayString.replaceAll(suggestRequest.getQuery(), "<b><em>" + suggestRequest.getQuery() + "</em></b>");
                                    displayString += " (" + id + ") ";
                                    return displayString;
                                }

                                @Override
                                public String getReplacementString() {
                                    return id;
                                }
                            };
                            suggestionList.add(suggestion);
                        }
                    }

                    Response r = new Response();
                    r.setSuggestions(suggestionList);
                    suggestCallback.onSuggestionsReady(suggestRequest, r);
                }

                @Override
                public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
                    Bootbox.alert("Error: " + exception);
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
            Bootbox.alert("Request exception via " + e);
        }

    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getUsePreferredSuggestions() {
        return usePreferredSuggestions;
    }

    public void setUsePreferredSuggestions(Boolean usePreferredSuggestions) {
        this.usePreferredSuggestions = usePreferredSuggestions;
    }
}
