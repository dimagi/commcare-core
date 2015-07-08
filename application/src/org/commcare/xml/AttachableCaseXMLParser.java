/**
 *
 */
package org.commcare.xml;

import java.io.IOException;

import org.commcare.cases.model.Case;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.PropertyUtils;
import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 *
 */
public class AttachableCaseXMLParser extends CaseXmlParser {

    public AttachableCaseXMLParser(KXmlParser parser, int[] tallies, boolean acceptCreateOverwrites, IStorageUtilityIndexed storage) {
        super(parser, tallies, acceptCreateOverwrites, storage);
    }

    protected void removeAttachment(Case caseForBlock, String attachmentName) {

    }

    protected String processAttachment(String src, String from, String name, KXmlParser parser) {

        //Parse from the local environment
        if(CaseXmlParser.ATTACHMENT_FROM_LOCAL.equals(from)) {
            //This makes no sense in the j2me context, as we always process local forms though the model processor.
        } else if(CaseXmlParser.ATTACHMENT_FROM_REMOTE.equals(from)) {

            //Get a random filename and extension
            String dest = PropertyUtils.genUUID();

            //add an extension
            int lastDot =src.lastIndexOf('.');
            if(lastDot != -1) {
                dest += src.substring(lastDot);
            }

            String destination = "jr://file/commcarecaseattachments/" + dest;

            Reference destRef;
            try {
                destRef = ReferenceManager._().DeriveReference(destination);
            } catch (InvalidReferenceException e) {
                Logger.log("case", "Attachments not supported on local platform.");
                return null;
            }

            Reference sourceRef;
            try {
                sourceRef = ReferenceManager._().DeriveReference(src);
            } catch (InvalidReferenceException e) {
                Logger.log("case", "Couldn't determine where to fetch attachment " + src);
                return null;
            }

            try {
                StreamsUtil.writeFromInputToOutput(sourceRef.getStream(), destRef.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                Logger.log("case", "Sorry, couldn't download attachment " + src + ". Error: " + e.getMessage());
                return null;
            }

            return destination;
        }
        return null;
    }

}
