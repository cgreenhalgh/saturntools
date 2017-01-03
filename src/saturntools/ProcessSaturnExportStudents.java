/**
 * 
 */
package saturntools;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import saturntools.ProcessSaturnExportMarks.Mark;

/** Process various saturn exports (schoollist, student advanced search ug/pg further details, modules3 for multiple years)
 * and try to work out what each student has done.
 * 
 * 2011-01-09 test: 
 * Modes of study: [Full time, Part time]
 * Qualifications: [BSc (Ordinary), BSc Hons, BSc Jt Hons, MPhil, MRes, MSc, MSci Hons, No Qual (PG), No Qual (UG), PhD]
 * Years of course: [1, 2, 3, 4, 5]
 * 
 * Some more notes:
 * - year on course is 3 for final year UNNC students, while saturn only has marks for 2 years (i.e. those in the UK)
 * - year on course has been 2 for an Ord student in final year (1 example, several show 3); not sure why, perhaps reflects when they were transfered to ord.
 * - year on course can be less than number of years for which we have marks for students transfering in from another school 
 *   e.g. their first year in that other school may be present. ditto for foundation students, where foundation year is visible.
 * - don't know what direct entry to part I students would show
 * 
 * - total credits after qualifying year in each degree band matter due to boundary rule
 * - in a stage, credits 40+, 3x and <30 matter for UG; 50+, 4x, 3x and <30 matter for PGT.
 * 
 * - we don't normally allow carrying credits, so each year is usually completely allocated to a particular stage.
 * - with ord. students they may have 'too many' credits on the books because of transfer from honours, and best must be found/taken.
 * 
 * - in general UNQF rules should allow allocation of years to stages. (can't trust PT/FT status as that might have changed during study)
 * 
 * @author cmg
 *
 */
public class ProcessSaturnExportStudents {

	private static final String VERSION_STRING = "1.0 2010-02-04 11:40";


	// further details column headings
	public static String FD_DEGREE_QUAL_AIMS = "Degree qual aim";
	public static String FD_COURSE_CODE = "Course code";
	public static String FD_YEAR_OF_COURSE = "Year of course";
	public static String FD_MODE_OF_STUDY = "Mode of study";
	public static String FD_SURNAME = "surname";
	public static String FD_FIRST_NAMES = "First names";
	public static String FD_REGISTERED ="Registered";
	public static String FD_DATE_OF_ENTRY = "Date of entry";
	public static String MOS_FULL_TIME = "Full time";
	public static String FD_DEGREE_TITLE = "Degree Title";

	/** heuristic to identify MSc dissertations */
	private static final int DISSERTATION_CREDITS = 60;
	private static final String DISSERTATION_SEMESTER = "Summer";

	public static class Student {
		public String id;
		public String qualification;
		public String courseCode;
		public String courseTitle;
		public String yearOfCourse;
		public String modeOfStudy;
		public String surname;
		public String firstNames;
		public boolean registered;
		public String dateOfEntry;
		public String attendanceStatus;
		public String specialStudentStatus;
		public String email;
		/** cons */
		public Student() {			
		}
		@Override
		public String toString() {
			return "Student [courseCode=" + courseCode + ", courseTitle="
					+ courseTitle + ", dateOfEntry=" + dateOfEntry
					+ ", firstNames=" + firstNames + ", id=" + id
					+ ", modeOfStudy=" + modeOfStudy + ", qualification="
					+ qualification + ", registered=" + registered
					+ ", surname=" + surname + ", yearOfCourse=" + yearOfCourse
					+ "]";
		}
	}
	public static class MarkProfile {
		boolean includesResits;
		boolean hasResits;
		int credits;
		int c70=0, c60=0, c50 = 0, c40 = 0, c30 = 0, cfail = 0;
		double average;
		public MarkProfile() {}
		public void add(MarkProfile mp) {
			if (mp.hasResits)
				hasResits = true;
			credits += mp.credits;
			c70 += mp.c70;
			c60 += mp.c60;
			c50 += mp.c50;
			c40 += mp.c40;
			c30 += mp.c30;
			cfail += mp.cfail;
		}
	}
	
	
	public static class Qualification {
		public String title;
		public String names[];
		public String fullNames[];
		public boolean postgraduate;// ie pass mark of 50% rather than 40%
		public Requirement[] requirements; // overall
		public Stage[] stages;
		public ClassificationRule[] classificationRules;
		public String classificationAlternatives[];
		public String progressionAlternatives[];
		/** special case for Ordinary taking best of part I & 2 (optional); also other 'fallback' awards such as PG Diploma */
		public Requirement combinedClassificationRequirements[];
		/** special case for 'fallback' awards such as PG Diploma which have their own requirements for eligibility */
		public ProgressionRule combinedProgressionRrequirements[];
		public Qualification() {}
		public Qualification(String title, String[] names, String fullNames[],
				boolean postgraduate, Requirement[] requirements,
				Stage[] stages, ClassificationRule[] classificationRules) {
			super();
			this.title = title;
			this.names = names;
			this.fullNames = fullNames;
			this.postgraduate = postgraduate;
			this.requirements = requirements;
			this.stages = stages;
			this.classificationRules = classificationRules;
		}
		public Qualification(String title, String[] names, String fullNames[],
				boolean postgraduate, Requirement[] requirements,
				Stage[] stages, ClassificationRule[] classificationRules,
				String[] classificationAlternatives, String [] progressionAlternatives) {
			super();
			this.title = title;
			this.names = names;
			this.fullNames = fullNames;
			this.postgraduate = postgraduate;
			this.requirements = requirements;
			this.stages = stages;
			this.classificationRules = classificationRules;
			this.classificationAlternatives = classificationAlternatives;
			this.progressionAlternatives = progressionAlternatives;
		}
		
		public Qualification(String title, String[] names, String fullNames[],
				boolean postgraduate, Requirement[] requirements,
				Stage[] stages, ClassificationRule[] classificationRules,
				String[] classificationAlternatives,
				String[] progressionAlternatives,
				Requirement[] combinedClassificationRequirements,
				ProgressionRule[] combinedProgressionRrequirements) {
			super();
			this.title = title;
			this.names = names;
			this.fullNames = fullNames;
			this.postgraduate = postgraduate;
			this.requirements = requirements;
			this.stages = stages;
			this.classificationRules = classificationRules;
			this.classificationAlternatives = classificationAlternatives;
			this.progressionAlternatives = progressionAlternatives;
			this.combinedClassificationRequirements = combinedClassificationRequirements;
			this.combinedProgressionRrequirements = combinedProgressionRrequirements;
		}
		public Integer getPassMark() {
			if ("Taught Masters".equals(title))
				return 50;
			else
				return 40;
		}
		@Override
		public String toString() {
			return "Qualification [classificationAlternatives="
					+ Arrays.toString(classificationAlternatives)
					+ ", classificationRules="
					+ Arrays.toString(classificationRules)
					+ ", combinedClassificationRequirements="
					+ Arrays.toString(combinedClassificationRequirements)
					+ ", names=" + Arrays.toString(names) + ", postgraduate="
					+ postgraduate + ", progressionAlternatives="
					+ Arrays.toString(progressionAlternatives)
					+ ", requirements=" + Arrays.toString(requirements)
					+ ", stages=" + Arrays.toString(stages) + ", title="
					+ title + "]";
		}
	}
	public static class Stage {
		public String name;
		public Requirement[] requirements;
		public ProgressionRule progressionRules[];
		public boolean progressionExcludesResits; // MSc end of Part II!
		public boolean optional;
		public double defaultClassificationWeight; // zero for qualifying (and foundation)
//		/** MSc-specific fudge to handle combined taught/dissertation 'stage' [try two stages] */
//		public boolean progressionExcludesDissertation;
		public boolean summerDissertation;
		public String classificationAlternatives[];
		public Stage() {}
		public Stage(String name, Requirement[] requirements) {
			super();
			this.name = name;
			this.requirements = requirements;
		}
		public Stage(String name, Requirement[] requirements,
				double defaultClassificationWeight) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.defaultClassificationWeight = defaultClassificationWeight;
		}
		public Stage(String name, Requirement[] requirements, boolean optional) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.optional = optional;
		}
		public Stage(String name, Requirement[] requirements,
				ProgressionRule[] progressionRules) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.progressionRules = progressionRules;
		}
		public Stage(String name, Requirement[] requirements,
				ProgressionRule[] progressionRules, boolean optional) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.progressionRules = progressionRules;
			this.optional = optional;
		}
		public Stage(String name, Requirement[] requirements,
				ProgressionRule[] progressionRules, boolean optional,
				double defaultClassificationWeight) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.progressionRules = progressionRules;
			this.optional = optional;
			this.defaultClassificationWeight = defaultClassificationWeight;
		}
