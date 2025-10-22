package xyz.becvar.websiteinspector.core;

/**
 * Class Config
 *
 * Main application config
 *
 * @package xyz.becvar.websiteinspector.core
 */
public class Config {

    // Application configuration
    public static final String APP_PREFIX = "WI";
    public static final String USER_AGENT = "website-inspector";

    // HTTP client configuration
    public static final int CONNECTION_TIMEOUT = 5;
    public static final int SCANNER_THREAD_POOL_SIZE = 30;

    // Catch all detector configuration
    public static final int RANDOM_TEST_COUNT = 20;
}
