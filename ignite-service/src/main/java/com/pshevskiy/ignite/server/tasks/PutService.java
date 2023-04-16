package com.pshevskiy.ignite.server.tasks;

import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Status;

import java.util.List;

public interface PutService {

    List<Status> put(BulkOperation putParamsList);
}
