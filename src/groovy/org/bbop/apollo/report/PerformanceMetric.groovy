package org.bbop.apollo.report

/**
 * Created by nathandunn on 7/21/15.
 */
class PerformanceMetric{
    String className
    String methodName
    Integer count
    Float min, max, mean, stddev
    Float total
    Float totalPercent
}
