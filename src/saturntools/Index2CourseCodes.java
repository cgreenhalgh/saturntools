package saturntools;

import java.io.*;
import java.util.regex.*;
/** reads a saturn course maintainance course list HTML file and outputs
 * internal course codes and other info in CSV
 * @author cmg
 *
 */
public class Index2CourseCodes
{
    public static void main(String args[])
    {
	try
	{
	    //../asp/sos_details.asp?crs_id=018617&year_id=000110"><img SRC="../images/SmallInfo.GIF" ALT="SoS Information" ALIGN="right" BORDER="0" WIDTH="18" HEIGHT="17"></a>
	    //							</TD></TR><TR><TD>G403</TD><TD>MSc</TD><TD>Advanced Computing Science<TD>Part time</TD><TD>PG Part-time</TD></TD>
	    Pattern p = Pattern.compile("sos_details.asp[?]crs_id=([0-9]*)[^\\?]*<TR><TD>([^<]*)</TD><TD>([^<]*)</TD><TD>([^<]*)<TD>([^<]*)</TD><TD>([^<]*)</TD>");
	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    StringBuffer buf = new StringBuffer();
	    while (true)
	    {
		String line = br.readLine();
		if (line == null)
		    break;
		buf.append(line);
		buf.append("\n");
	    }
	    Matcher m = p.matcher(buf.toString());
	    while (m.find())
	    {
		System.out.println(m.group(1)+ "," + escape(m.group(2)) + "," + escape(m.group(3)) + "," + escape(m.group(4)) + "," + escape(m.group(5)) + "," + escape(m.group(6)));
	    }
	}
	catch (Exception e)
	{
	    System.err.println("Error: " + e);
	    e.printStackTrace(System.err);
	}
    }
    static String escape(String s)
    {
	StringBuffer b = new StringBuffer();
	boolean toUpper = false;
	for (int i = 0; i < s.length(); i++)
	{
	    char c = s.charAt(i);
	    if (Character.isLetterOrDigit(c))
	    {
		if (toUpper)
		{
		    b.append(Character.toUpperCase(c));
		    toUpper = false;
		}
		else
		    b.append(c);
	    }
	    else
		toUpper = true;
	}
	return b.toString();
    }
}
