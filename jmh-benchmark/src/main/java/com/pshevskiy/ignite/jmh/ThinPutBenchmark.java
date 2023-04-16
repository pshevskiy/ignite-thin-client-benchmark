package com.pshevskiy.ignite.jmh;

import com.pshevskiy.ignite.server.tasks.model.Entity;
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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.ignite.transactions.TransactionConcurrency.OPTIMISTIC;


@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ThinPutBenchmark extends IgniteBenchmark {
    private IgniteClient igniteClient;

    private IgniteClient[] igniteClientPool;
    private final AtomicInteger counter = new AtomicInteger(0);
    private IgniteClient ignitePAClient;

    @Setup
    public void setup() {
        igniteClient = startThinClient(IGNITE_IP_ADDRESS_LIST);
        createCaches(igniteClient, CACHE_NAME);
        generatePayload();
        ignitePAClient = startThinPAClient(IGNITE_IP_ADDRESS_LIST);
        igniteClientPool = new IgniteClient[IGNITE_IP_ADDRESS_LIST.length];
        for (int i = 0; i < IGNITE_IP_ADDRESS_LIST.length; i++) {
            igniteClientPool[i] = startThinClient(new String[]{IGNITE_IP_ADDRESS_LIST[i]});
        }
    }


    IgniteClient getIgniteClientPool() {
        return igniteClientPool[counter.updateAndGet(x -> (x + 1) % IGNITE_IP_ADDRESS_LIST.length)];

    }


    @Benchmark
    public void transaction_put(Blackhole blackhole) {
        ClientTransactions transactions = igniteClient.transactions();
        final ClientCache<String, BinaryObject> cache = igniteClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);

        try (ClientTransaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.READ_COMMITTED)) {
            cache.put(key, igniteClient.binary().toBinary(entity));
            tx.commit();
            blackhole.consume(tx);
        }
    }

    @Benchmark
    public void transaction_put_pa(Blackhole blackhole) {
        ClientTransactions transactions = ignitePAClient.transactions();
        final ClientCache<String, BinaryObject> cache = ignitePAClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);

        try (ClientTransaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.READ_COMMITTED)) {
            cache.put(key, ignitePAClient.binary().toBinary(entity));
            tx.commit();
            blackhole.consume(tx);
        }
    }


    @Benchmark
    public void put(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = igniteClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        cache.put(key, igniteClient.binary().toBinary(entity));
        blackhole.consume(entity);
    }


    @Benchmark
    public void put_pa(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = ignitePAClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        cache.put(key, ignitePAClient.binary().toBinary(entity));
        blackhole.consume(cache);
    }


    @Benchmark
    public void putAll_one_entity_pa(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = igniteClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        cache.putAll(Map.of(key, igniteClient.binary().toBinary(entity)));
        blackhole.consume(cache);
    }

    //PutAll implicit(два объекта)

    @Benchmark
    public void putAll_two_entity_pa(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = igniteClient.cache(CACHE_NAME);
        final Entity entity = createEntity(generateKey(), payload);
        final Entity entity2 = createEntity(generateKey(), payload);
        cache.putAll(Map.of(entity.id(), igniteClient.binary().toBinary(entity), entity2.id(), igniteClient.binary().toBinary(entity2)));
        blackhole.consume(cache);
    }

    @Benchmark
    public void optimistic_replace_pa(Blackhole blackhole) {
        final ClientCache<String, BinaryObject> cache = ignitePAClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        final BinaryObject oldObject = cache.get(key);
        final BinaryObject newObject = ignitePAClient.binary().toBinary(entity);
        boolean result = (oldObject == null) ? cache.putIfAbsent(key, newObject) : cache.replace(key, oldObject, newObject);
        blackhole.consume(result);
    }

    @Benchmark
    public void optimistic_put(Blackhole blackhole) {
        IgniteClient client = igniteClientPool[0];
        optimisticPut(blackhole, client);
    }


    @Benchmark
    public void optimistic_put_pool(Blackhole blackhole) {
        IgniteClient client = getIgniteClientPool();
        optimisticPut(blackhole, client);
    }

    private void optimisticPut(Blackhole blackhole, IgniteClient igniteClient) {
        final ClientCache<String, BinaryObject> cache = igniteClient.cache(CACHE_NAME);
        String key = generateKey();
        final Entity entity = createEntity(key, payload);
        final BinaryObject newObject = igniteClient.binary().toBinary(entity);

        ClientTransactions transactions = igniteClient.transactions();
        try (ClientTransaction tx = transactions.txStart(OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
            cache.get(key);
            // logic
            cache.put(key, newObject);
            tx.commit();
            blackhole.consume(tx);
        }
    }


    @TearDown
    public void tearDown() {
        ignitePAClient.close();
        igniteClient.close();
        for (int i = 0; i < IGNITE_IP_ADDRESS_LIST.length; i++) {
            igniteClientPool[i].close();
        }

    }

}