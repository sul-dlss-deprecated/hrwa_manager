package edu.columbia.ldpd.hrwa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.columbia.ldpd.hrwa.tasks.ArchiveFileReadTestTask;
import edu.columbia.ldpd.hrwa.tasks.ArchiveToMySQLTask;
import edu.columbia.ldpd.hrwa.tasks.DownloadArchiveFilesFromArchivitTask;
import edu.columbia.ldpd.hrwa.tasks.HrwaTask;
import edu.columbia.ldpd.hrwa.tasks.MySQLArchiveRecordsToSolrTask;
import edu.columbia.ldpd.hrwa.tasks.RegularMaintenanceTask;
import edu.columbia.ldpd.hrwa.tasks.SitesToSolrAndMySQLTask;
import edu.columbia.ldpd.hrwa.tasks.TalkToClioTestTask;
import edu.columbia.ldpd.hrwa.util.common.MetadataUtils;

public class HrwaManager {
	
	private static String pathToThisRunningJarFile = null;

	public static final int 	EXIT_CODE_SUCCESS 	= 1;
    public static final int 	EXIT_CODE_ERROR 	= 1;
	public static final String 	applicationName 	= "hrwa_indexer";
	
	public static final String multiValuedFieldMySQLSeparatorPrefixChar = "|";

	private static Options    	options;
    private static CommandLine	cmdLine;
    
    private static long appStartTime = System.currentTimeMillis();
    
    // Memory stuff
 	public static long maxAvailableMemoryInBytes = Runtime.getRuntime().maxMemory();
 	
 	//CPU Stuff
 	private static int maxAvailableProcessors = Runtime.getRuntime().availableProcessors();

    // Command line options
    public static boolean    	verbose					= false;
    public static boolean    	previewMode				= false;
    public static String    	logDirPath				= "." + File.separatorChar + "logs";
    public static String    	tmpDirPath				= "." + File.separatorChar + "tmp";
    public static String		logFilePrefix 			= new SimpleDateFormat("yy-MM-dd-HHmmss").format(new Date()); //default value, can be overridden by command line args
    public static String 		blobDirPath 			= "." + File.separatorChar + "blobs";
	public static String		archiveFileDirPath		= "." + File.separatorChar + "sample_data"; //default, should be overridden
	public static String		archiveItUsername		= ""; //default, should be overridden
	public static String		archiveItPassword		= ""; //default, should be overridden
	public static int			archiveItCollectionId 	= -1; //default, should be overridden
	public static String		mysqlUrl			= ""; //default, should be overridden
	public static String		mysqlDatabase		= ""; //default, should be overridden
	public static String		mysqlUsername		= ""; //default, should be overridden
	public static String		mysqlPassword		= ""; //default, should be overridden
	public static String		pathToRelatedHostsFile = ""; //default, should be overridden
	
	public static final String		MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME			= "web_archive_records";
	public static final String		MYSQL_MIMETYPE_CODES_TABLE_NAME					= "mimetype_codes";
	public static final String		MYSQL_SITES_TABLE_NAME							= "sites";
	public static final String		MYSQL_RELATED_HOSTS_TABLE_NAME					= "related_hosts";
	public static final String		MYSQL_FULLY_INDEXED_ARCHIVE_FILES_TABLE_NAME 	= "fully_indexed_archive_files";
	
	public static int				mysqlCommitBatchSize							= 1000; //default, can be overridden
	public static int				mySQLToSolrRowRetrievalSize						= 1000; //default, can be overridden
	
	public static int				regularMaintenanceMySQLRowRetrievalSize			= 1000; //default, can be overridden
	
	public static String			asfSolrUrl										= ""; //default, should be overridden
	
	public static int maxUsableProcessors = HrwaManager.maxAvailableProcessors - 1; //by default, might be overridden
	public static long maxMemoryThresholdInBytesForStartingNewThreadProcesses = (int)(maxAvailableMemoryInBytes*.75); //default, might be overridden
	
	//Shared constants
	public static final Pattern ARCHIVE_FILE_DATE_PATTERN = Pattern.compile(".+-(\\d{4})(\\d{2})\\d{2}\\d{2}\\d{2}\\d{2}-.+"); //Sample: //ARCHIVEIT-1716-SEMIANNUAL-XOYSOA-20121117062101-00002-wbgrp-crawl058.us.archive.org-6680.warc
	public static final String DESIRED_SOLR_INDEXED_MIMETYPE_CODES_STRING_FOR_MYSQL_WHERE_CLAUSE = "('DOCUMENT', 'HTML', 'PDF', 'SLIDESHOW', 'SPREADSHEET', 'XML')";
	
