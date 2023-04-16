package com.pshevskiy.ignite.jmh;

import com.pshevskiy.ignite.server.tasks.PutService;
import com.pshevskiy.ignite.server.tasks.model.BulkOperation;
import com.pshevskiy.ignite.server.tasks.model.Status;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
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
public class ThickServiceGridBenchmark extends IgniteBenchmark {


    private Ignite ignite;


    @Setup
    public void setup() throws IgniteCheckedException {
        ignite = startThickClient();
        createCaches(ignite, CACHE_NAME);
        generatePayload();
    }


    @Benchmark
    public void optimistic_put_thick_client_service_grid_1(Blackhole blackhole) {

        BulkOperation request = getPutParams(ignite.binary(), 1);
        final List<Status> status = ignite.services(ignite.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class, false).put(request);

        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @Benchmark
    @OperationsPerInvocation(10)
    public void optimistic_put_thick_client_service_grid_10(Blackhole blackhole) {

        BulkOperation request = getPutParams(ignite.binary(), 10);
        final List<Status> status = ignite.services(ignite.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class, false).put(request);

        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @Benchmark
    @OperationsPerInvocation(100)
    public void optimistic_put_thick_client_service_grid_100(Blackhole blackhole) {

        BulkOperation request = getPutParams(ignite.binary(), 100);
        final List<Status> status = ignite.services(ignite.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class, false).put(request);

        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }


    @Benchmark
    @OperationsPerInvocation(1000)
    public void optimistic_put_thick_client_service_grid_1000(Blackhole blackhole) {

        BulkOperation request = getPutParams(ignite.binary(), 1000);
        final List<Status> status = ignite.services(ignite.compute().clusterGroup().forRandom()).serviceProxy("PutService", PutService.class, false).put(request);

        assert request.getOperations().size() == status.size();
        blackhole.consume(status);
    }

    @TearDown
    public void tearDown() {
        ignite.close();

    }


}