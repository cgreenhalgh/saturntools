/**
 * 
 */
package saturntools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

/** Flip a CSV file diagonally
 * @author cmg
 *
 */
public class CsvMirror {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length!=1) {
			System.err.println("Usage: <csvfile>");
			System.exit(-1);			
		}
		File infile = new File(args[0]);
		try {
			Vector<Vector<String>> lines = new Vector<Vector<String>>();
			BufferedReader br = new BufferedReader(new FileReader(infile));
			int maxCols = 0;
			while(true) {
				String line = br.readLine();
				if (line==null)
					break;
				Vector<String> cols = CsvUtils.parseCsvLine(line);
				if (maxCols<cols.size())
					maxCols = cols.size();
				lines.add(cols);
			}
			// cols -> rows
			for (int ci=0; ci<maxCols; ci++) {
				System.err.println("Col "+ci+"/"+maxCols);
				for (int ri=0; ri<lines.size(); ri++) {
					Vector<String> cols = lines.get(ri);
					if (cols.size()>ci)
						System.out.print(CsvUtils.escape(cols.get(ci)));
					if (ri+1<lines.size())
						System.out.print(",");					
				}
				System.out.println();
			}
		}
		catch( Exception e) {
			System.err.println("Error mirroring "+infile+": "+e);
			e.printStackTrace(System.err);
		}
		System.exit(0);
	}

}
