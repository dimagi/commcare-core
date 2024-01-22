package org.javarosa.xpath.expr;


import org.commcare.util.EncryptionHelper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import static org.commcare.util.EncryptionKeyHelper.CC_KEY_ALGORITHM_AES;

public class XPathDecryptStringFunc extends XPathFuncExpr {

    public static final String NAME = "decrypt-string";
    private static final int EXPECTED_ARG_COUNT = 3;
    private EncryptionHelper encryptionHelper = new EncryptionHelper();

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
    private String decryptString(Object o1, Object o2, Object o3) {
        String message = FunctionUtils.toString(o1);
        String key = FunctionUtils.toString(o2);
        String algorithm = FunctionUtils.toString(o3);


        if (!algorithm.equals(CC_KEY_ALGORITHM_AES)) {
            throw new XPathException("Unknown algorithm \"" + algorithm +
                    "\" for " + NAME);
        }

        try {
            return encryptionHelper.decryptWithBase64EncodedKey(message, key);
        } catch (EncryptionHelper.EncryptionException e) {
            throw new XPathException(e);
        }
    }
}
