/**
 *
 */
package org.javarosa.j2me.log;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * @author ctsims
 *
 */
public interface XmlStatusProvider {
    public void getStatusReport(XmlSerializer o, String namespace) throws StatusReportException, IOException;
}
