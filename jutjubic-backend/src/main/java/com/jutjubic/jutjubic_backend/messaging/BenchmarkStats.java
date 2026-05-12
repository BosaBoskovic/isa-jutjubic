package com.jutjubic.jutjubic_backend.messaging;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BenchmarkStats {

    private static final int TARGET = 50;

    // Serialize totals
    private final AtomicInteger jsonSerCount = new AtomicInteger();
    private final AtomicLong jsonSerNs = new AtomicLong();
    private final AtomicLong jsonSerBytes = new AtomicLong();

    private final AtomicInteger protoSerCount = new AtomicInteger();
    private final AtomicLong protoSerNs = new AtomicLong();
    private final AtomicLong protoSerBytes = new AtomicLong();

    // Deserialize totals
    private final AtomicInteger jsonDeCount = new AtomicInteger();
    private final AtomicLong jsonDeNs = new AtomicLong();
    private final AtomicLong jsonDeBytes = new AtomicLong();

    private final AtomicInteger protoDeCount = new AtomicInteger();
    private final AtomicLong protoDeNs = new AtomicLong();
    private final AtomicLong protoDeBytes = new AtomicLong();

    public void addJsonSerialize(long ns, int bytes) {
        int c = jsonSerCount.incrementAndGet();
        jsonSerNs.addAndGet(ns);
        jsonSerBytes.addAndGet(bytes);
        if (c == TARGET) printSerializeAverages();
    }

    public void addProtoSerialize(long ns, int bytes) {
        int c = protoSerCount.incrementAndGet();
        protoSerNs.addAndGet(ns);
        protoSerBytes.addAndGet(bytes);
        if (c == TARGET) printSerializeAverages();
    }

    public void addJsonDeserialize(long ns, int bytes) {
        int c = jsonDeCount.incrementAndGet();
        jsonDeNs.addAndGet(ns);
        jsonDeBytes.addAndGet(bytes);
        if (c == TARGET) printDeserializeAverages();
    }

    public void addProtoDeserialize(long ns, int bytes) {
        int c = protoDeCount.incrementAndGet();
        protoDeNs.addAndGet(ns);
        protoDeBytes.addAndGet(bytes);
        if (c == TARGET) printDeserializeAverages();
    }

    private void printSerializeAverages() {
        if (jsonSerCount.get() >= TARGET && protoSerCount.get() >= TARGET) {
            double jsonMs = jsonSerNs.get() / 1_000_000.0 / jsonSerCount.get();
            double protoMs = protoSerNs.get() / 1_000_000.0 / protoSerCount.get();

            double jsonSize = jsonSerBytes.get() * 1.0 / jsonSerCount.get();
            double protoSize = protoSerBytes.get() * 1.0 / protoSerCount.get();

            System.out.println("===== MQ BENCHMARK (SERIALIZE) =====");
            System.out.printf("JSON  avg serialize: %.4f ms, avg size: %.1f bytes%n", jsonMs, jsonSize);
            System.out.printf("PROTO avg serialize: %.4f ms, avg size: %.1f bytes%n", protoMs, protoSize);
            System.out.println("====================================");
        }
    }

    private void printDeserializeAverages() {
        if (jsonDeCount.get() >= TARGET && protoDeCount.get() >= TARGET) {
            double jsonMs = jsonDeNs.get() / 1_000_000.0 / jsonDeCount.get();
            double protoMs = protoDeNs.get() / 1_000_000.0 / protoDeCount.get();

            double jsonSize = jsonDeBytes.get() * 1.0 / jsonDeCount.get();
            double protoSize = protoDeBytes.get() * 1.0 / protoDeCount.get();

            System.out.println("===== MQ BENCHMARK (DESERIALIZE) =====");
            System.out.printf("JSON  avg deserialize: %.4f ms, avg size: %.1f bytes%n", jsonMs, jsonSize);
            System.out.printf("PROTO avg deserialize: %.4f ms, avg size: %.1f bytes%n", protoMs, protoSize);
            System.out.println("======================================");
        }
    }
}
