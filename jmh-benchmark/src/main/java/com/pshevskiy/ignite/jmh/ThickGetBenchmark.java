package com.pshevskiy.ignite.jmh;

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

import java.util.concurrent.TimeUnit;

import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;


@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ThickGetBenchmark extends IgniteBenchmark {

    private Ignite ignite;


    @Setup
    public void setup() throws IgniteCheckedException {
        ignite = startThickClient();
        createCaches(ignite, CACHE_NAME);
        generatePayload();
        createEntities(ignite);
    }


    @Benchmark
    public void transaction_get(Blackhole blackhole) {
        IgniteTransactions transactions = ignite.transactions();
        final IgniteCache<String, BinaryObject> cache = ignite.cache(CACHE_NAME).withKeepBinary();
        BinaryObject object;
        try (Transaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.READ_COMMITTED)) {
            object = cache.get(nextId());
            tx.commit();
        }
        checkData(blackhole, object);
    }


    @Benchmark
    public void get(Blackhole blackhole) {
        final IgniteCache<String, BinaryObject> cache = ignite.cache(CACHE_NAME).withKeepBinary();
        BinaryObject object = cache.get(nextId());
        checkData(blackhole, object);
    }

    @TearDown
    public void tearDown() {
        ignite.close();
    }


}