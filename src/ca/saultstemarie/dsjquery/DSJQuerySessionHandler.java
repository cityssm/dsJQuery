package ca.saultstemarie.dsjquery;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import com.xerox.docushare.DSException;
import com.xerox.docushare.DSFactory;
import com.xerox.docushare.DSServer;
import com.xerox.docushare.DSSession;


/**
 * <b>DSJQuery Session Handler</b>
 * @author d.gowans
 *
 */
public class DSJQuerySessionHandler {

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
}
