package ca.saultstemarie.dsjquery;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSClass;
import com.xerox.docushare.DSContentElement;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSInvalidLicenseException;
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

	private static DSSession SESSION = null;
	
	
	/**
	 * Sets the DSSession object to be used for queries.
	 * @category SETUP
	 * 
	 * @param dsSession An active session with a DocuShare server.
	 */
	public static void sessionSetup(DSSession dsSession) {
		SESSION = dsSession;
	}
	
	
	private List<DSObject> dsObjects = null;
	
	
	/**
	 * Creates a new DSJQuery object at the root of the DocuShare library.
	 * It contains no DSObjects, but in it's state, and DSObject in the library can be selected.
	 * @throws DSJQueryException 
	 * @category CORE
	 * 
	 *  @see <a href="https://api.jquery.com/jQuery/">jQuery() | jQuery API</a>
	 */
	public DSJQuery() throws DSJQueryException {
		if (SESSION == null) {
			throw new DSJQueryException("No DocuShare session available. Set using DSJQuery.sessionSetup();");
		}
	}
	
	
	/**
	 * Creates a new DSJQuery object with DSObjects that match the given filter.
	 * @category CORE
	 * 
	 * @param findSelector
	 * @throws DSException 
	 * @throws DSInvalidLicenseException 
	 * @throws DSJQueryException 
	 */
	public DSJQuery(String findSelector) throws DSInvalidLicenseException, DSException, DSJQueryException {
		this();
		dsObjects = find(findSelector).dsObjects;
	}
	
	
	private DSJQuery(List<DSObject> dsObjects) throws DSJQueryException {
		this();
		this.dsObjects = dsObjects;
	}
	
	
	private DSJQuery(DSObject dsObject) throws DSJQueryException {
		this();
		dsObjects = new LinkedList<>();
		dsObjects.add(dsObject);
	}
	
	
	public DSJQuery find_all() throws DSInvalidLicenseException, DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
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

	
	public DSJQuery find_byHandle (String handle) throws DSInvalidLicenseException, DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
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
	
	
	public DSJQuery find_byClassName (String className) throws DSInvalidLicenseException, DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
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
	 * @category TRAVERSING
	 * 
	 * @param selector In jQuery-style, either * or starting with # or .
	 * @return A new DSJquery object
	 * @throws DSException 
	 * @throws DSInvalidLicenseException 
	 * @throws DSJQueryException 
	 * @see <a href="https://api.jquery.com/find/">find() | jQuery API</a>
	 */
	public DSJQuery find (String selector) throws DSInvalidLicenseException, DSException, DSJQueryException {

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
		
		throw new DSJQuerySelectorException(selector);
	}
	
	
	/**
	 * Retrieves the immediate descendants for all selected collections. 
	 * @category TRAVERSING
	 * 
	 * @return A new DSJQuery object
	 * @throws DSInvalidLicenseException
	 * @throws DSException
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/children/">children() | jQuery API</a>
	 */
	public DSJQuery children () throws DSInvalidLicenseException, DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>();
		
		if (dsObjects == null) {
			return new DSJQuery(new LinkedList<>());			
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
	 * @category TRAVERSING
	 * 
	 * @param filterSelector
	 * @return A new DSJQuery object
	 * @throws DSInvalidLicenseException
	 * @throws DSException
	 * @throws DSJQueryException 
	 * 
	 * @see <a href="https://api.jquery.com/children/">children() | jQuery API</a>
	 */
	public DSJQuery children (String filterSelector) throws DSInvalidLicenseException, DSException, DSJQueryException {
		return children().filter(filterSelector);
	}
	
		
	public DSJQuery filter_byAttribute_startsWith (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException, DSJQueryException {
				
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
	
	
	public DSJQuery filter_byAttribute_endsWith (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException, DSJQueryException {
		
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
	
	
	public DSJQuery filter_byAttribute_contains (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException, DSJQueryException {
		
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
	
	
	public DSJQuery filter_byAttribute_equals (String attributeName, String attributeValue, boolean ignoreCase) throws DSAuthorizationException, DSException, DSJQueryException {
		
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
	
	
	public DSJQuery filter_byClassName (String className) throws DSException, DSJQueryException {
		
		List<DSObject> newDsObjects = new LinkedList<>(dsObjects);
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
	 * @param selector
	 * @return A new DSJquery object
	 * @throws DSException
	 * @throws DSJQueryException 
	 * @see <a href="https://api.jquery.com/filter/">filter() | jQuery API</a>
	 */
	public DSJQuery filter (String selector) throws DSException, DSJQueryException {
		
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
				throw new DSJQuerySelectorException(selector);
			}
		}
		else {
			throw new DSJQuerySelectorException(selector);
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
		return new DSJQuery(new LinkedList<>());
	}
	
	
	/**
	 * Sorts the current set of objects using a Comparator.
	 * @category SORTING
	 * 
	 * @param comparator
	 * @return A new, sorted DSJQuery object
	 * @throws DSJQueryException 
	 */
	public DSJQuery sort (Comparator<DSObject> comparator) throws DSJQueryException {
		
		if (dsObjects == null)
			return new DSJQuery();
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(new LinkedList<>());
		}
			
		List<DSObject> newDsObjects = new LinkedList<>(dsObjects);

		newDsObjects.sort(comparator);
		
		return new DSJQuery(newDsObjects);
	}
	
	
	/**
	 * Sorts the current set of objects in ascending order by an attribute.
	 * @category SORTING
	 * 
	 * @param attributeName
	 * @return The current DSJQuery object
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
	 * Reverses the order of the current set of objects.  Can be used to reorder a list in descending order after calling sortAsc.
	 * @category SORTING
	 * 
	 * @return A new DSJQuery object
	 * @throws DSJQueryException 
	 */
	public DSJQuery reverse() throws DSJQueryException {
		
		if (dsObjects == null)
			return new DSJQuery();
		
		List<DSObject> newObjectList = new LinkedList<>(dsObjects);
		
		Collections.reverse(newObjectList);
		
		return new DSJQuery(newObjectList);
	}
	
	
	/**
	 * Get the value of an attribute for the first element in the current set.
	 * @category ATTRIBUTES
	 * 
	 * @param attributeName
	 * @return The selected attribute value
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
	 * Adds a new keyword to the keyword list if the keyword is not already part of the list.
	 * @category ATTRIBUTES
	 * 
	 * @param keywordToAdd
	 * @return The current DSJQuery object
	 * @throws DSAuthorizationException
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/addClass/">addClass() | jQuery API</a>
	 */
	public DSJQuery addKeyword (String keywordToAdd) throws DSAuthorizationException, DSException {
		
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
	 * @throws DSAuthorizationException
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/removeClass/">removeClass() | jQuery API</a>
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
	 * To override Object.clone(), this method assumes that the static session object is still available.
	 * 
	 * @see <a href="https://api.jquery.com/clone/">clone() | jQuery API</a>
	 */
	public DSJQuery clone() {
		try {
			return new DSJQuery(new LinkedList<>(dsObjects));
		} 
		catch (DSJQueryException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Links all child elements under the current set of collections.
	 * @category INSERTING
	 * 
	 * @param newChildren
	 * @return The current DSJquery object
	 * @throws DSException
	 * 
	 * @see <a href="https://api.jquery.com/append/">append() | jQuery API</a>
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

	
	public DSJQuery insertAndGet (File file) throws DSAuthorizationException, DSInvalidLicenseException, DSException, DSJQueryException {
		
		if (dsObjects == null) {
			return new DSJQuery(new LinkedList<>());
		}
		
		if (!file.exists()) {
			throw new DSJQueryException("File does not exist: " + file.getAbsolutePath());
		}
		else if (file.isDirectory()) {
			throw new DSJQueryException("File is a directory: " + file.getAbsolutePath());
		}
		
		LinkedList<DSObject> newDsObjects = new LinkedList<>();

		for (DSObject potentialParent : dsObjects) {
			
			if (potentialParent instanceof DSCollection) {
				
				DSCollection parentCollection = (DSCollection)potentialParent;
				
				String title = file.getName();
				
				// Document Prototype
				DSClass docClass = SESSION.getDSClass(DSDocument.classname);
				DSProperties docProto = docClass.createPrototype();
				docProto.setPropValue(DSObject.title, title);
				
				// Version Prototype
				DSClass versionClass = SESSION.getDSClass(DSVersion.classname);
				DSProperties versionProto = versionClass.createPrototype();
				versionProto.setPropValue(DSObject.title, title);
				versionProto.setPropValue(DSVersion.revision_comments, "(Initial version)");
				
				// Rendition Prototype
				DSClass renditionClass = SESSION.getDSClass(DSRendition.classname);
				DSProperties renditionProto = renditionClass.createPrototype();
				renditionProto.setPropValue(DSRendition.title, title);
				
				FileContentElement ce = new FileContentElement(file.getAbsolutePath(), false);
				
				DSHandle newDocHandle = SESSION.createDocument(
						docProto, 
						versionProto,
						renditionProto,
						new DSContentElement[] {ce},
						null,
						DSLinkDesc.containment,
						parentCollection,
						(DSLoginPrincipal)SESSION.getObject(SESSION.getLoginPrincipalHandle()),
						null);
				
				newDsObjects.add(SESSION.getObject(newDocHandle));
			}
		}
		
		return new DSJQuery(newDsObjects);
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
			return new LinkedList<DSObject>().iterator();
		}
		
		return dsObjects.iterator();
	}
}
