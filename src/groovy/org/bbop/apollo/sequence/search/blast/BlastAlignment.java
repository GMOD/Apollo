package org.bbop.apollo.sequence.search.blast;

import org.bbop.apollo.sequence.search.Alignment;
import org.bbop.apollo.Match;
import org.bbop.apollo.Region;
import org.bbop.apollo.AnalysisFeature;
import org.bbop.apollo.Feature;

public class BlastAlignment implements Alignment {

    public String queryId;
    public String subjectId;
    public double percentId;
    public int alignmentLength;
    public int numMismatches;
    public int numGaps;
    public int queryStrand;
    public int subjectStrand;
    public int queryStart;
    public int queryEnd;
    public int subjectStart;
    public int subjectEnd;
    public double eValue;
    public double bitscore;

    public BlastAlignment(String queryId, String subjectId, double percentId, int alignmentLength, int numMismatches, int numGaps,
            int queryStart, int queryEnd, int subjectStart, int subjectEnd, double eValue, double bitscore) {
        init(queryId, subjectId, percentId, alignmentLength, numMismatches, numGaps, queryStart, queryEnd, subjectStart, subjectEnd,
                eValue, bitscore);
    }
    
    protected void init(String queryId, String subjectId, double percentId, int alignmentLength, int numMismatches, int numGaps,
            int queryStart, int queryEnd, int subjectStart, int subjectEnd, double eValue, double bitscore) {
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
    
    public Match convertToMatch() {
        AnalysisFeature analysisFeature = new AnalysisFeature();
        Match match=new Match();
        match.setAnalysisFeature(analysisFeature);
        analysisFeature.setRawScore(getBitscore());
        analysisFeature.setSignificance(getEValue());
        Feature query = new Feature();
        query.setUniqueName(getQueryId());
        int queryFmin = getQueryStart();
        int queryFmax = getQueryEnd();
        queryStrand = 1;
        if (queryFmin > queryFmax) {
            int tmp = queryFmin;
            queryFmin = queryFmax;
            queryFmax = tmp;
            queryStrand = -1;
        }
        --queryFmin;
        match.setQueryFeatureLocation(queryFmin, queryFmax, queryStrand, query);
        Feature subject = new Feature();
        subject.setUniqueName(getSubjectId());
        int subjectFmin = getSubjectStart();
        int subjectFmax = getSubjectEnd();
        subjectStrand = 1;
        if (subjectFmin > subjectFmax) {
            int tmp = subjectFmin;
            subjectFmin = subjectFmax;
            subjectFmax = tmp;
            subjectStrand = -1;
        }
        --subjectFmin;
        match.setSubjectFeatureLocation(subjectFmin, subjectFmax, subjectStrand, subject);
        match.setIdentity(getPercentId());
        return match;
    }

    protected BlastAlignment() {
    }
    
}
