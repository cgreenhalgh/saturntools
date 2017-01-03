/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** First app to process module/student marks exported from SaturnWeb.
 * 
 * @author cmg
 *
 */
public class ProcessSaturnExportMarks {

	// for modules3 export
	public static final String STUDENT_ID = "StudentID"; // modules2: "Student ID";
	public static final String YEAR = "Session"; // modules2: "Year";
	public static final String MODULE_MNEM = "Module Code"; // modules2: "Module Mnem";
	public static final String OVERALL_MARK = "Overall Mark";
	public static final String SECOND_ATTEMPT = "Second Attempt";
	public static final String CREDIT = "Credits"; // modules2 "Credit";
	public static final String RESIT = "Resit";
	public static final String SEMESTER = "Semester";
	public static final String YEAR_OF_COURSE = "Year of Course";
	public static final String UCASE_COURSE = "Ucas Course"; // no course title in export modules-3
	
	public static final String []  MODULE_PREFIXES = new String [] { "G5", "G6" };
	
	public static final double minMean = 57.5;
	public static final double maxMean = 62.5;
	public static final int MIN_SIZE = 20;
	private static final Object EXCEL_FILE_EXTENSION = "xls";
	private static final double MAX_DIFF_OK = 4;
	public static final int MIN_SIZE_DIFF = 5;
	
	static class Mark {
		String student;
		String module;
		int year;
		Integer mark;
		Integer secondmark;
		int credit;
		boolean resit;
		boolean fromSpreadsheet;
		Boolean msc;
		String courseTitle;
		String yearOnCourse;
		String semester;
		@Override
		public String toString() {
			return "Mark [credit=" + credit + ", mark=" + mark + ", module="
					+ module + ", resit=" + resit + ", secondmark="
					+ secondmark + ", student=" + student + ", year=" + year
					+ "]";
		}
		public String getStudentmodule() {
			return module+"-"+student;
		}
		public Integer bestmark() {
			if (mark!=null && secondmark!=null && secondmark>mark)
				return secondmark;
			if (mark!=null)
				return mark;
			return null;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + credit;
			result = prime * result + ((mark == null) ? 0 : mark.hashCode());
			result = prime * result
					+ ((module == null) ? 0 : module.hashCode());
			result = prime * result + (resit ? 1231 : 1237);
			result = prime * result
					+ ((secondmark == null) ? 0 : secondmark.hashCode());
			result = prime * result
					+ ((student == null) ? 0 : student.hashCode());
			result = prime * result + year;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Mark other = (Mark) obj;
			if (credit != other.credit)
				return false;
			if (mark == null) {
				if (other.mark != null)
					return false;
			} else if (!mark.equals(other.mark))
				return false;
			if (module == null) {
				if (other.module != null)
					return false;
			} else if (!module.equals(other.module))
				return false;
			if (resit != other.resit)
				return false;
			if (secondmark == null) {
				if (other.secondmark != null)
					return false;
			} else if (!secondmark.equals(other.secondmark))
				return false;
			if (student == null) {
				if (other.student != null)
					return false;
			} else if (!student.equals(other.student))
				return false;
			if (year != other.year)
				return false;
			return true;
		}
		
	}
	static class Distribution {
		double sum;
		double sum2;
		int n;
		int nzero;
		double min;
		double max;
		int nbuckets;
		double bucketmin;
		double bucketsize;
		int bucketn[];
		// for detailed distribution
		double marks[];
		boolean sorted = true;
		static final int INITIAL_MARKS_LENGTH = 50;
		static final int MARKS_LENGTH_RATIO = 4;
		Distribution(int nbuckets, double bucketmin, double bucketsize) {
			this.nbuckets = nbuckets;
			this.bucketmin = bucketmin;
			this.bucketsize = bucketsize;
			bucketn = new int[nbuckets];
			marks = new double[INITIAL_MARKS_LENGTH];
		}
		void add(double v) {
			if (n>=marks.length) {
				double oldmarks[] = marks;
				marks = new double[MARKS_LENGTH_RATIO*oldmarks.length];
				System.arraycopy(oldmarks, 0, marks, 0, oldmarks.length);				
			}
			marks[n] = v;
			sorted = false;
			if (v==0)
				nzero++;
			sum += v;
			sum2 += v*v;
			n++;
			if (n<=1) {
				min = max = v;
			}
			else if (v<min)
				min = v;
			else if (v>max)
				max = v;
			int b = (int)((v-bucketmin)/bucketsize);
			if (b<0)
				b = 0;
			else if (b>=nbuckets)
				b = nbuckets-1;
			bucketn[b]++;
		}
		double mean() {
			if (n==0)
				return Double.NaN;
			return sum/n;
		}
		double meanNozero() {
			if (n-nzero==0)
				return Double.NaN;
			return sum/(n-nzero);
		}
		double sd() {
			if (n<2)
				return Double.NaN;
			double mean = mean();
			return Math.sqrt(n*1.0/(n-1)*(sum2/n-mean*mean));
		}		
		double sdNozero() {
			if (n-nzero<2)
				return Double.NaN;
			double mean = meanNozero();
			int n = this.n-this.nzero;
			return Math.sqrt(n*1.0/(n-1)*(sum2/n-mean*mean));
		}		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("n="+n+", \tmean="+mean()+", \tsd="+sd()+", \tmin="+min+", \tmax="+max);
			return sb.toString();
		}
		
