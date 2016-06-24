/**
 *
 */
package org.javarosa.log.util;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * @author ctsims
 *
 */
public interface DeviceReportElement {
    public void writeToDeviceReport(XmlSerializer serializer) throws IOException;
}
