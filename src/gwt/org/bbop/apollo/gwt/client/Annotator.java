package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.core.java.util.HashMap_CustomFieldSerializer;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.shared.ClientTokenGenerator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Annotator implements EntryPoint {

    public static EventBus eventBus = GWT.create(SimpleEventBus.class);
    private static Storage preferenceStore = Storage.getSessionStorageIfSupported();
    private static Map<String,String> backupPreferenceStore = new HashMap<>();

    // check the session once a minute
    private static Integer DEFAULT_PING_TIME = 60000;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        MainPanel mainPanel = MainPanel.getInstance();
        RootLayoutPanel rp = RootLayoutPanel.get();
        rp.add(mainPanel);

        Dictionary optionsDictionary = Dictionary.getDictionary("Options");
        if(optionsDictionary.keySet().contains(FeatureStringEnum.CLIENT_TOKEN.getValue())){
            String clientToken = optionsDictionary.get(FeatureStringEnum.CLIENT_TOKEN.getValue());
            if(ClientTokenGenerator.isValidToken(clientToken)){
                setPreference(FeatureStringEnum.CLIENT_TOKEN.getValue(),clientToken);
            }
        }
        Double height = 100d;
        Style.Unit heightUnit = Style.Unit.PCT;
        Double top = 0d;
        Style.Unit topUnit = Style.Unit.PCT;

        if (optionsDictionary.keySet().contains("top")) {
            top = Double.valueOf(optionsDictionary.get("top"));
        }
        if (optionsDictionary.keySet().contains("topUnit")) {
            topUnit = Style.Unit.valueOf(optionsDictionary.get("topUnit").toUpperCase());
        }
        if (optionsDictionary.keySet().contains("height")) {
            height = Double.valueOf(optionsDictionary.get("height"));
        }
        if (optionsDictionary.keySet().contains("heightUnit")) {
            heightUnit = Style.Unit.valueOf(optionsDictionary.get("heightUnit").toUpperCase());
        }
        rp.setWidgetTopHeight(mainPanel, top, topUnit, height, heightUnit);

        exportStaticMethod();
    }

    static void startSessionTimer() {
        startSessionTimer(DEFAULT_PING_TIME);
    }

    static void startSessionTimer(int i) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {

            private Boolean keepGoing = true ;
            private Boolean confirmOpen = false ;

            @Override
            public boolean execute() {
                if(MainPanel.hasCurrentUser()){
                    RestService.sendRequest(new RequestCallback() {
                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            int statusCode = response.getStatusCode();
                            if(statusCode==200 && response.getText().equals("{}")){
                                GWT.log("Still connected");
                            }
                            else{
                                if(!confirmOpen){
                                    confirmOpen = true ;
                                    Bootbox.confirm("Logged out or server failure.  Attempt to reconnect?", new ConfirmCallback() {
                                        @Override
                                        public void callback(boolean result) {
                                            if(result){
                                                Window.Location.reload();
                                            }

                                            confirmOpen = false ;
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onError(Request request, Throwable exception) {
                            Window.alert("failed to connect: "+exception.toString());
                            Bootbox.alert("Error: "+exception);
                        }
                    },"annotator/ping");

                    return keepGoing ;
                }
                else{
                    return false;
                }
            }
        },i);
    }

    public static native void exportStaticMethod() /*-{
        $wnd.setPreference = $entry(@org.bbop.apollo.gwt.client.Annotator::setPreference(Ljava/lang/String;Ljava/lang/Object;));
        $wnd.getPreference = $entry(@org.bbop.apollo.gwt.client.Annotator::getPreference(Ljava/lang/String;));
        $wnd.getClientToken = $entry(@org.bbop.apollo.gwt.client.Annotator::getClientToken());
        $wnd.getEmbeddedVersion = $entry(
            function apolloEmbeddedVersion() {
                return 'ApolloGwt-2.0';
            }
        );
    }-*/;

    public static void setPreference(String key, Object value) {
        if (preferenceStore != null) {
            preferenceStore.setItem(key, value.toString());
        }
        else{
            backupPreferenceStore.put(key,value.toString());
        }
    }

    public static String getPreference(String key) {
        if (preferenceStore != null) {
            return preferenceStore.getItem(key);
        }
        else{
            return backupPreferenceStore.get(key);
        }
    }


    public static String getRootUrl(){
        return GWT.getModuleBaseURL().replace("annotator/","");
    }

    public static String getClientToken() {
        String token = getPreference(FeatureStringEnum.CLIENT_TOKEN.getValue());
        if (!ClientTokenGenerator.isValidToken(token)) {
            token = ClientTokenGenerator.generateRandomString();
            setPreference(FeatureStringEnum.CLIENT_TOKEN.getValue(), token);
        }
        token = getPreference(FeatureStringEnum.CLIENT_TOKEN.getValue());
        return token ;

    }

}
