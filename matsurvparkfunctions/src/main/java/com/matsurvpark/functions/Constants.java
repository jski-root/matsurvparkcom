package com.matsurvpark.functions;

/**
 * Class container for constant values that are needed to use OAuth
 * 
 * @author jski
 *
 */
public class Constants {

	public final static String clientId = System.getenv("clientId");
	public final static String clientSecret = System.getenv("clientSecret");
	public final static String tenant = System.getenv("tenant");
	public final static String username = System.getenv("SharepointUsername");
	public final static String password = System.getenv("password");
	public final static String schema = System.getenv("schemaRequest");
}