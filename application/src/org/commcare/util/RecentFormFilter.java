/**
 *
 */
package org.commcare.util;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.suite.model.Profile;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.EntityFilter;

import java.util.Date;

/**
 * The RecentFormFilter specifies whether a form is currently
 * needed for review.
 *
 * It returns true for any form which should be listed for review
 * and false for any form not needing review.
 *
 * @author ctsims
 *
 */
public class RecentFormFilter extends EntityFilter<FormInstance> {

    private static int DEFAULT_DAYS = 7;
    int days;

    public RecentFormFilter() {
        days = DEFAULT_DAYS;

        //Otherwise, check to get the number of days before it is eligible for deletion
        String daysForReview = PropertyManager._().getSingularProperty(CommCareProperties.DAYS_FOR_REVIEW);
        if(daysForReview != null) {
            try {
                days = Integer.parseInt(daysForReview);
            } catch(NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
    }

    public RecentFormFilter(int days) {
        this.days = days;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.storage.EntityFilter#matches(java.lang.Object)
     */
    public boolean matches(FormInstance e) {
        //If review isn't enabled, nothing matches.
        if(!CommCareContext._().getManager().getCurrentProfile().isFeatureActive(Profile.FEATURE_REVIEW)) {
            return false;
        }

        try {
            //otherwise, check the number of days since the form was entered. If it has passed, the form no longer
            //qualifies
            if(Math.floor((((double)(new Date().getTime() - e.getDateSaved().getTime())) / DateUtils.DAY_IN_MS)) > days) {
                return false;
            } else {
                return true;
            }
        } catch(Exception ex) {
            //For stability reasons, if anything goes wrong with determining a forms deletion eligibility, assume
            //it is unreviewable
            return false;
        }
    }

}
