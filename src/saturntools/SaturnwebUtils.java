/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
	 
import org.apache.log4j.Logger;

import saturntools.DownloadSaturnModuleMaintFiles;

/**
 * @author cmg
 *
 */
public class SaturnwebUtils {
    static Logger logger = Logger.getLogger(SaturnwebUtils.class);

	private static final String LOGIN_SUFFIX = "asp/logon_saturn_frame.asp";
	private static final String CONTINUE_SUFFIX = "asp/main_frame.asp?Center=../asp/home.asp";
	private static final int DEFAULT_PORT = 80;

	private static final Object SET_COOKIE = "Set-Cookie";

	/** allow broken ssl 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException */
	public static void disableSslChecks() throws NoSuchAlgorithmException, KeyManagementException {
		// http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}
		};

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	
	}
	
	/** return cookie for access if successful */
//	public static String login(String baseUrl, String username, String password) throws IOException {
//		if (!baseUrl.endsWith("/"))
//			baseUrl = baseUrl+"/";
//		URL url = new URL(baseUrl+LOGIN_SUFFIX);
//		int port = url.getPort();
//		if(port<0)
//			port = DEFAULT_PORT;
//		Socket s = new Socket(url.getHost(), port);
//		PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "US-ASCII"));
//		pw.print("GET "+url.getPath()+" HTTP/1.0\r\n");
//		pw.print("Host: "+url.getHost()+"\r\n");
//		pw.print("Content-Type: application/x-www-form-urlencoded\r\n");
//		pw.print("\r\n");
//		pw.print("UserName="+URLEncoder.encode(username)+"&Password="+URLEncoder.encode(password));
//		pw.flush();
//		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), "US-ASCII"));
//		s.getOutputStream().close();
//		String response = br.readLine();
//		String resp[] = response.split(" ");
//		if (!"200".equals(resp[0])) {
//			logger.warn("Not OK response: "+resp[0]+" ("+response+")");
//			throw new IOException("Response code "+resp[0]);
//		}
//		StringBuilder cookie = new StringBuilder();
//		while (true) {
//			String line = br.readLine().trim();
//			if (line.length()==0)
//				break;
//			int ix = line.indexOf(":");
//			if (ix>=0) {
//				String key = line.substring(0, ix).trim().toLowerCase();
//				String value = line.substring(ix+1).trim();
//				if (key.equals(SET_COOKIE)) {
//					String cs [] = value.split(";");
//					for (int ci=0; ci<cs.length; ci++) 
//					{
//						String c = cs[ci].trim();
//						int ex = c.indexOf("=");
//						if (ex>=0) {
//							String cn = c.substring(0, ex).trim();
//							String cv = c.substring(ex+1).trim();
//							if (!"expires".equals(cn.toLowerCase())) {
//								if (cookie.length()>0)
//									cookie.append("; ");
//								cookie.append(cn);
//								cookie.append("=");
//								cookie.append(cv);
//							}
//				
//						}
//					}
//				}
//			}
//		}
//		br.close();
//		s.close();
//		if (cookie.length()>0)
//			return cookie.toString();
//		throw new IOException("No cookie found");
//	}
	
	/** return cookie for access if successful */
	public static String login(String baseUrl, String username, String password) throws IOException {
		if (!baseUrl.endsWith("/"))
			baseUrl = baseUrl+"/";
		URL url = new URL(baseUrl+LOGIN_SUFFIX);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "US-ASCII"));
		pw.print("UserName="+URLEncoder.encode(username)+"&Password="+URLEncoder.encode(password));
		pw.flush();
		pw.close();
		
		if (conn.getResponseCode()!=200) {
			logger.warn("Not OK response: "+conn.getResponseCode()+" ("+conn.getResponseMessage()+")");
			throw new IOException("Response code "+conn.getResponseCode());
		}
		StringBuilder cookie = new StringBuilder();
		Map<String,List<String>> headers = conn.getHeaderFields();
		for (String key: headers.keySet()) {
			logger.info("Header "+key+":");
			for (String value : headers.get(key)) {
				logger.info("  "+value);
			}
		}
		List<String> values = conn.getHeaderFields().get(SET_COOKIE);
		if (values==null)
			throw new IOException("No set-cookie returned");
		for (String value : values) {
			String cs [] = value.split(";");
			for (int ci=0; ci<cs.length; ci++) 
			{
				String c = cs[ci].trim();
				int ex = c.indexOf("=");
				if (ex>=0) {
					String cn = c.substring(0, ex).trim();
					String cv = c.substring(ex+1).trim();
					if (!"expires".equals(cn.toLowerCase()) && !"path".equals(cn.toLowerCase())) {
						if (cookie.length()>0)
							cookie.append("; ");
						cookie.append(cn);
						cookie.append("=");
						cookie.append(cv);
					}

				}
			}
		}
		conn.getInputStream().close();
		if (cookie.length()>0) {
			conn = (HttpURLConnection) new URL(baseUrl+CONTINUE_SUFFIX).openConnection();
			conn.setRequestMethod("POST");
			//conn.setDoOutput(true);
			conn.addRequestProperty("Cookie", cookie.toString());
			conn.addRequestProperty("Content-Length", "0");
			conn.setDoOutput(true);
			conn.getOutputStream().close();
			if (conn.getResponseCode()!=200) {
				logger.warn("Continue POST failed ("+conn.getResponseCode()+")");
			}
			return cookie.toString();
		}
		throw new IOException("No cookie found");
	}
	

	static final String NOTTINGHAM_BASE_URL = "https://saturnweb.nottingham.ac.uk/Nottingham";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length!=2) {
			System.err.println("Usage: username password");
			System.exit(-1);
		}
		try {
			SaturnwebUtils.disableSslChecks();
			String cookie = login(NOTTINGHAM_BASE_URL, args[0], args[1]);
			System.out.println("Got cookie: "+cookie);
		}
		catch (Exception e) {
			logger.error("Error logging in", e);
		}
	}

}
