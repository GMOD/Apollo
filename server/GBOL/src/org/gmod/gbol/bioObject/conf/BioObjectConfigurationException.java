package org.gmod.gbol.bioObject.conf;

/** Exception class thrown when there are issues parsing the configuration.
 * 
 * @author elee
 *
 */

public class BioObjectConfigurationException extends RuntimeException {
	
	private final static long serialVersionUID = 1L;
	
	/** Constructor.
	 * 
	 * @param errMsg - Error message
	 */
	public BioObjectConfigurationException(String errMsg)
	{
		super(errMsg);
	}
}
