package ca.saultstemarie.dsjquery;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.xerox.docushare.DSClass;
import com.xerox.docushare.DSContentElement;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSLoginPrincipal;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSObjectIterator;
import com.xerox.docushare.DSResultIterator;
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
		if (!DSJQuerySessionHandler.isServerSetup()) {
			throw new DSJQueryException("DocuShare server settings missing. Set using DSJQuerySessionHandler.serverSetup();");
		}
		else if (!DSJQuerySessionHandler.isSessionSetup()) {
			throw new DSJQueryException("DocuShare session settings missing. Set using DSJQuerySessionHandler.sessionSetup();");
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
	
	
	/**
	 * Creates a new DSJQuery object from a list of DSObjects.
	 * @category CORE
	 * 
	 * @param dsObjects
	 * 
	 * @throws DSJQueryException
	 */
	public DSJQuery(List<DSObject> dsObjects) throws DSJQueryException {
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
			dsSession = DSJQuerySessionHandler.getSession();
		
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
			DSJQuerySessionHandler.returnSession(dsSession);
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
			
			dsSession = DSJQuerySessionHandler.getSession();
		
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
			DSJQuerySessionHandler.returnSession(dsSession);
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
			
			dsSession = DSJQuerySessionHandler.getSession();
		
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
			DSJQuerySessionHandler.returnSession(dsSession);
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
	
	
	/**
	 * Reduces the set of objects to those with property values starting with a given value.
	 * @category FILTERING
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @param ignoreCase
	 * @return
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 */
	public DSJQuery filter_byProperty_startsWith (String propertyName, String propertyValue, boolean ignoreCase) throws DSException, DSJQueryException {
				
		List<DSObject> newDsObjects = new ArrayList<>(dsObjects.size() / 2 + 1);	
		
		final String propertyValueForCompare = (ignoreCase ? propertyValue.toLowerCase() : propertyValue);

		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(propertyName);
			
			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.startsWith(propertyValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Reduces the set of objects to those with property values ending with a given value.
	 * @category FILTERING
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @param ignoreCase
	 * @return
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 */
	public DSJQuery filter_byProperty_endsWith (String propertyName, String propertyValue, boolean ignoreCase) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new ArrayList<>(dsObjects.size() / 2 + 1);
		
		String propertyValueForCompare = (ignoreCase ? propertyValue.toLowerCase() : propertyValue);
		
		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(propertyName);

			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.endsWith(propertyValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Reduces the set of objects to those with property values containing a given value.
	 * @category FILTERING
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @param ignoreCase
	 * @return
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 */
	public DSJQuery filter_byProperty_contains (String propertyName, String propertyValue, boolean ignoreCase) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new ArrayList<>(dsObjects.size() / 2 + 1);
		
		final String propertyValueForCompare = (ignoreCase ? propertyValue.toLowerCase() : propertyValue);
		
		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(propertyName);

			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.contains(propertyValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Reduces the set of objects to those with property values equal to a given value.
	 * @category FILTERING
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @param ignoreCase
	 * @return
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 */
	public DSJQuery filter_byProperty_equals (String propertyName, String propertyValue, boolean ignoreCase) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new ArrayList<>(dsObjects.size() / 2 + 1);
		
		String propertyValueForCompare = (ignoreCase ? propertyValue.toLowerCase() : propertyValue);
		
		for (DSObject obj : dsObjects) {
			
			Object value = obj.get(propertyName);
			
			if (value != null) {

				String stringValue = value.toString();
				
				if (ignoreCase) {
					stringValue = stringValue.toLowerCase();
				}
				
				if (stringValue.equals(propertyValueForCompare)) {
					newDsObjects.add(obj);
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Reduces the set of objects to those of a given object class.
	 * @category FILTERING
	 * 
	 * @param className
	 * @return
	 * 
	 * @throws DSException
	 * @throws DSJQueryException
	 */
	public DSJQuery filter_byObjectClass (String className) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjectsCopy = new ArrayList<>(dsObjects.size() / 2 + 1);
		
		for (DSObject obj : dsObjects) {
			
			String objClassName = obj.getDSClass().getName();
			
			if (!objClassName.equals(className)) {
				newDsObjectsCopy.add(obj);
			}
		}
		
		return new DSJQuery(newDsObjectsCopy);
	}
	
	
	/**
	 * Reduces the set of objects to those that match the given selector.
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
				
				String propertyName = filterSelector.substring(1, filterSelector.indexOf("^="));
				String propertyValue = filterSelector.substring(filterSelector.indexOf("^=") + 3, filterSelector.length() - 2);
				
				return filter_byProperty_startsWith(propertyName, propertyValue, false);
			}
			
			// ends with
			else if (filterSelector.contains("$=")) {
					
				String propertyName = filterSelector.substring(1, filterSelector.indexOf("$="));
				String propertyValue = filterSelector.substring(filterSelector.indexOf("$=") + 3, filterSelector.length() - 2);
				
				return filter_byProperty_endsWith(propertyName, propertyValue, false);
			}
			
			// contains
			else if (filterSelector.contains("~=")) {
					
				String propertyName = filterSelector.substring(1, filterSelector.indexOf("~="));
				String propertyValue = filterSelector.substring(filterSelector.indexOf("~=") + 3, filterSelector.length() - 2);
				
				return filter_byProperty_contains(propertyName, propertyValue, false);
			}
			else if (filterSelector.contains("=")) {
				
				String propertyName = filterSelector.substring(1, filterSelector.indexOf("="));
				String propertyValue = filterSelector.substring(filterSelector.indexOf("=") + 2, filterSelector.length() - 2);
				
				return filter_byProperty_equals(propertyName, propertyValue, false);
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
	 * Sets an attribute across all current objects.
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
	
	
	private static boolean hasKeyword (DSObject dsObject, String keyword, boolean ignoreCase) throws DSException {
		
		String currentKeywordsString = dsObject.getKeywords().trim();
		
		if (ignoreCase) {
			keyword = keyword.toLowerCase();
			currentKeywordsString = currentKeywordsString.toLowerCase();
		}
		
		String[] currentKeywords = currentKeywordsString.split(",");
		
		for (String currentKeyword : currentKeywords) {

			if (currentKeyword.trim().equals(keyword)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Determines whether any of the current objects have a given keyword,
	 * with the option to ignore case.
	 * @category ATTRIBUTES
	 * 
	 * @param keyword
	 * @param ignoreCase - TRUE if case should be ignored
	 * @return TRUE if the keyword is assigned to at least one object
	 * 
	 * @throws DSException
	 */
	public boolean hasKeyword (String keyword, boolean ignoreCase) throws DSException {
		
		if (dsObjects == null) {
			return false;
		}
		
		for (DSObject dsObject : dsObjects) {
			
			if (hasKeyword(dsObject, keyword, ignoreCase)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Determines whether any of the current objects have a given keyword.
	 * @category ATTRIBUTES
	 * 
	 * @param keyword
	 * @return
	 * 
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/hasclass/">hasClass() | jQuery API</a>
	 */
	public boolean hasKeyword (String keyword) throws DSException {
		return hasKeyword (keyword, false);
	}
	
	
	/**
	 * Adds a new keyword to the comma-separated keyword list for each DSObject.
	 * The new keyword is only added if the keyword is not already part of the list.
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
			
			if (!hasKeyword(obj, keywordToAdd, false)) {
				
				String currentKeywordsString = obj.getKeywords();
				
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
					
					List<String> newKeywordsList = Arrays.asList(currentKeywords);
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
			
			dsSession = DSJQuerySessionHandler.getSession();
		
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
			DSJQuerySessionHandler.returnSession(dsSession);
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
			dsSession = DSJQuerySessionHandler.getSession();
		
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
			DSJQuerySessionHandler.returnSession(dsSession);
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
