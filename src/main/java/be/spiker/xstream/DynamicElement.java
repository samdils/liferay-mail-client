package be.spiker.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("dynamic-element")
public class DynamicElement {

	@XStreamAsAttribute
	private String name;

	@XStreamAlias("dynamic-content")
	private DynamicContent dynamicContent;

	public DynamicContent getDynamicContent() {
		return dynamicContent;
	}

	public void setDynamicContent(DynamicContent dynamicContent) {
		this.dynamicContent = dynamicContent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
