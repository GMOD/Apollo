package org.bbop.apollo.sequence;


import org.bbop.apollo.Gene;
import org.bbop.apollo.Transcript;

public class SimpleOverlapper implements Overlapper {

    @Override
    public boolean overlaps(Transcript transcript, Gene gene) {
        if(transcript.getFmin()> gene.getFmin() && transcript.getFmin() < gene.getFmax()){
            return true ;
        }
        if(transcript.getFmax()< gene.getFmax() && transcript.getFmax() > gene.getFmax()){
            return true ;
        }
        return false ;
    }

    @Override
    public boolean overlaps(Transcript transcript1, Transcript transcript2) {
        if(transcript1.getFmin()> transcript2.getFmin() && transcript1.getFmin() < transcript2.getFmax()){
            return true ;
        }
        if(transcript1.getFmax()< transcript2.getFmax() && transcript1.getFmax() > transcript2.getFmax()){
            return true ;
        }
        return false ;
    }
    
}
