/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import saturntools.ProcessSaturnExportMarks.Distribution;
import saturntools.ProcessSaturnExportMarks.Mark;

/** Do some basic checks of a (new-style) marks spreadsheet,
 * e.g. blanks, borderlines, missing student IDs
 * 
 * @author cmg
 *
 */
public class CheckMarksSpreadsheet {
	
	private static final int HEADING_ROW = 6;
	private static final String STUDENT_ID = "Student ID";
	private static final String OVERALL_MARK = "Overall Mark";
	private static final String SURNAME = "Surname";
	private static final String FORENAME = "Forename";
	private static final String COURSE_TITLE = "Course Title";
	private static final String YEAR_ON_COURSE = "Year on Crs";
	private static final String MNEMONIC = "Mnemonic :";
	private static final String YEAR = "Year :";
	private static final String ROUNDED = "Rounded";
	private static final String RESULT = "Result";
	/** column number of ? num of assessment elements in row 0 */
	private static final short COLUMN_NUM_ELEMENTS = 3;
	/** column number of year code in row 0 */
	private static final short COLUMN_YEAR = 1;
	private static final int YEAR_ZERO = 100; // year code 000109 = 2009
	private static final double CODE_ABS = -1;
	private static final double CODE_EC = -2;
	private static final double CODE_PLAG = -4;
	private static final String REPORT_FILE_SUFFIX = ".report.txt";
	private static final String RESIT_SUFFIX = "_resit";

	public static final String [] MSC_COURSE_NAMES = new String [] { 
		"Information Technology", 
		"Management of Information Technology", 
		"Interactive Systems Design",
		"Advanced Computing Science",
		"Scientific Computation",
		// not PGT, but hey ho
		"Digital Economy Horizon DTC",
		"Postgraduate - No Award" };	

