package xyz.becvar.websiteinspector;

import xyz.becvar.websiteinspector.utils.NetworkUtils;
import xyz.becvar.websiteinspector.utils.console.Logger;

/**
 * Class Validator
 * 
 * @package xyz.becvar.websiteinspector
 */
public class Validator
{
    /**
     * Validate the url
     * 
     * @param url to validate
     * 
     * @return string https url
     */
    public static String validateUrl(String url)
    {
        String httpsUrl;
        String httpUrl;

        if (url == null || url.trim().isEmpty()) {
            Logger.errorLog("URL is null or empty.");
        }

        // remove last slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // construct https and http URLs
        if (url.startsWith("https://")) {
            httpsUrl = url;
            httpUrl = "http://" + url.substring(8);
        } else if (url.startsWith("http://")) {
            httpsUrl = "https://" + url.substring(7);
            httpUrl = url;
        } else {
            httpsUrl = "https://" + url;
            httpUrl = "http://" + url;
        }

        // check if HTTPS is available
        if (NetworkUtils.checkIsWebsiteAvailable(httpsUrl)) {
            return httpsUrl;
        }

        // check if HTTP is available
        if (NetworkUtils.checkIsWebsiteAvailable(httpUrl)) {
            return httpUrl;
        } else {
            Logger.errorLog("Website is not available.");
            return null;
        }
    }
}
