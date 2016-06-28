package org.commcare.cases.util.test;

import org.commcare.cases.model.Case;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.xml.CaseXmlParser;
import org.junit.Before;
import org.junit.Test;
import org.kxml2.io.KXmlParser;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * Quick test to be able to restore a set of user data
 * and ensure users and groups are properly being included
 * in case purges.
 *
 * @author ctsims
 */
public class CaseParseReindexTests {

    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() throws Exception {
        sandbox = MockDataUtils.getStaticStorage();

        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("index_disruption/base_transactions.xml"), sandbox);
        SandboxUtils.extractEntityOwners(sandbox);
    }

    @Test
    public void testCloseDisruption() throws Exception {
        parseAndTestForBreakage("index_disruption/case_close.xml", new String[] {"base_case"});
    }

    @Test
    public void testNoDisruption() throws Exception {
        parseAndTestForBreakage("index_disruption/case_update_clean.xml", new String[] {});
    }

    @Test
    public void testChangeOwner() throws Exception {
        parseAndTestForBreakage("index_disruption/case_change_owner.xml", new String[] {"base_case"});
    }

    @Test
    public void testRemoveIndex() throws Exception {
        parseAndTestForBreakage("index_disruption/case_remove_index.xml", new String[] {"base_case"});
    }

    @Test
    public void testChangeIndex() throws Exception {
        parseAndTestForBreakage("index_disruption/case_change_index.xml", new String[] {"base_case"});
    }
    private void parseAndTestForBreakage(String fileName, String[] expectedDisruptedIds)
            throws Exception {
        final HashSet<String> distruptedIndexes = new HashSet<>();
        CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(sandbox) {
            @Override
            public void initCaseParser() {
                caseParser = new TransactionParserFactory() {
                    CaseXmlParser created = null;

                    @Override
                    public TransactionParser<Case> getParser(KXmlParser parser) {
                        if (created == null) {
                            created = new CaseXmlParser(parser, sandbox.getCaseStorage()) {
                                @Override
                                public void onIndexDisrupted(String caseId) {
                                    distruptedIndexes.add(caseId);
                                }
                            };
                        }

                        return created;
                    }
                };
            }
        };
        DataModelPullParser parser = new DataModelPullParser(this.getClass().getClassLoader().
                getResourceAsStream(fileName), factory, true, true);
        parser.parse();

        HashSet<String> expectedIds = new HashSet<>();
        Collections.addAll(expectedIds, expectedDisruptedIds);

        assertEquals("Incorrect Disrupted Indexes (" + fileName + ")", expectedIds, distruptedIndexes);
    }
}
