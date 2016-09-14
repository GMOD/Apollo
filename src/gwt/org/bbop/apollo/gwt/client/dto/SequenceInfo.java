package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.comparators.AlphanumericSorter;

/**
 * Created by Nathan Dunn on 12/18/14.
 * TODO: add OrganismInfo
 */
public class SequenceInfo implements Comparable<SequenceInfo>,HasJSON{

    private AlphanumericSorter alphanumericSorter = new AlphanumericSorter();

    private Long id ;
    private String name ;
    private Long length ;
    private Long start ;
    private Long end ;
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

    public Long getLength() {
        if(end!=null && start!=null){
            return end-start;
        }
        if(length!=null){
            return length ;
        }
        return -1L ;
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

    public void setLength(Long length) {
        this.length = length;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    @Override
    public int compareTo(SequenceInfo o) {
        return alphanumericSorter.compare(name,o.getName());
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
