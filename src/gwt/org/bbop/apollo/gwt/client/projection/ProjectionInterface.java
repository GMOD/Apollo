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
    Integer projectValue(Integer input);

    Integer projectReverseValue(Integer input);


    Coordinate projectCoordinate(int min, int max);
    Coordinate projectReverseCoordinate(int min, int max);
    Integer getLength();

    /**
     * This method projects a continuous sequence
     *
     * @param inputSequence
     * @param minCoordinate
     * @param maxCoordinate
     * @param offset
     * @return
     */
    String projectSequence(String inputSequence,Integer minCoordinate,Integer maxCoordinate,Integer offset);

    Integer clear(); // remove coordinates
}
