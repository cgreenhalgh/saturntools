/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import saturntools.CourseInfo.Outcome;
import saturntools.CourseInfo.OutcomeSection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.LinkedList;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.util.TreeSet;

/** Info about Saturn Module entr
 * @author cmg
 *
 */
public class ModuleInfo {
	public static enum Status { unknown, available, suspended, dormant };
	String moduletitle;
	String modulecode;
	String year;
	int credits;
	boolean availability;
	Status status;
	String semester;
	String level;
	String offeringschoool;
	String convenor;
	float exampercentage;
	int assessments;
	String examrequirements = "";
	float examdurationhours;
	String offeringschool;
	String summary;
	public static enum GroupRole { unknown, core, branch, leaf, convergence, project, other, foundation };
	String newgroup;
	GroupRole grouprole = GroupRole.unknown;
	String groupname;
	TreeSet<String> prerequisitecodes = new TreeSet<String> ();
	String prerequisitestext;
	TreeSet<String> corequisitecodes  = new TreeSet<String> ();
	String corequisitestext;
	TreeSet<String> allrequisitecodes = new TreeSet<String> ();
	String targetstudents;
	String educationaims;
	String learningoutcomes;
	public static class AssessmentInfo {
		String type;
		String weight;
		String requirements;
	}
	LinkedList<AssessmentInfo> assessmentInfos = new LinkedList<AssessmentInfo>();
	String lastupdated;
	String filemodified;
	CourseInfo.OutcomeSection outcomeSections[];

	/** cons */
	ModuleInfo() {
		outcomeSections = new CourseInfo.OutcomeSection[CourseInfo.NUM_OUTCOME_SECTIONS];
		for (int i=0; i<CourseInfo.NUM_OUTCOME_SECTIONS; i++) {
			outcomeSections[i] = new OutcomeSection();
			outcomeSections[i].outcomes = new Vector<Outcome>();
		}
	}
	
	/** logger */
    static Logger logger = Logger.getLogger(ModuleInfo.class);
	/** HTML parsing constant */
	public static final String ELEMENT_A = "a";
	/** HTML parsing constant */
	public static final String ATTRIBUTE_HREF= "href";
	/** module page title */
	public static final String PAGE_TITLE_ELEMENT = "h2";
	/** module page title */
	public static final String MAINT_PAGE_TITLE_ELEMENT = "h3";
	/** module page title - the module code/title in particular */
	public static final String MAINT_PAGE_TITLE_PREFIX = "Module Submission Document for ";
	public static final String MAINT_PAGE_TITLE_ELEMENT2 = "<BR>";
	/** table row element */
	public static final String TABLE_ROW_ELEMENT = "tr";
	/** table cell element */
	public static final String TABLE_CELL_ELEMENT = "td";
	/** table cell element */
	public static final String TABLE_ELEMENT = "table";
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
	/** convener text */
	public static final String MAINT_CONVENOR_TEXT = "Module Convenor:";
	/** availability element */
	public static final String AVAILABILITY_ELEMENT = "b";
	/** availability text */
	public static final String AVAILABILITY_TEXT = "Availability";
	/** availability text */
	public static final String MAINT_AVAILABILITY_TEXT = "Status:";
	/** Semester element */
	public static final String SEMESTER_ELEMENT = "th";
	public static final String MAINT_SEMESTER_ELEMENT = "b";
	/** Semester text */
	public static final String SEMESTER_TEXT = "Semester";
	public static final String MAINT_SEMESTER_TEXT = "Taught Semesters";
	/** Prerequisites element */
	public static final String PREREQUISITES_ELEMENT = "b";
	/** Semester text */
	public static final String PREREQUISITES_TEXT = "Prerequisites";
	/** Semester text */
	public static final String MAINT_PREREQUISITES_TEXT = "Pre-requisite(s)";
	/** corequisites element */
	public static final String COREQUISITES_ELEMENT = "b";
	/** corequisites text */
	public static final String COREQUISITES_TEXT = "Corequisites";
	/** corequisites text */
	public static final String MAINT_COREQUISITES_TEXT = "Co-requisite(s)";
	/** assessment element */
	public static final String ASSESSMENT_ELEMENT = "b";
	/** assessment text */
	public static final String ASSESSMENT_TEXT = "Method of Assessment";
	/** offering school element */
	public static final String OFFERINGSCHOOL_ELEMENT = "b";
	/** offering school  text */
	public static final String OFFERINGSCHOOL_TEXT = "Offering School";
	/** summary element */
	public static final String SUMMARY_ELEMENT = "b";
	/** summary text */
	public static final String SUMMARY_TEXT = "Summary of Content";
	/** summary end text */
	public static final String SUMMARY_END_TEXT[] = new String[] {"<P><B>Method and Frequency of Class:","<B>Module Web Links:"};
	/** summary theme pattern */
	public static final String SUMMARY_THEME_PATTERN = "This module is part of the ([a-zA-Z \\-]+) theme";
	/** summary end text */
	public static final String MAINT_SUMMARY_END_TEXT[] = new String[] {"</TD>"};
	/** element */
	public static final String TARGET_STUDENTS_ELEMENT = "b";
	/** element text */
	public static final String TARGET_STUDENTS_TEXT = "Target Students:";
	/** end text */
	public static final String TARGET_STUDENTS_END_TEXT[] = new String[] {"<p><b>Taught Semesters:","<P><B>Availability:"};
	public static final String MAINT_TARGET_STUDENTS_END_TEXT[] = new String[] {"<BR>"};
	/** element */
	public static final String EDUCATION_AIMS_ELEMENT = "b";
	/** element text */
	public static final String EDUCATION_AIMS_TEXT = "Education Aims:";
	/** element text */
	public static final String MAINT_EDUCATION_AIMS_TEXT = "Aims:";
	/** end text */
	public static final String EDUCATION_AIMS_END_TEXT = "<p><b>Learning Outcomes:";
	public static final String MAINT_EDUCATION_AIMS_END_TEXT = "</TD>";
	/** element */
	public static final String LEARNING_OUTCOMES_ELEMENT = "b";
	/** element text */
	public static final String LEARNING_OUTCOMES_TEXT = "Learning Outcomes:";
	/** end text */
	public static final String LEARNING_OUTCOMES_END_TEXT = "<p><b>Offering School:";
	public static final String MAINT_LEARNING_OUTCOMES_END_TEXT = "</TD>";
	/** last modified text 
	 */
	public static final String LAST_MODIFIED_PATTERN = "[(]Last Updated:([^)]*)[)]";
	
