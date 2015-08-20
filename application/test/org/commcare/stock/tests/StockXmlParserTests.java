/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.commcare.stock.tests;

import org.commcare.cases.ledger.Ledger;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.LedgerXmlParsers;
import org.kxml2.io.KXmlParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Vector;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class StockXmlParserTests extends TestCase {

    private static int NUM_TESTS = 1;

    private static final String BALANCE_GOOD = "<balance entity-id='a59d8b11-218e-47a9-ba49-fb8e6f615085' stock-id='supplies' date='2013-12-05' xmlns='http://commtrack.org/stock_report'><n2:product index='0' id='productguid0' quantity='10' /><n2:product index='1' id='productguid2' quantity='20' /></n2:balance></data>";

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    public StockXmlParserTests(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public StockXmlParserTests(String name) {
        super(name);
    }

    public StockXmlParserTests() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;

            aSuite.addTest(new StockXmlParserTests("Stock XML Parsing Test " + i, new TestMethod() {
                public void run (TestCase tc) {
                    ((StockXmlParserTests)tc).testMaster(testID);
                }
            }));
        }

        return aSuite;
    }
    public void testMaster (int testID) {
        //System.out.println("running " + testID);

        switch (testID) {
        case 1: testGoodParses(); break;
        }
    }

    public void testGoodParses() {
        Vector<Ledger> s = parseBlock(BALANCE_GOOD);
        assertTrue("Wrong number of stock records!", s.size() == 1);

    }

    public Vector<Ledger> parseBlock(String source) {
        final Vector<Ledger> stockResults = new Vector<Ledger>();

        TransactionParserFactory factory = new TransactionParserFactory() {

            public TransactionParser getParser(KXmlParser parser) {
                if (LedgerXmlParsers.STOCK_XML_NAMESPACE.equalsIgnoreCase(parser.getNamespace())) {
                    return new LedgerXmlParsers(parser, null) {

                        /* (non-Javadoc)
                         * @see org.commcare.xml.StockXmlParsers#commit(org.commcare.cases.stock.Stock[])
                         */

                        public void commit(Ledger[] parsed) throws IOException {
                            for(Ledger s : parsed) {
                                stockResults.addElement(s);
                            }
                        }

                        /* (non-Javadoc)
                         * @see org.commcare.xml.StockXmlParsers#retrieve(java.lang.String)
                         */

                        public Ledger retrieveOrCreate(String entityId) {
                            return null;
                        }

                    };
                }

                return null;
            }

        };

        //handle exceptions later
        try {
            DataModelPullParser modelParser = new DataModelPullParser(new ByteArrayInputStream(source.getBytes()), factory, true, true);
            modelParser.parse();
        } catch(Exception e) {
            throw new RuntimeException("Exception " + e.getClass().getName() +" while parsing stock: " + e.getMessage());
        }

        return stockResults;
    }

}
