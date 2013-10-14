package be.spiker;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import be.spiker.model.DataGroup;
import be.spiker.model.DataGroupModel;
import be.spiker.model.DataOrganisation;
import be.spiker.model.DataUser;
import be.spiker.model.DataUserModel;
import be.spiker.model.Template;
import be.spiker.thread.MailThread;
import be.spiker.xstream.CustomProperties;
import be.spiker.xstream.DynamicContent;
import be.spiker.xstream.DynamicElement;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.PortalPreferences;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@ManagedBean(name = "eformAdminManagedBean")
@Component
@Scope(value = "session")
public class MailBean implements Serializable {

	private static String TYPE_MAIL_TEMPLATE = "mail-template";

	private String namespace = "liferay-portal-namespace";

	private String comment;
	private String title;
	private String mail;
	private String templateTitle;

	private TreeNode root;
	private TreeNode[] selectedNodes;

	private DataUser[] selectedUsers;
	private DataGroup[] selectedGroups;

	private List<Template> templates;
	private Template selectedTemplate;

	private static Log sLog = LogFactory.getLog(MailBean.class);

	public MailBean() {

	}

	public void deleteTemplate(Long id) throws PortalException, SystemException {
		JournalArticleLocalServiceUtil.deleteJournalArticle(id);
	}

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

	public void submitMailForm() {
		sLog.error("submitting the form : " + this.title);
	}

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

	public void changeMailTitle(ValueChangeEvent event) {
		this.title = (String) event.getNewValue();
	}

	public void changeMailContent(ValueChangeEvent event) {
		this.comment = (String) event.getNewValue();
	}

	public void changeMailSender(ValueChangeEvent event) {
		this.mail = (String) event.getNewValue();
	}

	public void saveTemplate() throws PortalException, SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		Long globalId = themeDisplay.getCompany().getGroup().getGroupId();
		User user = PortalUtil.getUser(portletRequest);

		JournalStructure journalStructure = getStructure(globalId, user);
		JournalTemplate journalTemplate = verifyTemplate(globalId, user, journalStructure);

		XStream xstream = new XStream(new StaxDriver());
		xstream.autodetectAnnotations(true);

		xstream.alias("dynamic-content", DynamicContent.class);
		xstream.alias("dynamic-element", DynamicElement.class);
		xstream.alias("root", CustomProperties.class);

		CustomProperties customProperties = new CustomProperties();

		List<DynamicElement> dynamicElements = new ArrayList<DynamicElement>();

		DynamicElement elementContent = new DynamicElement();
		elementContent.setName("content");
		elementContent.setDynamicContent(new DynamicContent(this.comment));
		dynamicElements.add(elementContent);

		DynamicElement elementSender = new DynamicElement();
		elementSender.setName("sender");
		elementSender.setDynamicContent(new DynamicContent(this.mail));
		dynamicElements.add(elementSender);

		DynamicElement elementTitle = new DynamicElement();
		elementTitle.setName("title");
		elementTitle.setDynamicContent(new DynamicContent(this.title));
		dynamicElements.add(elementTitle);

		customProperties.setDynamicElements(dynamicElements);

		String content = xstream.toXML(customProperties);

		long userId = user.getUserId();
		long groupId = globalId;
		long classNameId = new Long(0);
		long classPK = new Long(0);
		String articleId = "";
		boolean autoArticleId = true;
		double version = 1.0;

		Map<Locale, String> titleMap = new HashMap<Locale, String>();
		titleMap.put(Locale.FRANCE, this.templateTitle);

		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
		String type = "general";
		String structureId = journalStructure.getStructureId();
		String templateId = journalTemplate.getTemplateId();
		String layoutUuid = "";

		int displayDateMonth = new DateTime().getMonthOfYear();
		int displayDateDay = new DateTime().getDayOfMonth();
		int displayDateYear = new DateTime().getYear();
		int displayDateHour = 0;
		int displayDateMinute = 0;

		int expirationDateMonth = new DateTime().getMonthOfYear();
		int expirationDateDay = new DateTime().getDayOfMonth();
		int expirationDateYear = new DateTime().plusYears(100).getYear();
		int expirationDateHour = 0;
		int expirationDateMinute = 0;

		boolean neverExpire = true;

		int reviewDateMonth = new DateTime().getMonthOfYear();
		int reviewDateDay = new DateTime().getDayOfMonth();
		int reviewDateYear = new DateTime().plusYears(100).getYear();
		int reviewDateHour = 0;
		int reviewDateMinute = 0;

