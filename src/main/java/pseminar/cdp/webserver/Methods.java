package pseminar.cdp.webserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

class Methods {

    static String unixTimeToDate(long unixTime, boolean toBase64) {
        String myDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(unixTime * 1000L));
        //System.out.println(myDate);
        if (toBase64) myDate = Base64.getEncoder().encodeToString(myDate.getBytes());
        return myDate;
    }

    static String getFileExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (i > p) {
            extension = fileName.substring(i);
        }
        return extension;
    }

    static String removeFileExtension(String text, String regex) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1");
    }

    static long countFilesRecursive(Path dir) throws IOException {
        return Files.walk(dir)
                .parallel()
                .filter(p -> !p.toFile().isDirectory())
                .count();
    }

    static long getFolderSize(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(p -> p.toFile().isFile())
                .mapToLong(p -> p.toFile().length())
                .sum();
    }

    static String decodeUrl(String value) throws UnsupportedEncodingException {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
    }
}