/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

/** Try to dump module information from Saturn. In particular, history of who convened each module, when.
 * @author cmg
 *
 */
public class SaturnModuleHistoryReader {
	/** logger */
    static Logger logger = Logger.getLogger(SaturnModuleHistoryReader.class);
	/** HTML parsing constant */
	public static final String ELEMENT_A = "a";
	/** HTML parsing constant */
	public static final String ATTRIBUTE_HREF= "href";
	/** module page title */
	public static final String PAGE_TITLE_ELEMENT = "h2";
	/** table row element */
	public static final String TABLE_ROW_ELEMENT = "tr";
	/** table cell element */
	public static final String TABLE_CELL_ELEMENT = "td";
	/** year title */
	public static final String YEAR_ELEMENT = "h3";
	/** year text */
	public static final String YEAR_TEXT = "year";
	/** credits element */
	public static final String CREDITS_ELEMENT = "b";
	/** credits text */
	public static final String CREDITS_TEXT = "Total Credits";
	/** level element */
	public static final String LEVEL_ELEMENT = "b";
	/** level text */
	public static final String LEVEL_TEXT = "Level";
	/** convener element */
	public static final String CONVENOR_ELEMENT = "b";
	/** convener text */
	public static final String CONVENOR_TEXT = "Convenor";
	/** availability element */
	public static final String AVAILABILITY_ELEMENT = "b";
	/** availability text */
	public static final String AVAILABILITY_TEXT = "Availability";
	/** Semester element */
	public static final String SEMESTER_ELEMENT = "th";
	/** Semester text */
	public static final String SEMESTER_TEXT = "Semester";
	/** saturn CS index URL */
	public static final String SATURN_CS_INDEX_URL = "http://modulecatalogue.nottingham.ac.uk/nottingham/asp/FindModule.asp?org_id=000245";
	/** year IDs */
	public static final String [] year_ids = new String[] { "000109", "000108", "000107", "000106", "000105", "000104" };
	/** main */
	public static void main(String [] args) {
		try {
			String indexUrl = SATURN_CS_INDEX_URL;
			if (args.length>0)
				indexUrl = args[0];
			logger.info("Trying to read module list from "+indexUrl);
			String baseUrl = indexUrl.substring(0, indexUrl.lastIndexOf("/")+1);
			String indexPage = indexUrl.substring(indexUrl.lastIndexOf("/")+1);
			HtmlDocument index = new HtmlDocument(baseUrl, indexPage);
			//index.dump();
			// get all links
			Vector<HtmlDocument.Node> links = index.getElementNodes(index.getRootNode(), ELEMENT_A);
			logger.info("Found "+links.size()+" links");
			Enumeration<HtmlDocument.Node> li = links.elements();
			System.out.println("modulecode,saturntitle,year,availability,saturnconvenor,...");
			while(li.hasMoreElements()) {
				HtmlDocument.Node link = li.nextElement();
				String url = link.getAttributeValue(ATTRIBUTE_HREF);
				String text = link.getBodyText();
				if (url==null || url.length()==0)
				{
					logger.warn("Ignoring <a> with no href value");
					continue;
				}
				int year_ix = url.indexOf("year_id=");
				for (int i=0; i<year_ids.length; i++) {
					// year_id always 6 digits?!
					String year_url = (year_ix<0) ? url+"&year_id="+year_ids[i] : url.substring(0, year_ix)+"year_id="+year_ids[i]+url.substring(year_ix+14);
					ModuleInfo mi = null;
					try {
						mi = processModulePage(baseUrl, text, year_url);
						
						//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
					} catch (Exception e) {
						logger.error("Processing module page for "+text+" - "+url, e);
					}
					if (i == 0)
					{
					    if (mi == null)
					    {
						System.out.print("??????," + year_url);
					    }
					    else
						System.out.print(mi.modulecode + "," + mi.moduletitle);
					}
					System.out.print(","+(mi==null ? "-" : (!mi.availability ? "Suspended" : (mi.convenor.length()==0 ? "Unknown convenor" : mi.convenor))));

				}
				System.out.println();
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	static class ModuleInfo {
		String moduletitle;
		String modulecode;
		String year;
		int credits;
		boolean availability;
		String semester;
		String level;
		String convenor;
	}
	/** process module page */
	static ModuleInfo processModulePage(String baseUrl, String text, String url) throws java.io.IOException {
		ModuleInfo mi = new ModuleInfo();
		String pagePath = url;
		if (url.startsWith("http:"))
		{
			baseUrl = url.substring(0, url.lastIndexOf("/")+1);
			pagePath = url.substring(url.lastIndexOf("/")+1);
	
		}
		logger.info("Processing module "+text+" page "+url);
		HtmlDocument page = new HtmlDocument(baseUrl, pagePath);
		//page.dump();
		//System.exit(-2);
		// text from (first) H2 should be module code <ws> module title
		HtmlDocument.Node heading = page.getElementNode(page.getRootNode(), PAGE_TITLE_ELEMENT);
		if (heading==null)
			throw new IOException("Couldn't find heading element in page for "+text);
		String codeAndTitle = heading.getBodyText();
		int ix = codeAndTitle.indexOf(" ");
		if (ix<0)
			throw new IOException("Couldn't find code/title in heading "+codeAndTitle);
		String modulecode = codeAndTitle.substring(0, ix);
		String moduletitle = codeAndTitle.substring(ix+1);
		// trim any following markup etc.
		ix = moduletitle.indexOf("<");
		if (ix>=0)
			moduletitle = moduletitle.substring(0, ix);
		if (!modulecode.equals(text)) {
			logger.warn("Found module "+modulecode+" in page "+text);
		}
		mi.modulecode = modulecode;
		mi.moduletitle = moduletitle;
		logger.info("Module Code: "+modulecode);
		logger.info("Module title: "+moduletitle);
		
		// text from (first) H3 should be Year &nbsp;XX/XX
		HtmlDocument.Node yearNode = page.getElementNodeStartingWith(page.getRootNode(), YEAR_ELEMENT, YEAR_TEXT);
		if (yearNode==null)
			throw new IOException("Couldn't find year element in page for "+text);
		String yearText = replaceEntityRefs(yearNode.getBodyText().substring(YEAR_TEXT.length())).trim();
		logger.info("Year: "+yearText);
		mi.year = yearText;
		
		// credits is e.g. "<p><b>Total Credits:&nbsp;</b>10"
		HtmlDocument.Node creditsNode = page.getElementNodeStartingWith(page.getRootNode(), CREDITS_ELEMENT, CREDITS_TEXT);
		String creditsText = "";
		if (creditsNode==null) {
			logger.warn("Could not find credits for "+text);
		} else {
			creditsText = page.getSimpleContentFollowing(creditsNode).trim();
			logger.info("Credits: "+creditsText);
			try {
				mi.credits = Integer.parseInt(creditsText);
			}
			catch (NumberFormatException nfe) {
				logger.error("Invalid credits format: "+creditsText, nfe);
			}
		}
		
		// level, e.g. "<p><b>Level:&nbsp;</b>Level 0"
		HtmlDocument.Node levelNode = page.getElementNodeStartingWith(page.getRootNode(), LEVEL_ELEMENT, LEVEL_TEXT);
		String levelText = "";
		if (levelNode==null) {
			logger.warn("Could not find level for "+text);
		} else {
			levelText = page.getSimpleContentFollowing(levelNode).trim();
			if (levelText.toLowerCase().startsWith("level"))
				levelText = levelText.substring("level".length()).trim();
			logger.info("Level: "+levelText);
		}
		mi.level = levelText;

		// credits is e.g. "<p><b>Convenor:&nbsp;</b><BR> Dr G Hutton<BR><p>"
		HtmlDocument.Node convenorNode = page.getElementNodeStartingWith(page.getRootNode(), CONVENOR_ELEMENT, CONVENOR_TEXT);
		String convenorText = "";
		if (convenorNode==null) {
			logger.warn("Could not find convenor for "+text);
		} else {
			convenorText = page.getSimpleContentFollowing(convenorNode, true).trim();
			logger.info("Convenor: "+convenorText);
		}
		mi.convenor = convenorText;
		
		// semester: <TR><TH>Semester</TH><TH>Assessment</TH></TR><TR><TD>Autumn&nbsp;</TD>
		HtmlDocument.Node semesterNode = page.getElementNodeStartingWith(page.getRootNode(), SEMESTER_ELEMENT, SEMESTER_TEXT);
		String semesterText = "";
		if (semesterNode==null) {
			logger.warn("Could not find semester for "+text);
		} else {
			// up two levels to TABLE
			HtmlDocument.Node teachingTable = semesterNode.parent.parent;
			if (teachingTable==null)
				logger.warn("Could not find table parent of semester node");
			else {
				Vector<HtmlDocument.Node> rows = teachingTable.getChildElements(TABLE_ROW_ELEMENT);
				if (rows.size()<2) {
					logger.warn("Could not find two rows in semester table ("+rows.size()+")");
				} else 
				{
					Vector<HtmlDocument.Node> cells = rows.get(1).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<1) {
						logger.warn("Could not find one cell in second row of semester table");
					} else {
						// down to second row, first element
						semesterText = replaceEntityRefs(cells.get(0).getBodyText()).trim();
						logger.info("Semester: "+semesterText);
						
					}
				}
			}
		}
		mi.semester= semesterText;
		
		// availability: <P><B>Availability:&nbsp; </B><I><FONT color='red'>Not Available in the 2008/09 academic session.</FONT></I>
		HtmlDocument.Node availabilityNode = page.getElementNodeStartingWith(page.getRootNode(), AVAILABILITY_ELEMENT, AVAILABILITY_TEXT);
		String availabilityText = "";
		if (availabilityNode==null) {
			logger.debug("Could not find availability for "+text);
		} else {
			availabilityText = page.getSimpleContentFollowing(availabilityNode, true).trim();
			logger.info("Availability: "+availabilityText);
		}
		mi.availability = !availabilityText.toLowerCase().startsWith("not") && !availabilityText.toLowerCase().contains("dormant");

		// TODO module web links
		// <B>Module Web Links:</B><BR><TABLE border='0' CELLSPACING ='3' CELLPADDING='2' width='80%' ><TR><TD>&nbsp;&nbsp;&nbsp;</TD><TD><LI><A target='_blank' href='https://www.nottingham.ac.uk/is/gateway/readinglists/local/displaylist?module=G5AHOC'>Reading List</A></LI></TD></TR></TABLE>
		
		// output
		//System.out.println("modulecode,saturntitle,saturnstatus,saturnsemester,saturnlevel,saturnconvenor");
		//System.out.println(modulecode+","+moduletitle+","+(availabilityText.toLowerCase().startsWith("not") ? "Suspended" : "")+","+semesterText+","+levelText+","+convenorText);

		// TODO
		return mi;
	}
	/** replace entity refs */
	static String replaceEntityRefs(String s) {
		StringBuffer b  = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c= s.charAt(i);
			if (c=='&') {
				int ix = s.indexOf(";", i);
				if (ix>=0) {
					String entity = s.substring(i+1, ix);
					if ("nbsp".equals(entity))
						b.append(" ");
					else if ("lt".equals(entity))
						b.append("<");
					else if ("gt".equals(entity))
						b.append(">");
					else if ("quot".equals(entity))
						b.append("\"");
					else if ("amp".equals(entity))
						b.append("amp");
					else if ("apos".equals(entity))
						b.append("'");
					else
						b.append("?");
					i = ix;
					continue;
				}
			}
			b.append(c);
		}
		return b.toString();
	}
}
