package saturntools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** password utils */
public class PasswordUtils {
	private static BufferedReader br = null;	
	/** cache of passwords */
	private static Map<File,String> passwords = new HashMap<File,String>();
	private static TreeSet<String> allPasswords = new TreeSet<String>();
	/** get a password - by default prompt user 
	 * @throws IOException */
	public static synchronized String getPassword(File file) throws IOException {
		if (passwords.containsKey(file)) {
			System.err.println("Trying cached password for "+file);
			String pw = passwords.get(file);
			if (pw!=null)
				return pw;
		}
		else {
			// first try - attempt default
			if (allPasswords.size()>0) {
				passwords.put(file, allPasswords.first());
				System.err.println("Trying previous password(s) for "+file);
				return allPasswords.first();
			}
		}
		System.err.print("Plase enter password for "+file+": ");
		System.err.flush();
		if (br==null) {
			br = new BufferedReader(new InputStreamReader(System.in));
		}
		String pw = br.readLine();
		allPasswords.add(pw);
		passwords.put(file, pw);
		return pw;
	}
	public static void resetInput() {
		br = null;
	}
	/** the last password didn't work */
	public static void forget(File f) {
		String pw = passwords.get(f);
		if (allPasswords.contains(pw) && allPasswords.tailSet(pw).size()>=2) {
			//try next
			Iterator<String> pws = allPasswords.tailSet(pw).iterator();
			pws.next();
			passwords.put(f, pws.next());			
		}
		else
			passwords.put(f,null);
	}
	/** password properties file */
	static final String PASSWORD_FILE = "users.properties";
	static final String PASSWORD_SUFFIX = ".password";
	/** get user password 
	 * @throws IOException */
	public static String getPassword(String username) throws IOException  {
		java.util.Properties props = new java.util.Properties();
		props.load(new FileInputStream(PASSWORD_FILE)); //PasswordUtils.class.getResourceAsStream(PASSWORD_FILE));
		String password = props.getProperty(username+PASSWORD_SUFFIX);
		return password;
	}
}