	/** test main */
	public static void main(String args[]) {
		for (int i=0; i<args.length; i++) {
			logger.info("Try to read "+args[i]);
			try {
				ModuleInfo mi = processModulePage(null, args[i], args[i]);
			}
			catch (Exception e) {
				logger.error("Error", e);
			}
		}
	}
	/** process module page */
	static ModuleInfo processModulePage(String baseUrl, String text, String url) throws java.io.IOException {
		return processModulePage(baseUrl, text, url, false);
	}
	/** process module page */
	static ModuleInfo processModulePage(String baseUrl, String text, String url, boolean maint) throws java.io.IOException {
		ModuleInfo mi = new ModuleInfo();
		String pagePath = url;
		if (url.startsWith("http:"))
		{
			baseUrl = url.substring(0, url.lastIndexOf("/")+1);
			pagePath = url.substring(url.lastIndexOf("/")+1);
	
		}
		logger.info("Processing module "+text+" page "+url+(maint ? " (maintainance mode)" : ""));
		HtmlDocument page = new HtmlDocument(baseUrl, pagePath);
		// dates/times
		{
			mi.filemodified = page.getFileLastModified();
			logger.info("File modified: "+mi.filemodified);
			Pattern p = Pattern.compile(LAST_MODIFIED_PATTERN);
			Matcher m = p.matcher(page.content);
			if (m.find()) {
				logger.info("Last updated: "+mi.lastupdated);
				mi.lastupdated = m.group(1);
			}
			else
				logger.warn("Could not find last updated");
		}
		
		//page.dump();
		//System.exit(-2);
		// text from (first) H2 should be module code <ws> module title
		// for maint it is <h3>...<br>Gxxxxx ....
		HtmlDocument.Node heading = page.getElementNode(page.getRootNode(), maint ? MAINT_PAGE_TITLE_ELEMENT : PAGE_TITLE_ELEMENT);
		if (heading==null) {
			if (!maint) {
				heading = page.getElementNode(page.getRootNode(), MAINT_PAGE_TITLE_ELEMENT);
				if (heading!=null) {
					logger.warn("Switching to maintainance mode for module "+text);
					maint = true;
				}
			}
			if (heading==null)
				throw new IOException("Couldn't find heading element in page for "+text);
		}
		String codeAndTitle = heading.getBodyText().trim();//.getBodyText();
		String yearText = null;
		int ix = 0;
		if(maint) {
			ix = codeAndTitle.indexOf(MAINT_PAGE_TITLE_ELEMENT2);
			if (ix<0)
				throw new IOException("Couldn't find code/title section in heading "+codeAndTitle);
			codeAndTitle = codeAndTitle.substring(ix+MAINT_PAGE_TITLE_ELEMENT2.length()).trim();
			ix = codeAndTitle.indexOf(MAINT_PAGE_TITLE_ELEMENT2);
			if (ix<0)
				throw new IOException("Couldn't find year section in heading "+codeAndTitle);
			yearText = codeAndTitle.substring(ix+MAINT_PAGE_TITLE_ELEMENT2.length()).trim();
			codeAndTitle = codeAndTitle.substring(0, ix);
		}
		ix = codeAndTitle.indexOf(" ");
		if (ix<0)
			throw new IOException("Couldn't find code/title in heading "+codeAndTitle);
		String modulecode = codeAndTitle.substring(0, ix);
		if (text!=null && !text.equalsIgnoreCase(modulecode)) 
			throw new IOException("Expected module "+text+" but found "+modulecode);
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
		if (!maint) {
			HtmlDocument.Node yearNode = page.getElementNodeStartingWith(page.getRootNode(), YEAR_ELEMENT, YEAR_TEXT);
			if (yearNode==null)
				throw new IOException("Couldn't find year element in page for "+text);
			yearText = replaceEntityRefs(yearNode.getBodyText().substring(YEAR_TEXT.length())).trim();
		}
		else if (yearText.startsWith("Year"))
			yearText = yearText.substring(4).trim();
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
		// maint <B>Level 4</B>
		HtmlDocument.Node levelNode = page.getElementNodeStartingWith(page.getRootNode(), LEVEL_ELEMENT, LEVEL_TEXT);
		String levelText = "";
		if (levelNode==null) {
			logger.warn("Could not find level for "+text);
		} else { 
			if (maint) {
				levelText = levelNode.getBodyText().trim();		
			}
			else {
				levelText = page.getSimpleContentFollowing(levelNode).trim();
			}
			if (levelText.toLowerCase().startsWith("level"))
				levelText = levelText.substring("level".length()).trim();
			logger.info("Level: "+levelText);
		}
		mi.level = levelText;

		// credits is e.g. "<p><b>Convenor:&nbsp;</b><BR> Dr G Hutton<BR><p>"
		HtmlDocument.Node convenorNode = page.getElementNodeStartingWith(page.getRootNode(), CONVENOR_ELEMENT, maint ? MAINT_CONVENOR_TEXT : CONVENOR_TEXT);
		String convenorText = "";
		if (convenorNode==null) {
			logger.warn("Could not find convenor for "+text);
		} else if (maint) {
			// up 2, next row, down to cell / table, each row, second cell (stripped)
			try {
				Vector<HtmlDocument.Node> rowNodes = convenorNode.parent.parent.getNextSibling().
					getChildElements(TABLE_CELL_ELEMENT).get(0).getChildElements(TABLE_ELEMENT).get(0).
					getChildElements(TABLE_ROW_ELEMENT);
				for (int ri=0; ri<rowNodes.size(); ri++) {
					Vector<HtmlDocument.Node> cells = rowNodes.get(ri).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<2) {
						logger.warn("Could not find two cells in "+ri+" row of convenor table");
					} else {						
						// down to second row, first element
						convenorText = convenorText + replaceEntityRefs(cells.get(1).getPureBodyText()).replace(",","").trim();
					}					
				}
			}
			catch (Exception e) {
				logger.warn("Could not find convenor table");
			}

		} else {
			convenorText = page.getSimpleContentFollowing(convenorNode, true).trim();
			logger.info("Convenor: "+convenorText);
		}
		mi.convenor = convenorText;
		
