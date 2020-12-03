package org.bbop.apollo.gwt.client.comparators;

import org.bbop.apollo.gwt.client.dto.OrganismInfo;

import java.util.Comparator;

public class OrganismComparator implements Comparator<OrganismInfo> {
  @Override
  public int compare(OrganismInfo o1, OrganismInfo o2) {
    if(o1.getGenus()==null && o2.getGenus()!=null) return 1 ;
    if(o1.getGenus()!=null && o2.getGenus()==null) return -1 ;
    if(o1.getGenus()!=null && o2.getGenus()!=null){
      if(!o1.getGenus().equalsIgnoreCase(o2.getGenus())) return o1.getGenus().compareToIgnoreCase(o2.getGenus());
      if(o1.getGenus().equalsIgnoreCase(o2.getGenus())) {
        if(o1.getSpecies()==null && o2.getSpecies()!=null) return 1 ;
        if(o1.getSpecies()!=null && o2.getSpecies()==null) return -1 ;
        if(o1.getSpecies()==null && o2.getSpecies()==null) return o1.getName().compareToIgnoreCase(o2.getName());
        if(!o1.getSpecies().equalsIgnoreCase(o2.getSpecies())) {
          return o1.getSpecies().compareToIgnoreCase(o2.getSpecies());
        }
      }
    }
    return o1.getName().compareToIgnoreCase(o2.getName());
  }

}
