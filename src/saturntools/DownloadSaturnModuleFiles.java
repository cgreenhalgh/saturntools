/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/** Cache files from saturn.
 * @author cmg
 *
 */
public class DownloadSaturnModuleFiles {
	/** logger */
    static Logger logger = Logger.getLogger(DownloadSaturnModuleFiles.class);
	/** HTML parsing constant */
	public static final String ELEMENT_A = "a";
	/** HTML parsing constant */
	public static final String ATTRIBUTE_HREF= "href";
	/** saturn CS index URL */
	public static final String SATURN_CS_INDEX_URL = "http://modulecatalogue.nottingham.ac.uk/Nottingham/asp/FindModule.asp?org_id=000245";
	// Malaysia, 002006
	public static final String UNMC_CS_INDEX_URL = "http://modulecatalogue.nottingham.ac.uk/Malaysia/asp/FindModule.asp?org_id=002006";
	// Ningbo, 003010
	public static final String UNNC_CS_INDEX_URL = "http://modulecatalogue.nottingham.ac.uk/Ningbo/asp/FindModule.asp?org_id=003010";
	public static final String [] CAMPUS_IDS = new String[] { "UK", "UNMC", "UNNC" };
	public static final String [] CAMPUS_INDEX_URLS = new String[] { SATURN_CS_INDEX_URL, UNMC_CS_INDEX_URL, UNNC_CS_INDEX_URL };
	/** year IDs */
	public static final String [] DEFAULT_YEAR_IDS = new String[] { "000112", "000111", "000110", "000109" , "000108", "000107", "000106", "000105", "000104" };
	/** main */
	public static void main(String [] args) {
		try {
			System.err.println("Usage: [yearcode ...]\ne.g. 000110 = 2010/11");
			for (int ci=0; ci<CAMPUS_INDEX_URLS.length; ci++) {
				String indexUrl = CAMPUS_INDEX_URLS[ci];
				String campus = CAMPUS_IDS[ci];

				String year_ids[] = DEFAULT_YEAR_IDS;
				if (args.length>0) {
					//indexUrl = args[0];
					year_ids = args;
				}
				logger.info("Trying to read module list for "+campus+" from "+indexUrl);
				String baseUrl = indexUrl.substring(0, indexUrl.lastIndexOf("/")+1);
				String indexPage = indexUrl.substring(indexUrl.lastIndexOf("/")+1);
				for (int i=0; i<year_ids.length; i++) {
					HtmlDocument index = new HtmlDocument(baseUrl, indexPage+"&year_id="+year_ids[i]);
					//index.dump();
					// get all links
					Vector<HtmlDocument.Node> links = index.getElementNodes(index.getRootNode(), ELEMENT_A);
					logger.info("Found "+links.size()+" links");
					Enumeration<HtmlDocument.Node> li = links.elements();
					while(li.hasMoreElements()) {
						HtmlDocument.Node link = li.nextElement();
						String url = link.getAttributeValue(ATTRIBUTE_HREF);
						String text = link.getBodyText();
						if (url==null || url.length()==0)
						{
							logger.warn("Ignoring <a> with no href value");
							continue;
						}
						if (text==null || text.length()!=6)
						{
							logger.warn("Ignoring <a> with non-module-code text ("+text+")");
							continue;
						}
						// year_id always 6 digits?!
						int year_ix = url.indexOf("year_id=");
						String year_url = (year_ix<0) ? url+"&year_id="+year_ids[i] : url.substring(0, year_ix)+"year_id="+year_ids[i]+url.substring(year_ix+14);
						try {
							logger.info("Download "+text+" from "+year_url);
							//mi = processModulePage(baseUrl, text, year_url);
							downloadFile(baseUrl+year_url, text+"_"+year_ids[i]+"_"+campus+".html");

							//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
						} catch (Exception e) {
							logger.error("Processing module page for "+text+" - "+url, e);
						}
					}
					System.out.println();
				}
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	static void downloadFile(String urlString, String filename)  throws MalformedURLException, IOException {
		logger.info("Download "+urlString+" to "+filename);
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
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
	}
}
