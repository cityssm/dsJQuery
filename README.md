# dsJQuery - DocuShare jQuery

An attempt to simplify the [Xerox DocuShare](https://www.docushare.com/) API by modeling after the familiar 
[jQuery API](http://api.jquery.com/).

## Basic Usage

Be sure to include a `dsapi.jar` that corresponds to your DocuShare installation,
available on the [DocuShare Developer Network website](https://docushare.xerox.com/dsdn/).

Developed and tested against DocuShare 6.6.1.

```java
	
try {
    // Initialize the DSJQuery Session Handler with connection information
    DSJQuerySessionHandler.serverSetup(ds_serverName);
    DSJQuerySessionHandler.sessionSetup(ds_domain, ds_userName, ds_password);
    
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
    // Close open DocuShare sessions
    DSJQuerySessionHandler.closeOpenSessions();
}
```

## Key Methods

[See the JavaDocs](https://cityssm.github.io/dsJQuery/)

**DSJQuerySessionHandler.serverSetup(String serverName, int serverPort);**

**DSJQuerySessionHandler.serverSetup(String serverName);**

- Initializes the DSJQuery Session Handler with the DocuShare server details that should be used.
- REQUIRED BEFORE USE.
- Uses default port number 1099 if the shorthand method is used.


**DSJQuerySessionHandler.sessionSetup(String userDomain, String userName, String password);**

**DSJQuerySessionHandler.sessionSetup(String userName, String password);**

- Initializes the DSJQuery Session Handler with the DocuShare session details that should be used.
- REQUIRED BEFORE USE.
- Uses default domain name "DocuShare" if the shorthand method is used.


**dsjQuery.find(String selector);**

Searches beneath all collections for objects that match the given selector.

- Use `*` to select all child objects.
- Use `.` to select by class.
  - i.e. `.Collection`, `.Document`
- Use `#` to select a specific object by handle.
  - i.e. `#Collection-111`


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

## Troubleshooting "Couldn't get a stream to remote file" and "Connection refused to host"

If you are using dsJQuery to upload files, you may run into an exception similar to the one below.

```
com.xerox.docushare.content.ContentStoreException: Couldn't get a stream to remote file: C:\file.txt; nested exception is: 
	java.rmi.ConnectException: Connection refused to host: 192.168.56.1; nested exception is: 
	java.net.ConnectException: Connection refused: connect; nested exception is: 
	com.xerox.docushare.DSContentElementException: Couldn't get a stream to remote file: C:\file.txt; nested exception is: 
	java.rmi.ConnectException: Connection refused to host: 192.168.56.1; nested exception is: 
	java.net.ConnectException: Connection refused: connect
	...
```

This happens when the remote method gets bound to an incorrect IP address.
In my case, the IP address in the exception is associated with Virtualbox.

There are many ways to solve this.
[See this question on Stack Overflow](https://stackoverflow.com/q/15685686).

The [answer I chose](https://stackoverflow.com/a/28800991) explicitly sets the `java.rmi.server.hostname` property,
**before initializing the Session Handler**.

```java
System.setProperty("java.rmi.server.hostname", properNetworkIpAddress);

try {
    DSJQuerySessionHandler.serverSetup(ds_serverName);
    DSJQuerySessionHandler.sessionSetup(ds_domain, ds_userName, ds_password);
    // ...
}
// ...
```
