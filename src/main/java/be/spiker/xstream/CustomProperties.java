package be.spiker.xstream;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("root")
public class CustomProperties {

	@XStreamImplicit(itemFieldName = "dynamic-element")
	private List<DynamicElement> dynamicElements;

	public List<DynamicElement> getDynamicElements() {
		return dynamicElements;
	}

	public void setDynamicElements(List<DynamicElement> dynamicElements) {
		this.dynamicElements = dynamicElements;
	}

}
