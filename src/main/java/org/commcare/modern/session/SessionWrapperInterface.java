package org.commcare.modern.session;

import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.analysis.XPathAnalyzable;

import java.util.Set;

/**
 * Created by willpride on 1/3/17.
 */

public interface SessionWrapperInterface {
    CommCareInstanceInitializer getIIF();
    String getNeededData();
    SessionDatum getNeededDatum(Entry entry);
    EvaluationContext getEvaluationContext(String commandId);
    EvaluationContext getEvaluationContext();
    EvaluationContext getRestrictedEvaluationContext(String commandId, Set<String> instancesToInclude);
    EvaluationContext getEvaluationContextWithAccumulatedInstances(String commandID, XPathAnalyzable xPathAnalyzable);

    void prepareExternalSources(RemoteInstanceFetcher fetcher) throws RemoteInstanceFetcher.RemoteInstanceException;
}
