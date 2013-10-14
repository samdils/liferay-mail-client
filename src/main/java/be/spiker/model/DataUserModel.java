package be.spiker.model;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.ListDataModel;

import org.primefaces.model.SelectableDataModel;

public class DataUserModel extends ListDataModel<DataUser> implements SelectableDataModel, Serializable {

	public DataUserModel() {
	}

	public DataUserModel(List<DataUser> data) {
		super(data);
	}

	@Override
	public Object getRowData(String arg0) {

		List<DataUser> cusers = (List<DataUser>) getWrappedData();

		for (DataUser cuser : cusers) {
			if (cuser.screename.equals(arg0))
				return cuser;
		}

		return null;
	}

	@Override
	public Object getRowKey(Object arg0) {
		DataUser cusers = (DataUser) arg0;
		return cusers.getScreename();
	}

}
