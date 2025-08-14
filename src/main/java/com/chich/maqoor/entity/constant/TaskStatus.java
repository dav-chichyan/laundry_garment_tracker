package com.chich.maqoor.entity.constant;

public enum TaskStatus {
    TODO("To Do", "bg-secondary"),
    IN_PROGRESS("In Progress", "bg-primary"),
    REVIEW("Review", "bg-info"),
    DONE("Done", "bg-success");

    private final String displayName;
    private final String cssClass;

    TaskStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
