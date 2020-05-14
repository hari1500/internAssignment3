package com.example.assignment3;

class Utils {
//    Big file
    static final String sourceUrl = "https://www.db-book.com/db7/slides-dir/PDF-dir/ch7.pdf";
//    Small file
//    static final String sourceUrl = "https://www.cse.iitb.ac.in/~ttc/endsemsp2019-20.pdf";
    static final String logTag = "Assignment3";
    static final Integer sleepTime = 1000;
    static final String downloadDirectoryPath = "test";
//    static final String downloadFileName = "outputFile.pdf";

    static class DownloadStatuses {
        static final int PENDING                        = -1;
        static final int ONGOING                        = 0;
        static final int COMPLETED                      = 1;
        static final int CONNECTION_FAILED              = 2;
        static final int SD_CARD_NOT_EXISTS             = 3;
        static final int OUTPUT_DIR_CREATION_FAILED     = 4;
        static final int OUTPUT_FILE_CREATION_FAILED    = 5;
        static final int FAILED                         = 6;
    }

    static class TextViewStrings {
        static final String REQUESTED                   = "Download Requested";
        static final String PENDING                     = "Download Pending...";
        static final String COMPLETED                   = "Download Completed...";
        static final String CONNECTION_FAILED           = "Connection Failed :(";
        static final String SD_CARD_NOT_EXISTS          = "SD Card does not exists :(";
        static final String OUTPUT_DIR_CREATION_FAILED  = "Output Folder Creation Failed :(";
        static final String OUTPUT_FILE_CREATION_FAILED = "Output File Creation Failed :(";
        static final String FAILED                      = "Download Failed :(";
    }
}
