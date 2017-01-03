/**
 * 
 */
package saturntools;

import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

/** Html Document for processing
 * @author cmg
 *
 */
public class HtmlDocument {
	/** logger */
	static Logger logger = Logger.getLogger(HtmlDocument.class);
	public static void main(String args[]) {
		try {
			for (int i=0; i<args.length; i++) {
				logger.info("Read "+args[i]);
				HtmlDocument doc = new HtmlDocument(null, args[0]);
				doc.dump();
			}
		}
		catch (Exception e) {
			logger.error("Error", e);
		}
	}
	/** download and process */
	public HtmlDocument(String baseUrl, String path) throws MalformedURLException, IOException {
		InputStreamReader isr = null;
		if (baseUrl==null || baseUrl.indexOf(":")<0) {
			// file?!
			logger.debug("Read as file "+path);
			isr = new FileReader(path);
			fileLastModified = new Date(new File(path).lastModified()).toString();
		}
		else {
			url = new URL(baseUrl+path);
			URLConnection conn = url.openConnection();
			String encoding = conn.getContentEncoding();
			if (encoding==null) {
				logger.debug("Warning: No encoding returned for "+path);
				isr = new InputStreamReader(conn.getInputStream());
			}
			else
				isr = new InputStreamReader(conn.getInputStream(), encoding);
		}
		char buf [] = new char[10000];
		int count = 0;
		while(true) {
			if (count>=buf.length) {
				char nbuf[] = new char[buf.length*4];
				System.arraycopy(buf, 0, nbuf, 0, count);
				buf = nbuf;				
			}
			int n = isr.read(buf, count, buf.length-count);
			if (n<0)
				break;
			count += n;
		}
		content = new String(buf, 0, count);
		//logger.debug("Retrieved "+content+" from "+baseUrl+path);
		root = parse(content);
		// TODO
	}
	/** url */
	protected URL url;
	/** content */
	protected String content;
	/** root */
	protected Node root;
	/** last updated */
	protected String fileLastModified;
	/** get last updated - may only work for local file at the mo */
	public String getFileLastModified() {
		return fileLastModified;
	}
	/** node */
	class Node {
		protected Node parent;
		protected int from;
		protected int to;
		protected int bodyFrom;
		protected int bodyTo;
		protected String name;
		protected Vector<Node> children = new Vector<Node>();
		/** cons */
		Node(Node parent, int from, int to, String name) {
			this.parent = parent;
			this.from = from;
			this.to = this.bodyFrom = this.bodyTo = to;
			this.name = name;
		}
		/** Print */
		public String toString() {
			String body = dump(content.substring(bodyFrom, bodyTo).trim());
			return "Node "+name+", "+from+"-"+to+" \""+
					(body.length()>20 ? body.substring(0, 17)+"..." : body)+"\"";
		}
		/** body text */
		public String getBodyText() {
			String body = content.substring(bodyFrom, bodyTo).trim();
			return body;
		}
		/** body text excluding tags */
		public String getPureBodyText() {
			StringBuffer buf = new StringBuffer();
			int pos = bodyFrom;
			int nextChild = 0;
			while (pos<bodyTo) {
				if (nextChild<children.size()) {
					buf.append(content.substring(pos, children.get(nextChild).from));
					buf.append(children.get(nextChild).getPureBodyText());
					pos = children.get(nextChild).to;
				}
				nextChild++;
				if (nextChild>=children.size()) {
					if (nextChild-1>=0 && nextChild-1<children.size())
						buf.append(content.substring(children.get(nextChild-1).to, bodyTo));
					else
						buf.append(content.substring(bodyFrom, bodyTo));
					break;
				}
			}
			return buf.toString().replace("&nbsp;"," ").replace("&nbsp"," ").trim();
		}
		/** get attribute value */
		public String getAttributeValue(String attributeName) {
			String value = null;
			int ix = this.from;
			while(ix<this.bodyFrom) {
				// skip to whitespace
				if (!Character.isWhitespace(content.charAt(ix))) {
					ix++;
					continue;
				}
				// skip the whitespace
				while (ix<this.bodyFrom && Character.isWhitespace(content.charAt(ix)))
						ix++;
				if (ix>=this.bodyFrom)
					break;				
				if (!Character.isLetter(content.charAt(ix))) {
					logger.debug("Ignore non-letter "+content.charAt(ix)+" in "+this);
					ix++;
					continue;
				}
				int nameFrom = ix;
				while (ix+1<this.bodyFrom && Character.isLetter(content.charAt(ix+1)))
					ix++;
				ix++;
				int nameTo = ix;
				String name = content.substring(nameFrom, nameTo);
				while (ix<this.bodyFrom && Character.isWhitespace(content.charAt(ix)))
						ix++;
				if (ix>=this.bodyFrom)
					break;
				if (content.charAt(ix)!='=') {
					logger.debug("Warning: found possible attribute '"+name+"' followed by "+content.charAt(ix)+" (not =)");
					continue; // eh?
				}
				ix++;
				while (ix<this.bodyFrom && Character.isWhitespace(content.charAt(ix)))
						ix++;
				if (ix>=this.bodyFrom)
					break;
				char quote = content.charAt(ix);
				if (quote!='\'' && quote!='"') {
					logger.debug("Warning: found possible attribute '"+name+"' followed by ="+content.charAt(ix)+" (not ='/\")");
					continue; // eh?
				}
				ix++;
				int valueFrom = ix;
				while(ix<this.bodyFrom && content.charAt(ix)!=quote)
					ix++;
				if (ix>=this.bodyFrom) {
					logger.debug("End of node in attribute value "+content.substring(nameFrom, bodyFrom));
					break;					
				}
				if (name.toLowerCase().equals(attributeName)) {
					value = content.substring(valueFrom, ix);
					return value;
				}
				ix++;
			}
			// not found
			return null;
		}
		/** return list of immediate child Nodes of given name within node list */
		public Vector<Node> getChildElements(String elementName) {
			elementName = elementName.toLowerCase().trim();
			Vector<Node> nodes = new Vector<Node>();
			for (int i=0; children!=null && i<children.size(); i++) 
			{
				Node n = children.get(i);
				if (n.name.toLowerCase().equals(elementName)) {
					nodes.add(n);
				}
			}
			return nodes;
		}
		/** next node in parents list */
		public Node getNextSibling() {
			if (parent==null)
				return null;
			for (int i=0; i+1<parent.children.size(); i++) {
				if (parent.children.get(i)==this)
					return parent.children.get(i+1);
			}
			return null;
		}
	}
	/** parse */
	protected Node parse(String content) {
		Vector<Node> nodes = new Vector<Node>();
		Node root = null;
		// find an element...
		int from = 0;
		while(true) {
			int ix = content.indexOf("<", from);
			if (ix<0 || ix+1>=content.length())
				break;
			StringBuffer sb = new StringBuffer();
			from = ix;
			ix++;
			String endToken = ">";
			while(true) {
				if (ix>=content.length())
					break;
				char nc = content.charAt(ix);
				if (nc=='>' || Character.isWhitespace(nc))
					break;
				sb.append(nc);
				if (sb.toString().equals("!--")) {
					// comment special case
					endToken = "-->";
					break;
				}
				if (sb.toString().equals("![CDATA[")) {
					// CDATA special case
					endToken = "]]>";					
					break;
				}
				if (sb.toString().equals("?")) {
					// processing command special case
					endToken = "?>";					
					break;
				}
				ix++;
			}
			String token = sb.toString();
			ix = content.indexOf(endToken, ix);
			if (ix<0) {
				throw new RuntimeException("Unclosed XML element <"+token+" at "+from+": "+content.substring(from));
			}
			if (token.startsWith("!")) {
				// not a normal element
				from = ix+endToken.length();
				continue;
			}
			else if (token.startsWith("/")) {
				// end element 
				boolean done = false;
				int i;
				for (i=nodes.size()-1; i>=0; i--) {
					Node n = nodes.get(i);
					if (n.name.equals(token.substring(1))) {
						n.bodyTo = from;
						n.to = ix+endToken.length();
						nodes.removeElementAt(i);
						done = true;
						break;
					}
				}
				if (done) {
					while (nodes.size()>i) {
						Node n = nodes.get(i);
						nodes.removeElementAt(i);
						logger.debug("Discard unclosed node "+n.name+" at "+ix+" (handling "+token+")");
					}
				}
				else {
					logger.debug("Found end element "+token+" with no matching start element");
				}
			}
			else {
				// same as element name on stack? assume close? for 'p' anyway :-/
				if (token.toLowerCase().equals("p") || token.toLowerCase().equals("tr") || token.toLowerCase().equals("td")) {
					for (int i=nodes.size()-1; i>=0; i--) {
						Node n = nodes.get(i);
						if (n.name.toLowerCase().equals(token.toLowerCase())) {
							n.bodyTo = from;
							n.to = from;
							nodes.removeElementAt(i);
							break;
						}
					}
				}
				
				// start element
				Node parent = (nodes.size()==0 ? null : nodes.get(nodes.size()-1));
				Node n = new Node(parent, from, ix+endToken.length(), token);
				if (nodes.size()==0) 
				{
					if (root==null)
						root = n;
					else
						throw new RuntimeException("Found second 'root' element, "+token+" at "+from);					
				}
				else
					nodes.get(nodes.size()-1).children.add(n);
				nodes.add(n);
				
				// empty? or special case of 'br'
				if (content.charAt(ix-1)=='/' || n.name.toLowerCase().equals("br") /* || n.name.toLowerCase().equals("p") */) {
					nodes.removeElementAt(nodes.size()-1);
				}
			}
			from = ix+endToken.length();
		}
		if (root==null)
			throw new RuntimeException("Found no root element");
		return root;
	}
	/** dump */
	public void dump() {
		dump(root, 0);
	}
	protected void dump(Node n, int level) {
		for (int i=0; i<level; i++)
			System.out.print("  ");
		System.out.println(n);
		for (Node nc: n.children) {
			dump(nc, level+1);
		}
	}
	protected static String dump(String s) {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c))
				b.append(" ");
			else if (Character.isISOControl(c))
				b.append("?");
			else
				b.append(c);
		}
		return b.toString();
	}
	/** return list of Nodes within section */
	public Vector<Node> getSectionNodes(String headingNode, String headingName) {
		headingNode = headingNode.toLowerCase();
		headingName = headingName.toLowerCase().trim();
		Vector<Node> nodes = new Vector<Node>();
		// depth-first traversal
		Vector<Node> ns = new Vector<Node>();
		ns.add(this.root);
		boolean found = false;
		while(ns.size()>0) {
			Node n = ns.get(0);
			ns.remove(0);
			if (n.name.toLowerCase().equals(headingNode)) {
				String body = content.substring(n.bodyFrom, n.bodyTo).trim().toLowerCase();
				if (body.startsWith(headingName)) {
					// found!
					found = true;
				}
				else 
					found = false;
			}
			else if (found) {
				nodes.add(n);
				// no recurse
			} else {
				// not found - recurse
				ns.addAll(0, n.children);
			}
		}
		return nodes;
	}
	/** return content within section */
	public String getSectionText(String headingNode, String headingName) {
		headingNode = headingNode.toLowerCase();
		headingName = headingName.toLowerCase().trim();
		int from = 0, to = 0;
		// depth-first traversal
		Vector<Node> ns = new Vector<Node>();
		ns.add(this.root);
		boolean found = false;
		while(ns.size()>0) {
			Node n = ns.get(0);
			ns.remove(0);
			if (n.name.toLowerCase().equals(headingNode)) {
				String body = content.substring(n.bodyFrom, n.bodyTo).trim().toLowerCase();
				if (body.startsWith(headingName)) {
					// found!
					found = true;
					from = n.to;
				}
				else {
					if (found)
						to = n.from;
					found = false;
				}
			}
			else if (found) {
				// no recurse
			} else {
				// not found - recurse
				ns.addAll(0, n.children);
			}
		}
		if (found) {
			to = root.bodyTo;
		}
		if (from==to)
			return null;
		return content.substring(from, to);
	}
	
	/** return list of Nodes of given name within node list */
	public Vector<Node> getElementNodes(Vector<Node> nodes1, String elementName) {
		elementName = elementName.toLowerCase().trim();
		Vector<Node> nodes = new Vector<Node>();
		if (nodes1==null)
			return nodes;
		// depth-first traversal
		Vector<Node> ns = new Vector<Node>();
		ns.addAll(nodes1);
		while(ns.size()>0) {
			Node n = ns.get(0);
			ns.remove(0);
			if (n.name.toLowerCase().equals(elementName)) {
				nodes.add(n);
			}
			// not found - recurse
			ns.addAll(0, n.children);
		}
		return nodes;
	}
	/** return list of Nodes of given name within node */
	public Vector<Node> getElementNodes(Node node1, String elementName) {
		Vector<Node> nodes1 = new Vector<Node>();
		nodes1.add(node1);
		return getElementNodes(nodes1, elementName);
	}
	/** root */
	public Node getRootNode() {
		return root;		
	}
	/** return first Node of given name within node */
	public Node getElementNode(Node node1, String elementName) {
		Vector<Node> nodes1 = new Vector<Node>();
		nodes1.add(node1);
		Vector<Node> nodes = getElementNodes(nodes1, elementName);
		if (nodes.size()>0)
			return nodes.get(0);
		return null;
	}
	/** return first Node of given name with body text starting with sample (case insensitive) within node */
	public Node getElementNodeStartingWith(Node node1, String elementName, String bodyTextStarts) {
		bodyTextStarts = bodyTextStarts.toLowerCase();
		Vector<Node> nodes1 = new Vector<Node>();
		nodes1.add(node1);
		Vector<Node> nodes = getElementNodes(nodes1, elementName);
		for (int i=0; i<nodes.size(); i++) {
			Node n = nodes.get(i);
			String text = n.getBodyText().toLowerCase();
			if (text.startsWith(bodyTextStarts))
				return n;
		}
		return null;
	}
	/** return first Node of given name with given attribute value within node */
	public Node getElementNode(Node node1, String elementName, String attributeName, String attributeValue) {
		Vector<Node> nodes1 = new Vector<Node>();
		nodes1.add(node1);
		Vector<Node> nodes = getElementNodes(nodes1, elementName);
		for (int i=0; i<nodes.size(); i++) {
			Node n = nodes.get(i);
			String value = n.getAttributeValue(attributeName);
			if (value!=null && value.equals(attributeValue))
				return n;
		}
		return null;
	}
	/** get simple content following node */
	public String getSimpleContentFollowing(Node node) {
		int ix = node.to;
		while(ix<content.length() && content.charAt(ix)!='<')
			ix++;
		return content.substring(node.to, ix);
	}
	/** text format */
	static final String[] formatElements = new String[] { "i", "br", "font" };
	/** get simple content following node, option to ignore break & other text formatting */
	public String getSimpleContentFollowing(Node node, boolean ignoreBreak) {
		if (!ignoreBreak)
			return getSimpleContentFollowing(node);
		StringBuffer b = new StringBuffer();
		
		int ix = node.to;
		while(ix<content.length()) {
			char c= content.charAt(ix);
			if (c=='<') {
				int ex = content.indexOf(">", ix);
				if (ex<0)
					// nope
					break;
				if (content.charAt(ix+1)=='/')
					ix++; // end tag - same rules
				String elname = content.substring(ix+1, ex);
				int sx = elname.indexOf(" ");
				if (sx>=0)
					elname = elname.substring(0,sx);
				elname = elname.toLowerCase();
				boolean ignore = false;
				for(int i=0; !ignore && i<formatElements.length; i++)
					if (elname.equals(formatElements[i]))
						ignore = true;
				if (ignore) {
					ix = ex+1;
					continue;
				}
				// not ignore
				break;
			}
			b.append(c);
			ix++;
		}
		return b.toString();
	}
	/** get content following to specified text (else simple) */
	public String getFollowingText(Node node, String endText[]) {
		int ix = node.to;
		int ix2 = content.indexOf(endText[0],ix);
		for (int i=1; i<endText.length; i++) {
			int ix3  = content.indexOf(endText[i], ix);
			if (ix3<0)
				continue;
			if (ix3<ix2)
				ix2 = ix3;
		}
		if (ix2<0) {
			logger.warn("Did not find end text following node "+node);
			return getSimpleContentFollowing(node).trim();
		}
		return removeComments(content.substring(ix,ix2)).trim();
	}
	/** get content following to specified text (else simple) */
	public String getFollowingText(Node node, String endText) {
		return getFollowingText(node, new String[] { endText });
	}
	/** remove xml/html comments */
	public static String removeComments(String s) {		
		if (!s.contains("<!--"))
			return s;
		StringBuffer b = new StringBuffer();
		int ix =0;
		while(ix<s.length()) {
			int ix2 = s.indexOf("<!--", ix);
			if (ix2<0) {
				b.append(s.substring(ix,s.length()));
				break;
			}
			b.append(s.substring(ix,ix2));
			ix = s.indexOf("-->", ix2);
			if (ix<0)
				break;
			ix += "-->".length();
		}
		return b.toString();
	}
}
