package be.spiker.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

public class DataGroupModel extends LazyDataModel<DataGroup> implements Serializable {

	private static Log sLog = LogFactory.getLog(DataGroupModel.class);
	private List<DataGroup> groups;

	public DataGroupModel() {

	}

	public DataGroupModel(List<DataGroup> groups) {
		this.groups = groups;
	}

	@Override
	public DataGroup getRowData(String rowKey) {

		try {
			UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(new Long(rowKey));
			return new DataGroup(userGroup.getUserGroupId(), userGroup.getName());
		} catch (Exception e) {
			sLog.error("failed to find group", e);
		}

		return null;
	}

	@Override
	public Object getRowKey(DataGroup group) {
		return group.getId();
	}

	@Override
	public List<DataGroup> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		int from = first;
		int to = ((first + 1) + pageSize);

		List<UserGroup> userGroups = new ArrayList<UserGroup>();

		try {
			DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(UserGroup.class, PortalClassLoaderUtil.getClassLoader());

			// companyId
			dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", themeDisplay.getCompanyId()));

			// name
			if (filters.containsKey("name")) {

				String value = filters.get("name");

				if (value != null && !value.isEmpty()) {
					dynamicQuery.add(RestrictionsFactoryUtil.ilike("name", "%" + value + "%"));
				}

			}

			userGroups = UserGroupLocalServiceUtil.dynamicQuery(dynamicQuery, -1, -1);

			this.setRowCount(userGroups.size());

			if (userGroups.size() < to) {
				userGroups = userGroups.subList(from, userGroups.size());
			} else {
				userGroups = userGroups.subList(from, to);
			}

		} catch (Exception e) {
			sLog.error("failed to query groups ", e);
		}

		List<DataGroup> groups = new ArrayList<DataGroup>();

		for (UserGroup userGroup : userGroups) {
			groups.add(new DataGroup(userGroup.getUserGroupId(), userGroup.getName()));
		}

		return groups;
	}

}
