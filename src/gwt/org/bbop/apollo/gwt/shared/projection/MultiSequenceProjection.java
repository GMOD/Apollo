package org.bbop.apollo.gwt.shared.projection;

import java.util.*;

/**
 * Created by nathandunn on 10/10/16.
 */
public class MultiSequenceProjection extends AbstractProjection {

    // if a projection includes multiple sequences, this will include greater than one

    private TreeMap<ProjectionSequence, DiscontinuousProjection> sequenceDiscontinuousProjectionMap = new TreeMap<>();

    // TODO: refactor into TrackProjection, not needed elsewhere
    public ProjectionChunkList projectionChunkList = new ProjectionChunkList();

    public static int DEFAULT_SCAFFOLD_BORDER_LENGTH = 0;

    /**
     *
     * @param input
     * @return
     */
    public ProjectionSequence getUnProjectedSequence(Long input) {
        if (input == null) {
            return null;
        }
        List<ProjectionSequence> projectionSequenceList = new ArrayList<>();
        for (ProjectionSequence projectionSequence : getProjectedSequences()) {
            Long bufferedLength = sequenceDiscontinuousProjectionMap.get(projectionSequence).getBufferedLength();
            if (input >= projectionSequence.getProjectedOffset() && input <= projectionSequence.getProjectedOffset() + bufferedLength) {
                projectionSequenceList.add(projectionSequence);
            }
        }
        // because the end-point is exclusive, we should always use the second sequence if there is an overlap
        return !projectionSequenceList.isEmpty() ? projectionSequenceList.get(projectionSequenceList.size() - 1) : null;
    }

    /**
     * return all project sequences from an unprojected range
     *
     * @param minInput
     * @param maxInput
     * @return
     */
    public List<ProjectionSequence> getProjectionSequences(Long minInput, Long maxInput) {
        List<ProjectionSequence> orderedSequences = new ArrayList<>();

        List<ProjectionSequence> projectionSequenceList = getProjectedSequences();
        for (int i = 0; i < projectionSequenceList.size(); i++) {
            ProjectionSequence projectionSequence = projectionSequenceList.get(i);
            if (!orderedSequences.contains(projectionSequence)) {
                if (minInput <= projectionSequence.getStart() && maxInput > projectionSequence.getStart() // left-edge
                        || minInput < projectionSequence.getEnd() && maxInput >= projectionSequence.getEnd() // right-edge
                        || minInput <= projectionSequence.getStart() && maxInput >= projectionSequence.getEnd() // outside
                        || minInput >= projectionSequence.getStart() && maxInput <= projectionSequence.getEnd() // inside
                        ) {
                    orderedSequences.add(projectionSequence);
                }
            }
        }
        return orderedSequences;
    }

    /**
     * Finds Project Sequences for projected min and max.
     * @param minInput
     * @param maxInput
     * @return
     */
    public List<ProjectionSequence> getUnProjectedSequences(Long minInput, Long maxInput) {
        List<ProjectionSequence> orderedSequences = new ArrayList<>();

        ProjectionSequence minProjectionSequence = getUnProjectedSequence(minInput);
        ProjectionSequence maxProjectionSequence = getUnProjectedSequence(maxInput);

        // TODO this is hacky as we should be more accurately determining this by using the offset
        Integer startOrder = minProjectionSequence != null ? minProjectionSequence.getOrder() : null;
        if (startOrder == null) {
            startOrder = 0;
        }
        Integer endOrder = maxProjectionSequence != null ? maxProjectionSequence.getOrder() : null;
        if (endOrder == null) {
            endOrder = getLastSequence().getOrder();
        }

        for (ProjectionSequence projectionSequence : getProjectedSequences()) {
            if (projectionSequence.getOrder() >= startOrder && projectionSequence.getOrder() <= endOrder) {
                orderedSequences.add(projectionSequence);
            }
        }

        return orderedSequences;
    }

