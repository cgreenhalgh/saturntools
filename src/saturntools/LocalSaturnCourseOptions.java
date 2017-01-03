/** Based on (my) cswbtools.SaturnModuleReader.
 * 
 */
package saturntools;

import org.apache.log4j.Logger;

import saturntools.SortModulesByGroup.ModuleComparator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.StringTokenizer;

/** Possible Modules choice options for course(s).
 * Variant to read from local files.
 * Currently runs out of heap space.
 * 
 * @author cmg
 *
 */
public class LocalSaturnCourseOptions {
	/** logger */
    static Logger logger = Logger.getLogger(LocalSaturnCourseOptions.class);
	/** main */
	public static void main(String [] args) {
		if (args.length<4) {
			System.err.println("Usage: LocalSaturnCourseOptions <module-summary.csv> <course-credits.csv> <module-tags.csv> <course-page.html> ...");
			System.exit(-1);
		}
		try {
			HashMap<String,HashMap<String,String>> modulemap = ReadCsvFile.readCsvFile(new File(args[0]), "modulecode", false);
			HashMap<String,HashMap<String,String>> coursecredits = ReadCsvFile.readCsvFile(new File(args[1]), "coursecode", false);
			HashMap<String,HashMap<String,String>> moduletags = ReadCsvFile.readCsvFile(new File(args[2]), "modulecode", false);
			LinkedList<HashMap<String,String>> modules = new LinkedList<HashMap<String,String>>(modulemap.values());
			java.util.Collections.sort(modules, new GroupModuleComparator(false));
			LinkedList<String> modulecodes = new LinkedList<String>();
			for (HashMap<String,String> module : modules) {
				modulecodes.add(module.get(MODULECODE_NAME));
			}
			//java.util.Collections.sort(modulecodes);
			String moduleUse [][] = new String[modulecodes.size()][];
			for (int i=0; i<modulecodes.size(); i++)  {
				moduleUse[i] = new String[args.length];
				moduleUse[i][0] = modulecodes.get(i);
				for (int argi=1; argi<args.length; argi++) {
					moduleUse[i][argi] = "";
				}
			}			
			CourseInfo courseInfos[] = new CourseInfo[args.length-3];
			for (int argi=3; argi<args.length; argi++) {
				CourseInfo ci = null;
				try {
					ci = CourseInfo.processCourseFile(args[argi]);
					for (int i=0; i<ci.compulsory.length; i++)
						for (String compulsory : ci.compulsory[i]) {
							int ix = modulecodes.indexOf(compulsory);
							if (ix<0)
								logger.warn("Could not find "+compulsory+" in module-summary");
							else
								moduleUse[ix][argi] = moduleUse[ix][argi]+"C"+(ci.mscflag ? (4+i) : (1+i)+" ");
						}
					for (int i=0; i<ci.alternative.length; i++)
						for (String modulecode: ci.alternative[i]) {
							int ix = modulecodes.indexOf(modulecode);
							if (ix<0)
								logger.warn("Could not find "+modulecode+" in module-summary");
							else
								moduleUse[ix][argi] = moduleUse[ix][argi]+"A"+(ci.mscflag ? (4+i) : (1+i)+" ");
						}
					for (int i=0; i<ci.restricted.length; i++)
						for (String modulecode: ci.restricted[i]) {
							int ix = modulecodes.indexOf(modulecode);
							if (ix<0)
								logger.warn("Could not find "+modulecode+" in module-summary");
							else
								moduleUse[ix][argi] = moduleUse[ix][argi]+"O"+(ci.mscflag ? (4+i) : (1+i)+" ");
						}
					//System.out.println(mi.modulecode+","+mi.year+","+mi.moduletitle+","+(!mi.availability ? "Suspended" : "")+","+mi.convenor);
				} catch (Exception e) {
					logger.error("Processing course page "+args[argi], e);
				}
				if (ci!=null) {
					// TODO
					courseInfos[argi-3] = ci;
				}
			}
			System.out.print("coursecode,stage0autumn,stage0spring,stage1autumn,stage1spring,stage2autumn,stage2spring,stage3autumn,stage3spring,stage4autumn,stage4spring,stage4summer");
			TreeSet<String> tags = new TreeSet<String>();
			if (moduletags.size()>0) {
				tags = new TreeSet<String>(moduletags.values().iterator().next().keySet());
				tags.remove(MODULECODE_NAME);
				tags.remove("linenumber");
				tags.remove("");
				for (String tag : tags) {
					System.out.print(","+tag);
				}
			}	
			//TreeSet<String> modulecodes = new TreeSet<String>(modulemap.keySet());
			for (String modulecode : modulecodes) {
				System.out.print(","+modulecode);
			}
			System.out.println();
				//	"modulecode,status,moduletitle,newgroup,credits,prerequisites,level,semester");
			int numSemesters = Semesters.values().length;
			for (int i=0; i<courseInfos.length; i++) {
				CourseInfo ci = courseInfos[i];
				logger.info("Try course "+ci.coursecode);
				int stageMin[] = new int[NUM_STAGES];
				int stageMax[] = new int[NUM_STAGES];
				HashMap<String,String> cc = coursecredits.get(ci.coursecode);
				if (cc==null) {
					logger.error("No entry for course "+ci.coursecode+" in course_credits.csv");
					continue;
				}
				boolean error = false;
				for (int si=0; si<NUM_STAGES; si++) {
					try {
						stageMin[si] = Integer.parseInt(cc.get("stage"+si+"min"));
					}
					catch (Exception e) {
						logger.error("Error with credit value for course "+ci.coursecode+" stage "+si+" min", e);
						error = true;
					}
					try {
						stageMax[si] = Integer.parseInt(cc.get("stage"+si+"max"));
					}
					catch (Exception e) {
						logger.error("Error with credit value for course "+ci.coursecode+" stage "+si+" max", e);
						error = true;
					}
				}
				Option o = new Option(modulecodes.size());
				LinkedList<Option> options = new LinkedList<Option>();
				LinkedList<Option> optionsToDo = new LinkedList<Option>();
				optionsToDo.add(o);
				nextoption:
				while(optionsToDo.size()!=0) {
					o = optionsToDo.removeFirst();
					// can we add to the current semester?
					int si = o.currentSemester.ordinal();
					int stage = si/2;
					int semesterMin = stage!=5 ? stageMin[stage]/2 : stageMin[stage];
					int semesterMax = stage!=5 ? stageMax[stage]/2 : stageMax[stage];
					// compulsory
					// (not foundation)
					if (stage>=1 && o.stageCredits[stage]==0) {
						if (stage-1<ci.compulsory.length) {
							// add all compulsory modules to current stage
							for (String cmodulecode : ci.compulsory[stage-1]) {
								if (cmodulecode.length()==0)
									continue;
								HashMap<String,String> moduleinfo = modulemap.get(cmodulecode);
								if (moduleinfo==null) {
									logger.error("Unknown compulsory module '"+cmodulecode+"'");
									continue;
								}
								String semester = moduleinfo.get(SEMESTER_NAME);
								int credits = 0;
								try {
									credits = Integer.parseInt(moduleinfo.get(CREDITS_NAME));
								}
								catch (Exception e) {
									logger.error("Module "+cmodulecode+" credits invalid");
									continue;
								}
								Semesters sem = o.currentSemester;
								if ("spring".equalsIgnoreCase(semester))
									sem = Semesters.values()[sem.ordinal()+1];
								boolean fullyear = "full year".equalsIgnoreCase(semester);
								o.stageCredits[stage] += credits;
								if (fullyear)
									credits = credits/2;
								int mi = modulecodes.indexOf(cmodulecode);
								if (mi<=0) {
									logger.error("Compulsory module "+cmodulecode+" not in summary");
									continue;
								}
								if (o.choice[mi]!=0) {
									logger.error("Compulsory module "+cmodulecode+" at "+stage+" already taken at "+o.choice[mi]);
									// give up on this option
									continue nextoption;
								}
								o.choice[mi] = (byte)stage;
								o.semesterCredits[sem.ordinal()] += credits;
								if (fullyear)
									o.semesterCredits[sem.ordinal()+1] += credits;
							}
							//logger.info("Added compulsories ("+ci.compulsory[stage-1]+")");
							//o.dump(System.err, modulecodes);
						}
						else {
							// TODO project
							if (stage<5)
								logger.warn("At stage "+stage+" - not enough compulsory stages in course");
						}
					}
					// TODO: 50/70 & 70/50 split(s)
					if (o.semesterCredits[si]>=semesterMin) {
						// valid option
						Option o2 = o.copy();
						if (si+1>=numSemesters) { 
							// done
//							options.add(o2);
//							logger.info("found one!");
//							o2.dump(System.err, modulecodes);
							System.out.print(ci.coursecode);
							for (int ii=0; ii<Semesters.values().length; ii++)
								System.out.print(","+o.semesterCredits[ii]);
							for (int mi=0; mi<modulecodes.size(); mi++) {
								if (o.choice[mi]!=0)
									System.out.print(","+o.choice[mi]);
								else
									System.out.print(",");
							}
							System.out.println();
						}
						else
						{
							o2.currentSemester = Semesters.values()[si+1];
							o2.moduleIndex = 0;
//							logger.info("next semester ("+o2.currentSemester+")");
//							o2.dump(System.err, modulecodes);
							optionsToDo.addFirst(o2);
						}
					}
					if (o.semesterCredits[si]>=semesterMax) {
						// no way we can do anything with this - discard
						continue;
					}

					// add all possible modules to the current semester...
					boolean added = false;
					nextmodule:
					for (int mi=o.moduleIndex; mi<modulecodes.size(); mi++) {
						String modulecode = modulecodes.get(mi);
						// not foundation?!
						if (stage==0)
							continue;
						if ((stage-1>ci.alternative.length || !ci.alternative[stage-1].contains(modulecode)) &&
								(stage-1>ci.restricted.length || !ci.restricted[stage-1].contains(modulecode)))
							// not an option
							continue;
						HashMap<String,String> moduleinfo = modulemap.get(modulecode);
						if (o.choice[mi]!=0)
							// done already
							continue;
						int credits = 0;
						try {
							credits = Integer.parseInt(moduleinfo.get(CREDITS_NAME));
							if (o.semesterCredits[si]+credits>semesterMax)
								// too many credits
								continue;
						}
						catch (Exception e) {
							logger.error("Module "+modulecode+" credits invalid");
							continue;
						}
						// satisfies pre-requisistes?
						String prerequisites = moduleinfo.get(PREREQUISITES_NAME);
						StringTokenizer toks = new StringTokenizer(prerequisites, " ");
						while(toks.hasMoreTokens()) {
							String tok = toks.nextToken();
							int pi = modulecodes.indexOf(tok);
							if (pi<0)
								logger.error("Unknown prerequisite '"+tok+"' of "+modulecode);
							else
								if (o.choice[pi]==0) {
//									logger.info("Missing prereq '"+tok+"' for "+modulecode);
//									o.dump(System.err,modulecodes);
									// not taken
									continue nextmodule;
								}
						}		
						// TODO: does or could satisfy pre-requisites?
						// TODO: credits at level at stage
						// make as option
						Option o2 = o.copy();
						o2.choice[mi] = (byte)stage;
						o2.semesterCredits[si] += credits;
						o2.stageCredits[stage] += credits;
						o2.moduleIndex = mi+1;
						optionsToDo.addFirst(o2);
						added = true;
						//logger.info("# todo: "+optionsToDo.size()+" (semester "+si+")");
					}
					if (!added) {
//						logger.info("No more options available at "+o.currentSemester);
//						o.dump(System.err,modulecodes);
//						if (stage>0 && stage-1<ci.alternative.length)
//							System.err.println("Alternative: "+ci.alternative[stage-1]);
//						if (stage>0 && stage-1<ci.restricted.length)
//							System.err.println("Restricted: "+ci.restricted[stage-1]);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Reading saturn", e);
		}
	}
	
	// partial option
	static class Option {
		byte choice[]; // index by modulecode index
		int stageCredits[]; // index by stage 0-5
		int semesterCredits[]; // index by Semesters
		Semesters currentSemester;
		int moduleIndex; // next module to try in this semester
		Option(int nummodulecodes) {
			choice = new byte[nummodulecodes];
			stageCredits = new int[NUM_STAGES];
			semesterCredits = new int[Semesters.values().length];
			currentSemester = Semesters.stage0autumn;
		}
		Option copy() {
			Option o = new Option(choice.length);
			System.arraycopy(this.stageCredits, 0, o.stageCredits, 0, this.stageCredits.length);
			System.arraycopy(this.choice, 0, o.choice, 0, this.choice.length);
			System.arraycopy(this.semesterCredits, 0, o.semesterCredits, 0, this.semesterCredits.length);
			o.currentSemester = this.currentSemester;
			o.moduleIndex = this.moduleIndex;
			return o;
		}
		void dump(PrintStream ps, LinkedList<String> modulecodes) {
			ps.print("credits: ");
			for (int i=0; i<semesterCredits.length; i++)
				ps.print(semesterCredits[i]+" ");
			ps.print("module: ");
			for (int i=0; i<choice.length; i++)
				if (choice[i]!=0)
					ps.print(modulecodes.get(i)+"("+choice[i]+") ");
			// ...?
		}
	}
	// foundation, ..., msc
	static final int NUM_STAGES = 6;
	static enum Stages {foundation,qualifying,part1,part2,msc,dissertation};
	// as per csv headings in course_credits.csv
	static enum Semesters { stage0autumn,stage0spring,stage1autumn,stage1spring,stage2autumn,stage2spring,stage3autumn,stage3spring,stage4autumn,stage4spring,stage4summer };
	public static final String LEVEL_NAME = "level";
	public static final String STATUS_NAME = "status";
	public static final String CREDITS_NAME = "credits";
	public static final String SEMESTER_NAME = "semester";
	public static final String MODULECODE_NAME = "modulecode";
	public static final String MODULETITLE_NAME = "moduletitle";
	public static final String NEWGROUP_NAME = "newgroup";
	public static final String PREREQUISITES_NAME = "prerequisites";
	public static final String COREQUISITES_NAME = "corequisites";

}
