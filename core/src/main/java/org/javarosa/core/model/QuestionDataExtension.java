package org.javarosa.core.model;

import org.javarosa.core.util.externalizable.Externalizable;

/**
 * Represents any single additional piece of information about a question type. A QuestionDef
 * object holds a list of such extensions, created within the XFormParser, each of which is
 * later applied to the QuestionWidget that is created from that QuestionDef during form entry
 *
 * @author amstone
 */
public interface QuestionDataExtension extends Externalizable {

}
