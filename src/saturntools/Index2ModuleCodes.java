package saturntools;
import java.io.*;
import java.util.regex.*;
/** reads a saturn module maintainance module list html page and outputs
 * internal saturn ids and module codes.
 * 
 * @author cmg
 *
 */
public class Index2ModuleCodes {
  public static void main(String args[]) {
    try {
      Pattern p = Pattern.compile("ViewModule.asp[?]crs_id=([0-9]*)[^\\?]*alt='View Module ([^']*)'");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      while(true) {
	String line = br.readLine();
	if (line==null) 
	  break;
	Matcher m = p.matcher(line);
	while (m.find()) {
	  System.out.println(m.group(1)+" "+m.group(2));
	}	
      }
    } catch (Exception e) {
      System.err.println("Error: "+e);
      e.printStackTrace(System.err);
    }
  }

}
