package com.pshevskiy.ignite.jmh;

import com.pshevskiy.ignite.server.tasks.AtomicPut;
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
public class ThinComputeTaskBenchmark extends IgniteBenchmark {

    private IgniteClient igniteClient;

    @Setup
    public void setup() {
        igniteClient = startThinClient(IGNITE_IP_ADDRESS_LIST);
        createCaches(igniteClient, CACHE_NAME);
        generatePayload();
    }


    @Benchmark
    public void optimistic_put_compute_task_1(Blackhole blackhole) throws InterruptedException {
        thinClientCall(blackhole, 1);
    }

    @Benchmark
    @OperationsPerInvocation(10)
    public void optimistic_put_compute_task_10(Blackhole blackhole) throws InterruptedException {
        thinClientCall(blackhole, 10);
    }


    @Benchmark
    @OperationsPerInvocation(100)
    public void optimistic_put_compute_task_100(Blackhole blackhole) throws InterruptedException {
        thinClientCall(blackhole, 100);
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    public void optimistic_put_compute_task_1000(Blackhole blackhole) throws InterruptedException {
        thinClientCall(blackhole, 1000);
    }


    @TearDown
    public void tearDown() {
        igniteClient.close();
    }

    private void thinClientCall(Blackhole blackhole, int batchSize) throws InterruptedException {
        BulkOperation request = getPutParams(igniteClient.binary(), batchSize);
        final List<Status> statuses = igniteClient.compute(igniteClient.compute().clusterGroup().forRandom())
                .execute(AtomicPut.class.getName(), request);
        assert request.getOperations().size() == statuses.size();
        assert statuses.stream().allMatch(status -> status == Status.ADDED);

        blackhole.consume(statuses);
    }


}