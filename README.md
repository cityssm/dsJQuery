# dsJQuery

DocuShare jQuery

An attempt to simplify the [Xerox DocuShare](https://www.docushare.com/) API by modeling after the familiar 
[jQuery API](http://api.jquery.com/).

## Basic Usage

Be sure to include a `dsapi.jar` that corresponds to your DocuShare installation,
available on the [DocuShare Developer Network website](https://docushare.xerox.com/dsdn/).

Developed and tested against DocuShare 6.6.1.

```java
DSServer dsServer = null;
DSSession dsSession = null;
	
try {
     // Create a DocuShare session
    dsServer = DSFactory.createServer(ds_serverName);
    dsSession = dsServer.createSession(ds_domain, ds_userName, ds_password);
    
    // Define the session
    DSJQuery.sessionSetup(dsSession);
    
    // Create a new DSJquery object and retrieve the root collection
    DSJQuery dsjQuery_rootCollection = new DSJQuery("#Collection-111");
    
    // Retrieve all documents under the root collection
    DSJQuery dsjQuery_documents = dsjQuery_rootCollection.find(".Document");
    
    dsjQuery_documents
        .print()
        .sortAsc_byAttribute("title")
        .print();	
}
catch (Exception e) {
    e.printStackTrace();
}
finally {
    // Close the DocuShare session
    try {
        dsSession.close();
        dsServer.close();
    }
    catch (Exception e) {
        // ignore
    }
}
```

## Key Methods

**DSJQuery.sessionSetup(DSSession dsSession);**

Sets the DocuShare session that should be used.

**dsjQuery.find(String selector);**

Searches beneath all collections for objects the match the given selector.

- Use `*` to select all child objects.
- Use `.` to select by class.
  - i.e. `.Collection`, `.Document`
- Use `#` to select a specific object by handle.
  - i.e. `.Collection-111`

**dsjQuery.filter(String selector);**

Reduces the set of matched objects to those that match the selector.

- Use `.` to filter by class.  i.e. `.Collection`, `.Document`
- Use `[name='value']` to filter those objects with attributes equal to a given value.
  - i.e. `[locale='en']`
- Use `[name^='value']` to filter those objects with attributes starting with a given value.
  - i.e. `[content_type^='image/']`
- Use `[name$='value']` to filter those objects with attributes ending with a given value.
  - i.e. `[original_file_name$='.docx']`
- Use `[name~='value']` to filter those objects with attributes containing a given value.
  - i.e. `[keywords~='logo']`

## Samples Selectors

**Retrieve all documents under known collection.**

```java
DSJQuery dsjQuery_documents = new DSJQuery("#Collection-111").find(".Document");
```
    
**Retrieve all PNG documents with the word 'Logo' in the title sorted with the newer files first.**

```java
DSJQuery dsjQuery_documents = new DSJQuery(".Document")
    .filter("[title~='Logo']")
    .filter("[content_type='image/png']")
    .sortAsc_byAttribute("create_date")
    .reverse();
```