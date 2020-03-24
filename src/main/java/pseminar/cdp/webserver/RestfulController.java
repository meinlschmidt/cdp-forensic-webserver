package pseminar.cdp.webserver;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static pseminar.cdp.webserver.Methods.*;
import static pseminar.cdp.webserver.WebserverApplication.*;

@SuppressWarnings("unchecked Call")
@CrossOrigin
@RestController
@RequestMapping
public class RestfulController {

    @GetMapping("/hostname")
    public JSONArray getHostnames() {

        JSONArray hostnameJson = new JSONArray();
        try (Stream<Path> walk = Files.walk(Paths.get(cdpServerMetaDirectory))) {

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.getFileName().toString()).collect(Collectors.toList());

            hostnameJson.addAll(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Data for following Hosts available: " + hostnameJson);
        return hostnameJson;
    }

    @GetMapping("/restoreList/{hostname}")
    public JSONArray getRestoreList(@PathVariable String hostname, @RequestParam(required = false, defaultValue = "/") String regex, @RequestParam(required = false) Long startDate, @RequestParam(required = false) Long endDate) throws ParseException, UnsupportedEncodingException {

        regex = decodeUrl(regex);

        UriGenerator uriGen = new UriGenerator();
        String uri = uriGen.createCDPServerUri(hostname, regex, startDate, endDate, "False", "True");

        System.out.println("GET restoreList: Request Data for Host " + hostname + " with Filter " + regex + " from CDP-Server with generated URL: " + uri);

        RestTemplate restTemplate = new RestTemplate();
        JSONObject result = restTemplate.getForObject(uri.replaceAll("%3D", "="), JSONObject.class);
        System.out.println("GET restoreList: Data received from CDP-Server");
        //System.out.println(result);

        Transformer tf = new Transformer();
        JSONArray transformed = tf.transformForFrontend(result);

        return transformed;
    }

    @GetMapping("/getFile/{hostname}")
    public ResponseEntity<Resource> restoreAndSendFile(@PathVariable String hostname, @RequestParam String path, @RequestParam String name, @RequestParam long mtime, @RequestParam(required = false) boolean download) throws IOException, InterruptedException {

        path = decodeUrl(path);
        name = decodeUrl(name);

        System.out.println("GET getFile: Requested File " + name + " at " + path + " from User " + hostname + " with modifyTime " + mtime);

        //generate Request-ID
        String requestID = UUID.randomUUID().toString();

        //restore requested File in tmp Directory
        new File(tmpDataDirectory + requestID).mkdir();

        //start the Restore Process using the sauvegarde Restore-Client
        ProcessBuilder builder = new ProcessBuilder();
        String restorePath = tmpDataDirectory + requestID + "/";
        builder.directory(new File(restorePath));
        builder.command("cdpfglrestore", "-r", "^\\Q" + path + "\\E$", "-n", hostname, "-t", unixTimeToDate(mtime, false));
        Process process = builder.start();
        if (DEBUG_CDP_SERVER) {
            InputStream inputStream = process.getInputStream();
            Consumer<String> consumer = System.out::println;
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
        process.waitFor();

        //return the restored File
        File restoredFile = new File(restorePath + name);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(restoredFile));

        String fileExtension = getFileExtension(name);
        String mediaType = fileExtension.equals(".pdf") ? "application/pdf" : "application/octet-stream";
        String contentDisposition = download ? "attachment" : "inline";

        HttpHeaders header = createHttpHeaders(contentDisposition + "; filename=" + removeFileExtension(name, fileExtension) + "_" + mtime + fileExtension);

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(restoredFile.length())
                .contentType(MediaType.parseMediaType(mediaType))
                .body(resource);
    }

    @GetMapping("/restoreFolder/{hostname}")
    public String restoreAndSaveFile(@PathVariable String hostname, @RequestParam String path, @RequestParam String name, @RequestParam Long mtimestart, @RequestParam Long mtimeend, @RequestParam(required = false, defaultValue = "/") String regex) throws IOException, InterruptedException, ParseException {

        name = decodeUrl(name);
        path = decodeUrl(path);
        regex = decodeUrl(regex);

        //default Filter is / and matches all Files
        System.out.println("GET restoreFolder: Requested Restore of Folder " + name + " at " + path + " from User " + hostname + " from modifyTime " + mtimestart + " until " + mtimeend + " with Filter " + regex);

        long currentUnixTime = Instant.now().getEpochSecond();
        new File(restoreDataDirectory + currentUnixTime).mkdir();

        UriGenerator uriGen = new UriGenerator();
        String uri = uriGen.createCDPServerUri(hostname, path, mtimestart, mtimeend, "False", "True");

        System.out.println("GET restoreFolder: Request Data for Host " + hostname + " from CDP-Server with generated URL: " + uri);

        RestTemplate restTemplate = new RestTemplate();
        JSONObject result = restTemplate.getForObject(uri, JSONObject.class);

        JSONParser parser = new JSONParser();
        JSONObject parsedJson = (JSONObject) parser.parse(result.toJSONString());
        JSONArray fileList = (JSONArray) parsedJson.get("file_list");

        long restoreCount = 0;

        for (Object obj : fileList) {

            JSONObject jObj = (JSONObject) obj;
            String filePath = (String) jObj.get("name");
            long fileType = (long) jObj.get("filetype");
            long mtime = (long) jObj.get("mtime");

            if (fileType != 2 && filePath.contains(regex)) {

                LinkedList<String> pathList = new LinkedList<>(Arrays.asList(filePath.split("/")));
                pathList.removeLast();
                String tmpPath = "";
                for (String s : pathList) {
                    tmpPath += "/" + s;
                }
                tmpPath = tmpPath.replaceFirst("/", "");
                //System.out.println(tmpPath);

                new File(restoreDataDirectory + currentUnixTime + "/" + tmpPath).mkdirs();
                String restorePath = restoreDataDirectory + currentUnixTime + "/" + tmpPath;

                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.directory(new File(restorePath));
                processBuilder.command("cdpfglrestore", "-r", "^\\Q" + filePath + "\\E$", "-n", hostname, "-t", unixTimeToDate(mtime, false));
                Process process = processBuilder.start();
                if (DEBUG_CDP_SERVER) {
                    InputStream inputStream = process.getInputStream();
                    Consumer<String> consumer = System.out::println;
                    new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
                }
                process.waitFor();
                restoreCount++;
            }
        }

        long fileCount = countFilesRecursive(Paths.get(restoreDataDirectory + currentUnixTime));

        String answer = "Successfully restored " + fileCount + "/" + restoreCount + " Files!\n";
        if (fileCount < restoreCount)
            answer += "To identify the failed Files, calculate Entropy and check the Webserver Log.\n";
        answer += "You can download the restored Folder now!";
        System.out.println(answer);

        return answer;
    }

    @GetMapping("/folderList")
    public JSONArray getRestoredFolderList() throws IOException {
        JSONArray folderArray = new JSONArray();
        File[] files = new File(restoreDataDirectory).listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                JSONObject jObj = new JSONObject();
                jObj.put("name", file.getName());
                jObj.put("fsize", getFolderSize(Paths.get(file.getAbsolutePath())));
                folderArray.add(jObj);
            }
        }
        return folderArray;
    }

    @GetMapping("/downloadFolder/{name}")
    public void downloadFolder(HttpServletResponse response, @PathVariable String name) throws IOException {

        System.out.println("GET downloadFolder: Requested Download of restored Folder " + name);

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name + ".zip");
        try (ZipOutputStream zs = new ZipOutputStream(response.getOutputStream())) {
            Path pp = Paths.get(restoreDataDirectory + name);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        File tmp = new File(path.toString());
                        zipEntry.setTime(tmp.lastModified());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            zs.finish();
        }
    }

    @DeleteMapping("/restoredFolder/{name}")
    public String deleteRestoredFolder(@PathVariable long name) {
        File file = new File(restoreDataDirectory + name);
        FileSystemUtils.deleteRecursively(file);
        System.out.println("Folder " + name + " deleted!");
        return "Folder " + name + " deleted!";
    }

    @GetMapping("/calculateEntropy/{hostname}")
    public String calculateEntropy(@PathVariable String hostname, @RequestParam String path, @RequestParam String name, @RequestParam Long mtimestart, @RequestParam Long mtimeend, @RequestParam(required = false, defaultValue = "/") String regex) throws IOException, InterruptedException, ParseException {

        path = decodeUrl(path);
        name = decodeUrl(name);
        regex = decodeUrl(regex);

        //default Filter is / and matches all Files
        System.out.println("GET calculateEntropy: Requested Entropy of Folder " + name + " at " + path + " from User " + hostname + " from modifyTime " + mtimestart + " until " + mtimeend + " with Filter " + regex);

        long currentUnixTime = Instant.now().getEpochSecond();

        UriGenerator uriGen = new UriGenerator();
        String uri = uriGen.createCDPServerUri(hostname, path, mtimestart, mtimeend, "False", "True");

        System.out.println("GET calculateEntropy: Request Data for Host " + hostname + " from CDP-Server with generated URL: " + uri);

        RestTemplate restTemplate = new RestTemplate();
        JSONObject result = restTemplate.getForObject(uri, JSONObject.class);

        JSONParser parser = new JSONParser();
        JSONObject parsedJson = (JSONObject) parser.parse(result.toJSONString());
        JSONArray fileList = (JSONArray) parsedJson.get("file_list");

        File csvFile = new File(entropyDataDirectory + currentUnixTime + ".csv");

        long count = 0;
        long failed = 0;

        for (Object obj : fileList) {

            JSONObject jObj = (JSONObject) obj;
            String filePath = (String) jObj.get("name");
            long fileType = (long) jObj.get("filetype");
            long mtime = (long) jObj.get("mtime");

            if (fileType != 2 && filePath.contains(regex)) {

                List<String> pathList = Arrays.asList(jObj.get("name").toString().split("/"));
                String filename = pathList.get(pathList.size() - 1);

                //restore every requested File in tmp-Directory
                String tmpFileID = UUID.randomUUID().toString();
                new File(tmpDataDirectory + tmpFileID).mkdir();
                String restorePath = tmpDataDirectory + tmpFileID;

                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.directory(new File(restorePath));
                processBuilder.command("cdpfglrestore", "-r", "^\\Q" + filePath + "\\E$", "-n", hostname, "-t", unixTimeToDate(mtime, false));
                Process process = processBuilder.start();
                if (DEBUG_CDP_SERVER) {
                    InputStream inputStream = process.getInputStream();
                    Consumer<String> consumer = System.out::println;
                    new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
                }
                process.waitFor();
                count++;

                //calculate Entropy for every restored File
                File restoredFile = new File(restorePath + "/" + filename);
                Entropies entropies = new Entropies();
                try {
                    entropies.calculateEntropyLudwig(csvFile, restoredFile, filePath, mtime);
                } catch (NoSuchFileException e) {
                    System.err.println("calculateEntropy failed for File " + restoredFile.getAbsolutePath() + " not found, " +
                            "because File was not restored.\n Check the CDP-Server Logs for this File!");
                    failed++;
                }
            }
        }

        String answer = "Calculated Entropy for " + (count - failed) + "/" + count + " Files!\n";
        if (failed > 0) answer += "Check the Webserver Log for the failed Files.\n";
        answer += "You can download the Entropy File now!";
        System.out.println(answer);

        return answer;
    }

    @GetMapping("/entropyList")
    public JSONArray getCalculatedEntropyList() {
        JSONArray entropyArray = new JSONArray();
        File[] files = new File(entropyDataDirectory).listFiles();

        for (File file : files) {
            if (file.isFile()) {
                JSONObject jObj = new JSONObject();
                jObj.put("name", file.getName());
                jObj.put("fsize", file.length());
                entropyArray.add(jObj);
            }
        }
        return entropyArray;
    }

    @GetMapping("/downloadEntropy/{name}")
    public ResponseEntity<Resource> downloadEntropy(@PathVariable String name) throws FileNotFoundException {

        System.out.println("GET downloadEntropy: Requested Download of calculated Entropy " + name);
        File file = new File(entropyDataDirectory + name);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        String mediaType = "text/csv";
        String contentDisposition = "attachment; filename=" + name;
        HttpHeaders header = createHttpHeaders(contentDisposition);

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType(mediaType))
                .body(resource);
    }

    @DeleteMapping("/entropy/{name}")
    public String deleteEntropy(@PathVariable String name) {
        File file = new File(entropyDataDirectory + name);
        FileSystemUtils.deleteRecursively(file);
        System.out.println("Entropy " + name + " deleted!");
        return "Entropy " + name + " deleted!";
    }

    private HttpHeaders createHttpHeaders(String contentDisposition) {

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        header.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        header.add(HttpHeaders.PRAGMA, "no-cache");
        header.add(HttpHeaders.EXPIRES, "0");

        return header;
    }
}