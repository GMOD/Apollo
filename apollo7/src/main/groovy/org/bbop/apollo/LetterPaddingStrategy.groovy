package org.bbop.apollo
/**
 * Created by ndunn on 5/9/15.
 */
class LetterPaddingStrategy implements PaddingStrategy {

  final Integer maxInt = 26
  final char startLetter = 'a'
  final int asciiStart = 97

  String pad(Integer count) {
    int arrayIndex = count / maxInt ;
    int finalCount = count % maxInt;
    char arrayLetter = (char) finalCount + asciiStart ;
    char[] charArray = new char[arrayIndex+1]
    for (int i = 0; i < arrayIndex ; i++) {
      charArray[i] = startLetter;
    }
    charArray[arrayIndex] = arrayLetter
    return charArray.toString()
  }
}