	//Task stuff
	private static boolean runDownloadArchiveFilesTask		= false;
	private static boolean runSitesToSolrAndMySQLTask		= false;
	private static boolean runArchiveToMySQLTask			= false;
	private static boolean runMySQLArchiveRecordsToSolrTask = false;
	private static boolean runRegularMaintenanceTask		= false;
	
	private static boolean runTalkToClioTestTask			= false;
	private static boolean runArchiveFileReadTestTask		= false;
	
	// Log stuff
	private static BufferedWriter mysqlStandardLogWriter;
	private static BufferedWriter mysqlErrorLogWriter;
	private static BufferedWriter mysqlNoticeLogWriter;
	private static BufferedWriter mysqlMemoryLogWriter;
	public static final int LOG_TYPE_STANDARD = 0;
	public static final int LOG_TYPE_ERROR = 1;
	public static final int LOG_TYPE_NOTICE = 2;
	public static final int LOG_TYPE_MEMORY = 3;
	
	private static ArrayList<HrwaTask> tasksToRun = new ArrayList<HrwaTask>(); 

	public static void main(String[] args) {
		try {
			HrwaManager.pathToThisRunningJarFile = HrwaManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
		System.out.println("Total memory: " + Runtime.getRuntime().totalMemory());
		System.out.println("Max memory: " + Runtime.getRuntime().maxMemory());
		
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
		
		HrwaManager.writeToLog("Max Available Memory: " + bytesToMegabytes(maxAvailableMemoryInBytes) + " MB", true, LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Max Available Processors: " + maxAvailableProcessors, true, LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Max USABLE Processors (based on default value or user preferences): " + maxUsableProcessors, true, LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Max memory threshhold for starting new thread processes: " + HrwaManager.bytesToMegabytes(maxMemoryThresholdInBytesForStartingNewThreadProcesses) + " MB)", true, LOG_TYPE_STANDARD);
		
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
		
		//Test tasks
		if(runTalkToClioTestTask) {
			tasksToRun.add(new TalkToClioTestTask());
		}
		if(runArchiveFileReadTestTask) {
			tasksToRun.add(new ArchiveFileReadTestTask());
		}
		
		//Real tasks
		if(runDownloadArchiveFilesTask) {
			tasksToRun.add(new DownloadArchiveFilesFromArchivitTask());
		}
		if(runSitesToSolrAndMySQLTask) {
			tasksToRun.add(new SitesToSolrAndMySQLTask());
		}
		if(runArchiveToMySQLTask) {
			tasksToRun.add(new ArchiveToMySQLTask());
		}
		if(runMySQLArchiveRecordsToSolrTask) {
			tasksToRun.add(new MySQLArchiveRecordsToSolrTask());
		}
		if(runRegularMaintenanceTask) {
			tasksToRun.add(new RegularMaintenanceTask());
		}
		
		//And run those tasks
		HrwaManager.writeToLog("Total number of tasks to run: " + tasksToRun.size(), true, LOG_TYPE_STANDARD);
		for(HrwaTask singleTask : tasksToRun) {
			singleTask.runTask();
		}

		HrwaManager.writeToLog(
				"---------------------------------------------\n" +
				"HRWAManager run complete!\n" + HrwaManager.getCurrentAppRunTime() + "\n" +
				"---------------------------------------------\n",
				true,
				HrwaManager.LOG_TYPE_STANDARD);
		
		HrwaManager.closeLogFileWriters();
		
		System.out.println(applicationName + ": Done!");
	}
	
	
	
	
	
	
	///////////////
	/* Log Stuff */
	///////////////
	
	public static void openLogFileWriters() {
		try {
			mysqlErrorLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "error-log.txt"));
			writeToLog("Error log is open.", true, LOG_TYPE_ERROR);
			mysqlStandardLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "standard-log.txt"));
			writeToLog("Standard log is open.", true, LOG_TYPE_STANDARD);
			mysqlNoticeLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "notice-log.txt"));
			writeToLog("Notice log is open.", true, LOG_TYPE_NOTICE);
			mysqlMemoryLogWriter = new BufferedWriter(new FileWriter(HrwaManager.logDirPath + File.separatorChar + HrwaManager.logFilePrefix + "-" + "memory-log.txt"));
			writeToLog("Memory log is open.", true, LOG_TYPE_MEMORY);
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
				synchronized(mysqlErrorLogWriter) {
					mysqlErrorLogWriter.write(stringToWrite + "\n");
					mysqlErrorLogWriter.flush();
				}
			}
			else if(log_type == LOG_TYPE_NOTICE) {
				synchronized(mysqlNoticeLogWriter) {
					mysqlNoticeLogWriter.write(stringToWrite + "\n");
					mysqlNoticeLogWriter.flush();
				}
			}
			else if(log_type == LOG_TYPE_MEMORY) {
				synchronized(mysqlMemoryLogWriter) {
					mysqlMemoryLogWriter.write(stringToWrite + "\n");
					mysqlMemoryLogWriter.flush();
				}
			}
			else {
				synchronized(mysqlStandardLogWriter) {
					mysqlStandardLogWriter.write(stringToWrite + "\n");
					mysqlStandardLogWriter.flush();
				}
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
	        
	        if ( cmdLine.hasOption( "relatedhostsfile") ) {
	        	pathToRelatedHostsFile = cmdLine.getOptionValue( "relatedhostsfile" );
	        	System.out.println("Related hosts file location: " + pathToRelatedHostsFile);
	        } else {
	        	HrwaManager.writeToLog("Error: A related hosts file is required (command line option --relatedhostsfile).", true, HrwaManager.LOG_TYPE_ERROR);
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
	        	System.out.println("An archive-it username has been supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "archiveitpassword") ) {
	        	archiveItPassword = cmdLine.getOptionValue( "archiveitpassword" );
	        	System.out.println("An archive-it password has been supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "archiveitcollectionid") ) {
	        	archiveItCollectionId = Integer.parseInt(cmdLine.getOptionValue( "archiveitcollectionid" ));
	        	System.out.println("An archive-it collection id was supplied.");
	        	
	        	if(HrwaManager.archiveItCollectionId != 1068 && HrwaManager.archiveItCollectionId != 1716) {
	        		System.out.println("Error: Invalid Archive-It collection specified (or none supplied). Collection ID: " + HrwaManager.archiveItCollectionId);
	        		System.out.println(	"Valid options:\n" +
	    								"-- 1068: Real HRWA archive file set\n" +
	    								"-- 1716: Small non-HRWA set for testing");
	        		
	    			System.exit(HrwaManager.EXIT_CODE_ERROR);
	    		}
	        	
	        }
	        
	        if ( cmdLine.hasOption( "maxusableprocessors") ) {
	        	if( Integer.parseInt(cmdLine.getOptionValue( "maxusableprocessors" )) > HrwaManager.maxAvailableProcessors ) {
	        		System.err.println("Error: Supplied command line value for maxusableprocessors (" + Integer.parseInt(cmdLine.getOptionValue( "maxusableprocessors" )) + ") is greater than the number of available processors on this machine (" + HrwaManager.maxAvailableProcessors + ")");
	        		System.exit(EXIT_CODE_ERROR);
	        	} else {
	        		maxUsableProcessors = Integer.parseInt(cmdLine.getOptionValue( "maxusableprocessors" ));
	        		System.out.println("The maximum number of usable processors has been set to: " + HrwaManager.maxUsableProcessors);
	        	}
	        }
	        
	        if ( cmdLine.hasOption( "maxmemorythresholdpercentageforstartingnewthreadprocesses") ) {
        		maxMemoryThresholdInBytesForStartingNewThreadProcesses = (long)(HrwaManager.maxAvailableMemoryInBytes*(Double.parseDouble("." + cmdLine.getOptionValue( "maxmemorythresholdpercentageforstartingnewthreadprocesses" ))));
        		System.out.println("The maximum memory threshold for starting new thread processes has been set to: " + HrwaManager.bytesToMegabytes(maxMemoryThresholdInBytesForStartingNewThreadProcesses) + " MB");
	        }
	        
	        if ( cmdLine.hasOption( "mysqlurl") ) {
	        	mysqlUrl = cmdLine.getOptionValue( "mysqlurl" );
	        	System.out.println("A MySQL URL has been supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "mysqldatabase") ) {
	        	mysqlDatabase = cmdLine.getOptionValue( "mysqldatabase" );
	        	System.out.println("A MySQL database has been supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "mysqlusername") ) {
	        	mysqlUsername = cmdLine.getOptionValue( "mysqlusername" );
	        	System.out.println("A MySQL username has been supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "mysqlpassword") ) {
	        	mysqlPassword = cmdLine.getOptionValue( "mysqlpassword" );
	        	System.out.println("A MySQL password has been supplied.");
	        }
	        
	        if ( cmdLine.hasOption( "mysqlcommitbatchsize") ) {
	        	mysqlCommitBatchSize = Integer.parseInt(cmdLine.getOptionValue( "mysqlcommitbatchsize" ));
	        	System.out.println("A MySQL commit batch size has been supplied.");
	        	
	        	if(HrwaManager.mysqlCommitBatchSize < 1) {
	    			System.out.println("Error: The --mysqlcommitbatchsize must be > 1. Please change the command line argument value that you supplied.");
	    			System.exit(HrwaManager.EXIT_CODE_ERROR);
	    		}
	        }
	        
	        if ( cmdLine.hasOption( "mysqltosolrrowretrievalsize") ) {
	        	mySQLToSolrRowRetrievalSize = Integer.parseInt(cmdLine.getOptionValue( "mysqltosolrrowretrievalsize" ));
	        	System.out.println("A MySQL to Solr row retrieval size has been supplied.");
	        	
	        	if(HrwaManager.mySQLToSolrRowRetrievalSize < 1) {
	    			System.out.println("Error: The --mySQLToSolrRowRetrievalSize must be > 1. Please change the command line argument value that you supplied.");
	    			System.exit(HrwaManager.EXIT_CODE_ERROR);
	    		}
	        }
	        
	        if ( cmdLine.hasOption( "regularmaintenancemysqlrowretrievalsize") ) {
	        	regularMaintenanceMySQLRowRetrievalSize = Integer.parseInt(cmdLine.getOptionValue( "regularmaintenancemysqlrowretrievalsize" ));
	        	System.out.println("A regular maintenance MySQL row retrieval size has been supplied.");
	        	
	        	if(HrwaManager.regularMaintenanceMySQLRowRetrievalSize < 1) {
	    			System.out.println("Error: The --regularmaintenancemysqlrowretrievalsize must be > 1. Please change the command line argument value that you supplied.");
	    			System.exit(HrwaManager.EXIT_CODE_ERROR);
	    		}
	        }
	        
	        if ( cmdLine.hasOption( "asfsolrurl") ) {
	        	asfSolrUrl = cmdLine.getOptionValue( "asfsolrurl" );
	        	System.out.println("An ASF Solr URL has been supplied.");
	        }
	        
	        //Test Task
	        if( cmdLine.hasOption( "talktocliotest") ) {
	        	HrwaManager.runTalkToClioTestTask = true;
	        	System.out.println("* Will run TalkToClioTestTask.");
	        }
	        
	        //Test Task
	        if( cmdLine.hasOption( "archivefilereadtest") ) {
	        	HrwaManager.runArchiveFileReadTestTask = true;
	        	System.out.println("* Will run ArchiveFileReadTestTask.");
	        }
	        
	        //Task 1: downloadarchivefiles
	        if ( cmdLine.hasOption( "downloadarchivefiles") ) {
	        	HrwaManager.runDownloadArchiveFilesTask = true;
	        	System.out.println("* Will run DownloadArchiveFilesFromArchivitTask.");
	        }
	        
	        //Task 2: sitestomysqlandsolr
	        if ( cmdLine.hasOption( "sitestosolrandmysql") ) {
	        	HrwaManager.runSitesToSolrAndMySQLTask = true;
	        	System.out.println("* Will run SutesToSolrAndMySQLTask.");
	        }
	        
	        //Task 3: archivetomysql
	        if ( cmdLine.hasOption( "archivetomysql") ) {
	        	HrwaManager.runArchiveToMySQLTask = true;
	        	System.out.println("* Will run ArchiveToMySQLTask.");
	        }
	        
	        //Task 4: mysqlarchiverecordstosolr
	        if ( cmdLine.hasOption( "mysqlarchiverecordstosolr") ) {
	        	HrwaManager.runMySQLArchiveRecordsToSolrTask = true;
	        	System.out.println("* Will run MySQLArchiveRecordsToSolrTask.");
	        }
	        
	        //Task 5: regularmaintenance
	        if ( cmdLine.hasOption( "regularmaintenance") ) {
	        	HrwaManager.runRegularMaintenanceTask = true;
	        	System.out.println("* Will run RegularMaintenanceTask.");
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
        options.addOption( "sitestosolrandmysql",		false, "Run SitesToSolrAndMySQLTask" );
        options.addOption( "archivetomysql",			false, "Run ArchiveToMySQLTask" );
        options.addOption( "talktocliotest",			false, "Run TalkToClioTestTask" );
        options.addOption( "archivefilereadtest",		false, "Run ArchiveFileReadTestTask" );
        options.addOption( "mysqlarchiverecordstosolr",	false, "Run MySQLArchiveRecordsToSolrTask" );
        options.addOption( "regularmaintenance",	false, "Run RegularMaintenanceTask" );
        
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
        		OptionBuilder.withArgName( "file" )
                .hasArg()
                .withDescription( "File that contains related hosts into for the related hosts table." )
                .create( "relatedhostsfile" )
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
        
        options.addOption(
        		OptionBuilder.withArgName( "integer" )
                .hasArg()
                .withDescription( "Numeric ID of the Archive-It collection that we want to download from." )
                .create( "archiveitcollectionid" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "integer" )
                .hasArg()
                .withDescription( "The maximum number of processors that should be used by this program. Defaults to (number of processors - 1). Note: Supplied value muse be <= the number of cores available on the machine." )
                .create( "maxusableprocessors" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "integer" )
                .hasArg()
                .withDescription( "The maximum memory threshold percentage for starting new thread processes. Defaults to 75% of the RAM allocated to this java process. Note: Supplied value should be between 50 (%) and 100 (%)." )
                .create( "maxmemorythresholdpercentageforstartingnewthreadprocesses" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "MySQL URL to connect to." )
                .create( "mysqlurl" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "MySQL database to connect to." )
                .create( "mysqldatabase" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "MySQL username." )
                .create( "mysqlusername" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "MySQL password." )
                .create( "mysqlpassword" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "integer" )
                .hasArg()
                .withDescription( "MySQL commit batch size (e.g. commit records in batches of 1000)." )
                .create( "mysqlcommitbatchsize" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "integer" )
                .hasArg()
                .withDescription( "MySQL to solr row retrieval size (e.g. When indexing to Solr, select records from MySQL in groups of 1000)." )
                .create( "mysqltosolrrowretrievalsize" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "integer" )
                .hasArg()
                .withDescription( "Regular maintenance MySQL row retrieval size (e.g. When performing regular HRWA app maintenance, select records from MySQL in groups of 1000)." )
                .create( "regularmaintenancemysqlrowretrievalsize" )
        );
        
        options.addOption(
        		OptionBuilder.withArgName( "string" )
                .hasArg()
                .withDescription( "ASF Solr URL to connect to." )
                .create( "asfsolrurl" )
        );
        
    }
	
	public static String getCurrentAppRunTime() {
		return "Current run time: " + TimeStringFormat.getTimeString((System.currentTimeMillis() - HrwaManager.appStartTime)/1000);
	}
	
	public static long getCurrentAppMemoryUsageInBytes() {
		return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
	}
	
	public static String getCurrentAppMemoryUsageString() {
		
		int bytesInAMegabyte = 1048576;
		
		return "Current memory usage: " + (getCurrentAppMemoryUsageInBytes()/bytesInAMegabyte) + "/" + (maxAvailableMemoryInBytes/bytesInAMegabyte) + " MB";
	}
	
	/**
	 * Returns a date string of the format "YYYY_MM" for the given archive file fileName.
	 * @return A string of the format "YYYY_MM", or null if the fileName cannot be parsed properly.
	 */
	public static String getCaptureYearAndMonthStringFromArchiveFileName(String fileName){
		String[] captureYearAndMonth = extractCaptureYearAndMonthStringsFromArchiveFileName(fileName);
		if(captureYearAndMonth == null) {
			return null;
		} else {
			return captureYearAndMonth[0] + "_" + captureYearAndMonth[1];
		}
	}
	
	/**
	 * Returns a String[] holding the capture year at index [0] and the capture month at index [1].
	 * @param fileName
	 * @return String[] holding the capture year and capture month. Returns null if the file name cannot be parsed properly.
	 */
	public static String[] extractCaptureYearAndMonthStringsFromArchiveFileName(String fileName){
		
		Matcher matcher = HrwaManager.ARCHIVE_FILE_DATE_PATTERN.matcher(fileName);
		
		if(matcher.matches()) {
			//Note matcher.group(0) returns the entire matched string 
			//matcher.group(1) returns the year
			//matcher.group(2) returns the month
			String[] arrToReturn = {matcher.group(1), matcher.group(2)}; 
			return arrToReturn;
		} else {
			return null;
		}
		
	}
	
	public static String getHoststringFromUrl(String url) {
		try {
			return MetadataUtils.parseHoststring(url);
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			HrwaManager.writeToLog("Unable to parse url: " + url, true, HrwaManager.LOG_TYPE_ERROR);
			return null;
		}
	}
	
	public static int bytesToMegabytes(long valueInBytes) {
		return (int)(valueInBytes/1048576L);
	}

}