		// offering school is e.g. "<p><b>Offering School:&nbsp; </b>Computer Science"
		HtmlDocument.Node offeringschoolNode = page.getElementNodeStartingWith(page.getRootNode(), OFFERINGSCHOOL_ELEMENT, OFFERINGSCHOOL_TEXT);
		String offeringschoolText = "";
		if (offeringschoolNode==null) {
			logger.warn("Could not find offeringschool for "+text);
		} else {
			offeringschoolText = page.getSimpleContentFollowing(offeringschoolNode).trim();
			logger.info("Offering school: "+offeringschoolText);
			mi.offeringschool = offeringschoolText;
		}
		
		// summary is e.g. "<p><b>Summary of Content: &nbsp;</b>The course begins by introducing the generic concepts of distributed
		// systems as they relate to networking. The course starts from the basic..."
		HtmlDocument.Node summaryNode = page.getElementNodeStartingWith(page.getRootNode(), SUMMARY_ELEMENT, SUMMARY_TEXT);
		String summaryText = "";
		if (summaryNode==null) {
			logger.warn("Could not find summary for "+text);
		} else {
			summaryText = page.getFollowingText(summaryNode, maint ? MAINT_SUMMARY_END_TEXT : SUMMARY_END_TEXT);
			logger.info("Summary: "+summaryText);
			Pattern p = Pattern.compile(SUMMARY_THEME_PATTERN);
			Matcher m = p.matcher(summaryText);
			if (m.find())
			{
				String themeName = m.group(1);
				logger.info("Theme name: "+themeName);
				mi.groupname = themeName;
			}
			else 
				logger.warn("Could not find theme in summary for "+text);
			mi.summary = summaryText;
		}
		
		HtmlDocument.Node targetstudentsNode = page.getElementNodeStartingWith(page.getRootNode(), TARGET_STUDENTS_ELEMENT, TARGET_STUDENTS_TEXT);
		if (targetstudentsNode==null) {
			logger.warn("Could not find targetstudents for "+text);
		} else {
			mi.targetstudents = page.getFollowingText(targetstudentsNode, maint ? MAINT_TARGET_STUDENTS_END_TEXT : TARGET_STUDENTS_END_TEXT);
		}
		HtmlDocument.Node educationaimsNode = page.getElementNodeStartingWith(page.getRootNode(), EDUCATION_AIMS_ELEMENT, maint ? MAINT_EDUCATION_AIMS_TEXT : EDUCATION_AIMS_TEXT);
		if (educationaimsNode==null) {
			logger.warn("Could not find educationaims for "+text);
		} else {
			mi.educationaims = page.getFollowingText(educationaimsNode, maint ? MAINT_EDUCATION_AIMS_END_TEXT : EDUCATION_AIMS_END_TEXT);
		}
		HtmlDocument.Node learningoutcomesNode = page.getElementNodeStartingWith(page.getRootNode(), LEARNING_OUTCOMES_ELEMENT, LEARNING_OUTCOMES_TEXT);
		if (learningoutcomesNode==null) {
			logger.warn("Could not find learningoutcomes for "+text);
		} else {
			mi.learningoutcomes = page.getFollowingText(learningoutcomesNode, maint ? MAINT_LEARNING_OUTCOMES_END_TEXT : LEARNING_OUTCOMES_END_TEXT);
			parseLearningOutcomes(mi);
		}
		
