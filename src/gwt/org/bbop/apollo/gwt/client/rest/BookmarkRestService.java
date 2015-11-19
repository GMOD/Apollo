package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfo;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfoConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
}
