package org.bbop.apollo
/**
 * Created by Nathan Dunn on 5/9/15.
 */
class LetterPaddingStrategy implements PaddingStrategy {


    String pad(Integer count) {

        char startLetter = 'a';

        for(int i = 0 ; i < count ; i++){
            ++startLetter
        }

        return startLetter
    }
}
