package com.pshevskiy.ignite.server.tasks;

import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Entity;
import com.pshevskiy.ignite.server.tasks.model.Operation;
import com.pshevskiy.ignite.server.tasks.model.Status;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.processors.resource.GridSpringResourceContext;
import org.apache.ignite.lang.IgniteBiTuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AtomicPutTest {
    protected static final String CACHE_NAME = "AFFINITY_ENTITY";
    protected static Ignite ignite;
    protected static IgniteClient thinClient;
    public final static int N = 10;


    @BeforeAll
    static void initAll() throws IgniteCheckedException {
        ignite = startServerNode();
        createCaches(ignite, CACHE_NAME);
        thinClient = startClient();
        ignite.services().deployNodeSingleton("PutService", new AtomicPutService());
    }

    @AfterAll
    static void destroyAll() throws Exception {
        thinClient.close();
        ignite.close();
    }


    @Test
    void shouldPutNewEntities_whenVersionIsNewer_service_grid() throws InterruptedException {
        final String id = UUID.randomUUID().toString();
        final String collocationId = UUID.randomUUID().toString();
        int version = 1;
        final Entity entity = Entity.builder()
                .id(id)
                .collocationId(collocationId)
                .version(version)
                .binaryData(("TEST_" + version).getBytes(StandardCharsets.UTF_8))
                .build();

        final AffinityKey<String> affinityKey = new AffinityKey(id, collocationId);


        final Operation operation = Operation.builder()
                .key(affinityKey)
                .rewriteSameVersion(false)
                .object(thinClient.binary().toBinary(entity))
                .cacheName(CACHE_NAME).build();

        final Status put = thinClient.services().serviceProxy("PutService", PutService.class).put(new BulkOperation(Collections.singletonList(operation))).get(0);

        assertEquals(Status.ADDED, put);

        final ClientCache<AffinityKey<String>, BinaryObject> cache = thinClient.cache(CACHE_NAME).withKeepBinary();
        final BinaryObject actualBinaryObject = cache.get(affinityKey);
        assertNotNull(actualBinaryObject);

        Entity actualEntity = new Entity(actualBinaryObject);

        assertEquals(entity, actualEntity);

    }

    @Test
    void shouldPutNewEntities_whenVersionIsNewer_compute_task() throws InterruptedException {

        final String id = UUID.randomUUID().toString();
        final String collocationId = UUID.randomUUID().toString();
        int version = 1;
        final Entity entity = Entity.builder()
                .id(id)
                .collocationId(collocationId)
                .version(version)
                .binaryData(("TEST_" + version).getBytes(StandardCharsets.UTF_8))
                .build();

        final AffinityKey<String> affinityKey = new AffinityKey(id, collocationId);


        final Operation operation = Operation.builder()
                .key(affinityKey)
                .rewriteSameVersion(false)
                .object(thinClient.binary().toBinary(entity))
                .cacheName(CACHE_NAME).build();

        final List<Status> results = thinClient.compute().execute(AtomicPut.class.getName(), new BulkOperation(Collections.singletonList(operation)));

        assertEquals(1, results.size());
        assertEquals(Status.ADDED, results.get(0));

        final ClientCache<AffinityKey<String>, BinaryObject> cache = thinClient.cache(CACHE_NAME).withKeepBinary();
        final BinaryObject actualBinaryObject = cache.get(affinityKey);
        assertNotNull(actualBinaryObject);

        Entity actualEntity = new Entity(actualBinaryObject);

        assertEquals(entity, actualEntity);
    }


    public static Ignite startServerNode() throws IgniteCheckedException {
        IgniteBiTuple<Collection<IgniteConfiguration>, ? extends GridSpringResourceContext> igniteConfig =
                IgnitionEx.loadConfigurations("vm-ignite-config-test.xml");
        return Ignition.getOrStart((IgniteConfiguration) igniteConfig.get1().toArray()[0]);
    }

    public static IgniteClient startClient() {
        return Ignition.startClient(new ClientConfiguration()
                .setAddresses("127.0.0.1"));
    }


    private static void createCaches(Ignite ignite, String cacheName) {
        addCacheTemplates(ignite);
        ignite.getOrCreateCache(cacheName);
    }

    private static void addCacheTemplates(Ignite ignite) {
        RendezvousAffinityFunction affinity = new RendezvousAffinityFunction().setPartitions(1024);
        Collections.singletonList(
                new CacheConfiguration<>()
                        .setName("AFFINITY_*")
                        .setCacheMode(CacheMode.PARTITIONED)
                        .setBackups(0)
                        .setGroupName("DEFAULT_CACHE_GROUP")
                        .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                        .setRebalanceMode(CacheRebalanceMode.ASYNC)
                        .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC)
                        .setMaxConcurrentAsyncOperations(0)
                        .setStatisticsEnabled(false)
                        .setAffinity(affinity)
                        .setQueryEntities(getAffinityKeyQueryEntities())
        ).forEach(ignite::addCacheConfiguration);


    }

    private static List<QueryEntity> getAffinityKeyQueryEntities() {
        QueryEntity affinityKeyQueryEntity = new QueryEntity(AffinityKey.class, Entity.class);
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("collocationId", "java.lang.String");
        affinityKeyQueryEntity.setFields(fields);
        List<QueryIndex> collocationId = new ArrayList<>();
        collocationId.add(new QueryIndex("collocationId"));
        affinityKeyQueryEntity.setIndexes(collocationId);
        List<QueryEntity> queryEntities = new ArrayList<>();
        queryEntities.add(affinityKeyQueryEntity);
        return queryEntities;
    }

}