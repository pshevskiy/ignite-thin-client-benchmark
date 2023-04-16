package com.pshevskiy.ignite.server.tasks.model;

public enum Status {

    ADDED(true),
    REWRITE_SAME_VERSION(true),
    REWRITE_NEWER_VERSION(true),
    ALREADY_EXIST(false),
    ERROR(false);

    private final boolean isDataChanged;

    Status(boolean isDataChanged) {
        this.isDataChanged = isDataChanged;
    }

    public boolean isDataChanged() {
        return isDataChanged;
    }

}
