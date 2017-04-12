package org.bbop.apollo.sequence

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.bbop.apollo.AnnotationException

/**
 * Created by ndunn on 10/29/14.
 */
class SequenceTranslationHandler {

    private static Map<String, TranslationTable> translationTables = new HashMap<>();
    private static Set<String> spliceAcceptorSites = new HashSet<String>();
    private static Set<String> spliceDonorSites = new HashSet<String>();

    public final static String DEFAULT_TRANSLATION_TABLE = "1"

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
            if(i==0 && translationTable.isStartCodon(codon)){
                aminoAcid = "M"
            }

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
    public static TranslationTable getTranslationTableForGeneticCode(String code) throws AnnotationException {
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
        return getTranslationTableForGeneticCode(DEFAULT_TRANSLATION_TABLE)
    }

    private static void initTranslationTables(String code) {
        if (code == DEFAULT_TRANSLATION_TABLE) {
            translationTables.put(code.toString(), new StandardTranslationTable())
        } else {
            File parentFile = FileUtils.listFiles(new File("."),new NameFileFilter("ncbi_1_translation_table.txt"),TrueFileFilter.INSTANCE).first().parentFile
            translationTables.put(code.toString(), readTable(new File(parentFile.absolutePath+"/ncbi_${code}_translation_table.txt")))
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

    /**
     * Pocesses deltas from the standard (1) translation table:
     * http://www.ncbi.nlm.nih.gov/Taxonomy/Utils/wprintgc.cgi?mode=t
     *
     * code, codon <start|stop|none>
     *
     * if stop codon (*), add to stop codon table
     *     if "none" then remove from alternate translation table, else add to alternatetranslation table
     * else
     *     if "none", remove codon from stop codon, and from alternate translation table
     *
     *
     * if 3 is start, add to start codons, else remove from start codons
     *
     *
     * @param file
     * @return
     */
    public static TranslationTable readTable(File file) {
        TranslationTable ttable = new StandardTranslationTable().cloneTable()
        ttable.name = file.name
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
