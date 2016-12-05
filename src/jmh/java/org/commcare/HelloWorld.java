package org.commcare;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

public class HelloWorld {
    @State(Benchmark)
    public static class Numbers {
        public static final int[] sortedNumbers = new int[100];
        public static final Random rnd = new Random();

        static {
            for (int x = 0; x < sortedNumbers.length; x++) {
                sortedNumbers[x] = x;
            }
        }
    }

    @Benchmark
    @Group("variableaccess")
    public void messWithSameVariable(Blackhole bh){
        int x = 5;
        int y = 3;
        x *= 5;
        x *= 3;
        bh.consume(x);
        bh.consume(y);
    }
    @Benchmark
    @Group("variableaccess")
    public void messWithDifferentVariables(Blackhole bh){
        int x = 5;
        int y = 3;
        x *= 5;
        y *= 3;
        bh.consume(x);
        bh.consume(y);
    }
    @Benchmark
    @Group("search")
    public int linearSearch() {
        final int needle = Numbers.rnd.nextInt(Numbers.sortedNumbers.length);
        for (int x = 0; x < Numbers.sortedNumbers.length; x++) {
            final int currentValue = Numbers.sortedNumbers[x];
            if (currentValue == needle) {
                return x;
            }
            if (currentValue > needle) {
                return -1;
            }

        }
        return -1;
    }

    @Benchmark()
    @Group("search")
    public int binarySearch() {
        final int needle = Numbers.rnd.nextInt(Numbers.sortedNumbers.length);
        int lo = 0;
        int hi = Numbers.sortedNumbers.length - 1;
        boolean found = false;
        while (lo <= hi) {
            final int mid = lo + (hi - lo) / 2;
            final int currentValue = Numbers.sortedNumbers[mid];
            if (needle < currentValue) hi = mid - 1;
            else if (needle > currentValue) lo = mid + 1;
            else {
                return mid;
            }
        }
        return -1;
    }

    // Execute using:
    //  java -jar build\libs\MicroBenchmarking-1.0-jmh.jar -wi 5 -i 5 search
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(HelloWorld.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
