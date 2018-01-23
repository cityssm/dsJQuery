package ca.saultstemarie.dsjquery;

public class DSJQueryException extends Exception {

	private static final long serialVersionUID = 504683154200081254L;
	
	public DSJQueryException() {
		super();
	}
	
	public DSJQueryException(String message) {
		super(message);
	}
	
	
	/**
	 * An exception thrown when an unsupported selector is used.
	 * @author d.gowans
	 *
	 */
	public static class DSJQuerySelectorException extends DSJQueryException {

		private static final long serialVersionUID = 3181053289405098940L;

		public DSJQuerySelectorException(String selector) {
			super(selector);
		}
	}
}
