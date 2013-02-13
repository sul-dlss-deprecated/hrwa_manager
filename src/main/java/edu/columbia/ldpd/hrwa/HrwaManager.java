package edu.columbia.ldpd.hrwa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.columbia.ldpd.hrwa.tasks.DownloadArchiveFilesFromArchivitTask;
import edu.columbia.ldpd.hrwa.tasks.HrwaTask;
import edu.columbia.ldpd.hrwa.tasks.IndexFSFDataToSolrAndMySQLTask;

public class HrwaManager {
	
	private static String pathToThisRunningJarFile = null;

	public static final int 	EXIT_CODE_SUCCESS 	= 1;
    public static final int 	EXIT_CODE_ERROR 	= 1;
	public static final String 	applicationName 	= "hrwa_indexer";

	private static Options    	options;
    private static CommandLine	cmdLine;
    
    private static long appStartTime = System.currentTimeMillis();

    // Command line options
    public static boolean    	verbose					= false;
    public static boolean    	previewMode				= false;
    public static String    	logDirPath				= "." + File.separatorChar + "logs";
    public static String    	tmpDirPath				= "." + File.separatorChar + "tmp";
    public static String		logFilePrefix 			= new SimpleDateFormat("yy-MM-dd-HHmmss").format(new Date()); //default value, can be overridden by command line args
    public static String 		blobDirPath 			= "." + File.separatorChar + "blobs";
	public static String		archiveFileDirPath		= "." + File.separatorChar + "sample_data"; //default, should be overridden
	public static String		archiveItUsername	= ""; //default, should be overridden
	public static String		archiveItPassword	= ""; //default, should be overridden
	
	//Task stuff
	public static boolean runDownloadArchiveFilesTask	= false;
	public static boolean runIndexFSFDataTask			= false;
	
	// Log stuff
	private static BufferedWriter mysqlStandardLogWriter;
	private static BufferedWriter mysqlErrorLogWriter;
	private static BufferedWriter mysqlNoticeLogWriter;
	private static BufferedWriter mysqlMemoryLogWriter;
	public static final int LOG_TYPE_STANDARD = 0;
	public static final int LOG_TYPE_ERROR = 1;
	public static final int LOG_TYPE_NOTICE = 2;
	public static final int LOG_TYPE_MEMORY = 3;
	
	// Memory stuff
	public static long maxAvailableMemoryInBytes = Runtime.getRuntime().maxMemory();
	public static long maxAvailableProcessors = Runtime.getRuntime().availableProcessors();
	
	private static ArrayList<HrwaTask> tasksToRun = new ArrayList<HrwaTask>(); 

