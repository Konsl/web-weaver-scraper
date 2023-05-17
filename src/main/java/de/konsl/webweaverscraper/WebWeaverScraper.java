package de.konsl.webweaverscraper;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebWeaverScraper {
    static final Logger LOGGER = LoggerFactory.getLogger(WebWeaverScraper.class);

    private static final Pattern PATTERN_OPEN_POPUP = Pattern.compile("function\\s+my_init\\s*\\(\\s*\\)\\s*\\{\\s*OpenPopUp\\s*\\(\\s*'([^']*(?:[^']*\\\\')*)'\\s*\\)\\s*;\\s*}", Pattern.MULTILINE);

    private final CloseableHttpClient httpClient;

    private URI uri;
    private Document document;

    public WebWeaverScraper() {
        httpClient = HttpClients.custom()
                .useSystemProperties()
                .disableRedirectHandling()
                .build();
    }

    public void navigate(URI target) throws IOException {
        navigate(target, false);
    }

    public Popup navigate(URI target, boolean loadPopup) throws IOException {
        uri = HttpUtils.resolveWebWeaverURI(uri, target);
        WebWeaverResponse response = HttpUtils.executeWebWeaverRequest(uri, httpClient);
        uri = response.uri();

        String content = response.content();
        document = Jsoup.parse(content);

        URI popupUri = null;
        for (Element scriptElement : document.getElementsByTag("script")) {
            String script = scriptElement.data();
            Matcher popupMatcher = PATTERN_OPEN_POPUP.matcher(script);

            if (popupMatcher.find()) {
                popupUri = URI.create(popupMatcher.group(1));
                break;
            }
        }

        if (popupUri == null) return null;
        if (!loadPopup) return new Popup(popupUri, null);

        popupUri = HttpUtils.resolveWebWeaverURI(uri, popupUri);
        WebWeaverResponse popupResponse = HttpUtils.executeWebWeaverRequest(popupUri, httpClient);

        return new Popup(popupResponse.uri(), Jsoup.parse(popupResponse.content()));
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public URI getUri() {
        return uri;
    }

    public Document getDocument() {
        return document;
    }
}
