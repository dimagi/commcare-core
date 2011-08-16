/**
 * 
 */
package org.commcare.view;

import java.util.Vector;

import javax.microedition.lcdui.Command;

import org.commcare.util.CommCareSense;
import org.commcare.util.MultimediaListener;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.formmanager.view.CustomChoiceGroup;
import org.javarosa.utilities.media.MediaUtils;

import de.enough.polish.ui.ChoiceGroup;
import de.enough.polish.ui.List;

/**
 * @author ctsims
 *
 */
public class CommCareListView extends List implements MultimediaListener {	
	
	private Vector<String> audioLocations = new Vector<String>();
	
	public CommCareListView(String title) {
		this(title, CommCareSense.formEntryQuick(), !CommCareSense.formEntryQuick());
	}
	
	public CommCareListView(String title, boolean autoSelect, boolean numericNavigation) {
		super(title, List.IMPLICIT);
		this.choiceGroup = new CustomChoiceGroup(null, ChoiceGroup.IMPLICIT, autoSelect, numericNavigation) {
			public void playAudio(int index) {
				if(audioLocations.size() > index && audioLocations.elementAt(index) != null) {
					MediaUtils.playAudio(audioLocations.elementAt(index));
				}
			}
		};
		this.choiceGroup.isFocused = true;
		this.container = this.choiceGroup;
	}

	public void registerAudioTrigger(int index, String audioURI) {
		if(audioLocations.size() < index+1) {
			audioLocations.setSize(index+1);
		}
		audioLocations.setElementAt(audioURI, index);
	}
}
