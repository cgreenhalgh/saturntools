package saturntools;

import java.io.File;

public class FileUtils {
	private static File outputDirectory;
	
	public static File getOutputDirectory() {
		if (outputDirectory!=null)
			return outputDirectory;
		return new File(".");
	}
	public static void setOutputDirectory(File f) {
		outputDirectory = f;
	}
}
