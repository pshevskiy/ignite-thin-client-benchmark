package com.pshevskiy.ignite.server.tasks.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class BulkOperation {

    private final List<Operation> operations;

}
