/**
 *
 */
package org.commcare.util;

import javax.microedition.io.ConnectionNotFoundException;

import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.model.Entity;
import org.javarosa.entity.model.EntitySet;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * Wrapper entity select controller to handle telephone callouts
 *
 * @author ctsims
 *
 */
public class CommCareEntitySelectController<E> extends EntitySelectController<E> {

    public CommCareEntitySelectController (String title, EntitySet<E> set, Entity<E> entityPrototype) {
        super(title, set,entityPrototype);
    }

    public CommCareEntitySelectController (String title, EntitySet<E> set, Entity<E> entityPrototype, int newMode, boolean immediatelySelectNewlyCreated) {
        super(title, set, entityPrototype, newMode, immediatelySelectNewlyCreated);
    }


    public CommCareEntitySelectController (String title, EntitySet<E> set, Entity<E> entityPrototype,
            int newMode, boolean immediatelySelectNewlyCreated, boolean bailOnEmpty) {
        super(title, set, entityPrototype, newMode, immediatelySelectNewlyCreated, bailOnEmpty);
    }

    public void attemptCallout(String number) {
        try {
            String scrubbed = "";
            String valid = "01234567890+#*";
            for(int i = 0; i < number.length() ; ++i) {
                if(valid.indexOf(number.charAt(i)) != -1) {
                    scrubbed += number.charAt(i);
                }
            }
            CommCareContext._().getMidlet().platformRequest("tel:" + scrubbed);
        } catch (ConnectionNotFoundException e) {
            Logger.exception("calling : " + number,e);
            J2MEDisplay.showError("Error", "Connection not found: " + e.getMessage());
        }
    }

}
