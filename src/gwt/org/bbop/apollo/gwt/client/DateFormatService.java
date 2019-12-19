package org.bbop.apollo.gwt.client;

import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.Date;

public class DateFormatService {
    private static DateTimeFormat outputFormatDate = DateTimeFormat.getFormat("MMM dd, yyyy");
    private static DateTimeFormat outputFormatDateTime = DateTimeFormat.getFormat("MMM dd, yyyy hh:mm a");

    public static String formatDate(Date date){
        return outputFormatDate.format(date);
    }

    public static String formatTimeAndDate(String dateLongString){
        Date date = new Date(Long.parseLong(dateLongString));
        return outputFormatDateTime.format(date);
    }

    public static String formatTimeAndDate(Date date){
        return outputFormatDateTime.format(date);
    }
}
