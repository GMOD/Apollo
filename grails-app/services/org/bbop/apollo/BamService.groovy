package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import htsjdk.samtools.*
import htsjdk.samtools.util.StringUtil
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.util.regex.Matcher
import java.util.regex.Pattern

@Transactional
class BamService {

    // relevant pattern matching: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L785
    static final Pattern mdPat = Pattern.compile("\\G(?:([0-9]+)|([ACTGNactgn])|(\\^[ACTGNactgn]+))");
    /**
     * matching DraggableAlignments.js
     */
    @NotTransactional
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, BAMFileReader samReader, int start, int end, File sourceFile) {

        ProjectionSequence projectionSequence = projection.getProjectedSequences().first()
        String sequenceName = projectionSequence.name

        int actualStart = projection.unProjectValue(start)
        int actualEnd = projection.unProjectValue(end)
        if (actualStart > actualEnd) {
            Long tmp = actualStart
            actualStart = actualEnd
            actualEnd = tmp
        }

        def samRecordList = samReader.query(sequenceName, actualStart, actualEnd, false).toList()
        for (SAMRecord samRecord in samRecordList.sort() { a, b -> a.start <=> b.start }) {
            JSONObject jsonObject = new JSONObject()

            // flag mappings:
            // FROM:
            // https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Store/SeqFeature/BAM/LazyFeature.js#L399-L412
            // TO:
            // https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/SAMFlag.java#L34-L45

            jsonObject.name = samRecord.readName
            jsonObject.seq = samRecord.readString
            jsonObject.seq_length = samRecord.readString.length()
            jsonObject.seq_reverse_complemented = samRecord.readNegativeStrandFlag
            jsonObject.secondary_alignment = samRecord.notPrimaryAlignmentFlag
            jsonObject.supplementary_alignment = samRecord.supplementaryAlignmentFlag
            jsonObject.qc_failed = samRecord.readFailsVendorQualityCheckFlag
            jsonObject.duplicate = samRecord.duplicateReadFlag


            jsonObject.unmapped = samRecord.readUnmappedFlag
            jsonObject.multi_segment_template = samRecord.readPairedFlag


            if (!jsonObject.unmapped) {
                jsonObject.start = projection.projectValue(samRecord.start)
                jsonObject.end = projection.projectValue(samRecord.end)
                jsonObject.strand = samRecord.readNegativeStrandFlag ? -1 : 1 // I thikn this is correct
                jsonObject.score = samRecord.mappingQuality
                jsonObject.qual = samRecord.baseQualities.join(" ")

//                jsonObject.mq= samRecord.cigarString // ?
                jsonObject.cigar = samRecord.cigarString
                jsonObject.length_on_ref = samRecord.readLength

                jsonObject.type = "match"

                if (jsonObject.start > jsonObject.end) {
                    Long tmp = jsonObject.start
                    jsonObject.start = jsonObject.end
                    jsonObject.end = tmp
                    jsonObject.strand = -jsonObject.strand
                }

            }
            if (jsonObject.multi_segment_template) {
                jsonObject.multi_segment_all_correctly_aligned = samRecord.properPairFlag
                jsonObject.multi_segment_next_segment_unmapped = samRecord.mateUnmappedFlag
                jsonObject.multi_segment_next_segment_reversed = samRecord.mateNegativeStrandFlag
                jsonObject.multi_segment_first = samRecord.firstOfPairFlag
                jsonObject.multi_segment_last = samRecord.secondOfPairFlag

                // uses next_ref_id
//                jsonObject.next_segment_position= samRecord.secondOfPairFlag
            }

            // reverse strand moved to negative strand
            jsonObject.remove("class")


            samRecord.getAttributes().each { attribute ->
                if (attribute.value instanceof Character) {
                    jsonObject[attribute.tag] = attribute.value.toString()
                } else {
                    jsonObject[attribute.tag] = attribute.value
                }
//                switch (attribute.tag){
//                    case 'XT':
//                        jsonObject[attribute.tag] = attribute.value.toString()
//                        break
//                    default:
//                        jsonObject[attribute.tag] = attribute.value
//                }
            }

            jsonObject.subfeatures = calculateMatches(projection, samRecord)

            // TODO: add projection
            jsonObject.mismatches = calculateMismatches(samRecord)
            if (jsonObject.MD) {
                handleMdMismatch(jsonObject, samRecord)
            }

            featuresArray.add(jsonObject)
        }

        return featuresArray

    }

    def handleMdMismatch(JSONObject featureObject, SAMRecord samRecord) {

        JSONArray filteredCigarMismatches = new JSONArray()
        featureObject.mismatches.each {
            if (!(it.type == 'deletion' || it.type == 'mismatch')) {
                filteredCigarMismatches.add(it)
            }
        }
        return handleMdMismatch3(featureObject, filteredCigarMismatches, samRecord)
    }

    int getTemplateCoord(int refCoord, Cigar cigar) {
        int templateOffset = 0
        int refOffset = 0

        log.debug "input refCoord ${refCoord}"


        for (CigarElement cigarElement in cigar.cigarElements) {
            if (refOffset > refCoord) {
                if (cigarElement.operator == CigarOperator.S || cigarElement.operator == CigarOperator.I) {
                    templateOffset += cigarElement.length
                } else if (cigarElement.operator == CigarOperator.D || cigarElement.operator == CigarOperator.P) {
                    refOffset += cigarElement.length
                } else {
                    templateOffset += cigarElement.length
                    refOffset += cigarElement.length
                }
            }
        }

        int returnValue = templateOffset - (refOffset - refCoord)
        log.debug "returnValue[${returnValue}]"

        return returnValue
    }

    /**
     * Returns new JSONObject
     *
     * @param cigarMismatches
     * @param generatedMismatch
     * @param mismatchRecords
     * @return
     */
    JSONObject nextRecord(JSONArray cigarMismatches, JSONObject generatedMismatch, JSONArray mismatchRecords) {
//        // correct the start of the current mismatch if it comes after a cigar skip
        for (JSONObject cigarMismatch in cigarMismatches) {
            if (cigarMismatch.type == 'skip' && generatedMismatch.start >= cigarMismatch.start) {
                generatedMismatch.start += cigarMismatch.length;
            }
        }

        // record it
        mismatchRecords.push(generatedMismatch);

        // get a new mismatch record ready
        JSONObject jsonObject = new JSONObject(
                start: generatedMismatch.start + generatedMismatch.length,
                length: 0,
                base: '',
                type: 'mismatch'
        )
        return jsonObject
//        curr = { start: curr.start + curr.length, length: 0, base: '', type: 'mismatch'};
    }

    /**
     *
     //        https://github.com/vsbuffalo/devnotes/wiki/The-MD-Tag-in-BAM-Files
     //        https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Store/SeqFeature/_MismatchesMixin.js

     // relevant pattern matching: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L785
     // relevant code process: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L819
     // match token as either


     // if the mdString
     //        mdString.findAll(/(\d+|\^[a-z]+|[a-z])/).each { token ->
     //
     //        }//        JSONObject jsonObject = new JSONObject(
     //                start: 0,
     //                base: '',
     //                length: 0,
     //                type: 'mismatch'
     //        )
     *
     *
     * @param featureObject
     * @param cigarMismatches
     * @param samRecord
     */

    JSONArray handleMdMismatch3(JSONObject featureObject, JSONArray cigarMismatches, SAMRecord samRecord) {
        log.debug "handling mismatch ${cigarMismatches as JSON}"
        log.debug "handling feature mismatch ${featureObject as JSON}"
        JSONArray mismatchRecords = new JSONArray()
        String md = featureObject.getString(SAMTag.MD.name())
        String seq = featureObject.getString('seq');

        final Matcher match = mdPat.matcher(md);

        Cigar cigar = samRecord.cigar

        int curSeqPos = 0
        int savedBases = 0
        int maxOutputLength = 0;
        final byte[] ret = new byte[maxOutputLength];
        for (final CigarElement cigarElement : cigar.getCigarElements()) {
            maxOutputLength += cigarElement.getLength();
        }
//        final byte[] seq = samRecord.getReadBases();
        int outIndex = 0


        JSONObject curr = new JSONObject(start: 0, base: '', length: 0, type: 'mismatch')


        for (final CigarElement cigEl : cigar.getCigarElements()) {
            final int cigElLen = cigEl.getLength();
            final CigarOperator cigElOp = cigEl.getOperator();

            int basesMatched = 0;

            // Do we have any saved matched bases?
//            while ((savedBases > 0) && (basesMatched < cigElLen)) {
//                ret[outIndex++] = seq[curSeqPos++];
//                savedBases--;
//                basesMatched++;
//            }

            while (basesMatched < cigElLen) {

                boolean matched = match.find()
                log.debug "is matched ${matched}"
                if (matched) {
                    String mg;
                    log.debug "is md ${md}"

                    if (((mg = match.group(1)) != null) && (!mg.isEmpty())) {
                        // It's a number , meaning a series of matches
                        log.debug "Group 1 in ${mg}"
                        final int num = Integer.parseInt(mg);
                        for (int i = 0; i < num; i++) {
//                            curr.start += parseInt( token );
                            if (basesMatched < cigElLen) {
//                                ret[outIndex++] = seq[curSeqPos++];
                            } else {
                                savedBases++;
                            }
                            basesMatched++;
                        }
                        curr.start += num
                        log.debug "Group 1 out ${mg}"
                    } else if (((mg = match.group(2)) != null) && (!mg.isEmpty())) {
                        // It's a single nucleotide, meaning a mismatch

                        log.debug "Group 2 in ${mg}"
                        for (int i = 0; i < mg.length(); i++) {
                            curr.length = 1;
                            // TODO: base is getting the wrong substring
                            log.debug "CURRENT START: ${curr.start} for ${seq}"

                            int startIndex
                            if(cigElOp){
                                log.debug "cigElOp ${cigElOp}"
                                startIndex = getTemplateCoord(curr.start, cigar)
                            }
                            else{
                                startIndex = curr.start
                            }
                            log.debug "startIndex ${startIndex}"

                            if(seq){
                                log.debug "seq ${seq}"
                                curr.base = seq.substring(startIndex, startIndex+1 )
                            }
                            else{
                                curr.base = CigarOperator.X.name()
                            }
                            log.debug "curr.base ${curr.base}"

                            curr.altbase = md

                            log.debug "new curr ${curr}"

                            curr = nextRecord(cigarMismatches, curr, mismatchRecords);
                        }


                        if (basesMatched < cigElLen) {
//                            ret[outIndex++] = StringUtil.charToByte(mg.charAt(0));
                            curSeqPos++
                        } else {
                            throw new IllegalStateException("Should never happen.");
                        }
                        basesMatched++;
                        log.debug "Group 2 out ${mg}"
                    } else if (((mg = match.group(3)) != null) && (!mg.isEmpty())) {
                        // It's a deletion, starting with a caret
                        // don't include caret
                        log.debug "Group 3 in ${mg}"

                        curr.length = mg.length() - 1
                        curr.base = '*'
                        curr.type = 'deletion'
                        curr.seq = mg.substring(1);
//                        nextRecord();
                        curr = nextRecord(cigarMismatches, curr, mismatchRecords);


                        basesMatched += mg.length() - 1;

                        // Check just to make sure.
                        if (basesMatched != cigElLen) {
                            throw new SAMException("Got a deletion in CIGAR (" + cigar + ", deletion " + cigElLen +
                                    " length) with an unequal ref insertion in MD (" + md + ", md " + basesMatched + " length");
                        }
                        if (cigElOp != CigarOperator.DELETION) {
                            throw new SAMException("Got an insertion in MD (" + md + ") without a corresponding deletion in cigar (" + cigar + ")");
                        }
                        log.debug "Group 3 out ${mg}"

                    } else {
                        log.debug "Group 4 not mateched all ${mg}"
                        matched = false;
                    }
                }
                if (!matched) {
                    throw new SAMException("Illegal MD pattern: " + md + " for read " + samRecord.getReadName() +
                            " with CIGAR " + samRecord.getCigarString());
                }
            }
        }

        log.debug "successful return ${mismatchRecords as JSON}"

        return mismatchRecords

    }

    /**
     * Emulates _MismatchesMixin.js :: _mdToMismatches
     *
     * @param featureObject
     * @param mismatches
     * @param samRecord
     * @return
     */
    def handleMdMismatch2(JSONObject featureObject, JSONArray mismatches, SAMRecord samRecord) {
        log.debug "handling mismatch ${mismatches as JSON}"
        log.debug "handling feature mismatch ${featureObject as JSON}"
        def mismatchRecords = new JSONArray()
        String mdString = featureObject.getString(SAMTag.MD.name())

        final Matcher match = mdPat.matcher(mdString);

        Cigar cigar = samRecord.cigar

        int curSeqPos = 0
        int savedBases = 0
        int maxOutputLength = 0;
        final byte[] ret = new byte[maxOutputLength];
        for (final CigarElement cigarElement : cigar.getCigarElements()) {
            maxOutputLength += cigarElement.getLength();
        }
        final byte[] seq = samRecord.getReadBases();
        int outIndex = 0

//        https://github.com/vsbuffalo/devnotes/wiki/The-MD-Tag-in-BAM-Files
//        https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Store/SeqFeature/_MismatchesMixin.js

        // relevant pattern matching: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L785
        // relevant code process: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L819
        while (match.find()) {

//            String mg;
//            if (((mg = match.group(1)) != null) && (!mg.isEmpty())) {
//                // It's a number , meaning a series of matches
//                final int num = Integer.parseInt(mg);
//                for (int i = 0; i < num; i++) {
//                    if (basesMatched < cigElLen) {
//                        ret[outIndex++] = seq[curSeqPos++];
//                    } else {
//                        savedBases++;
//                    }
//                    basesMatched++;
//                }
//            } else if (((mg = match.group(2)) != null) && (!mg.isEmpty())) {
//                // It's a single nucleotide, meaning a mismatch
//                if (basesMatched < cigElLen) {
//                    ret[outIndex++] = StringUtil.charToByte(mg.charAt(0));
//                    curSeqPos++;
//                } else {
//                    throw new IllegalStateException("Should never happen.");
//                }
//                basesMatched++;
//            } else if (((mg = match.group(3)) != null) && (!mg.isEmpty())) {
//                // It's a deletion, starting with a caret
//                // don't include caret
//                if (includeReferenceBasesForDeletions) {
//                    final byte[] deletedBases = StringUtil.stringToBytes(mg);
//                    System.arraycopy(deletedBases, 1, ret, outIndex, deletedBases.length - 1);
//                    outIndex += deletedBases.length - 1;
//                }
//                basesMatched += mg.length() - 1;
//
//                // Check just to make sure.
//                if (basesMatched != cigElLen) {
//                    throw new SAMException("Got a deletion in CIGAR (" + cigar + ", deletion " + cigElLen +
//                            " length) with an unequal ref insertion in MD (" + md + ", md " + basesMatched + " length");
//                }
//                if (cigElOp != CigarOperator.DELETION) {
//                    throw new SAMException("Got an insertion in MD (" + md + ") without a corresponding deletion in cigar (" + cigar + ")");
//                }
//
//            } else {
//                matched = false;
//            }
        }
        // match token as either

        // if the mdString
//        mdString.findAll(/(\d+|\^[a-z]+|[a-z])/).each { token ->
//
//        }
//        JSONObject jsonObject = new JSONObject(
//                start: 0,
//                base: '',
//                length: 0,
//                type: 'mismatch'
//        )
//        }
        return mismatches
    }

    /**
     *
     * Emulates the htsjdk / SequenceUtil.java
     *
     * @param featureObject
     * @param mismatches
     * @param samRecord
     * @return
     */
    def handleMdMismatch1(JSONObject featureObject, JSONArray mismatches, SAMRecord samRecord) {
        log.debug "handling mismatch ${mismatches as JSON}"
        log.debug "handling feature mismatch ${featureObject as JSON}"
        def mismatchRecords = new JSONArray()
        String mdString = featureObject.getString(SAMTag.MD.name())

        final Matcher match = mdPat.matcher(mdString);
        Cigar cigar = samRecord.cigar

        int curSeqPos = 0
        int savedBases = 0
        int maxOutputLength = 0;
        final byte[] ret = new byte[maxOutputLength];
        for (final CigarElement cigarElement : cigar.getCigarElements()) {
            maxOutputLength += cigarElement.getLength();
        }
        final byte[] seq = samRecord.getReadBases();
        int outIndex = 0;

        for (def cigEl in cigar.cigarElements) {
            final int cigElLen = cigEl.length
            final CigarOperator cigElOp = cigEl.operator
            if (cigElOp == CigarOperator.SKIPPED_REGION) {
                // We've decided that MD tag will not contain bases for skipped regions, as they
                // could be megabases long, so just put N in there if caller wants reference bases,
                // otherwise ignore skipped regions.
//                if (includeReferenceBasesForDeletions) {
//                    for (int i = 0; i < cigElLen; ++i) {
//                        ret[outIndex++] = N;
//                    }
//                }
            }
            // If it consumes reference bases, it's either a match or a deletion in the sequence
            // read.  Either way, we're going to need to parse through the MD.
            else if (cigElOp.consumesReferenceBases()) {
                // We have a match region, go through the MD
                int basesMatched = 0;

                // Do we have any saved matched bases?
                while ((savedBases > 0) && (basesMatched < cigElLen)) {
//                    ret[outIndex++] = seq[curSeqPos++];
                    savedBases--
                    basesMatched++
                }

                while (basesMatched < cigElLen) {
                    boolean matched = match.find();
                    if (matched) {
                        String mg;
                        if (((mg = match.group(1)) != null) && (!mg.isEmpty())) {
                            // It's a number , meaning a series of matches
                            final int num = Integer.parseInt(mg);
                            for (int i = 0; i < num; i++) {
                                if (basesMatched < cigElLen) {
                                    ret[outIndex++] = seq[curSeqPos++];
                                } else {
                                    savedBases++;
                                }
                                basesMatched++;
                            }
                        } else if (((mg = match.group(2)) != null) && (!mg.isEmpty())) {
                            // It's a single nucleotide, meaning a mismatch
                            if (basesMatched < cigElLen) {
                                ret[outIndex++] = StringUtil.charToByte(mg.charAt(0));
                                curSeqPos++;
                            } else {
                                throw new IllegalStateException("Should never happen.");
                            }
                            basesMatched++;
                        } else if (((mg = match.group(3)) != null) && (!mg.isEmpty())) {
                            // It's a deletion, starting with a caret
                            // don't include caret
//                            if (includeReferenceBasesForDeletions) {
//                                final byte[] deletedBases = StringUtil.stringToBytes(mg);
//                                System.arraycopy(deletedBases, 1, ret, outIndex, deletedBases.length - 1);
//                                outIndex += deletedBases.length - 1;
//                            }
//                            basesMatched += mg.length() - 1;

                            // Check just to make sure.
                            if (basesMatched != cigElLen) {
                                throw new SAMException("Got a deletion in CIGAR (" + cigar + ", deletion " + cigElLen +
                                        " length) with an unequal ref insertion in MD (" + md + ", md " + basesMatched + " length");
                            }
                            if (cigElOp != CigarOperator.DELETION) {
                                throw new SAMException("Got an insertion in MD (" + md + ") without a corresponding deletion in cigar (" + cigar + ")");
                            }

                        } else {
                            matched = false;
                        }
                    }

                    if (!matched) {
                        throw new SAMException("Illegal MD pattern: " + mdString + " for read " + samRecord.getReadName() +
                                " with CIGAR " + samRecord.getCigarString());
                    }
                }

            } else if (cigElOp.consumesReadBases()) {
                // We have an insertion in read
                for (int i = 0; i < cigElLen; i++) {
                    final char c = (cigElOp == CigarOperator.SOFT_CLIP) ? '0' : '-';
                    ret[outIndex++] = StringUtil.charToByte(c);
                    curSeqPos++;
                }
            } else {
                // It's an op that consumes neither read nor reference bases.  Do we just ignore??
            }


        }

        // match token as either

//        https://github.com/vsbuffalo/devnotes/wiki/The-MD-Tag-in-BAM-Files
//        https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Store/SeqFeature/_MismatchesMixin.js

        // relevant pattern matching: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L785
        // relevant code process: https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/util/SequenceUtil.java#L819

        // if the mdString
//        mdString.findAll(/(\d+|\^[a-z]+|[a-z])/).each { token ->
//
//        }
//        JSONObject jsonObject = new JSONObject(
//                start: 0,
//                base: '',
//                length: 0,
//                type: 'mismatch'
//        )
        return mismatches
    }

    def calculateMatches(MultiSequenceProjection projection, SAMRecord samRecord) {
        // TODO: put in a separate method
        JSONArray subfeatsArray = new JSONArray()
        if (!samRecord.cigar) {
            return subfeatsArray
        }

        Integer min = samRecord.start
        Integer max = null
//            log.debug "cigar string ${samRecord.cigarString}"
        for (CigarElement cigarElement in samRecord.cigar.cigarElements) {
//                log.debug "OP: '${cigarElement.operator.toString()}'"
            // parse matches for subfeatures
            switch (cigarElement.operator) {
                case CigarOperator.M:
                case CigarOperator.D:
                case CigarOperator.N:
                case CigarOperator.EQ:
                case CigarOperator.X:
                    max = min + cigarElement.length
                    break
                case CigarOperator.I:
                    max = min
                    break
                case CigarOperator.P:
                case CigarOperator.H:
                case CigarOperator.S:
                    break
            }

            if (max) {
                if (cigarElement.operator != CigarOperator.N) {
                    JSONObject subFeature = new JSONObject()
                    subFeature.type = cigarElement.operator.toString()
                    subFeature.start = min
                    subFeature.end = max
                    subFeature.strand = samRecord.readNegativeStrandFlag ? -1 : 1 // I thikn this is correct


                    subFeature.start = projection.projectValue(subFeature.start)
                    subFeature.end = projection.projectValue(subFeature.end)
                    if (subFeature.start > subFeature.end) {
                        Long tmp = subFeature.start
                        subFeature.start = subFeature.end
                        subFeature.end = tmp
                        subFeature.strand = -subFeature.strand
                    }


                    subFeature.cigar_op = samRecord.cigarString
                    subfeatsArray.add(subFeature)
                }
                min = max
            }

        }
        return subfeatsArray
    }

    def calculateMismatches(SAMRecord samRecord) {

        def cigarElements = samRecord.cigar.cigarElements
        JSONArray mismatches = new JSONArray()
        int currentOffset = 0

        for (CigarElement cigarElement in cigarElements) {
            switch (cigarElement.operator) {
                case CigarOperator.I:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "insertion"
                            , base: String.valueOf(cigarElement.length)
                            , length: 1
                    ))
                    break
                case CigarOperator.D:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "deletion"
                            , base: '*'
                            , length: cigarElement.length
                    ))
                    break
                case CigarOperator.N:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "skip"
                            , base: 'N'
                            , length: cigarElement.length
                    ))
                    break
                case CigarOperator.X:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "mismatch"
                            , base: 'N'
                            , length: cigarElement.length
                    ))
                    break
                case CigarOperator.H:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "hardclip"
                            , base: 'H' + cigarElement.length
                            , length: 1
                    ))
                    break
                case CigarOperator.S:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "softclip"
                            , base: 'S' + cigarElement.length
                            , cliplen: cigarElement.length
                            , length: 1
                    ))
                    break
            }

            switch (cigarElement.operator) {
                case CigarOperator.I:
                case CigarOperator.S:
                case CigarOperator.H:
                    break
                default:
                    currentOffset += cigarElement.length
            }

        }

        return mismatches
    }
}
