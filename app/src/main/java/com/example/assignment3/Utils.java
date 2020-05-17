package com.example.assignment3;

class Utils {
//    Big file
    static final String sourceUrl = "https://www.db-book.com/db7/slides-dir/PDF-dir/ch7.pdf";
//    Small file
//    static final String sourceUrl = "https://www.cse.iitb.ac.in/~ttc/endsemsp2019-20.pdf";
    static final String logTag = "Assignment3";
    static final String downloadDirectoryPath = "Assignment3";

    static class IntentAndBundleKeys {
        static final String sourceUrlKey        = "SOURCE_URL";
        static final String resultReceiverKey   = "RESULT_RECEIVER";
        static final String downloadStatusKey   = "DOWNLOAD_STATUS";
        static final String progressPercentKey   = "PROGRESS_PERCENT";
    }

    static class DownloadStatuses {
        static final int PENDING                        = -1;
        static final int ONGOING                        = 0;
        static final int COMPLETED                      = 1;
        static final int CONNECTION_FAILED              = 2;
        static final int SD_CARD_NOT_EXISTS             = 3;
        static final int OUTPUT_DIR_CREATION_FAILED     = 4;
        static final int OUTPUT_FILE_CREATION_FAILED    = 5;
        static final int IMPROPER_URL                   = 6;
        static final int FAILED                         = 7;
    }

    static class TextViewMessages {
        static final String REQUESTED                   = "Download Requested...";
        static final String PENDING                     = "Download Pending...";
        static final String COMPLETED                   = "Download Completed\n\nFile stored in ";
        static final String CONNECTION_FAILED           = "Connection Failed :(";
        static final String SD_CARD_NOT_EXISTS          = "SD Card does not exists :(";
        static final String OUTPUT_DIR_CREATION_FAILED  = "Output Folder Creation Failed :(";
        static final String OUTPUT_FILE_CREATION_FAILED = "Output File Creation Failed :(";
        static final String IMPROPER_URL                = "Please check url again :(";
        static final String FAILED                      = "Download Failed :(\n\nPlease check :\n\t1.Internet Connectivity \n\t2.Storage Permissions";
        static final String PROVIDE_STORAGE_ACCESS      = "Please provide Storage Access :(";
    }
}
