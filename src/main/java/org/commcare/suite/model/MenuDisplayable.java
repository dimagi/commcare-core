package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;

import io.reactivex.Observable;

/**
 * Created by wpride1 on 4/27/15.
 *
 * Interface to be implemented by objects that want to be
 * displayed in MenuLists and MenuGrids
 */
public interface MenuDisplayable {

    String getAudioURI();

    String getImageURI();

    String getDisplayText();

    Observable<String> getTextForBadge(EvaluationContext ec);

    String getCommandID();

    Text getRawBadgeTextObject();
}
