package org.javarosa.core.model;

/**
 * Any class that needs to be able to receive/apply info from a QuestionDataExtension should
 * implement this interface.
 *
 * @author amstone
 */
public interface QuestionExtensionReceiver {

    void applyExtension(QuestionDataExtension extension);

}