		// maint: <TR><TD valign="top" NOWRAP colspan='2'><B>Taught Semesters and Assessment Period: </B></TD>
		// <TR><TD><TABLE  border='0' ><TR><TD>&nbsp;&nbsp;&nbsp;</TD><TD><LI>Autumn</LI></TD><TD align='center'>(Default)</TD><TD>&nbsp;&nbsp;Assessed by end of Autumn Semester</TD></TR></TABLE>

		// semester: <TR><TH>Semester</TH><TH>Assessment</TH></TR><TR><TD>Autumn&nbsp;</TD>
		HtmlDocument.Node semesterNode = page.getElementNodeStartingWith(page.getRootNode(), maint ? MAINT_SEMESTER_ELEMENT : SEMESTER_ELEMENT, maint ? MAINT_SEMESTER_TEXT : SEMESTER_TEXT);
		String semesterText = "";
		if (semesterNode==null) {
			logger.warn("Could not find semester for "+text);
		} else if (maint) {
			// up two 2 row, next row, down to cell / table, first row, second element
			try {
				HtmlDocument.Node semesterRow = semesterNode.parent.parent.getNextSibling().
					getChildElements(TABLE_CELL_ELEMENT).get(0).getChildElements(TABLE_ELEMENT).get(0).
					getChildElements(TABLE_ROW_ELEMENT).get(0).getChildElements(TABLE_CELL_ELEMENT).get(1);
				semesterText = replaceEntityRefs(semesterRow.getPureBodyText()).trim();
				logger.info("Semester: "+semesterText);
			}
			catch (Exception e) {
				logger.warn("Could not find semester cell in Taught Semesters table");
			}
		} else {
			// up two levels to TABLE (maint 3)
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
		// or "Module is dormant..."
		HtmlDocument.Node availabilityNode = page.getElementNodeStartingWith(page.getRootNode(), AVAILABILITY_ELEMENT, maint ? MAINT_AVAILABILITY_TEXT : AVAILABILITY_TEXT);
		String availabilityText = "";
		if (availabilityNode==null) {
			logger.debug("Could not find availability for "+text);
		} else {
			availabilityText = page.getSimpleContentFollowing(availabilityNode, true).trim();
			logger.info("Availability: "+availabilityText);
		}
		mi.status = Status.available;
		if (maint) {
			if (availabilityText.toLowerCase().startsWith("suspend"))
				mi.status = Status.suspended;
			else if (availabilityText.toLowerCase().startsWith("dormant"))
				mi.status = Status.dormant;
		} else {
			if (availabilityText.toLowerCase().startsWith("not"))
				mi.status = Status.suspended;
			else if (availabilityText.toLowerCase().contains("dormant"))
				mi.status = Status.dormant;
		}
		mi.availability = mi.status==Status.available;

		// TODO module web links
		// <B>Module Web Links:</B><BR><TABLE border='0' CELLSPACING ='3' CELLPADDING='2' width='80%' ><TR><TD>&nbsp;&nbsp;&nbsp;</TD><TD><LI><A target='_blank' href='https://www.nottingham.ac.uk/is/gateway/readinglists/local/displaylist?module=G5AHOC'>Reading List</A></LI></TD></TR></TABLE>
		
		// output
		//System.out.println("modulecode,saturntitle,saturnstatus,saturnsemester,saturnlevel,saturnconvenor");
		//System.out.println(modulecode+","+moduletitle+","+(availabilityText.toLowerCase().startsWith("not") ? "Suspended" : "")+","+semesterText+","+levelText+","+convenorText);

		// Prerequisites: <p><b>Prerequisites:&nbsp;</b><p><center><TABLE border='0' CELLSPACING ='3' CELLPADDING='2' ><TR><TH NOWRAP>Mnem</TH><TH>Title</TH></TR><TR><TD ><A HREF='moduledetails.asp?crs_id=002252&year_id=000109'>G52CCN</A>&nbsp;</TD><TD>Computer Communications and Networks&nbsp;</TD></TR><TR><TD ><A HREF='moduledetails.asp?crs_id=012192&year_id=000109'>G51PRG</A>&nbsp;</TD><TD>Programming&nbsp;</TD></TR></TABLE>
		HtmlDocument.Node prerequisitesNode = page.getElementNodeStartingWith(page.getRootNode(), PREREQUISITES_ELEMENT, maint ? MAINT_PREREQUISITES_TEXT : PREREQUISITES_TEXT);
		if (prerequisitesNode==null) {
			logger.warn("Could not find prerequisites for "+text);
		} else if (maint) {
			// up 2 to row, next row x2, down to cell, check it is not bold
			try {
				HtmlDocument.Node coreqTextNode = prerequisitesNode.parent.parent.getNextSibling().
				getNextSibling().getChildElements(TABLE_CELL_ELEMENT).get(0);
				String corequisitestext = coreqTextNode.getBodyText().trim();
				if (corequisitestext.length()>0 && corequisitestext.indexOf("<")<0) {
					mi.prerequisitestext = corequisitestext;
					logger.info("prerequisitestext: "+mi.prerequisitestext);
				}
					
			}
			catch (Exception e) {
				logger.warn("Could not find prerequisites text row");
			}
			// up 2 to row, next row, down to cell / table, all rows, cell 2 (stripped)
			try {
				Vector<HtmlDocument.Node> rowNodes = prerequisitesNode.parent.parent.getNextSibling().
					getChildElements(TABLE_CELL_ELEMENT).get(0).getChildElements(TABLE_ELEMENT).get(0).
					getChildElements(TABLE_ROW_ELEMENT);
				for (int ri=0; ri<rowNodes.size(); ri++) {
					Vector<HtmlDocument.Node> cells = rowNodes.get(ri).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<2) {
						logger.warn("Could not find two cells in "+ri+" row of prerequisites table");
					} else {						
						// down to second row, first element
						String prerequisiteText = replaceEntityRefs(cells.get(1).getPureBodyText()).trim();
						logger.info("prerequisite: "+prerequisiteText); //+" ("+cells.get(0).getBodyText()+")");
						mi.prerequisitecodes.add(prerequisiteText);
					}					
				}
			}
			catch (Exception e) {
				logger.warn("Could not find prerequisites table");
			}
			
		} else {
			mi.prerequisitestext = page.getSimpleContentFollowing(prerequisitesNode, true).trim();
			logger.info("prerequisitestext: "+mi.prerequisitestext);

			HtmlDocument.Node possibleTableNode = prerequisitesNode.parent.getNextSibling();
			if (possibleTableNode==null || !possibleTableNode.name.toLowerCase().equals("p") || possibleTableNode.children.size()!=1) 
				logger.warn("Could not find parent-sibling of prerequisites node");
			else {
				Vector<HtmlDocument.Node> rowNodes = page.getElementNodes(possibleTableNode, TABLE_ROW_ELEMENT);
				for (int ri=0; ri<rowNodes.size(); ri++) {
					Vector<HtmlDocument.Node> cells = rowNodes.get(ri).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<2) {
						logger.warn("Could not find two cells in "+ri+" row of prerequisites table");
					} else {						
						// down to second row, first element
						String prerequisiteText = replaceEntityRefs(cells.get(0).getPureBodyText()).trim();
						logger.info("prerequisite: "+prerequisiteText); //+" ("+cells.get(0).getBodyText()+")");
						mi.prerequisitecodes.add(prerequisiteText);
					}					
				}
			}
		}

		HtmlDocument.Node corequisitesNode = page.getElementNodeStartingWith(page.getRootNode(), COREQUISITES_ELEMENT, maint ? MAINT_COREQUISITES_TEXT : COREQUISITES_TEXT);
		if (corequisitesNode==null) {
			logger.warn("Could not find corequisites for "+text);
		} else if (maint) {
			// up 2 to row, next row x2, down to cell, check it is not bold
			try {
				HtmlDocument.Node coreqTextNode = corequisitesNode.parent.parent.getNextSibling().
				getNextSibling().getChildElements(TABLE_CELL_ELEMENT).get(0);
				String corequisitestext = coreqTextNode.getBodyText().trim();
				if (corequisitestext.length()>0 && corequisitestext.indexOf("<")<0) {
					mi.corequisitestext = corequisitestext;
					logger.info("corequisitestext: "+mi.corequisitestext);
				}

			}
			catch (Exception e) {
				logger.warn("Could not find corequisites text row");
			}
			// up 2 to row, next row, down to cell / table, all rows, cell 2 (stripped)
			try {
				Vector<HtmlDocument.Node> rowNodes = corequisitesNode.parent.parent.getNextSibling().
					getChildElements(TABLE_CELL_ELEMENT).get(0).getChildElements(TABLE_ELEMENT).get(0).
					getChildElements(TABLE_ROW_ELEMENT);
				for (int ri=0; ri<rowNodes.size(); ri++) {
					Vector<HtmlDocument.Node> cells = rowNodes.get(ri).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<2) {
						logger.warn("Could not find two cells in "+ri+" row of corequisites table");
					} else {						
						// down to second row, first element
						String prerequisiteText = replaceEntityRefs(cells.get(1).getPureBodyText()).trim();
						logger.info("corequisite: "+prerequisiteText); //+" ("+cells.get(0).getBodyText()+")");
						mi.corequisitecodes.add(prerequisiteText);
					}					
				}
			}
			catch (Exception e) {
				logger.warn("Could not find corequisites table");
			}
			
			
		} else {
			mi.corequisitestext = page.getSimpleContentFollowing(corequisitesNode, true).trim();
			logger.info("corequisitestext: "+mi.corequisitestext);

			HtmlDocument.Node possibleTableNode = corequisitesNode.parent.getNextSibling();
			if (possibleTableNode==null || !possibleTableNode.name.toLowerCase().equals("p") || possibleTableNode.children.size()!=1) 
				logger.warn("Could not find parent-sibling of corequisites node");
			else {
				Vector<HtmlDocument.Node> rowNodes = page.getElementNodes(possibleTableNode, TABLE_ROW_ELEMENT);
				for (int ri=0; ri<rowNodes.size(); ri++) {
					Vector<HtmlDocument.Node> cells = rowNodes.get(ri).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<2) {
						logger.warn("Could not find two cells in "+ri+" row of corequisites table");
					} else {						
						// down to second row, first element
						String corequisiteText = replaceEntityRefs(cells.get(0).getPureBodyText()).trim();
						logger.info("corequisite: "+corequisiteText); //+" ("+cells.get(0).getBodyText()+")");
						mi.corequisitecodes.add(corequisiteText);
					}					
				}
			}
		}

		// method of assessment: <p><b>Method of Assessment:&nbsp; </b><center><TABLE border='0' CELLSPACING ='3' CELLPADDING='2' ><TR><TH NOWRAP>Assessment Type</TH><TH>Weight</TH><TH>Requirements</TH></TR><TR><TD >Exam 1&nbsp;</TD><TD align='center'>100&nbsp;</TD><TD width='40%' align='center'>2 hr written examination&nbsp;</TD></TR></TABLE></center>
		HtmlDocument.Node assessmentNode = page.getElementNodeStartingWith(page.getRootNode(), ASSESSMENT_ELEMENT, ASSESSMENT_TEXT);
		if (assessmentNode==null) {
			logger.warn("Could not find assessment for "+text);
		} else {
			HtmlDocument.Node possibleTableNode = assessmentNode.getNextSibling();
			if (possibleTableNode==null)
				logger.warn("Could not find sibling of assessment node");
			else {
				Vector<HtmlDocument.Node> rowNodes = page.getElementNodes(possibleTableNode, TABLE_ROW_ELEMENT);
				for (int ri=0; ri<rowNodes.size(); ri++) {
					Vector<HtmlDocument.Node> cells = rowNodes.get(ri).getChildElements(TABLE_CELL_ELEMENT);					
					if (cells.size()<3) {
						logger.warn("Could not find three cells in "+ri+" row of assessment table");
					} else {						
						// down to second row, first element
						String assessmentTypeText = replaceEntityRefs(cells.get(0).getPureBodyText()).trim();
						String assessmentWeightText = replaceEntityRefs(cells.get(1).getPureBodyText()).trim();
						String assessmentRequirementsText = replaceEntityRefs(cells.get(2).getPureBodyText()).trim();
						logger.info("assessment: "+assessmentTypeText+" ("+assessmentWeightText+"%)"); //+" ("+cells.get(0).getBodyText()+")");
						//mi.corequisitecodes.add(corequisiteText);
						AssessmentInfo ai = new AssessmentInfo();
						ai.type = assessmentTypeText;
						ai.weight = assessmentWeightText;
						ai.requirements = assessmentRequirementsText;
						mi.assessmentInfos.add(ai);
						mi.assessments++;
						if (assessmentTypeText.toLowerCase().startsWith("exam")) {
							try {
								mi.exampercentage += Float.parseFloat(assessmentWeightText);
							} catch (NumberFormatException nfe) {
								logger.warn("Assessment weight format error: "+assessmentWeightText, nfe);
							}
							mi.examrequirements = assessmentRequirementsText;
							// duration pattern n+[.n] h[ou]r [n+[ ]m...
							Pattern p = Pattern.compile("^(\\d+([.]\\d+)?)(\\s*[hH]([oO][uU])?[rR]\\s*(\\d+))?.*");
							Matcher m = p.matcher(mi.examrequirements);
							if (m.find()) {
								try {
									mi.examdurationhours = Float.parseFloat(m.group(1));
									if (m.group(5)!=null)
										mi.examdurationhours += Integer.parseInt(m.group(5))/60.0;
								}
								catch (Exception e) {
									logger.error("Parsing exam duration: "+mi.examrequirements, e);
								}
							}
							else
								logger.warn("length not found for exam: "+mi.examrequirements);
						}
					}					
				}
			}
		}
		logger.info("Assessments: "+mi.assessments+", "+mi.exampercentage+"% exam");

		
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
	static enum SummaryHeading {
		modulecode,moduletitle,year,level,semester,credits,available,status,convenor,prerequisites,corequisites,assessments,exampercentage,examdurationhours,examrequirements,newgroup
	};
	static String getSummaryHeadingLine() {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<SummaryHeading.values().length; i++) {
			if (i>0)
				b.append(",");
			b.append(SummaryHeading.values()[i]);
		}
		return b.toString();
	}
	String toSummaryLine() {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<SummaryHeading.values().length; i++) {
			if (i>0)
				b.append(",");
			SummaryHeading heading = SummaryHeading.values()[i];
			switch(heading) {
			case modulecode:
				b.append(this.modulecode);
				break;
			case moduletitle:
				b.append(CsvUtils.escape(this.moduletitle));
				break;
			case year:
				b.append(this.year);
				break;
			case level:
				b.append(this.level);
				break;
			case semester:
				b.append(this.semester);
				break;
			case credits:
				b.append(this.credits);
				break;
			case available:
				b.append((this.availability ? "Y" : "N"));
				break;
			case status:
				b.append((this.status!=null ? this.status.toString() : ""));
				break;
			case convenor:
				b.append(CsvUtils.escape(this.convenor));
				break;
			case prerequisites:
				b.append("\"");
				for (String requisite: this.prerequisitecodes)
					b.append(requisite+" ");				
				b.append("\"");
				break;
			case corequisites:
				b.append("\"");
				for (String requisite: this.corequisitecodes)
					b.append(requisite+" ");				
				b.append("\"");
				break;
			case assessments:
				b.append(this.assessments);
				break;
			case exampercentage:
				b.append(this.exampercentage);
				break;
			case examdurationhours:
				b.append(this.examdurationhours);
				break;
			case examrequirements:
				b.append(this.examrequirements);
				break;
			case newgroup:
				b.append(this.newgroup);
				break;
			default:
				logger.error("Unhandled heading "+heading);	
			}
		}
		return b.toString();
	}
	static HashMap<String,ModuleInfo> readSummaryFile(java.io.File file) throws IOException {
		HashMap<String,HashMap<String,String>> modules = ReadCsvFile.readCsvFile(file, SummaryHeading.modulecode.name(), false);
		HashMap<String,ModuleInfo> moduleinfos = new HashMap<String,ModuleInfo>();
		for (String modulecode : modules.keySet()) {
			HashMap<String,String> values = modules.get(modulecode);
			ModuleInfo mi = new ModuleInfo();
			mi.modulecode = modulecode;
			moduleinfos.put(modulecode, mi);
			try {
				if (values.containsKey(SummaryHeading.assessments.name()))
					mi.assessments = Integer.parseInt(values.get(SummaryHeading.assessments.name()));
				else
					logger.warn("assessments not found in "+values);
			} catch (NumberFormatException nfe) {
				throw new IOException("assessments not a number: "+values.get(SummaryHeading.assessments.name()));
			}
			if (values.containsKey(SummaryHeading.available.name()))
				mi.availability= values.get(SummaryHeading.available.name()).toLowerCase().startsWith("y");
			else
				logger.warn("availability not found in "+values);
			if (values.containsKey(SummaryHeading.convenor.name()))
				mi.convenor = values.get(SummaryHeading.convenor.name());
			else
				logger.warn("convenor not found in "+values);
			if (values.containsKey(SummaryHeading.corequisites.name())) {
				StringTokenizer toks = new StringTokenizer(values.get(SummaryHeading.corequisites.name()));
				while (toks.hasMoreTokens()) {
					String tok = toks.nextToken();
					mi.corequisitecodes.add(tok);
				}
			}
			else
				logger.warn("corequisites not found in "+values);
			if (values.containsKey(SummaryHeading.prerequisites.name())) {
				StringTokenizer toks = new StringTokenizer(values.get(SummaryHeading.prerequisites.name()));
				while (toks.hasMoreTokens()) {
					String tok = toks.nextToken();
					mi.prerequisitecodes.add(tok);
				}
			}
			else
				logger.warn("prerequisites not found in "+values);
			try {
				if (values.containsKey(SummaryHeading.credits.name()))
					mi.credits = Integer.parseInt(values.get(SummaryHeading.credits.name()));
				else
					logger.warn("credits not found in "+values);
			} catch (NumberFormatException nfe) {
				throw new IOException("credits not a number: "+values.get(SummaryHeading.credits.name()));
			}
			try {
				if (values.containsKey(SummaryHeading.examdurationhours.name()))
					mi.examdurationhours = Float.parseFloat(values.get(SummaryHeading.examdurationhours.name()));
				else
					logger.warn("examdurationhours not found in "+values);
			} catch (NumberFormatException nfe) {
				throw new IOException("examdurationhours not a number: "+values.get(SummaryHeading.examdurationhours.name()));
			}
			try {
				if (values.containsKey(SummaryHeading.exampercentage.name()))
					mi.exampercentage = Float.parseFloat(values.get(SummaryHeading.exampercentage.name()));
				else
					logger.warn("exampercentage not found in "+values);
			} catch (NumberFormatException nfe) {
				throw new IOException("examdpercentage not a number: "+values.get(SummaryHeading.exampercentage.name()));
			}
			if (values.containsKey(SummaryHeading.examrequirements.name()))
				mi.examrequirements = values.get(SummaryHeading.examrequirements.name());
			else
				logger.warn("examrequirements not found in "+values);
			if (values.containsKey(SummaryHeading.level.name()))
				mi.level = values.get(SummaryHeading.level.name());
			else
				logger.warn("level not found in "+values);
			if (values.containsKey(SummaryHeading.moduletitle.name()))
				mi.moduletitle = values.get(SummaryHeading.moduletitle.name());
			else
				logger.warn("moduletitle not found in "+values);
			if (values.containsKey(SummaryHeading.semester.name()))
				mi.semester = values.get(SummaryHeading.semester.name());
			else
				logger.warn("semester not found in "+values);
			if (values.containsKey(SummaryHeading.year.name()))
				mi.year = values.get(SummaryHeading.year.name());
			else
				logger.warn("year not found in "+values);
			if (values.containsKey(SummaryHeading.newgroup.name()))
				mi.newgroup = values.get(SummaryHeading.newgroup.name());
			//else
				//logger.warn("newgroup not found in "+values);
			if (values.containsKey(SummaryHeading.status.name()) && values.get(SummaryHeading.status.name()).length()>0) {
				try {
					mi.status = Status.valueOf(values.get(SummaryHeading.status.name()));
				}
				catch (Exception e) {
					logger.warn("Unknown status "+SummaryHeading.status.name());					
				}
			}
			else
				logger.warn("status not found in "+values);
		}
		return moduleinfos;
	}
	static void printSummary(java.util.Collection<ModuleInfo> modules) {
		System.out.println(ModuleInfo.getSummaryHeadingLine());
		for (ModuleInfo mi : modules)
			System.out.println(mi.toSummaryLine());
	}
	public static final String LEARNING_OUTCOME_SECTIONS [] = new String[] {
		"Knowledge and Understanding", "Intellectual Skills", "Professional Skills", "Transferable Skills"
	};

	static void parseLearningOutcomes(ModuleInfo mi) {
		if (mi.learningoutcomes==null)
			return;
//		logger.info("parseLearningOutcomes:" +mi.learningoutcomes);
		int ix = 0, nix = 0;
		ix = mi.learningoutcomes.indexOf(LEARNING_OUTCOME_SECTIONS[0], ix);
		if (ix>=0) 
			ix += LEARNING_OUTCOME_SECTIONS[0].length();
		for (int section = 0; section <  LEARNING_OUTCOME_SECTIONS.length && ix>=0 && ix<mi.learningoutcomes.length(); section++, ix = nix) {
			nix = mi.learningoutcomes.length();
			if (section+1 < LEARNING_OUTCOME_SECTIONS.length) 
				nix = mi.learningoutcomes.indexOf(LEARNING_OUTCOME_SECTIONS[section+1], ix);
			if (nix<0) 
			{
				logger.warn("Could not fine learning outcome section "+LEARNING_OUTCOME_SECTIONS[section+1]+" in "+mi.learningoutcomes);
				nix = mi.learningoutcomes.length();
				for (int si2=section+1; si2<LEARNING_OUTCOME_SECTIONS.length; si2++)
				{
					int i = mi.learningoutcomes.indexOf(LEARNING_OUTCOME_SECTIONS[si2], ix);
					if (i>ix) {
						nix = i;
						break;
					}
				}
			}
			String span = mi.learningoutcomes.substring(ix,nix);
			int id = 1;
			// NLs followed by spaces, XML, full-stops after letters and semi-colons may be item markers
			int six = 0, nsix = 0;
			for (; six>=0 && six<span.length(); six=nsix) {
//				logger.info("ix="+ix+", six="+six);
				if (span.charAt(six)=='<') {
					int i = span.indexOf('>', six);
					if (i<=six)
						break;
					nsix = i+1;
					continue;
				} 
				else if (!Character.isLetterOrDigit(span.charAt(six))) {
					nsix = six+1;
					continue;
				}
				nsix = span.length();
				int i;
				i = span.indexOf('\n', six);
				if (i>six && i+1<nsix && Character.isWhitespace(span.charAt(i+1)))
					nsix = i;
				i = span.indexOf('<', six);
				if (i>six && i<nsix)
					nsix = i;
				i = span.indexOf(';', six);
				if (i>six && i<nsix)
					nsix = i;
				i = span.indexOf('.', six);
				if (i>six && i<nsix) {
					// .NET etc
					while (i>0 &&
							((i+1<span.length() && 
									Character.isLetterOrDigit(span.charAt(i+1)))
						|| (i>3 && span.substring(i-3,i+1).equals("e.g."))))
						i = span.indexOf('.', i+1);
					if (i>six && i<nsix)
						nsix = i;
				}
				//logger.info("Span "+six+":"+nsix+" of "+span);
				String text = span.substring(six, nsix);
				CourseInfo.Outcome outcome =new CourseInfo.Outcome();
				outcome.text = text.trim();
				outcome.id = ""+((char)('A'+section))+id;
				id++;
				mi.outcomeSections[section].outcomes.add(outcome);
			}
			
			if (nix<mi.learningoutcomes.length())
				nix += LEARNING_OUTCOME_SECTIONS[section+1].length();
		}
	}
	@Override
	public String toString() {
		return "ModuleInfo [allrequisitecodes=" + allrequisitecodes
				+ ", assessmentInfos=" + assessmentInfos + ", assessments="
				+ assessments + ", availability=" + availability
				+ ", convenor=" + convenor + ", corequisitecodes="
				+ corequisitecodes + ", corequisitestext=" + corequisitestext
				+ ", credits=" + credits + ", educationaims=" + educationaims
				+ ", examdurationhours=" + examdurationhours
				+ ", exampercentage=" + exampercentage + ", examrequirements="
				+ examrequirements + ", filemodified=" + filemodified
				+ ", grouprole=" + grouprole + ", lastupdated=" + lastupdated
				+ ", learningoutcomes=" + learningoutcomes + ", level=" + level
				+ ", modulecode=" + modulecode + ", moduletitle=" + moduletitle
				+ ", newgroup=" + newgroup + ", offeringschool="
				+ offeringschool + ", offeringschoool=" + offeringschoool
				+ ", outcomeSections=" + Arrays.toString(outcomeSections)
				+ ", prerequisitecodes=" + prerequisitecodes
				+ ", prerequisitestext=" + prerequisitestext + ", semester="
				+ semester + ", status=" + status + ", summary=" + summary
				+ ", targetstudents=" + targetstudents + ", year=" + year + "]";
	}
}
