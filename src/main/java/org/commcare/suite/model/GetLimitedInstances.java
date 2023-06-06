package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

public class GetLimitedInstances {
    /**
     *
     * @param limitingList a list of instance names to restrict the returning set to; null
     *                     if no limiting is being used
     * @return a hashtable representing the data instances that are in scope for this Entry,
     * potentially limited by @limitingList
     */
    // getLimitedInstances move into its own class
    // make static
    public static Hashtable<String, DataInstance> getLimitedInstances(Set<String> limitingList,
            Hashtable<String, DataInstance> instances) {
        Hashtable<String, DataInstance> copy = new Hashtable<>();
        for (Enumeration en = instances.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();

            //This is silly, all of these are external data instances. TODO: save their
            //construction details instead.
            DataInstance cur = instances.get(key);
            if (limitingList == null || limitingList.contains(cur.getInstanceId())) {
                // Make sure we either aren't using a limiting list, or the instanceid is in the list
                if (cur instanceof ExternalDataInstance) {
                    //Copy the EDI so when it gets populated we don't keep it dependent on this object's lifecycle!!
                    copy.put(key, new ExternalDataInstance(((ExternalDataInstance)cur).getReference(), cur.getInstanceId()));
                } else {
                    copy.put(key, cur);
                }
            }
        }

        return copy;
    }
}
