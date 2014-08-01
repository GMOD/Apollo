package org.bbop.apollo.tools.seq.search.blat;

public class BlatCommandLineProteinToNucleotide extends BlatCommandLine {

    public BlatCommandLineProteinToNucleotide() {
        blatOptions = new String[]{ "-t=dnax", "-q=prot" };
    }
    
}
