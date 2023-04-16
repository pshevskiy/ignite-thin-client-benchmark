package com.pshevskiy.ignite.jmh;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.ClientTransactions;
import org.apache.ignite.client.IgniteClient;
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
public class ThinGetBenchmark extends IgniteBenchmark {

    private IgniteClient client;
    private IgniteClient clientPA;


    @Setup
    public void setup() throws IgniteCheckedException {
        client = startThinClient(IGNITE_IP_ADDRESS_LIST);
        clientPA = startThinPAClient(IGNITE_IP_ADDRESS_LIST);
        createCaches(client, CACHE_NAME);
        generatePayload();
        createEntities(client);
    }


    @Benchmark
    public void transaction_get(Blackhole blackhole) {
        final ClientTransactions transactions = client.transactions();
        final ClientCache<String, BinaryObject> cache = client.cache(CACHE_NAME).withKeepBinary();
        BinaryObject object;
        try (ClientTransaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.READ_COMMITTED)) {
            object = cache.get(nextId());
            tx.commit();
        }
        checkData(blackhole, object);
    }


    @Benchmark
    public void get(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = client.cache(CACHE_NAME).withKeepBinary();
        BinaryObject object = cache.get(nextId());
        checkData(blackhole, object);
    }

    @Benchmark
    public void get_pa(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = clientPA.cache(CACHE_NAME).withKeepBinary();
        BinaryObject object = cache.get(nextId());
        checkData(blackhole, object);
    }

    @TearDown
    public void tearDown() {
        client.close();
        clientPA.close();
    }


}