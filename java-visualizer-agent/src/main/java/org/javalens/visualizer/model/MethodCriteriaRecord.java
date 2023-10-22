package org.javalens.visualizer.model;

public record MethodCriteriaRecord(String eventName, Type type, MethodCriteria methodCriteria) {

    public enum Type {
        START, END
    }

}
