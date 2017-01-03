/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;
import java.util.Vector;
import java.util.Enumeration;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.PrintStream;
import java.util.Properties;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

/** Try to munge module convenor information from Saturn into an IMS Enterprise 1.01 file for import
 * to (e.g.) Moodle. Reads from local files (e.g. downloaded with DownloadSaturnModuleFiles) and uses Uni
 * LDAP to try to look up people (But note that this is clearly different data, e.g. my title is different
 * in saturn vs LDAP).
 *  
 * @author cmg
 *
 */
public class LocalSaturnModuleConvenorsToImsEnterprise {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnModuleConvenorsToImsEnterprise.class);
    /** default source name */
    static String source_name = "University of Nottigham Saturn";
    /** default target name */
    static String target_name = "VLE";
    /** default organisation */
    static String organisation_name = "University of Nottingham";
    /** possible titles */
    static String titles [] = new String[] { "Dr", "Professor", "Mr", "Mrs", "Ms", "Miss" };
	/** main */
	public static void main(String [] args) {
		try {
			if (args.length<2) {
				System.err.println("Usage: <semesterdate.properties> <customuserids.properties> <modulepage.html> ...");
				System.exit(-1);
			}
			LocalSaturnModulesToImsEnterprise.readSemesterDates(args[0]);
			System.out.println("Read custom userids from "+args[1]);
			Properties userids = new Properties();
			userids.load(new FileInputStream(args[1]));
			for (Object userid : userids.keySet()) {
				System.out.println("Custom userid: "+userid+" = "+userids.getProperty(userid.toString()));
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//.getDateInstance(DateFormat.)
			String date = dateFormat.format(new Date());
			String outfile = "enterprise-module-convenors-"+date+".xml";
			System.out.println("Writing to "+outfile);
			PrintStream ps = new PrintStream(outfile, "UTF-8");
			LocalSaturnModulesToImsEnterprise.writeIMSHeader(ps);
			LocalSaturnModulesToImsEnterprise.writeIMSProperties(ps);
			TreeSet<String> convenorNames = new TreeSet<String>();
			HashMap<String,TreeSet<String>> moduleConvenors = new HashMap<String,TreeSet<String>>();
			HashMap<String,String> convenorSourceids = new HashMap<String,String>();
			for (int argi=2; argi<args.length; argi++) {
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
					if (!mi.availability) {
						ps.println("  <!-- "+mi.modulecode+" not available "+mi.year+" -->");
						continue;
					}
					LocalSaturnModulesToImsEnterprise.writeModuleAsGroup(mi, ps);
					
					String convenors = splitModuleConvenors(mi.convenor);
					
					ps.println("  <!-- "+convenors+" (was "+mi.convenor+") -->");
					TreeSet<String> mc = new TreeSet<String>();
					String cs [] = convenors.split(";");
					for (int i=0; i<cs.length; i++) {
						String c = cs[i].trim();
						if (c.length()==0)
							continue;
						mc.add(c);
					}
					convenorNames.addAll(mc);
					moduleConvenors.put(mi.modulecode+"-"+mi.year, mc);
				}
			}
			// convenors...
			for (String convenorName : convenorNames) {
				String title = null;
				String firstInitial = null;
				String middleInitials = null;
				String familyName = null;
				int ix1 = convenorName.indexOf(" ");
				if (ix1<0) {
					logger.warn("Cannot handle name format "+convenorName);
					familyName = convenorName;
				}
				else {
					title = convenorName.substring(0, ix1);
					ix1++;
					while(ix1<convenorName.length()) {
						int ix2 = convenorName.indexOf(" ", ix1);
						if (ix2==ix1+1) {
							// initial
							if (firstInitial==null)
								firstInitial = convenorName.substring(ix1, ix2);
							else if (middleInitials==null)								
								middleInitials = convenorName.substring(ix1, ix2);
							else
								middleInitials = middleInitials+convenorName.substring(ix1, ix2);
							ix1=ix2+1;
						}
						else
							break;	
					}
					if (ix1<convenorName.length()) {
						familyName = convenorName.substring(ix1);
						// remove whitespace?!
					}
					else
						logger.warn("Cannot handle name format "+convenorName+" - no family name?!");
						
				}
				// attempt lookup in LDAP
				System.err.println("Look up "+firstInitial+" "+familyName+"...");
				NamingEnumeration<SearchResult> results = LdapClient.findPerson(firstInitial, familyName);
				SearchResult bestResult = null;
				String bestUid = null;
				boolean uncertain = false;
				while (results.hasMore()) {
					SearchResult result = results.next();
					Attribute uido = result.getAttributes().get("uid");
					if (uido==null || uido.get()==null)
						continue;
					String uid = uido.get().toString().toLowerCase();
					System.out.println("Found "+uid);
					if (bestResult==null) {
						bestResult = result;
						bestUid = uid;
					}
					else {
						// default to "PS" for CS or "IT" for IT
						if ((uid.startsWith("ps") || uid.startsWith("it")) && !bestUid.startsWith("ps") && !bestUid.startsWith("it")) {
							bestResult = result;
							bestUid = uid;
							uncertain = false;
						}
						else if (!uid.startsWith("ps") && !uid.startsWith("it") && (bestUid.startsWith("ps")|| bestUid.startsWith("it")))
							; // no op
						else {
							System.err.println("Uncertain: "+uid+" vs "+bestUid);
							uncertain = true;
						}
					}
				}
				// try custom 
				for (Object userid : userids.keySet()) {
					String name = userids.getProperty(userid.toString());
					if (convenorName.equals(name)) {
						if (bestResult!=null) {
							if (!userid.equals(bestUid)) {
								System.err.println("Warning: custom userids override for "+convenorName+" from "+bestUid+" to "+userid);
								bestUid = userid.toString();
								ps.println("  <!-- Note: custom userids override for "+convenorName+" from "+bestUid+" to "+userid+" -->");
								uncertain = false;
								bestResult = null;
							}
							else if (uncertain) {
								ps.println("  <!-- Note: custom userids resolved uncertainty for "+convenorName+"  as "+userid+" -->");						
								uncertain = false;
							}
							else
								System.err.println("Note: custom userid "+userid+":"+convenorName+" not required");
						}
						else {
							bestUid = userid.toString();
							ps.println("  <!-- Note: custom userids used "+userid+":"+convenorName+" -->");
						}
					}
				}
				if (bestUid==null) {
					System.err.println("*** Unknown identity for "+convenorName);
					ps.println("  <!-- WARNING: Unknown convenor "+convenorName+" -->");
				}
				else if (uncertain) {
					System.err.println("*** Uncertain identity for "+convenorName);
					ps.println("  <!-- WARNING: Uncertain convenor "+convenorName+" -->");
				} else {
					//Attribute uido = bestResult.getAttributes().get("uid");
					String uid = bestUid; //uido.get().toString().toLowerCase();
					convenorSourceids.put(convenorName, uid);
					ps.println("  <PERSON>");
					ps.println("    <SOURCEDID>");
					ps.println("      <SOURCE>"+source_name+"</SOURCE>");
					// uid
					ps.println("      <ID>"+uid+"</ID>");
					ps.println("    </SOURCEDID>");
					ps.println("    <USERID>"+uid+"</USERID>");
					ps.println("    <NAME>");
					ps.println("      <FN>"+convenorName+"</FN>");
					ps.println("      <N>");
					if (bestResult!=null && bestResult.getAttributes().get("givenName")!=null && bestResult.getAttributes().get("givenName").get()!=null)
						ps.println("        <GIVEN>"+bestResult.getAttributes().get("givenName").get()+"</GIVEN>");
					else if (firstInitial!=null)
						ps.println("        <GIVEN>"+firstInitial+"</GIVEN>");					
					if (bestResult!=null && bestResult.getAttributes().get("sn")!=null && bestResult.getAttributes().get("sn").get()!=null)
						ps.println("        <FAMILY>"+bestResult.getAttributes().get("sn").get()+"</FAMILY>");
					else if (familyName!=null)
						ps.println("        <FAMILY>"+familyName+"</FAMILY>");
					ps.println("      </N>");
					ps.println("    </NAME>");
					if (bestResult!=null && bestResult.getAttributes().get("mail")!=null && bestResult.getAttributes().get("mail").get()!=null)
						ps.println("      <EMAIL>"+bestResult.getAttributes().get("mail").get()+"</EMAIL>");
				
					ps.println("  </PERSON>");
				}
			}
			// membership(s)
			for (Map.Entry<String, TreeSet<String>> module : moduleConvenors.entrySet()) {
				String moduleid = module.getKey();
				ps.println("  <MEMBERSHIP>");
				// group
				ps.println("    <SOURCEDID>");
				ps.println("      <SOURCE>"+source_name+"</SOURCE>");
				ps.println("      <ID>"+moduleid+"</ID>");
				ps.println("    </SOURCEDID>");
				TreeSet<String> convenors = module.getValue();
				for (String convenor : convenors) {
					String convenorSourceid = convenorSourceids.get(convenor);
					if (convenorSourceid==null) {
						ps.println("  <!-- WARNING: Could not add convenor "+convenor+" to module "+moduleid+" - not found/unique -->");
					}
					else {
						// member - convenor
						ps.println("    <MEMBER>");
						ps.println("      <SOURCEDID>");
						ps.println("        <SOURCE>"+source_name+"</SOURCE>");
						ps.println("        <!-- "+convenor+" -->");
						ps.println("        <ID>"+convenorSourceid+"</ID>");
						ps.println("      </SOURCEDID>");
						// instructor (02) and content developer (03) (administrator? 07)
						ps.println("      <ROLE roletype=\"02\">");
						// active
						ps.println("        <STATUS>1</STATUS>");
						// RECSTATUS? - added
						ps.println("      </ROLE>");
						ps.println("      <ROLE roletype=\"03\">");
						// active 
						ps.println("        <STATUS>1</STATUS>");
						// RECSTATUS? - added
						ps.println("      </ROLE>");
						ps.println("    </MEMBER>");
					}
				}
				ps.println("  </MEMBERSHIP>");
			}
			LocalSaturnModulesToImsEnterprise.writeIMSTrailer(ps);
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	public static String splitModuleConvenors(String convenors) {
		convenors = convenors.trim();
		//String convenors = mi.convenor;
		for (int i=0; i<titles.length; i++) {
			// split at possible titles
			int ix = 1;
			while(ix>0 && ix<convenors.length()) {
				ix = convenors.indexOf(titles[i]+" ", ix);
				if (ix<0)
					break;
				convenors = convenors.substring(0,ix).trim()+";"+convenors.substring(ix).trim();
				ix = convenors.substring(0,ix).trim().length()+1+titles[i].length();
			}
		}
		return convenors;
	}
	public static String splitModuleConvenorsNoTitles(String convenors) {
		convenors = convenors.trim();
		//String convenors = mi.convenor;
			// split at possible titles
		String orig = convenors;
		for (int i=0; i<titles.length; i++) {
			int ix = 0;
			while(ix>=0 && ix<convenors.length()) {
				ix = convenors.indexOf(titles[i]+" ", ix);
				if (ix<0)
					break;
				convenors = convenors.substring(0,ix).trim()+";"+convenors.substring(ix+titles[i].length()).trim();
				ix = convenors.substring(0,ix).trim().length()+1;
			}
			if (convenors.indexOf(titles[i])>=0) 
				System.out.println("Warning: Still found "+titles[i]+" in "+convenors+" (started as "+orig+")");
			if (convenors.startsWith(";"))
				convenors = convenors.substring(1);
		}
		return convenors;
	}
	static String escapeXml(String s) {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<s.length();i++) {
			char c= s.charAt(i);
			if (Character.isISOControl(c))
				continue;
			switch (c) {
			case '<':
				b.append("&lt;");
				break;
			case '&':
				b.append("&amp;");
				break;
			case '\'':
				b.append("&apos;");
				break;
			default:
				b.append(c);
			}			
		}
		return s.toString();
	}
}
