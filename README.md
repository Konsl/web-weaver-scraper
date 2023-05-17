# web-weaver-scraper
Web scraper for WebWeaver based platforms

[![](https://jitci.com/gh/Konsl/web-weaver-scraper/svg)](https://jitci.com/gh/Konsl/web-weaver-scraper)

## Example usage


```JAVA
// use web-weaver-api to request an autologin url
WebWeaverClient client = new WebWeaverClient();
client.login(...);

String autologinUrl = client.request(new SelfAutologinRequest("some_class@webweaver.de",
                        "learning_plan",
                        "some learning plan"),
                FocusObject.TRUSTS)
        .getUrl();

// use web-weaver-scraper to read the learning plan's contents
WebWeaverScraper scraper = new WebWeaverScraper();
Popup popup = scraper.navigate(URI.create(autologinUrl), true);

Element learningPlanContent = popup.document().selectFirst("#main_content > p.panel");
if (learningPlanContent != null)
    System.out.println(learningPlanContent.wholeText());

Element logoutLink = scraper.getDocument().selectFirst("a[href^=107480.php]");
if (logoutLink != null) {
    URI logoutUri = URI.create(logoutLink.attr("href"));
    scraper.navigate(logoutUri);
}

...

// log out from web-weaver-api client
client.logout();
```
