package com.pshevskiy.ignite.server.tasks.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.ignite.binary.BinaryObject;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Entity {
    @EqualsAndHashCode.Include
    private String id;
    private String collocationId;
    private int version;
    private long flags;
    private byte[] binaryData;

    public Entity(BinaryObject bo) {
        id = bo.field("id");
        collocationId = bo.field("collocationId");
        version = bo.field("version");
        flags = bo.field("flags");
        binaryData = bo.field("binaryData");
    }
}
