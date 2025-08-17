package se.hydroleaf.repository.dto;

/**
 * Projection for live-now aggregation rows.
 */
public interface LiveNowRow {
    String getSystem();
    String getLayer();
    String getType();
    Double getAverage();
    Long getCount();
}