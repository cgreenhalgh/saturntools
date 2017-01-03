package saturntools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

public class CourseInfo {
	/** logger */
    static Logger logger = Logger.getLogger(CourseInfo.class);

    boolean mscflag;
    String course;
    String coursecode;
    String title;
    String year;
    String filemodified;
    
    TreeSet<String> compulsory[];
	TreeSet<String> restricted[];
	TreeSet<String> alternative[];

	// minimal module info from pre/co-requisites
	Map<String,ModuleInfo> modules = new HashMap<String,ModuleInfo>();
	
	public static final int KNOWLEDGE_AND_UNDERSTANDING = 0,
		INTELLECTUAL_SKILLS = 1, PROFESSIONAL_PRACTICAL_SKILLS = 2,
		TRANSFERABLE_KEY_SKILLS = 3, NUM_OUTCOME_SECTIONS = 4;
	static class Outcome {
		String id;
		String text;
	}
	static class OutcomeSection {
		String preface;
		Vector<Outcome> outcomes;
		String teachingAndLearning;
		String assessment;
	}
	OutcomeSection outcomeSections[];
	
	
	static final int NUM_STAGES = 4;
	CourseInfo () {
		compulsory = new TreeSet[NUM_STAGES];
		restricted = new TreeSet[NUM_STAGES];
		alternative = new TreeSet[NUM_STAGES];
		for (int i=0; i<NUM_STAGES; i++) {
			compulsory[i] = new TreeSet<String>();
			restricted[i] = new TreeSet<String>();
			alternative[i] = new TreeSet<String>();
		}
		outcomeSections = new OutcomeSection[NUM_OUTCOME_SECTIONS];
		for (int i=0; i<NUM_OUTCOME_SECTIONS; i++) {
			outcomeSections[i] = new OutcomeSection();
			outcomeSections[i].outcomes = new Vector<Outcome>();
		}
	}
	static final String STAGE_ELEMENT = "B";
	static final String QUALIFYING_TEXT = "Qualifying Year";
	static final String PART1_TEXT = "Part I";
	static final String PART2_TEXT = "Part II";
	static final String PART3_TEXT = "Part III";
	static final String TABLE_ELEMENT = "Table";
	static final String COMPULSORY_TEXT = "Compulsory";
	static final String RESTRICTED_TEXT = "Restricted";
	static final String ALTERNATIVE_TEXT = "Alternative";
	static final String ROW_ELEMENT = "TR";
	static final String COLUMN_ELEMENT = "TD";
	static final String TITLE_ELEMENT = "H3";
	static final String TITLE_TEXT = "Regulations";
	static final String COMPULSORY_ELEMENT = "I";
	//static final String COMPULSORY_TEXT = "Compulsory";
	// for programme spec
	static final String SECTIONA1_TITLE_TEXT = "1 Title";
	static final String SECTIONA2_COURSE_CODE_TEXT = "2 Course code";
	static final String SECTIONA3_RESPONSIBLE_TEXT = "3 School(s) Responsible For Management Of The Course";
	static final String SECTIONA4_TEXT = "4 Type of course";
	static final String SESSION_ELEMENT = "BR";
	static final String SESSION_TEXT = "SESSION";
	
