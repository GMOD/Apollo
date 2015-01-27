package org.bbop.apollo.gwt.client.dto;

import org.bbop.apollo.gwt.client.comparators.AlphanumericSorter;

/**
 * Created by ndunn on 12/18/14.
 */
public class SequenceInfo implements Comparable<SequenceInfo>{

    private AlphanumericSorter alphanumericSorter = new AlphanumericSorter();

    private Long id ;
    private String name ;
    private Integer length ;
    private Integer start ;
    private Integer end ;
    private Boolean selected = false ;
    private Boolean aDefault = false ;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLength() {
        if(end!=null && start!=null){
            return end-start;
        }
        if(length!=null){
            return length ;
        }
        return -1 ;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean isDefault() {
        return aDefault;
    }

    public void setDefault(Boolean aDefault) {
        this.aDefault = aDefault;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    @Override
    public int compareTo(SequenceInfo o) {
        return alphanumericSorter.compare(name,o.getName());
    }
}
