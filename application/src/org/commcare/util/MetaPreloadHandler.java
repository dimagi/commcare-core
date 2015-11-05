/**
 *
 */
package org.commcare.util;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.model.User;

/**
 * @author ctsims
 *
 */
public class MetaPreloadHandler implements IPreloadHandler {
    private User u;

    public MetaPreloadHandler(User u) {
        this.u = u;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IPreloadHandler#handlePostProcess(org.javarosa.core.model.instance.TreeElement, java.lang.String)
     */
    public boolean handlePostProcess(TreeElement node, String params) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IPreloadHandler#handlePreload(java.lang.String)
     */
    public IAnswerData handlePreload(String preloadParams) {
        System.out.println("asked to preload: " + preloadParams);
        if(preloadParams.equals("UserName")) {
            return new UncastData(u.getUsername());
        }else if(preloadParams.equals("UserID")) {
            return new UncastData(String.valueOf(u.getUniqueId()));
        } else if(preloadParams.equals("AppVersion")) {
            return new StringData(CommCareUtil.getVersion());
        }
        System.out.println("FAILED to preload: " + preloadParams);
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IPreloadHandler#preloadHandled()
     */
    public String preloadHandled() {
        return "meta";
    }

}
