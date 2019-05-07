package org.bbop.apollo.gwt.client.oracles;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 4/24/15.
 */
public class BiolinkOntologyOracle extends MultiWordSuggestOracle{

    private final Integer ROWS = 20 ;
    private final String FINAL_URL = "http://api.geneontology.org/api/search/entity/autocomplete/";

    private String prefix = null ;

    public BiolinkOntologyOracle(){ }

    public BiolinkOntologyOracle(String prefix){

        this.prefix = prefix ;
    }


    @Override
    public void requestSuggestions(final Request suggestRequest, final Callback suggestCallback) {
        String url = FINAL_URL + suggestRequest.getQuery() + "?rows="+ROWS ;
        if(prefix!=null){
            url += "&prefix="+prefix;
        }

        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);

        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(com.google.gwt.http.client.Request request, com.google.gwt.http.client.Response response) {
//                    GWT.log(response.getText());
//                    JSON.parse(response.get()).
                    JSONArray jsonArray = JSONParser.parseStrict(response.getText()).isObject().get("docs").isArray();
//                    createSuggestion(response.getText(), response.getText());
                    List<Suggestion> suggestionList = new ArrayList<>();

                    for(int i = 0 ; i < jsonArray.size() ; i++){
                        final JSONObject jsonObject = jsonArray.get(i).isObject();
                        Suggestion suggestion = new Suggestion() {
                            @Override
                            public String getDisplayString() {
                                String displayString = jsonObject.get("label").isArray().get(0).isString().stringValue();
                                return displayString.replaceAll(suggestRequest.getQuery(),"<b><em>"+suggestRequest.getQuery()+"</em></b>");
                            }

                            @Override
                            public String getReplacementString() {
                                return jsonObject.get("id").isString().stringValue();
                            }
                        };
                        suggestionList.add(suggestion);
                    }

                    Response r = new Response();
                    r.setSuggestions(suggestionList);
                    suggestCallback.onSuggestionsReady(suggestRequest,r);
                }

                @Override
                public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
                    Bootbox.alert("Error: "+exception);
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
            Bootbox.alert("Request exception via " + e);
        }

    }
}