		boolean neverReview = true;
		boolean indexable = true;
		boolean smallImage = false;

		String smallImageURL = "";
		File smallImageFile = null;
		Map<String, byte[]> images = new HashMap<String, byte[]>();
		String articleURL = "";

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setScopeGroupId(globalId);

		JournalArticleLocalServiceUtil.addArticle(userId, groupId, classNameId, classPK, articleId, autoArticleId, version, titleMap, descriptionMap, content, type, structureId, templateId, layoutUuid, displayDateMonth, displayDateDay, displayDateYear, displayDateHour, displayDateMinute, expirationDateMonth, expirationDateDay, expirationDateYear, expirationDateHour, expirationDateMinute, neverExpire, reviewDateMonth, reviewDateDay, reviewDateYear, reviewDateHour, reviewDateMinute, neverReview,
				indexable, smallImage, smallImageURL, smallImageFile, images, articleURL, serviceContext);

	}

	public void sendMail() {

		FacesContext facesContext = LiferayFacesContext.getCurrentInstance();
		ExternalContext externalContext = LiferayFacesContext.getCurrentInstance().getExternalContext();
		PortletRequest portletRequest = (PortletRequest) externalContext.getRequest();

		String host = getPortalPreference(portletRequest, "mail.session.mail.smtp.host", null);

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

				/**
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

				/**
				 * then we send mails to all organization users
				 */
				for (TreeNode treeNode : this.selectedNodes) {

					DataOrganisation dataOrganisation = (DataOrganisation) treeNode.getData();

					Runnable worker = new MailThread(sender, subject, body, host, imageReferences, new ArrayList<String>(), dataOrganisation.getId());
					executor.execute(worker);

				}

				/**
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

				executor.shutdown();

				facesContext.addMessage(null, new FacesMessage("Mails sent successfully", ""));

			} catch (Exception e) {
				sLog.error("Failed send mails to community users", e);
			}
		}
	}

	public DataUserModel getUsers() throws SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		List<User> users = UserLocalServiceUtil.getGroupUsers(themeDisplay.getScopeGroupId());
		List<DataUser> cUsers = new ArrayList<DataUser>();

		for (User user : users) {

			List<Role> roles = RoleLocalServiceUtil.getUserRelatedRoles(user.getUserId(), themeDisplay.getScopeGroupId());

			StringBuilder builder = new StringBuilder();
			for (Role role : roles) {
				builder.append(role.getName() + ", ");
			}
			cUsers.add(new DataUser(user.getUserId(), user.getFullName(), user.getScreenName(), builder.toString()));
		}

		return new DataUserModel(cUsers);
	}

	public String getComment() {

		if (this.selectedTemplate != null) {
			this.comment = this.selectedTemplate.getContent();
		}

		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getTitle() {

		if (this.selectedTemplate != null) {
			this.title = this.selectedTemplate.getTitle();
		}

		return title;
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

	public TreeNode getRoot() throws SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		Long parentOrganizationId = new Long(0);
		this.root = new DefaultTreeNode("Root", null);

		buildOrganizationTree(themeDisplay.getCompanyId(), parentOrganizationId, this.root);

		return root;
	}

	private void buildOrganizationTree(Long companyId, Long parentOrganizationId, TreeNode treeNode) throws SystemException {

		List<Organization> organizations = OrganizationLocalServiceUtil.getOrganizations(companyId, parentOrganizationId);

		for (Organization organization : organizations) {

			DataOrganisation dataOrganisation = new DataOrganisation(organization.getOrganizationId(), organization.getName().split("_")[0]);
			TreeNode node = new DefaultTreeNode(dataOrganisation, treeNode);

			buildOrganizationTree(companyId, organization.getOrganizationId(), node);
		}
	}

	public void setRoot(TreeNode root) {
		this.root = root;
	}

	public TreeNode[] getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(TreeNode[] selectedNodes) {
		this.selectedNodes = selectedNodes;
	}

	public DataGroupModel getGroups() throws SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		List<DataGroup> groups = new ArrayList<DataGroup>();

		List<UserGroup> userGroups = UserGroupLocalServiceUtil.getUserGroups(themeDisplay.getCompanyId());
		for (UserGroup userGroup : userGroups) {
			groups.add(new DataGroup(userGroup.getUserGroupId(), userGroup.getName()));
		}

		return new DataGroupModel(groups);
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

	public List<Template> getTemplates() throws SystemException, PortalException, IOException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		this.templates = new ArrayList<Template>();

		Long globalId = themeDisplay.getCompany().getGroup().getGroupId();
		User user = PortalUtil.getUser(portletRequest);

		JournalStructure journalStructure = getStructure(globalId, user);
		verifyTemplate(globalId, user, journalStructure);

		List<JournalArticle> articles = JournalArticleLocalServiceUtil.getStructureArticles(globalId, journalStructure.getStructureId());

		XStream xstream = new XStream(new StaxDriver());
		xstream.autodetectAnnotations(true);

		xstream.alias("dynamic-content", DynamicContent.class);
		xstream.alias("dynamic-element", DynamicElement.class);
		xstream.alias("root", CustomProperties.class);

		for (JournalArticle journalArticle : articles) {

			Long id = journalArticle.getId();
			String name = journalArticle.getTitle(Locale.FRANCE);
			String title = "";
			String content = "";
			String sender = "";

			CustomProperties customProperties = (CustomProperties) xstream.fromXML(journalArticle.getContent());
			for (DynamicElement dynamicElement : customProperties.getDynamicElements()) {

				if (dynamicElement.getName().equals("content")) {
					content = dynamicElement.getDynamicContent().getValue();
				} else if (dynamicElement.getName().equals("sender")) {
					sender = dynamicElement.getDynamicContent().getValue();
				} else if (dynamicElement.getName().equals("title")) {
					title = dynamicElement.getDynamicContent().getValue();
				}
			}

			templates.add(new Template(id, name, title, content, sender));
		}

		return templates;
	}

	private JournalTemplate verifyTemplate(Long globalId, User user, JournalStructure journalStructure) throws SystemException, PortalException {

		List<JournalTemplate> templates = JournalTemplateLocalServiceUtil.getStructureTemplates(globalId, journalStructure.getStructureId());

		if (templates.isEmpty()) {

			long userId = user.getUserId();
			long groupId = globalId;
			String templateId = "";
			boolean autoTemplateId = true;
			String structureId = journalStructure.getStructureId();

			Map<Locale, String> nameMap = new HashMap<Locale, String>();
			nameMap.put(Locale.FRANCE, TYPE_MAIL_TEMPLATE);

			Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
			String xsl = "<div></div>";
			boolean formatXsl = false;
			String langType = "vm";
			boolean cacheable = true;
			boolean smallImage = false;
			String smallImageURL = "";
			File smallImageFile = null;

			ServiceContext serviceContext = new ServiceContext();
			serviceContext.setScopeGroupId(groupId);

			return JournalTemplateLocalServiceUtil.addTemplate(userId, groupId, templateId, autoTemplateId, structureId, nameMap, descriptionMap, xsl, formatXsl, langType, cacheable, smallImage, smallImageURL, smallImageFile, serviceContext);

		} else {
			return templates.get(0);
		}
	}

	private JournalStructure getStructure(Long globalId, User user) throws SystemException, PortalException {

		List<JournalStructure> structures = JournalStructureLocalServiceUtil.getStructures(globalId);

		for (JournalStructure structure : structures) {

			String name = structure.getName(Locale.FRANCE);

			if (name.equals(TYPE_MAIL_TEMPLATE)) {
				return structure;
			}
		}

		long userId = user.getUserId();
		long groupId = globalId;
		String structureId = "";
		boolean autoStructureId = true;
		String parentStructureId = null;

		Map<Locale, String> nameMap = new HashMap<Locale, String>();
		nameMap.put(Locale.FRANCE, TYPE_MAIL_TEMPLATE);

		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
		String xsd = "<root><dynamic-element name=\"title\" type=\"text\" index-type=\"\" repeatable=\"false\"/><dynamic-element name=\"content\" type=\"text_area\" index-type=\"\" repeatable=\"false\"/><dynamic-element name=\"sender\" type=\"text\" index-type=\"\" repeatable=\"false\"/></root>";

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setScopeGroupId(groupId);

		return JournalStructureLocalServiceUtil.addStructure(userId, groupId, structureId, autoStructureId, parentStructureId, nameMap, descriptionMap, xsd, serviceContext);
	}

	public void setTemplates(List<Template> templates) {
		this.templates = templates;
	}

	public Template getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(Template selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public String getPortalPreference(PortletRequest renderRequest, String key, String defaultValue) {

		try {
			PortalPreferences preferences = PortletPreferencesFactoryUtil.getPortalPreferences(renderRequest);
			return preferences.getValue(namespace, key, defaultValue);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return defaultValue;

	}

}
