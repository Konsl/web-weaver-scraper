package de.konsl.webweaverscraper;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HttpUtils {
    public static boolean shouldRedirect(ClassicHttpResponse response) {
        if (!response.containsHeader(HttpHeaders.LOCATION))
            return false;
        else return switch (response.getCode()) {
            case HttpStatus.SC_REDIRECTION,
                    HttpStatus.SC_MOVED_PERMANENTLY,
                    HttpStatus.SC_MOVED_TEMPORARILY,
                    HttpStatus.SC_SEE_OTHER,
                    HttpStatus.SC_TEMPORARY_REDIRECT,
                    HttpStatus.SC_PERMANENT_REDIRECT -> true;
            default -> false;
        };
    }

    public static boolean isSuccessWithContent(ClassicHttpResponse response) {
        return switch (response.getCode()) {
            case HttpStatus.SC_OK,
                    HttpStatus.SC_PARTIAL_CONTENT,
                    HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION -> true;
            default -> false;
        };
    }

    public static URI resolveWebWeaverURI(URI base, URI target) {
        if (base != null && !target.isAbsolute()) target = base.resolve(target);
        if (target.getPath() != null && target.getPath().endsWith("9.php"))
            target = target.resolve(target.getRawFragment());

        return target;
    }

    public static WebWeaverResponse executeWebWeaverRequest(URI uri, CloseableHttpClient httpClient) throws IOException {
        final AtomicBoolean succeeded = new AtomicBoolean(false);
        final AtomicReference<URI> uriAtomic = new AtomicReference<>(uri);

        String content = "";

        while (!succeeded.get()) {
            HttpGet request = new HttpGet(uriAtomic.get());

            content = httpClient.execute(request, response -> {
                if (HttpUtils.shouldRedirect(response)) {
                    uriAtomic.set(HttpUtils.resolveWebWeaverURI(uriAtomic.get(), URI.create(response.getHeader(HttpHeaders.LOCATION).getValue())));
                    return "";
                }

                if (HttpUtils.isSuccessWithContent(response)) {
                    succeeded.set(true);
                    return EntityUtils.toString(response.getEntity());
                }

                return "";
            });
        }

        return new WebWeaverResponse(uriAtomic.get(), content);
    }
}
