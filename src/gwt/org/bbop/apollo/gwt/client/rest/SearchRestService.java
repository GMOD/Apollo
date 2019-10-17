package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;

/**
 *
 */
public class SearchRestService {

  public static void getTools(RequestCallback requestCallback) {
    RestService.sendRequest(requestCallback, "annotationEditor/getSequenceSearchTools");
  }
}