	private static DecimalFormat df2 = new DecimalFormat("00");
	private static DecimalFormat df2p = new DecimalFormat("0.00");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length<1) {
			System.err.println("Usage: <marks-spreadsheet.xls> ...");
			System.exit(-1);
		}
		//BufferedReader consolein = new BufferedReader(new InputStreamReader(System.in));
		for (int ai=0; ai<args.length; ai++) {
			File f = new File(args[ai]);
			checkFile(f);
		}
	}
	public static List<Mark> checkFile(File f) {
		try {
			File outputDir = FileUtils.getOutputDirectory();
			Workbook wb = null;
			InputStream inp = null;
			try {
				// repeat with option of different passwords...
				while(true) {
					inp = new FileInputStream(f);
					try {
						String pw = PasswordUtils.getPassword(f);
						Biff8EncryptionKey.setCurrentUserPassword(pw);
						//InputStream inp = new FileInputStream("workbook.xlsx");
						wb = WorkbookFactory.create(inp);
						// ok
						break;
					} catch (EncryptedDocumentException ee) {
						PasswordUtils.forget(f);
						// try again
						continue;
					}
				}
			}
			catch (Exception e) {
				System.err.println("Error: Unable to read "+f+": "+e);
				e.printStackTrace(System.err);
				return null;
			}
			File reportfile = new File(outputDir, f.getName()+REPORT_FILE_SUFFIX);
			System.err.println("Writing report for "+f+" to "+reportfile);
			PrintWriter report = new PrintWriter(new FileWriter(reportfile));
			
			report.println("Marks Spreadsheet report");
			report.println("Reading marks file "+f);
			report.println("Date: "+new Date());
				
			report.println();
			
		    Sheet sheet = wb.getSheetAt(0);
		    //System.out.println("Sheet: "+sheet);
		    //System.out.println("FirstRow: "+sheet.getFirstRowNum());
		    //System.out.println("LastRow: "+sheet.getLastRowNum());
		    //System.out.println("LeftCol: "+sheet.getLeftCol());
		    Row hr = sheet.getRow(HEADING_ROW);
		    /*System.out.println("Heading row ("+hr.getFirstCellNum()+"-"+hr.getLastCellNum()+"):");
		    for (int i=hr.getFirstCellNum(); i<hr.getLastCellNum(); i++) {
		    	Cell c = hr.getCell(i);
		    	System.out.println("Heading cell "+i+" = "+(c!=null ? "type "+c.getCellType() : "null")+": "+(c!=null && c.getCellType()==Cell.CELL_TYPE_STRING ? c.getStringCellValue() : (c!=null && c.getCellType()==Cell.CELL_TYPE_NUMERIC ? ""+c.getNumericCellValue() : "")));
		    }*/
		    int studentidColumn = findColumn(hr, STUDENT_ID);
		    int surnameColumn = findColumn(hr, SURNAME);
		    int forenameColumn = findColumn(hr, FORENAME);
		    int markColumn = findColumn(hr, OVERALL_MARK);
		    int roundedColumn = findColumn(hr, ROUNDED);
		    int resultColumn = findColumn(hr, RESULT);
		    int courseTitleColumn = findColumn(hr, COURSE_TITLE);
		    int yearOnCourseColumn = findColumn(hr, YEAR_ON_COURSE);
		    //System.out.println("studentidColumn="+studentidColumn);
		    
	    	int nblank = 0;
	    	int nborder = 0;
	    	int nstudents = 0;
	    	int nzero = 0;
	    	int nabsentzero = 0;
	    	
	    	String module = null;
	    	String syear = null;

	    	// row 0 has various information in, e.g.:
	    	//cell 0 = type 1: 019275
	    	//cell 1 = type 1: 000109 - year
	    	//cell 2 = type 1: 504902
	    	//cell 3 = type 0: 2.0 - number of assessment elements??
	    	//cell 4 = type 1: 504789
	    	//cell 5 = type 1: 900950
	    	int year = 0;
	    	int numElements = 0; // unknown
	    	Row r0 = sheet.getRow(0);
	    	if (r0!=null) {
	    		// year code from row 0
	    		String yearcode = getString(r0, COLUMN_YEAR);
	    		if (yearcode!=null) {
	    			try {
	    				year = Integer.parseInt(yearcode)-YEAR_ZERO;
	    			}
	    			catch (Exception e) {
	    				report.println("Error: hidden year code not valid (expecting 0001nn): "+yearcode);
	    			}
	    		}
	    		// num elements? (guessing...) from row 0
	    		Double dnumElements = getNumber(r0, COLUMN_NUM_ELEMENTS);
	    		if (dnumElements!=null) {
    				numElements = dnumElements.intValue();
    				if (numElements<0 || numElements>5) {
    					report.println("Header assumed number of assessment elements is not reasonable ("+numElements+")");
    					numElements = 0;
	    			}
	    		}
	    		/*for (int i=r0.getFirstCellNum(); i<r0.getLastCellNum(); i++) {
			    	Cell c = r0.getCell(i);
			    	System.out.println("Row0 cell "+i+" = "+(c!=null ? "type "+c.getCellType() : "null")+": "+(c!=null && c.getCellType()==Cell.CELL_TYPE_STRING ? c.getStringCellValue() : (c!=null && c.getCellType()==Cell.CELL_TYPE_NUMERIC ? ""+c.getNumericCellValue() : "")));
			    }*/	    		
	    	}
    		if (year==0)
    			report.println("Error: hidden year code not found");
    		
		    // check element mark columns
		    Vector<Integer> elementMarkColumns = new Vector<Integer>();
		    Vector<Double> elementMarkWeights = new Vector<Double>();
		    while(numElements==0 || elementMarkColumns.size()<numElements) {
		    	int col = resultColumn+2*elementMarkColumns.size()+1;
		    	if (col+1>=hr.getLastCellNum() || hr.getCell(col)==null || hr.getCell(col+1)==null || hr.getCell(col).getCellType()!=Cell.CELL_TYPE_STRING || hr.getCell(col+1).getCellType()!=Cell.CELL_TYPE_NUMERIC) {
		    		// not an element mark
		    		if (elementMarkColumns.size()<numElements) {
		    			report.println("Warning: Could not find heading(s) for assessment element "+(elementMarkColumns.size()+1));
		    		} else if (numElements==0 && elementMarkColumns.size()==0) {
		    			report.println("Warning: Could not find any apparent assessment element headings");
		    		} else 
		    			// (elementMarkColumns.size()>=numElements)
		    			break;		    			
		    	}
		    	else {
		    		// probably element mark
		    		if (elementMarkColumns.size()>=numElements && numElements>0) {
		    			report.println("Warning: possible additional element heading "+hr.getCell(col).getStringCellValue()+" vs header count of "+numElements);
		    			break;
		    		}
		    		// add
		    		elementMarkColumns.add(col);
		    		elementMarkWeights.add(hr.getCell(col+1).getNumericCellValue());
		    	}
		    }		    
	    	
	    	for (int ri=1; ri<HEADING_ROW; ri++) {
		    	Row row = sheet.getRow(ri);
		    	if (row==null) {
		    		report.println("Row "+ri+" null");
		    		continue;
		    	}
	    		String info = getString(row, 0);
	    		if (info==null)
	    			continue;
	    		if (info.startsWith(MNEMONIC))
	    			module = info.substring(MNEMONIC.length()).trim().toUpperCase();
	    		if (info.startsWith(YEAR))
	    			syear = info.substring(YEAR.length()).trim();
	    	}
	    	
	    	if (module==null) 
	    		report.println("Error: module code not found in file header in "+f);
	    	else {
		    	String filename = f.getName();
		    	int eix = filename.lastIndexOf(".");
		    	if (eix>=0)
		    		filename = filename.substring(0, eix).toUpperCase();
		    	if (filename.endsWith(RESIT_SUFFIX.toUpperCase()))
		    		filename = filename.substring(0, filename.length()-RESIT_SUFFIX.length());
		    	if (!module.equals(filename)) {
		    		if (filename.startsWith(module)) 
		    			report.println("Note: module "+module+" data in file with non-standard name "+f);
		    		else
		    			report.println("Error: module "+module+" data found in file "+f);
		    	}
	    	}
	    	if (syear==null)
	    		report.println("Error: year not found in file header in "+f);
	    	else {
	    		String ystr = df2.format(year)+"/"+df2.format(year+1);
	    		if (!ystr.equals(syear))
	    			report.println("Error: year "+syear+" date found in year "+year+" file "+f);
	    	}
	    	
	    	List<Mark> marks = new LinkedList<Mark>();
	    	Distribution d = new Distribution(10,0,10);
	    	Distribution mscd = new Distribution(10,0,10);
	    	Distribution nonmscd = new Distribution(10,0,10);
	    	Distribution overallVsRounded = new Distribution(10,0,10);
	    	TreeSet<String> courseTitles = new TreeSet<String>();
	    	HashMap<String,Distribution> courseDistributions = new HashMap<String,Distribution>();
	    	Distribution elementds[] = new Distribution[elementMarkColumns.size()];
	    	for (int i=0;i<elementds.length; i++)
	    		elementds[i] = new Distribution(10, 0, 10);
	    	int nelementscount[] = new int[elementMarkColumns.size()+1];
	    	Distribution allemenentsd = new Distribution(10,0,10);
	    	
	    	int ri;
	    	for (ri=HEADING_ROW+1; ri<sheet.getLastRowNum(); ri++) {
	    		try {
	    			Row row = sheet.getRow(ri);
	    			if (row==null) {
	    				report.println("Row "+ri+" null");
	    				continue;
	    			}
	    			String studentid = getString(row, studentidColumn);
	    			//System.out.println("Row "+ri+", studentid="+studentid);
	    			String surname = getString(row, surnameColumn);
	    			String forename = getString(row, forenameColumn);
	    			String courseTitle = getString(row, courseTitleColumn);
	    			Integer mark = null;
	    			try {
	    				Double dmark = getNumber(row, markColumn);
	    				if (dmark!=null)
	    					mark = (int)Math.round(dmark);
	    			}
	    			catch (Exception e) {
	    				report.println("Error: overall mark is not number or blank for student "+studentid+" (row "+(ri+1)+")");
	    				continue;
	    			}
	    			
	    			if (studentid==null && surname==null && forename==null && mark==null) 
	    				// blank?!
	    				break;

	    			if (studentid==null) {
	    				report.println("Error: Missing studentid at row "+(ri+1)+" ("+surname+", "+forename+")");
	    			}

	    			String yearOnCourse = null;
	    			try {
	    				Double yoc = getNumber(row, yearOnCourseColumn);
	    				yearOnCourse = ""+(int)Math.round(yoc);
	    			}
	    			catch (Exception e) {
	    				report.println("Error: year on course is not number or blank for student "+studentid+" (row "+(ri+1)+")");
	    			}

	    			// checks...
	    			boolean allElementsAbsent = true; 
	    			boolean suspectedPlagiarism = false;
	    			boolean anyElementMissing = false;
	    			int nelements = 0;
	    			for (int ci=0; ci<elementMarkColumns.size(); ci++) {
	    				int col = elementMarkColumns.get(ci);
	    				Double elementMark = getNumber(row, col);
	    				if (elementMark==null) {
	    					report.println("Error: missing element mark ("+getString(hr,col)+") for student "+studentid+" (row "+(ri+1)+")");
	    					anyElementMissing = true;
	    				}
	    				else if(elementMark>100 || (elementMark<0 && elementMark!=CODE_ABS && elementMark!=CODE_EC && elementMark!=CODE_PLAG)) {
	    					report.println("Error: invalid element mark "+elementMark+" ("+getString(hr, col)+") for student "+studentid+" (row "+(ri+1)+")");
	    				}
	    				else if (elementMark>=0) {
	    					allElementsAbsent = false;
	    					elementds[ci].add(elementMark);
	    					nelements++;
	    				} else if (elementMark==CODE_PLAG)
	    					suspectedPlagiarism = true;
	    			}
	    			// count students attempting no. elements
	    			nelementscount[nelements]++;
	    			
	    			// overall mark, unless allElementsAbsent or suspectedPlagiarism
	    			if (mark!=null && allElementsAbsent) 
	    				report.println("Error: overall mark should be blank (vs "+mark+") where all elements absent for student "+studentid+" (row "+(ri+1)+")");
	    			else if (mark!=null && suspectedPlagiarism)
	    				report.println("Error: overall mark should not be included when suspected plagiarism for student "+studentid+" (row "+(ri+1)+")");
	    			else if (mark==null && !allElementsAbsent && !suspectedPlagiarism)
	    				report.println("Error: overall mark missing for student "+studentid+" (row "+(ri+1)+")");
	    			else if (mark!=null && mark==0 && !allElementsAbsent) {
	    				// not necessarily an error, but uncommon/noteworthy
	    				report.println("Note: overal mark of 0 for student who attended at least one assessment for student "+studentid+" (row "+(ri+1)+")");
	    			}
	    			if (mark!=null && mark>100) {
	    				report.println("Error: overall mark is too high for student "+studentid+" ("+mark+")"+" (row "+(ri+1)+")");
	    			}
	    			else if (mark!=null && mark<0) {
	    				report.println("Error: overall mark is too low for student "+studentid+" ("+mark+")"+" (row "+(ri+1)+")");
	    			}
	    			else if (mark!=null && (mark==29 || mark==39 || mark==49 || mark==59 || mark==69)) {
	    				report.println("Error: borderline mark for student "+studentid+" ("+mark+")"+" (row "+(ri+1)+")");
	    			}
	    			
	    			// overall vs rounded
	    			Double rounded = getNumber(row, roundedColumn);
	    			if (mark!=null && rounded!=null)
	    				overallVsRounded.add(mark-rounded);
	    			
	    			// counts...
	    			if (mark==null)
	    				nblank ++;
	    			else if (mark==29 || mark==39 || mark==49 || mark==59 || mark==69) {
	    				nborder ++;
	    			} else if (mark==0) {
	    				nzero ++;
		    			if (allElementsAbsent)
		    				nabsentzero++;
	    			}
	    			nstudents ++;

	    			Mark m = new Mark();
	    			m.student = studentid;
	    			m.mark = mark==null ? null : (int)Math.round(mark);
	    			m.year = year;
	    			m.module = module;
	    			m.fromSpreadsheet = true;
	    			m.msc = courseIsMSc(courseTitle);
	    			m.yearOnCourse = yearOnCourse;
	    			m.courseTitle = courseTitle;
	    			marks.add(m);

	    			if (mark!=null && (mark!=0 || !allElementsAbsent)) {
	    				d.add(mark);
	    				if (courseTitle!=null) {
	    					courseTitles.add(courseTitle);
	    					Distribution cd = courseDistributions.get(courseTitle);
	    					if (cd==null) {
	    						cd = new Distribution(10, 0, 10);
	    						courseDistributions.put(courseTitle, cd);
	    					}
	    					cd.add(mark);	    					
	    				}
	    				if (m.msc==Boolean.TRUE)
	    					mscd.add(mark);
	    				else if (m.msc==Boolean.FALSE)
	    					nonmscd.add(mark);
	    				// students completing all elements
		    			if (nelements==elementMarkColumns.size())
		    				allemenentsd.add(mark);
	    			}
	    		}
	    		catch (Exception e) {
	    			report.println("Error at row "+(ri+1)+": "+e);
	    			System.err.println("Error at row "+(ri+1)+": "+e);
	    			e.printStackTrace(System.err);
	    		}	    		
		    }
	    	// erroneous blank line?
	    	//System.out.println("Checking for extra rows from "+ri+" to "+sheet.getLastRowNum()+"...");
	    	for (; ri<sheet.getLastRowNum(); ri++) {
	    		Row row = sheet.getRow(ri);
	    		if (row==null)
	    			continue;
	    		if (row.getFirstCellNum()<=studentidColumn && studentidColumn<row.getLastCellNum() && row.getCell(studentidColumn)!=null) {
	    			Cell c= row.getCell(studentidColumn);
	    			//System.out.println("row "+ri+", cell = "+c);
	    			String val = null;
	    			if (c.getCellType()==Cell.CELL_TYPE_STRING) 
	    				val = c.getStringCellValue();
	    			else if (c.getCellType()==Cell.CELL_TYPE_NUMERIC) {
	    				double dval = c.getNumericCellValue();
	    				if (dval==(double)Math.round(dval))
	    					val = ""+Math.round(dval);
	    			}

	    			if (val!=null && val.length()==7 && Character.isDigit(val.charAt(0))) {
	    				report.println("Warning: possible student(s) after blank row(s) at row "+(ri+1));
	    				break;
	    			}
	    			else if (val!=null)
	    				report.println("Ignore extra value "+val);
	    		
	    		}
	    	}
	    	if (d.mean()>62.5 || d.meanNozero()<57.5) {
	    		if (d.n<20) 
	    			report.println("Warning: mean out of range (small numbers)");
	    		else
	    			report.println("Warning: mean out of range");
	    	}

	    	if (overallVsRounded.sd()!=0 && overallVsRounded.sd()!=Double.NaN) {
	    		report.println("Warning: overall marks differ from rounded by "+df2p.format(overallVsRounded.mean())+" SD "+df2p.format(overallVsRounded.sd())+" (min "+df2p.format(overallVsRounded.min)+", max "+df2p.format(overallVsRounded.max)+")");
	    	}

	    	report.println();
	    	
	    	if (nblank!=0)
	    		report.println("Note: "+nblank+" blank marks\n");
	    	if (nabsentzero!=0)
	    		report.println("Note: "+nabsentzero+" zero marks due to absence (not included below)\n");
	    	
	    	report.println("Summary for "+f);
	    	report.println("Module: "+module);
	    	if (year==0)
	    		report.println("Year: unknown");
	    	else
	    		report.println("Year: "+df2.format(year)+"/"+df2.format(year+1));
	    	//report.println("Note: "+nstudents+" students in "+f);
	    		
	    	printSummary(report, d, nabsentzero);
	    //	if (nzero!=0)
	    	//reprt.println("Note: "+nzero+" zero marks in "+f);
	    	if (nborder!=0)
	    		report.println("Warning: "+nborder+" borderline marks in "+f);
	    	
	    	if (elementMarkColumns.size()>1) {
	    		report.println();
	    		report.println("Number of elements of assessment attempted (out of "+elementMarkColumns.size()+"):");
	    		for (int i=0; i<nelementscount.length; i++) {
	    			report.println(i+": "+nelementscount[i]);
	    		}
	    		for (int i=0; i<elementds.length; i++) {
		    		report.println();
		    		report.println("Marks for assesment element "+(i+1)+" ("+hr.getCell(elementMarkColumns.get(i)).getStringCellValue()+"), weighted "+df2.format(100*elementMarkWeights.get(i))+"%:");
		    		printSummary(report, elementds[i], 0);
	    		}
	    		if (nelementscount[nelementscount.length-1]>0) {
	    			report.println();
	    			report.println("Students attempted all elements of assessment only:");
		    		report.println();
	    			report.println("Delta-mean vs whole class: "+df2p.format(allemenentsd.meanNozero()-d.meanNozero()));
	    			printSummary(report, allemenentsd, 0);
	    		}
	    	}
	    	
    		report.println();
	    	if (mscd.n==0) {
	    		report.println("No PGT students on this module");	    		
	    	}
	    	else if (nonmscd.n>0){
	    		report.println("PGT Students only ("+mscd.n+"):");
	    		report.println();
    			report.println("Delta-mean vs whole class: "+df2p.format(mscd.meanNozero()-d.meanNozero()));
    			printSummary(report, mscd, 0);
	    	}	    	
    		report.println();
	    	if (nonmscd.n==0) {
	    		report.println("No UG students on this module");	    		
	    	}
	    	else if (mscd.n>0){
	    		report.println("UG Students only ("+nonmscd.n+"):");
	    		report.println();
    			report.println("Delta-mean vs whole class: "+df2p.format(nonmscd.meanNozero()-d.meanNozero()));
    			printSummary(report, nonmscd, 0);
	    	}	    	
	    	for (String courseTitle : courseTitles) {
    			Distribution cd = courseDistributions.get(courseTitle);
	    		if (cd!=null) {
	    			report.println();
	    			report.println("For \""+courseTitle+"\" students only ("+cd.n+"):");
	    			report.println();
	    			report.println("Delta-mean vs whole class: "+df2p.format(cd.meanNozero()-d.meanNozero()));
	    			printSummary(report, courseDistributions.get(courseTitle), 0);
	    		}
	    	}
	    	
	    	inp.close();
	    	report.close();
			return marks;
		}
		catch (Exception e) {
			System.err.println("Error processing "+f+": "+e);
			e.printStackTrace(System.err);
			return null;
		}
	}
	private static Boolean courseIsMSc(String courseTitle) {
		for (int i=0; i<MSC_COURSE_NAMES.length; i++)
			if (MSC_COURSE_NAMES[i].equals(courseTitle))
				return true;
		return false;
	}
	private static void printSummary(PrintWriter report, Distribution d,
			int nabsentzero) {
    	report.println("N   : "+d.n+(nabsentzero!=0 ? " (excludes "+nabsentzero+" absent)" : ""));
    	report.println("Mean: "+df2p.format(d.mean()));
    	report.println("SD  : "+df2p.format(d.sd()));
    	report.println("Min : "+df2p.format(d.min));
    	report.println("0.05: "+df2p.format(d.getQuantile(0.05)));
    	report.println("0.25: "+df2p.format(d.getQuantile(0.25)));
    	report.println("0.5 : "+df2p.format(d.getQuantile(0.5)));
    	report.println("0.75: "+df2p.format(d.getQuantile(0.75)));
    	report.println("0.95: "+df2p.format(d.getQuantile(0.95)));
    	report.println("Max : "+df2p.format(d.max));
    	report.println("Zero: "+d.nzero+(nabsentzero!=0 ? " (excludes "+nabsentzero+" absent)" : ""));
    	report.println("Mean(no zero): "+df2p.format(d.meanNozero()));
    	report.println("SD  (no zero): "+df2p.format(d.sdNozero()));
		
    	if (d.n>0) {
	    	for (int i=0; i<d.nbuckets; i++) {
	    		report.println(i+"x  :\t"+d.bucketn[i]+"\t"+((int)(100*d.bucketn[i]/d.n))+"%\t"+bar(d.bucketn[i],d.n));
	    	}
    	}
	}
	private static final int MAX_BAR = 40;
	private static String bar(int count, int max) {
		int n = (int)Math.ceil(1.0*MAX_BAR*count/max);
		StringBuffer sb = new StringBuffer();
		sb.append("|");
		int i=0;
		for (; i<n; i++)
			sb.append("#");
		for (; i<MAX_BAR; i++)
			sb.append(" ");
		sb.append("|");
		return sb.toString();
	}
	private static String getString(Row row, int column) {
		if (row.getLastCellNum()<column)
			return null;
		Cell cell =row.getCell(column);
		if (cell==null || cell.getCellType()==Cell.CELL_TYPE_BLANK)
			return null;
		if (cell.getCellType()==Cell.CELL_TYPE_STRING || (cell.getCellType()==Cell.CELL_TYPE_FORMULA && cell.getCachedFormulaResultType()==Cell.CELL_TYPE_STRING)) {
			String value = cell.getStringCellValue().trim(); 
			if (value.length()==0)
				return null;
			return value;
		}
		System.err.println("Warning: cell "+column+" not string: "+cell);
		return null;
	}
	private static Double getNumber(Row row, int column) {
		if (row.getLastCellNum()<column)
			return null;
		Cell cell =row.getCell(column);
		if (cell==null || cell.getCellType()==Cell.CELL_TYPE_BLANK)
			return null;
		if (cell.getCellType()==Cell.CELL_TYPE_NUMERIC || (cell.getCellType()==Cell.CELL_TYPE_FORMULA && cell.getCachedFormulaResultType()==Cell.CELL_TYPE_NUMERIC))
			return cell.getNumericCellValue();		
		if (cell.getCellType()==Cell.CELL_TYPE_STRING || (cell.getCellType()==Cell.CELL_TYPE_FORMULA && cell.getCachedFormulaResultType()==Cell.CELL_TYPE_STRING)) {
			String value = cell.getStringCellValue().trim(); 
			if (value.length()==0)
				return null;
		}
		throw new RuntimeException("Cell "+column+" not number (or blank or formula): "+cell);
		//return null;
	}
	private static int findColumn(Row row, String name) {
		for (int ci=0; ci<row.getLastCellNum(); ci++) {
    		Cell cell = row.getCell(ci);
    		if (cell!=null && cell.getCellType()==Cell.CELL_TYPE_STRING && name.equals(cell.getStringCellValue()))
    			return ci;
		}
		throw new RuntimeException("No column '"+name+"' found in row "+row);
	}

}
