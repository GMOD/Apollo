package org.bbop.apollo.sequence

/**
 * Created by nathandunn on 1/4/16.
 */
class TranslationTableReader {


    public static TranslationTable readTable(File file) {
        TranslationTable ttable = new TranslationTable()
//        BufferedReader reader = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream(track.getTranslationTable())));
        file.text.readLines().each { String line ->
            String[] tokens = line.split("\t");
            String codon = tokens[0].toUpperCase();
            String aa = tokens[1].toUpperCase();
            ttable.getTranslationTable().put(codon, aa);
            if (aa.equals(TranslationTable.STOP)) {
                ttable.getStopCodons().add(codon);
                if (tokens.length == 3) {
                    ttable.getAlternateTranslationTable().put(codon, tokens[2]);
                } else {
                    ttable.getAlternateTranslationTable().remove(codon);
                }
            } else {
                ttable.getStopCodons().remove(codon);
                ttable.getAlternateTranslationTable().remove(codon);
            }
            if (tokens.length == 3) {
                if (tokens[2].equals("start")) {
                    ttable.getStartCodons().add(codon);
                }
            } else {
                ttable.getStartCodons().remove(codon);
            }
        }
        return ttable
    }
}
