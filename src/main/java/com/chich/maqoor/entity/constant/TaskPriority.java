package com.chich.maqoor.entity.constant;

public enum TaskPriority {
    LOW("Low", "text-success"),
    MEDIUM("Medium", "text-warning"),
    HIGH("High", "text-danger"),
    URGENT("Urgent", "text-danger fw-bold");

    private final String displayName;
    private final String cssClass;

    TaskPriority(String displayName, String cssClass) {
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
