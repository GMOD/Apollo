package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        MainPanel mainPanel = new MainPanel();

        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");
        mainPanel.setRootUrl(rootUrl);

        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);
        rp.setWidgetTopHeight(mainPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);


        // Focus the cursor on the name field when the app loads
//        nameField.setFocus(true);
//        nameField.selectAll();
//
//        nameField.addChangeHandler(new ChangeHandler() {
//
//            @Override
//            public void onChange(ChangeEvent event) {
//                String url = "/apollo/annotator/search";
//                RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("query", new JSONString("pax6a"));
//                builder.setRequestData("data=" + jsonObject.toString());
//                builder.setHeader("Content-type", "application/x-www-form-urlencoded");
//                RequestCallback requestCallback = new RequestCallback() {
//                    @Override
//                    public void onResponseReceived(Request request, Response response) {
//                        JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                        JSONObject jsonObject = returnValue.isObject();
//                        String queryString = jsonObject.get("query").isString().stringValue();
//
//                        // TODO: use proper array parsing
//                        String resultString = jsonObject.get("result").isString().stringValue();
//                        resultString = resultString.replace("[", "");
//                        resultString = resultString.replace("]", "");
//                        searchResult.setText(" asdflkj asdflkjdas fsearch for " + queryString + " yields [" + resultString + "]");
//                    }
//
//                    @Override
//                    public void onError(Request request, Throwable exception) {
//                        Window.alert("ow");
//                    }
//                };
//                try {
//                    builder.setCallback(requestCallback);
//                    builder.send();
//                } catch (RequestException e) {
//                    // Couldn't connect to server
//                    Window.alert(e.getMessage());
//                }
//
//            }
//        });

    }


}