    /**
     * Find which sequence I am on by iterating over coordinates.
     * <p>
     * I want to return the first projection sequence where the start / end contains the input
     *
     * @param input
     * @return
     */

    public ProjectionSequence getProjectionSequence(Long input) {

        Long offset = 0l;
        // should deliver these : order

        for (List<ProjectionSequence> projectionSequenceList : getOrderedSequences().values()) {
            for (ProjectionSequence projectionSequence : projectionSequenceList) {
                if (input >= projectionSequence.getStart() + offset && input <= projectionSequence.getEnd() + offset) {
                    return projectionSequence;
                }
            }
            // this is if the projectionsequences belong to the same sequence
            offset += projectionSequenceList.get(0).getUnprojectedLength();
        }

        return null;
    }

    /**
     * @return Returns the relative order order of scaffolds : relation to each other.
     */
    TreeMap<Integer, List<ProjectionSequence>> getOrderedSequences() {
        Map<String, Integer> orderedMap = getOrderedSequenceMap();

        TreeMap<Integer, List<ProjectionSequence>> map = new TreeMap<>();

        for (ProjectionSequence projectionSequence : getProjectedSequences()) {
            Integer order = orderedMap.get(projectionSequence.getName());
            List<ProjectionSequence> projectList = map.containsKey(order) ? map.get(order) : new ArrayList<ProjectionSequence>();
            projectList.add(projectionSequence);
            map.put(order, projectList);
        }

        return map;
    }

    public Long projectValue(Long input, Long inputOffset, Long outputOffset) {
        ProjectionSequence projectionSequence = getProjectionSequence(input);
        if (projectionSequence == null) {
            return UNMAPPED_VALUE;
        }
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);
        // TODO: DEFAULT_FOLDING_BUFFER for scaffolds is currently 1 . . the order
        Long returnValue = discontinuousProjection.projectValue(input - inputOffset);
        if (projectionSequence.getReverse() && !returnValue.equals(UNMAPPED_VALUE)) {
            returnValue = discontinuousProjection.getLength() + (discontinuousProjection.size() - 1) - returnValue;
        }
        return returnValue.equals(UNMAPPED_VALUE) ? returnValue : returnValue + outputOffset;
    }

    public Long getLengthForSequence(ProjectionSequence projectionSequence) {
        if (!sequenceDiscontinuousProjectionMap.containsKey(projectionSequence)) {
            return null;
        }
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).getLength();
    }

    public Long projectValue(Long input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input);
        if (projectionSequence == null) {
            return UNMAPPED_VALUE;
        }
        return projectValue(input, projectionSequence.getOriginalOffset(), projectionSequence.getProjectedOffset());
    }

    public Long projectLocalReverseValue(Long input) {
        ProjectionSequence projectionSequence = getUnProjectedSequence(input);
        if (projectionSequence == null) {
            return UNMAPPED_VALUE;
        }
        Long reverseValue = projectReverseValue(input, projectionSequence.getProjectedOffset(), projectionSequence.getOriginalOffset());
        if (projectionSequence.getReverse()) {
            // simplifies to this
            return projectionSequence.getLength() - reverseValue + projectionSequence.getOriginalOffset() + 2 * projectionSequence.getStart();
        } else {
            return reverseValue - projectionSequence.getOriginalOffset();
        }
    }

    public Long unProjectValue(Long input) {
        ProjectionSequence projectionSequence = getUnProjectedSequence(input);
        if (projectionSequence == null) {
            return UNMAPPED_VALUE;
        }
        return projectReverseValue(input, projectionSequence.getProjectedOffset(), projectionSequence.getOriginalOffset());
    }

    public Long projectReverseValue(Long input, Long inputOffset, Long outputOffset) {
        ProjectionSequence projectionSequence = getUnProjectedSequence(input);
        if (projectionSequence == null) {
            return UNMAPPED_VALUE;
        }
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);

        if (projectionSequence.getReverse()) {
            // need to flip the reverse value : the context of the projection sequence
            // length - ( i - offset ) + offset
            // length - i + (2 * offset)
            Long alteredInput = discontinuousProjection.getBufferedLength(1) - input + projectionSequence.getProjectedOffset();
            return discontinuousProjection.unProjectValue(alteredInput) + outputOffset;
        } else {
            return discontinuousProjection.unProjectValue(input - inputOffset) + outputOffset;
        }
    }

    public Long getLength() {
        Map.Entry<ProjectionSequence, DiscontinuousProjection> entry = sequenceDiscontinuousProjectionMap.lastEntry();
        return entry.getKey().getProjectedOffset() + entry.getValue().getLength();
    }

    @Override
    public String projectSequence(String inputSequence, Long minCoordinate, Long maxCoordinate, Long offset) {
        Long index = 0l;
        List<String> sequenceList = new ArrayList<>();

        // we start at the very bottom and go up
        for (ProjectionSequence projectionSequence : getProjectedSequences()) {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);
            Long sequenceLength = projectionSequence.getUnprojectedLength();
            offset = index;

            // case 5: no overlap
            if (index > maxCoordinate || index + sequenceLength < minCoordinate) {
                // do nothing
            }
            // case 3: inbetween
            else if (minCoordinate > index && maxCoordinate < index + sequenceLength) {
                sequenceList.add(discontinuousProjection.projectSequence(inputSequence, minCoordinate - index + offset, maxCoordinate - index + offset, offset));
            }
            // case 1: right edge
            else if (minCoordinate > index && maxCoordinate >= index + sequenceLength) {
                sequenceList.add(discontinuousProjection.projectSequence(inputSequence, minCoordinate - index + offset, sequenceLength + offset, offset));
            }
            // case 2: left edge
            else if (minCoordinate <= index && maxCoordinate < sequenceLength + index) {
                sequenceList.add(discontinuousProjection.projectSequence(inputSequence, 0 + offset, maxCoordinate - index + offset, offset));
            }
            // case 4: overlap / all
            else if (minCoordinate <= index && maxCoordinate >= index + sequenceLength) {
                sequenceList.add(discontinuousProjection.projectSequence(inputSequence, 0 + offset, sequenceLength + offset, offset));
            }
//            else{
//                throw new RuntimeException("Should not get here: ${minCoordinate},${maxCoordinate}")
//            }
            index += sequenceLength;
        }

        String returnString = "";
        for (String sequence : sequenceList) {
            returnString += sequence;
        }
        // not really used .  .. .  but otherwise would carve up into different bits
