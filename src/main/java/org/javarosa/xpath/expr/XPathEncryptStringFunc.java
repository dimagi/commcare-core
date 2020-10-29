package org.javarosa.xpath.expr;


import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

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
        final int TAG_LENGTH_BIT = 128;
        final int IV_LENGTH_BYTE = 12;
        final int KEY_LENGTH_BIT = 256;

        if (!algorithm.equals("AES")) {
            throw new XPathException("Unknown algorithm \"" + algorithm +
                                     "\" for " + NAME);
        }

        Base64.Decoder keyDecoder = Base64.getUrlDecoder();
        byte[] keyBytes = keyDecoder.decode(key);
        if (8 * keyBytes.length != KEY_LENGTH_BIT) {
            throw new XPathException("Key should be " + KEY_LENGTH_BIT +
                                     " bits long, not " + 8 * keyBytes.length);
        }
        SecretKey secret = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encryptedMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            byte[] ivPlusMessage = ByteBuffer.allocate(iv.length + encryptedMessage.length)
                .put(iv)
                .put(encryptedMessage)
                .array();
            Base64.Encoder outputEncoder = Base64.getUrlEncoder();
            return outputEncoder.encodeToString(ivPlusMessage);
        } catch (Exception ex) {
            throw new XPathException("Exception during encryption: " + ex);
        }
    }
}
