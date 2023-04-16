package com.pshevskiy.ignite.server.tasks;

import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Operation;
import com.pshevskiy.ignite.server.tasks.model.Status;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionIsolation;

import java.util.ArrayList;
import java.util.List;

import static com.pshevskiy.ignite.server.tasks.utils.EntityUtils.checkVersion;
import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;

public class AtomicPutService implements PutService, Service {

    @IgniteInstanceResource
    private Ignite ignite;

    @Override
    public List<Status> put(BulkOperation paramsList) {
        IgniteTransactions transactions = ignite.transactions();
        List<Status> result = new ArrayList<>(paramsList.getOperations().size());
        for (Operation params : paramsList.getOperations()) {
            final IgniteCache<Object, BinaryObject> cache = ignite.cache(params.getCacheName());
            Status status;

            try (Transaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                final BinaryObject currentEntity = cache.get(params.getKey());
                final Status putStatus = checkVersion(currentEntity, params.getObject(), params.isRewriteSameVersion());
                if (putStatus.isDataChanged()) {
                    cache.put(params.getKey(), params.getObject());
                }
                tx.commit();
                status = putStatus;
            } catch (Exception e) {
                status = Status.ERROR;
            }
            result.add(status);
        }
        return result;
    }


}
