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

    private static final Pattern PATTERN_INIT_OPEN_POPUP = Pattern.compile("function\\s+my_init\\s*\\(\\s*\\)\\s*\\{\\s*OpenPopUp\\s*\\(\\s*'([^']*(?:\\\\'[^']*)*)'\\s*\\)\\s*;\\s*}", Pattern.MULTILINE);

    private static final Pattern PATTERN_TOGGLE_AJAX_HTML = Pattern.compile("toggle_ajax_html\\s*\\(\\s*event\\s*,\\s*'([^']*(?:\\\\'[^']*)*)'\\s*\\)\\s*;");
    private static final Pattern PATTERN_OPEN_POPUP = Pattern.compile("return\\s+OpenPopUp\\s*\\(\\s*'([^']*(?:\\\\'[^']*)*)'\\s*\\)\\s*;");
    private static final Pattern PATTERN_HELP_POPUP = Pattern.compile("return\\s+help\\s*\\(\\s*'([^']*(?:\\\\'[^']*)*)'\\s*\\)\\s*;");
    private static final Pattern PATTERN_REDIRECT = Pattern.compile("return\\s+redirect\\s*\\(\\s*'([^']*(?:\\\\'[^']*)*)'\\s*,\\s*(?:true|false)\\)\\s*;");


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
            Matcher popupMatcher = PATTERN_INIT_OPEN_POPUP.matcher(script);

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

    public void navigate(String target) throws IOException {
        navigate(target, false);
    }

    public Popup navigate(String target, boolean loadPopup) throws IOException {
        WebWeaverURI parsed = parse(target);
        return navigateToParsed(parsed, loadPopup);
    }

    public void navigate(Element link) throws IOException {
        navigate(link, false);
    }

    public Popup navigate(Element link, boolean loadPopup) throws IOException {
        WebWeaverURI parsed = parse(link.attr("onclick"));
        if (parsed.type() == WebWeaverURI.Type.Unknown)
            parsed = parse(link.attr("href"));

        return navigateToParsed(parsed, loadPopup);
    }

    private Popup navigateToParsed(WebWeaverURI parsed, boolean loadPopup) throws IOException {
        switch (parsed.type()) {
            case Page -> {
                return navigate(parsed.uri(), loadPopup);
            }

            case Popup -> {
                if (!loadPopup) return new Popup(parsed.uri(), null);

                URI popupUri = HttpUtils.resolveWebWeaverURI(uri, parsed.uri());
                WebWeaverResponse popupResponse = HttpUtils.executeWebWeaverRequest(popupUri, httpClient);

                return new Popup(popupResponse.uri(), Jsoup.parse(popupResponse.content()));
            }
        }

        return null;
    }

    private WebWeaverURI parse(String query) {
        if (query == null || query.isBlank()) return new WebWeaverURI(WebWeaverURI.Type.Unknown, null);

        try {
            Matcher toggleAjaxMatcher = PATTERN_TOGGLE_AJAX_HTML.matcher(query);
            if (toggleAjaxMatcher.find())
                return new WebWeaverURI(WebWeaverURI.Type.Popup, URI.create(toggleAjaxMatcher.group(1)));

            Matcher openPopupMatcher = PATTERN_OPEN_POPUP.matcher(query);
            if (openPopupMatcher.find())
                return new WebWeaverURI(WebWeaverURI.Type.Popup, URI.create(openPopupMatcher.group(1)));

            Matcher helpPopupMatcher = PATTERN_HELP_POPUP.matcher(query);
            if (helpPopupMatcher.find())
                return new WebWeaverURI(WebWeaverURI.Type.Popup, URI.create(helpPopupMatcher.group(1)));

            Matcher redirectMatcher = PATTERN_REDIRECT.matcher(query);
            if (redirectMatcher.find())
                return new WebWeaverURI(WebWeaverURI.Type.Page, URI.create(redirectMatcher.group(1)));

            return new WebWeaverURI(WebWeaverURI.Type.Page, URI.create(query));
        } catch (IllegalArgumentException ignored) {
            return new WebWeaverURI(WebWeaverURI.Type.Unknown, null);
        }
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
