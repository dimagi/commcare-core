package org.javarosa.xpath.expr;


import static org.commcare.util.EncryptionKeyHelper.CC_KEY_ALGORITHM_AES;

import org.commcare.util.EncryptionHelper;
import org.commcare.util.EncryptionKeyHelper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;


public class XPathEncryptStringFunc extends XPathFuncExpr {
    public static final String NAME = "encrypt-string";
    private static final int EXPECTED_ARG_COUNT = 3;
    private EncryptionHelper encryptionHelper = new EncryptionHelper();

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
     * @param o1   a message to be encrypted
     * @param o2       the key used for encryption
     * @param o3 the encryption algorithm to use
     */
    private String encryptString(Object o1, Object o2, Object o3) {
        String message = FunctionUtils.toString(o1);
        String key = FunctionUtils.toString(o2);
        String algorithm = FunctionUtils.toString(o3);

        if (!algorithm.equals(CC_KEY_ALGORITHM_AES)) {
            throw new XPathException("Unknown algorithm \"" + algorithm +
                    "\" for " + NAME);
        }

        try {
            return encryptionHelper.encryptWithEncodedKey(message, key);
        } catch (EncryptionHelper.EncryptionException |
                 EncryptionKeyHelper.EncryptionKeyException e) {
            throw new XPathException(e);
        }
    }
}