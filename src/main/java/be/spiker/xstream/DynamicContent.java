package be.spiker.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

@XStreamAlias("dynamic-content")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = { "value" })
public class DynamicContent {

	private String value;

	public DynamicContent(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
