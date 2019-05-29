package org.bbop.apollo.gwt.client;

import com.google.gwt.user.client.ui.SuggestOracle;

import java.util.ArrayList;
import java.util.List;

public class SuggestedNameOracle extends SuggestOracle {


    @Override
    public void requestSuggestions(final Request suggestRequest, final Callback suggestCallback) {

        List<Suggestion> suggestionList = new ArrayList<>();

        // always suggest thyself
        suggestionList.add(new Suggestion() {
            @Override
            public String getDisplayString() {
                return suggestRequest.getQuery();
            }

            @Override
            public String getReplacementString() {
                return suggestRequest.getQuery();
            }
        });





        Response r = new Response();
        r.setSuggestions(suggestionList);
        suggestCallback.onSuggestionsReady(suggestRequest, r);

    }
}
