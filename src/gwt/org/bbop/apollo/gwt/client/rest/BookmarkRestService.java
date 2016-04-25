package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfo;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfoConverter;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * This class stores Boorkmars
 * Created by nathandunn on 10/1/15.
 */
public class BookmarkRestService {

    public static void loadBookmarks(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "bookmark/list");
    }

    public static void addBookmark(RequestCallback requestCallback,BookmarkInfo... bookmarkInfoCollection) {
        RestService.sendRequest(requestCallback, "bookmark/addBookmark", BookmarkInfoConverter.convertBookmarkInfoToJSONArray(bookmarkInfoCollection));
    }

    public static void removeBookmarks(RequestCallback requestCallback,BookmarkInfo... selectedSet) {
        RestService.sendRequest(requestCallback, "bookmark/deleteBookmark",BookmarkInfoConverter.convertBookmarkInfoToJSONArray(selectedSet));
    }

    public static void getBookmarks(RequestCallback requestCallback, BookmarkInfo bookmarkInfo) {
        RestService.sendRequest(requestCallback, "bookmark/getBookmark",BookmarkInfoConverter.convertBookmarkInfoToJSONObject(bookmarkInfo));
    }

    public static void searchBookmarks(RequestCallback requestCallback, String searchString) {
        String requestString = "bookmark/searchBookmarks/?searchQuery=" + searchString;
        RestService.sendRequest(requestCallback, requestString);
    }

    public static void clearBookmarkCache() {
        String requestString = "bookmark/clearBookmarkCache";
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Bootbox.alert("Removed Bookmark Cache");
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error clearning bookmark cache: "+exception.fillInStackTrace().toString());
            }
        };
        RestService.sendRequest(requestCallback,requestString);
    }
}