//		public Stage(String name, Requirement[] requirements,
//				ProgressionRule[] progressionRules, boolean optional,
//				double defaultClassificationWeight,
//				boolean progressionExcludesDissertation) {
//			super();
//			this.name = name;
//			this.requirements = requirements;
//			this.progressionRules = progressionRules;
//			this.optional = optional;
//			this.defaultClassificationWeight = defaultClassificationWeight;
//			this.progressionExcludesDissertation = progressionExcludesDissertation;
//		}
		public boolean affectsClassification() {
			return defaultClassificationWeight>0;
		}
		public Stage(String name, Requirement[] requirements,
				ProgressionRule[] progressionRules, boolean optional,
				double defaultClassificationWeight, boolean summerDissertation) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.progressionRules = progressionRules;
			this.optional = optional;
			this.defaultClassificationWeight = defaultClassificationWeight;
			this.summerDissertation = summerDissertation;
		}
		public Stage(String name, Requirement[] requirements,
				ProgressionRule[] progressionRules,
				boolean progressionExcludesResits, boolean optional,
				double defaultClassificationWeight, boolean summerDissertation) {
			super();
			this.name = name;
			this.requirements = requirements;
			this.progressionRules = progressionRules;
			this.progressionExcludesResits = progressionExcludesResits;
			this.optional = optional;
			this.defaultClassificationWeight = defaultClassificationWeight;
			this.summerDissertation = summerDissertation;
		}
		@Override
		public String toString() {
			return "Stage [defaultClassificationWeight="
					+ defaultClassificationWeight + ", name=" + name
					+ ", optional=" + optional + ", progressionExcludesResits="
					+ progressionExcludesResits + ", progressionRules="
					+ Arrays.toString(progressionRules) + ", requirements="
					+ Arrays.toString(requirements) + ", summerDissertation="
					+ summerDissertation + "]";
		}
		public int getStageLevel() {
			int stageLevel = -1;
			for (int i=0; i<requirements.length; i++) {
				Requirement r = requirements[i];
				if (r.level>stageLevel)
					stageLevel = r.level;
			}
			return stageLevel;
		}
	}
	public static class Requirement {
		public int credits;
		public int level;
		public Requirement() {}
		public Requirement(int credits, int level) {
			super();
			this.level = level;
			this.credits = credits;
		}
		static public boolean satisfies(Requirement [] requirements, int [] levelcredits) {
			for (int ri=0; ri<requirements.length; ri++) {
				int credits = 0;
				for (int li=requirements[ri].level; li<levelcredits.length; li++)
					credits += levelcredits[li];
				if (credits < requirements[ri].credits) {
					return false;
				}					
			}
			return true;
		}
		static public int extraCredits(Requirement [] requirements, int [] levelcredits) {
			int credits = 0;
			for (int li=0; li<levelcredits.length; li++)
				credits += levelcredits[li];
			int maxcredits = 0;
			for (int ri=0; ri<requirements.length; ri++) {
				if (maxcredits < requirements[ri].credits) {
					maxcredits = requirements[ri].credits;
				}					
			}
			return credits-maxcredits;
		}
		@Override
		public String toString() {
			return "Requirement [credits=" + credits + ", level=" + level + "]";
		}
	}
	public static class ProgressionRequirement {
		public int credits;
		public double mark;
		public ProgressionRequirement(int credits, double mark) {
			super();
			this.credits = credits;
			this.mark = mark;
		}
		@Override
		public String toString() {
			return "ProgressionRequirement [credits=" + credits + ", mark="
					+ mark + "]";
		}
	}
	public static class ProgressionRule {
		public String name;
		public double average;
		ProgressionRequirement requirements[];
		// does not include non-compensatable module(s) which depend on Programme Spec
		public ProgressionRule(String name, double average,
				ProgressionRequirement[] requirements) {
			super();
			this.name = name;
			this.average = average;
			this.requirements = requirements;
		}
		public boolean satisfiedBy(MarkProfile mp) {
			if (mp.average<this.average)
				return false;
			for (int ri=0; requirements!=null && ri<requirements.length; ri++) {
				ProgressionRequirement pr = requirements[ri];
				if (pr.mark>=69.5) {
					if (pr.credits > mp.c70)
						return false;
				} 
				else if (pr.mark>=59.5) {
					if (pr.credits > mp.c70+mp.c60)
						return false;					
				}
				else if (pr.mark>=49.5) {
					if (pr.credits > mp.c70+mp.c60+mp.c50)
						return false;					
				}
				else if (pr.mark>=39.5) {
					if (pr.credits > mp.c70+mp.c60+mp.c50+mp.c40)
						return false;					
				}
				else if (pr.mark>=29.5) {
					if (pr.credits > mp.c70+mp.c60+mp.c50+mp.c40+mp.c30)
						return false;					
				}
				else if (pr.mark==0) {
					if (pr.credits > mp.c70+mp.c60+mp.c50+mp.c40+mp.c30+mp.cfail)
						return false;					
				}
			}
			return true;
		}
		@Override
		public String toString() {
			return "ProgressionRule [average=" + average + ", name=" + name
					+ ", requirements=" + Arrays.toString(requirements) + "]";
		}		
	}
	public static class ClassificationRule {
		public String result;
		public String rule;
		public double average; // strictly depends on Programme Spec, but standard for CS
		public boolean includeResits;
		ProgressionRequirement requirement; // optional - for borderlines
		//ProgressionRequirement requirements[];

		// does not include stage weightings which depend on Programme Spec
		public ClassificationRule(String result, String rule, double average,
				boolean includeResits, ProgressionRequirement requirement) {
			super();
			this.result = result;
			this.rule = rule;
			this.average = average;
			this.includeResits = includeResits;
			this.requirement = requirement;
		}

		public ClassificationRule(String result, double average,
				boolean includeResits, ProgressionRequirement requirement) {
			super();
			this.result = result;
			this.rule = result;// default
			this.average = average;
			this.includeResits = includeResits;
			this.requirement = requirement;
		}

		@Override
		public String toString() {
			return "ClassificationRule [average=" + average
					+ ", includeResits=" + includeResits + ", requirement="
					+ requirement + ", result=" + result + ", rule=" + rule
					+ "]";
		}
		
	}
	/** supplementary regs information */
	public static class SupplementaryRegulations {
		public String qualificationName;
		public String ucasCode;
		public String courseTitle;
		public int year; // 7 = 2007/8
		public double stageWeightings[];
		public TreeSet<String> noncompensatableModules;
		public int firstBorderline;
		// special case for ISD - if you have ANY resits you can't get a distinction
		public TreeSet<String> classificationsPrecludingResits;
		public SupplementaryRegulations(String qualificationName,
				String ucasCode, String courseTitle, int year, double[] stageWeightings,
				int firstBorderline, String noncompensatableModules, String classificationsPrecludingResits) {
			super();
			this.qualificationName = qualificationName;
			this.ucasCode = ucasCode;
			this.courseTitle = courseTitle;
			this.year = year;
			this.stageWeightings = stageWeightings;
			this.firstBorderline = firstBorderline;
			this.noncompensatableModules = new TreeSet<String>();
			String modules[] = noncompensatableModules.split(" ");
			for (String module : modules) {
				this.noncompensatableModules.add(module);
			}
			this.classificationsPrecludingResits = new TreeSet<String>();
			String classifications[] = classificationsPrecludingResits.split(" ");
			for (String module : modules) {
				this.classificationsPrecludingResits.add(module);
			}
		}
		@Override
		public String toString() {
			return "SupplementaryRegulations [courseTitle=" + courseTitle
					+ ", firstBorderline="
					+ firstBorderline + ", noncompensatableModules="
					+ noncompensatableModules + ", qualificationName="
					+ qualificationName + ", stageWeightings="
					+ Arrays.toString(stageWeightings) + ", ucasCode="
					+ ucasCode + ", year=" + year + "]";
		}
	}
	/** standard undergraduate progression rules */
	public static final ProgressionRule ugProgressionRules[] = new ProgressionRule[] {
		new ProgressionRule("10(a)", /*average*/39.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(80, 39.5),
				new ProgressionRequirement(120, 29.5),
		}),
		new ProgressionRule("10(b)", /*average*/49.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(100, 39.5),
		}),
		new ProgressionRule("10(c)", /*average*/44.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(90, 39.5),
				new ProgressionRequirement(110, 29.5),
		}),
	};
	public static final ProgressionRule msciProgressionRules[] = new ProgressionRule[] {
		new ProgressionRule("10(a)(MSci)", /*average*/54.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(80, 39.5),
				new ProgressionRequirement(120, 29.5),
		}),
		new ProgressionRule("10(b)(MSci)", /*average*/54.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(100, 39.5),
		}),
		new ProgressionRule("10(c)(MSci)", /*average*/54.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(90, 39.5),
				new ProgressionRequirement(110, 29.5),
		}),
	};
	public static final ProgressionRule ordProgressionRules[] = new ProgressionRule[] {
		new ProgressionRule("10(a)(Ord)", /*average*/39.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(60, 39.5),
				new ProgressionRequirement(100, 29.5),
		}),
		new ProgressionRule("10(b)(Ord)", /*average*/49.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(80, 39.5),
		}),
		new ProgressionRule("10(c)(Ord)", /*average*/44.5, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(70, 39.5),
				new ProgressionRequirement(90, 29.5),
		}),
	};
	// special-case?!
	public static final ProgressionRule pgProgressionRules[] = new ProgressionRule[] {
		new ProgressionRule("MSc Taught Stage", /*average*/50, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(80, 49.5),
				new ProgressionRequirement(120, 39.5),
		}),
		new ProgressionRule("MSc Taught Stage (SuppRegs)", /*average*/50, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(80, 49.5),
				new ProgressionRequirement(105, 39.5),
				// strictly from supp. regs, but it is the same on all our degrees
				new ProgressionRequirement(120, 29.5),
		}),
	};	
	// required to pass dissertation
	public static final ProgressionRule dissertationProgressionRules[] = new ProgressionRule[] {
		new ProgressionRule("MSc Project Stage", /*average*/50, /*requirements*/new ProgressionRequirement[] {				
				new ProgressionRequirement(60, 49.5),
		}),
	};	
	private static final String BSC_ORDINARY = "BSc (Ordinary)";

	/** UNQF 2011-01-09 */
	public static final Qualification QUALIFICATIONS [] = new Qualification[] {
		new Qualification("Honours Degree", new String[] { "BSc Hons", "BSc Jt Hons" }, 
				new String[] { "Bachelor of Science with Honours", "Bachelor of Science with Joint Honours" }, false, new Requirement[] {
				new Requirement(360,1),
				new Requirement(190,2),
				new Requirement(100,3),
		}, 
		new Stage[] {
				new Stage("Foundation Stage", new Requirement[] {
						new Requirement(120, 0),	
					}, ugProgressionRules, true),
				new Stage("Qualifying Stage", new Requirement[] {
					new Requirement(120, 1),	
				}, ugProgressionRules),
				new Stage("Part I", new Requirement[] {
						new Requirement(120, 1),	
						new Requirement(90, 2),	
				}, ugProgressionRules, false, 0.4),
				new Stage("Part II", new Requirement[] {
						new Requirement(120, 1),	
						new Requirement(100, 3),	
				}, null, false, 0.6),
		},
		new ClassificationRule[] {
				new ClassificationRule("1st", 69.5, false, null),
				new ClassificationRule("1st", "1st borderline", 67.5, false, 
						new ProgressionRequirement(120, 69.5)
				),
				new ClassificationRule("2:1", 59.5, false, null),
				new ClassificationRule("2:1","2:1 borderline", 58.5, false, 
						new ProgressionRequirement(120, 59.5)
				),
				new ClassificationRule("2:2", 49.5, false, null),
				new ClassificationRule("2:2","2:2 borderline", 48.5, false, 
						new ProgressionRequirement(120, 49.5)
				),
				new ClassificationRule("3rd", 39.5, false, null),
				new ClassificationRule("3rd","3rd borderline", 38.5, false, 
						new ProgressionRequirement(120, 39.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(120, 39.5)
				),
		},
		new String[] { "Ordinary Degree", "UGDip", "UGCert" },
		new String[] { "Ordinary Degree" }),
		
		new Qualification("Integrated Masters", new String[] { "MSci Hons" }, 
				new String[] { "Master in Science with Honours" }, false, new Requirement[] {
				new Requirement(480,1),
				new Requirement(290,2),
				new Requirement(200,3),
				new Requirement(120,4),
		}, 
		new Stage[] {
				new Stage("Foundation Stage", new Requirement[] {
						new Requirement(120, 0),	
					}, ugProgressionRules, true),
				new Stage("Qualifying Stage", new Requirement[] {
					new Requirement(120, 1),	
				}, ugProgressionRules),
				new Stage("Part I", new Requirement[] {
						new Requirement(120, 1),	
						new Requirement(90, 2),	
				}, msciProgressionRules, false, 0.2),
				new Stage("Part II", new Requirement[] {
						new Requirement(120, 1),	
						new Requirement(100, 3),	
				}, msciProgressionRules, 
				// NB excludes resits after Part II
				true, false, 0.4, false),
				new Stage("Part III", new Requirement[] {
						new Requirement(120, 1),	
						new Requirement(100, 3),
						new Requirement(90, 4),	
				}, null, false, 0.4),
		},
		new ClassificationRule[] {				
				new ClassificationRule("1st", 69.5, false, null),
				new ClassificationRule("1st", "1st borderline", 67.5, false, 
						new ProgressionRequirement(180, 69.5)
				),
				new ClassificationRule("2:1", 59.5, false, null),
				new ClassificationRule("2:1", "2:1 borderline", 58.5, false, 
						new ProgressionRequirement(180, 59.5)
				),
				new ClassificationRule("2:2", 49.5, false, null),
				new ClassificationRule("2:2", "2:2 borderline", 48.5, false, 
						new ProgressionRequirement(180, 49.5)
				),
				new ClassificationRule("3rd", 39.5, false, null),
				new ClassificationRule("3rd", "3rd borderline", 38.5, false, 
						new ProgressionRequirement(180, 39.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(180, 39.5)
				),
		},
		new String[] { "Honours Degree", "Ordinary Degree", "UGDip", "UGCert"  },
		new String[] { "Honours Degree", "Ordinary Degree" }),
		
		new Qualification("Ordinary Degree", new String[] { "BSc (Ordinary)" }, 
				new String[] { "Bachelor of Science  (Ordinary)" }, false, new Requirement[] {
				new Requirement(300,1),
				new Requirement(60,3),
		}, 
		new Stage[] {
				new Stage("Foundation Stage", new Requirement[] {
						new Requirement(120, 0),	
					}, ugProgressionRules, true),
				new Stage("Qualifying Stage", new Requirement[] {
					new Requirement(100, 1),	
				}, ordProgressionRules),
				new Stage("Part I", new Requirement[] {
						new Requirement(100, 1),	
						new Requirement(80, 2),	
						// weightings 50/50?!
				}, ordProgressionRules, false, 0.5),
				new Stage("Part II", new Requirement[] {
						new Requirement(100, 1),	
						new Requirement(60, 3),	
						// weightings 50/50?!
				}, null, false, 0.5),
		},
		new ClassificationRule[] {				
				new ClassificationRule("Distinction", 69.5, false, null),
				new ClassificationRule("Distinction", "Distinction borderline", 67.5, false, 
						new ProgressionRequirement(100, 69.5)
				),
				new ClassificationRule("Merit", 59.5, false, null),
				new ClassificationRule("Merit", "Merit borderline", 58.5, false, 
						new ProgressionRequirement(100, 59.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(100, 39.5)
				),
		},
		new String[] { "UGDip", "UGCert" }, new String[0], 
		// combined classification requirements
		new Requirement[] {
				new Requirement(200, 1),	
				new Requirement(60, 3),	
		},
		// no combined progression requirement for ordinary as it is handled by stage mapping from Honours
		null),

		new Qualification("Taught Masters", new String[] { "MSc" }, 
				new String[] { "Master of Science" }, true, new Requirement[] {
				new Requirement(180,1),
				new Requirement(150,4),
		}, 
		new Stage[] {
//				new Stage("Combined Taught/Disseration Stage", new Requirement[] {
//						new Requirement(180, 1),	
//						new Requirement(150, 4),	
//					}, pgProgressionRules, false, 1.0, 
//					//note: progression excludes dissertation
//					true),
				new Stage("Taught Stage", new Requirement[] {
					new Requirement(120, 1),	
					new Requirement(90, 4),	
				}, pgProgressionRules, false, 120.0/180.0),
				new Stage("Dissertation/Project Stage", new Requirement[] {
						new Requirement(60, 4),	
				}, dissertationProgressionRules, false, 60.0/180.0, 
				// summer dissertation
				true),
		},
		new ClassificationRule[] {				
				new ClassificationRule("Distinction", 69.5, false, null),
				new ClassificationRule("Distinction", "Distinction borderline", 67.5, false, 
						new ProgressionRequirement(120, 69.5)
				),
				new ClassificationRule("Merit", 59.5, false, null),
				new ClassificationRule("Merit", "Merit borderline", 58.5, false, 
						new ProgressionRequirement(120, 59.5)
				),
				new ClassificationRule("Pass", 49.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 48.5, true, 
						new ProgressionRequirement(120, 49.5)
				),
		},
		new String[] { "PGDip", "PGCert" }, new String[0]),
		
		// other qualifications
		new Qualification("UGCert", new String[0], new String[0], false, new Requirement[] {
				new Requirement(120,1),
		}, 
		new Stage[] {
				// UGCert aligns with Qualifying stage
				new Stage("Foundation Stage", new Requirement[] {
						new Requirement(120, 0),	
					}, ugProgressionRules, true),
				new Stage("Qualifying Stage", new Requirement[] {
					new Requirement(120, 1),	
				}, null, false, 1.0),
		},
		new ClassificationRule[] {				
				new ClassificationRule("Distinction", 69.5, false, null),
				new ClassificationRule("Distinction", "Distinction borderline", 67.5, false, 
						new ProgressionRequirement(600, 69.5)
				),
				new ClassificationRule("Merit", 59.5, false, null),
				new ClassificationRule("Merit", "Merit borderline", 58.5, false, 
						new ProgressionRequirement(600, 59.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(600, 39.5)
				),
		}, new String[0], new String[0], new Requirement[] {
				new Requirement(120,1),
		}, null/*new ProgressionRule[] {
				
		}*/), 

		new Qualification("UGDip", new String[0], new String[0], false, new Requirement[] {
				new Requirement(240,1),
				new Requirement(90,2),
		}, 
		new Stage[] {
				// UGDip aligns wit Qualifying and Part I
				new Stage("Foundation Stage", new Requirement[] {
						new Requirement(120, 0),	
					}, ugProgressionRules, true),
				new Stage("Qualifying Stage", new Requirement[] {
					new Requirement(120, 1),	
				}, ugProgressionRules, false, 0.5),
				new Stage("Part I", new Requirement[] {
						new Requirement(120, 1),	
						new Requirement(90, 2),	
				}, null, false, 0.5),
		},
		new ClassificationRule[] {				
				new ClassificationRule("Distinction", 69.5, false, null),
				new ClassificationRule("Distinction", "Distinction borderline", 67.5, false, 
						new ProgressionRequirement(120, 69.5)
				),
				new ClassificationRule("Merit", 59.5, false, null),
				new ClassificationRule("Merit", "Merit borderline", 58.5, false, 
						new ProgressionRequirement(120, 59.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(120, 39.5)
				),
		},
		new String[] { "UGCert" }, new String[0], new Requirement[] {
				new Requirement(240,1),
				new Requirement(90,2),
		}, null/*new ProgressionRule[] {
				
		}*/), 

		new Qualification("PGCert", new String[0], new String[0], false, new Requirement[] {
				new Requirement(60,1),
				new Requirement(50,4),
		}, null,
		new ClassificationRule[] {				
				new ClassificationRule("Distinction", 69.5, false, null),
				new ClassificationRule("Distinction", "Distinction borderline", 67.5, false, 
						new ProgressionRequirement(30, 69.5)
				),
				new ClassificationRule("Merit", 59.5, false, null),
				new ClassificationRule("Merit", "Merit borderline", 58.5, false, 
						new ProgressionRequirement(30, 59.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(30, 39.5)
				),
		}, new String[0], new String[0], new Requirement[] {
				new Requirement(60,1),
				new Requirement(50,4),
		}, new ProgressionRule[] {
				new ProgressionRule("PGCert point 19.", 0, new ProgressionRequirement[] {
						new ProgressionRequirement(60, 39.5),
					}),	
				new ProgressionRule("PGCert point 11.", /*average*/39.5, /*requirements*/new ProgressionRequirement[] {				
						new ProgressionRequirement(40, 39.5),
						new ProgressionRequirement(60, 29.5),
				}),
		}), 

		new Qualification("PGDip", new String[0], new String[0], false, new Requirement[] {
				new Requirement(120,1),
				new Requirement(90,4),
		}, null,
		new ClassificationRule[] {				
				new ClassificationRule("Distinction", 69.5, false, null),
				new ClassificationRule("Distinction", "Distinction borderline", 67.5, false, 
						new ProgressionRequirement(60, 69.5)
				),
				new ClassificationRule("Merit", 59.5, false, null),
				new ClassificationRule("Merit", "Merit borderline", 58.5, false, 
						new ProgressionRequirement(60, 59.5)
				),
				new ClassificationRule("Pass", 39.5, true, null),
				new ClassificationRule("Pass", "Pass borderline", 38.5, true, 
						new ProgressionRequirement(60, 39.5)
				),
		},
		new String[] { "PGCert" }, new String[0], new Requirement[] {
				new Requirement(120,1),
				new Requirement(90,4),
		}, new ProgressionRule[] {
				new ProgressionRule("PGDip point 19.", 0, new ProgressionRequirement[] {
						new ProgressionRequirement(120, 39.5),
					}),	
				new ProgressionRule("PGDip point 11.", /*average*/39.5, /*requirements*/new ProgressionRequirement[] {				
						new ProgressionRequirement(80, 39.5),
						new ProgressionRequirement(120, 29.5),
				}),
		}), 

	};

	static SupplementaryRegulations supplementaryRegulations[] = new SupplementaryRegulations[] {
		new SupplementaryRegulations("BSc Hons", "G400", "Computer Science", 0,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
			"G51PRG", ""),
		new SupplementaryRegulations("BSc Hons", "G400", "Computer Science", 10,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
			"G51PRG G51OOP", ""),
		new SupplementaryRegulations("BSc (Ordinary)", "G400", "Computer Science", 0,
					new double[] { 0.0, 0.0, 0.5, 0.5 },
					68, 
				"", ""),
		new SupplementaryRegulations("MSc", "G402", "Computer Science and Entrepreneurship", 0,
				null,// default weights
				68, 
				"G64PRE", ""),
		new SupplementaryRegulations("MSc", "G403", "Advanced Computing Science", 0,
				null,// default weights
				68, 
				"", ""),
		new SupplementaryRegulations("MSci Hons", "G404", "Computer Science", 0,
				new double[] { 0.0, 0.0, 0.2, 0.4, 0.4 },
				68, 
				"G51PRG", ""),
		new SupplementaryRegulations("BSc Hons", "G404", "Computer Science", 10,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG G51OOP", ""),
		new SupplementaryRegulations("BSc Hons", "G4G7", "Computer Science with Artificial Intelligence", 0,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG", ""),
		new SupplementaryRegulations("BSc Hons", "G4G7", "Computer Science with Artificial Intelligence", 10,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG G51OOP", ""),
		new SupplementaryRegulations("BSc (Ordinary)", "G4G7", "Computer Science with Artificial Intelligence", 0,
						new double[] { 0.0, 0.0, 0.5, 0.5 },
						68, 
						"", ""),
		new SupplementaryRegulations("BSc Hons", "G4H6", "Computer Science with Robotics", 0,
					new double[] { 0.0, 0.0, 0.4, 0.6 },
						68, 
						"G51PRG", ""),
		new SupplementaryRegulations("BSc Hons", "G4H6", "Computer Science with Robotics", 10,
						new double[] { 0.0, 0.0, 0.4, 0.6 },
						68, 
						"G51PRG G51OOP", ""),
		new SupplementaryRegulations("BSc (Ordinary)", "G4H6", "Computer Science with Robotics", 0,
								new double[] { 0.0, 0.0, 0.5, 0.5 },
								68, 
								"", ""),
		new SupplementaryRegulations("BSc Jt Hons", "GN42", "Computer Science and Management Studies", 0,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG", ""),
		new SupplementaryRegulations("BSc Jt Hons", "GN42", "Computer Science and Management Studies", 10,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG G51OOP", ""),
		new SupplementaryRegulations("BSc (Ordinary)", "GN42", "Computer Science and Management Studies", 0,
						new double[] { 0.0, 0.0, 0.5, 0.5 },
						68, 
						"", ""),
		new SupplementaryRegulations("BSc Hons", "GNK1", "E-Commerce and Digital Business", 0,
						new double[] { 0.0, 0.0, 0.4, 0.6 },
						68, 
						"G51PRG", ""),
		new SupplementaryRegulations("BSc (Ordinary)", "GNK1", "E-Commerce and Digital Business", 0,
				new double[] { 0.0, 0.0, 0.5, 0.5 },
				68, 
				"", ""),
		/*// not our students
		new SupplementaryRegulations("MSci Jt Hons", "GG14", "Mathematics and Computer Science", 0,
				new double[] { 0.0, 0.0, 0.2, 0.4, 0.4 },
				68, 
				"G51PRG G11ACF G11CAL G11LMA"),
		new SupplementaryRegulations("MSci Jt Hons", "GG14", "Mathematics and Computer Science", 10,
				new double[] { 0.0, 0.0, 0.2, 0.4, 0.4 },
				68, 
				"G51PRG G51OOP G11ACF G11CAL G11LMA"),
		new SupplementaryRegulations("BSc Hons", "GG41", "Mathematics and Computer Science", 0,
					new double[] { 0.0, 0.0, 0.35, 0.65 },
						68, 
						"G51PRG G11ACF G11CAL G11LMA"),
		new SupplementaryRegulations("BSc Jt Hons", "GG41", "Mathematics and Computer Science", 9,
						new double[] { 0.0, 0.0, 0.33, 0.67 },
						68, 
						"G51PRG G11ACF G11CAL G11LMA"),
		new SupplementaryRegulations("BSc Jt Hons", "GG41", "Mathematics and Computer Science", 10,
						new double[] { 0.0, 0.0, 0.33, 0.67 },
						68, 
						"G51PRG G51OOP G11ACF G11CAL G11LMA"),
		*/
		new SupplementaryRegulations("BSc Hons", "G601", "Software Systems", 0,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG", ""),
		new SupplementaryRegulations("BSc Hons", "G601", "Software Systems", 10,
				new double[] { 0.0, 0.0, 0.4, 0.6 },
				68, 
				"G51PRG G51OOP", ""),
		new SupplementaryRegulations("BSc (Ordinary)", "G601", "Software Systems", 0,
						new double[] { 0.0, 0.0, 0.5, 0.5 },
						68, 
						"", ""),
		new SupplementaryRegulations("BSc Hons", "G425", "Computing and Information Systems", 0,
						new double[] { 0.0, 0.0, 0.4, 0.6 },
						68, 
						"G51PRG", ""),
		new SupplementaryRegulations("BSc Hons", "G425", "Computing and Information Systems", 10,
						new double[] { 0.0, 0.0, 0.4, 0.6 },
						68, 
						"G51PRG G51OOP", ""),
		new SupplementaryRegulations("MSc", "G507", "Information Technology", 0,
				null,// default weights
				68, 
				"G64OOS", ""),
		new SupplementaryRegulations("MSc", "G565", "Management of Information Technology", 0,
				null,// default weights
				68, 
				"G64MIT", ""),
		new SupplementaryRegulations("MSc", "G900", "Scientific Computation", 0,
				null,// default weights
				68, 
				"G14SCD", ""),
		new SupplementaryRegulations("MSc", "GH57", "Interactive Systems Design", 0,
				null,// default weights
				68, 
				"G64IDS", "Distinction"),
				//....
	};
	static SupplementaryRegulations getSupplementaryRegulations(Student s, int year) {
		SupplementaryRegulations sr = null;
		for (int i=0; i<supplementaryRegulations.length; i++) {
			SupplementaryRegulations sr2 = supplementaryRegulations[i];
			if (sr2.qualificationName.equals(s.qualification) && sr2.ucasCode.equals(s.courseCode) && sr2.year<=year && 
					(sr==null || sr2.year>sr.year))
				sr = sr2;
		}
		return sr;
	}
	static SupplementaryRegulations getSupplementaryRegulations(Student s, int year, Qualification altQual) {
		SupplementaryRegulations sr = null;
		for (int n=0; n<altQual.names.length; n++) {
			for (int i=0; i<supplementaryRegulations.length; i++) {
				SupplementaryRegulations sr2 = supplementaryRegulations[i];
				if (sr2.qualificationName.equals(altQual.names[n]) && sr2.ucasCode.equals(s.courseCode) && sr2.year<=year && 
						(sr==null || sr2.year>sr.year))
					sr = sr2;
			}
		}
		return sr;
	}
	static MarkProfile getMarkProfile(List<Mark> smarks, boolean includeResits) {
		MarkProfile mp = new MarkProfile();
		mp.includesResits = includeResits;
		int sum =0;
		for (Mark m : smarks) {
			Integer mark = (includeResits) ? m.bestmark() : m.mark;
					
			if (mark!=null) {
				sum += m.credit*mark;
				mp.credits += m.credit;
				if (mark>=70)
					mp.c70 += m.credit;
				else if (mark>=60)
					mp.c60 += m.credit;
				else if (mark>=50)
					mp.c50 += m.credit;
				else if (mark>=40)
					mp.c40 += m.credit;
				else if (mark>=30)
					mp.c30 += m.credit;
				else
					mp.cfail += m.credit;
			}
			if (m.secondmark!=null)
				mp.hasResits = true;
		}
		if(mp.credits>0)
			mp.average = 1.0*sum/mp.credits;
		return mp;
	}
	static enum MarkPrintOption { MARK_FIRST, MARK_RESIT, MARK_BEST }; 
	static void printMarkProfile(PrintWriter pw, String title, MarkProfile mp, List<Mark> smarks, MarkPrintOption option) {
		pw.print(",,"+title+","+df2Format(mp.average)+","+mp.credits+","+mp.c70+","+mp.c60+","+mp.c50+","+mp.c40+","+mp.c30+","+mp.cfail);
		for (Mark m : smarks) {
				pw.print(",");
				switch(option) {
				case MARK_BEST:
					if (m.bestmark()!=null) {
						pw.print(m.bestmark());
					}
					break;
				case MARK_FIRST:
					if (m.mark!=null) {
						pw.print(m.mark);
					}
					break;
				case MARK_RESIT:
					if (m.secondmark!=null) {
						pw.print(m.secondmark);
					}
					break;
				}
		}
		pw.println();
	}
	static void printMarkSet(PrintWriter pw, String title, List<Mark> smarks, boolean includeResits) {
		Collections.sort(smarks, new ModuleSemesterAndNameComparator());
		pw.print(","+title+",modules,mean,credits,\"70+\",\"6x\",\"5x\",\"4x\",\"3x\",\"<30\"");
		for (Mark m : smarks) 
			pw.print(","+m.module);
		pw.println();
		// semester
		pw.print(",,semester,,,,,,,,");
		for (Mark m : smarks) 
			pw.print(","+m.semester);
		pw.println();
		// credits
		pw.print(",,credits,,,,,,,,");
		for (Mark m : smarks) 
			pw.print(","+m.credit);
		pw.println();
		// first sit
		MarkProfile mp = getMarkProfile(smarks, false);
		printMarkProfile(pw, "first", mp, smarks, MarkPrintOption.MARK_FIRST);
		if (mp.hasResits && includeResits) {
			mp = getMarkProfile(smarks, true);
			printMarkProfile(pw, "best", mp, smarks, MarkPrintOption.MARK_RESIT);
		}

	}
	static List<Mark> getBestMarks(List<Mark> marks, int credits, boolean includeResits, TreeSet<String> noncompensatableModules, Requirement[] requirements) {
		LinkedList<Mark> best = new LinkedList<Mark>();
		// skip missing marks?!
		for (Mark m : marks) {
			if (m.bestmark()!=null)
				best.add(m);
		}
		Collections.sort(best, includeResits ? new BestMarkComparator() : new FirstMarkComparator());
		int cs = 0;
		for (Mark m : best)
			cs += m.credit;
		if (cs<credits) {
			//System.err.println("Warning: getBestMarks() given only "+cs+"/"+credits+")");
		}
		boolean requirementsImpossible = false;
		for (int j=best.size()-1; j>=0; j--) {
			// try removing j, starting with lowest mark
			Mark m = best.get(j);
			// non-comp?
			if (noncompensatableModules!=null && noncompensatableModules.contains(m.module))
				continue;
			// leave enough at high level?
			int level = getModuleLevel(m.module);
			if (level>1 && requirements!=null && requirements.length>1 && level>=requirements[1].level) {
				int lc = 0;
				for (Mark m2 : best) {
					int level2 = getModuleLevel(m2.module);
					if (level2>=requirements[1].level)
						lc += m2.credit;
				}
				if (lc-m.credit<requirements[1].credits)
					// nope
					continue;
				if (lc<requirements[1].credits && !requirementsImpossible) {
					requirementsImpossible = true;
					System.err.println("Warning: getBestMarks() given only "+lc+"/"+requirements[1].credits+" at level "+requirements[1].level);
				}
			}
			// leave enough credits?
			if (cs-m.credit<credits)
				continue;
			// ok
			cs -= m.credit;
			best.remove(j);	
		}
		return best;
	}
	static int[] getLevelCredits(List<Mark> marks) {
		int levelcredits[] = new int[6]; // levels 0,1,2,3,4,5
		for (Mark m : marks) {
			int level = getModuleLevel(m.module);
			levelcredits[level] += m.credit;
		}	
		return levelcredits;
	}
	static int getTotalCredits(List<Mark> marks) {
		int totalcredits = 0;
		for (Mark m : marks) {
			totalcredits += m.credit;
		}	
		return totalcredits;
	}
	static boolean satifiesReqirements(Requirement [] requirements, List<Mark> marks) {
		int levelcredits[] = getLevelCredits(marks);
		return Requirement.satisfies(requirements, levelcredits);
	}
	static int guessStageLevel(List<Mark> marks) {
		int levelcredits[] = getLevelCredits(marks);
		int max = 0, level = -1;
		for (int i=0; i<levelcredits.length; i++) {
			if (levelcredits[i]>max) {
				level = i;
				max = levelcredits[i];
			}
		}
		return level;
	}
	static String identifyMissingModules(List<Mark> allMarks, List<Mark> bestMarks) {
		StringBuffer sb = new StringBuffer();
		TreeSet<String> best = new TreeSet<String>();
		for (Mark m : bestMarks)
			best.add(m.module);
		for (Mark m : allMarks) {
			if (best.contains(m.module))
				continue;
			sb.append(m.module+" ("+m.credit+"c "+(m.mark!=null ? m.mark : "")+"/"+(m.secondmark!=null ? m.secondmark : "")+"%) ");
		}
		return sb.toString();
	}
	static String requirementsText(Requirement requirements[]) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; requirements!=null && i<requirements.length; i++) {
			if (i==1)
				sb.append(", including ");
			else if (i>1)
				sb.append(" and ");
			sb.append(requirements[i].credits+" at level "+requirements[i].level+" or above");
		}
		return sb.toString();
	}
	static void printVersion(PrintWriter pw) {
		pw.println("\"Generated by saturntools.ProcessSaturnExportStudents, version "+VERSION_STRING+", "+new Date()+"\"");
	}
	//static DecimalFormat df2 = new DecimalFormat("#0.0"); // should be df1 :-)
	static DecimalFormat df1 = new DecimalFormat("#0.0"); // should be df1 :-)
	static String df2Format(double d) {
		// ensure decimal place is rounded down
		return df1.format(Math.floor(d*10)/10);
	}
	static class StageInfo {
		List<Mark> stageMarks;
		boolean optional;
		boolean progresses;
		boolean hasMissingMarks;
		boolean hasResitsOutstanding;
		boolean assumedDirectEntry;
	}
	static void printReadingFile(PrintWriter pw, File file, String content) {
		String msg = "\"Reading "+content+" from "+file+", size "+file.length()+", last modified "+new Date(file.lastModified())+"\"";
		pw.println(msg);
		System.out.println(msg);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length<4) {
			System.err.println("Usage <schoollist> <student-further-details-ug> <...-pg> <modules3> ... ");
			System.exit(-1);
		}
		try {
			PrintWriter pw = new PrintWriter(new FileWriter("out_student_details.csv"));
			printVersion(pw);
			
			File slfile = new File(args[0]);
			printReadingFile(pw, slfile, "school list");
			HashMap<String,HashMap<String,String>> schoollist = ReadCsvFile.readCsvFile(slfile, "Student ID", false);
			HashMap<String,HashMap<String,String>> furtherdetails = new HashMap<String,HashMap<String,String>>();
			for (int i=1; i<=2; i++) {
				File sdfile = new File(args[i]);
				printReadingFile(pw, sdfile, "student further details");
				furtherdetails.putAll(ReadCsvFile.readCsvFile(sdfile, "student_id", false));
			}
			List<Mark> allmarks = new LinkedList<Mark>();
			for (int ai=3; ai<args.length; ai++) {
				File f = new File(args[ai]);
				printReadingFile(pw, f, "saturn marks (modules3 export)");
				allmarks.addAll(ProcessSaturnExportMarks.readMarks(f));				
			}
			ProcessSaturnExportMarks.discardDuplicateMarks(allmarks);
			Map<String,List<Mark>> studentmarks = ProcessSaturnExportMarks.getMarksByStudent(allmarks);
			
			TreeSet<String> students = new TreeSet<String>();
			students.addAll(schoollist.keySet());
			
			System.out.println("School list has "+students.size()+" students");
			students.addAll(furtherdetails.keySet());
			System.out.println("Including further details, "+students.size()+" students");
			TreeSet<String> modesOfStudy = new TreeSet<String>();
			TreeSet<String> qualifications = new TreeSet<String>();
			TreeSet<String> yearsOfCourse = new TreeSet<String>();
			
			pw.println();
			
			for (String sid : students) {
				HashMap<String,String> sfd = furtherdetails.get(sid);
				if (sfd==null) {
					pw.println("\"Warning: could not find furtherdetails for student "+sid+" ("+schoollist.get(sid)+")\"");
					pw.println();
					continue;
				}
				Student s= new Student();
				s.id = sid;
				s.courseCode = sfd.get(FD_COURSE_CODE);
				s.dateOfEntry = sfd.get(FD_DATE_OF_ENTRY);
				s.firstNames = sfd.get(FD_FIRST_NAMES);
				s.modeOfStudy = sfd.get(FD_MODE_OF_STUDY);
				modesOfStudy.add(s.modeOfStudy);
				s.qualification = sfd.get(FD_DEGREE_QUAL_AIMS);
				qualifications.add(s.qualification);
				s.registered = sfd.get(FD_REGISTERED).startsWith("Y");
				s.surname = sfd.get(FD_SURNAME);
				s.yearOfCourse = sfd.get(FD_YEAR_OF_COURSE);
				s.courseTitle = sfd.get(FD_DEGREE_TITLE);
				yearsOfCourse.add(s.yearOfCourse);
				// TODO
				List<Mark> smarks = studentmarks.get(sid);
				if (smarks==null) {
					if (schoollist.containsKey(sid)) {
						pw.println("\"Warning: could not find any marks for student "+sid+" ("+s+")\"");
						pw.println();
					}
					// or silently ignore if not in school list
					continue;
				}
				
				if (!schoollist.containsKey(sid)) {
					pw.println("\"Note: student "+sid+" was not in school list but has marks\"");
				}
				
				Collections.sort(smarks, new TranscriptMarkComparator());
				
				// sorted by year
				TreeSet<Integer> syears = new TreeSet<Integer>();
				for (Mark m : smarks) {
					syears.add(m.year);
				}

				// can we now allocate them to stages?
				
				// first we need to find the qualification...
				Qualification qual = getQualification(s.qualification);
				if (qual==null) {
					pw.println("\"Warning: could not find qualification "+s.qualification+" for student "+sid+" ("+s+")\"");
					//pw.println();
					//continue;
				}
				
				Map<Integer,Integer> year2stage = new HashMap<Integer,Integer>();
				Map<Integer,TreeSet<Integer>> stage2years = new HashMap<Integer,TreeSet<Integer>>();
				TreeSet<Integer> preTransferYears = new TreeSet<Integer>();
				Mark dissertationMark = null;
				boolean foundDissertationStage = false;
				if (qual!=null && qual.stages!=null && qual.stages.length>0) {
					// allocate years and marks to stages...
					// credits at a level to satisfy a later stage might imply direct entry or UNMC...
					// multiple year(s) and too many credits at a single stage might imply course transfer...
					int istage = 0;
					int levelcredits[] = new int[6]; // levels 0,1,2,3,4,5
					TreeSet<Integer> stageyears = new TreeSet<Integer>();
					for (int year : syears) {
						// accumulate credits at levels for this year
						for (Mark m : smarks) {
							if (m.year==year) {
								if (DISSERTATION_SEMESTER.equals(m.semester) && m.credit==DISSERTATION_CREDITS) {
									if (dissertationMark!=null) 
										pw.println("Warning: found second dissertation mark for student "+s.id+": "+m);
									dissertationMark = m;
									//pw.println("Debug: found dissertation in year "+year);
								}
								else {
									int level = getModuleLevel(m.module);
									if (level==4 && m.credit==DISSERTATION_CREDITS)
										pw.println("Debug: found possible dissertation "+m);
									levelcredits[level] += m.credit;
								}
							}
						}						
						// TODO...
						stageyears.add(year);
						// satisfy current level?
						boolean extrastage = istage >= qual.stages.length;
						if (extrastage) {
							pw.println("Warning: Year "+year+" includes marks but all stages are complete");
							break;
						}
						Stage stage = qual.stages[istage];
						boolean reset = false;
						if (!stage.summerDissertation && Requirement.satisfies(stage.requirements, levelcredits)) {
							int extra = Requirement.extraCredits(stage.requirements, levelcredits);
							if (extra>0) {
								if (stageyears.size()>1) {
									// does this year do it on its own? if, so probably a transfer...
									int yearlevelcredits[] = new int[6]; // levels 0,1,2,3,4,5
									for (Mark m : smarks) {
										if (m.year==year) {
											if (!DISSERTATION_SEMESTER.equals(m.semester) ||  m.credit!=DISSERTATION_CREDITS) {
												int level = getModuleLevel(m.module);
												yearlevelcredits[level] += m.credit;
											}
										}
									}						
									if (Requirement.satisfies(stage.requirements, yearlevelcredits)) {
										// yes!
										pw.println("\"Debug: year "+year+" satisfies stage "+istage+" ("+stage.name+") on its own (rather than "+stageyears+") - assumed course transfer\"");
										levelcredits = yearlevelcredits;
										stageyears.remove(year);
										preTransferYears.addAll(stageyears);
										stageyears.clear();
										stageyears.add(year);
										extra = Requirement.extraCredits(stage.requirements, levelcredits);
									}
								}
								if (extra>0)
									pw.println("\"Debug: Years "+stageyears+" have "+extra+" extra credits cf stage "+istage+" ("+stage.name+")\"");
							}
							pw.println("\"Debug: Years "+stageyears+" satisfy stage "+istage+" ("+stage.name+")\"");
							if (!stage2years.containsKey(istage))
								stage2years.put(istage, new TreeSet<Integer>());
							for (int y : stageyears) {
								year2stage.put(y, istage);
								stage2years.get(istage).add(y);
							}
							istage ++;				
							reset = true;
							// satisfy next level (in isolation)?
							while (istage<qual.stages.length) {
								Stage stage2 = qual.stages[istage];
								if (!stage2.summerDissertation && Requirement.satisfies(stage2.requirements, levelcredits)) {
									if (stage.optional)
										pw.println("\"Debug: Years "+stageyears+" also satisfy stage "+(istage)+" ("+stage2.name+") - previous stage is optional\"");
									else
										pw.println("\"Debug: years "+stageyears+" also satisfy stage "+(istage)+" ("+stage2.name+") - possible direct entry!\"");
									// remove previous year
									stage2years.remove(istage-1);
									extra = Requirement.extraCredits(stage2.requirements, levelcredits);
									if (extra>0) {
										pw.println("\"Debug: Years "+stageyears+" have "+extra+" extra credits cf stage "+istage+" ("+stage2.name+")\"");
									}
									if (!stage2years.containsKey(istage))
										stage2years.put(istage, new TreeSet<Integer>());
									for (int y : stageyears) {
										year2stage.put(y, istage);
										stage2years.get(istage).add(y);
									}
									istage ++;
									stage = stage2;
								}	
								else 
									break;
							}
						}
						else if (istage>0) {
							// satisfies previous level(s)? -> transfer of course?
							for (int is=istage-1; is>=0; is--) {
								Stage stage2 = qual.stages[is];
								if (Requirement.satisfies(stage2.requirements, levelcredits)) {
									pw.println("\"Debug: years "+stageyears+" satisfy stage "+is+" ("+stage2.name+") but not expected stage "+istage+" ("+stage.name+") - possible course transfer\"");
									int extra = Requirement.extraCredits(stage2.requirements, levelcredits);
									if (extra>0) {
										pw.println("\"Debug: Years "+stageyears+" have "+extra+" extra credits cf stage "+istage+" ("+stage.name+")\"");
									}
									// remove and ignore all previous apparent attempts at any stage?!
									preTransferYears.addAll(year2stage.keySet());
									stage2years.clear();
									year2stage.clear();
								
									if (!stage2years.containsKey(is)) 
										stage2years.put(is, new TreeSet<Integer>());
//									else {
//										// Just ignoring same stage? - no all stages
//										TreeSet<Integer> ignoreYears = stage2years.get(is);
//										for (int y : ignoreYears) {
//											year2stage.remove(y);
//											pw.println("\"Warning: ignoring year "+y+" as apparent past attempt at "+stage.name+"\"");
//										}
//										ignoreYears.clear();
//									}
									for (int y : stageyears) {
										year2stage.put(y, is);
										stage2years.get(is).add(y);
									}
									istage = is+1;									
									reset = true;
									break;
								}
							}
						}
						if (reset) {
							levelcredits = new int[6]; // levels 0,1,2,3,4,5
							stageyears = new TreeSet<Integer>();							
						} 
						else {
							int extra = Requirement.extraCredits(stage.requirements, levelcredits);
							if (extra<0) {
								pw.println("\"Debug: Years "+stageyears+" are "+extra+" credits short for stage "+istage+" ("+stage.name+")\"");
							}
							else {
								pw.println("\"Debug: Years "+stageyears+" have "+extra+" extra credits but don't satisfy stage "+istage+" ("+stage.name+")\"");
							}
						}
						if (istage<qual.stages.length && qual.stages[istage].summerDissertation) {
							foundDissertationStage = true;
							// now reached dissertation, just or previously
							if (dissertationMark!=null) {
								// (already) found dissertation
								stage2years.put(istage, new TreeSet<Integer>());
								stage2years.get(istage).add(year);
								istage++;
							}
						}
					}
					if (foundDissertationStage && dissertationMark==null) {
						pw.println("\"Warning: advanced to dissertation stage but dissertation module not found\"");						
					}
					else if (dissertationMark!=null && !foundDissertationStage) {
						pw.println("\"Warning: found summer dissertation but have not reached a dissertation stage\"");
					}
					if (stageyears.size()>0) {
						if (istage<qual.stages.length) {
							Stage stage = qual.stages[istage];
							int extra = Requirement.extraCredits(stage.requirements, levelcredits);
							if (extra<0) {
								pw.println("\"Debug: final year(s) "+stageyears+" are "+extra+" credits short for stage "+istage+" ("+stage.name+")\"");
							}						
						}
						else {
							pw.println("\"Debug: final year(s) "+stageyears+" have mark(s) but stages are complete\"");
						}
					}
					else {
						istage--;
					}
					if (qual.stages[0].optional && istage>0)
						istage--;
					try {
						int yoc = Integer.parseInt(s.yearOfCourse);
						if (istage!=yoc-1) {
							// could be year-out student => yoc 3, completed part I, no modules in current year
							// could be part-time => yoc >= stage
							if (MOS_FULL_TIME.equals(s.modeOfStudy)) {
								if (istage==yoc-2 && yoc==3)
									pw.println("Warning: yearOfCourse "+s.yearOfCourse+" does not match stage - possible year out");
								else
									pw.println("Warning: yearOfCourse "+s.yearOfCourse+" does not match stage");
							}
							else {
								if (istage>yoc-1)
									pw.println("Warning: non-fulltime stage not consistent with yearOfCourse "+s.yearOfCourse);
								else
									pw.println("Debug: Not full-time (yearOfCourse "+s.yearOfCourse+" does not match stage)");
							}
							
						}
					}
					catch (Exception e) {
						System.err.println("Error parsing yearOfCourse for student "+s.id+": "+s.yearOfCourse);
					}
				}
				
				pw.println("id,surname,firstNames,course,qual,coursetitle,mode,yearOnCourse,registered,level1credits,level2credits,level3credits,level4credits,level5credits");
				// level credits
				{
					int levelcredits [] = new int[6];
					for (Mark m : smarks) {
						if (m.bestmark()!=null) {
							if (m.module.length()<3) {
								System.err.println("Error: short module code: "+m.module+" (student "+sid+")");
								continue;
							}

							int level = getModuleLevel(m.module);
							levelcredits[level] += m.credit;
						}
					}
					pw.print(s.id+",\""+s.surname+"\",\""+s.firstNames+"\","+s.courseCode+","+s.qualification+","+s.courseTitle+","+s.modeOfStudy+","+s.yearOfCourse+","+s.registered);
					for (int i=1; i<=5; i++) 
						pw.print(","+levelcredits[i]);
					pw.println();
				}

				// Marks by year...
				for (Integer year : syears) {		
					// main heading
					String stagetext = "";
					int istage = -1;
					if (year2stage.containsKey(year)) {
						istage = year2stage.get(year);
						stagetext = " ("+qual.stages[istage].name+")";
						if (stage2years.get(istage).size()>1) {
							stagetext += " (1 of "+stage2years.get(istage).size()+")";
						}
					}
					else if (preTransferYears.contains(year))
						stagetext = " (Ignored - assumed course transfer)";
					else if (qual==null) 
						stagetext = " (Unsupported qualification)";
					else {
						stagetext = " (Incomplete stage)";
					}
					List<Mark> yearMarks = new LinkedList<Mark>();
					//int yearCredits = 0;
					for (Mark m : smarks) 
						if (m.year==year) {
							yearMarks.add(m);
							//yearCredits += m.credit;
						}					
					printMarkSet(pw, "year "+year+stagetext, yearMarks, true);

				}
				
				if (qual!=null && qual.stages!=null && qual.stages.length>0) {
					// now allocate modules to stages...
					StageInfo stageInfos[] = new StageInfo[qual.stages.length];
					for (int istage=0; istage<qual.stages.length; istage++) {
						stageInfos[istage] = new StageInfo();
						if (qual.stages[istage].optional)
							stageInfos[istage].optional = true;
						stageInfos[istage].stageMarks = new LinkedList<Mark>();
						Stage stage = qual.stages[istage];
						if (stage.summerDissertation) {
							if (dissertationMark!=null)
								stageInfos[istage].stageMarks.add(dissertationMark);
						} else {
							TreeSet<Integer> stageyears = stage2years.get(istage);
							if (stageyears==null) {
								// stage not taken
								continue;
							}
							// assumed previous stages direct entry?
							for (int j=istage-1; j>=0; j--) {
								if (stageInfos[j].stageMarks.size()==0 && !qual.stages[j].optional)
									stageInfos[j].assumedDirectEntry = true;
							}
							for (Mark m : smarks) {
								if (stageyears.contains(m.year)) {
									if (DISSERTATION_SEMESTER.equals(m.semester) &&  m.credit==DISSERTATION_CREDITS) {
										// ignore
									}
									else {
										stageInfos[istage].stageMarks.add(m);
									}
								}
							}
						}
					}
					SupplementaryRegulations supplementaryRegulations = null;
					boolean checkedForRegulations = false;
					// then check progression at each intermediate stage...
					boolean failedToProgress = false;
					for (int istage=0; istage<qual.stages.length; istage++) {
						Stage stage = qual.stages[istage];
						StageInfo si = stageInfos[istage];
						if (si.stageMarks.size()==0) {
							pw.println("Debug: skip empty stage "+stage.name);
							continue;
						}
						// year of starting this stage
						int year = 0;// default
						if (stage2years.containsKey(istage))
							year = stage2years.get(istage).first();
 							
						if (failedToProgress)
							pw.println("Warning: checking progression from "+stage.name+" on "+qual.title+" but student has failed to progress from a previous stage");
						
						// get any supplementary regulations
						SupplementaryRegulations sr = supplementaryRegulations;
						if (!checkedForRegulations) {
							sr = getSupplementaryRegulations(s, year);
							if (sr==null)
								pw.println("Warning: could not find supplementary regulations for "+s.courseCode+" "+s.qualification+" for year "+year+"/"+(year+1)+" - cannot check non-compensatable modules");
							if (stage.affectsClassification()) {
								checkedForRegulations = true;
								// 'fix' choice of regulations
								supplementaryRegulations = sr;
							}
						}
						checkProgression(pw, qual, stage, sr, s, si);
						if (!si.progresses)
							failedToProgress = true;
						
						if (!si.progresses && !si.hasMissingMarks && !si.hasResitsOutstanding) {
							// transfer??
							for (int pi=0; qual.progressionAlternatives!=null && pi<qual.progressionAlternatives.length; pi++) {
								String altTitle = qual.progressionAlternatives[pi];
								Qualification altQual = findQualificationByTitle(altTitle);
								if (altQual==null) {
									pw.println("Configuration Error: could not find transfer option "+altTitle);
									continue;
								}
								if (altQual.stages==null || altQual.stages.length<=istage) {
									pw.println("Note: cannot transfer to "+altQual.title+" - it does not have "+stage.name);
									continue;
								}
								Stage altStage = altQual.stages[istage];
								SupplementaryRegulations altSr = getSupplementaryRegulations(s, year, altQual);
								//if (altSr!=null)
								//	pw.println("Debug: using supplementary regulations for "+altSr.qualificationName+" "+altSr.ucasCode+" "+altSr.year+" with non-compensatable modules "+altSr.noncompensatableModules);
								StageInfo altSi = new StageInfo();
								altSi.stageMarks = si.stageMarks;
								pw.println("Check progression on alternative qualification "+altQual.title+":");
								checkProgression(pw, altQual, altStage, altSr, s, altSi);
								
								if (altSi.progresses)
									break;
							}
						}
					}
					// then check classification at final stage...
					String result = null;
					int year = 0;// default
					Stage finalStage = qual.stages[qual.stages.length-1];
					StageInfo finalSi = stageInfos[qual.stages.length-1];
					if (finalSi.stageMarks.size()==0) {
						pw.println("Debug: skip empty final stage "+finalStage.name);
					}
					else
					{
						int istage = qual.stages.length-1;
						
						// year of starting this stage
						if (stage2years.containsKey(istage))
							year = stage2years.get(istage).first();
 							
						if (failedToProgress)
							pw.println("Warning: checking classification on "+qual.title+" but student has failed to progress from a previous stage");						

						// get any supplementary regulations
						SupplementaryRegulations sr = supplementaryRegulations;
						if (!checkedForRegulations) {
							sr = getSupplementaryRegulations(s, year);
							if (sr==null)
								pw.println("Warning: could not find supplementary regulations for "+s.courseCode+" "+s.qualification+" for year "+year+"/"+(year+1)+" - cannot check non-standard weightings, etc.");
						}

						result = checkClassification(pw, qual, sr, s, stageInfos, failedToProgress);
					}
					// check fallback qualification?
					if (result==null || failedToProgress) {
						for (int pi=0; qual.classificationAlternatives!=null && pi<qual.classificationAlternatives.length; pi++) {
							String altTitle = qual.classificationAlternatives[pi];
							Qualification altQual = findQualificationByTitle(altTitle);
							if (altQual==null) {
								pw.println("Configuration Error: could not find alternative award option "+altTitle);
								continue;
							}
							SupplementaryRegulations altSr = getSupplementaryRegulations(s, year, altQual);
							pw.println("Check classification on alternative qualification "+altQual.title+":");
							result = checkClassification(pw, altQual, altSr, s, stageInfos, false);
							
							if (result!=null)
								break;
						}
					}
				}				
				pw.println();
			}
			
			// students with marks but no other mention (typically non-CS)
			pw.println("Students with module marks but not in school list or further details (should be non-CS students):");
			pw.println("Student ID,Course Title,Year on course,Example Module,Module Year");
			TreeSet<String> allStudents = new TreeSet<String>();
			Map<String,Mark> exampleMarks = new HashMap<String,Mark>();
			TreeSet<String> studentsWithMarks = new TreeSet<String>();
			for (Mark m : allmarks) {
				if (m.student!=null) {
					allStudents.add(m.student);
					exampleMarks.put(m.student, m);
					if (m.bestmark()!=null)
						studentsWithMarks.add(m.student);
				}
			}
			for (String sid : studentsWithMarks) {
				if (students.contains(sid))
					continue;
				Mark m = exampleMarks.get(sid);
				pw.println(sid+","+m.courseTitle+","+m.yearOnCourse+","+m.module+","+m.year);
			}
			pw.println();

			pw.println("Students with module enrolment but no marks and not in school list or further details (should be applicants):");
			pw.println("Student ID,Course Title,Year on course,Example Module,Module Year");
			for (String sid : allStudents) {
				if (students.contains(sid) || studentsWithMarks.contains(sid))
					continue;
				Mark m = exampleMarks.get(sid);
				pw.println(sid+","+m.courseTitle+","+m.yearOnCourse+","+m.module+","+m.year);
			}
			pw.println();

			// dump qualifications and regulations
			pw.println("Qualifications definitions used:");
			pw.println();
			for (Qualification qual : QUALIFICATIONS) {
				pw.println("\""+qual.toString()+"\"");
				pw.println();
			}
			pw.println("Supplementary Regulations used:");
			pw.println();
			for (SupplementaryRegulations sr : supplementaryRegulations) {
				pw.println("\""+sr.toString()+"\"");
				pw.println();
			}
			
			pw.close();
			
			System.out.println("Modes of study: "+modesOfStudy);
			System.out.println("Qualifications: "+qualifications);
			System.out.println("Years of course: "+yearsOfCourse);
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}
	protected static Qualification getQualification(String qualification) {
		Qualification qual = null;
		for (int qi=0; qual==null && qi<QUALIFICATIONS.length; qi++) {
			Qualification q = QUALIFICATIONS[qi];
			for (int ti=0; q.names!=null && ti<q.names.length; ti++) {
				if (q.names[ti].equals(qualification)) {
					qual = q;
					break;
				}
			}
		}
		return qual;
	}
	private static String checkClassification(PrintWriter pw,
			Qualification qual, SupplementaryRegulations sr, Student s,
			StageInfo[] stageInfos, boolean failedToProgress) {
		TreeSet<String> noncompensatableModules = null;
		if (sr!=null)
			noncompensatableModules = sr.noncompensatableModules;
		// classify by weighted stage average(s)
		double weights[] = null;
		if (qual.stages!=null) {
			weights = new double[qual.stages.length];
			for (int si=0; si<qual.stages.length; si++) {
				if (qual.stages[si].optional)
					continue;
				double weight = qual.stages[si].defaultClassificationWeight;
				if (sr!=null && sr.stageWeightings!=null) {
					weight = 0;
					if (si<sr.stageWeightings.length)
						weight = sr.stageWeightings[si];
				}
				weights[si] = weight;
			}
		}
		else {
			weights = new double[stageInfos.length];
			for (int i=0; i<weights.length; i++)
				weights[i] = 1.0/weights.length;
		}
		pw.println("Debug: check classification for "+qual.title+"...");
		String result = null;
		String rule = null;
		boolean couldIncludeResits = false;
		boolean insufficientCredits = false;
		boolean hasResits = false;
		boolean failedCombinedProgressionRrequirements = false;
		// without resits, then with
		for (int resit=0; result==null && resit<=1; resit++) {
			if (resit!=0 && !couldIncludeResits && !failedCombinedProgressionRrequirements) {
				pw.println("Note: no classification for this qualification can include resits");
				break;
			}
			if (resit!=0 && !hasResits)
				break;
			pw.println((resit!=0?"Resit":"First attempt")+",Stage,Weight,Credits,Stage credits,Average,Modules ignored");
			double average = 0;
			List<Mark> contributingMarks = new LinkedList<Mark>();
			
			// for each stage, find best modules and stage average, and collect contributing modules 
			for (int si=0; si<stageInfos.length; si++) {
				String stageName = qual.stages!=null && qual.stages.length>si ? qual.stages[si].name : "stage "+(si+1);
				double weight = 1;
				String weightText = "";
				
				if (stageInfos[si].optional)
					// ignore, at least for now
					continue;
				
				Stage stage = null;
				if (qual.combinedClassificationRequirements==null) {
					if (si>=weights.length) {
						if (stageInfos[si].stageMarks.size()>0)
							pw.println("Warning: marks for stage "+stageName+" but this stage is not covered by qualification");
						continue;
					}
					// not combined
					weight = weights[si];
					weightText = df2Format(weight);
					if (weight<=0)
						// real zero weight
						continue;
					if (qual.stages!=null && si>=qual.stages.length) {
						pw.println("Error: marks for stage "+si+" but this stage is not covered by qualification");
						continue;
					}
					stage = qual.stages!=null ? qual.stages[si] : null;				
				} 
				else if (qual.stages!=null) {
					// zero weight is still significant - for Ordinary against Part I/II (III) only
					if (si<weights.length && weights[si]<=0) {
						pw.println("\"Note: marks for "+qual.stages[si].name+" are not considered for this award\"");
						continue;
					}
				}
				if (stageInfos[si].stageMarks.size()==0) 
				{
					if (qual.stages!=null)
						pw.println("Warning: no marks for "+stageName+" "+weightText);
					continue;
				}
				
				if (resit==0)
					// check if any resits - avoid checking resits if not
					for (Mark m : stageInfos[si].stageMarks)
						if (m.secondmark!=null) {
							hasResits = true;
							break;
						}
				
				int stageCredits = 0;
				if (stage!=null) {
					for (int ri=0; stage.requirements!=null && ri<stage.requirements.length; ri++)
						if (stage.requirements[ri].credits > stageCredits)
							stageCredits = stage.requirements[ri].credits;
				}
				else {
					for (Mark m : stageInfos[si].stageMarks)
						stageCredits += m.credit;
				}
							
				List<Mark> bestMarks = null;
				if (stage!=null)
					bestMarks = getBestMarks(stageInfos[si].stageMarks, stageCredits, resit!=0, noncompensatableModules, stage.requirements);
				else {
					bestMarks = stageInfos[si].stageMarks;
				}
				
				if (qual.combinedClassificationRequirements!=null) 
					contributingMarks.addAll(stageInfos[si].stageMarks);
				else
					contributingMarks.addAll(bestMarks);

				MarkProfile mp = getMarkProfile(bestMarks, resit!=0);
				if (mp.credits<stageCredits)
					insufficientCredits = true;
				double stageAverage = mp.average*mp.credits/stageCredits;
				average += weight*stageAverage;
				String missingModules = identifyMissingModules(stageInfos[si].stageMarks, bestMarks);
				if (missingModules.length()>0 && stage!=null && stage.requirements!=null)
					missingModules += " - "+requirementsText(stage.requirements);
				pw.println(","+stageName+","+weightText+","+mp.credits+","+stageCredits+","+df2Format(stageAverage)+",\""+missingModules+"\"");				
			}
			// stage average
			//average = Math.floor(average*10)/10;
			if (qual.combinedClassificationRequirements==null && qual.stages!=null)
				pw.println(",Final"+(resit!=0 ? " inc. resits":"")+",,,,"+df2Format(average));
			
			// special case where stages are ignored for final classification, i.e. CS Ordinary
			if (qual.combinedClassificationRequirements!=null) {
				// recheck this overall!
				insufficientCredits = false;
				if (!satifiesReqirements(qual.combinedClassificationRequirements, contributingMarks)) 
				{
					pw.println("Warning: not enough combined credits/levels to satisfy the award requirements for "+qual.title);
					return null;
				}
				int combinedCredits = 0;
				for (int ri=0; qual.combinedClassificationRequirements!=null && ri<qual.combinedClassificationRequirements.length; ri++)
					if (qual.combinedClassificationRequirements[ri].credits > combinedCredits)
						combinedCredits = qual.combinedClassificationRequirements[ri].credits;

				List<Mark> bestMarks = getBestMarks(contributingMarks, combinedCredits, resit!=0, noncompensatableModules, qual.combinedClassificationRequirements);
				pw.println("\"Note: best "+requirementsText(qual.combinedClassificationRequirements)+"\"");
				printMarkSet(pw, "Best "+combinedCredits, bestMarks, resit!=0);
				
				MarkProfile mp = getMarkProfile(bestMarks, resit!=0);
				// progression?
				if (qual.combinedProgressionRrequirements!=null) {
					boolean progresses = false;
					for (int pi=0; !progresses && pi<qual.combinedClassificationRequirements.length; pi++)
						if (qual.combinedProgressionRrequirements[pi].satisfiedBy(mp)) { 
							pw.println("\"Note: qualifies for consideration for "+qual.title+" according to rule "+qual.combinedProgressionRrequirements[pi].name+"\"");
							progresses = true;
						}
					if (!progresses) {
						if (resit!=0 || !hasResits)
							pw.println("\"Warning: marks do not satisfy the requirements for consideration for "+qual.title+"\"");
						else
							pw.println("\"Note: marks do not satisfy the requirements for consideration for "+qual.title+"\"");
						failedCombinedProgressionRrequirements = true;
						continue;
					}
				}
				else
					pw.println("\"Note: this program has no progression-style mark constraints for award of "+qual.title+"\"");
				
				double combinedAverage = mp.average*mp.credits/combinedCredits;//Math.round(mp.average*mp.credits/combinedCredits);
				String missingModules = identifyMissingModules(contributingMarks, bestMarks);
				pw.println(",Best "+combinedCredits+" credits,,"+mp.credits+","+combinedCredits+","+df2Format(combinedAverage)+",\""+missingModules+"\"");
				if (mp.credits<combinedCredits && !insufficientCredits) {
					insufficientCredits = true;
					//pw.println("Error: not enough combined credits - "+mp.credits+" vs "+combinedCredits);
				}
				average = combinedAverage;
				contributingMarks = bestMarks;
			}
			
			// now check each classification option in turn
			for (int ci=0; result==null && qual.classificationRules!=null && ci<qual.classificationRules.length; ci++) {
				ClassificationRule cr = qual.classificationRules[ci];
				if (cr.includeResits)
					couldIncludeResits = true;
				if (resit!=0 && !cr.includeResits)
					// nope
					continue;
				if (average<cr.average)
					// too low
					continue;
				if (cr.requirement!=null) {
					int credits = 0;
					// additional requirement across relevant stages
					for (Mark m : contributingMarks)
						if ((m.mark!=null && m.mark>= cr.requirement.mark) || (resit!=0 && m.bestmark()!=null && m.bestmark() >= cr.requirement.mark))
							credits += m.credit;
					if (credits < cr.requirement.credits)
						continue;
				}
				// ok!
				result = cr.result;
				rule = cr.rule;
			}
		}
		if (result!=null) {
			if (insufficientCredits)
				pw.println("Note: students is missing marks; from current marks alone their result would be "+result);
			else if (failedToProgress)
				pw.println("Note: students has failed to progress to award; otherwise their result would be "+result);
			else
				pw.println("Note: final result for "+qual.title+" is "+result+" (by rule "+rule+")");
		}
		else if (insufficientCredits){
			pw.println("Note: students is missing marks; from current marks alone they would fail");
		} else {
			pw.println("Warning: final result for "+qual.title+" is fail");
			boolean hasFinalStageResitsOutstanding = false;
			StageInfo finalSi = stageInfos[stageInfos.length-1];
			for (Mark m : finalSi.stageMarks)
				if (m.secondmark==null && (m.mark==null || m.mark < qual.getPassMark())) {
					hasFinalStageResitsOutstanding = true;
					break;
				}
			if (couldIncludeResits && hasFinalStageResitsOutstanding)
				pw.println("Note: Could still take final stage resit(s)");
		}
		return result;
	}
	private static Qualification findQualificationByTitle(String altTitle) {
		// first we need to find the qualification...
		Qualification altQual = null;
		for (int qi=0; altQual==null && qi<QUALIFICATIONS.length; qi++) {
			Qualification q = QUALIFICATIONS[qi];
			if (altTitle.equals(q.title))
				altQual = q;
		}
		if (altQual==null) {
			System.err.println("Error: Could not find alternative qualification "+altTitle);
		}
		return altQual;
	}
	private static void checkProgression(PrintWriter pw, Qualification qual, Stage stage, SupplementaryRegulations sr, Student s, StageInfo si) {
		TreeSet<String> noncompensatableModules = null;
		if (sr!=null)
			noncompensatableModules = sr.noncompensatableModules;

		// check total credits
		MarkProfile stageProfile = getMarkProfile(si.stageMarks, false);

		int stageCredits = 0;
		for (int ri=0; stage.requirements!=null && ri<stage.requirements.length; ri++)
			if (stage.requirements[ri].credits > stageCredits)
				stageCredits = stage.requirements[ri].credits;

		if (stageProfile.credits>stageCredits) {
			if (s.qualification.equals(BSC_ORDINARY) && stageProfile.credits-stageCredits<=20)
				pw.println("Note: taking best "+stageCredits+" out of "+stageProfile.credits+" credits for assumed transfer to Ordinary");
			else
				pw.println("Warning: student has extra "+(stageProfile.credits-stageCredits)+" credits for "+stage.name+" - taking best marks");
		}
		if (stageProfile.credits<stageCredits) {
			printMarkSet(pw, stage.name+" (incomplete)", si.stageMarks, true);
			pw.println("Warning: student does not have enough marks yet to satisfy stage "+stage.name);
			for (Mark m : si.stageMarks) {
				if (sr!=null && sr.noncompensatableModules.contains(m.module)) {
					if (m.bestmark()==null) {
						//pw.println("Warning: no mark for non-compensatable module "+m.module);
					} else if (m.bestmark()<qual.getPassMark()) {
						pw.println("Warning: failed non-compensatable module "+m.module);
					}
				}
			}
			si.hasMissingMarks = true;
		}
		else {
			boolean progresses = false;							
			if (stageProfile.credits<=stageCredits) 
				printMarkSet(pw, stage.name, si.stageMarks, true);
			// first marks
			{
				List<Mark> bestMarks = getBestMarks(si.stageMarks, stageCredits, false, noncompensatableModules, stage.requirements);
				String missingModules = identifyMissingModules(si.stageMarks, bestMarks);
				if (missingModules.length()>0) {
					if (stage.requirements!=null)
						pw.print("\"Note: best "+requirementsText(stage.requirements)+";");
					else 
						pw.print("\"Note: ");
					pw.println("ignoring modules "+missingModules+"\"");
				}
				if (stageProfile.credits>stageCredits) 
					printMarkSet(pw, stage.name+" (first)", bestMarks, false);
				MarkProfile bestMp = getMarkProfile(bestMarks, false);
				// progression options?
				boolean failedNoncompensatable = false;
				for (Mark m : bestMarks) {
					if (sr!=null && sr.noncompensatableModules.contains(m.module)) {
						if (m.mark==null) {
							if (!stageProfile.hasResits)
								pw.println("Warning: no mark for non-compensatable module "+m.module);
							failedNoncompensatable = true;
						} else if (m.mark<qual.getPassMark()) {
							if (!stageProfile.hasResits)
								pw.println("Warning: failed non-compensatable module "+m.module);
							failedNoncompensatable = true;										
						}
					}
				}
				for (int pi=0; bestMp.credits>=stageCredits && !progresses && stage.progressionRules!=null && pi<stage.progressionRules.length; pi++) {
					if (stage.progressionRules[pi].satisfiedBy(bestMp)) {
						if (failedNoncompensatable) {
							if (!stageProfile.hasResits)
								pw.println("Note: would progresses from "+stage.name+" by rule "+stage.progressionRules[pi].name+" except for failed non-compensatable module");										
							break;
						}
						else {
							pw.println("Progresses from "+stage.name+" by rule "+stage.progressionRules[pi].name);
							progresses = true;
						}
					}
				}
				if (bestMp.credits>=stageCredits && !progresses && stage.progressionRules==null) {
					if (failedNoncompensatable) {
						if (!stageProfile.hasResits) 
							pw.println("Note: would complete from "+stage.name+" except for failed non-compensatable module");
					}
					else {
						if (!stageProfile.hasResits) {
							pw.println("Completes "+stage.name+" (no progression rules)");
							progresses = true;
						}
					}								
				}
			}
			if (!progresses && stageProfile.hasResits && !stage.progressionExcludesResits) {
				// resits
				List<Mark> bestMarks = getBestMarks(si.stageMarks, stageCredits, true, noncompensatableModules, stage.requirements);
				String missingModules = identifyMissingModules(si.stageMarks, bestMarks);
				if (missingModules.length()>0) {
					if (stage.requirements!=null)
						pw.print("\"Note: best "+requirementsText(stage.requirements)+";");
					else 
						pw.print("\"Note: ");
					pw.println("ignoring modules "+missingModules+"\"");
				}
				if (stageProfile.credits>stageCredits) 
					printMarkSet(pw, stage.name+" (best)", bestMarks, true);
				MarkProfile bestMp = getMarkProfile(bestMarks, true);
				// progression options?
				boolean failedNoncompensatable = false;
				for (Mark m : bestMarks) {
					if (sr!=null && sr.noncompensatableModules.contains(m.module)) {
						if (m.bestmark()==null) {
							pw.println("Warning: no mark for non-compensatable module "+m.module);
							failedNoncompensatable = true;
						} else if (m.bestmark()<qual.getPassMark()) {
							pw.println("Warning: failed non-compensatable module "+m.module);
							failedNoncompensatable = true;										
						}
					}
				}
				for (int pi=0; bestMp.credits>=stageCredits && !progresses && stage.progressionRules!=null && pi<stage.progressionRules.length; pi++) {
					if (stage.progressionRules[pi].satisfiedBy(bestMp)) {
						if (failedNoncompensatable) {
							pw.println("Note: would progress from "+stage.name+" by rule "+stage.progressionRules[pi].name+" except for failed non-compensatable module");										
							break;
						}
						else {
							pw.println("Progresses from "+stage.name+" by rule "+stage.progressionRules[pi].name);
							progresses = true;
						}
					}
				}
				if (bestMp.credits>=stageCredits && !progresses && stage.progressionRules==null) {
					if (failedNoncompensatable) {
						pw.println("Note: would complete from "+stage.name+" except for failed non-compensatable module");										
					}
					else {
						pw.println("Completes "+stage.name+" (no progression rules)");
						progresses = true;
					}
				}								
			}
			if (!progresses) {
				if (stage.progressionExcludesResits) {
					pw.println("Warning: does not progress from stage "+stage.name+" (resits not considered for progression)");									
				} else {
					boolean hasResitsOutstanding = false;
					for (Mark m : si.stageMarks) {
						if (m.mark!=null && m.mark<qual.getPassMark() && m.secondmark==null) {
							//pw.println("Debug: awaiting resit mark for "+m.module);
							hasResitsOutstanding = true;
						}
					}
					si.hasResitsOutstanding = hasResitsOutstanding;
					if (hasResitsOutstanding)
						pw.println("Warning: does not currently progress from stage "+stage.name+" but has resits outstanding");
					else
						pw.println("Warning: does not progress from stage "+stage.name+" (no resits outstanding)");
				}
			}
			si.progresses = progresses;
		}
	}
	static class TranscriptMarkComparator implements Comparator<Mark> {

		//@Override
		public int compare(Mark m1, Mark m2) {
			if (m1.year<m2.year)
				return -1;
			if (m1.year>m2.year)
				return 1;
			return m1.module.compareTo(m2.module);
		}		
		
	}
	static class FirstMarkComparator implements Comparator<Mark> {

		//@Override
		public int compare(Mark m1, Mark m2) {
			int mark1 = m1.mark==null ? 0 : m1.mark;
			int mark2 = m2.mark==null ? 0 : m2.mark;
			if (mark1>mark2)
				return -1;
			if (mark1<mark2)
				return 1;
			return 0;
		}		
		
	}
	static class BestMarkComparator implements Comparator<Mark> {

		//@Override
		public int compare(Mark m1, Mark m2) {
			int mark1 = m1.bestmark()==null ? 0 : m1.bestmark();
			int mark2 = m2.bestmark()==null ? 0 : m2.bestmark();
			if (mark1>mark2)
				return -1;
			if (mark1<mark2)
				return 1;
			return 0;
		}		
		
	}
	static class ModuleNameComparator implements Comparator<Mark> {

		//@Override
		public int compare(Mark m1, Mark m2) {
			if (m1.module==null)
				return m2.module==null ? 0 : 1;
			return m1.module.compareTo(m2.module);
		}		
		
	}
	static class ModuleSemesterAndNameComparator extends ModuleNameComparator {

		//@Override
		public int compare(Mark m1, Mark m2) {
			if (m1.semester==null)
				return m2.semester==null ? super.compare(m1,m2) : 1;
			int val = m1.semester.compareTo(m2.semester);
			if (val==0)
				return super.compare(m1, m2);
			else
				return val;
		}		
		
	}
	static int getModuleLevel(String module) {
		char level = module.charAt(2);
		if (level>='0' && level<='5')
			return level-'0';
		else if (level>='A' && level<='D')
			return level-'A'+2;
		else {
			System.err.println("Error: invalide module level: "+level+" in code "+module);
			return 0;
		}
	}
}
