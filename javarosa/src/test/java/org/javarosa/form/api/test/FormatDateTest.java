package org.javarosa.form.api.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * @author wspride
 */
public class FormatDateTest {
    private FormParseInit fpi;

    /**
     * load and parse form
     */
    @Before
    public void initForm() {
        System.out.println("init FormDateTest");
        fpi = new FormParseInit("/format_date_tests.xml");
    }

    /**
     * Tests whether format-date works for date strings,
     * both wrapped and not wrapped by date()
     */
    @Test
    public void testAnswerQuestion() {
        DateData ans;
        FormEntryController fec = fpi.getFormEntryController();
        FormEntryPrompt prompt;
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        Localizer l = fpi.getFormDef().getLocalizer();
        l.setDefaultLocale(l.getAvailableLocales()[0]);
        l.setLocale(l.getAvailableLocales()[0]);
        fec.stepToNextEvent();

        ans = new DateData(new Date());
        fec.answerQuestion(ans);
        fec.stepToNextEvent();

        prompt = fpi.getFormEntryModel().getQuestionPrompt();
        String unwrappedDateString = prompt.getLongText();
        String javaDateString = new SimpleDateFormat("d MMM, yyyy", Locale.US).format(new Date());
        assertEquals(javaDateString, unwrappedDateString);

        fec.stepToNextEvent();
        prompt = fpi.getFormEntryModel().getQuestionPrompt();
        unwrappedDateString = prompt.getLongText();
        javaDateString = new SimpleDateFormat("d MMM, yyyy", Locale.US).format(new Date());
        assertEquals(javaDateString, unwrappedDateString);
    }


}
