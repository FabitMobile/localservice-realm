package ru.parking.localservice_realm;


public enum AggregationFunction {
    MAX("max"),
    MIN("min"),
    SUM("sum"),
    SIZE("size"),
    AVERAGE("average");

    private final String text;

    AggregationFunction(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static AggregationFunction findItemByValue(String stringValue) {
        for (AggregationFunction issueStatus : AggregationFunction.values()) {
            if (issueStatus.toString().equals(stringValue)) {
                return issueStatus;
            }
        }
        return null;
    }
}