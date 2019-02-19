package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.ExportPanel;
import org.bbop.apollo.gwt.client.SequencePanel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.TrackInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class TrackRestService {

    public static void loadTracks(RequestCallback requestCallback, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback, "track/getTracksForOrganism/" + organismInfo.getId());
    }

    public static void updateTrack(RequestCallback requestCallback, TrackInfo trackInfo) {
        RestService.sendRequest(requestCallback, "track/updateTrack/" +trackInfo.getOrganismInfo() ,"data="+trackInfo.toJSON());
    }

//            RestService.sendRequest(requestCallback, "IOService/write", "data=" + jsonObject.toString());

}
