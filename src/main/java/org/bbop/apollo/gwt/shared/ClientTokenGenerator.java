package org.bbop.apollo.gwt.shared;

import java.util.Random;

/**
 * Created by ndunn on 4/15/16.
 */
public class ClientTokenGenerator {


    private final static Random random = new Random(); // or SecureRandom
    public static final int DEFAULT_LENGTH = 20 ;
    public static final int MIN_TOKEN_LENGTH = 10;

    public static String generateRandomString() {
        return generateRandomString(DEFAULT_LENGTH);
    }

    public static String generateRandomString(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        while(stringBuilder.length()<length){
            stringBuilder.append(Math.abs(random.nextInt()));
        }
        return stringBuilder.toString();
    }

    public static boolean isValidToken(String clientID) {
        return (clientID!=null && clientID.length()>MIN_TOKEN_LENGTH);
    }
}
