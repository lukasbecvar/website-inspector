package xyz.becvar.websiteinspector;

/**
 * This class provides help information
 */
public class HelpComponent {

    /**
     * Prints the help message
     */
    public static void printHelp() {
        System.out.println("Usage: java -jar WebsiteInspector.jar [options] <url>\n");
        System.out.println("Options:");
        System.out.println("  -h, --help                Display this help message.");
        System.out.println("  --output=<format>         Specify the output format (e.g., NORMAL, JSON).");
        System.out.println("  --subdomains-file=<file>  Specify a custom wordlist for subdomain scanning.");
        System.out.println("  --routes-file=<file>      Specify a custom wordlist for route scanning.");
        System.out.println("  --no-file-log             Disable logging to a file.");
        System.out.println("  --simple                  Disable subdomain and directory scanning.\n");
        System.out.println("Examples:");
        System.out.println("  Normal usage:");
        System.out.println("    java -jar WebsiteInspector.jar https://example.com --output=NORMAL\n");
        System.out.println("  JSON usage:");
        System.out.println("    java -jar WebsiteInspector.jar https://example.com --output=JSON");
    }
}