	static CourseInfo processCourseFile(String filename) throws java.io.IOException {
		return processCourseFile(new File(filename));
	}
	static CourseInfo processCourseFile(File file) throws java.io.IOException {
	
		CourseInfo ci = new CourseInfo();
		ci.course = file.getName().toUpperCase();
		if (ci.course.indexOf(".")>=0)
			ci.course = file.getName().substring(0, ci.course.indexOf("."));
		HtmlDocument page = new HtmlDocument(null, file.getAbsolutePath());
		ci.filemodified = page.getFileLastModified();		
		logger.info("Last modified: "+ci.filemodified);
		
		//page.dump();
		// session? (prog. spec format)
		Pattern sessionPattern = Pattern.compile("SESSION (\\d\\d\\d\\d)/(\\d\\d\\d\\d)");
		Matcher sessionMatcher = sessionPattern.matcher(page.content);
		if (sessionMatcher.find()) {
			String year1 = sessionMatcher.group(1);
			String year2 = sessionMatcher.group(2);
			logger.info("Session: "+year1+"-"+year2);
			// Module uses 2 digit years / 
			ci.year = year1.substring(2)+"/"+year2.substring(2);
		}
		else
			logger.info("No session found");

		// MSc?
		ci.mscflag = false;
		HtmlDocument.Node title = page.getElementNode(page.getRootNode(), TITLE_ELEMENT);
		if (title!=null) {
			if (title.getBodyText().contains("MSc"))
			{
				logger.info("Seems to be MSc: "+file.getName());
				ci.mscflag = true;
			}
		} else {
			// programme spec catalogue format?
			// <TR><TD colspan="2"><B>1 Title</B></TD></TR>
			// <TR><TD>&nbsp;</TD>
			// <TR><TD>&nbsp;</TD>
			//     <TD><TABLE border="0" width="100%">
			//              <TR><TD>Master of Science Advanced Computing Science</TD><TR>
			//              <TR><TD>&nbsp;</TD></TR>
			//         </TABLE>
			//    </TD>
			// </TR><TR>
			ci.title = getSectionValue(page, SECTIONA1_TITLE_TEXT);
			logger.info("Title: "+ci.title);
			ci.coursecode = getSectionValue(page, SECTIONA2_COURSE_CODE_TEXT);
			logger.info("coursecode: "+ci.coursecode);
			// this isn't working
			String responsible  = getSectionValue(page, SECTIONA3_RESPONSIBLE_TEXT);
			logger.info("responsible: "+responsible);
		}
		if (!ci.mscflag) {
			boolean donePart1 = false;
			try {
				processStage(ci, page, 0, QUALIFYING_TEXT);
			}
			catch (Exception e) {
				logger.warn("Failing for full UG - try as direct year 2 entry UG...", e);
				try {
					processStage(ci, page, 1, PART1_TEXT);	
					donePart1 = true;
				}
				catch (Exception e2) {
					logger.warn("Failing as UG - try as MSc...", e);
					ci.mscflag = true;
				}
			}
			if (!ci.mscflag) {
				if (!donePart1)
					processStage(ci, page, 1, PART1_TEXT);
				processStage(ci, page, 2, PART2_TEXT);
				try {
					processStage(ci, page, 3, PART3_TEXT);					
				}
				catch (Exception e) {
					logger.warn("Assuming not 3year UG", e);
				}
			}
		}
		if (ci.mscflag) {
			processStage(ci, page, 0, null);			
		}
		processLearningOutcomes(ci, page);
		// TODO
		return ci;
	}
	static String getSectionValue(HtmlDocument page, String title) {
		Vector<String> values = getSectionValues(page, title);
		
		StringBuffer b = new StringBuffer();
		for (int i=0; i<values.size(); i++) {
			if (i>0)
				b.append(" ");
			b.append(values.get(i));
		}
		return b.toString().trim();
	}
	/** Programme spec format - for basic course data */
	static Vector<String> getSectionValues(HtmlDocument page, String title) {
		Vector<String> values = new Vector<String>();
		// programme spec catalogue format?
		// <TR><TD colspan="2"><B>1 Title</B></TD></TR>
		// <TR><TD>&nbsp;</TD>
		// <TR><TD>&nbsp;</TD>
		//     <TD><TABLE border="0" width="100%">
		//              <TR><TD>Master of Science Advanced Computing Science</TD><TR>
		//              <TR><TD>&nbsp;</TD></TR>
		//         </TABLE>
		//    </TD>
		// </TR><TR>
		// <B>...
		HtmlDocument.Node titleNode = page.getElementNodeStartingWith(page.getRootNode(), "B", title);
		logger.info("section node for "+title+": "+titleNode);
		// <TR><TD><B>...
		HtmlDocument.Node rowNode = titleNode.parent.parent;
		logger.info("check row node: "+rowNode);
		done:
		while (rowNode!=null) {
			rowNode = rowNode.getNextSibling();
			if (rowNode==null)
				break;
			logger.info("- check row node: "+rowNode);
			Vector<HtmlDocument.Node> cellNodes = rowNode.getChildElements("TD");
			logger.info("- "+cellNodes.size()+" cells");
			for (int ci=0; ci<cellNodes.size(); ci++) {
				Vector<HtmlDocument.Node> tableNodes = cellNodes.get(ci).getChildElements("TABLE");
				logger.info("- "+tableNodes.size()+" table nodes");
				if (tableNodes.size()==0)
					continue;
				// found table!
				Vector<HtmlDocument.Node> tableRowNodes = tableNodes.get(0).getChildElements("TR");
				logger.info("- "+tableRowNodes.size()+" table row nodes");
				for (int tri=0; tri<tableRowNodes.size(); tri++) {
					Vector<HtmlDocument.Node> tableCellNodes = tableRowNodes.get(tri).getChildElements("TD");
					logger.info("- "+tableCellNodes.size()+" table cell nodes");
					if (tableCellNodes.size()==0)
						continue;
					String value = tableCellNodes.get(0).getPureBodyText().trim();
					logger.info("- got value! "+value+" ("+tableCellNodes.get(0)+")");
					values.add(value);
				}
				break done;
			}
		}
		return values;
	}
	static void processStage(CourseInfo ci, HtmlDocument page, int stage, String stageTitle) throws java.io.IOException  {
		HtmlDocument.Node current = null;
		boolean compulsory = false;
		if (stageTitle!=null) {
			HtmlDocument.Node heading = page.getElementNodeStartingWith(page.getRootNode(), STAGE_ELEMENT, stageTitle);
			if (heading==null)
				throw new IOException("Couldn't find stage heading '"+stageTitle+"'");
		/* 	
		 *     Node TABLE, 5261-10125 "<TR><TD colspan='..."
      Node TR, 5291-5343 "<TD colspan='2'><..."
        Node TD, 5295-5338 "<B>Qualifying Yea..."
          Node B, 5311-5333 "Qualifying Year"
      Node TR, 5343-5441 "<TD width='2%'>&n..."
        Node TD, 5347-5373 "&nbsp;"
        Node TD, 5373-5436 "<B><I>Compulsory ..."
          Node B, 5377-5431 "<I>Compulsory    ..."
            Node I, 5380-5427 "Compulsory"
      Node TR, 5441-5518 "<TD>&nbsp;</TD><T..."
        Node TD, 5445-5460 "&nbsp;"
        Node TD, 5460-5513 "Students must tak..."
      Node TD, 5518-5533 "&nbsp;"
      Node TD, 5533-8036 "<TABLE border='0'..."
        Node TABLE, 5550-8026 "<TR><TD><B>Code</..."
          Node TR, 5568-5737 "<TD><B>Code</B></..."
            Node TD, 5572-5592 "<B>Code</B>"
              Node B, 5576-5587 "Code"
            Node TD, 5592-5613 "<B>Title</B>"
              Node B, 5596-5608 "Title"
            Node TD, 5613-5651 "<B>Credits</B>"
              Node B, 5632-5646 "Credits"
            Node TD, 5651-5695 "<B>Compensatable</B>"
              Node B, 5670-5690 "Compensatable"
            Node TD, 5695-5732 "<B>Taught</B>"
              Node B, 5714-5727 "Taught"
          Node TR, 5737-5991 "<TD><A href='http..."
            Node TD, 5741-5869 "<A href='http://w..."
              Node A, 5745-5854 ""
            Node TD, 5869-5905 "Algorithmic Probl..."
            Node TD, 5905-5931 "10"
            Node TD, 5931-5956 "Y"
            Node TD, 5956-5986 "Autumn"
         */
			logger.info("Found stage "+stageTitle);
			// parent = TD -> parent = TR 
			current = heading.parent.parent;
		}
		else {
			// MSc
			// <TABLE width='95%' border='0'><TR><TD colspan='2'><B>&nbsp;</B></TD></TR>
			// <TR><TD width='2%'>&nbsp;</TD><TD><B><I>Compulsory                              </I></B></TD></TR>
			HtmlDocument.Node heading = page.getElementNodeStartingWith(page.getRootNode(), COMPULSORY_ELEMENT, COMPULSORY_TEXT);
			if (heading==null)
				throw new IOException("Couldn't find Compulsory section");
			// -> B -> TD -> TR
			current = heading.parent.parent.parent;
			compulsory = true;
		}
		boolean alternative = false;
		while (current!=null) {
			current = current.getNextSibling();
			
			if (current==null)
				break;
			// table as first child = module list
			Vector<HtmlDocument.Node> tables = current.getChildElements(TABLE_ELEMENT);
			if (tables.size()==1) {
				logger.info("Found table");
				// TODO
				Vector<HtmlDocument.Node> rows = tables.get(0).getChildElements(ROW_ELEMENT);
				for (int r=1; r<rows.size(); r++) {
					Vector<HtmlDocument.Node> cells = rows.get(r).getChildElements(COLUMN_ELEMENT);
					if (cells.size()!=5) {
						logger.warn("Found row with "+cells.size()+" elements");
					}
					else {
						String modulecode = cells.get(0).getPureBodyText().trim();
						
						String title = cells.get(1).getPureBodyText().trim();
						String credits = cells.get(2).getPureBodyText().trim();
						String compensatable = cells.get(3).getPureBodyText().trim();
						String semester = cells.get(4).getPureBodyText().trim();
						// bug fix - leaving </... on 
						int ix = modulecode.indexOf("<");
						if (ix>=0)
							modulecode = modulecode.substring(0, ix);
						if (modulecode.length()==0)
							continue;
						logger.info("Found "+(compulsory ? "compulsory" : "optional?")+" module "+modulecode+" (title='"+title+"', credits="+credits+", compensatable="+compensatable+", semester="+semester+")");
						ModuleInfo mi = new ModuleInfo();
						mi.modulecode = modulecode;
						mi.moduletitle = title;
						try {
							mi.credits = Integer.parseInt(credits);													
						}
						catch(Exception e) {
							logger.warn("Credits not an int: '"+credits+"'");
						}
						mi.semester = semester;
						ci.modules.put(modulecode, mi);
						if (compulsory)
							ci.compulsory[stage].add(modulecode);
						else if (alternative)
							ci.alternative[stage].add(modulecode);
						else
							ci.restricted[stage].add(modulecode);

					}
				}
			}
			else 
			{
				String text = ModuleInfo.replaceEntityRefs(current.getPureBodyText()).trim();
				logger.info("found line: "+text);
				if (text.equals(COMPULSORY_TEXT)) {
					compulsory = true;
					logger.info("Compulsory");
				}
				else if (text.equals(RESTRICTED_TEXT)) {
					compulsory = false;
					alternative = false;
					logger.info("Restricted");
				}
				else if (text.equals(ALTERNATIVE_TEXT)) {
					compulsory = false;
					alternative = true;
					logger.info("Alternative");
				}
			}
			// TODO
		}
	}
	static final String LEARNING_OUTCOMES_ELEMENT = "B";

