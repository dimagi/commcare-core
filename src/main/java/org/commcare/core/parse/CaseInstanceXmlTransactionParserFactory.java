package org.commcare.core.parse;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.modern.engine.cases.CaseIndexTable;
import org.commcare.xml.bulk.BulkCaseInstanceXmlParser;
import org.kxml2.io.KXmlParser;

public class CaseInstanceXmlTransactionParserFactory implements TransactionParserFactory {

    private final UserSandbox sandbox;
    private final CaseIndexTable caseIndexTable;
    private TransactionParserFactory caseParser;

    public CaseInstanceXmlTransactionParserFactory(UserSandbox sandbox, CaseIndexTable caseIndexTable) {
        this.sandbox = sandbox;
        this.caseIndexTable = caseIndexTable;
        initCaseParser();
    }

    private void initCaseParser() {
        caseParser =  new TransactionParserFactory() {
            BulkCaseInstanceXmlParser created = null;

            @Override
            public TransactionParser getParser(KXmlParser parser) {
                if (created == null) {
                    created = new BulkCaseInstanceXmlParser(parser, sandbox.getCaseStorage(), caseIndexTable);
                }

                return created;
            }
        };
    }

    @Override
    public TransactionParser getParser(KXmlParser parser) {
        String name = parser.getName();
        if ("case".equalsIgnoreCase(name)) {
            if (caseParser == null) {
                throw new RuntimeException("Couldn't receive Case transaction without initialization!");
            }
            return caseParser.getParser(parser);
        }
        return null;
    }
}
