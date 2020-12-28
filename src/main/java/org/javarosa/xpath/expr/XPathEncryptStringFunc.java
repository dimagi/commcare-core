package org.javarosa.xpath.expr;


import org.commcare.util.Base64;
import org.commcare.util.Base64DecoderException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;

public class XPathEncryptStringFunc extends XPathFuncExpr {
    public static final String NAME = "encrypt-string";
    private static final int EXPECTED_ARG_COUNT = 3;

    public XPathEncryptStringFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathEncryptStringFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return encryptString(evaluatedArgs[0], evaluatedArgs[1], evaluatedArgs[2]);
    }

    /**
     * Encrypt a message with the given algorithm and key.
     *
     * @param message    a message to be encrypted
     * @param key        the key used for encryption
     * @param algorithm  the encryption algorithm to use
     */
    private static String encryptString(Object o1, Object o2, Object o3) {
        String message = FunctionUtils.toString(o1);
        String key = FunctionUtils.toString(o2);
        String algorithm = FunctionUtils.toString(o3);

        final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
        final int MIN_IV_LENGTH_BYTE = 1;
        final int MAX_IV_LENGTH_BYTE = 255;
        final int KEY_LENGTH_BIT = 256;

        if (!algorithm.equals("AES")) {
            throw new XPathException("Unknown algorithm \"" + algorithm +
                                     "\" for " + NAME);
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(key);
        } catch (Base64DecoderException e) {
            XPathException throwable = new XPathException("Encryption key base 64 encoding is invalid");
            throwable.initCause(e);
            throw throwable;
        }
        if (8 * keyBytes.length != KEY_LENGTH_BIT) {
            throw new XPathException("Key should be " + KEY_LENGTH_BIT +
                                     " bits long, not " + 8 * keyBytes.length);
        }
        SecretKey secret = new SecretKeySpec(keyBytes, "AES");

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes(Charset.forName("UTF-8")));
            byte[] iv = cipher.getIV();
            if (iv.length < MIN_IV_LENGTH_BYTE || iv.length > MAX_IV_LENGTH_BYTE) {
                throw new XPathException("Initialization vector should be between " +
                                         MIN_IV_LENGTH_BYTE + " and " + MAX_IV_LENGTH_BYTE +
                                         " bytes long, but it is " + iv.length + " bytes");
            }
            // The conversion of iv.length to byte takes the low 8 bits. To
            // convert back, cast to int and mask with 0xFF.
            byte[] ivPlusMessage = ByteBuffer.allocate(1 + iv.length + encryptedMessage.length)
                .put((byte) iv.length)
                .put(iv)
                .put(encryptedMessage)
                .array();
            return Base64.encode(ivPlusMessage);
        } catch (Exception ex) {
            throw new XPathException("Exception during encryption: " + ex);
        }
    }
}
