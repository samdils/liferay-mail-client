package be.spiker;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.TreeNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import be.spiker.model.DataGroup;
import be.spiker.model.DataGroupModel;
import be.spiker.model.DataOrganisation;
import be.spiker.model.DataSite;
import be.spiker.model.DataSiteModel;
import be.spiker.model.DataUser;
import be.spiker.model.DataUserModel;
import be.spiker.model.Template;
import be.spiker.thread.MailThread;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.User;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;

@ManagedBean
@Component
@Scope(value = "request")
public class MailBean implements Serializable {

	private String comment;
	private String title;
	private String mail;
	private String templateTitle;

	private LazyDataModel<DataUser> users;
	private LazyDataModel<DataGroup> groups;
	private LazyDataModel<DataSite> sites;
	private TreeNode organizations;

	private TreeNode[] selectedNodes;

	private DataUser[] selectedUsers;
	private DataGroup[] selectedGroups;
	private DataSite[] selectedSites;

	private List<Template> templates;
	private Template selectedTemplate;

	private static Log sLog = LogFactory.getLog(MailBean.class);

	public MailBean() {

	}

	/**
	 * Sends the actual E-Mail
	 */
	public void sendMail() {

		FacesContext facesContext = LiferayFacesContext.getCurrentInstance();
		String host = PropsUtil.get("mail.session.mail.smtp.host");

		if (host == null) {
			facesContext.addMessage(null, new FacesMessage("Host is null, please configure first", ""));
		} else {

			try {

				String description = this.title;
				String sender = this.mail;
				String content = this.comment;
				String subject = description;
				String body = content;

				/*
				 * now we embed the images inside the mail.
				 */

				Map<String, Long> imageReferences = new HashMap<String, Long>();
				Document doc = Jsoup.parse(body);
				ListIterator<Element> images = doc.getElementsByTag("img").listIterator();
				int x = 0;

				while (images.hasNext()) {

					try {
						Element imageTag = images.next();
						String src = imageTag.attr("src");

						String url = src.split("\\?")[0];

						String groupId = url.split("\\/")[2];
						String folderId = url.split("\\/")[3];
						String title = URLDecoder.decode(url.split("\\/")[4], "UTF-8");

						DLFileEntry entry = DLFileEntryLocalServiceUtil.getFileEntry(new Long(groupId), new Long(folderId), title);
						// /documents/11824/0/organigramme%20DGPFP.jpg?t=1349697029656

						String ref = UUID.randomUUID().toString();
						imageReferences.put(ref, entry.getFileEntryId());
						imageTag.attr("src", "cid:" + ref);
						x++;
					} catch (Exception e) {
						sLog.error("failed to add image to mail", e);
					}
				}

				body = doc.toString();

				/**
				 * we send the mails in different threads
				 */

				ExecutorService executor = Executors.newFixedThreadPool(10);

				/*
				 * first we send mail to selected users
				 */

				List<String> emails = new ArrayList<String>();

				for (DataUser dataUser : this.selectedUsers) {

					try {

						User user = UserLocalServiceUtil.getUser(dataUser.getId());
						String mail = user.getEmailAddress();
						if (mail != null && mail.trim().length() > 0 && mail.contains("@")) {
							emails.add(mail);
						}

					} catch (Exception e) {
						sLog.warn("failed to find emailfor user : " + dataUser.getScreename());
					}

				}

				if (emails.size() > 0) {
					Runnable worker = new MailThread(sender, subject, body, host, imageReferences, emails, null);
					executor.execute(worker);
				}

				/*
				 * then we send mails to all organization users
				 */
				for (TreeNode treeNode : this.selectedNodes) {

					DataOrganisation dataOrganisation = (DataOrganisation) treeNode.getData();

					Runnable worker = new MailThread(sender, subject, body, host, imageReferences, new ArrayList<String>(), dataOrganisation.getId());
					executor.execute(worker);

				}

				/*
				 * then we send mails to the user groups
				 */

				for (DataGroup dataGroup : this.selectedGroups) {

					emails = new ArrayList<String>();

					try {
						List<User> users = UserLocalServiceUtil.getUserGroupUsers(dataGroup.getId());

						for (User user : users) {

							String mail = user.getEmailAddress();

							if (mail != null && mail.trim().length() > 0 && mail.contains("@")) {
								emails.add(mail);
							}
						}

					} catch (Exception e) {
						sLog.warn("failed to find emailfor user group : " + dataGroup.getName());
					}

					if (emails.size() > 0) {
						Runnable worker = new MailThread(sender, subject, body, host, imageReferences, emails, null);
						executor.execute(worker);
					}

				}

				/*
				 * then we send mails to site users
				 */

				for (DataSite dataSite : this.selectedSites) {

					emails = new ArrayList<String>();
					
					try {
						List<User> users = UserLocalServiceUtil.getGroupUsers(dataSite.getId());

						for (User user : users) {

							String mail = user.getEmailAddress();

							if (mail != null && mail.trim().length() > 0 && mail.contains("@")) {
								emails.add(mail);
							}
						}

					} catch (Exception e) {
						sLog.error("failed to send mail to site : " + dataSite.getName(), e);
					}
					
					if (emails.size() > 0) {
						Runnable worker = new MailThread(sender, subject, body, host, imageReferences, emails, null);
						executor.execute(worker);
					}

				}

				executor.shutdown();
				facesContext.addMessage(null, new FacesMessage("Mails sent successfully", ""));

			} catch (Exception e) {
				sLog.error("Failed send mails to community users", e);
			}
		}
	}

