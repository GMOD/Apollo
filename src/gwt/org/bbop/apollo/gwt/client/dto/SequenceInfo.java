package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.comparators.AlphanumericSorter;

/**
 * Created by ndunn on 12/18/14.
 */
public class SequenceInfo implements Comparable<SequenceInfo>,HasJSON{

    private AlphanumericSorter alphanumericSorter = new AlphanumericSorter();

    private Long id ;
    private String name ;
    private Integer length ;
    private Integer start ;
    private Integer end ;
    private Integer startBp ; // preference values
    private Integer endBp ;// preference values
    private Integer count ;
    private Boolean selected = false ;
    private Boolean aDefault = false ;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
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

    public Integer getStartBp() {
        return startBp;
    }


    public void setStartBp(Integer startBp) {
        this.startBp = startBp;
    }

    public void setStartBp(Double startBp ) {
        if(startBp!=null){
            this.startBp = startBp.intValue();
        }
    }

    public Integer getEndBp() {
        return endBp;
    }

    public void setEndBp(Integer endBp) {
        this.endBp = endBp;
    }

    public void setEndBp(Double endBp) {
        if(endBp!=null){
            this.endBp = endBp.intValue();
        }
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", new JSONNumber(id));
        jsonObject.put("name", new JSONString(name));
        jsonObject.put("start", new JSONNumber(start));
        if(count != null) jsonObject.put("count", new JSONNumber(count));
        jsonObject.put("end", new JSONNumber(end));
        jsonObject.put("length", new JSONNumber(length));
        return jsonObject;

    }
}
