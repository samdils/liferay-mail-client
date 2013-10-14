package be.spiker.model;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.ListDataModel;

import org.primefaces.model.SelectableDataModel;

public class DataGroupModel extends ListDataModel<DataGroup> implements SelectableDataModel, Serializable{

	public DataGroupModel() {

	}

	public DataGroupModel(List<DataGroup> data) {
		super(data);
	}

	@Override
	public Object getRowData(String arg0) {

		List<DataGroup> cusers = (List<DataGroup>) getWrappedData();

		for (DataGroup cuser : cusers) {
			if (cuser.getId().equals(new Long(arg0))){
				return cuser;
			}
		}

		return null;
	}

	@Override
	public Object getRowKey(Object arg0) {

		DataGroup cusers = (DataGroup) arg0;
		return cusers.getId();
	}

}
