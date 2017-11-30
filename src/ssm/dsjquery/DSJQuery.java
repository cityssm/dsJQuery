package ssm.dsjquery;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSInvalidLicenseException;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSResultIterator;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.db.DatabaseException;
import com.xerox.docushare.object.DSCollection;
import com.xerox.docushare.query.DSCollectionScope;
import com.xerox.docushare.query.DSQuery;

/**
 * <b>DSJQuery - DocuShare jQuery</b><br />
 * A DocuShare object query library similar to jQuery.
 * @author d.gowans
 *
 */
public class DSJQuery {

	private DSSession SESSION;
	private List<DSObject> dsObjects = null;
	
	
	/**
	 * Connect to a DocuShare server using an existing session.
	 * @param session
	 */
	public DSJQuery(DSSession session) {
		SESSION = session;
	}
	
	
	public DSJQuery(DSSession session, List<DSObject> dsObjects) {
		this(session);
		this.dsObjects = dsObjects;
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
		
		return new DSJQuery(SESSION, newDsObjects);
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
		
		return new DSJQuery(SESSION, newDsObjects);
	}
	
	
	public DSJQuery find_byClassName (String className) throws DSException {
		
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
		
		return new DSJQuery(SESSION, newDsObjects);
	}
	
	/**
	 * Searches beneath all currently selected collections for documents and collections.
	 * @param selector
	 * @return
	 * @throws Exception
	 */
	public DSJQuery find (String selector) throws Exception {

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
		
		return new DSJQuery(SESSION, new LinkedList<DSObject>());
	}
	
	
	/**
	 * Filters the current set of documents and collections using a selector.
	 * @param selector
	 * @return
	 * @throws DSException
	 * @throws Exception
	 */
	public DSJQuery filter (String selector) throws DSException, Exception {
		
		/*
		 * For a filter to work, we must have some objects.
		 * If none, quit now!
		 */
		
		if (dsObjects == null)
			return new DSJQuery(SESSION);
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(SESSION, new LinkedList<DSObject>());
		}
			
		List<DSObject> newDsObjects = new LinkedList<DSObject>(dsObjects);
		List<DSObject> newDsObjectsCopy = new LinkedList<DSObject>(dsObjects);
		
		if (selector.startsWith(".")) {
			
			String filterClassName = selector.substring(1);

			for (DSObject obj : newDsObjects) {
				
				String objClassName = obj.getDSClass().getName();
				
				if (!objClassName.equals(filterClassName)) {
					newDsObjectsCopy.remove(obj);
				}
			}
		}
		else if (selector.startsWith("[")) {

			if (selector.contains("=")) {
				
				String propertyName = selector.substring(1, selector.indexOf("="));
				
				String startValue = selector.substring(selector.indexOf("=") + 2, selector.length() - 2);
				
				for (DSObject obj : newDsObjects) {
	
					Object value = obj.get(propertyName);
					
					if (value == null || !value.toString().equals(startValue)) {
						newDsObjectsCopy.remove(obj);
					}
				}
			}

			// starts with
			else if (selector.contains("^=")) {
				
				String propertyName = selector.substring(1, selector.indexOf("^="));
				
				String startValue = selector.substring(selector.indexOf("^=") + 3, selector.length() - 2);
				
				for (DSObject obj : newDsObjects) {
	
					Object value = obj.get(propertyName);
					
					if (value == null || !value.toString().startsWith(startValue)) {
						newDsObjectsCopy.remove(obj);
					}
				}
			}
			
			// ends with
			else if (selector.contains("$=")) {
					
				String propertyName = selector.substring(1, selector.indexOf("$="));
				
				String startValue = selector.substring(selector.indexOf("$=") + 3, selector.length() - 2);
				
				for (DSObject obj : newDsObjects) {
	
					Object value = obj.get(propertyName);
	
					if (value == null || !value.toString().endsWith(startValue)) {
						newDsObjectsCopy.remove(obj);
					}
				}
			}
			
			// contains
			else if (selector.contains("~=")) {
					
				String propertyName = selector.substring(1, selector.indexOf("~="));
				
				String startValue = selector.substring(selector.indexOf("~=") + 3, selector.length() - 2);
				
				for (DSObject obj : newDsObjects) {
	
					Object value = obj.get(propertyName);
	
					if (value == null || !value.toString().contains(startValue)) {
						newDsObjectsCopy.remove(obj);
					}
				}
			}
			else {
				throw new Exception("Unknown filter selector: " + selector);
			}
		}
		else {
			throw new Exception("Unknown filter selector: " + selector);
		}
		
		return new DSJQuery(SESSION, newDsObjectsCopy);
	}

	
	/**
	 * Sorts the current set of objects using a Comparator.
	 * @param comparator
	 * @return
	 */
	public DSJQuery sort (Comparator<DSObject> comparator) {
		
		if (dsObjects == null)
			return new DSJQuery(SESSION);
		
		if (dsObjects.size() == 0) {
			return new DSJQuery(SESSION, new LinkedList<DSObject>());
		}
			
		List<DSObject> newDsObjects = new LinkedList<DSObject>(dsObjects);

		newDsObjects.sort(comparator);
		
		return new DSJQuery(SESSION, newDsObjects);
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
			return new DSJQuery(SESSION);
		
		List<DSObject> newObjectList = new LinkedList<>(dsObjects);
		
		Collections.reverse(newObjectList);
		
		return new DSJQuery(SESSION, newObjectList);
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
	 * Creates a new DSJQuery object with the same set of matching objects.
	 */
	public DSJQuery clone() {
		return new DSJQuery(SESSION, new LinkedList<DSObject>(dsObjects));
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

	
	
	
	public DSJQuery print() throws DSException {
		
		for (DSObject obj : dsObjects) {
			System.out.println (obj.getHandle().toString() + " - " + obj.getTitle());
		}
		System.out.println();
		
		return this;
	}
}
