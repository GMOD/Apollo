package org.bbop.apollo.sequence.search.blat;

public class BlatCommandLineProteinToNucleotide extends BlatCommandLine {

    public BlatCommandLineProteinToNucleotide() {
        blatOptions = new String[]{ "-t=dnax", "-q=prot" };
    }
    
}
