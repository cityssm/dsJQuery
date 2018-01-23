package ca.saultstemarie.dsjquery;

public class DSJQueryException extends Exception {

	private static final long serialVersionUID = 504683154200081254L;
	
	public DSJQueryException() {
		super();
	}
	
	public DSJQueryException(String message) {
		super(message);
	}

	public static class DSJQueryFilterException extends DSJQueryException {

		private static final long serialVersionUID = 3181053289405098940L;

		public DSJQueryFilterException(String filter) {
			super(filter);
		}
	}
}
