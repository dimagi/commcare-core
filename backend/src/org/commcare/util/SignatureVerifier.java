package org.commcare.util;

/**
 * Created by amstone326 on 4/6/16.
 */
public abstract class SignatureVerifier {

    public abstract boolean verify(String message, String signature);

}
