package be.spiker;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.joda.time.DateTime;

import be.spiker.model.Template;
import be.spiker.xstream.CustomProperties;
import be.spiker.xstream.DynamicContent;
import be.spiker.xstream.DynamicElement;

import com.liferay.faces.portal.context.LiferayFacesContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class TemplateController implements Serializable {

	private static String TYPE_MAIL_TEMPLATE = "mail-template";

	/**
	 * Saves a new Template to the Global Webcontent
	 * 
	 * @param templateTitle
	 * @param title
	 * @param comment
	 * @param mail
	 * @throws PortalException
	 * @throws SystemException
	 */
	public static void saveTemplate(String templateTitle, String title, String comment, String mail) throws PortalException, SystemException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		List<Locale> locales = new ArrayList<Locale>();
		locales.add(themeDisplay.getLocale());
		if (LocaleUtil.getDefault() != themeDisplay.getLocale()) {
			locales.add(LocaleUtil.getDefault());
		}

		Long globalId = themeDisplay.getCompany().getGroup().getGroupId();
		User user = PortalUtil.getUser(portletRequest);

		JournalStructure journalStructure = getStructure(locales, globalId, user);
		JournalTemplate journalTemplate = verifyTemplate(locales, globalId, user, journalStructure);

		XStream xstream = new XStream(new StaxDriver());
		xstream.autodetectAnnotations(true);

		xstream.alias("dynamic-content", DynamicContent.class);
		xstream.alias("dynamic-element", DynamicElement.class);
		xstream.alias("root", CustomProperties.class);

		CustomProperties customProperties = new CustomProperties();

		List<DynamicElement> dynamicElements = new ArrayList<DynamicElement>();

		DynamicElement elementContent = new DynamicElement();
		elementContent.setName("content");
		elementContent.setDynamicContent(new DynamicContent(comment));
		dynamicElements.add(elementContent);

		DynamicElement elementSender = new DynamicElement();
		elementSender.setName("sender");
		elementSender.setDynamicContent(new DynamicContent(mail));
		dynamicElements.add(elementSender);

		DynamicElement elementTitle = new DynamicElement();
		elementTitle.setName("title");
		elementTitle.setDynamicContent(new DynamicContent(title));
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

		for (Locale locale : locales) {
			titleMap.put(locale, templateTitle);
		}

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

	/**
	 * Gets a list of available templates from the Global Webcontent
	 * 
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 * @throws IOException
	 */
	public static List<Template> getTemplates() throws SystemException, PortalException, IOException {

		PortletRequest portletRequest = (PortletRequest) LiferayFacesContext.getCurrentInstance().getExternalContext().getRequest();
		ThemeDisplay themeDisplay = (ThemeDisplay) portletRequest.getAttribute(WebKeys.THEME_DISPLAY);

		List<Locale> locales = new ArrayList<Locale>();
		locales.add(themeDisplay.getLocale());
		if (LocaleUtil.getDefault() != themeDisplay.getLocale()) {
			locales.add(LocaleUtil.getDefault());
		}

		List<Template> templates = new ArrayList<Template>();

		Long globalId = themeDisplay.getCompany().getGroup().getGroupId();
		User user = PortalUtil.getUser(portletRequest);

		JournalStructure journalStructure = getStructure(locales, globalId, user);
		verifyTemplate(locales, globalId, user, journalStructure);

		List<JournalArticle> articles = JournalArticleLocalServiceUtil.getStructureArticles(globalId, journalStructure.getStructureId());

		XStream xstream = new XStream(new StaxDriver());
		xstream.autodetectAnnotations(true);

		xstream.alias("dynamic-content", DynamicContent.class);
		xstream.alias("dynamic-element", DynamicElement.class);
		xstream.alias("root", CustomProperties.class);

		for (JournalArticle journalArticle : articles) {

			Long id = journalArticle.getId();
			String name = journalArticle.getTitle(themeDisplay.getLocale());
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

	private static JournalTemplate verifyTemplate(List<Locale> locales, Long globalId, User user, JournalStructure journalStructure) throws SystemException, PortalException {

		List<JournalTemplate> templates = JournalTemplateLocalServiceUtil.getStructureTemplates(globalId, journalStructure.getStructureId());

		if (templates.isEmpty()) {

			long userId = user.getUserId();
			long groupId = globalId;
			String templateId = "";
			boolean autoTemplateId = true;
			String structureId = journalStructure.getStructureId();

			Map<Locale, String> nameMap = new HashMap<Locale, String>();
			for (Locale locale : locales) {
				nameMap.put(locale, TYPE_MAIL_TEMPLATE);
			}

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

	private static JournalStructure getStructure(List<Locale> locales, Long globalId, User user) throws SystemException, PortalException {

		List<JournalStructure> structures = JournalStructureLocalServiceUtil.getStructures(globalId);

		for (JournalStructure structure : structures) {

			for (Locale locale : locales) {

				String name = structure.getName(locale);

				if (name.equals(TYPE_MAIL_TEMPLATE)) {
					return structure;
				}
			}
		}

		long userId = user.getUserId();
		long groupId = globalId;
		String structureId = "";
		boolean autoStructureId = true;
		String parentStructureId = null;

		Map<Locale, String> nameMap = new HashMap<Locale, String>();

		for (Locale locale : locales) {
			nameMap.put(locale, TYPE_MAIL_TEMPLATE);
		}

		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
		String xsd = "<root><dynamic-element name=\"title\" type=\"text\" index-type=\"\" repeatable=\"false\"/><dynamic-element name=\"content\" type=\"text_area\" index-type=\"\" repeatable=\"false\"/><dynamic-element name=\"sender\" type=\"text\" index-type=\"\" repeatable=\"false\"/></root>";

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setScopeGroupId(groupId);

		return JournalStructureLocalServiceUtil.addStructure(userId, groupId, structureId, autoStructureId, parentStructureId, nameMap, descriptionMap, xsd, serviceContext);
	}
}
