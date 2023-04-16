package com.pshevskiy.ignite.server.tasks;

import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Operation;
import com.pshevskiy.ignite.server.tasks.model.Status;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionIsolation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.pshevskiy.ignite.server.tasks.utils.EntityUtils.checkVersion;
import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;

public class AtomicPut extends ComputeTaskAdapter<BulkOperation, List<Status>> {

    public static final String VERSION = "version";

    @IgniteInstanceResource
    private Ignite ignite;


    public static class PutJob extends ComputeJobAdapter {


        private final List<Operation> params;

        @IgniteInstanceResource
        private Ignite ignite;


        public PutJob(List<Operation> params) {
            this.params = params;
        }

        @Override
        public Object execute() throws IgniteException {

            IgniteTransactions transactions = ignite.transactions();
            List<Status> result = new ArrayList<>(params.size());
            for (Operation param : params) {
                final IgniteCache<Object, BinaryObject> cache = ignite.cache(param.getCacheName());
                Status status;

                try (Transaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                    final BinaryObject currentEntity = cache.get(param.getKey());
                    status = checkVersion(currentEntity, param.getObject(), param.isRewriteSameVersion());
                    if (status.isDataChanged()) {
                        cache.put(param.getKey(), param.getObject());
                    }
                    tx.commit();
                } catch (Exception e) {
                    status = Status.ERROR;
                }
                result.add(status);
            }

            return result;
        }
    }


    @Override
    public @NotNull Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, @Nullable BulkOperation args) throws IgniteException {
        Map<ClusterNode, List<Operation>> jobs = new HashMap<>();
        for (Operation operation : args.getOperations()) {
            final Affinity<Object> affinity = ignite.affinity(operation.getCacheName());
            final ClusterNode clusterNode = affinity.mapKeyToNode(operation.getKey());

            List<Operation> list = jobs.computeIfAbsent(clusterNode, k -> new ArrayList<>());
            list.add(operation);
        }

        return jobs.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> new PutJob(entry.getValue()), Map.Entry::getKey));
    }


    @Override
    public @Nullable List<Status> reduce(List<ComputeJobResult> results) throws IgniteException {
        List<Status> reduceResult = new ArrayList<>();
        results.forEach(res -> reduceResult.addAll(res.getData()));
        return reduceResult;
    }
}
