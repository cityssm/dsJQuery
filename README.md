# dsJQuery

DocuShare jQuery

An attempt to simplify the Xerox DocuShare API by emulating the familiar jQuery API.

## Basic Usage

Be sure to include a `dsapi.jar` that corresponds to your DocuShare installation,
available on the [Docushare Developer Network website](https://docushare.xerox.com/dsdn/). 

    DSServer dsServer = null;
    DSSession dsSession = null;
		
	 try {
	     // Create a DocuShare session
        dsServer = DSFactory.createServer(ds_serverName);
        dsSession = dsServer.createSession(ds_domain, ds_userName, ds_password);
        
        // Create a new DSJquery object and retrieve the root collection
        DSJQuery dsjQuery_rootCollection = new DSJQuery(dsSession)
            .find("#Collection-111")
            .print();
        
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

## Sample Selectors

**Retrieve all documents under known collection.**

    DSJQuery dsjQuery_documents = new DSJQuery(dsSession)
        .find("#Collection-111")
        .find(".Document");
    
**Retrieve all documents with the word 'Logo' in the title.**

    DSJQuery dsjQuery_documents = new DSJQuery(dsSession)
        .find(".Document")
        .filter("[title~='Logo']")