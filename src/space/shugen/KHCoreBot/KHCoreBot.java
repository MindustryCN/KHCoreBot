package space.shugen.KHCoreBot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class KHCoreBot {
    private static final Logger logger = LogManager.getLogger(KHCoreBot.class);
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";

    public static final File prefsFile = new File("prefs.properties");
    public static Prefs prefs = new Prefs(prefsFile);
    public static ContentHandler contentHandler = new ContentHandler();
    public static Net net = new Net();
    public static KHBot khBot = new KHBot(prefs.get("KHToken", null));
    public static MessageHandler messageHandler = new MessageHandler();

    public static void main(String[] args) {
        logger.info("KHCoreBot Start");
        new KHCoreBot();
    }
}
