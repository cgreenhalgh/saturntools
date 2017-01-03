/**
 * 
 */
package saturntools;

/**
 * @author cmg
 *
 */
public class DownloadSaturnModuleMaintCompare extends
		DownloadSaturnModuleMaintFiles {
	private static final String COMPARE_URL = BASE_URL+"/ModuleCatalogue/asp/CompareModule.asp?crs_id=${code}&year_id=${year}&asp_type_id=000009";
;

	public static void main(String args[]) {
		new DownloadSaturnModuleMaintCompare().download(args);
	}

	@Override
	protected void downloadModuleFile(String saturncode, String modulecode,
			String year, String cookie) {
		String urlString = COMPARE_URL.replace("${code}", saturncode).replace("${year}", year);
		System.out.println("Download "+saturncode+" "+modulecode+" from "+urlString);
		String filename = modulecode+"_"+year+".html";
		// DOESN't WORK: redirects to https://saturnweb.nottingham.ac.uk/nottingham/saturn/asp/murn/asp/main_frame.asp?left=expire
		downloadFile(urlString, null, filename, cookie);
	}
	
}
