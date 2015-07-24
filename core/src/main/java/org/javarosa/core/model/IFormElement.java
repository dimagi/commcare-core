package org.javarosa.core.model;

import java.util.Vector;

import org.javarosa.core.services.locale.Localizable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.model.xform.XPathReference;

/**
 * An IFormDataElement is an element of the physical interaction for
 * a form, an example of an implementing element would be the definition
 * of a Question.
 *
 * @author Drew Roos
 */
public interface IFormElement extends Localizable, Externalizable {

    /**
     * @return The unique ID of this element
     */
    int getID();

    /**
     * @param id The new unique ID of this element
     */
    void setID(int id);

    /**
     * get the TextID for this element used for localization purposes
     *
     * @return the TextID (bare, no ;form appended to it!!)
     */
    String getTextID();

    /**
     * Set the textID for this element for use with localization.
     *
     * @param id the plain TextID WITHOUT any form specification (e.g. ;long)
     */
    void setTextID(String id);


    /**
     * @return A vector containing any children that this element
     * might have. Null if the element is not able to have child
     * elements.
     */
    Vector<IFormElement> getChildren();

    /**
     * @param v the children of this element, if it is capable of having
     *          child elements.
     * @throws IllegalStateException if the element is incapable of
     *                               having children.
     */
    void setChildren(Vector<IFormElement> v);

    /**
     * @param fe The child element to be added
     * @throws IllegalStateException if the element is incapable of
     *                               having children.
     */
    void addChild(IFormElement fe);

    IFormElement getChild(int i);

    /**
     * @return A recursive count of how many elements are ancestors of this element.
     */
    int getDeepChildCount();

    /**
     * @return The data reference for this element
     */
    XPathReference getBind();

    /**
     * Registers a state observer for this element.
     */
    public void registerStateObserver(FormElementStateListener qsl);

    /**
     * Unregisters a state observer for this element.
     */
    public void unregisterStateObserver(FormElementStateListener qsl);

    /**
     * This method returns the regular
     * innertext betweem label tags (if present) (&ltlabel&gtinnertext&lt/label&gt).
     *
     * @return &ltlabel&gt innertext or null (if innertext is not present).
     */
    public String getLabelInnerText();

    public String getAppearanceAttr();

    public void setAppearanceAttr(String appearanceAttr);
}
