package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 4/24/15.
 */
public class ReferenceSequenceOracle extends MultiWordSuggestOracle{

    private final String rootUrl = Annotator.getRootUrl() + "sequence/lookupSequenceByName/?q=";

    @Override
    public void requestSuggestions(final SuggestOracle.Request suggestRequest, final Callback suggestCallback) {

        String url = rootUrl+ suggestRequest.getQuery();
        url += "&clientToken="+ Annotator.getClientToken();
        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);
//        rb.setHeader("Content-type", "application/x-www-form-urlencoded");

        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(com.google.gwt.http.client.Request request, com.google.gwt.http.client.Response response) {
                    GWT.log(response.getText());
                    JSONArray jsonArray = JSONParser.parseStrict(response.getText()).isArray();
                    createSuggestion(response.getText(), response.getText());
                    List<Suggestion> suggestionList = new ArrayList<>();


                    for(int i = 0 ; i < jsonArray.size() ; i++){
                        final String value = jsonArray.get(i).isString().stringValue();
                        Suggestion suggestion = new Suggestion() {
                            @Override
                            public String getDisplayString() {
                                return value ;
                            }

                            @Override
                            public String getReplacementString() {
                                return value ;
                            }
                        };
                        suggestionList.add(suggestion);
                    }

                    SuggestOracle.Response r = new SuggestOracle.Response();
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
