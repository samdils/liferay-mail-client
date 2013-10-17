package be.spiker.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;

public class DataUserModel extends LazyDataModel<DataUser> implements Serializable {

	private static Log sLog = LogFactory.getLog(DataUserModel.class);
	private List<DataUser> users;

	public DataUserModel() {

	}

	public DataUserModel(List<DataUser> users) {
		this.users = users;
	}

	@Override
	public DataUser getRowData(String rowKey) {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		try {
			User user = UserLocalServiceUtil.getUserByScreenName(themeDisplay.getCompanyId(), rowKey);
			return buildUser(themeDisplay, user);
		} catch (Exception e) {
			sLog.error("failed to find user", e);
		}
		
		return null;
	}

	@Override
	public Object getRowKey(DataUser dataUser) {
		return dataUser.getScreename();
	}

	@Override
	public List<DataUser> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		int from = first;
		int to = ((first + 1) + pageSize);

		List<User> users = new ArrayList<User>();

		try {
			DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(User.class, PortalClassLoaderUtil.getClassLoader());

			// companyId
			dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", themeDisplay.getCompanyId()));

			// name
			if (filters.containsKey("name")) {

				String value = filters.get("name");

				if (value != null && !value.isEmpty()) {

					Criterion firstName = RestrictionsFactoryUtil.ilike("firstName", "%" + value + "%");
					Criterion lastName = RestrictionsFactoryUtil.ilike("lastName", "%" + value + "%");
					dynamicQuery.add(RestrictionsFactoryUtil.or(firstName, lastName));
				}
			}

			// screename
			if (filters.containsKey("screename")) {

				String value = filters.get("screename");

				if (value != null && !value.isEmpty()) {
					dynamicQuery.add(RestrictionsFactoryUtil.ilike("screenName", "%" + value + "%"));
				}
			}

			// roles
			if (filters.containsKey("email")) {

				String value = filters.get("email");

				if (value != null && !value.isEmpty()) {
					dynamicQuery.add(RestrictionsFactoryUtil.ilike("emailAddress", "%" + value + "%"));
				}
			}

			// roles
			if (filters.containsKey("roles")) {

				String value = filters.get("roles");

				if (value != null && !value.isEmpty()) {

					DynamicQuery dynamicQueryRole = DynamicQueryFactoryUtil.forClass(Role.class, PortalClassLoaderUtil.getClassLoader());

					// companyId
					dynamicQueryRole.add(RestrictionsFactoryUtil.eq("companyId", themeDisplay.getCompanyId()));
					dynamicQueryRole.add(RestrictionsFactoryUtil.ilike("name", "%" + value + "%"));

					List<Role> roles = RoleLocalServiceUtil.dynamicQuery(dynamicQueryRole);

					for (Role role : roles) {

						Object[] userRole = ArrayUtils.toObject(UserLocalServiceUtil.getRoleUserIds(role.getRoleId()));

						if (userRole.length > 0) {

							Collection<Object> userIds = Arrays.asList(userRole);
							dynamicQuery.add(RestrictionsFactoryUtil.in("userId", userIds));
						}
					}
				}
			}

			users = UserLocalServiceUtil.dynamicQuery(dynamicQuery, -1, -1);
			this.setRowCount(users.size());

			if (users.size() < to) {
				users = users.subList(from, users.size());
			} else {
				users = users.subList(from, to);
			}

		} catch (Exception e) {
			sLog.error("failed to query users ", e);
		}

		List<DataUser> dataUsers = new ArrayList<DataUser>();

		for (User user : users) {

			DataUser dataUser = buildUser(themeDisplay, user);

			this.users.add(first, dataUser);
			first++;

			dataUsers.add(dataUser);
		}

		return dataUsers;

	}

	private DataUser buildUser(ThemeDisplay themeDisplay, User user) {

		List<Role> roles = new ArrayList<Role>();

		try {
			roles = RoleLocalServiceUtil.getUserRelatedRoles(user.getUserId(), themeDisplay.getScopeGroupId());
		} catch (SystemException e) {
			e.printStackTrace();
		}

		return new DataUser(user.getUserId(), user.getFullName(), user.getScreenName(), user.getEmailAddress(), roles);
	}
}
