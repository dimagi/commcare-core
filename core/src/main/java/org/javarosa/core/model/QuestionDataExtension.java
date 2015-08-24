package org.javarosa.core.model;

import org.javarosa.core.util.externalizable.Externalizable;

/**
 * Represents some additional piece of information about a question type. A QuestionDef
 * object holds a list of such extensions, each of which can later be applied to the QuestionWidget
 * that is created from that QuestionDef during form entry.  An implementing class of
 * QuestionDataExtension must be created by a QuestionExtensionParser, and applied by a
 * QuestionExtensionReceiver
 *
 * @author amstone
 */
public interface QuestionDataExtension extends Externalizable {

}
