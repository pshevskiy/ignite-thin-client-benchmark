package com.pshevskiy.ignite.server.tasks.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.ignite.binary.BinaryObject;

@AllArgsConstructor
@Builder
@Getter
public class Operation {

    private final String cacheName;
    private final Object key;
    private final BinaryObject object;

    private final boolean rewriteSameVersion;


}
