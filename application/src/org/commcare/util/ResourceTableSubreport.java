/**
 *
 */
package org.commcare.util;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.util.DeviceReportElement;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * @author ctsims
 *
 */
public class ResourceTableSubreport implements DeviceReportElement {

    private ResourceTable table;

    public ResourceTableSubreport(ResourceTable table) {
        this.table = table;
    }

    /* (non-Javadoc)
     * @see org.javarosa.log.util.DeviceReportElement#writeToDeviceReport(org.xmlpull.v1.XmlSerializer)
     */
    public void writeToDeviceReport(XmlSerializer o) throws IOException {

        o.startTag(DeviceReportState.XMLNS, "globaltable_subreport");
        try {
            for(Resource r : CommCarePlatform.getResourceListFromProfile(table)) {
                o.startTag(DeviceReportState.XMLNS, "resource");
                try {
                    o.attribute(null, "id", r.getResourceId());
                    o.attribute(null, "version", String.valueOf(r.getVersion()));
                    DeviceReportState.writeText(o, "status", ResourceTable.getStatusString(r.getStatus()));
                }finally {
                    o.endTag(DeviceReportState.XMLNS, "resource");
                }
            }
        } finally {
            o.endTag(DeviceReportState.XMLNS, "globaltable_subreport");
        }
    }
}
