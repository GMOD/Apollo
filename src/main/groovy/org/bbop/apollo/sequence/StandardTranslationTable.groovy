package org.bbop.apollo.sequence

/**
 * Created by ndunn on 10/29/14.
 */
class StandardTranslationTable extends TranslationTable {

    public StandardTranslationTable() {
        translationTable.put("TTT", "F");
        translationTable.put("TTC", "F");
        translationTable.put("TTA", "L");
        translationTable.put("TTG", "L");
        translationTable.put("CTT", "L");
        translationTable.put("CTC", "L");
        translationTable.put("CTA", "L");
        translationTable.put("CTG", "L");
        translationTable.put("ATT", "I");
        translationTable.put("ATC", "I");
        translationTable.put("ATA", "I");
        translationTable.put("ATG", "M");
        translationTable.put("GTT", "V");
        translationTable.put("GTC", "V");
        translationTable.put("GTA", "V");
        translationTable.put("GTG", "V");

        translationTable.put("TCT", "S");
        translationTable.put("TCC", "S");
        translationTable.put("TCA", "S");
        translationTable.put("TCG", "S");
        translationTable.put("CCT", "P");
        translationTable.put("CCC", "P");
        translationTable.put("CCA", "P");
        translationTable.put("CCG", "P");
        translationTable.put("ACT", "T");
        translationTable.put("ACC", "T");
        translationTable.put("ACA", "T");
        translationTable.put("ACG", "T");
        translationTable.put("GCT", "A");
        translationTable.put("GCC", "A");
        translationTable.put("GCA", "A");
        translationTable.put("GCG", "A");

        translationTable.put("TAT", "Y");
        translationTable.put("TAC", "Y");
        translationTable.put("TAA", STOP);
        translationTable.put("TAG", STOP);
        translationTable.put("CAT", "H");
        translationTable.put("CAC", "H");
        translationTable.put("CAA", "Q");
        translationTable.put("CAG", "Q");
        translationTable.put("AAT", "N");
        translationTable.put("AAC", "N");
        translationTable.put("AAA", "K");
        translationTable.put("AAG", "K");
        translationTable.put("GAT", "D");
        translationTable.put("GAC", "D");
        translationTable.put("GAA", "E");
        translationTable.put("GAG", "E");

        translationTable.put("TGT", "C");
        translationTable.put("TGC", "C");
        translationTable.put("TGA", STOP);
        translationTable.put("TGG", "W");
        translationTable.put("CGT", "R");
        translationTable.put("CGC", "R");
        translationTable.put("CGA", "R");
        translationTable.put("CGG", "R");
        translationTable.put("AGT", "S");
        translationTable.put("AGC", "S");
        translationTable.put("AGA", "R");
        translationTable.put("AGG", "R");
        translationTable.put("GGT", "G");
        translationTable.put("GGC", "G");
        translationTable.put("GGA", "G");
        translationTable.put("GGG", "G");

        startCodons.add("ATG");

        stopCodons.add("TAA");
        stopCodons.add("TAG");
        stopCodons.add("TGA");

        alternateTranslationTable.put("TGA", "U");
    }

}
