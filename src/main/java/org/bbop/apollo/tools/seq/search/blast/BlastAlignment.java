package org.bbop.apollo.tools.seq.search.blast;

import org.bbop.apollo.tools.seq.search.Alignment;
import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.Region;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.AnalysisFeature;
import org.gmod.gbol.simpleObject.Feature;

public class BlastAlignment implements Alignment {

    private String queryId;
    private String subjectId;
    private double percentId;
    private int alignmentLength;
    private int numMismatches;
    private int numGaps;
    private int queryStart;
    private int queryEnd;
    private int subjectStart;
    private int subjectEnd;
    private double eValue;
    private double bitscore;

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
    
    public Match convertToMatch(BioObjectConfiguration conf) {
        AnalysisFeature analysisFeature = new AnalysisFeature();
        Match match = new Match(null, null, false, null, analysisFeature, conf);
        analysisFeature.setRawScore(getBitscore());
        analysisFeature.setSignificance(getEValue());
        Feature query = new Feature();
        query.setType(conf.getDefaultCVTermForClass("Region"));
        query.setUniqueName(getQueryId());
        int queryFmin = getQueryStart();
        int queryFmax = getQueryEnd();
        int queryStrand = 1;
        if (queryFmin > queryFmax) {
            int tmp = queryFmin;
            queryFmin = queryFmax;
            queryFmax = tmp;
            queryStrand = -1;
        }
        --queryFmin;
        match.setQueryFeatureLocation(queryFmin, queryFmax, queryStrand, new Region(query, conf));
        Feature subject = new Feature();
        subject.setType(conf.getDefaultCVTermForClass("Region"));
        subject.setUniqueName(getSubjectId());
        int subjectFmin = getSubjectStart();
        int subjectFmax = getSubjectEnd();
        int subjectStrand = 1;
        if (subjectFmin > subjectFmax) {
            int tmp = subjectFmin;
            subjectFmin = subjectFmax;
            subjectFmax = tmp;
            subjectStrand = -1;
        }
        --subjectFmin;
        match.setSubjectFeatureLocation(subjectFmin, subjectFmax, subjectStrand, new Region(subject, conf));
        match.setIdentity(getPercentId());
        return match;
    }

    protected BlastAlignment() {
    }
    
}
