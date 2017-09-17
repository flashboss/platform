/**
 * Copyright ( C ) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.platform.portlet.juzu.calendar;

import static java.lang.Integer.parseInt;
import static java.lang.Math.round;
import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateInstance;
import static java.util.Arrays.asList;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.getInstance;
import static java.util.Collections.sort;
import static java.util.ResourceBundle.getBundle;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.Collectors.toList;
import static juzu.impl.common.Tools.UTF_8;
import static org.apache.commons.lang.ArrayUtils.add;
import static org.apache.commons.lang.ArrayUtils.removeElement;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.HOME_PAGE_CALENDAR_SETTINGS;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.JOUR_MS;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.contains;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.getBeginDay;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.getCurrentCalendar;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.getCurrentUser;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.getCurrentUserCalendarSetting;
import static org.exoplatform.platform.portlet.juzu.calendar.models.CalendarPortletUtils.getEndDay;
import static org.exoplatform.calendar.model.Event.NEEDS_ACTION;
import static org.exoplatform.calendar.model.Event.TYPE_EVENT;
import static org.exoplatform.calendar.model.Event.TYPE_TASK;
import static org.exoplatform.calendar.service.Calendar.build;
import static org.exoplatform.calendar.service.Utils.EXO_FROM_DATE_TIME;
import static org.exoplatform.calendar.service.Utils.getDefaultCalendarId;
import static org.exoplatform.calendar.service.impl.NewUserListener.defaultCalendarName;
import static org.exoplatform.commons.api.settings.SettingValue.create;
import static org.exoplatform.commons.api.settings.data.Context.USER;
import static org.exoplatform.commons.api.settings.data.Scope.APPLICATION;
import static org.exoplatform.portal.webui.util.Util.getPortalRequestContext;
import static org.exoplatform.services.log.ExoLogger.getLogger;
import static org.exoplatform.web.application.RequestContext.getCurrentInstance;
import static org.gatein.common.text.EntityEncoder.FULL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.exoplatform.calendar.model.Event;
import org.exoplatform.calendar.model.query.CalendarQuery;
import org.exoplatform.calendar.model.query.EventQuery;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.CalendarSetting;
import org.exoplatform.calendar.service.ExtendedCalendarService;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.template.Template;

@SessionScoped
public class CalendarPortletController {

	private Comparator<Event> eventsComparator = new Comparator<Event>() {
		public int compare(Event e1, Event e2) {
			Long d1 = e1.getToDateTime().getTime() - e1.getFromDateTime().getTime();
			Long d2 = e2.getToDateTime().getTime() - e2.getFromDateTime().getTime();

			if ((d1 > JOUR_MS) && (d2 > JOUR_MS))
				return -(round(e1.getFromDateTime().getTime() - e2.getToDateTime().getTime()));
			else if ((d1 > JOUR_MS) && (d2 < JOUR_MS))
				return 1;
			else if ((d1 < JOUR_MS) && (d2 > JOUR_MS))
				return -1;
			else if ((d1 < JOUR_MS) && (d2 == JOUR_MS))
				return -1;
			else if ((d1 == JOUR_MS) && (d2 < JOUR_MS))
				return 1;
			else if ((d1 > JOUR_MS) && (d2 == JOUR_MS))
				return 1;
			else if ((d1 == JOUR_MS) && (d2 > JOUR_MS))
				return -1;
			else if ((d1 == JOUR_MS) && (d2 == JOUR_MS))
				return 0;
			else if ((d1 < JOUR_MS) && (d2 < JOUR_MS)) {
				if (e1.getFromDateTime().getTime() == e2.getFromDateTime().getTime()) {
					if (d1 < d2)
						return 1;
					if (d1 > d2)
						return -1;
				}
				;
				return ((int) (e1.getFromDateTime().compareTo(e2.getFromDateTime())));

			}
			return 0;
		}
	};
	private Comparator<Event> tasksComparator = new Comparator<Event>() {
		public int compare(Event e1, Event e2) {
			if (((e2.getEventState().equals(NEEDS_ACTION)) && (e2.getToDateTime().compareTo(new Date()) < 0))
					&& ((e1.getEventState().equals(NEEDS_ACTION) && (e1.getToDateTime().compareTo(new Date()) >= 0))
							|| (!e1.getEventState().equals(NEEDS_ACTION)))) {
				return 1;
			} else if (((e1.getEventState().equals(NEEDS_ACTION)) && (e1.getToDateTime().compareTo(new Date()) < 0))
					&& ((e2.getEventState().equals(NEEDS_ACTION) && (e2.getToDateTime().compareTo(new Date()) >= 0))
							|| (!e2.getEventState().equals(NEEDS_ACTION)))) {
				return -1;
			} else if (((e1.getEventState().equals(NEEDS_ACTION)) && (e1.getToDateTime().compareTo(new Date()) < 0))
					&& (((e2.getEventState().equals(NEEDS_ACTION))
							&& (e2.getToDateTime().compareTo(new Date()) < 0)))) {
				return (int) (e2.getFromDateTime().getTime() - e1.getFromDateTime().getTime());
			} else
				return (int) (e2.getFromDateTime().getTime() - e1.getFromDateTime().getTime());
		}
	};
	List<org.exoplatform.calendar.service.Calendar> calendarDisplayedList = new ArrayList<org.exoplatform.calendar.service.Calendar>();
	List<org.exoplatform.calendar.service.Calendar> calendarNonDisplayedList = new ArrayList<org.exoplatform.calendar.service.Calendar>();
	List<Event> eventsDisplayedList = new ArrayList<Event>();
	List<org.exoplatform.calendar.service.Calendar> displayedCalendar = new ArrayList<org.exoplatform.calendar.service.Calendar>();
	Map<String, org.exoplatform.calendar.service.Calendar> displayedCalendarMap = new HashMap<String, org.exoplatform.calendar.service.Calendar>();
	List<Event> tasksDisplayedList = new ArrayList<Event>();
	List<org.exoplatform.calendar.service.Calendar> searchResult = new ArrayList<org.exoplatform.calendar.service.Calendar>();
	String[] nonDisplayedCalendarList = null;
	String nbclick = "0";

	@Inject
	CalendarService calendarService_;
	@Inject
	ExtendedCalendarService extendedCalendarService_;
	@Inject
	OrganizationService organization_;
	@Inject
	SettingService settingService_;

	private static final Log LOG = getLogger(CalendarPortletController.class);

	@Inject
	@Path("calendar.gtmpl")
	Template calendar;

	@Inject
	@Path("settings.gtmpl")
	Template setting;

	@Inject
	@Path("search.gtmpl")
	Template search;

	@Inject
	@Path("calendarPortletContainer.gtmpl")
	org.exoplatform.platform.portlet.juzu.calendar.templates.calendarPortletContainer container;

	@View
	public Response.Content index() {
		return container.ok();
	}

	// Format the Date pattern
	private String formatDate(Locale locale) {
		String datePattern = "";
		DateFormat dateFormat = getDateInstance(SHORT, locale);
		// convert to unique pattern
		datePattern = ((SimpleDateFormat) dateFormat).toPattern();
		if (!datePattern.contains("yy")) {
			datePattern = datePattern.replaceAll("y", "yy");
		}
		if (!datePattern.contains("yyyy")) {
			datePattern = datePattern.replaceAll("yy", "yyyy");
		}
		if (!datePattern.contains("dd")) {
			datePattern = datePattern.replaceAll("d", "dd");
		}
		if (!datePattern.contains("MM")) {
			datePattern = datePattern.replaceAll("M", "MM");
		}
		return datePattern;
	}

	@Ajax
	@Resource
	public Response.Content calendarHome() throws Exception {

		displayedCalendar.clear();
		displayedCalendarMap.clear();
		tasksDisplayedList.clear();
		eventsDisplayedList.clear();
		String date_act = null;
		String username = getCurrentUser();
		Locale locale = getPortalRequestContext().getLocale();
		String dp = formatDate(locale);
		DateFormat d = new SimpleDateFormat(dp);
		DateFormat dTimezone = getDateInstance(SHORT, locale);
		dTimezone.setCalendar(getCurrentCalendar());
		Long date = new Date().getTime();
		int clickNumber = parseInt(nbclick);
		if (clickNumber != 0)
			date = incDecJour(date, clickNumber);
		Date currentTime = new Date(date);
		// get current date base on calendar setting
		CalendarSetting calSetting = getCurrentUserCalendarSetting();
		String strTimeZone = calSetting.getTimeZone();
		dTimezone.setTimeZone(getTimeZone(strTimeZone));
		date_act = dTimezone.format(currentTime);
		// Get Calendar object set to the date and time of the given Date object
		Calendar cal = getCurrentCalendar();
		cal.setTime(currentTime);

		// Set time fields to zero
		cal.set(HOUR_OF_DAY, 0);
		cal.set(MINUTE, 0);
		cal.set(SECOND, 0);
		cal.set(MILLISECOND, 0);

		// Put it back in the Date object
		currentTime = cal.getTime();
		Date comp = currentTime;
		String defaultCalendarLabel = "Default";
		String dateLabel = "";
		try {
			ResourceBundle rs = getBundle("locale/portlet/calendar/calendar", locale);
			defaultCalendarLabel = FULL.encode(rs.getString("UICalendars.label.defaultCalendarId"));
			if (clickNumber == 0)
				dateLabel = rs.getString("today.label") + ": ";
			else if (clickNumber == -1)
				dateLabel = rs.getString("yesterday.label") + ": ";
			else if (clickNumber == 1)
				dateLabel = rs.getString("tomorrow.label") + ": ";
			else
				dateLabel = "";
		} catch (MissingResourceException ex) {
			if (clickNumber == 0)
				dateLabel = "today.label" + ": ";
			else if (clickNumber == -1)
				dateLabel = "yesterday.label" + ": ";
			else if (clickNumber == 1)
				dateLabel = "tomorrow.label" + ": ";
			else
				dateLabel = "";
		}

		FULL.encode(dateLabel);
		dateLabel = new StringBuffer(dateLabel).append(date_act).toString();
		if (nonDisplayedCalendarList == null) {
			SettingValue<?> settingNode = settingService_.get(USER, APPLICATION, HOME_PAGE_CALENDAR_SETTINGS);
			if ((settingNode != null) && (settingNode.getValue().toString().split(":").length == 2)) {
				nonDisplayedCalendarList = settingNode.getValue().toString().split(":")[1].split(",");
			}
		}
		ListAccess<Event> userEvents = getEvents(username, cal);
		if ((userEvents != null) && (userEvents.getSize() > 0)) {
			Event[] itr = userEvents.load(0, userEvents.getSize());
			for (Event event : itr) {
				Date from = d.parse(dTimezone.format(event.getFromDateTime()));
				Date to = d.parse(dTimezone.format(event.getToDateTime()));
				if ((event.getEventType().equals(TYPE_EVENT)) && (from.compareTo(d.parse(dTimezone.format(comp))) <= 0)
						&& (to.compareTo(d.parse(dTimezone.format(comp))) >= 0)) {
					if (!contains(nonDisplayedCalendarList, event.getCalendarId())) {
						org.exoplatform.calendar.model.Calendar calendar = extendedCalendarService_.getCalendarHandler()
								.getCalendarById(event.getCalendarId());
						if (calendar.getGroups() == null) {
							if (calendar.getId().equals(getDefaultCalendarId(username))
									&& calendar.getName().equals(defaultCalendarName)) {
								calendar.setName(defaultCalendarLabel);
							}
						}
						eventsDisplayedList.add(event);
						if (!displayedCalendarMap.containsKey(calendar.getId())) {
							org.exoplatform.calendar.service.Calendar builtCalendar = build(calendar);
							displayedCalendarMap.put(builtCalendar.getId(), builtCalendar);
							displayedCalendar.add(builtCalendar);
						}
					}
				} else if ((event.getEventType().equals(TYPE_TASK))
						&& (((from.compareTo(comp) <= 0) && (to.compareTo(comp) >= 0))
								|| ((event.getEventState().equals(NEEDS_ACTION)) && (to.compareTo(comp) < 0)))) {
					tasksDisplayedList.add(event);
				}
			}
			sort(eventsDisplayedList, eventsComparator);
			sort(tasksDisplayedList, tasksComparator);
		}
		return calendar.with().set("displayedCalendar", displayedCalendar)
				.set("calendarDisplayedMap", displayedCalendarMap).set("eventsDisplayedList", eventsDisplayedList)
				.set("tasksDisplayedList", tasksDisplayedList).set("date_act", dateLabel).ok().withCharset(UTF_8);
	}

	@Ajax
	@Resource
	public Response.Content setting() throws Exception {
		calendarDisplayedList.clear();
		calendarNonDisplayedList.clear();
		String username = getCurrentInstance().getRemoteUser();
		String defaultCalendarLabel = "Default";
		Iterator<org.exoplatform.calendar.service.Calendar> itr1 = getAllCal(username).iterator();
		while (itr1.hasNext()) {
			org.exoplatform.calendar.service.Calendar c = (org.exoplatform.calendar.service.Calendar) itr1.next();
			if (c.getGroups() == null) {
				if (c.getId().equals(getDefaultCalendarId(username)) && c.getName().equals(defaultCalendarName)) {
					c.setName(defaultCalendarLabel);
				}
			}
			if (contains(nonDisplayedCalendarList, c.getId())) {
				calendarNonDisplayedList.add(c);
			} else {
				calendarDisplayedList.add(c);
			}
		}
		return setting.with().set("displayedCalendar", calendarDisplayedList)
				.set("nonDisplayedCalendar", calendarNonDisplayedList).ok().withCharset(UTF_8);
	}

	@Ajax
	@Resource
	public Response.Content addCalendar(String calendarId) throws Exception {

		StringBuilder cals = new StringBuilder();
		int i = 0;
		nonDisplayedCalendarList = (String[]) removeElement(nonDisplayedCalendarList, calendarId);
		while (i < nonDisplayedCalendarList.length) {
			if (!nonDisplayedCalendarList[i].equals(calendarId))
				cals.append(nonDisplayedCalendarList[i]).append(",");
			i++;
		}
		settingService_.remove(USER, APPLICATION, HOME_PAGE_CALENDAR_SETTINGS);
		settingService_.set(USER, APPLICATION, HOME_PAGE_CALENDAR_SETTINGS,
				create("NonDisplayedCalendar:" + cals.toString()));
		return setting();
	}

	@Ajax
	@Resource
	public Response.Content deleteCalendar(String calendarId) throws Exception {

		nonDisplayedCalendarList = (String[]) add(nonDisplayedCalendarList, calendarId);
		StringBuffer cal = new StringBuffer();
		int i = 0;
		while (i < nonDisplayedCalendarList.length) {
			cal.append(nonDisplayedCalendarList[i]).append(",");
			i++;
		}
		settingService_.remove(USER, APPLICATION, HOME_PAGE_CALENDAR_SETTINGS);
		settingService_.set(USER, APPLICATION, HOME_PAGE_CALENDAR_SETTINGS,
				create("NonDisplayedCalendar:" + cal.toString()));
		return setting();
	}

	@Ajax
	@Resource
	public Response.Content incDate(String nbClick) throws Exception {
		int clickNumber = parseInt(nbclick);
		clickNumber++;
		nbclick = new Integer(clickNumber).toString();
		return calendarHome();
	}

	@Ajax
	@Resource
	public Response.Content decDate(String nbClick) throws Exception {
		int clickNumber = parseInt(nbclick);
		clickNumber--;
		nbclick = new Integer(clickNumber).toString();
		return calendarHome();
	}

	@Ajax
	@Resource
	public Response.Content getSearchResult(String key) {

		Iterator<org.exoplatform.calendar.service.Calendar> itr = null;
		if (calendarNonDisplayedList != null)
			itr = calendarNonDisplayedList.iterator();
		searchResult.clear();
		while (itr.hasNext()) {
			org.exoplatform.calendar.service.Calendar c = itr.next();
			if (c.getName().toLowerCase().contains(key.toLowerCase()))
				searchResult.add(c);
		}

		return search.with().set("searchResultList", searchResult).ok().withCharset(UTF_8);
	}

	public static Long incDecJour(Long date, int nbJour) {
		Calendar cal;
		cal = getInstance();
		cal.setTimeInMillis(date);
		cal.add(DAY_OF_MONTH, nbJour);
		return cal.getTime().getTime();
	}

	public String[] getUserGroups(String username) throws Exception {
		String[] groupsList;
		if (username == getCurrentInstance().getRemoteUser()) {
			Set<String> groups = ConversationState.getCurrent().getIdentity().getGroups();
			groupsList = groups.toArray(new String[groups.size()]);
		} else {
			Object[] objs = organization_.getGroupHandler().findGroupsOfUser(username).toArray();
			groupsList = new String[objs.length];
			for (int i = 0; i < objs.length; i++) {
				groupsList[i] = ((Group) objs[i]).getId();
			}
		}
		return groupsList;
	}

	public List<org.exoplatform.calendar.service.Calendar> getAllCal(String username) throws Exception {
		CalendarQuery calendarQuery = new CalendarQuery();
		Collection<MembershipEntry> groups = asList(getUserGroups(username)).stream().map(e -> new MembershipEntry(e))
				.collect(toList());
		Identity identity = new Identity(username, groups);
		calendarQuery.setIdentity(identity);
		List<org.exoplatform.calendar.service.Calendar> calList = new ArrayList<org.exoplatform.calendar.service.Calendar>();
		List<org.exoplatform.calendar.model.Calendar> lgcd = extendedCalendarService_.getCalendarHandler()
				.findCalendars(calendarQuery);
		List<String> calIds = new ArrayList<String>();
		for (org.exoplatform.calendar.model.Calendar c : lgcd) {

			if (!calIds.contains(c.getId())) {
				calIds.add(c.getId());
				calList.add(build(c));
			}
		}
		return calList;
	}

	String[] getCalendarsIdList(String username) {

		StringBuilder sb = new StringBuilder();
		List<org.exoplatform.calendar.model.Calendar> listCalendar = null;
		try {
			CalendarQuery calendarQuery = new CalendarQuery();
			Collection<MembershipEntry> groups = asList(getUserGroups(username)).stream()
					.map(e -> new MembershipEntry(e)).collect(toList());
			Identity identity = new Identity(username, groups);
			calendarQuery.setIdentity(identity);
			listCalendar = extendedCalendarService_.getCalendarHandler().findCalendars(calendarQuery);
		} catch (Exception e) {
			LOG.error("Error while checking User Calendar :" + e.getMessage(), e);
		}
		for (org.exoplatform.calendar.model.Calendar g : listCalendar) {
			sb.append(g.getId()).append(",");
		}
		String[] list = sb.toString().split(",");
		return list;
	}

	ListAccess<Event> getEvents(String username, Calendar cal) {

		String[] calList = getCalendarsIdList(username);
		Calendar begin = getBeginDay(cal);
		Calendar end = getEndDay(cal);
		end.add(MILLISECOND, -1);

		EventQuery eventQuery = new EventQuery();
		eventQuery.setFromDate(begin.getTimeInMillis());
		eventQuery.setToDate(end.getTimeInMillis());
		eventQuery.setOrderBy(new String[] { EXO_FROM_DATE_TIME });

		eventQuery.setCalendarIds(calList);
		ListAccess<Event> userEvents = null;
		try {
			userEvents = extendedCalendarService_.getEventHandler().findEventsByQuery(eventQuery);

		} catch (Exception e) {
			LOG.error("Error while checking User Events:" + e.getMessage(), e);
		}
		return userEvents;
	}

	private String getDateDelimiter(String date) {
		String[] availableDelimiter = { "/", "-", "." };
		for (String delim : availableDelimiter) {
			if (date.indexOf(delim) > 0) {
				return delim;
			}
		}
		return null;
	}
}
