package ca.saultstemarie.dsjquery;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import com.xerox.docushare.DSClass;
import com.xerox.docushare.DSContentElement;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSFactory;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSLoginPrincipal;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSObjectIterator;
import com.xerox.docushare.DSResultIterator;
import com.xerox.docushare.DSServer;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.FileContentElement;
import com.xerox.docushare.db.DatabaseException;
import com.xerox.docushare.object.DSCollection;
import com.xerox.docushare.object.DSDocument;
import com.xerox.docushare.object.DSRendition;
import com.xerox.docushare.object.DSVersion;
import com.xerox.docushare.property.DSLinkDesc;
import com.xerox.docushare.property.DSProperties;
import com.xerox.docushare.query.DSCollectionScope;
import com.xerox.docushare.query.DSQuery;

import ca.saultstemarie.dsjquery.DSJQueryException.DSJQuerySelectorException;

/**
 * <b>DSJQuery - DocuShare jQuery</b>
 * A DocuShare object query library similar to jQuery.
 * 
 * @author d.gowans
 * 
 * @see <a href="https://github.com/cityssm/dsJQuery">dsJQuery on GitHub</a>
 */
public class DSJQuery implements Iterable<DSObject> {
	
	
	/*
	 * STATIC SESSION DETAILS
	 */
	
	
	/**
	 * {@value #DEFAULT_SERVER_PORT}, the most commonly used port for DocuShare connections.
	 */
	public final static int DEFAULT_SERVER_PORT = 1099;
	
	
	/**
	 * {@value #DEFAULT_SESSION_DOMAIN}, the domain used for users managed by the DocuShare server.
	 * 
	 */
	public final static String DEFAULT_SESSION_DOMAIN = "DocuShare";
	
	
	private static Deque<DSSession> SESSION_STACK = null;
	private static Semaphore        SESSION_STACK_AVAILABLE = null;
	private static int              SESSION_STACK_SIZE = 2;
	
	
	private static String SERVER_NAME = null;
	private static int    SERVER_PORT = DEFAULT_SERVER_PORT;
	
	
	private static String SESSION_DOMAIN   = DEFAULT_SESSION_DOMAIN;
	private static String SESSION_USERNAME = null;
	private static String SESSION_PASSWORD = null;
	
	
	/**
	 * Initializes DSJQuery with DocuShare server details.
	 * Uses the default DocuShare port number.
	 * @category SETUP
	 * 
	 * @param serverName - DocuShare server name
	 * 
	 * @throws DSJQueryException
	 */
	public static void serverSetup (String serverName) throws DSJQueryException {
		serverSetup(serverName, DEFAULT_SERVER_PORT);
	}
	
	
	/**
	 * Initializes DSJQuery with complete DocuShare server details.
	 * @category SETUP
	 * 
	 * @param serverName - DocuShare server name
	 * @param serverPort - DocuShare server port
	 * 
	 * @throws DSJQueryException
	 */
	public static void serverSetup (String serverName, int serverPort) throws DSJQueryException {

		if (hasSessionsInUse()) {
			throw new DSJQueryException("DSJQuery currently in use.");
		}
		
		closeOpenSessions();
		
		SERVER_NAME = serverName;
		SERVER_PORT = serverPort;
		
	}
	
	
	/**
	 * Tests if the DocuShare server details have been initialized.
	 * @category SETUP
	 *  
	 * @return TRUE if the server name has been set.
	 */
	public static boolean isServerSetup() {
		return (SERVER_NAME != null);
	}
	
		
	/**
	 * Initializes DSJQuery with session login details.
	 * Uses the default {@value #DEFAULT_SESSION_DOMAIN} domain.
	 * @category SETUP
	 * 
	 * @param userName - DocuShare user name
	 * @param password - DocuShare password
	 * 
	 * @throws DSJQueryException
	 */
	public static void sessionSetup (String userName, String password) throws DSJQueryException {
		sessionSetup (DEFAULT_SESSION_DOMAIN, userName, password);
	}
	
	
	/**
	 * Initializes DSJQuery with complete session login details.
	 * Note that this user must have read permissions for all objects it queries.
	 * This user will also be associated with any modifications it makes through DSJQuery.
	 * @category SETUP
	 * 
	 * @param userDomain - The user domain name
	 * @param userName   - The user name
	 * @param password   - The password
	 * 
	 * @throws DSJQueryException
	 */
	public static void sessionSetup (String userDomain, String userName, String password) throws DSJQueryException {

		if (hasSessionsInUse()) {
			throw new DSJQueryException("DSJQuery currently in use.");
		}
		
		closeOpenSessions();
		
		SESSION_DOMAIN   = userDomain;
		SESSION_USERNAME = userName;
		SESSION_PASSWORD = password;
	}
	
	
	/**
	 * Tests if the DocuShare session login details have been initialized.
	 * @category SETUP
	 * 
	 * @return TRUE if the session login details have been set.
	 */
	public static boolean isSessionSetup() {
		return (SESSION_USERNAME != null && SESSION_PASSWORD != null);
	}
	
	
	/**
	 * Checks if there are any outstanding DSSession objects in use by DSJQuery.
	 * If outstanding sessions exist, server and session details cannot be changed.
	 * @category SETUP
	 * 
	 * @return TRUE if there are any DQJQuery actions holding onto and DSSession objects.
	 */
	private static boolean hasSessionsInUse() {
		
		if (SESSION_STACK_AVAILABLE != null &&
				SESSION_STACK_AVAILABLE.availablePermits() < SESSION_STACK_SIZE) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * Gets a DSSession object from a pool of available objects.
	 * 
	 * @return A connected DSSession object.
	 * 
	 * @throws InterruptedException
	 * @throws DSException
	 */
	protected static synchronized DSSession getSession() throws InterruptedException, DSException {
		
		if (SESSION_STACK == null) {
			SESSION_STACK = new LinkedBlockingDeque<>(SESSION_STACK_SIZE);
			SESSION_STACK_AVAILABLE = new Semaphore(SESSION_STACK_SIZE, true);
		}
		
		SESSION_STACK_AVAILABLE.acquire();
		
		if (SESSION_STACK.isEmpty()) {
			DSServer dsServer = DSFactory.createServer(SERVER_NAME, SERVER_PORT);
			DSSession dsSession = dsServer.createSession(SESSION_DOMAIN, SESSION_USERNAME, SESSION_PASSWORD);
			return dsSession;
			
		} else {
			DSSession dsSession = SESSION_STACK.pop();
			
			if (dsSession.isClosed()) {
				DSServer dsServer = DSFactory.createServer(SERVER_NAME, SERVER_PORT);
				dsSession = dsServer.createSession(SESSION_DOMAIN, SESSION_USERNAME, SESSION_PASSWORD);
			}
			
			return dsSession;
		}
	}
	
	
	/**
	 * Returns a DSSession object to the pool for other threads to use.
	 * 
	 * @param dsSession - A DSSession object that will no longer be used by the thread returning it.
	 */
	protected static synchronized void returnSession(DSSession dsSession) {
		if (dsSession != null) {
			SESSION_STACK.push(dsSession);
		}
		SESSION_STACK_AVAILABLE.release();
	}

	
	/**
	 * Closes all DSSession objects currently queued.
	 * This method should be called when DSJQuery is done being used, or won't be used for a while.
	 */
	public static synchronized void closeOpenSessions() {
		
		if (SESSION_STACK == null) {
			return;
		}
		
		while (!SESSION_STACK.isEmpty()) {
			try {
				DSSession dsSession = SESSION_STACK.pop();
				DSServer dsServer = dsSession.getServer();
				
				dsSession.close();
				dsServer.close();
			}
			catch (Exception e) {
				// ignore
			}
		}
	}
	
	
	private List<DSObject> dsObjects = null;
	
	
	/**
	 * Creates a new DSJQuery object at the root of the DocuShare library.
	 * It contains no DSObjects, but in it's state, any DSObject in the library can be selected.
	 * @category CORE
	 * 
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/jQuery/">jQuery() | jQuery API</a>
	 */
	public DSJQuery() throws DSJQueryException {
		if (!isServerSetup()) {
			throw new DSJQueryException("DocuShare server settings missing. Set using DSJQuery.serverSetup();");
		}
		else if (!isSessionSetup()) {
			throw new DSJQueryException("DocuShare session settings missing. Set using DSJQuery.sessionSetup();");
		}
	}
	
	
	/**
	 * Creates a new DSJQuery object with DSObjects that match the given filter.
	 * @category CORE
	 * 
	 * @param findSelector - A find selector
	 * 
	 * @throws DSException 
	 * @throws DSJQueryException 
	 * @throws InterruptedException 
	 */
	public DSJQuery(String findSelector) throws DSException, DSJQueryException, InterruptedException {
		this();
		dsObjects = find(findSelector).dsObjects;
	}
	
	
	private DSJQuery(List<DSObject> dsObjects) throws DSJQueryException {
		this();
		this.dsObjects = dsObjects;
	}
	
	
	private DSJQuery(DSObject dsObject) throws DSJQueryException {
		this();
		dsObjects = new ArrayList<>(1);
		dsObjects.add(dsObject);
	}
	
	
	/**
	 * Searches beneath all currently selected Collections
	 * for all Documents and Collections.
	 * Equivalent to {@code find("*")}
	 * @category TRAVERSING
	 *  
	 * @return A new DSJQuery object
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 * @throws InterruptedException
	 */
	public DSJQuery find_all() throws DSException, DSJQueryException, InterruptedException {
		
		DSSession dsSession = null;
		
		try {
			dsSession = getSession();
		
			List<DSObject> newDsObjects = null;
			
			if (dsObjects == null) {
				
				DSQuery query = new DSQuery();
				DSResultIterator result = dsSession.search(query).iterator();
				
				newDsObjects = new ArrayList<>(result.size());

				while (result.hasNext()) {
					DSObject item = result.nextObject().getObject();
					newDsObjects.add(item);
				}
			}
			else {
				
				newDsObjects = new LinkedList<>();
				
				for (DSObject parentObj : dsObjects) {
					
					if (parentObj instanceof DSCollection) {
	
						DSQuery query = new DSQuery();
						query.addCollectionScope( new DSCollectionScope( new DSHandle[]{parentObj.getHandle()}) );
	
						DSResultIterator result = dsSession.search(query).iterator();
						
						while (result.hasNext()) {
							DSObject item = result.nextObject().getObject();
							newDsObjects.add(item);
						}
					}
				}
			}
			
			return new DSJQuery(newDsObjects);
		}
		finally {
			returnSession(dsSession);
		}
	}

	
	/**
	 * Searches beneath all currently selected Collections
	 * for objects with the given handle.
	 * @category TRAVERSING
	 * 
	 * @param handle - i.e. "Document-111"
	 * @return A new DSJQuery object 
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 * @throws InterruptedException
	 */
	public DSJQuery find_byHandle (String handle) throws DSException, DSJQueryException, InterruptedException {
		
		DSSession dsSession = null;
		
		try {
			
			dsSession = getSession();
		
			List<DSObject> newDsObjects = new ArrayList<>(1);
			
			if (dsObjects == null) {
				
				try {
					DSObject obj = dsSession.getObject(new DSHandle(handle));
					newDsObjects.add(obj);
				}
				catch (Exception e) {
					// ignore
				}
			}
			else {
				for (DSObject parentObj : dsObjects) {
					
					if (parentObj instanceof DSCollection) {
	
						DSQuery query = new DSQuery( DSQuery.matches("handle", handle) );
						query.addCollectionScope( new DSCollectionScope( new DSHandle[]{parentObj.getHandle()}) );
						
						DSResultIterator result = dsSession.search(query).iterator();
						
						if (result.hasNext()) {
							DSObject item = result.nextObject().getObject();
							newDsObjects.add(item);
							break;
						}
					}
					
					// Handles are unique.
					if (newDsObjects.size() > 0) {
						break;
					}
				}
			}
			
			return new DSJQuery(newDsObjects);
		}
		finally {
			returnSession(dsSession);
		}
	}
	
	
	/**
	 * Searches beneath all currently selection Collections
	 * for objects with a given object class.
	 * @category TRAVERSING
	 * 
	 * @param className - i.e. "Document"
	 * @return A new DSJQuery object
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 * @throws InterruptedException
	 */
	public DSJQuery find_byObjectClass (String className) throws DSException, DSJQueryException, InterruptedException {
		
		DSSession dsSession = null;
		
		try {
			
			dsSession = getSession();
		
			List<DSObject> newDsObjects = null;
			
			if (dsObjects == null) {
				
				DSQuery query = new DSQuery();
				query.addClassScope(className);
				
				DSResultIterator result = dsSession.search(query).iterator();
				
				newDsObjects = new ArrayList<>(result.size());
				
				while (result.hasNext()) {
					DSObject item = result.nextObject().getObject();
					newDsObjects.add(item);
				}
			}
			else {
				
				newDsObjects = new LinkedList<>();
				
				for (DSObject parentObj : dsObjects) {
					
					if (parentObj instanceof DSCollection) {
	
						DSQuery query = new DSQuery();
						query.addCollectionScope( new DSCollectionScope( new DSHandle[]{parentObj.getHandle()}) );
						query.addClassScope(className);
						
						DSResultIterator result = dsSession.search(query).iterator();
						
						while (result.hasNext()) {
							DSObject item = result.nextObject().getObject();
							newDsObjects.add(item);
						}
					}
				}
			}
			
			return new DSJQuery(newDsObjects);
		}
		finally {
			returnSession(dsSession);
		}
	}
	
	
	/**
	 * Searches beneath all currently selected Collections for Documents and Collections
	 * that match the given selector.
	 * @category TRAVERSING
	 * 
	 * @param findSelector - i.e. "*" or ".Document" or "#Document-111"
	 * @return A new DSJquery object
	 * 
	 * @throws DSException 
	 * @throws DSJQueryException 
	 * @throws InterruptedException 
	 * 
	 * @see <a href="https://api.jquery.com/find/">find() | jQuery API</a>
	 */
	public DSJQuery find (String findSelector) throws DSException, DSJQueryException, InterruptedException {

		/*
		 * If selectorToken is *, retrieve all child elements
		 */
		if (findSelector.equals("*")) {
			return find_all();
		}
		
		
		/*
		 * If selectorToken starts with "#", retrieve by Handle.
		 */
		else if (findSelector.startsWith("#")) {
			
			String handle = findSelector.substring(1);
			return find_byHandle(handle);
		}
		
		/*
		 * If selectorToken starts with a ".", search by class name.
		 */
		else if (findSelector.startsWith(".")) {
			
			String className = findSelector.substring(1);
			return find_byObjectClass(className);	
		}
		
		throw new DSJQuerySelectorException(findSelector);
	}
	
	
	/**
	 * Retrieves the immediate descendants for all selected collections. 
	 * @category TRAVERSING
	 * 
	 * @return A new DSJQuery object
	 * 
	 * @throws DSException
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/children/">children() | jQuery API</a>
	 */
	public DSJQuery children () throws DSException, DSJQueryException {
		
		if (dsObjects == null) {
			return new DSJQuery(new ArrayList<>(0));			
		}
		
		List<DSObject> newDsObjects = new LinkedList<>();
			
		for (DSObject parentObj : dsObjects) {
			
			if (parentObj instanceof DSCollection) {
				
				DSObjectIterator iterator = ((DSCollection) parentObj).children(null);

				while (iterator.hasNext()) {
					DSObject item = iterator.nextObject();
					newDsObjects.add(item);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}

	
	/**
	 * Retrieves the immediate descendants for all selected collections which satisfy a given filter. 
	 * @category TRAVERSING
	 * 
	 * @param filterSelector - i.e. ".Document", "[content_type^='image/']"
	 * @return A new DSJQuery object
	 * 
	 * @throws DSException
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/children/">children() | jQuery API</a>
	 */
	public DSJQuery children (String filterSelector) throws DSException, DSJQueryException {
		return children().filter(filterSelector);
	}
	
		
	public DSJQuery filter_byAttribute_startsWith (String attributeName, String attributeValue, boolean ignoreCase) throws DSException, DSJQueryException {
				
		List<DSObject> newDsObjects = new LinkedList<>();	
		
		String attributeValueForCompare = (ignoreCase ? attributeValue.toLowerCase() : attributeValue);

		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(attributeName);
			
			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.startsWith(attributeValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	public DSJQuery filter_byAttribute_endsWith (String attributeName, String attributeValue, boolean ignoreCase) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
		String attributeValueForCompare = (ignoreCase ? attributeValue.toLowerCase() : attributeValue);
		
		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(attributeName);

			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.endsWith(attributeValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	public DSJQuery filter_byAttribute_contains (String attributeName, String attributeValue, boolean ignoreCase) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
		String attributeValueForCompare = (ignoreCase ? attributeValue.toLowerCase() : attributeValue);
		
		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(attributeName);

			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.contains(attributeValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	public DSJQuery filter_byAttribute_equals (String attributeName, String attributeValue, boolean ignoreCase) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
		String attributeValueForCompare = (ignoreCase ? attributeValue.toLowerCase() : attributeValue);
		
		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(attributeName);
			
			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.equals(attributeValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	public DSJQuery filter_byObjectClass (String className) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new ArrayList<>(dsObjects);
		List<DSObject> newDsObjectsCopy = new LinkedList<>(dsObjects);
		
		for (DSObject obj : newDsObjects) {
			
			String objClassName = obj.getDSClass().getName();
			
			if (!objClassName.equals(className)) {
				newDsObjectsCopy.remove(obj);
			}
		}
		
		return new DSJQuery(newDsObjectsCopy);
	}
	
	
	/**
	 * Filters the current set of documents and collections using a selector.
	 * @category FILTERING
	 * 
	 * @param filterSelector - i.e. ".Document", "[content_type^='image/']"
	 * @return A new DSJquery object
	 * 
	 * @throws DSException
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/filter/">filter() | jQuery API</a>
	 */
	public DSJQuery filter (String filterSelector) throws DSException, DSJQueryException {
		
		/*
		 * For a filter to work, we must have some objects.
		 * If none, quit now!
		 */
		
		if (dsObjects == null)
			return new DSJQuery();
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(new ArrayList<>(0));
		}
			
		
		if (filterSelector.startsWith(".")) {
			
			String filterClassName = filterSelector.substring(1);
			return filter_byObjectClass(filterClassName);
		}
		else if (filterSelector.startsWith("[")) {
			
			// starts with
			if (filterSelector.contains("^=")) {
				
				String attributeName = filterSelector.substring(1, filterSelector.indexOf("^="));
				String attributeValue = filterSelector.substring(filterSelector.indexOf("^=") + 3, filterSelector.length() - 2);
				
				return filter_byAttribute_startsWith(attributeName, attributeValue, false);
			}
			
			// ends with
			else if (filterSelector.contains("$=")) {
					
				String attributeName = filterSelector.substring(1, filterSelector.indexOf("$="));
				String attributeValue = filterSelector.substring(filterSelector.indexOf("$=") + 3, filterSelector.length() - 2);
				
				return filter_byAttribute_endsWith(attributeName, attributeValue, false);
			}
			
			// contains
			else if (filterSelector.contains("~=")) {
					
				String attributeName = filterSelector.substring(1, filterSelector.indexOf("~="));
				String attributeValue = filterSelector.substring(filterSelector.indexOf("~=") + 3, filterSelector.length() - 2);
				
				return filter_byAttribute_contains(attributeName, attributeValue, false);
			}
			else if (filterSelector.contains("=")) {
				
				String attributeName = filterSelector.substring(1, filterSelector.indexOf("="));
				String attributeValue = filterSelector.substring(filterSelector.indexOf("=") + 2, filterSelector.length() - 2);
				
				return filter_byAttribute_equals(attributeName, attributeValue, false);
			}
			else {
				throw new DSJQuerySelectorException(filterSelector);
			}
		}
		else {
			throw new DSJQuerySelectorException(filterSelector);
		}
	}

	
	/**
	 * Filters the current set of documents and collections to only include the first one.
	 * @category FILTERING
	 * 
	 * @return A new DSJquery object
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/first/">first() | jQuery API</a>
	 */
	public DSJQuery first () throws DSJQueryException {
		
		if (dsObjects.size() > 0) {
			return new DSJQuery(dsObjects.get(0));
		}
		return new DSJQuery(new ArrayList<>(0));
	}
	
	
	/**
	 * Sorts the current set of objects using a Comparator.
	 * @category SORTING
	 * 
	 * @param comparator - The sorting function
	 * @return A new, sorted DSJQuery object
	 * 
	 * @throws DSJQueryException 
	 */
	public DSJQuery sort (Comparator<DSObject> comparator) throws DSJQueryException {
		
		if (dsObjects == null)
			return new DSJQuery();
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(new ArrayList<>(0));
		}
			
		List<DSObject> newDsObjects = new ArrayList<>(dsObjects);

		newDsObjects.sort(comparator);
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Sorts the current set of objects in ascending order by an attribute.
	 * @category SORTING
	 * 
	 * @param attributeName - The attribute to sort by.
	 * @return A new, sorted DSJQuery object
	 * 
	 * @throws DSException
	 * @throws DSJQueryException 
	 */
	public DSJQuery sortAsc_byAttribute (String attributeName) throws DSException, DSJQueryException {
		
		Comparator<DSObject> attributeComparator = new Comparator<DSObject>() {

			@SuppressWarnings("unchecked")
			@Override
			public int compare(DSObject o1, DSObject o2) {

				Object attr1 = null;
				try {
					attr1 = o1.get(attributeName);
				}
				catch (Exception e) {}
				
				Object attr2 = null;
				try {
					attr2 = o2.get(attributeName);
				}
				catch (Exception e) {}
				
				if (attr1 == null && attr2 == null) {
					return 0;
				}
				else if (attr1 == null) {
					return -1;
				}
				else if (attr2 == null) {
					return 1;
				}
				else if (attr1 instanceof Comparable<?> && attr2 instanceof Comparable<?> && attr1.getClass().equals(attr2.getClass())) {
					Comparable<Object> obj1 = (Comparable<Object>)attr1;
					Comparable<Object> obj2 = (Comparable<Object>)attr2;
							
					return obj1.compareTo(obj2);
				}
				else {
					String string1 = attr1.toString();
					String string2 = attr2.toString();
					
					return string1.compareTo(string2);
				}
			}
		};
		
		return sort(attributeComparator);
	}
	
	
	/**
	 * Reverses the order of the current set of objects.
	 * Can be used to reorder a list in descending order after calling sortAsc.
	 * @category SORTING
	 * 
	 * @return A new DSJQuery object
	 * 
	 * @throws DSJQueryException 
	 */
	public DSJQuery reverse() throws DSJQueryException {
		
		if (dsObjects == null)
			return new DSJQuery();
		
		List<DSObject> newObjectList = new ArrayList<>(dsObjects);
		
		Collections.reverse(newObjectList);
		
		return new DSJQuery(newObjectList);
	}
	
	
	/**
	 * Get the value of an attribute for the first element in the current set.
	 * @category ATTRIBUTES
	 * 
	 * @param attributeName
	 * @return The selected attribute value
	 * 
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/attr/">attr() | jQuery API</a>
	 */
	public Object attr (String attributeName) throws DSException {
		
		if (dsObjects == null)
			return null;
		
		if (dsObjects.size() == 0) {
			return null;
		}
		
		Object value = dsObjects.get(0).get(attributeName);
		return value;
	}
	
	
	/**
	 * Sets an attribute across all current elements.
	 * @category ATTRIBUTES
	 * 
	 * @param attributeName
	 * @param value
	 * @return The current DSJQuery object
	 * 
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/attr/">attr() | jQuery API</a>
	 */
	public DSJQuery attr (String attributeName, Object value) throws DSException {
		
		if (dsObjects == null)
			return this;
		
		for (DSObject obj : dsObjects) {
			obj.set(attributeName, value);
			obj.save();
		}
		
		return this;
	}
	
	
	/**
	 * Adds a new keyword to the comma-separated keyword list.
	 * The new keyword is only added if the keyword is not already part of the list.
	 * 
	 * @category ATTRIBUTES
	 * 
	 * @param keywordToAdd
	 * @return The current DSJQuery object
	 * 
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/addClass/">addClass() | jQuery API</a>
	 */
	public DSJQuery addKeyword (String keywordToAdd) throws DSException {
		
		if (dsObjects == null)
			return this;
		
		for (DSObject obj : dsObjects) {
			
			boolean keywordFound = false;
			
			String currentKeywordsString = obj.getKeywords().trim();
			String[] currentKeywords = currentKeywordsString.split(",");
			
			for (String keyword : currentKeywords) {
				if (keyword.trim().equals(keywordToAdd)) {
					keywordFound = true;
					break;
				}
			}
			
			if (!keywordFound) {
				obj.setKeywords(currentKeywordsString + 
						(currentKeywordsString.equals("") ? "" : ", ") +
						keywordToAdd);
				obj.save();
			}
		}
		
		return this;
	}
	
	
	/**
	 * Removes the first instance of a keyword from the keyword list if it is found. 
	 * @category ATTRIBUTES
	 * 
	 * @param keywordToRemove
	 * @return The current DSJQuery object
	 * 
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/removeClass/">removeClass() | jQuery API</a>
	 */
	public DSJQuery removeKeyword (String keywordToRemove) throws DSException {
		
		if (dsObjects == null)
			return this;
		
		for (DSObject obj : dsObjects) {

			String[] currentKeywords = obj.getKeywords().split(",");
			
			for (String keyword : currentKeywords) {
				if (keyword.trim().equals(keywordToRemove)) {
					
					List<String> newKeywordsList = new LinkedList<>(Arrays.asList(currentKeywords));
					newKeywordsList.remove(keyword);
					
					obj.setKeywords(String.join(", ", newKeywordsList));
					obj.save();
					
					break;
				}
			}
		}
		
		return this;
	}
	
	
	/**
	 * Creates a new DSJQuery object with the same set of matching objects.
	 * 
	 * @see <a href="https://api.jquery.com/clone/">clone() | jQuery API</a>
	 */
	public DSJQuery clone() {
		
		try {
			if (dsObjects == null) {
				return new DSJQuery();
			}
			else {
				return new DSJQuery(new ArrayList<>(dsObjects));
			}
		}
		catch (Exception e) {
			// Exceptions are caught to maintain compatibility with Object.clone();
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Links all child elements under the current set of collections.
	 * @category INSERTING
	 * 
	 * @param newChildren
	 * @return The current DSJQuery object
	 * 
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/append/">append() | jQuery API</a>
	 */
	public DSJQuery append (DSJQuery newChildren) throws DSException {
		
		if (dsObjects == null) {
			return this;
		}
		
		if (newChildren.length() == 0) {
			return this;
		}
		
		for (DSObject potentialParent : dsObjects) {
			
			if (potentialParent instanceof DSCollection) {
				
				DSCollection parentCollection = (DSCollection)potentialParent;

				for (DSObject newChild : newChildren.dsObjects) {
					try {
						parentCollection.addChild(newChild);
					}
					catch (DatabaseException e) {
						// ignore
					}
				}
				
				parentCollection.save();
			}
		}
		
		return this;
	}

	
	/**
	 * Uploads a new Document to DocuShare under each Collection in the set.
	 * @category INSERTING
	 * 
	 * @param file - The file to upload to DocuShare.
	 * @return A new DSJQuery object containing all of the created Documents.
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 * @throws InterruptedException 
	 */
	public DSJQuery insertAndGet (File file) throws DSException, DSJQueryException, InterruptedException {
		
		if (dsObjects == null) {
			return new DSJQuery(new ArrayList<>(0));
		}
		
		if (!file.exists()) {
			throw new DSJQueryException("File does not exist: " + file.getAbsolutePath());
		}
		else if (file.isDirectory()) {
			throw new DSJQueryException("File is a directory: " + file.getAbsolutePath());
		}
		
		DSSession dsSession = null;
		
		try {
			
			dsSession = getSession();
		
			List<DSObject> newDsObjects = new ArrayList<>(1);
	
			for (DSObject potentialParent : dsObjects) {
				
				if (potentialParent instanceof DSCollection) {
					
					DSCollection parentCollection = (DSCollection)potentialParent;
					
					String title = file.getName();
					
					// Document Prototype
					DSClass docClass = dsSession.getDSClass(DSDocument.classname);
					DSProperties docProto = docClass.createPrototype();
					docProto.setPropValue(DSObject.title, title);
					
					// Version Prototype
					DSClass versionClass = dsSession.getDSClass(DSVersion.classname);
					DSProperties versionProto = versionClass.createPrototype();
					versionProto.setPropValue(DSObject.title, title);
					versionProto.setPropValue(DSVersion.revision_comments, "(Initial version)");
					
					// Rendition Prototype
					DSClass renditionClass = dsSession.getDSClass(DSRendition.classname);
					DSProperties renditionProto = renditionClass.createPrototype();
					renditionProto.setPropValue(DSRendition.title, title);
					
					FileContentElement ce = new FileContentElement(file.getAbsolutePath(), false);
					
					DSHandle newDocHandle = dsSession.createDocument(
							docProto, 
							versionProto,
							renditionProto,
							new DSContentElement[] {ce},
							null,
							DSLinkDesc.containment,
							parentCollection,
							(DSLoginPrincipal)dsSession.getObject(dsSession.getLoginPrincipalHandle()),
							null);
					
					newDsObjects.add(dsSession.getObject(newDocHandle));
				}
			}
			
			return new DSJQuery(newDsObjects);
		}
		finally {
			returnSession(dsSession);
		}
	}

	
	/**
	 * Creates a new Collection under each Collection in the set.
	 * @category INSERTING
	 * 
	 * @param collectionName - The name of the new Collection.
	 * @return A new DSJQuery object containing all of the created Collections.
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 * @throws InterruptedException
	 */
	public DSJQuery insertCollectionAndGet (String collectionName) throws DSException, DSJQueryException, InterruptedException {
		
		if (dsObjects == null) {
			return new DSJQuery(new LinkedList<>());
		}
		
		DSSession dsSession = null;
		
		try {
			dsSession = getSession();
		
			List<DSObject> newDsObjects = new ArrayList<>(1);
	
			for (DSObject potentialParent : dsObjects) {
				
				if (potentialParent instanceof DSCollection) {
					
					DSCollection parentCollection = (DSCollection)potentialParent;
	
					// Document Prototype
					DSClass colClass = dsSession.getDSClass(DSCollection.classname);
					DSProperties colProto = colClass.createPrototype();
					colProto.setPropValue(DSObject.title, collectionName);
	
					DSHandle newDocHandle = dsSession.createObject(
							colProto, 
							DSLinkDesc.containment,
							parentCollection,
							(DSLoginPrincipal)dsSession.getObject(dsSession.getLoginPrincipalHandle()),
							null);
					
					newDsObjects.add(dsSession.getObject(newDocHandle));
				}
			}
			
			return new DSJQuery(newDsObjects);
		}
		finally {
			returnSession(dsSession);
		}
	}
	
	
	/**
	 * Returns the total number of DSObjects in the DSJQuery object.
	 * 
	 * @return
	 * 
	 * @see <a href="https://api.jquery.com/length/">length | jQuery API</a>
	 */
	public int length() {
		if (dsObjects == null) {
			return 0;
		}
		
		return dsObjects.size();
	}
	
	
	/**
	 * Similar to $().toArray(), returns the List of DSObjects.
	 * 
	 * @return
	 * 
	 * @see <a href="https://api.jquery.com/toArray/">toArray() | jQuery API</a>
	 */
	public List<DSObject> toList() {
		return dsObjects;
	}
		
	
	/**
	 * For debug purposes, outputs the handles and titles of objects currently the DSJQuery object.
	 * 
	 * @return The current DSJQuery object
	 * 
	 * @throws DSException
	 */
	public DSJQuery print() throws DSException {
		
		for (DSObject obj : dsObjects) {
			System.out.println (obj.getHandle().toString() + " - " + obj.getTitle());
		}
		System.out.println();
		
		return this;
	}

	
	public Iterator<DSObject> iterator() {
		if (dsObjects == null) {
			return new ArrayList<DSObject>(0).iterator();
		}
		
		return dsObjects.iterator();
	}
}
