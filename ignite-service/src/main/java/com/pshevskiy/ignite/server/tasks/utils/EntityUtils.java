package com.pshevskiy.ignite.server.tasks.utils;

import com.pshevskiy.ignite.server.tasks.model.Status;
import org.apache.ignite.binary.BinaryObject;

public class EntityUtils {
    public static final String VERSION = "version";

    private EntityUtils() {
    }

    public static Status checkVersion(BinaryObject currentEntity,
                                      BinaryObject newEntity,
                                      boolean rewriteSameVersion) {
        if (currentEntity == null) {
            return Status.ADDED;
        } else {
            int currentVersion = currentEntity.field(VERSION);
            int newVersion = newEntity.field(VERSION);
            if (rewriteSameVersion && currentVersion == newVersion) {
                return Status.REWRITE_SAME_VERSION;
            } else if (currentVersion >= newVersion) {
                return Status.ALREADY_EXIST;
            } else {
                return Status.REWRITE_NEWER_VERSION;
            }
        }
    }
}
