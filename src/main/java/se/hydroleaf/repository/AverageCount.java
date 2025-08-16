package se.hydroleaf.repository;

public record AverageCount(Double average, Long count) {
    public Double getAverage() { return average; }
    public Long getCount() { return count; }
}
