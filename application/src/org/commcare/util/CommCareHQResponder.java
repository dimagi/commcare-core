package org.commcare.util;

import org.commcare.model.PeriodicEvent;
import org.commcare.util.time.TimeMessageEvent;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.formmanager.view.transport.TransportResponseProcessor;
import org.javarosa.services.transport.CommUtil;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.user.transport.HttpUserRegistrationTranslator;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CommCareHQResponder implements TransportResponseProcessor {

    String apiLevelGuess;
    OpenRosaApiResponseProcessor orHandler;

    /**
     * A processor for responses from CommCare HQ. Currently handles responses
     * from CCHQ 0.9, and for CCHQ 1.0 (assuming the OpenRosa 1.0 response API).
     *
     * @param apiLevelGuess A guess for what system we're talking to. Available
     * through a property set in the profile, generally.
     */
    public CommCareHQResponder(String apiLevelGuess) {
        this.apiLevelGuess = apiLevelGuess;
        //One central processor (for handling multiple payloads)
        this.orHandler = new OpenRosaApiResponseProcessor();
    }

    //TODO: Replace all response semantics with a single unified response system

    public String getResponseMessage(TransportMessage message) {
        String returnstr = "";

        // Make sure this is a normal HTTP message before trying anything fancy
        if(message.isSuccess() && message.getClass() == SimpleHttpTransportMessage.class) {
            String numForms = "";
            boolean understoodResponse = true;
            byte[] response = null;

            // No class-cast-exception possible since we just checked
            SimpleHttpTransportMessage msg = (SimpleHttpTransportMessage)message;

            //Check for date inconsistencies
            dateInconsistencyHelper(msg.getResponseProperties().getGMTDate());

            //If the server didn't tell us what OR API version to use, but we have a guess, use
            //that.
            if(msg.getResponseProperties().getORApiVersion() == null && apiLevelGuess != null) {
                msg.getResponseProperties().setRequestProperty("X-OpenRosa-Version",apiLevelGuess);
            }


            if(orHandler.handlesResponse(msg)) {

                //For now the failure mode from this point forward will assume that
                //with the server having and data safely received, that any state
                //lost is recoverable, so we'll report on any problems, but won't
                //fail-fast just yet, since the HTTP layer is not the appropriate
                //place for ensuring transaction security.

                try{
                    orHandler.processResponse(msg);
                } catch (InvalidStructureException e) {
                    //XML doesn't match the appropriate structure
                    Logger.exception("hq responder [ise]", e);
                    return Localization.get("sending.status.didnotunderstand", new String[] {e.getMessage()});
                } catch (IOException e) {
                    //Bad stream - RETRY, maybe?
                    Logger.exception("hq responder [ioe]", e);
                    return Localization.get("sending.status.problem.datasafe");
                } catch (UnfullfilledRequirementsException e) {
                    //Misreported version somewhere, device doesn't know how to handle response
                    Logger.exception("hq responder [ure]", e);
                    return Localization.get("sending.status.didnotunderstand", new String[] {e.getMessage()});
                } catch (XmlPullParserException e) {
                    //Bad XML
                    Logger.exception("hq responder [xppe]", e);
                    return Localization.get("sending.status.problem.datasafe");
                }
            }

            //If the response is something custom and unexpected, go through
            //the old response formats.

            response = msg.getResponseBody();
            Document doc = CommUtil.getXMLResponse(response);

            //1.0-ish, but not properly declared (and can't handle transactions)
            if(doc != null && "OpenRosaResponse".equals(doc.getRootElement().getName()) && HttpUserRegistrationTranslator.XMLNS_ORR.equals(doc.getRootElement().getNamespace())) {
                //Only relevant (for now!) for Form Submissions
                try{
                    Element e = doc.getRootElement().getElement(HttpUserRegistrationTranslator.XMLNS_ORR,"message");
                    String responseText = e.getText(0);
                    return responseText;
                } catch(Exception e) {
                    //No response message
                    if( msg.getResponseCode() == 202 ) {
                        return Localization.get("sending.status.problem.datasafe");
                    } else if(msg.getResponseCode() >= 200 && msg.getResponseCode() < 300) {
                        return Localization.get("sending.status.success");
                    } else {
                        return "";
                    }
                }
            }

            //Old (pre 1.0 responder logic)

            // 200 means everything is cool. 202 means data safe, but a problem
            if( msg.getResponseCode() == 200 ) {

                if (doc != null) {
                    Element e = doc.getRootElement();
                    for (int i = 0; i < e.getChildCount(); i++) {
                        if (e.getType(i) == Element.ELEMENT) {
                            Element child = e.getElement(i);
                            if(child.getName().equals("FormsSubmittedToday")) {
                                numForms = child.getText(0);
                                System.out.println("Found it! numforms:"+numForms);
                                break;
                            }
                        }
                    }
                } else {
                    understoodResponse = false;
                }
            }

            if (!understoodResponse) {
                returnstr = Localization.get("sending.status.didnotunderstand",
                        new String[] {response != null ? CommUtil.getString(response) : "[none]"});
            } else if (numForms.equals(""))
                returnstr = Localization.get("sending.status.problem.datasafe");
            else
                returnstr = Localization.get("sending.status.success", new String[]{numForms});
        }

        return returnstr;
    }

    /**
     * Helper method for identifying whether the server and phone have substantially
     * different notions of what the current date and time are.
     *
     * @param date The most recent authoritative (from the server) Date in GMT
     */
    private void dateInconsistencyHelper(long date) {
        try {
            //Don't do anything if we didn't get back a useful date.
            if(date == 0) {
                return;
            } else {
                //Get the date into the phone's default timezone
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.setTime(new Date(date));
                c.setTimeZone(TimeZone.getDefault());

                long difference = Math.abs(c.getTime().getTime() - new Date().getTime());


                //TODO: Property for this limit
                if(difference > DateUtils.DAY_IN_MS * 1.5) {
                    Logger.log("dih","Date off. Difference(ms): " + difference + ". Adding event.");
                    PeriodicEvent.schedule(new TimeMessageEvent());
                }
            }
        }
        catch(Exception e) {
            //This is purely helper code. Don't want it to ever crash the system
            Logger.exception("While checking dates", e);
        }
    }

    public boolean hasSummativeResponse() {
        if(orHandler.getCompiledResponses().length > 0) {
            return true;
        }
        return false;
    }

    public String getSummativeReseponse() {
        String summative = "";
        String[] responses = orHandler.getCompiledResponses();
        for(int i = 0 ; i < responses.length; ++i) {
            summative += responses[i] + "\n";
        }
        return summative;
    }

}
