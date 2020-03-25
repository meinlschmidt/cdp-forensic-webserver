package pseminar.cdp.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.FileSystemUtils;

import java.io.File;

@SpringBootApplication
public class WebserverApplication {

    //cdpServerMetaDirectory must be equal to the one in the CDP-Server-Config!
    static final String cdpServerMetaDirectory = "/var/cdpfgl/server/meta";

    static final String tmpDataDirectory = System.getProperty("user.home") + "/cdp-webserver-tmp/";
    static final String restoreDataDirectory = System.getProperty("user.home") + "/cdp-webserver-perm/restore/";
    static final String entropyDataDirectory = System.getProperty("user.home") + "/cdp-webserver-perm/entropy/";

    //set true for detailed Information on Console
    static final Boolean DEBUG_CDP_SERVER = false;
    static final Boolean DEBUG_ENTROPY = false;

    public static void main(String[] args) {

        SpringApplication.run(WebserverApplication.class, args);

        //delete tmpDataDirectory at Application-Start
        FileSystemUtils.deleteRecursively(new File(tmpDataDirectory));
        //create DataDirectories
        new File(tmpDataDirectory).mkdirs();
        new File(restoreDataDirectory).mkdirs();
        new File(entropyDataDirectory).mkdirs();
    }
}