	public static void main(String[] args) {
		try {
			HrwaManager.pathToThisRunningJarFile = HrwaManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		System.out.println("Application " + applicationName + ": start!");
		System.out.println("Path to jar file: " + HrwaManager.pathToThisRunningJarFile);
		
		parseCommandLineOptions(args);
		
		//Create required directories if they don't already exist
		(new File(HrwaManager.logDirPath)).mkdirs();
		(new File(HrwaManager.tmpDirPath)).mkdirs();
		(new File(HrwaManager.blobDirPath)).mkdirs();
		(new File(HrwaManager.archiveFileDirPath)).mkdirs();
		
		//Create log file writers
		HrwaManager.openLogFileWriters();
		HrwaManager.writeToLog("Application " + applicationName + " is running...", true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Started on: " + (new SimpleDateFormat().format(new Date())), true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Print out max memory allocated to this process
		
		HrwaManager.writeToLog("Max Available Memory: " + (maxAvailableMemoryInBytes / (1024*1024)) + " MB", true, LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("maxAvailableProcessors: " + maxAvailableProcessors, true, LOG_TYPE_STANDARD);
		
		//Add preview mode notation to log if in previewMode
		if(previewMode) {
			
			HrwaManager.writeToLog(
				"**************************************\n" +
				"* Running in preview mode!           *\n" +
				"* No permanent changes will be made! *\n" +
				"**************************************",
				true,
				LOG_TYPE_STANDARD
			);
		}
		
		//Determine which tasks to run
		if(runDownloadArchiveFilesTask) {
			tasksToRun.add(new DownloadArchiveFilesFromArchivitTask());
		}
		if(runIndexFSFDataTask) {
			tasksToRun.add(new IndexFSFDataToSolrAndMySQLTask());
		}
		//tasksToRun.add(new IndexArchiveFilesTask(archiveFileDirPath));
		
		//And run those tasks
		HrwaManager.writeToLog("Total number of tasks to run: " + tasksToRun.size(), true, LOG_TYPE_STANDARD);
		for(HrwaTask singleTask : tasksToRun) {
			singleTask.runTask();
		}

		HrwaManager.closeLogFileWriters();
		
		System.out.println(applicationName + ": Done!");
	}
	
	
	
	
	
	
	///////////////
	/* Log Stuff */
	///////////////
	
	public static void openLogFileWriters() {
		try {
			mysqlErrorLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "erorr-log.txt"));
			mysqlStandardLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "standard-log.txt"));
			mysqlNoticeLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "notice-log.txt"));
			mysqlMemoryLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "memory-log.txt"));
		} catch (IOException e) {
			System.out.println("Error: Could not create mysql indexer log file(s)");
			e.printStackTrace();
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
	}
	
	public static void closeLogFileWriters() {
		try {
			writeToLog("Closing memory log.", true, LOG_TYPE_MEMORY);
			mysqlMemoryLogWriter.close();
			writeToLog("Closing notice log.", true, LOG_TYPE_NOTICE);
			mysqlNoticeLogWriter.close();
			writeToLog("Closing standard log.", true, LOG_TYPE_STANDARD);
			mysqlStandardLogWriter.close();
			writeToLog("Closing error log.", true, LOG_TYPE_ERROR);
			mysqlErrorLogWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeToLog(String stringToWrite, boolean printToConsole, int log_type)
	{
		if(printToConsole || verbose)
		{
			System.out.println(stringToWrite);
		}

		try {
			if(log_type == LOG_TYPE_ERROR) {
				mysqlErrorLogWriter.write(stringToWrite + "\n");
				mysqlErrorLogWriter.flush();
			}
			else if(log_type == LOG_TYPE_NOTICE) {
				mysqlNoticeLogWriter.write(stringToWrite + "\n");
				mysqlNoticeLogWriter.flush();
			}
			else if(log_type == LOG_TYPE_MEMORY) {
				mysqlMemoryLogWriter.write(stringToWrite + "\n");
				mysqlMemoryLogWriter.flush();
			}
			else {
				mysqlStandardLogWriter.write(stringToWrite + "\n");
				mysqlStandardLogWriter.flush();
			}
		} catch (IOException e) {
			
			if(log_type != LOG_TYPE_ERROR) {
				writeToLog("An error occurred while attempting to write to a log file. Log type: " + log_type, true, LOG_TYPE_ERROR);
				writeToLog(e.getMessage(), true, LOG_TYPE_ERROR);
			} else {
				System.err.println("An error occurred while attempting to write to the error log.");
				e.printStackTrace();
			}
		}
	}
	
	
	////////////////////////////////
	/* Command Line Parsing Stuff */
	////////////////////////////////

	private static void parseCommandLineOptions( String[] args ) {
        addCommandLineOptions();

        CommandLineParser parser  = new GnuParser();

        try {
            cmdLine = parser.parse( options, args );
        } catch(ParseException e){
            System.err.println( "Command line parsing failed.  Reason:" + e.getMessage() );
        }


        // If the user isn't asking for usage help, validate the given command line options.
        if( ! cmdLine.hasOption( "help" ) )
        {
	        //Check For Invalid options/combinations
        	//Note: Nothing to check for right now.

	        //And then process the command line args
	        if ( cmdLine.hasOption( "verbose" ) ) {
	        	verbose  = true;
	        	System.out.println("Note: Running in verbose mode.");
	        }
	        
	        if ( cmdLine.hasOption( "preview" ) ) {
	        	previewMode  = true;
	        	System.out.println(	"Running in PREVIEW mode.");
	        }
	        
	        if ( cmdLine.hasOption( "archivefiledir") ) {
	        	archiveFileDirPath = cmdLine.getOptionValue( "archivefiledir" );
	        	System.out.println("Archive File Directory: " + archiveFileDirPath);
	        }

	        if ( cmdLine.hasOption( "logfileprefix") ) {
	        	logFilePrefix = cmdLine.getOptionValue( "logfileprefix" );
	        	System.out.println("Log File Prefix: " + logFilePrefix);
	        }

	        if ( cmdLine.hasOption( "blobdir") ) {
	        	blobDirPath = cmdLine.getOptionValue( "blobdir" );
	        	System.out.println("Out Directory For Blob Files: " + blobDirPath);
	        }
	        
	        if ( cmdLine.hasOption( "logdir") ) {
	        	logDirPath = cmdLine.getOptionValue( "logdir" );
	        	System.out.println("Log Directory Path Set Manually: " + logDirPath);
	        }
	        
	        if ( cmdLine.hasOption( "tmpdir") ) {
	        	tmpDirPath = cmdLine.getOptionValue( "tmpdir" );
	        	System.out.println("Tmp Directory Path Set Manually: " + tmpDirPath);
	        }
	        
	        if ( cmdLine.hasOption( "archiveitusername") ) {
	        	archiveItUsername = cmdLine.getOptionValue( "archiveitusername" );
	        	System.out.println("An archive-it username was supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "archiveitpassword") ) {
	        	archiveItPassword = cmdLine.getOptionValue( "archiveitpassword" );
	        	System.out.println("An archive-it password was supplied.");
	        }
	        
	        //Task 1: downloadarchivefiles
	        if ( cmdLine.hasOption( "downloadarchivefiles") ) {
	        	HrwaManager.runDownloadArchiveFilesTask = true;
	        	System.out.println("* Will run DownloadArchiveFilesFromArchivitTask.");
	        }
	        
	        //Task 2: indexfsfdata
	        if ( cmdLine.hasOption( "indexfsfdata") ) {
	        	HrwaManager.runIndexFSFDataTask = true;
	        	System.out.println("* Will run IndexFSFDataToSolrAndMySQLTask.");
	        }
        }
        else
        {
            usage( options );
            System.exit( EXIT_CODE_SUCCESS );
        }

    }

	private static void usage( Options options ) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp( applicationName, options );
    }

	@SuppressWarnings("static-access")
	// It's okay to suppress the static-access warnings for this method because
	// the Apache CLI documentation actually recommends the use of OptionBuilder
	// that I have below.
	private static void addCommandLineOptions() {
        options = new Options();

        options.addOption( "help",			false, "Display help"     );
        options.addOption( "verbose",		false, "More verbose console output." );
        options.addOption( "preview",		false, "Run all operations in preview mode (no real changes will be made)." );
        options.addOption( "downloadarchivefiles",	false, "Run DownloadArchiveFilesFromArchivitTask" );
        options.addOption( "indexfsfdata",			false, "Run IndexFSFDataToSolrAndMySQLTask" );
        
        options.addOption(
        		OptionBuilder.withArgName( "directory" )
                .hasArg()
                .withDescription( "Location of log directory. Default to ./logs (relative to the " )
                .create( "logdir" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "directory" )
                .hasArg()
                .withDescription( "Location of temporary working directory." )
                .create( "tmpdir" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "Prefix to be prepended to log files in the form: outputfileprefix-standar.log, outputfileprefix-error.log, etc." )
                .create( "logfileprefix" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "directory" )
                .hasArg()
                .withDescription( "Output directory where processed archive record blobs will go. The specified directory will be created if it does not already exist." )
                .create( "blobdir" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "directory" )
                .hasArg()
                .withDescription( "Root directory of archive files (arc.gz/warc.gz) to be indexed (recurses through subdirectories)." )
                .create( "archivefiledir" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "Username required for logging into the Archive-It website and downloading archive files." )
                .create( "archiveitusername" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "Password required for logging into the Archive-It website and downloading archive files." )
                .create( "archiveitpassword" )
        );
        
    }
	
	public static String getCurrentAppRunTime() {
		return "Current run time: " + TimeStringFormat.getTimeString((System.currentTimeMillis() - HrwaManager.appStartTime)/1000);
	}

}
