package pseminar.cdp.webserver;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;

import static pseminar.cdp.webserver.Methods.unixTimeToDate;

class UriGenerator {

    private String baseUri = "http://localhost:5468/File/List.json";

    String createCDPServerUri(String hostname, String filename, Long afterdate, Long beforedate, String latest, String reduced) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        builder.queryParam("hostname", hostname);
        builder.queryParam("filename", Base64.getEncoder().encodeToString((filename).getBytes()));
        if (!(afterdate == null)) builder.queryParam("afterdate", unixTimeToDate(afterdate, true));
        if (!(beforedate == null)) builder.queryParam("beforedate", unixTimeToDate(beforedate + 1, true));
        builder.queryParam("latest", latest);
        builder.queryParam("reduced", reduced);
        String uri = builder.build().encode().toUriString();
        return uri.replaceAll("%3D", "=");

    }
}