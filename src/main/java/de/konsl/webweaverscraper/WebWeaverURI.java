package de.konsl.webweaverscraper;

import java.net.URI;

public record WebWeaverURI(Type type, URI uri) {
    public enum Type {
        Unknown,
        Page,
        Popup
    }
}
