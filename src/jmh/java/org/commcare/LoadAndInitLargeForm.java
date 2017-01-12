package org.commcare;

import org.commcare.session.SessionNavigator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.test.utilities.MockApp;
import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.javarosa.form.api.FormEntryController;

public class LoadAndInitLargeForm {

    @Benchmark()
    @Group("form_init")
    public void recForm() throws Exception {
        MockApp mockApp = new MockApp("/app_performance/");
        MockSessionNavigationResponder mockSessionNavigationResponder =
                new MockSessionNavigationResponder(mockApp.getSession());
        SessionNavigator sessionNavigator =
                new SessionNavigator(mockSessionNavigationResponder);
        SessionWrapper session = mockApp.getSession();
        sessionNavigator.startNextSessionStep();
        session.setCommand("m1");
        sessionNavigator.startNextSessionStep();
        session.setDatum("case_id", "3b6bff05-b9c3-42d8-9b12-9b27a834d330");
        sessionNavigator.startNextSessionStep();

        session.setCommand("m1-f2");
        sessionNavigator.startNextSessionStep();
        session.setDatum("case_id_new_imci_visit_0",
                "593ef28a-34ff-421d-a29c-6a0fd975df95");
        sessionNavigator.startNextSessionStep();

        FormEntryController fec = mockApp.loadAndInitForm("large_tdh_form.xml");
        fec.getModel().getEvent();
    }

    // Execute using:
    //  java -jar build\libs\commcare-core-jmh.jar -wi 5 -i 5 form_init
    // -wi sets the warmup iteration count
    // -i sets the iteration count
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LoadAndInitLargeForm.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
