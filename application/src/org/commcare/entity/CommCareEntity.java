/**
 * 
 */
package org.commcare.entity;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.entity.model.Entity;

/**
 * @author ctsims
 *
 */
public class CommCareEntity<E extends Persistable> extends Entity<E> {
	
	Entry e;
	Detail shortDetail;
	Detail longDetail;
	FormInstanceLoader<E> loader;
	FormInstance instance;
	
	public CommCareEntity(Entry e, Detail shortDetail, Detail longDetail, FormInstanceLoader<E> loader) {
		this.e = e;
		this.shortDetail = shortDetail;
		this.longDetail = longDetail;
		this.loader = loader;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#entityType()
	 */
	public String entityType() {
		return shortDetail.getTitle().evaluate();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#factory()
	 */
	public Entity<E> factory() {
		return new CommCareEntity<E>(e,shortDetail,longDetail, loader);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getHeaders(boolean)
	 */
	public String[] getHeaders(boolean detailed) {
		Text[] text;
		if(!detailed) {
			text = shortDetail.getHeaders();
		} else{
			text = longDetail.getHeaders();
		}
		
		String[] output = new String[text.length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = text[i].evaluate(instance);
		}
		return output;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getLongFields(org.javarosa.core.services.storage.Persistable)
	 */
	public String[] getLongFields(E e) {
		loader.prepare(e);
		FormInstance specificInstance = loader.loadInstance(longDetail.getInstance());
		Text[] text = longDetail.getTemplates();
		String[] output = new String[text.length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = text[i].evaluate(specificInstance);
		}
		return output;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getShortFields()
	 */
	public String[] getShortFields() {
		Text[] text = shortDetail.getTemplates();
		String[] output = new String[text.length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = text[i].evaluate(instance);
		}
		return output;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#loadEntity(org.javarosa.core.services.storage.Persistable)
	 */
	protected void loadEntity(E entity) {
		loader.prepare(entity);
		instance = loader.loadInstance(shortDetail.getInstance());
	}
}
