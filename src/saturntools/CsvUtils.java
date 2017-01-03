package saturntools;

import java.util.Vector;

public class CsvUtils {
	/** parse csv line with escaping (, -> "...,..."; "  -> "...""...")
	 */
	static Vector<String> parseCsvLine(String line) 
	{
		int pos = 0;
		Vector<String> vals = new Vector<String>();
		while(pos<line.length()) 
		{
			StringBuffer text = new StringBuffer();
			boolean quoted = false;
			boolean justUnquoted = false;
			for (; pos<line.length() && 
			!(line.charAt(pos)==',' && !quoted); pos++) 
			{

				if (line.charAt(pos)=='"') 
				{
					if (justUnquoted)
						text.append('"');
					quoted = !quoted;
					justUnquoted = !quoted;
				}
				else 
				{
					justUnquoted = false;
					text.append(line.charAt(pos));
				}
			}
			vals.addElement(text.toString());
			if (pos<line.length() && line.charAt(pos)==',') 
			{
				pos++;
				if (pos>=line.length())
					// trailing empty value
					vals.addElement("");
			}
		}
		return vals;
	}
	static String escape(String s) {
		if (s==null)
			return "";
		if (!s.contains(",") && !s.contains("\n") && !s.contains("\f") && !s.contains("\r") && !s.startsWith("\""))
			return s;
		if (!s.contains("\"") && !s.contains("\n") && !s.contains("\f") && !s.contains("\r"))
			return "\""+s+"\"";
		StringBuffer b = new StringBuffer();
		b.append("\"");
		for (int i=0; i<s.length();i++) {
			char c= s.charAt(i);
			switch (c) {
			case '\n':
			case '\f':
			case '\r':
				b.append(" ");// whitespace??!
				break;
			case '"':
				b.append('"');
			//case '\\':
				//b.append('\\');
				// fallthrough
			default:
				if (Character.isISOControl(c) || ((int)c)>127)
					b.append('#');
				else
					b.append(c);
			}			
		}
		b.append("\"");
		return b.toString();
	}

}
