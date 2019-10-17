package org.bbop.apollo.gwt.shared.sequence;

import com.google.gwt.json.client.JSONObject;

public class SearchHit {
  String id;
  Long start;
  Long end;
  Double score;
  Double significance;
  Double identity;

  public SearchHit(JSONObject hit) {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public Double getSignificance() {
    return significance;
  }

  public void setSignificance(Double significance) {
    this.significance = significance;
  }

  public Double getIdentity() {
    return identity;
  }

  public void setIdentity(Double identity) {
    this.identity = identity;
  }
}
