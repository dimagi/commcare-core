package org.javarosa.xform.parse;

import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.expr.XPathPathExpr;

import static org.javarosa.xform.parse.XFormParser.getAbsRef;

public class ItemSetParsingUtils {

    public static void setLabel(ItemsetBinding itemset, String labelXpath) {
        boolean labelItext = false;
        if (labelXpath != null) {
            if (labelXpath.startsWith("jr:itext(") && labelXpath.endsWith(")")) {
                labelXpath = labelXpath.substring("jr:itext(".length(), labelXpath.indexOf(")"));
                labelItext = true;
            }
        } else {
            throw new XFormParseException("<label> in <itemset> requires 'ref'");
        }

        XPathPathExpr labelPath = XPathReference.getPathExpr(labelXpath);
        itemset.labelRef = FormInstance.unpackReference(getAbsRef(new XPathReference(labelPath), itemset.nodesetRef));
        itemset.labelExpr = new XPathConditional(labelPath);
        itemset.labelIsItext = labelItext;
    }

    public static void setValue(ItemsetBinding itemset, String valueXpath) {
        if (valueXpath == null) {
            throw new XFormParseException("<value> in <itemset> requires 'ref'");
        }

        XPathPathExpr valuePath = XPathReference.getPathExpr(valueXpath);
        itemset.valueRef = FormInstance.unpackReference(getAbsRef(new XPathReference(valuePath), itemset.nodesetRef));
        itemset.valueExpr = new XPathConditional(valuePath);
        itemset.copyMode = false;
    }

    public static void setSort(ItemsetBinding itemset, String sortXpathString) {
        if (sortXpathString == null) {
            throw new XFormParseException("<sort> in <itemset> requires 'ref'");
        }

        XPathPathExpr sortPath = XPathReference.getPathExpr(sortXpathString);
        itemset.sortRef = FormInstance.unpackReference(getAbsRef(new XPathReference(sortPath), itemset.nodesetRef));
        itemset.sortExpr = new XPathConditional(sortPath);
    }

    public static void setNodeset(ItemsetBinding itemset, String nodesetStr, String elementName) {
        if (nodesetStr == null) {
            throw new RuntimeException("No nodeset attribute in element: " + elementName);
        }

        XPathPathExpr path = XPathReference.getPathExpr(nodesetStr);
        itemset.nodesetExpr = new XPathConditional(path);
        XPathReference nodesetRef;
        nodesetRef = getAbsRef(new XPathReference(path.getReference()), itemset.contextRef);
        itemset.nodesetRef = FormInstance.unpackReference(nodesetRef);
    }
}
