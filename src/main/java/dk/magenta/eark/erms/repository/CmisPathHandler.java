package dk.magenta.eark.erms.repository;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CmisPathHandler {

	private Path topLevelCmisPath;
	
	/**
	 * 
	 * @param topLevelCmisPath to top-level path from which the relative paths are to be determined
	 */
	public CmisPathHandler(String topLevelCmisPath) {
		this.topLevelCmisPath = Paths.get(topLevelCmisPath);
	}
	
	/**
	 * Gets the path of the given cmis folder path relative to the top-level CMIS path
	 * @param cmisFolderPath
	 * @return the relative path
	 */
	public Path getRelativePath(String cmisFolderPath) {
		Path path = Paths.get(cmisFolderPath);
		Path relativePath = path.relativize(topLevelCmisPath);
		// System.out.println(x);
		return relativePath;
	}
}
