package org.bbop.apollo.config;

import org.gmod.gbol.util.GBOLUtilException;
import org.gmod.gbol.util.SequenceUtil;
import org.gmod.gbol.util.SequenceUtil.TranslationTable;

/** Holds different configuration options.
 * 
 * @author elee
 *
 */
public class Configuration {

    private TranslationTable translationTable;
    private int translationCode = 1;
    private boolean partialTranslationExtensionAllowed = false;

    /** Get the translation table associated with the NCBI translation code in this configuration.
     *  Returns null if the configuration code is invalid.
     * 
     * @return translation table associated with the NCBI translation code in this configuration
     */
    public TranslationTable getTranslationTable() {
        if (translationTable == null) {
            try {
                return SequenceUtil.getTranslationTableForGeneticCode(getTranslationCode());
            } catch (GBOLUtilException e) {
                return null;
            }
        }
        else {
            return translationTable;
        }
    }
    
    public void setTranslationTable(TranslationTable translationTable) {
        this.translationTable = translationTable;
    }
    
    /** Get the NCBI translation table code to be used in translation.
     * 
     * @return NCBI translation table code to be used in translation
     */
    public int getTranslationCode() {
        return translationCode;
    }

    /** Set the NCBI translation table code to be used in translation.
     * 
     * @param NCBI translation table code to be used in translation
     */
    public void setTranslationCode(int translationCode) {
        this.translationCode = translationCode;
    }

    /** Get whether partial translation extensions are allowed.
     * 
     * @return true if partial translations are allowed
     */
    public boolean isPartialTranslationExtensionAllowed() {
        return partialTranslationExtensionAllowed;
    }

    /** Set whether partial translation extensions are allowed.
     * 
     * @param partialTranslationsAllowed - Whether partial translations are allowed
     */
    public void setPartialTranslationsExtensionAllowed(boolean partialTranslationsAllowed) {
        this.partialTranslationExtensionAllowed = partialTranslationsAllowed;
    }

    
}
