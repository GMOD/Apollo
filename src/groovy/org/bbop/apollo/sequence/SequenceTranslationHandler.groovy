package org.bbop.apollo.sequence

import org.bbop.apollo.AnnotationException

/**
 * Created by ndunn on 10/29/14.
 */
class SequenceTranslationHandler {

    private static Map<Integer, TranslationTable> translationTables = new HashMap<>();
    private static Set<String> spliceAcceptorSites = new HashSet<String>();
    private static Set<String> spliceDonorSites = new HashSet<String>();

    public final static Integer DEFAULT_TRANSLATION_TABLE = 1

    /** Reverse complement a nucleotide sequence.
     *
     * @param sequence - String for the nucleotide sequence to be reverse complemented
     * @return Reverse complemented nucleotide sequence
     */
    public static String reverseComplementSequence(String sequence) {
        StringBuilder buffer = new StringBuilder(sequence);
        buffer.reverse();
        for (int i = 0; i < buffer.length(); ++i) {
            switch (buffer.charAt(i)) {
                case 'A':
                    buffer.setCharAt(i, 'T' as char);
                    break;
                case 'C':
                    buffer.setCharAt(i, 'G' as char);
                    break;
                case 'G':
                    buffer.setCharAt(i, 'C' as char);
                    break;
                case 'T':
                    buffer.setCharAt(i, 'A' as char);
                    break;
            }
        }
        return buffer.toString();
    }

    /** Translate a nucleotide sequence into an amino acid sequence using the translation table.  The returned
     *  translated sequence will not include the stop amino acid and will stop at the first stop codon it
     *  finds as determined by the translation table.
     *
     * @param sequence - Nucleotide sequence to be translated
     * @param translationTable - TranslationTable that contains the codon translation table
     * @return Translated amino acid sequence
     */
    public static String translateSequence(String sequence, TranslationTable translationTable) {
        return translateSequence(sequence, translationTable, false, false);
    }

    /**  Translate a nucleotide sequence into an amino acid sequence using the translation table.
     *
     * @param sequence - Nucleotide sequence to be translated
     * @param translationTable - TranslationTable that contains the codon translation table
     * @param includeStop - Whether to include the stop amino acid in the translated sequence
     * @param translateThroughStop - Whether to continue translation through stop codons
     * @return Translated amino acid sequence
     */
    public static String translateSequence(String sequence, TranslationTable translationTable,
                                           boolean includeStop, boolean translateThroughStop) {
//        if (sequence.length() % 3 != 0) {
//            throw new AnnotationException("Sequence to be translated must have length of factor of 3");
//        }
        StringBuilder buffer = new StringBuilder();
        int stopCodonCount = 0;
        for (int i = 0; i + 3 <= sequence.length(); i += 3) {
            String codon = sequence.substring(i, i + 3);
            String aminoAcid = translationTable.translateCodon(codon);
            if (aminoAcid.equals(TranslationTable.STOP)) {
                if (includeStop) {
                    buffer.append(aminoAcid);
                }
                if (!translateThroughStop) {
                    break;
                }
                // TODO: not sure why this is written this way . . .clearly a bug
                else {
                    if (++stopCodonCount > 1) {
                        break;
                    }
                }
            } else {
                buffer.append(aminoAcid);
            }
        }
        return buffer.toString();
    }

    /** Get the translation table for a NCBI translation table code.
     *
     * @param code - NCBI translation table code
     * @return TranslationTable for the NCBI translation table code
     * @throws AnnotationException - If an invalid NCBI translation table code is used
     */
    public static TranslationTable getTranslationTableForGeneticCode(int code) throws AnnotationException {
        if (!translationTables.containsKey(code)) {
            initTranslationTables(code);
        }
        if (code < DEFAULT_TRANSLATION_TABLE || !translationTables.containsKey(code)) {
            throw new AnnotationException("Invalid translation table code");
        }
        return translationTables.get(code);
    }

    /** Get the default translation table (NCBI translation table code 1).
     *
     * @return Default translation table
     */
    public static TranslationTable getDefaultTranslationTable() {
        return getTranslationTableForGeneticCode(1)
    }

    private static void initTranslationTables(Integer code) {
        if (code == 1) {
            translationTables.put(code, new StandardTranslationTable())
        } else {
            translationTables.put(code, TranslationTableReader.readTable(new File("ncbi_${code}_translation_table.txt")))
        }
    }

/** Get the splice acceptor sites.
 *
 * @return Set of strings for splice acceptor sites
 */
    public static Set<String> getSpliceAcceptorSites() {
        return spliceAcceptorSites;
    }

/** Add a splice acceptor site.
 *
 * @param spliceAcceptorSite - String for splice acceptor site
 */
    public static void addSpliceAcceptorSite(String spliceAcceptorSite) {
        spliceAcceptorSites.add(spliceAcceptorSite);
    }

/** Remove a splice acceptor site.
 *
 * @param spliceAcceptorSite - String for splice acceptor site
 */
    public static void deleteSpliceAcceptorSite(String spliceAcceptorSite) {
        spliceAcceptorSites.remove(spliceAcceptorSite);
    }

/** Get the splice donor sites.
 *
 * @return Set of string for splice donor sites
 */
    public static Set<String> getSpliceDonorSites() {
        return spliceDonorSites;
    }

/** Add a splice donor site.
 *
 * @param spliceDonorSite - Strings for splice donor site
 */
    public static void addSpliceDonorSite(String spliceDonorSite) {
        spliceDonorSites.add(spliceDonorSite);
    }

/** Remove a splice donor site.
 *
 * @param spliceDonorSite - String for splice donor site
 */
    public static void deleteSpliceDonorSite(String spliceDonorSite) {
        spliceDonorSites.remove(spliceDonorSite);
    }
}
