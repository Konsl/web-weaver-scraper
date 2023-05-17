package de.konsl.webweaverscraper;

import java.net.URI;

public record WebWeaverResponse(URI uri, String content) {
}