//        return StringUtils.join(sequenceList, "");
        return returnString;
    }

    public List<Coordinate> listCoordinates() {
        List<Coordinate> coordinateList = new ArrayList<>();
        for (DiscontinuousProjection projection : sequenceDiscontinuousProjectionMap.values()) {
            coordinateList.addAll(projection.minMap.values());
        }
        return coordinateList;
    }

    public void addInterval(Long min, Long max, ProjectionSequence sequence) {
        Coordinate coordinate = new Coordinate(min, max, sequence);
        addCoordinate(coordinate);
    }


    public Integer size() {
        Integer count = 0;
        for (DiscontinuousProjection projection : sequenceDiscontinuousProjectionMap.values()) {
            count += projection.size();
        }

        return count;
    }

    public void addCoordinates(List<Coordinate> coordinates) {
        for (Coordinate coordinate : coordinates) {
            addCoordinate(coordinate);
        }
    }

    @Override
    public Integer clear() {
        int size = sequenceDiscontinuousProjectionMap.size();
        sequenceDiscontinuousProjectionMap.clear();
        return size;
    }

    // here we are adding a location to project
    public DiscontinuousProjection addCoordinate(Coordinate coordinate) {
        // if a single projection . . the default .. then assert that it is the same sequence / projection
        ProjectionSequence projectionSequence = getProjectionSequenceForCoordinate(coordinate);
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);
        if (discontinuousProjection != null) {
            discontinuousProjection.addInterval(coordinate.getMin(), coordinate.getMax(), 0);
            return discontinuousProjection;
        } else {
            DiscontinuousProjection thisDiscontinuousProjection = new DiscontinuousProjection();
            thisDiscontinuousProjection.addInterval(coordinate.getMin(), coordinate.getMax(), 0);
            sequenceDiscontinuousProjectionMap.put(projectionSequence, thisDiscontinuousProjection);
            return thisDiscontinuousProjection;
        }
    }

    /**
     * Finds the most appropriate sequence projection for a given location
     *
     * @param coordinate
     * @return
     */
    private ProjectionSequence getProjectionSequenceForCoordinate(Coordinate coordinate) {
        ProjectionSequence matchSequence = coordinate.getSequence();
        TreeMap<Integer, ProjectionSequence> projectionSequenceTreeMap = new TreeMap<>();
        for (ProjectionSequence it : sequenceDiscontinuousProjectionMap.keySet()) {
//        sequenceDiscontinuousProjectionMap.keySet().each {
            int score = it.getName().equals(matchSequence.getName()) ? 1 : 0;
            score += it.getStart().equals(matchSequence.getStart()) ? 1 : 0;
            score += it.getEnd().equals(matchSequence.getEnd()) ? 1 : 0;
            projectionSequenceTreeMap.put(score, it);
        }
        return projectionSequenceTreeMap.lastEntry().getValue();
    }

    /**
     * This is done at the end to make offsets render properly
     */
    public List<ProjectionSequence> calculateOffsets() {
        Integer currentOrder = 0;
        Long lastLength = 0L;
        Long originalLength = 0L;
//        def projectionSequences = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b ->
//                a.order <=> b.order
//        }

        List<ProjectionSequence> projectionSequences = getProjectedSequences();

        // generate set of projection sequences
        // they may have different offsets, but different originalOffset
        Map<String, Long> originalOffsetMap = generateOriginalOffsetsForSequences(projectionSequences);

        for (ProjectionSequence projectionSequence : projectionSequences) {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);

            projectionSequence.setProjectedOffset(lastLength);
            projectionSequence.setOriginalOffset(originalOffsetMap.get(projectionSequence.getName()));

            assert projectionSequence.getUnprojectedLength() != null;
            assert projectionSequence.getUnprojectedLength() > 0;
            if (discontinuousProjection != null) {
                lastLength += discontinuousProjection.getBufferedLength();
            }
            lastLength += DEFAULT_SCAFFOLD_BORDER_LENGTH;
            ++currentOrder;
        }

        return projectionSequences;
    }

    private
    static Map<String, Long> generateOriginalOffsetsForSequences(List<ProjectionSequence> projectionSequences) {
        Map<String, Long> returnMap = new HashMap<>();
        Long originalOffset = 0l;

        for (ProjectionSequence it : projectionSequences) {
            if (!returnMap.containsKey(it.getName())) {
                returnMap.put(it.getName(), originalOffset);
                originalOffset += it.getUnprojectedLength();
            }
        }

        return returnMap;
    }

    public ProjectionSequence getProjectionSequence(String sequenceName, String organismName) {
        return getProjectionSequence(sequenceName, null, organismName);
    }

    public ProjectionSequence getProjectionSequence(String sequenceName, String sequenceId, String organismName) {
        for (ProjectionSequence projectionSequence : sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence.getName().equals(sequenceName)) {
                if (projectionSequence.getOrganism() != null && organismName != null) {
                    if (!projectionSequence.getOrganism().equals(organismName)) {
                        return projectionSequence;
                    }
                }
                if (projectionSequence.getId() != null && sequenceId != null) {
                    if (!projectionSequence.getId().equals(sequenceId)) {
                        return null;
                    }
                }
                return projectionSequence;
            }
        }
        return null;
    }


    public Boolean containsSequence(String sequenceName, String organismName) {
        return getProjectionSequence(sequenceName, null, organismName) != null;
    }


    public String toString() {
        return "MultiSequenceProjection{" +
                "sequenceDiscontinuousProjectionMap=" + sequenceDiscontinuousProjectionMap +
                '}';
    }


    TreeMap<Long, Coordinate> getMinMap() {
        TreeMap<Long, Coordinate> minMap = new TreeMap<>();
        List<ProjectionSequence> projectionSequenceList = getProjectedSequences();

        for (ProjectionSequence projectionSequence : projectionSequenceList) {
            Map<Long, Coordinate> returnMap = new TreeMap<>();

            for (Map.Entry<Long, Coordinate> it : sequenceDiscontinuousProjectionMap.get(projectionSequence).minMap.entrySet()) {
                Coordinate coordinate = new Coordinate(it.getValue().getMin(), it.getValue().getMax());
                coordinate.addOffset(projectionSequence.getOriginalOffset());
                returnMap.put(it.getKey() + projectionSequence.getOriginalOffset(), coordinate);
            }
            minMap.putAll(returnMap);
        }

        return minMap;
    }

    TreeMap<Long, Coordinate> getMaxMap() {
        TreeMap<Long, Coordinate> maxMap = new TreeMap<>();

        for (ProjectionSequence projectionSequence : getProjectedSequences()) {
            Map<Long, Coordinate> returnMap = new TreeMap<>();
            // add a set with an offset

            for (Map.Entry<Long, Coordinate> it : sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.entrySet()) {
//            sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.each {
                Coordinate coordinate = new Coordinate(it.getValue().getMin(), it.getValue().getMax());
                coordinate.addOffset(projectionSequence.getOriginalOffset());
                returnMap.put(it.getKey() + projectionSequence.getOriginalOffset(), coordinate);
            }
            maxMap.putAll(returnMap);
        }

        return maxMap;
    }

    public Coordinate getMaxCoordinate() {
        return getMaxMap().lastEntry().getValue();
    }

    public Coordinate getMinCoordinate() {
        return getMinMap().firstEntry().getValue();
    }

    public Coordinate getMaxCoordinate(ProjectionSequence projectionSequence) {
        assert projectionSequence != null;
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.lastEntry().getValue();
    }

    //
    public Coordinate getMinCoordinate(ProjectionSequence projectionSequence) {
        assert projectionSequence != null;
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).minMap.firstEntry().getValue();
    }

    public Long getOffsetForSequence(String sequenceName) {
        if (projectionChunkList != null) {
            ProjectionChunk projectionChunk = projectionChunkList.findProjectChunkForName(sequenceName);
            if (projectionChunk != null) {
                return projectionChunk.getSequenceOffset();
            }
        }
        for (ProjectionSequence projectionSequence : sequenceDiscontinuousProjectionMap.keySet()) {
            if (sequenceName.equals(projectionSequence.getName())) {
                return projectionSequence.getOriginalOffset();
            }
        }
        return 0L;
    }

    ProjectionSequence getLastSequence() {
        return getProjectedSequences().get(sequenceDiscontinuousProjectionMap.keySet().size() - 1);
    }

    public List<ProjectionSequence> getProjectedSequences() {
//        List<ProjectionSequence> orderedSequences = sequenceDiscontinuousProjectionMap.keySet().sort() {
//            a, b -> a.order <= > b.order
//        }
        List<ProjectionSequence> orderedSequences = new ArrayList<>();
        orderedSequences.addAll(sequenceDiscontinuousProjectionMap.keySet());
        return orderedSequences;
    }

    /**
     * Returns the first entry of a ProjectionSequence
     *
     * @return
     */
    public Map<String, Integer> getOrderedSequenceMap() {
        Map<String, Integer> returnMap = new HashMap<>();
        for (ProjectionSequence projectionSequence : sequenceDiscontinuousProjectionMap.keySet()) {
            if (!returnMap.containsKey(projectionSequence.getName())) {
                returnMap.put(projectionSequence.getName(), projectionSequence.getOrder());
            }
        }
        return returnMap;
    }

    Boolean overlaps(ProjectionSequence projectionSequenceA, ProjectionSequence projectionSequenceB) {
        assert projectionSequenceA.getName().equals(projectionSequenceB.getName());
        if (projectionSequenceA.getStart() <= projectionSequenceB.getStart() && projectionSequenceA.getEnd() >= projectionSequenceB.getStart()) {
            return true;
        }
        if (projectionSequenceA.getStart() <= projectionSequenceB.getEnd() && projectionSequenceA.getStart() >= projectionSequenceB.getEnd()) {
            return true;
        }
        return false;
    }

    ProjectionSequence overlaps(ProjectionSequence projectionSequence) {
        for (ProjectionSequence aProjSequence : getProjectedSequences()) {
            if (aProjSequence.getName().equals(projectionSequence.getName())) {
                if (overlaps(aProjSequence, projectionSequence)) {
                    return aProjSequence;
                }
            }
        }
        return null;
    }

    /**
     * Merge ProjectionSequenceA with ProjectionSequenceB
     * <p>
     * We assume that they are overlapped and belong to the same one.
     * <p>
     * Return ProjectionSequenceA
     *
     * @param projectionSequenceA
     * @param projectionSequenceB
     * @return
     */
    ProjectionSequence merge(ProjectionSequence projectionSequenceA, ProjectionSequence projectionSequenceB) {
        if (projectionSequenceB.getStart() < projectionSequenceA.getStart()) {
            projectionSequenceA.setStart(projectionSequenceB.getStart());
        }
        if (projectionSequenceB.getEnd() > projectionSequenceA.getEnd()) {
            projectionSequenceA.setEnd(projectionSequenceB.getEnd());
        }

        return projectionSequenceA;
    }

    public void addProjectionSequences(Collection<ProjectionSequence> theseProjectionSequences) {
        for (ProjectionSequence it : theseProjectionSequences) {
            ProjectionSequence overlappingProjectionSequence = overlaps(it);
            if (overlappingProjectionSequence != null) {
                sequenceDiscontinuousProjectionMap.remove(overlappingProjectionSequence);
                overlappingProjectionSequence = merge(overlappingProjectionSequence, it);
                sequenceDiscontinuousProjectionMap.put(overlappingProjectionSequence, null);
            } else {
                sequenceDiscontinuousProjectionMap.put(it, null);
            }
        }
    }

    /**
     * - No overlap (just merge)
     * - Discontinuous projection must be contained within the ProjectionSequence
     * - Projection Sequence must be sequentially ordered
     *
     * @return
     */
    public Boolean isValid() {

        Map<String, Boolean> reverseMap = new HashMap<>();

        for (ProjectionSequence projectionSequence : getProjectedSequences()) {
            if (reverseMap.containsKey(projectionSequence.getName())) {
                if (!reverseMap.get(projectionSequence.getName()).equals(projectionSequence.getReverse())) {
                    return false;
                }
            } else {
                reverseMap.put(projectionSequence.getName(), projectionSequence.getReverse());
            }
        }
        return true;
    }

    public DiscontinuousProjection getProjectionForSequence(ProjectionSequence projectionSequence) {
        return sequenceDiscontinuousProjectionMap.get(projectionSequence);
    }

    public TreeMap<ProjectionSequence, DiscontinuousProjection> getSequenceDiscontinuousProjectionMap() {
        return sequenceDiscontinuousProjectionMap;
    }

    public Coordinate getCoordinateForPosition(Long input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input);
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);
        return discontinuousProjection.getCoordinateForInput(input);
    }


    public Coordinate replaceCoordinate(Coordinate coordinate, Long min, Long max) {
        // TODO: handle cross-sequence?
        ProjectionSequence projectionSequence = getProjectionSequence(min);
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence);
        return discontinuousProjection.replaceCoordinate(coordinate, min, max);
    }

}