	public static final String LEARNING_OUTCOMES_TEXT = "Section D. Learning Outcomes";
	public static final String LEARNING_OUTCOMES_SECTION_TEXTS [] = new String[] {
		"Knowledge and Understanding", "Intellectual Skills", "Professional/Practical Skills", "Transferable/Key Skills"
	};
	// teacing and learning
	// assessment
	static void processLearningOutcomes(CourseInfo ci, HtmlDocument page) throws java.io.IOException  {
		HtmlDocument.Node current = null;
		HtmlDocument.Node heading = page.getElementNodeStartingWith(page.getRootNode(), LEARNING_OUTCOMES_ELEMENT, LEARNING_OUTCOMES_TEXT);
		if (heading==null)
			throw new IOException("Couldn't find learning outcome section");
		/* <TR><TD colspan="2"><font size=+1><B>Section D. Learning Outcomes</B></font></TD></TR>
		 * <TR><TD>&nbsp;</TD>
		 * <TR><TD>&nbsp;</TD><TD>
		 * 		<TABLE border="0" width="100%">
		 * 			<TR><TD>The following learning outcomes refer to the BSc (Hons) course. Students who transfer to the BSc (Ord) course will be expected to meet most, but not all, of these outcomes, and will generally require a lower level of achievement.</TD></TR>
		 * 			<TR><TD><B>Knowledge and Understanding</B></TD></TR>
		 * 			<TR><TD>Graduates should demonstrate knowledge and understanding of</TD></TR>
		 * 			<TR><TD><LI>The theory of programming</LI></TD></TR>
		 * 			<TR><TD><LI>The practice of...
		 */ 
		HtmlDocument.Node table = null;
		try {
			table = heading.parent.parent.parent.
			getNextSibling().getNextSibling().
			getChildElements(COLUMN_ELEMENT).get(1).getChildElements(TABLE_ELEMENT).firstElement();
		}
		catch (Exception e) {
			throw new IOException("Couldn't find learning outcome value table");
		}
		Vector<HtmlDocument.Node> rows = table.getChildElements(ROW_ELEMENT);
		int section = -1;
		int itemNumber = 0;
		HtmlDocument.Node row = rows.firstElement();
		while (section < NUM_OUTCOME_SECTIONS) {
			// next?
			String rowText = null;
			try {
				rowText = row.getChildElements(COLUMN_ELEMENT).firstElement().getBodyText();
			} catch (Exception e) {
				logger.warn("Could not find cell/text on row "+row);
			}
			if (rowText.startsWith("<B>")) {
				if (section+1<NUM_OUTCOME_SECTIONS && 
						rowText.substring(3).startsWith(LEARNING_OUTCOMES_SECTION_TEXTS[section+1])) {
					section++;
					itemNumber = 0;
				}
				else {
					logger.warn("quit outcomes at unknown section "+rowText);
					break;
				}
			} else if (rowText.startsWith("<LI>")) {
				if (section >= 0)
				{
					String text = rowText.replaceAll("</?LI>","").trim();
					Matcher m = Pattern.compile("([ABCD][0-9]+)[.]*[ \\t]+(.*)").matcher(text);
					if (m.matches()) {
						Outcome outcome = new Outcome();
						outcome.id = m.group(1);
						outcome.text = m.group(2).trim();
						ci.outcomeSections[section].outcomes.add(outcome);
					} else if (text.startsWith("<B>COMPUTER SCIENCE") || text.startsWith("<b>COMPUTER SCIENCE") || text.startsWith("COMPUTER SCIENCE")) {
						// CS/Maths special case handling
						// <B>COMPUTER SCIENCE</B><BR>Students should be able to:<BR>    D1	solve problems<BR>  D2	utilise mathematics
						String textLower = text.toLowerCase();
						int ix = textLower.indexOf("<br>");
						while (ix>=0) {
							int nix = textLower.indexOf("<br>", ix+4);
							String item = nix>ix ? text.substring(ix+4,nix).trim() : text.substring(ix+4).trim();
							if (item.length()>0) {
								Outcome outcome = new Outcome();
								m = Pattern.compile("([ABCD][0-9]+)[.]*[ \\t]+(.*)").matcher(item);
								if (m.matches()) {
									outcome.id = m.group(1);
									outcome.text = m.group(2).trim();
									itemNumber++;
								} 
								else {
									itemNumber++;
									outcome.id = ""+(char)('A'+section)+itemNumber;
									outcome.text = item;
								}
								ci.outcomeSections[section].outcomes.add(outcome);
							}
							ix = nix;
						}
						
					} else if (text.startsWith("<B>MATHEMATICS") || text.startsWith("<b>MATHEMATICS")|| text.startsWith("MATHEMATICS")) {
						// CS/Maths special case handling
						// ignore
					} else {
						Outcome outcome = new Outcome();
						itemNumber++;
						outcome.id = ""+(char)('A'+section)+itemNumber;
						outcome.text = text;
						ci.outcomeSections[section].outcomes.add(outcome);
					}
				}
			} else if (section>=0 && ci.outcomeSections[section].preface==null)
				ci.outcomeSections[section].preface = rowText;
			// next row
			row = row.getNextSibling();
			if (row==null)
				break;
		}
	}
}
