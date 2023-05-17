package de.konsl.webweaverscraper;

import org.jsoup.nodes.Document;

import java.net.URI;

public record Popup(URI uri, Document document) {
}
