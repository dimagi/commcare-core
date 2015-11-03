package org.commcare.util;

import org.commcare.util.cli.ApplicationHost;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by willpride on 10/5/15.
 */
public class TestHarness {

    ApplicationHost applicationHost;
    File testPlan;
    ArrayList<String> testSteps = new ArrayList<>();

    public TestHarness(ApplicationHost host, File testPlan){
        this.applicationHost = host;
        this.testSteps = parseTestPlan(testPlan);
        this.testPlan = testPlan;
    }

    private static ArrayList<String> parseTestPlan(File testPlan) {
        ArrayList<String> steps = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(testPlan))) {
            String line;
            System.out.println("Test Plan");
            System.out.println("=========");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                steps.add(line);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return steps;
    }

    public void runTests() {
        try (BufferedReader br = new BufferedReader(new FileReader(testPlan))) {
            applicationHost.setReader(br);
            applicationHost.run();
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    
}
