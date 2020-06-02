package org.bbop.apollo.gwt.client;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.SuggestOracle;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

public class SuggestedGeneProductOracle extends SuggestOracle {

  @Override
  public void requestSuggestions(final Request suggestRequest, final Callback suggestCallback) {

    final List<Suggestion> suggestionList = new ArrayList<>();

    final String queryString = suggestRequest.getQuery();


    String url = Annotator.getRootUrl() + "geneProduct/search";
    url += "?query=" + suggestRequest.getQuery();
    if (MainPanel.getInstance().getCurrentOrganism() != null) {
      String organismId = MainPanel.getInstance().getCurrentOrganism().getId();
      url += "&organism=" + organismId;
    }
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);

    try {
      rb.sendRequest(null, new RequestCallback() {
        @Override
        public void onResponseReceived(com.google.gwt.http.client.Request request, com.google.gwt.http.client.Response response) {
          // always suggest thyself
          suggestionList.add(new Suggestion() {
            @Override
            public String getDisplayString() {
              return queryString;
            }

            @Override
            public String getReplacementString() {
              return queryString;
            }
          });

          JSONArray jsonArray = JSONParser.parseStrict(response.getText()).isArray();
          for (int i = 0; i < jsonArray.size(); i++) {
            // always suggest thyself
            final String name = jsonArray.get(i).isString().stringValue();
//                        final String name = jsonObject.get("name").isString().stringValue();
            suggestionList.add(new Suggestion() {
              @Override
              public String getDisplayString() {
                // TODO: add mathcing string
//                                String displayString = queryString
                return name;
              }

              @Override
              public String getReplacementString() {
                return name;
              }
            });
          }
          Response r = new Response();
          r.setSuggestions(suggestionList);
          suggestCallback.onSuggestionsReady(suggestRequest, r);
        }

        @Override
        public void onError(com.google.gwt.http.client.Request request, Throwable exception) {
          Bootbox.alert("There was an error with the request: " + exception.getMessage());
        }
      });


    } catch (RequestException e) {
      Bootbox.alert("Request exception via " + e);
    }
  }
}
