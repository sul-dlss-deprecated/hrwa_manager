package edu.columbia.ldpd.hrwa.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import edu.columbia.ldpd.hrwa.HrwaManager;

public class IndexArchiveFilesTask extends HrwaTask {
	
	private String archiveFileDirPath;

	public IndexArchiveFilesTask(String the_archiveFileDirPath) {
		this.archiveFileDirPath = the_archiveFileDirPath;
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Scan through warc file directory and generate an alphabetically-sorted list of all warc files to index
		String[] validFileExtensions = {"arc.gz", "warc.gz"};
		File[] archiveFilesToProcess = getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(this.archiveFileDirPath, validFileExtensions);
		
		if(archiveFilesToProcess.length < 1) {
			HrwaManager.writeToLog("No files found for indexing in directory: " + archiveFileDirPath, true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		
		HrwaManager.writeToLog("Number of archive files to process: " + archiveFilesToProcess.length , true, HrwaManager.LOG_TYPE_STANDARD);
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	////////////////////////////////
	/* Archive File List Creation */
	////////////////////////////////

	/**
	 * Recursively scans a directory, collecting Files that are matched by the passed fileExtensionFilter. Returns the resulting File[],
	 * with files alphabetically sorted by their full paths. 
	 * @param String dir The directory to recursively search through.
	 * @param String[] fileExtensionFilter File extensions to include in the search. All unspecified file extensions will be excluded.
	 * @return
	 */
	public static File[] getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(String pathToArchiveDirectory, String[] fileExtensionFilter) {

		File directory = new File(pathToArchiveDirectory);

		if (! directory.exists()) {
			HrwaManager.writeToLog("Error: achiveFileDir path " + directory.toString() + " does not exist.", true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		if (! directory.isDirectory()) {
			HrwaManager.writeToLog("Error: achiveFileDir path " + directory.toString() + " is not a directory.", true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		if (! directory.canRead()) {
			HrwaManager.writeToLog("Error: achiveFileDir path " + directory.toString() + " is not readable.", true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}

		// Get file iterator that will recurse subdirectories in
		// archiveDirectory, only grabbing files with valid archive file
		// extensions
		// Note: FileUtils.iterateFiles returns a basic Iterator, and NOT a
		// generic Iterator. That's unfortunate (because I like Generics), but
		// I'll have to work with it anyway.
		Iterator<File> fileIterator = FileUtils.iterateFiles(
			directory,
			fileExtensionFilter,
			true
		);

		//Generate an alphabetically-ordered list of all the files that we'll be using
		ArrayList<File> fileList = new ArrayList<File>();
		while(fileIterator.hasNext())
		{
			fileList.add(fileIterator.next());
		}

		sortFileList(fileList);

		return fileList.toArray(new File[fileList.size()]);
	}

	public static void sortFileList(ArrayList<File> fileListToSort) {

		Collections.sort(fileListToSort, new Comparator<File>(){
			
			public int compare(File f1, File f2)
		    {
		        return (f1.getPath()).compareTo(f2.getPath());
		    }

		});

	}

	
}
