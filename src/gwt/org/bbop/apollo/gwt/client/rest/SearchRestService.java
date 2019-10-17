package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 *
 */
public class SearchRestService {

  public static void getTools(RequestCallback requestCallback) {
    RestService.sendRequest(requestCallback, "annotationEditor/getSequenceSearchTools");
  }

  public static void searchSequence(RequestCallback requestCallback, String searchToolKey , String residues, String databaseId) {

//    RestService.sendRequest(requestCallback, "organism/updateOrganismInfo", "data=" + organismInfoObject.toString());
    JSONObject searchObject = new JSONObject();

    JSONObject searchKey = new JSONObject();
    searchKey.put("key",new JSONString(searchToolKey));
    searchKey.put("residues",new JSONString(residues));
    if(databaseId!=null){
      searchKey.put("database_id",new JSONString(databaseId));
    }

    searchObject.put("search",searchKey);

    RestService.sendRequest(requestCallback, "annotationEditor/searchSequence","data="+searchObject.toString());
  }
}
