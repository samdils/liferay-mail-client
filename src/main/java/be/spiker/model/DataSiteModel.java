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
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

public class DataSiteModel extends LazyDataModel<DataSite> implements Serializable {

	private static Log sLog = LogFactory.getLog(DataSiteModel.class);
	private List<DataSite> sites;

	public DataSiteModel() {

	}

	public DataSiteModel(List<DataSite> sites) {
		this.sites = sites;
	}

	@Override
	public DataSite getRowData(String rowKey) {

		try {
			Group group = GroupLocalServiceUtil.getGroup(new Long(rowKey));
			return new DataSite(group.getGroupId(), group.getName());
		} catch (Exception e) {
			sLog.error("failed to find site", e);
		}
		
		return null;

	}

	@Override
	public Object getRowKey(DataSite site) {
		return site.getId();
	}

	@Override
	public List<DataSite> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		int from = first;
		int to = ((first + 1) + pageSize);

		List<Group> siteGroups = new ArrayList<Group>();

		try {
			DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(Group.class, PortalClassLoaderUtil.getClassLoader());

			// companyId
			dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", themeDisplay.getCompanyId()));
			dynamicQuery.add(RestrictionsFactoryUtil.eq("site", true));

			// name
			if (filters.containsKey("name")) {

				String value = filters.get("name");

				if (value != null && !value.isEmpty()) {
					dynamicQuery.add(RestrictionsFactoryUtil.ilike("name", "%" + value + "%"));
				}

			}

			siteGroups = GroupLocalServiceUtil.dynamicQuery(dynamicQuery, -1, -1);

			this.setRowCount(siteGroups.size());

			if (siteGroups.size() < to) {
				siteGroups = siteGroups.subList(from, siteGroups.size());
			} else {
				siteGroups = siteGroups.subList(from, to);
			}

		} catch (Exception e) {
			sLog.error("failed to query groups ", e);
		}

		List<DataSite> sites = new ArrayList<DataSite>();

		for (Group group : siteGroups) {
			sites.add(new DataSite(group.getGroupId(), group.getName()));
		}

		return sites;
	}
}
