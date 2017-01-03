/**
 * 
 */
package saturntools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.TreeSet;

import saturntools.LocalSaturnModulesToImsEnterprise.SemesterDates;


/**
 * @author cmg
 *
 */
public class SaturnStudentExportToImsEnterprise {

	/** Read file from Saturn2 Student Exports, School Modules 2 option, and IMS Enterprise 
	 * person & enrolment information.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=1) {
			System.err.println("Usage: java SaturnStudentExportToImsEnterprise <saturn-modules-2-export-file.txt>");
			System.exit(-1);
		}
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//.getDateInstance(DateFormat.)
			String date = dateFormat.format(new Date());
			String outfile = "enterprise-students-"+date+".xml";
			System.out.println("Writing to "+outfile);
			PrintStream ps = new PrintStream(outfile, "UTF-8");
			LocalSaturnModulesToImsEnterprise.writeIMSHeader(ps);
			LocalSaturnModulesToImsEnterprise.writeIMSProperties(ps);
			
			System.out.println("# Reading "+args[0]+", "+(new Date()));
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			String header = br.readLine();
			if (header==null) {
				System.err.println("Could not read header line from "+args[0]);
				System.exit(-1);
			}

			// exports-2
			// "Module Mnem","Year","Module Title","Student ID","Surname","Forenames","Title","UCAS Code","Year of Study","Semester","Credit","Resit","e-Mail"
			// "G50WEB","09/10 ","Introduction to the Web","4070478","Avenell","David","Mr","C100","3","Spring","10 ","N","plybda@nottingham.ac.uk"
			// (Note: export-2 shows Year 09/10 resit Y where export-3 shows 08/09 for resit Y.)
			
			// Exports-3 & school list - no email => no user id (could try LDAP?!)
			
			// a representative StudentInfo  for each student (id)
			HashMap<String,StudentInfo> students = new HashMap<String,StudentInfo>();
			HashMap<String,TreeSet<String>> modules = new HashMap<String,TreeSet<String>>();
			HashMap<String,StudentInfo> moduleinfos = new HashMap<String,StudentInfo>();
			
			StudentInfo.init(header);
			int count = 0;
			while (true) {
				String line = br.readLine();
				if (line==null)
					break;
				count++;
				// break in quote?!
				while (!line.trim().endsWith("\"")) {
					String continuation = br.readLine();
					if (continuation==null)
						break;
					line = line+continuation;
				}
				StudentInfo info = StudentInfo.parse(line);
				if (!students.containsKey(info.studentid)) {
					students.put(info.studentid, info);
				}
				String moduleid = info.getModuleSourceid();
				if (!moduleinfos.containsKey(moduleid))
					moduleinfos.put(moduleid, info);
				TreeSet<String> modulestudents = modules.get(moduleid);
				if (modulestudents==null)
				{
					modulestudents = new TreeSet<String>();
					modules.put(moduleid, modulestudents);
				}
				modulestudents.add(info.studentid);
			}
			System.out.println("# read "+count+" entries in total");
			// modules (short form) 
			// students...
			TreeSet<String> moduleids = new TreeSet<String>();
			moduleids.addAll(moduleinfos.keySet());
			for (String moduleid : moduleids) {
				StudentInfo info = moduleinfos.get(moduleid);
				ps.println("  <!-- this is just a short version from student export, not module catalogue -->");
				ps.println("  <GROUP>");
				ps.println("    <SOURCEDID>");
				ps.println("      <SOURCE>"+LocalSaturnModulesToImsEnterprise.source_name+"</SOURCE>");
				// course id is code plus year code
				ps.println("      <ID>"+info.modulecode+"-"+info.moduleyear+"</ID>");
				ps.println("    </SOURCEDID>");
				ps.println("    <DESCRIPTION>");
				// standard moodle plugin does short -> fullname; 
				// minted should do short -> shortname; long -> fullname; full -> summary
				ps.println("      <SHORT>"+info.modulecode+" ("+info.moduleyear+")</SHORT>");
				ps.println("      <LONG>"+info.modulecode+" "+info.moduletitle+" ("+info.moduleyear+")</LONG>");
				ps.println("    </DESCRIPTION>");
				// 1=add (default?!), 2=update, 3=delete - should be an optional field! (event-driven only)
				//ps.println("    <RECSTATUS>1</RECSTATUS>");
				ps.println("    <ORG>");
				ps.println("      <ORGNAME>"+LocalSaturnModulesToImsEnterprise.organisation_name+"</ORGNAME>");
				ps.println("    </ORG>");
				// allow enrolment in principle in VLE
				ps.println("    <ENROLLCONTROL>");
				ps.println("      <ENROLLALLOWED>1</ENROLLALLOWED>");
				ps.println("    </ENROLLCONTROL>");
				ps.println("    <EXTENSION>");
				// minted extension for moodle - course is not visible by default
				ps.println("      <VISIBLE>1</VISIBLE>");
				// more of our own extesions for moodle
				ps.println("      <GUESTACCESS>1</GUESTACCESS>");
				ps.println("    </EXTENSION>");
				ps.println("  </GROUP>");
			}
			// students...
			TreeSet<String> studentids = new TreeSet<String>();
			studentids.addAll(students.keySet());
			for (String studentid : studentids) {
				StudentInfo info = students.get(studentid);
				String uid = null;
				int ix = info.email.indexOf("@");
				if (ix<=0) {
					ps.println("    <!-- could not find @ in email "+info.email+" for student "+studentid+" -->");
					System.err.println("Error: no @ in email "+info.email+" for student "+studentid+": "+info.line);
					//continue;
				} 
				else {
					uid = info.email.substring(0,ix);
					if (!ALLOWED_EMAIL_DOMAIN.equals(info.email.substring(ix+1))) {
						ps.println("    <!-- Not allowed email domain '"+ALLOWED_EMAIL_DOMAIN+"' in email "+info.email+" for student "+studentid+" -->");
						System.err.println("Error: Not allowed email domain '"+ALLOWED_EMAIL_DOMAIN+"' in email "+info.email+" for student "+studentid+": "+info.line);
						uid = null;
						//continue;
					}
					else if (uid.contains(".")) {
						ps.println("    <!-- Found '.' in email "+info.email+" for student "+studentid+" -->");
						System.err.println("Error: found '.' in email "+info.email+" for student "+studentid+": "+info.line);
						uid = null;
						//continue;
					}
					else {
						// ok
					}
				}
				// first as representative of student
				ps.println("  <PERSON>");
				ps.println("    <SOURCEDID>");
				ps.println("      <SOURCE>"+LocalSaturnModulesToImsEnterprise.source_name+"</SOURCE>");
				// uid
				ps.println("      <ID>"+info.studentid+"</ID>");
				ps.println("    </SOURCEDID>");
				if (uid!=null)
					ps.println("    <USERID>"+uid+"</USERID>");
				ps.println("    <NAME>");
				ps.println("      <FN>"+info.givenname+" "+info.surname+"</FN>");
				ps.println("      <N>");
				ps.println("        <GIVEN>"+info.givenname+"</GIVEN>");
				ps.println("        <FAMILY>"+info.surname+"</FAMILY>");
				ps.println("      </N>");
				ps.println("    </NAME>");
				ps.println("    <EMAIL>"+info.email+"</EMAIL>");
				ps.println("    <EXTENSION>");
				ps.println("      <RESIT>"+(info.resit.toLowerCase().startsWith("y") ? "1" : "0")+"</RESIT>");
				ps.println("    </EXTENSION>");
				ps.println("  </PERSON>");
			}
			// module enrolments...
			// membership(s)
			for (Map.Entry<String, TreeSet<String>> module : modules.entrySet()) {
				String moduleid = module.getKey();
				ps.println("  <MEMBERSHIP>");
				// group
				ps.println("    <SOURCEDID>");
				ps.println("      <SOURCE>"+LocalSaturnModulesToImsEnterprise.source_name+"</SOURCE>");
				ps.println("      <ID>"+moduleid+"</ID>");
				ps.println("    </SOURCEDID>");
				TreeSet<String> modulestudentids = module.getValue();
				for (String studentid : modulestudentids) {
					// member - student
					ps.println("    <MEMBER>");
					ps.println("      <SOURCEDID>");
					ps.println("        <SOURCE>"+LocalSaturnModulesToImsEnterprise.source_name+"</SOURCE>");
					ps.println("        <ID>"+studentid+"</ID>");
					ps.println("      </SOURCEDID>");
					// learner (01) 
					ps.println("      <ROLE roletype=\"01\">");
					// active
					ps.println("        <STATUS>1</STATUS>");
					// RECSTATUS? - added
					ps.println("      </ROLE>");
					ps.println("    </MEMBER>");
				}
				ps.println("  </MEMBERSHIP>");
			}
			LocalSaturnModulesToImsEnterprise.writeIMSTrailer(ps);
			
			System.err.println("Done");
			System.exit(0);
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}
	/** allowed email */
	static final String ALLOWED_EMAIL_DOMAIN = "nottingham.ac.uk";
	/** header constant */
	static final String MODULE_CODE = "Module Mnem";
	/** header constant */
	static final String MODULE_TITLE = "Module Title";
	/** header constant */
	static final String MODULE_YEAR = "Year";
	/** header constant */
	static final String STUDENT_ID = "Student ID";
	/** header constant */
	static final String STUDENT_SURNAME = "Surname";
	/** header constant */
	static final String STUDENT_GIVEN_NAMES = "Forenames";
	/** header constant */
	static final String STUDENT_EMAIL = "e-Mail";
	/** header constant */
	static final String STUDENT_RESIT = "Resit";
	//"Module Mnem","Year","Module Title","Student ID","Surname","Forenames","Title","UCAS Code","Year of Study","Semester","Credit","Resit","e-Mail"
	static class StudentInfo {
		String line;
		String modulecode;
		String moduletitle;
		String moduleyear;
		String studentid;
		String surname;
		String givenname;
		String email;
		String resit;
		static int indexModulecode, indexModuletitle, indexModuleyear, indexStudentid, indexSurname, indexGivenname, indexEmail, indexResit;
		static void init(String header) throws java.io.IOException {
			System.out.println("Header: "+header);
			Vector<String> headers = CsvUtils.parseCsvLine(header);
			for (int i=0; i<headers.size(); i++)  
				System.out.print("  '"+headers.get(i)+"'");
			indexModulecode = headers.indexOf(MODULE_CODE);
			indexModuletitle = headers.indexOf(MODULE_TITLE);
			indexModuleyear = headers.indexOf(MODULE_YEAR);
			indexStudentid = headers.indexOf(STUDENT_ID);
			indexSurname = headers.indexOf(STUDENT_SURNAME);
			indexGivenname = headers.indexOf(STUDENT_GIVEN_NAMES);
			indexEmail = headers.indexOf(STUDENT_EMAIL);
			indexResit = headers.indexOf(STUDENT_RESIT);
			System.out.println("Indexes: "+indexModulecode+","+indexModuletitle+","+indexModuleyear+","+indexStudentid+","+indexSurname+","+indexGivenname+","+indexEmail+","+indexResit);
			if (indexModulecode<0 || indexModuletitle<0 || indexModuleyear<0 || indexStudentid<0 || indexSurname<0 || indexGivenname<0 || indexEmail<0 || indexResit<0) 
				throw new java.io.IOException("Did not find expected headers in input file - are you sure it is a module-2 export?!");
		}
		static StudentInfo parse(String line) throws java.io.IOException {
			Vector<String> values = CsvUtils.parseCsvLine(line);
			StudentInfo self = new StudentInfo();
			self.line = line;
			try {
				self.modulecode = values.elementAt(indexModulecode).trim();
				self.moduletitle = values.elementAt(indexModuletitle).trim();
				self.moduleyear = values.elementAt(indexModuleyear).trim();
				self.studentid = values.elementAt(indexStudentid).trim();
				self.surname = values.elementAt(indexSurname).trim();
				self.givenname = values.elementAt(indexGivenname).trim();
				self.email = values.elementAt(indexEmail).trim();
				self.resit = values.elementAt(indexResit).trim();
				//System.out.println("Info: "+self.modulecode+","+self.moduleyear+","+self.studentid+","+self.surname+","+self.givenname+","+self.email+","+self.resit);
				return self;
			}
			catch (Exception e) {
				throw new java.io.IOException("Not enough fields in line: "+line);
			}
		}
		String getModuleSourceid() {
			return modulecode+"-"+moduleyear;
		}
	}
}