	// MAIL FORM ACTIONS

	/**
	 * Gets the mail Title
	 * 
	 * @return
	 */
	public String getTitle() {

		if (this.selectedTemplate != null) {
			this.title = this.selectedTemplate.getTitle();
		}

		return title;
	}

	/**
	 * Gets the Content of the Mail
	 * 
	 * @return
	 */
	public String getComment() {

		if (this.selectedTemplate != null) {
			this.comment = this.selectedTemplate.getContent();
		}

		return comment;
	}

	/**
	 * gets the current user email
	 * 
	 * @return
	 */
	public String getMail() {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		Long userId = PortalUtil.getUserId(portletRequest);

		if (!userId.equals(new Long(0))) {

			try {
				User user = UserLocalServiceUtil.getUser(userId);
				this.mail = user.getEmailAddress();
			} catch (PortalException e) {
				e.printStackTrace();
			} catch (SystemException e) {
				e.printStackTrace();
			}
		}

		if (this.selectedTemplate != null) {
			this.mail = this.selectedTemplate.getSender();
		}

		return this.mail;
	}

	/**
	 * Resets the Mail Form
	 * 
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void reset() throws PortalException, SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();

		this.comment = "";
		this.title = "";
		this.mail = PortalUtil.getUser(portletRequest).getEmailAddress();
		this.selectedUsers = new DataUser[0];
		this.selectedNodes = new TreeNode[0];
		this.selectedGroups = new DataGroup[0];
		this.selectedTemplate = null;
	}

	// MAIL TEMPLATE ACTIONS

	/**
	 * Deletes a Mail Template
	 * 
	 * @param id
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void deleteTemplate(Long id) throws PortalException, SystemException {
		JournalArticleLocalServiceUtil.deleteJournalArticle(id);
	}

	/**
	 * Saves the Template
	 * 
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void saveTemplate() throws PortalException, SystemException {
		TemplateController.saveTemplate(templateTitle, title, comment, mail);
	}

	/**
	 * gets all the Mail Templates
	 * 
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 * @throws IOException
	 */
	public List<Template> getTemplates() throws SystemException, PortalException, IOException {
		this.templates = TemplateController.getTemplates();
		return this.templates;
	}

	// LISTING OF AVAILABLE USERS

