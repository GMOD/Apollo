package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.ExportPanel;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;

/**
 * Created by ndunn on 1/14/15.
 */
public class AppRestService {

    private static AppRestService instance ;

    public static AppRestService getInstance(){
        if(instance==null){
            instance = new AppRestService();
        }
        return instance;
    }

    private AppRestService(){}

    /**
     * TODO: make it so
     * @param organismId
     */
    public void setCurrentOrganism(Long organismId){

    }

    /**
     * TODO: make it so
     * @param organismId
     * @param sequenceId
     */
    public void setCurrentOrganismAndSequence(Long organismId,Long sequenceId){

    }

//    public static void loadSequences(RequestCallback requestCallback,Long organismId){
//        if(MainPanel.getInstance().getCurrentOrganism()==null){
//            GWT.log("organism not set...returning");
//            return ;
//        }
//        RestService.sendRequest(requestCallback,"/sequence/loadSequences/"+ organismId);
//    }

//    public static void loadSequences(final List<SequenceInfo> sequenceInfoList,Long organismId) {
//        RequestCallback requestCallback = new RequestCallback() {
//            @Override
//            public void onResponseReceived(Request request, Response response) {
//                JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                JSONArray array = returnValue.isArray();
//                sequenceInfoList.clear();
//
//                for(int i = 0 ; i < array.size() ; i++){
//                    JSONObject object = array.get(i).isObject();
//                    SequenceInfo sequenceInfo = new SequenceInfo();
//                    sequenceInfo.setId((long) object.get("id").isNumber().doubleValue());
//                    sequenceInfo.setName(object.get("name").isString().stringValue());
//                    sequenceInfo.setLength((int) object.get("length").isNumber().doubleValue());
//                    sequenceInfo.setStart((int) object.get("start").isNumber().doubleValue());
//                    sequenceInfo.setEnd((int) object.get("end").isNumber().doubleValue());
////                    GWT.log("get default: "+object.get("default"));
//                    if(object.get("default")!=null){
////                        GWT.log("setting default to "+ sequenceInfo.getName());
//                        sequenceInfo.setDefault(object.get("default").isBoolean().booleanValue());
//                    }
//                    sequenceInfoList.add(sequenceInfo);
//                }
//                SequenceLoadEvent contextSwitchEvent = new SequenceLoadEvent(SequenceLoadEvent.Action.FINISHED_LOADING);
//                Annotator.eventBus.fireEvent(contextSwitchEvent);
//                GWT.log("added # sequences: "+sequenceInfoList.size());
//
//            }
//
//            @Override
//            public void onError(Request request, Throwable exception) {
//                Window.alert("Error loading organisms");
//            }
//        };
//        if(MainPanel.currentOrganismId==null){
//            GWT.log("organism not set...returning");
//            sequenceInfoList.clear();
//            return ;
//        }
//        loadSequences(requestCallback, organismId);
//    }

//    public static void setDefaultSequence(RequestCallback requestCallback,final String sequenceName) {
//        RestService.sendRequest(requestCallback, "/sequence/setDefaultSequence/" + MainPanel.currentOrganismId + "?sequenceName=" + sequenceName);
//    }

}
