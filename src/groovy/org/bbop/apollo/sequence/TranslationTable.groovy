package org.bbop.apollo.sequence

/**
 * Created by ndunn on 10/29/14.
 */
/** Abstract class that all specific translation tables must inherit from.
 *
 * @author elee
 *
 */
class TranslationTable {

    /** Amino acid for a stop codon.
     *
     */
    public static final String STOP = "*";

    String name
    protected Map<String, String> translationTable;
    protected Set<String> startCodons;
    protected Set<String> stopCodons;
    protected Map<String, String> alternateTranslationTable;

    protected TranslationTable() {
        translationTable = new HashMap<String, String>();
        startCodons = new HashSet<String>();
        stopCodons = new HashSet<String>();
        alternateTranslationTable = new HashMap<String, String>();
    }

    public TranslationTable cloneTable() {
        TranslationTable clone = null;
        try {
            clone = getClass().newInstance();
            clone.translationTable = new HashMap<String, String>(translationTable);
            clone.startCodons = new HashSet<String>(startCodons);
            clone.stopCodons = new HashSet<String>(stopCodons);
            clone.alternateTranslationTable = new HashMap<String, String>(alternateTranslationTable);
        }
        catch (InstantiationException e) {
        }
        catch (IllegalAccessException e) {
        }
        return clone;
    }

    /** Return the amino acid corresponding to the codon translation.  Returns "X" if the codon doesn't
     *  exist in the translation table.
     *
     * @param codon - Codon to be translated
     * @return Amino acid corresponding to the the codon
     */
    public String translateCodon(String codon) {
        String aa = translationTable.get(codon);
        if (aa == null) {
            return "X";
        }
        return aa;
    }

    /** Return a collection of start codons for the translation table.
     *
     * @return Collection of Strings representing the start codons
     */
    public Collection<String> getStartCodons() {
        return startCodons;
    }

    /** Return a collection of stop codons for the translation table.
     *
     * @return Collection of Strings representing the stop codons
     */
    public Collection<String> getStopCodons() {
        return stopCodons;
    }

    protected boolean isStopCodon(String codon) {
        return translateCodon(codon).equals(STOP);
    }

    public Map<String, String> getTranslationTable() {
        return translationTable;
    }

    public Map<String, String> getAlternateTranslationTable() {
        return alternateTranslationTable;
    }

    boolean isStartCodon(String s) {
        return startCodons.contains(s)
    }

}
