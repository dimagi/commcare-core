package org.commcare.util.cli;

import org.commcare.suite.model.Detail;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.io.PrintStream;
import java.util.Vector;

/**
 * Created by jschweers on 9/10/2015.
 */
public class EntitySubnodeDetailSubscreen extends EntityListSubscreen {
    public EntitySubnodeDetailSubscreen(Detail shortDetail, EvaluationContext context, TreeReference contextualizedNodeset) throws CommCareSessionException {
        super(shortDetail, context.expandReference(contextualizedNodeset), context);
    }

    @Override
    public void prompt(PrintStream out) {

        int maxLength = String.valueOf(mChoices.length).length();
        out.println(CliUtils.pad("", maxLength + 1) + mHeader);
        out.println("==============================================================================================");

        for (int i = 0; i < mChoices.length; ++i) {
            out.println(rows[i]);
        }

        out.println("Press enter to select this case");
    }

    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host) throws CommCareSessionException {
        return true;
    }
}
