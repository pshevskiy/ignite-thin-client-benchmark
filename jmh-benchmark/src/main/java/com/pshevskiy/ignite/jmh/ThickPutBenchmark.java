package com.pshevskiy.ignite.jmh;

import com.pshevskiy.ignite.server.tasks.model.Entity;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionIsolation;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import javax.cache.Cache;
import java.util.concurrent.TimeUnit;

import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;


@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ThickPutBenchmark extends IgniteBenchmark {
    private Ignite ignite;

    @Setup
    public void setup() throws IgniteCheckedException {
        ignite = startThickClient();
        createCaches(ignite, CACHE_NAME);
        generatePayload();
    }


    @Benchmark
    public void transaction_put(Blackhole blackhole) {
        IgniteTransactions transactions = ignite.transactions();
        final IgniteCache<String, BinaryObject> cache = ignite.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);

        try (Transaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.READ_COMMITTED)) {
            cache.put(key, ignite.binary().toBinary(entity));
            tx.commit();
            blackhole.consume(tx);
        }
    }


    @Benchmark
    public void put(Blackhole blackhole) {
        final IgniteCache<String, BinaryObject> cache = ignite.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        cache.put(key, ignite.binary().toBinary(entity));
        blackhole.consume(entity);
    }


    @Benchmark
    public void optimistic_put(Blackhole blackhole) {
        final Cache<String, BinaryObject> cache = ignite.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        final BinaryObject newObject = ignite.binary().toBinary(entity);

        final IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
            cache.get(key);
            // logic
            cache.put(key, newObject);
            tx.commit();
            blackhole.consume(tx);
        }
    }


    @TearDown
    public void tearDown() {
        ignite.close();
    }


}