		public String checkMean() {
			double m1 = mean();
			double m2 = meanNozero();			
			if ((m1<minMean && m2<minMean)) {
				if (n<MIN_SIZE)
					return "n<20 (low)";
				return "low";				
			}
			// m2 always >= m1 anyway!
			if ((m1>maxMean && m2>maxMean)) {
				if (n<MIN_SIZE)
					return "n<20 (high)";
				return "high";				
			}
			return "ok";
		}
		public void sort() {
			if (!sorted) {
				sorted = true;
				if (n==0)
					return;
				Arrays.sort(marks, 0, n);
			}
		}
		/** get a mark from part-way through the ordered distribution of marks.
		 * e.g. getDistributionMark(0.5) is the median.
		 * 
		 * @param cumulativeFraction
		 * @return
		 */
		public double getQuantile(double p) {
			sort();
			if (n==0)
				return Double.NaN;
			double i = n*p;
			if (i<=0)
				// min (not really a quantile!)
				return marks[0];
			int fi = (int)Math.floor(i);
			if (fi>=n) 
				// max - not really a quantile
				return marks[n-1];
			double di = i-fi;
			if (di<=0) {
				// exactly on the boundary of two samples?
				if (fi==0)
					// min (not really a quantile!)
					return marks[0];
				// average down one to avoid bias and meet normal definition e.g. for median
				return 0.5*(marks[fi]+marks[fi-1]);
			}
			return marks[fi];
		}
		public double getMedian() {
			return getQuantile(0.5);
		}
		public double getLowerQuartile() {
			return getQuantile(0.25);
		}
		public double getUpperQuartile() {
			return getQuantile(0.75);
		}
	}
	static class Correlation {
		public int n;
		public double sumx;
		public double sumy;
		public double sumxy;
		public double sumxx;
		public double sumyy;
		public Correlation () {}
		public void add(double x, double y) {
			n++;
			sumx += x;
			sumy += y;
			sumxy += x*y;
			sumxx += x*x;
			sumyy += y*y;
		}
		public double getR2() {
			return 2*(sumxy-sumx*sumy/n)/(sumxx-sumx*sumx/n+sumyy-sumy*sumy/n);
		}
	}
	
	public static boolean includeModule(String code) {
		for (int i=0; i<MODULE_PREFIXES.length; i++)
			if (code.startsWith(MODULE_PREFIXES[i]))
				return true;
		return false;
	}
	static DecimalFormat dfyear = new DecimalFormat("00");
	static DecimalFormat df2 = new DecimalFormat("#0.00");
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length==0)
		{
			System.err.println("Usage: <export.csv> ...");
			System.exit(-1);			
		}
		//BufferedReader consolein = new BufferedReader(new InputStreamReader(System.in));
		try {
			List<Mark> allmarks = new LinkedList<Mark>();
			File outputDir = FileUtils.getOutputDirectory();
			for (int ai=0; ai<args.length; ai++) {
				File f = new File(args[ai]);
				// marks spreadsheet? if excel...
				if (EXCEL_FILE_EXTENSION.equals(getFileExtension(f))) {
					List<Mark> marks = CheckMarksSpreadsheet.checkFile(f);
					if (marks!=null)
						allmarks.addAll(marks);
				}
				else
					// assume saturn modules3 export
					allmarks.addAll(readMarks(f));				
			}
			discardDuplicateMarks(allmarks);
			
			Map<String,ProcessSaturnExportStudents.Student> studentinfos = new HashMap<String,ProcessSaturnExportStudents.Student>();
			TreeSet<String> students = new TreeSet<String>();
			TreeSet<String> modules = new TreeSet<String>();
//			TreeSet<String> modulemarkids = new TreeSet<String>();
			TreeSet<Integer> years = new TreeSet<Integer>();
			TreeSet<Integer> credits = new TreeSet<Integer>();
			for (Mark m : allmarks) {
				if (m.student!=null)
					students.add(m.student);
				else
					System.err.println("Warning: mark with no student: "+m);
				if (m.module!=null)
					modules.add(m.module);
				else
					System.err.println("Warning: mark with no module: "+m);
//				if (includeModule(m.module)) {
//					if (m.mark!=null)
//						modulemarkids.add(getModuleMarkID(m.module,m.year,false));
//					if (m.secondmark!=null)
//						modulemarkids.add(getModuleMarkID(m.module,m.year,true));
//				}
				years.add(m.year);
				credits.add(m.credit);
				if (!studentinfos.containsKey(m.student)) {
					ProcessSaturnExportStudents.Student si = new ProcessSaturnExportStudents.Student();
					si.id = m.student;
					si.courseTitle = m.courseTitle;
					si.yearOfCourse = ""+m.yearOnCourse;
					studentinfos.put(si.id, si);
				}
			}
			System.out.println("Found "+students.size()+" students");
			System.out.println("Found "+modules.size()+" modules");
			System.out.println("Found "+years.size()+" years: "+years);
			System.out.println("Found "+credits.size()+" credit weightings: "+credits);
			
			// for each year, for each module, form distribution of all students best mark on that module
			// Note: this is superceded by the following section which includes per-student checks
			
			for (int year : years) {
				String syear = dfyear.format(year)+dfyear.format(year+1);
				File fout = new File(outputDir, "out_modules_"+syear+".csv");
				System.out.println("Output module distributions for "+syear+" to "+fout);
				PrintWriter pw = new PrintWriter(new FileWriter(fout));
				//System.out.println("Module distributions for year "+year+":");
				pw.println("#Module distributions for year "+year+"/"+(year+1)+" generated "+(new Date()));
				pw.print("module,status,n,mean,sd,meanNozero,sdNozero,min,Q.05,Q.25,median,Q.75,Q.95,max,zero,marks...");
				//for (int i=0; i<10; i++) 
				//	pw.print(",\""+i+"x\"");
				pw.println();
				for (String module : modules) {
					if (!includeModule(module))
						continue;
					Distribution d = new Distribution(10, 0, 10);
					// accumulate best mark
					for (Mark m : allmarks)
						if (m.year==year && m.module.equals(module) && m.bestmark()!=null)
							d.add(m.bestmark());
					if (d.n==0)
						continue;
					try {
						//System.out.println(module+": "+d);
						pw.print(module+","+d.checkMean()+","+d.n+","+df2.format(d.mean())+","+df2.format(d.sd())+","+df2.format(d.meanNozero())+","+df2.format(d.sdNozero()));
						pw.print(","+df2.format(d.min)+","+df2.format(d.getQuantile(0.05))+","+df2.format(d.getLowerQuartile())+","+df2.format(d.getMedian())+","+df2.format(d.getUpperQuartile())+","+df2.format(d.getQuantile(0.95))+","+df2.format(d.max)+","+d.nzero);
						for (int i=0; i<d.n; i++) 
							pw.print(","+df2.format((d.marks[i])));
						pw.println();						
					}
					catch (Exception e) {
						System.out.println("Cannot produce distribution for "+module+": "+e);
					}
				}
				pw.close();
			}
			
			// first organise Marks by student
			Map<String,List<Mark>> studentmarks = getMarksByStudent(allmarks);
			
			// output all marks for each student
			for (int year : years) {
				String syear = dfyear.format(year)+dfyear.format(year+1);
				File fout = new File(outputDir, "out_student_allmarks_"+syear+".csv");
				System.out.println("Output student all marks for "+syear+" to "+fout);
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fout)));
				pw.print("student,course,yearOnCourse");
				pw.print(",nmarks-ex0,mean,min,Q.25,Q.5,Q.75,max");
