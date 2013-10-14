package be.spiker.model;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.ListDataModel;

import org.primefaces.model.SelectableDataModel;

public class DataOrganisationModel extends ListDataModel<DataOrganisation> implements SelectableDataModel, Serializable {

	public DataOrganisationModel() {
	}

	public DataOrganisationModel(List<DataOrganisation> data) {
		super(data);
	}

	@Override
	public Object getRowData(String arg0) {

		List<DataOrganisation> orgs = (List<DataOrganisation>) getWrappedData();

		for (DataOrganisation org : orgs) {
			if (org.getId().equals(new Long(arg0)))
				return org;
		}

		return null;
	}

	@Override
	public Object getRowKey(Object arg0) {
		DataOrganisation orgs = (DataOrganisation) arg0;
		return orgs.getId();
	}

}
