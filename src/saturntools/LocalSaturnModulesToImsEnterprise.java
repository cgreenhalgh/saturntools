/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.FileReader;
import java.util.Properties;

/** Try to munge module information from Saturn into an IMS Enterprise 1.01 file for import
 * to (e.g.) Moodle. Reads from local files (e.g. downloaded with DownloadSaturnModuleFiles).
 *  
 * @author cmg
 *
 */
public class LocalSaturnModulesToImsEnterprise {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModulesToImsEnterprise.class);
    /** default source name */
    static String source_name = "University of Nottigham Saturn";
    /** default target name */
    static String target_name = "VLE";
    /** default organisation */
    static String organisation_name = "University of Nottingham";
    /** property name constants */
    static String TEACH_PROPERTY_NAME = ".teach";
    /** property name constants */
    static String ENROL_PROPERTY_NAME = ".enrol";
    /** property name constants */
    static String BEGIN_PROPERTY_NAME = ".begin";
    /** property name constants */
    static String END_PROPERTY_NAME = ".end";
    /** semester names */
    static String SEMESTER_NAMES[] = new String[] { "autumn", "spring", "summer", "fullyear" };
	static SemesterDates semesterDates[] = new SemesterDates[SEMESTER_NAMES.length];
    /** teach dates for a semester */
    static class SemesterDates {
    	String teach_start, teach_end, enrol_start, enrol_end;
    }
    
	/** main */
	public static void main(String [] args) {
		try {
			if (args.length<3) {
				System.err.println("Usage: <semesterdate.properties> <modulepage.html> ...");
				System.exit(-1);
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//.getDateInstance(DateFormat.)
			readSemesterDates(args[0]);
			String date = dateFormat.format(new Date());
			String outfile = "enterprise-modules-"+date+".xml";
			System.out.println("Writing to "+outfile);
			PrintStream ps = new PrintStream(outfile, "UTF-8");
			writeIMSHeader(ps);
			writeIMSProperties(ps);
			for (int argi=1; argi<args.length; argi++) {
				String text = args[argi].substring(0, 6);
				String url = args[argi];
				ModuleInfo mi = null;
				try {
					mi = ModuleInfo.processModulePage(null, text, url);

					//ps.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing module page for "+text+" - "+url, e);
				}
				if (mi!=null) {
					writeModuleAsGroup(mi, ps);
				}
			}
			writeIMSTrailer(ps);
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	static void writeIMSHeader(PrintStream ps) {
//	writeIMSHeader(ps);
		ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		ps.println("<ENTERPRISE>");
	}
	static void writeIMSProperties(PrintStream ps) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//.getDateInstance(DateFormat.)
		//writeIMSProperties(ps);
		ps.println("  <PROPERTIES>");
		ps.println("    <DATASOURCE>"+source_name+"</DATASOURCE>");
		ps.println("    <TARGET>"+target_name+"</TARGET>");
		//optional/arbitrary: ps.println("    <TYPE>REFRESH</TYPE>");
		ps.println("    <DATETIME>"+dateFormat.format(new Date())+"</DATETIME>");
		ps.println("  </PROPERTIES>");
	}
	static void readSemesterDates(String filename) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//.getDateInstance(DateFormat.)
		System.out.println("Reading semester dates from "+filename);
		Properties semesters = new Properties();
		semesters.load(new FileInputStream(filename));
		// check properties
		for (int i=0; i<SEMESTER_NAMES.length; i++) {
			SemesterDates sd = new SemesterDates();
			semesterDates[i] = sd;
			for (int t=0; t<=1; t++) 
				for (int e=0; e<=1; e++) {
					String pname = SEMESTER_NAMES[i]+(t==1 ? TEACH_PROPERTY_NAME : ENROL_PROPERTY_NAME)+(e==1 ? END_PROPERTY_NAME : BEGIN_PROPERTY_NAME);
					String value = semesters.getProperty(pname);
					if (value==null) {
						System.err.println("Semester date property '"+pname+"' undefined in "+filename);
						System.exit(-2);
					}
					try {
						dateFormat.parse(value);
					}
					catch (Exception ee) {
						System.err.println("Semester date property '"+pname+"' not a valid date ('yyyy-MM-dd', e.g. '2009-08-25') in "+filename);
						System.exit(-3);
					}
					if (t==1 && e==1)
						sd.teach_end = value;
					else if (t==1)
						sd.teach_start = value;
					else if (e==1)
						sd.enrol_end = value;
					else
						sd.enrol_start = value;
				}
		}

	}
	static void writeIMSTrailer(PrintStream ps ) {
		//writeIMSTrailer();
		ps.println("</ENTERPRISE>");
		ps.close();
	}
	static void writeModuleAsGroup(ModuleInfo mi, PrintStream ps) {
		if (!mi.availability) {
			ps.println("  <!-- "+mi.modulecode+" not available "+mi.year+" -->");
			return;
		}
		ps.println("  <GROUP>");
		ps.println("    <SOURCEDID>");
		ps.println("      <SOURCE>"+source_name+"</SOURCE>");
		// course id is code plus year code
		ps.println("      <ID>"+mi.modulecode+"-"+mi.year+"</ID>");
		ps.println("    </SOURCEDID>");
		ps.println("    <DESCRIPTION>");
		// standard moodle plugin does short -> fullname; 
		// minted should do short -> shortname; long -> fullname; full -> summary
		ps.println("      <SHORT>"+mi.modulecode+" ("+mi.year+")</SHORT>");
		ps.println("      <LONG>"+mi.modulecode+" "+mi.moduletitle+" ("+mi.year+")</LONG>");
		// <FULL> - summary - not extracted as yet
		ps.println("      <FULL>"+escapeSummary(mi.summary)+"</FULL>");
		ps.println("    </DESCRIPTION>");
		// 1=add (default?!), 2=update, 3=delete - should be an optional field! (event-driven only)
		//ps.println("    <RECSTATUS>1</RECSTATUS>");
		ps.println("    <ORG>");
		ps.println("      <ORGNAME>"+organisation_name+"</ORGNAME>");
		// NB Mangle year into "organisational unit" as this maps to category and we want to 
		// separate years of courses
		if (mi.offeringschool!=null)
			ps.println("      <ORGUNIT>"+mi.offeringschool+" ("+mi.year+")</ORGUNIT>");
		else
			ps.println("      <ORGUNIT>"+mi.year+"</ORGUNIT>");
		ps.println("    </ORG>");
		SemesterDates sd = null;
		if (mi.semester==null) 
			logger.warn("Semester unspecified for "+mi.modulecode);
		else {
			int si = 0;
			String semester = mi.semester.toLowerCase().replace(" ", "");
			for (si=0; si<SEMESTER_NAMES.length; si++) 
				if (SEMESTER_NAMES[si].equals(semester)) {
					sd = semesterDates[si];
					break;
				}
			if (sd==null)
				logger.warn("Semester "+mi.semester+" not a standard value - unsupported");
		}
		if (sd!=null) {
			ps.println("    <TIMEFRAME>");
			ps.println("      <BEGIN>"+sd.teach_start+"</BEGIN>");
			ps.println("      <END>"+sd.teach_end+"</END>");
			ps.println("      <ADMINPERIOD>"+mi.semester+"</ADMINPERIOD>");
			ps.println("    </TIMEFRAME>");				
		}
		// allow enrolment in principle in VLE
		ps.println("    <ENROLLCONTROL>");
		ps.println("      <ENROLLALLOWED>1</ENROLLALLOWED>");
		ps.println("    </ENROLLCONTROL>");
		ps.println("    <EXTENSION>");
		// minted extension for moodle - course is not visible by default
		ps.println("      <VISIBLE>1</VISIBLE>");
		// enrolment dates - our own extension
		if (sd!=null) {
			ps.println("      <ENROLLCONTROL>");
			ps.println("        <ENROLLDATERANGE>");
			ps.println("          <BEGIN>"+sd.enrol_start+"</BEGIN>");
			ps.println("          <END>"+sd.enrol_end+"</END>");
			ps.println("        </ENROLLDATERANGE>");
			ps.println("      </ENROLLCONTROL>");
		}
		// more of our own extesions for moodle
		ps.println("      <GUESTACCESS>1</GUESTACCESS>");
		ps.println("    </EXTENSION>");
		ps.println("  </GROUP>");

	}
	/** escape summary in particular (compensate for badly formatted html */
	static String escapeSummary(String s) {
		String sl = s.toLowerCase();
		if (sl.contains("<li>") && !sl.contains("<ul>"))
			s = "<ul>"+s+"</ul>";
		else if (sl.contains("<ul>") && !sl.contains("</ul>"))
			s = s+"</ul>";
		s = "<div>"+s+"</div>";
		return escapeXml(s);
	}
	static String escapeXml(String s) {
		return "<![CDATA["+s+"]]>";
//		StringBuffer b = new StringBuffer();
//		for (int i=0; i<s.length();i++) {
//			char c= s.charAt(i);
//			if (Character.isISOControl(c))
//				continue;
//			switch (c) {
//			case '<':
//				b.append("&lt;");
//				break;
//			case '&':
//				b.append("&amp;");
//				break;
//			case '\'':
//				b.append("&apos;");
//				break;
//			default:
//				b.append(c);
//			}			
//		}
//		return b.toString();
	}
}
