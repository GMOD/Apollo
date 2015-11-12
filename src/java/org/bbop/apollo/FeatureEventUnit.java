package org.bbop.apollo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nathan Dunn on 6/15/15.
 */
public class FeatureEventUnit  implements Comparable<FeatureEventUnit>{

    private List<FeatureEvent> featureEventList = new ArrayList<>();

    public FeatureEventUnit(FeatureEvent featureEvent){
        featureEventList.add(featureEvent);
    }

    public void add(FeatureEvent featureEvent){
        featureEventList.add(featureEvent) ;
    }

    public int size(){
        return featureEventList.size();
    }


    public FeatureEvent get(int index){
        return featureEventList.get(index);
    }

    @Override
    public int compareTo(FeatureEventUnit first) {
        if(first.size()==0) return -1 ;
        if(this.size()==0) return 1 ;

        return this.get(0).getDateCreated().compareTo(first.get(0).getDateCreated());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureEventUnit that = (FeatureEventUnit) o;
        if(featureEventList.size()==that.featureEventList.size()){
            for(int i = 0 ; i < featureEventList.size() ; i++){
                if(!featureEventList.get(i).getUniqueName().equals(that.featureEventList.get(i).getUniqueName())){
                    return false ;
                }
            }
            return true ;
        }
        return false ;
    }

    @Override
    public int hashCode() {
        return featureEventList.hashCode();
    }
}