	/**
	 * Gets a list of users
	 * 
	 * @return
	 * @throws SystemException
	 */
	public LazyDataModel<DataUser> getUsers() throws SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		if (this.users != null) {
			return this.users;
		}

		int initialCapacity = UserLocalServiceUtil.getCompanyUsersCount(themeDisplay.getCompanyId());
		this.users = new DataUserModel(new ArrayList<DataUser>(initialCapacity));

		return this.users;

	}

	public void setUsers(LazyDataModel<DataUser> users) {
		this.users = users;
	}

	/**
	 * Gets all the Sites
	 * 
	 * @return
	 * @throws SystemException
	 */
	public LazyDataModel<DataSite> getSites() throws SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		if (this.sites != null) {
			return this.sites;
		}

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(Group.class, PortalClassLoaderUtil.getClassLoader());

		dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", themeDisplay.getCompanyId()));
		dynamicQuery.add(RestrictionsFactoryUtil.eq("site", true));

		int initialCapacity = new Long(GroupLocalServiceUtil.dynamicQueryCount(dynamicQuery)).intValue();
		this.sites = new DataSiteModel(new ArrayList<DataSite>(initialCapacity));

		return sites;
	}

	public void setSites(LazyDataModel<DataSite> sites) {
		this.sites = sites;
	}

	/**
	 * Get all the User Groups
	 * 
	 * @return
	 * @throws SystemException
	 */
	public LazyDataModel<DataGroup> getGroups() throws SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		if (this.groups != null) {
			return this.groups;
		}

		int initialCapacity = GroupLocalServiceUtil.getCompanyGroupsCount(themeDisplay.getCompanyId());
		this.groups = new DataGroupModel(new ArrayList<DataGroup>(initialCapacity));

		return this.groups;
	}

	public void setGroups(LazyDataModel<DataGroup> groups) {
		this.groups = groups;
	}

	/**
	 * gets a TreeNode of all Organizations
	 * 
	 * @return
	 * @throws SystemException
	 */
	public TreeNode getOrganizations() throws SystemException {

		if (this.organizations != null) {
			return this.organizations;
		}

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		Long parentOrganizationId = new Long(0);
		this.organizations = new DefaultTreeNode("Root", null);

		buildOrganizationTree(themeDisplay.getCompanyId(), parentOrganizationId, this.organizations);

		return organizations;
	}

	private void buildOrganizationTree(Long companyId, Long parentOrganizationId, TreeNode treeNode) throws SystemException {

		List<Organization> organizations = OrganizationLocalServiceUtil.getOrganizations(companyId, parentOrganizationId);

		for (Organization organization : organizations) {

			DataOrganisation dataOrganisation = new DataOrganisation(organization.getOrganizationId(), organization.getName().split("_")[0]);
			TreeNode node = new DefaultTreeNode(dataOrganisation, treeNode);

			buildOrganizationTree(companyId, organization.getOrganizationId(), node);
		}
	}

	public void setOrganizations(TreeNode organizations) {
		this.organizations = organizations;
	}

	// GETTERS AND SETTERS

	public void setTemplates(List<Template> templates) {
		this.templates = templates;
	}

	public Template getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(Template selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public DataUser[] getSelectedUsers() {
		return selectedUsers;
	}

	public void setSelectedUsers(DataUser[] selectedUsers) {
		this.selectedUsers = selectedUsers;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public TreeNode[] getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(TreeNode[] selectedNodes) {
		this.selectedNodes = selectedNodes;
	}

	public DataGroup[] getSelectedGroups() {
		return selectedGroups;
	}

	public void setSelectedGroups(DataGroup[] selectedGroups) {
		this.selectedGroups = selectedGroups;
	}

	public String getTemplateTitle() {
		return templateTitle;
	}

	public void setTemplateTitle(String templateTitle) {
		this.templateTitle = templateTitle;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public DataSite[] getSelectedSites() {
		return selectedSites;
	}

	public void setSelectedSites(DataSite[] selectedSites) {
		this.selectedSites = selectedSites;
	}

}
