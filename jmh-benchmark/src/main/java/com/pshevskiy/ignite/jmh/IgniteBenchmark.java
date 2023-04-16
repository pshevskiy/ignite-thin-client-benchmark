package com.pshevskiy.ignite.jmh;

import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Entity;
import com.pshevskiy.ignite.server.tasks.model.Operation;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteBinary;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.infra.Blackhole;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class IgniteBenchmark {

    byte[] payload;

    public static final int SIZE = 1000;

    private final AtomicInteger getCounter = new AtomicInteger(0);

    public static final String IGNITE_CONFIG_XML = System.getenv("CONFIG_URI");
    public static final String CACHE_NAME = "DATA_TEST_CACHE";
    public static final String[] IGNITE_IP_ADDRESS_LIST = {"ignite_node_1", "ignite_node_2"};


    protected void generatePayload() {
        payload = new byte[1024];
        new Random().nextBytes(payload);
    }

    String nextId() {
        return String.valueOf(getCounter.updateAndGet(x -> (x + 1) % SIZE));

    }


    protected String generateKey() {
        return UUID.randomUUID().toString();
    }

    protected Ignite startThickClient() throws IgniteCheckedException {
        IgniteConfiguration igniteConfig =
                (IgniteConfiguration) IgnitionEx.loadConfigurations(IGNITE_CONFIG_XML).get1().toArray()[0];
        igniteConfig.setClientMode(true);
        return Ignition.getOrStart(igniteConfig);
    }

    @NotNull
    protected BulkOperation getPutParams(IgniteBinary igniteClient, int batchSize) {
        List<Operation> request = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            String key = generateKey();
            final Entity entity = createEntity(key, payload);
            final BinaryObject newObject = igniteClient.toBinary(entity);

            final Operation req = new Operation(CACHE_NAME, key, newObject, true);
            request.add(req);
        }
        return new BulkOperation(request);
    }


    @NotNull
    Entity createEntity(String key, byte[] payload) {
        return new Entity(key, key, 1, 0, payload);
    }


    static IgniteClient startThinClient(String[] ipAddress) {
        return Ignition.startClient(new ClientConfiguration()
                .setPartitionAwarenessEnabled(false)
                .setAddresses(ipAddress));
    }

    static IgniteClient startThinPAClient(String[] ipAddress) {
        return Ignition.startClient(new ClientConfiguration()
                .setPartitionAwarenessEnabled(true)
                .setAddresses(ipAddress));
    }

    static void createCaches(Ignite ignite, String cacheName) {
        ignite.getOrCreateCache(cacheName);
    }

    static void createCaches(IgniteClient ignite, String cacheName) {
        ignite.getOrCreateCache(cacheName);
    }

    void createEntities(Ignite client) {
        final Cache<String, BinaryObject> cache = client.cache(CACHE_NAME);
        for (int i = 0; i < IgniteBenchmark.SIZE; i++) {
            final String key = String.valueOf(i);
            cache.put(key, client.binary().toBinary(createEntity(key, payload)));
        }
    }

    void createEntities(IgniteClient client) {
        final ClientCache<Object, Object> cache = client.cache(CACHE_NAME);
        for (int i = 0; i < IgniteBenchmark.SIZE; i++) {
            final String key = String.valueOf(i);
            cache.put(key, client.binary().toBinary(createEntity(key, payload)));
        }
    }

    void checkData(Blackhole blackhole, BinaryObject object) {
        assert object != null;
        assert object.field("binaryData") == payload;
        blackhole.consume(object);
    }


    protected static String[] shuffleArray(String[] ar) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            String tmp = ar[index];
            ar[index] = ar[i];
            ar[i] = tmp;
        }
        return ar;
    }
}
