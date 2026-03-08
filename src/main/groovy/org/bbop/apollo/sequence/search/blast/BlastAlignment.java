package org.bbop.apollo.sequence.search.blast;

public class BlastAlignment {

    private String queryId;
    private String subjectId;
    private double percentId;
    private int alignmentLength;
    private int numMismatches;
    private int numGaps;
    private int queryStrand;
    private int subjectStrand;
    private int queryStart;
    private int queryEnd;
    private int subjectStart;
    private int subjectEnd;
    private double eValue;
    private double bitscore;

    protected void init(String queryId, String subjectId, double percentId, int alignmentLength, int numMismatches, int numGaps,
            int queryStart, int queryEnd, int subjectStart, int subjectEnd, double eValue, double bitscore,int queryStrand,int subjectStrand) {
        this.queryId = queryId;
        this.subjectId = subjectId;
        this.percentId = percentId;
        this.alignmentLength = alignmentLength;
        this.numMismatches = numMismatches;
        this.numGaps = numGaps;
        this.queryStart = queryStart;
        this.queryEnd = queryEnd;
        this.subjectStart = subjectStart;
        this.subjectEnd = subjectEnd;
        this.eValue = eValue;
        this.bitscore = bitscore;
        this.queryStrand = queryStrand;
        this.subjectStrand = subjectStrand;
    }

    public String getQueryId() {
        return queryId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public double getPercentId() {
        return percentId;
    }

    public int getAlignmentLength() {
        return alignmentLength;
    }

    public int getNumMismatches() {
        return numMismatches;
    }

    public int getNumGaps() {
        return numGaps;
    }

    public int getQueryStart() {
        return queryStart;
    }

    public int getQueryEnd() {
        return queryEnd;
    }

    public int getSubjectStart() {
        return subjectStart;
    }

    public int getSubjectEnd() {
        return subjectEnd;
    }

    public double getEValue() {
        return eValue;
    }

    public double getBitscore() {
        return bitscore;
    }

    public int getQueryStrand() {
      return queryStrand;
    }

    public int getSubjectStrand() {
      return subjectStrand;
    }

  @Override
  public String toString() {
    return "BlastAlignment{" +
      "queryId='" + queryId + '\'' +
      ", subjectId='" + subjectId + '\'' +
      ", percentId=" + percentId +
      ", alignmentLength=" + alignmentLength +
      ", numMismatches=" + numMismatches +
      ", numGaps=" + numGaps +
      ", queryStrand=" + queryStrand +
      ", subjectStrand=" + subjectStrand +
      ", queryStart=" + queryStart +
      ", queryEnd=" + queryEnd +
      ", subjectStart=" + subjectStart +
      ", subjectEnd=" + subjectEnd +
      ", eValue=" + eValue +
      ", bitscore=" + bitscore +
      '}';
  }
}
