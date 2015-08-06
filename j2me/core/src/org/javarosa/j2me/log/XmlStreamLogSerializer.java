package org.javarosa.j2me.log;

import org.javarosa.core.log.LogEntry;
import org.javarosa.core.log.StreamLogSerializer;
import org.javarosa.core.model.utils.DateUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public class XmlStreamLogSerializer extends StreamLogSerializer {

    XmlSerializer o;
    String ns;

    public XmlStreamLogSerializer(XmlSerializer o, String ns) {
        this.o = o;
        this.ns = ns;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.log.ILogSerializer#serializeLog(org.javarosa.core.log.IncidentLog)
     */
    protected void serializeLog(LogEntry log) throws IOException {
        o.startTag(ns, "log");
        try {
        o.attribute(null, "date", DateUtils.formatDateTime(log.getTime(), DateUtils.FORMAT_ISO8601));

        o.startTag(ns, "type");
        try {
            o.text(log.getType());
        } finally {
            o.endTag(ns, "type");
        }

        o.startTag(ns, "msg");
        try {
            o.text(log.getMessage());
        } finally {
            o.endTag(ns, "msg");
        }
        } finally {
            o.endTag(ns, "log");
        }
    }
}
