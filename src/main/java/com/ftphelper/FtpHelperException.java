package com.ftphelper;

/**
 * Unchecked wrapper for unexpected FTP/SFTP errors.
 */
public class FtpHelperException extends RuntimeException {

    public FtpHelperException(String message) {
        super(message);
    }

    public FtpHelperException(String message, Throwable cause) {
        super(message, cause);
    }
}
