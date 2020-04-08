package org.bbop.apollo.gwt.client.oracles;


import org.gwtbootstrap3.client.ui.SuggestBox;

public class BiolinkSuggestBox extends SuggestBox {

    public BiolinkSuggestBox(BiolinkOntologyOracle oracle) {
        super(oracle);
    }

    @Override
    public void showSuggestionList() {
        if(getText().length()>=0){
            super.showSuggestionList();
        }
    }
}
