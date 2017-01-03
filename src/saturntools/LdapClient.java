/**
 * 
 */
package saturntools;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.log4j.Logger;

/**
 * @author cmg
 *
 */
public class LdapClient {
	/** logger */
	static Logger logger = Logger.getLogger(LdapClient.class);
	
	static final String LDAP_URL = "ldap://ldap.nottingham.ac.uk:389";
	static final String BASE_NAME = "ou=Users,o=University";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Hashtable env = new Hashtable();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, LDAP_URL);
			env.put(Context.SECURITY_AUTHENTICATION, "none");
			System.out.println("Connect to "+LDAP_URL+"...");
			DirContext ctx = new InitialDirContext(env);

			//NamingEnumeration<NameClassPair> objects = ctx.list(BASE_NAME);
			//while(objects.hasMore()) {
			//NameClassPair ncp = objects.next();
			//System.out.println("Found "+ncp.getClassName()+" "+ncp.getName());
			//}
			BasicAttributes searchAtts = new BasicAttributes();
			searchAtts.put("cn", "pszcmg");
			
			SearchControls constraints = new SearchControls();
			constraints.setSearchScope (SearchControls.SUBTREE_SCOPE); 
			
			//NamingEnumeration<SearchResult> results = ctx.search("ou=PS,ou=P,"+BASE_NAME, searchAtts);
			// search filter as per rfc2254
			// some people who don't use their first name have the initial in as initial
			NamingEnumeration<SearchResult> results = ctx.search(BASE_NAME, "(&(sn=Greenhalgh)(initials=M)(givenName=C*))", constraints);
			while (results.hasMore()) {
				SearchResult result = results.next();
				System.out.println("Found "+result.getClassName()+" "+result.getClassName()+" "+result.getAttributes());
			}
			ctx.close();
		}
		catch (Exception e) {
			logger.error("Ldap", e);
		}

		
	}
	/**
	 * @param args
	 */
	public static NamingEnumeration<SearchResult> findPerson(String firstInitial, String familyName) {
		try {
			Hashtable env = new Hashtable();
			env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, LDAP_URL);
			env.put(Context.SECURITY_AUTHENTICATION, "none");
			System.out.println("Connect to "+LDAP_URL+"...");
			DirContext ctx = new InitialDirContext(env);

			//NamingEnumeration<NameClassPair> objects = ctx.list(BASE_NAME);
			//while(objects.hasMore()) {
			//NameClassPair ncp = objects.next();
			//System.out.println("Found "+ncp.getClassName()+" "+ncp.getName());
			//}
			BasicAttributes searchAtts = new BasicAttributes();
			searchAtts.put("cn", "pszcmg");
			
			SearchControls constraints = new SearchControls();
			constraints.setSearchScope (SearchControls.SUBTREE_SCOPE); 
			
			//NamingEnumeration<SearchResult> results = ctx.search("ou=PS,ou=P,"+BASE_NAME, searchAtts);
			// search filter as per rfc2254
			// some people who don't use their first name have the initial in as initial
			NamingEnumeration<SearchResult> results = ctx.search(BASE_NAME, "(&(sn="+familyName+")(|(givenName="+firstInitial+"*)(initials="+firstInitial+"*)))", constraints);
			//while (results.hasMore()) {
	//			SearchResult result = results.next();
		//		System.out.println("Found "+result.getClassName()+" "+result.getClassName()+" "+result.getAttributes());
			//}
			ctx.close();
			return results;
		}
		catch (Exception e) {
			logger.error("Ldap", e);
			return null;
		}

		
	}
}
