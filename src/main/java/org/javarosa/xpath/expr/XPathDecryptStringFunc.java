package org.javarosa.xpath.expr;

import static org.commcare.util.EncryptionUtils.decryptWithBase64EncodedKey;
import static org.commcare.util.EncryptionUtils.encryptionKeyProvider;

import org.commcare.util.EncryptionUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathDecryptStringFunc extends XPathFuncExpr {

    public static final String NAME = "decrypt-string";
    private static final int EXPECTED_ARG_COUNT = 3;

    public XPathDecryptStringFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDecryptStringFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return decryptString(evaluatedArgs[0], evaluatedArgs[1], evaluatedArgs[2]);
    }

    /**
     * Encrypt a message with the given algorithm and key.
     *
     * @param o1 a message to be decrypted
     * @param o2 the key used for encryption
     * @param o3 the encryption algorithm to use
     */
    private static String decryptString(Object o1, Object o2, Object o3) {
        String message = FunctionUtils.toString(o1);
        String key = FunctionUtils.toString(o2);
        String algorithm = FunctionUtils.toString(o3);

        if (!algorithm.equals(encryptionKeyProvider.getAESKeyAlgorithmRepresentation()) &&
                !algorithm.equals(encryptionKeyProvider.getRSAKeyAlgorithmRepresentation())) {
            throw new XPathException("Unknown algorithm \"" + algorithm +
                    "\" for " + NAME);
        }

        try {
            return decryptWithBase64EncodedKey(algorithm, message, key);
        } catch (EncryptionUtils.EncryptionException e) {
            throw new XPathException(e);
        }
    }
}
