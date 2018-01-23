package ssm.dsjquery;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSInvalidLicenseException;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSObjectIterator;
import com.xerox.docushare.DSResultIterator;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.db.DatabaseException;
import com.xerox.docushare.object.DSCollection;
import com.xerox.docushare.query.DSCollectionScope;
import com.xerox.docushare.query.DSQuery;

import ssm.dsjquery.DSJQueryException.DSJQueryFilterException;

/**
 * <b>DSJQuery - DocuShare jQuery</b><br />
 * A DocuShare object query library similar to jQuery.
 * @author d.gowans
 *
 */
public class DSJQuery {

	private static DSSession SESSION = null;
	
	
	/**
	 * Sets the DSSession object to be used for queries.
	 * @param dsSession
	 */
	public static void sessionSetup(DSSession dsSession) {
		SESSION = dsSession;
	}
	
	
	private List<DSObject> dsObjects = null;
	
	
	public DSJQuery() {
		if (SESSION == null) {
			throw new NullPointerException("No DocuShare session available. Set using DSJQuery.sessionSetup();");
		}
	}
	
	
	public DSJQuery(String findSelector) throws Exception {
		this();
		dsObjects = find(findSelector).dsObjects;
	}
	
	
	private DSJQuery(List<DSObject> dsObjects) {
		this();
		this.dsObjects = dsObjects;
	}
	
	
	private DSJQuery(DSObject dsObject) {
		this();
		dsObjects = new LinkedList<>();
		dsObjects.add(dsObject);
	}
	
	
	public DSJQuery find_all() throws DSInvalidLicenseException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
		if (dsObjects == null) {
			
			DSQuery query = new DSQuery();
			DSResultIterator result = SESSION.search(query).iterator();
			
			while (result.hasNext()) {
				DSObject item = result.nextObject().getObject();
				newDsObjects.add(item);
			}
			
		}
		else {
			
			for (DSObject parentObj : dsObjects) {
				
				if (parentObj instanceof DSCollection) {

					DSQuery query = new DSQuery();
					query.addCollectionScope( new DSCollectionScope( new DSHandle[]{parentObj.getHandle()}) );

					DSResultIterator result = SESSION.search(query).iterator();
					
					while (result.hasNext()) {
						DSObject item = result.nextObject().getObject();
						newDsObjects.add(item);
					}
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}

	
	public DSJQuery find_byHandle (String handle) throws DSInvalidLicenseException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
		if (dsObjects == null) {
			
			try {
				DSObject obj = SESSION.getObject(new DSHandle(handle));
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
					
					DSResultIterator result = SESSION.search(query).iterator();
					
					if (result.hasNext()) {
						DSObject item = result.nextObject().getObject();
						newDsObjects.add(item);
						break;
					}
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	public DSJQuery find_byClassName (String className) throws DSInvalidLicenseException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
		if (dsObjects == null) {
			
			DSQuery query = new DSQuery();
			query.addClassScope(className);
			
			DSResultIterator result = SESSION.search(query).iterator();
			
			while (result.hasNext()) {
				DSObject item = result.nextObject().getObject();
				newDsObjects.add(item);
			}
			
		}
		else {
			
			for (DSObject parentObj : dsObjects) {
				
				if (parentObj instanceof DSCollection) {

					DSQuery query = new DSQuery();
					query.addCollectionScope( new DSCollectionScope( new DSHandle[]{parentObj.getHandle()}) );
					query.addClassScope(className);
					
					DSResultIterator result = SESSION.search(query).iterator();
					
					while (result.hasNext()) {
						DSObject item = result.nextObject().getObject();
						newDsObjects.add(item);
					}
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Searches beneath all currently selected collections for documents and collections.
	 * @param selector
	 * @return
	 * @throws DSException 
	 * @throws DSInvalidLicenseException 
	 * @throws Exception
	 */
	public DSJQuery find (String selector) throws DSInvalidLicenseException, DSException {

		/*
		 * If selectorToken is *, retrieve all child elements
		 */
		if (selector.equals("*")) {
			return find_all();
		}
		
		
		/*
		 * If selectorToken starts with "#", retrieve by Handle.
		 */
		else if (selector.startsWith("#")) {
			
			String handle = selector.substring(1);
			return find_byHandle(handle);
			
			
		}
		
		/*
		 * If selectorToken starts with a ".", search by class name.
		 */
		else if (selector.startsWith(".")) {
			
			String className = selector.substring(1);
			return find_byClassName(className);	
		}
		
		return new DSJQuery(new LinkedList<DSObject>());
	}
	
	
	/**
	 * Retrieves the immediate descendants for all selected collections. 
	 * @return
	 * @throws DSInvalidLicenseException
	 * @throws DSException
	 */
	public DSJQuery children () throws DSInvalidLicenseException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
		if (dsObjects == null) {
			return new DSJQuery(new LinkedList<DSObject>());			
		}
		else {
			
			for (DSObject parentObj : dsObjects) {
				
				if (parentObj instanceof DSCollection) {
					
					DSObjectIterator iterator = ((DSCollection) parentObj).children(null);

					while (iterator.hasNext()) {
						DSObject item = iterator.nextObject();
						newDsObjects.add(item);
					}
				}
			}
		}
		
		return new DSJQuery(newDsObjects);
	}

	
	/**
	 * Retrieves the immediate descendants for all selected collections which satisfy a given filter. 
	 * @return
	 * @throws DSInvalidLicenseException
	 * @throws DSException
	 */
	public DSJQuery children (String filterSelector) throws DSInvalidLicenseException, DSException, DSJQueryFilterException {
		return children().filter(filterSelector);
	}
	
	
	public DSJQuery filter_byAttribute_startsWith (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException {
				
		List<DSObject> newDsObjects = new LinkedList<DSObject>();	
		
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
	
	
	public DSJQuery filter_byAttribute_endsWith (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
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
	
	
	public DSJQuery filter_byAttribute_contains (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
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
	
	
	public DSJQuery filter_byAttribute_equals (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>();
		
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
	
	
	public DSJQuery filter_byClassName (String className) throws DSException {
		
		List<DSObject> newDsObjects = new LinkedList<DSObject>(dsObjects);
		List<DSObject> newDsObjectsCopy = new LinkedList<DSObject>(dsObjects);
		
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
	 * @param selector
	 * @return
	 * @throws DSException
	 * @throws DSJQueryFilterException 
	 */
	public DSJQuery filter (String selector) throws DSException, DSJQueryFilterException {
		
		/*
		 * For a filter to work, we must have some objects.
		 * If none, quit now!
		 */
		
		if (dsObjects == null)
			return new DSJQuery();
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(new LinkedList<>());
		}
			
		
		if (selector.startsWith(".")) {
			
			String filterClassName = selector.substring(1);
			return filter_byClassName(filterClassName);
		}
		else if (selector.startsWith("[")) {
			
			// starts with
			if (selector.contains("^=")) {
				
				String attributeName = selector.substring(1, selector.indexOf("^="));
				String attributeValue = selector.substring(selector.indexOf("^=") + 3, selector.length() - 2);
				
				return filter_byAttribute_startsWith(attributeName, attributeValue, false);
			}
			
			// ends with
			else if (selector.contains("$=")) {
					
				String attributeName = selector.substring(1, selector.indexOf("$="));
				String attributeValue = selector.substring(selector.indexOf("$=") + 3, selector.length() - 2);
				
				return filter_byAttribute_endsWith(attributeName, attributeValue, false);
			}
			
			// contains
			else if (selector.contains("~=")) {
					
				String attributeName = selector.substring(1, selector.indexOf("~="));
				String attributeValue = selector.substring(selector.indexOf("~=") + 3, selector.length() - 2);
				
				return filter_byAttribute_contains(attributeName, attributeValue, false);
			}
			else if (selector.contains("=")) {
				
				String attributeName = selector.substring(1, selector.indexOf("="));
				String attributeValue = selector.substring(selector.indexOf("=") + 2, selector.length() - 2);
				
				return filter_byAttribute_equals(attributeName, attributeValue, false);
			}

			
			else {
				throw new DSJQueryException.DSJQueryFilterException(selector);
			}
		}
		else {
			throw new DSJQueryException.DSJQueryFilterException(selector);
		}
	}

	
	/**
	 * Filters the current set of documents and collections to only include the first one.
	 * @return
	 */
	public DSJQuery first () {
		if (dsObjects.size() > 0) {
			return new DSJQuery(dsObjects.get(0));
		}
		return new DSJQuery(new LinkedList<>());
	}
	
	
	/**
	 * Sorts the current set of objects using a Comparator.
	 * @param comparator
	 * @return
	 */
	public DSJQuery sort (Comparator<DSObject> comparator) {
		
		if (dsObjects == null)
			return new DSJQuery();
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(new LinkedList<DSObject>());
		}
			
		List<DSObject> newDsObjects = new LinkedList<>(dsObjects);

		newDsObjects.sort(comparator);
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Sorts the current set of objects in ascending order by an attribute.
	 * @param attributeName
	 * @return
	 * @throws DSException
	 */
	public DSJQuery sortAsc_byAttribute (String attributeName) throws DSException {
		
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
	 * Reverses the order of the current set of objects.  Can be used to reorder a list in descending order after calling sortAsc.
	 * @return
	 */
	public DSJQuery reverse() {
		
		if (dsObjects == null)
			return new DSJQuery();
		
		List<DSObject> newObjectList = new LinkedList<>(dsObjects);
		
		Collections.reverse(newObjectList);
		
		return new DSJQuery(newObjectList);
	}
	
	
	/**
	 * Get the value of an attribute for the first element in the current set.
	 * @param attributeName
	 * @return
	 * @throws DSException 
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
	 * @param attributeName
	 * @param value
	 * @return
	 * @throws DSException
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
	 * Adds a new keyword to the keyword list if the keyword is not already part of the list.
	 * @param keywordToAdd
	 * @return
	 * @throws DSAuthorizationException
	 * @throws DSException
	 */
	public DSJQuery addKeyword (String keywordToAdd) throws DSAuthorizationException, DSException {
		
		if (dsObjects == null)
			return this;
		
		for (DSObject obj : dsObjects) {
			
			boolean keywordFound = false;
			String[] currentKeywords = obj.getKeywords().split(",");
			
			for (String keyword : currentKeywords) {
				if (keyword.trim().equals(keywordToAdd)) {
					keywordFound = true;
					break;
				}
			}
			
			if (!keywordFound) {
				obj.setKeywords(obj.getKeywords() + (currentKeywords.length > 0 ? ", " : "") + keywordToAdd);
				obj.save();
			}
		}
		
		return this;
	}
	
	
	/**
	 * Removes the first instance of a keyword from the keyword list if it is found. 
	 * @param keywordToRemove
	 * @return
	 * @throws DSAuthorizationException
	 * @throws DSException
	 */
	public DSJQuery removeKeyword (String keywordToRemove) throws DSAuthorizationException, DSException {
		
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
	 */
	public DSJQuery clone() {
		return new DSJQuery(new LinkedList<>(dsObjects));
	}
	
	
	/**
	 * Links all child elements under the current set of collections.
	 * @param newChildren
	 * @return
	 * @throws DSException
	 */
	public DSJQuery append (DSJQuery newChildren) throws DSException {
		
		if (dsObjects == null) {
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
			}
		}
		
		return this;
	}

	
	/**
	 * Returns the total number of DSObjects in the DSJquery object.
	 * @return
	 */
	public int length() {
		if (dsObjects == null) {
			return 0;
		}
		
		return dsObjects.size();
	}
	
	
	/**
	 * Similar to $().toArray(), returns the List of DSObjects.
	 * @return
	 */
	public List<DSObject> toList() {
		return dsObjects;
	}
		
	
	/**
	 * For debug purposes, outputs the handles and titles of objects currently the DSJQuery object.
	 * @return
	 * @throws DSException
	 */
	public DSJQuery print() throws DSException {
		
		for (DSObject obj : dsObjects) {
			System.out.println (obj.getHandle().toString() + " - " + obj.getTitle());
		}
		System.out.println();
		
		return this;
	}
}
