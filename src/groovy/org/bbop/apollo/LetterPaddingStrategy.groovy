package org.bbop.apollo
/**
 * Created by ndunn on 5/9/15.
 */
class LetterPaddingStrategy implements PaddingStrategy{

    char startLetter = 'a';

    String pad(Integer count){

        for(i in 0..count){
            startLetter++
        }

        return startLetter
    }
}
