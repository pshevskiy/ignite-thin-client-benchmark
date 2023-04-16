package com.pshevskiy.ignite.jmh;

import com.pshevskiy.ignite.server.tasks.PutService;
import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Status;
import org.apache.ignite.client.IgniteClient;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;


@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ThinServiceGridBenchmark extends IgniteBenchmark {


    private IgniteClient igniteClient;


    @Setup
    public void setup() {
        igniteClient = startThinClient(IGNITE_IP_ADDRESS_LIST);
        createCaches(igniteClient, CACHE_NAME);
        generatePayload();
    }


    @Benchmark
    public void optimistic_put_service_grid_1(Blackhole blackhole) {

        BulkOperation request = getPutParams(igniteClient.binary(), 1);
        final List<Status> status = igniteClient.services(igniteClient.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class).put(request);
        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @Benchmark
    @OperationsPerInvocation(10)
    public void optimistic_put_service_grid_10(Blackhole blackhole) {

        BulkOperation request = getPutParams(igniteClient.binary(), 10);
        final List<Status> status = igniteClient.services(igniteClient.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class).put(request);
        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @Benchmark
    @OperationsPerInvocation(100)
    public void optimistic_put_service_grid_100(Blackhole blackhole) {

        BulkOperation request = getPutParams(igniteClient.binary(), 100);
        final List<Status> status = igniteClient.services(igniteClient.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class).put(request);
        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @Benchmark
    @OperationsPerInvocation(1000)
    public void optimistic_put_service_grid_1000(Blackhole blackhole) {

        BulkOperation request = getPutParams(igniteClient.binary(), 1000);
        final List<Status> status = igniteClient.services(igniteClient.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class).put(request);
        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @TearDown
    public void tearDown() {
        igniteClient.close();

    }


}