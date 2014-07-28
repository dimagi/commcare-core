package org.commcare.suite.model.graph;

public interface ConfigurableData {

	public void setConfiguration(String key, String value);

	public String getConfiguration(String key);

}
