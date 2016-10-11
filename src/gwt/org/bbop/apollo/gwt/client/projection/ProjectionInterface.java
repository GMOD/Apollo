package org.bbop.apollo.gwt.client.projection;

/**
 * Created by nathandunn on 10/10/16.
 */
public interface ProjectionInterface {
    /**
     *
     * Probably just works on FeatureLocation
     *
     * @param input
     * @return
     */
    Long projectValue(Long input);

    Long projectReverseValue(Long input);


    Coordinate projectCoordinate(Long min, Long max);
    Coordinate projectReverseCoordinate(Long min, Long max);
    Long getLength();

    /**
     * This method projects a continuous sequence
     *
     * @param inputSequence
     * @param minCoordinate
     * @param maxCoordinate
     * @param offset
     * @return
     */
    String projectSequence(String inputSequence,Long minCoordinate,Long maxCoordinate,Long offset);

    Integer clear(); // remove coordinates
}
