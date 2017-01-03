/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * @author cmg
 *
 */
public class DownloadSaturnModuleMaintFiles {
	/** logger */
    static Logger logger = Logger.getLogger(DownloadSaturnModuleMaintFiles.class);
	static final String BASE_URL = "https://saturnweb.nottingham.ac.uk/nottingham";
	static final String ORGANISATION_CODE = "000245"; // computer science
	static final String MODULE_LIST_URL = BASE_URL+"/modulecatalogue/asp/FindModule.asp";
	static final String MODULE_LIST_BODY = "org_id=${org_id}&year_id=${year}";
	static final String MODULE_URL = BASE_URL+"/ModuleCatalogue/asp/ViewModule.asp?crs_id=${code}&year_id=${year}&asp_type_id=000009";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DownloadSaturnModuleMaintFiles().download(args);
	}
	protected void download(String [] args) {
		
		if (args.length!=3) {
			System.err.println("Usage: <username> <yearcode> <modulelist>");
			System.err.println("Note: <modulelist.html> should be downloaded from Saturn Module Catalogue Maintenance");
//			System.err.println("<cookie> is a cookie of the form COOKIENAME=COOKIEVALUE for a currently authenticated saturnweb session.");
//			System.err.println("E.g. if you are logged into saturnweb from Google Chrome then view cookies (e.g. Chrome -> Options -> Under the Bonnet -> Content Settings... -> Cookies -> Show cookies and other site data... and search for 'saturnweb', find the current (most recently created) cookie, and copy the Name and Content (value).");
			System.err.println("<Yearcode> for example 000112 = 2012/13 (six digits, year-1900)");
			System.exit(-1);			
		}
		try {
			SaturnwebUtils.disableSslChecks();
			String username = args[0];
			String password = null;
			try {
				password = PasswordUtils.getPassword(username);
			}
			catch (Exception e) {
				logger.error("Could not get password for "+username+" - does users.properties exist and contain "+username+".password=...?");
				System.exit(-1);
			}
			String cookie = SaturnwebUtils.login(BASE_URL, username, password);
			String year = args[1];
			// I want to download the module list from saturn, but at the moment i only get back a version without View links...
			String modulelist = args[2];//"modulelist.html";
//			String modulesFile = MODULE_LIST_URL;
//			downloadFile(modulesFile, MODULE_LIST_BODY.replace("${org_id}", ORGANISATION_CODE).replace("${year}", year), modulelist, cookie);
//			File modulelist = new File(args[0]);
//			System.out.println("Read modulelist "+modulelist);
			Pattern p = Pattern.compile("ViewModule.asp[?]crs_id=([0-9]*)[^\\?]*alt='View Module ([^']*)'");
			BufferedReader br = new BufferedReader(new FileReader(modulelist));
			while(true) {
				String line = br.readLine();
				if (line==null) 
					break;
				Matcher m = p.matcher(line);
				while (m.find()) {
					downloadModuleFile(m.group(1), m.group(2), year, cookie);
				}	
			}
			System.out.println("Done");
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}

	protected void downloadModuleFile(String saturncode, String modulecode, String year, String cookie) {
		String urlString = MODULE_URL.replace("${code}", saturncode).replace("${year}", year);
		System.out.println("Download "+saturncode+" "+modulecode+" from "+urlString);
		String filename = modulecode+"_"+year+".html";
		downloadFile(urlString, null, filename, cookie);
	}
	protected static void downloadFile(String urlString, String body, String filename, String cookie) {
		try {
			//DownloadSaturnModuleFiles.downloadFile(url, modulecode+"_"+year+".html");
			//logger.info("Download "+urlString+" to "+filename);
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.addRequestProperty("Cookie", cookie);
			if(body!=null) {
				conn.setRequestMethod("POST");
				conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				conn.setDoInput(true);
				conn.setDoOutput(true);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "US-ASCII"));
				pw.print(body);
				pw.flush();
				pw.close();
			}
			String encoding = conn.getContentEncoding();
			InputStreamReader isr = null;
			if (encoding==null) {
				logger.debug("Warning: No encoding returned for "+urlString);
				isr = new InputStreamReader(conn.getInputStream());
			}
			else
				isr = new InputStreamReader(conn.getInputStream(), encoding);
			
			FileWriter osr = new FileWriter(filename);
			char buf [] = new char[10000];
			int count = 0;
			while(true) {
				int n = isr.read(buf, 0, buf.length);
				if (n<0)
					break;
				osr.write(buf, 0, n);
				count += n;
			}
			osr.close();
			isr.close();
			logger.info("Copied "+count+" characters");
			
		} catch (Exception e) {
			System.err.println("Error downloading "+urlString+": "+e);
			e.printStackTrace(System.err);
		}

	}

}
