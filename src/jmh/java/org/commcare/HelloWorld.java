package org.commcare;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.model.test.FormDefTest;

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

    @Benchmark()
    @Group("search")
    public void recForm() {
        FormParseInit fpi = new FormParseInit("/format_date_tests.xml");
        FormDefTest.initFormEntry(fpi);
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