//				for (String modulemarkid : modulemarkids) {
//					if (!modulemarkid.contains(syear))
//						continue;
//					pw.print(","+modulemarkid);
//				}
				for (String modulecode : modules){
					if (includeModule(modulecode)) {
						pw.print(","+getModuleMarkID(modulecode, false));
						pw.print(","+getModuleMarkID(modulecode, true));
					}
				}
				pw.println();
				for (String sid : students) {
					ProcessSaturnExportStudents.Student si = studentinfos.get(sid);
					pw.print(si.id+","+si.courseTitle+","+si.yearOfCourse);
					List<Mark> smarks = studentmarks.get(sid);
					if (smarks==null)
						continue;
					Distribution sd = new Distribution(10,0,10);
					for (Mark m : smarks) {
						if (m.bestmark()!=null && m.bestmark()!=0 && includeModule(m.module))
							sd.add(m.bestmark());
					}
					pw.print(","+sd.n+","+format(sd.mean())+","+format(sd.min)+","+format(sd.getLowerQuartile())+","+format(sd.getMedian())+","+format(sd.getUpperQuartile())+","+format(sd.max));
					// some overall information about the student:
					// 
					nextmodule:
//					for (String modulemarkid : modulemarkids) {
//						if (!modulemarkid.contains(syear))
//							continue;
					for (String modulecode : modules){
						if (!includeModule(modulecode))
							continue;
						for (Mark m : smarks) {
							if (!modulecode.equals(m.module))
								continue;
							//							String mid = getModuleMarkID(m.module, false);
							//							if (mid.equals(modulemarkid)) {
							if (m.mark!=null)
								pw.print(","+m.mark);
							else
								pw.print(",");
							//								continue nextmodule;
							//							}
							//							mid = getModuleMarkID(m.module, m.year, true);
							//							if (mid.equals(modulemarkid)) {
							if (m.secondmark!=null)
								pw.print(","+m.secondmark);
							else
								pw.print(",");
							continue nextmodule;
//							}
						}
						// nope
						pw.print(",");
						pw.print(",");
					}					
					pw.println();
				}
				pw.close();
			}
			// for each year, for each module, for distribution of difference between students mark on this module and students mark on (a) another module taken in that year (b) another module whatever year it was taken in
			for (int year : years) {
				String syear = dfyear.format(year)+dfyear.format(year+1);
				File fout = new File(outputDir, "out_modules_delta_"+syear+".csv");
				System.out.println("Output module delta distributions for year "+syear+" to "+fout);
				PrintWriter pw = new PrintWriter(new FileWriter(fout));
				//System.out.println("Module distributions for year "+year+":");
				pw.println("#Module delta distributions for year "+syear+" generated "+(new Date()));
				pw.print("module,mean-status,diff-status,n-spreadsheet,n,mean,sd,meanNozero,sdNozero,min,max,zero,");
				pw.print("n-diff-sameyear,mean-diff-sameyear,sd-diff-sameyear,r2-sameyear,r2-pm-sameyear,n-diff-anyyear,mean-diff-anyyear,sd-diff-anyyear,r2-anyyear,r2-pm-anyyear");
				pw.print(",msc-n,msc-mean,msc-sd,msc-meanNozero,msc-sdNozero,msc-min,msc-max,msc-zero");
				pw.print(",nonmsc-n,nonmsc-mean,nonmsc-sd,nonmsc-meanNozero,nonmsc-sdNozero,nonmsc-min,nonmsc-max,nonmsc-zero");
				pw.println();
				for (String module : modules) {
					if (!includeModule(module))
						continue;
					Distribution d = new Distribution(10, 0, 10);
					Distribution mscd = new Distribution(10, 0, 10);
					Distribution nonmscd = new Distribution(10, 0, 10);
					Distribution dsameyear = new Distribution(20, -100, 10);
					Distribution danyyear = new Distribution(20, -100, 10);
					// per mark pair
					Correlation csameyear = new Correlation();
					Correlation canyyear = new Correlation();
					// per student
					Correlation csameyearps = new Correlation();
					Correlation canyyearps = new Correlation();
					// number from spreadsheet(s)
					int nspreadsheet = 0;
					
					for (String sid : students) {
						int nanyyear = 0, nsameyear = 0;
						double sumanyyear = 0, sumsameyear = 0;

						List<Mark> smarks = studentmarks.get(sid);
						if (smarks==null)
							continue;
						Mark mm = null;
						for (Mark m : smarks)
							if (m.module.equals(module) && m.year==year) {
								mm = m;
								break;
							}
						if (mm==null)
							// didn't take this
							continue;
						if (mm.bestmark()==null)
							// haven't got a mark for it
							continue;

						// basic distribution
						d.add(mm.bestmark());

						if (mm.msc!=null) {
							if (mm.msc)
								mscd.add(mm.bestmark());
							else
								nonmscd.add(mm.bestmark());
						}
						
						if (mm.fromSpreadsheet)
							nspreadsheet++;

						if (mm.bestmark()==0)
							// ignore zeros for comparison
							continue;
						
						for (Mark m : smarks) {
							if (m==mm || m.module.equals(module))
								// same module
								continue;
							if (m.bestmark()==null)
								// no mark
								continue;
							if (m.bestmark()==0)
								// ignore zeros for comparison
								continue;
							double diff = m.bestmark()-mm.bestmark();
							// (b)
							//Do vs average per student, later: danyyear.add(diff);
							canyyear.add(m.bestmark(),mm.bestmark());
							nanyyear++;
							sumanyyear += m.bestmark();
							if (m.year==mm.year) {
								// (a)
								//Do vs average per student, later: dsameyear.add(diff);
								csameyear.add(m.bestmark(),mm.bestmark());
								nsameyear++;
								sumsameyear += m.bestmark();
							}
							check(dsameyear.n <= danyyear.n, "dsameyear.n <= danyyear.n");
						}
						if (nsameyear>0) {
							double meansameyear = sumsameyear/nsameyear;
							csameyearps.add(mm.bestmark(), meansameyear);
							dsameyear.add(meansameyear-mm.bestmark());
						}
						if (nanyyear>0) {
							double meananyyear = sumanyyear/nanyyear;
							canyyearps.add(mm.bestmark(), meananyyear);
							danyyear.add(meananyyear-mm.bestmark());
						}
					}
					
					if (d.n==0)
						continue;
					check(dsameyear.n <= danyyear.n, "dsameyear.n <= danyyear.n");
					try {
						//System.out.println(module+": "+d);
						pw.print(module+",");
						// mean 'status'
						pw.print(d.checkMean()+",");
						// diff 'status'					
						if (Math.abs(dsameyear.mean())>MAX_DIFF_OK) 
							pw.print((dsameyear.mean()<0 ? "high" : "low")+(dsameyear.n<MIN_SIZE_DIFF ? " (nd<"+MIN_SIZE_DIFF+")" : "")+",");
						else
							pw.print("ok,");
						pw.print(nspreadsheet+","+d.n+","+df2.format(d.mean())+","+df2.format(d.sd())+","+df2.format(d.meanNozero())+","+df2.format(d.sdNozero())+","+d.min+","+d.max+","+d.nzero+",");
						pw.print(dsameyear.n+","+df2.format(dsameyear.mean())+","+df2.format(dsameyear.sd())+","+df2.format(csameyearps.getR2())+","+df2.format(csameyear.getR2())+","+danyyear.n+","+df2.format(danyyear.mean())+","+df2.format(danyyear.sd())+","+df2.format(canyyearps.getR2())+","+df2.format(canyyear.getR2()));
						pw.print(","+mscd.n+","+df2.format(mscd.mean())+","+df2.format(mscd.sd())+","+df2.format(mscd.meanNozero())+","+df2.format(mscd.sdNozero())+","+mscd.min+","+mscd.max+","+mscd.nzero);
						pw.print(","+nonmscd.n+","+df2.format(nonmscd.mean())+","+df2.format(nonmscd.sd())+","+df2.format(nonmscd.meanNozero())+","+df2.format(nonmscd.sdNozero())+","+nonmscd.min+","+nonmscd.max+","+nonmscd.nzero);
						pw.println();						
					}
					catch (Exception e) {
						System.out.println("Cannot produce distribution for "+module+": "+e);
					}
				}
				pw.close();
			}
			// all of the module-to-module comparison aswell for good measure...
			
			// for each year, for each module, for each other module, distribution of difference between students mark on the two modules 
			for (int year : years) {
				String syear = dfyear.format(year)+dfyear.format(year+1);
				File fout = new File(outputDir, "out_modules_compare_"+syear+".csv");
				System.out.println("Output module comparative module distributions for "+syear+" to "+fout);
				PrintWriter pw = new PrintWriter(new FileWriter(fout));
				//System.out.println("Module distributions for year "+year+":");
				pw.println("#Module compare distributions for year "+syear+" generated "+(new Date()));
				pw.print("module1,module2,");
				pw.print("n-d,mean-d,sd-d,r2");
				pw.print(",min,Q.05,Q.25,median,Q.75,Q.95,max");
				pw.print(",msc-n-d,msc-mean-d,msc-sd-d");
				pw.print(",nonmsc-n-d,nonmsc-mean-d,nonmsc-sd-d");
				pw.println();
				for (String module : modules) {
					if (!includeModule(module))
						continue;
					// distributions per module2
					Map<String,Distribution> dms = new HashMap<String,Distribution>();
					Map<String,Distribution> mscdms = new HashMap<String,Distribution>();
					Map<String,Distribution> nonmscdms = new HashMap<String,Distribution>();
					Map<String,Correlation> cms = new HashMap<String,Correlation>();

					for (String sid : students) {
						List<Mark> smarks = studentmarks.get(sid);
						if (smarks==null)
							continue;
						Mark mm = null;
						for (Mark m : smarks)
							if (m.module.equals(module) && m.year==year) {
								mm = m;
								break;
							}
						if (mm==null)
							// didn't take this
							continue;
						if (mm.bestmark()==null)
							// haven't got a mark for it
							continue;

						if (mm.bestmark()==0)
							// ignore zeros for comparison
							continue;

						for (Mark m : smarks) {
							if (m.bestmark()==null)
								// haven't got a mark for it
								continue;

							if (m.bestmark()==0)
								// ignore zeros for comparison
								continue;
							
							double diff = m.bestmark()-mm.bestmark();
							
							Distribution dm = dms.get(m.module);
							if (dm==null) {
								dm = new Distribution(10, 0, 10);
								dms.put(m.module, dm);
							}
							if (m.msc==Boolean.TRUE || mm.msc==Boolean.TRUE) {
								Distribution mscdm = mscdms.get(m.module);
								if (mscdm==null) {
									mscdm = new Distribution(10, 0, 10);
									mscdms.put(m.module, mscdm);
								}
								mscdm.add(diff);								
							} else if (m.msc==Boolean.FALSE || mm.msc==Boolean.FALSE) {
								Distribution mscdm = nonmscdms.get(m.module);
								if (mscdm==null) {
									mscdm = new Distribution(10, 0, 10);
									nonmscdms.put(m.module, mscdm);
								}
								mscdm.add(diff);								
							} 
							Correlation cm = cms.get(m.module);
							if (cm==null) {
								cm = new Correlation();
								cms.put(m.module, cm);
							}
							
							dm.add(diff);
							cm.add(mm.bestmark(),m.bestmark());
						}
					}// student
					for (String module2 : modules) {
						if (!includeModule(module2))
							continue;
						Distribution dm = dms.get(module2);
						if (dm==null)
							continue;
						// per mark pair
						Correlation cm = cms.get(module2);
						Distribution mscdm = mscdms.get(module2);
						Distribution nonmscdm = nonmscdms.get(module2);

						try {
							//System.out.println(module+": "+d);
							pw.print(module+","+module2+","+dm.n+","+df2.format(dm.mean())+","+df2.format(dm.sd())+",");
							pw.print(df2.format(cm.getR2()));
							pw.print(","+df2.format(dm.min)+","+df2.format(dm.getQuantile(0.05))+","+df2.format(dm.getQuantile(0.25))+","+df2.format(dm.getQuantile(0.5))+","+df2.format(dm.getQuantile(0.75))+","+df2.format(dm.getQuantile(0.95))+","+df2.format(dm.max));
							if (mscdm!=null)
								pw.print(","+mscdm.n+","+df2.format(mscdm.mean())+","+df2.format(mscdm.sd()));
							else
								pw.print(",,,");
							if (nonmscdm!=null)
								pw.print(","+nonmscdm.n+","+df2.format(nonmscdm.mean())+","+df2.format(nonmscdm.sd()));
							else
								pw.print(",,,");
							pw.println();						
						}
						catch (Exception e) {
							System.out.println("Cannot produce distribution for "+module+": "+e);
						}
					}
				}
				pw.close();
			}
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}
	private static String format(double d) {
		if (Double.isNaN(d) || Double.isInfinite(d))
			return "NA";
		String s = df2.format(d);
		if ("?".equals(s))
			System.err.println("Warning: formatting "+d+" -> "+s);
		return s;
	}
	private static String getModuleMarkID(String module, int year, boolean resit) {
		return module+"_"+dfyear.format(year)+dfyear.format(year+1)+(resit? "_resit" :"");
	}
	private static String getModuleMarkID(String module, boolean resit) {
		return module+(resit? "_resit" :"");
	}
	private static String getFileExtension(File f) {
		String fname = f.getName();
		int ix = fname.lastIndexOf(".");
		if (ix<0)
			return null;
		return fname.substring(ix+1);
	}
	private static void check(boolean b, String msg) {
		// TODO Auto-generated method stub
		if (!b)
			throw new RuntimeException("Check failed: "+msg);		
	}
	static Map<String, List<Mark>> getMarksByStudent(List<Mark> allmarks) {
		Map<String,List<Mark>> studentmarks = new HashMap<String,List<Mark>>();
		for (Mark m : allmarks) {
			String sid = m.student;
			List<Mark> smarks = studentmarks.get(sid);
			if (smarks==null)
			{
				smarks = new LinkedList<Mark>();
				studentmarks.put(sid, smarks);
			}
			smarks.add(m);
		}
		return studentmarks;
	}
	static void discardDuplicateMarks(List<Mark> allmarks) {
		// find multiple marks for the same module
		List<Mark> discardmarks = new LinkedList<Mark>();
		Map<String,Mark> studentmodule = new HashMap<String,Mark>();
		int nomark = 0;
		for (Mark m : allmarks) {
//			if (m.bestmark()==null)
//			{
//				discardmarks.add(m);
//				nomark ++;
//				continue;
//			}
			String sm = m.getStudentmodule();
			if (studentmodule.containsKey(sm)) {
				Mark m2 = studentmodule.get(sm);

				// update credits (missing in spreadsheet?)
				if (m.fromSpreadsheet && m2.credit>0) {
					m.credit = m2.credit;
					m.resit = m2.resit;
					m.semester = m2.semester;
				}
				else if (m2.fromSpreadsheet && m.credit>0) {
					m2.credit = m.credit;
					m2.resit = m.resit;
					m2.semester = m.semester;
				}

				if (m.mark!=m2.mark || m.secondmark!=m2.secondmark) {
					if (m.fromSpreadsheet && !m2.fromSpreadsheet) {
						if ((m2.mark!=null && m.mark!=m2.mark) || m2.secondmark!=null) {
							System.err.println("Warning: spreadsheet mark differs from export mark: "+m+" vs "+m2);
						}
					}
					else if (!m.fromSpreadsheet && m2.fromSpreadsheet) {
						if ((m.mark!=null && m.mark!=m2.mark) || m.secondmark!=null) {
							System.err.println("Warning: spreadsheet mark differs from export mark: "+m2+" vs "+m);
						}						
					}
					else if ((m2.mark==null && m.mark!=null) || (m2.mark==m.mark && m2.secondmark==null)) {
						// should be ok - more information?!
						//System.err.println("Warning: non-identical repeat marks: "+m+" vs "+m2);
					}
					else
						System.err.println("Warning: non-identical repeat marks: "+m+" vs "+m2);
				}
				// we do get some not marked resit - not sure why!
				//if ((!m.resit || !m2.resit) && (!m.fromSpreadsheet && !m2.fromSpreadsheet))
				//	System.err.println("Warning: repeat marks not resits: "+m+" vs "+m2);

				//System.out.println("Repeat Mark: "+m+" vs "+m2);
				if ((!m.fromSpreadsheet && m2.fromSpreadsheet)) {
					// keep old mark
					discardmarks.add(m);
				}
				else {
					// replace with new mark
					studentmodule.put(sm, m);
					discardmarks.add(m2);
				}
			}
			else
				studentmodule.put(sm, m);
		}
		System.out.println("Discarding "+discardmarks.size()+" duplicate and unknown Marks (of which "+nomark+" were unknown marks)");
		for (Mark m : discardmarks)
			allmarks.remove(m);
		
	}
	static List<Mark> readMarks(File f) throws IOException {
		List<HashMap<String,String>> marks = ReadCsvFile.readCsvFile(f);
		System.out.println("Read "+marks.size()+" entries from "+f);
		LinkedList<Mark> res = new LinkedList<Mark>();
		
		for (int i=0; i<marks.size(); i++) {
			HashMap<String,String> mark = marks.get(i);
			Mark m = new Mark();
			if (!mark.containsKey(STUDENT_ID)) {
				System.err.println("Ignore "+f+" mark "+i+": no student id ("+mark+")");
				continue;
			}
			m.student = mark.get(STUDENT_ID);
			String year = mark.get(YEAR);
			if (year==null) {
				System.err.println("Ignore "+f+" mark "+i+": no year ("+mark+")");
				continue;				
			}
			int ix = year.indexOf("/");
			try {
				m.year = Integer.parseInt(year.substring(0,ix));				
			}
			catch (Exception e) {
				System.err.println("Ignore "+f+" mark "+i+": badly formed year ("+mark+")");
				continue;								
			}
			m.module = mark.get(MODULE_MNEM);
			if (m.module==null) {
				System.err.println("Ignore "+f+" mark "+i+": no module mnem ("+mark+")");
				continue;								
			}
			String smark = mark.get(OVERALL_MARK);
			if (smark!=null && smark.length()>0) {
				try {
					m.mark = Integer.parseInt(smark);
				}
				catch (Exception e) {
					System.err.println("Ignore "+f+" mark "+i+": badly formed overall mark ("+mark+")");
					continue;								
				}
			}
			String smark2 = mark.get(SECOND_ATTEMPT);
			if (smark2!=null && smark2.length()>0) {
				try {
					m.secondmark = Integer.parseInt(smark2);
				}
				catch (Exception e) {
					System.err.println("Ignore "+f+" mark "+i+": badly formed second attempt ("+mark+")");
					continue;								
				}
			}
			String credit = mark.get(CREDIT);
			if (credit==null) {
				System.err.println("Ignore "+f+" mark "+i+": no credit ("+mark+")");
				continue;				
			}
			try {
				m.credit = Integer.parseInt(credit.trim());				
			}
			catch (Exception e) {
				System.err.println("Ignore "+f+" mark "+i+": badly formed credit ("+mark+")");
				continue;								
			}
			m.resit = "Y".equals(mark.get(RESIT));
			m.yearOnCourse = mark.get(YEAR_OF_COURSE);
			m.courseTitle = mark.get(UCASE_COURSE);
			m.semester = mark.get(SEMESTER);
			res.add(m);
		}
		return res;
	}

}
