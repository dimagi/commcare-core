/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.util;

import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;

import java.util.Random;
import java.util.Vector;

public class PropertyUtils {

    //need 'addpropery' too.

    /**
     * Used by J2ME
     */
    public static String initializeProperty(String propName, String defaultValue) {
        Vector propVal = PropertyManager._().getProperty(propName);
        if (propVal == null || propVal.size() == 0) {
            propVal = new Vector();
            propVal.addElement(defaultValue);
            PropertyManager._().setProperty(propName, propVal);
            //#if debug.output==verbose
            System.out.println("No default value for [" + propName
                    + "]; setting to [" + defaultValue + "]"); // debug
            //#endif
            return defaultValue;
        }
        return (String)propVal.elementAt(0);
    }

    /**
     * Used by J2ME
     */
    public static void initalizeDeviceID() {
        String[] possibleIMEIrequests = {"phone.imei", "com.nokia.mid.imei", "com.nokia.IMEI", "com.sonyericsson.imei", "IMEI", "com.motorola.IMEI", "com.samsung.imei", "com.siemens.imei"};

        String nativeValue = null;
        for (String possible : possibleIMEIrequests) {
            try {
                String value = System.getProperty(possible);

                //no good way to identify if there are Error or Magical strings here.
                if (value != null && !"".equals(value)) {
                    nativeValue = value;
                    //TODO: Do we want to sort between different IMEI's here?
                    break;
                }
            } catch (Exception e) {
                //Nothing
            }
        }

        String currentValue = PropertyManager._().getSingularProperty("DeviceID");

        if (currentValue != null) {
            if (nativeValue != null && !nativeValue.equals(currentValue)) {
                //There was a deviceID on this device, but somehow it doesn't match the current device.
                //This can happen if an app is moved between memory cards. We want to update it and log    the change
                PropertyManager._().setProperty("DeviceID", nativeValue);
                Logger.log("device", "Inconsistent DeviceID persisted. Current ID: [" + currentValue + "] - New ID: [" + nativeValue + "].");
            }
        } else {
            //No ID on the phone currently, initialize one
            String newId = nativeValue == null ? PropertyUtils.genGUID(25) : nativeValue;
            PropertyManager._().setProperty("DeviceID", newId);
            Logger.log("device", "DeviceID set: [" + newId + "]");
        }
    }


    /**
     * Generate an RFC 1422 Version 4 UUID.
     *
     * @return a uuid
     */
    public static String genUUID() {
        return randHex(8) + "-" + randHex(4) + "-4" + randHex(3) + "-" + Integer.toString(8 + MathUtils.getRand().nextInt(4), 16) + randHex(3) + "-" + randHex(12);
    }

    /**
     * Create a globally unique identifier string in no particular format
     * with len characters of randomness.
     *
     * @param len The length of the string identifier requested.
     * @return A string containing len characters of random data.
     */
    public static String genGUID(int len) {
        String guid = "";
        for (int i = 0; i < len; i++) { // 25 == 128 bits of entropy
            guid += Integer.toString(MathUtils.getRand().nextInt(36), 36);
        }
        return guid.toUpperCase();
    }

    public static String randHex(int len) {
        String ret = "";
        Random r = MathUtils.getRand();
        for (int i = 0; i < len; ++i) {
            ret += Integer.toString(r.nextInt(16), 16);
        }
        return ret;
    }

    public static String trim(String guid, int len) {
        return guid.substring(0, Math.min(len, guid.length()));
    }
}
