/** Simple helper class to recursively return files in a directory with 
 *  a given extension.
 * 
 * @author Scott Sanner
 */

import java.io.File;
import java.util.ArrayList;

public class FileFinder {
	
	/** Main file finder
	 * 
	 * @param src source directory
	 * @param ext null if any extension OK
	 * @param recurse recurse on subdirectories
	 * @return
	 */
	public static ArrayList<File> GetAllFiles(String src, String ext, boolean recurse) {
		
		ArrayList<File> ret_files = new ArrayList<File>();
		File[] files = new File(src).listFiles();

		for (File f : files) {			
			if (f.isDirectory()) {
				if (recurse)
					ret_files.addAll(GetAllFiles(f.getPath(), ext, recurse));
			} else {
				if (ext == null || f.toString().endsWith(ext))
					ret_files.add(f);
			}
		}
		
		return ret_files;
	}
}
