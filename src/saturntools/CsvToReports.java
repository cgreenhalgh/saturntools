/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Vector;

import org.apache.log4j.Logger;

/** Convert a CSV file (e.g. marks spreadsheet) into individual feedback files.
 * 
 * @author cmg
 *
 */
public class CsvToReports {
	private static final String CHARSET = "UTF-8";
	private static final String FILE_SUFFIX = ".txt";
	/** logger */
    static Logger logger = Logger.getLogger(CsvToReports.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length!=3) {
			System.err.println("Usage: <csvfile> <outdir> <keycolumn>");
			System.exit(-1);
		}
		File csvfile = new File(args[0]);
		File outdir = new File(args[1]);
		String keycolumn = args[2];
		try {
			// charset?
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvfile), CHARSET));
			String headerline = br.readLine();
			if (headerline==null)
				throw new IOException("Did not find header line in "+csvfile);
			Vector<String> headers = CsvUtils.parseCsvLine(headerline);
			int keycolumnix = headers.indexOf(keycolumn);
			if (keycolumnix<0)
				throw new IOException("Did not find keycolumn "+keycolumn+" in "+csvfile);
			if (!outdir.exists()) {
				logger.info("Creating output directory "+outdir);
				outdir.mkdirs();
			}
			if (!outdir.isDirectory())
				throw new IOException("Cannot write to output directory "+outdir);
			int count = 1;
			while(true) {
				count++;
				String line= br.readLine();
				if (line==null)
					break;		
				Vector<String> values = CsvUtils.parseCsvLine(line);
				if (values.size()<=keycolumnix)
				{
					logger.warn("Ignore line "+count+" - no keycolumn value: "+values);
					continue;
				}
				String key = values.get(keycolumnix);
				if (key==null || key.length()==0) {
					logger.warn("Ignore line "+count+" - keycolumn value undefined: "+values);
					continue;
				}
				File outfile = new File(outdir, key+FILE_SUFFIX);
				if (outfile.exists())
					logger.warn("File "+outfile+" already exists - replacing");
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile), CHARSET));
				for (int col=0; col<headers.size() && col<values.size(); col++) {
					String header = headers.get(col);
					String value = values.get(col);
					if (value!=null && value.trim().length()!=0) {
						pw.println(header+": "+value);
						pw.println();
					}
				}
				pw.close();
				logger.info("Wrote "+outfile);
			}
			br.close();
			logger.info("Done");
		}
		catch (Exception e) {
			logger.error("Error reading "+csvfile+", output to "+outdir+" with keycolumn "+keycolumn, e);
		}
	}

}
