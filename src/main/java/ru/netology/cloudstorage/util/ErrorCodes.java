package ru.netology.cloudstorage.util;

public class ErrorCodes {
    // 400 Bad Request
    public static final int BAD_CREDENTIALS = 1001;
    public static final int INVALID_INPUT_DATA = 1002;
    public static final int FILE_ALREADY_EXISTS = 1003;

    // 401 Unauthorized
    public static final int UNAUTHORIZED_ACCESS = 2001;

    // 500 Internal Server Error
    public static final int SERVER_ERROR = 3001;
    public static final int FILE_SYSTEM_ERROR = 3002;
}
