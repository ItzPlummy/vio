package com.plummy.vio.core.tool.catalog.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.config.Endpoints;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.BasicTool;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.requester.ToolRequester;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.Set;

public class WebSearchTool implements BasicTool {

    private static final int MAX_RESULTS = 10;

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "Use a search engine to find relevant URLs to the search query. Use this tool when you need to use a fetch tool, but you don't have a certainly valid URL.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("query", new ArgumentDefinition(ArgumentType.STRING, "Simple text search query, used to fetch relevant URLs."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(ToolRequester requester, Arguments arguments) {
        String query = arguments.getString("query");

        if (query == null || query.isBlank()) {
            return ExecutionResult.invalidArguments("Search query can't be empty");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(Endpoints.WEB_SEARCH_URL))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", Endpoints.WEB_SEARCH_USER_AGENT)
                .header("Accept", "text/html")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("q=%s".formatted(URLEncoder.encode(query, StandardCharsets.UTF_8))))
                .build();

        HttpResponse<String> response;

        try {
            response = Vio.getHttpClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            return ExecutionResult.failure("Couldn't search");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Couldn't search");
        }

        if (response.statusCode() != 200) {
            return ExecutionResult.failure("Couldn't search: HTTP status %s".formatted(response.statusCode()));
        }

        Document document = Jsoup.parse(response.body(), Endpoints.WEB_SEARCH_URL);

        if (document.selectFirst(".anomaly-modal, #challenge-form") != null) {
            return ExecutionResult.failure("Search was rate limited by DuckDuckGo, try again later");
        }

        JsonArray results = new JsonArray();
        Set<String> seenUrls = new HashSet<>();

        for (Element result : document.select("div.result:not(.result--ad)")) {
            if (results.size() >= MAX_RESULTS) {
                break;
            }

            Element link = result.selectFirst("a.result__a");

            if (link == null) {
                continue;
            }

            String url = resolveUrl(link.attr("abs:href"));

            if (url == null || !seenUrls.add(url)) {
                continue;
            }

            String title = link.text();
            Element snippetElement = result.selectFirst(".result__snippet");
            String snippet = snippetElement != null ? snippetElement.text() : "";

            JsonObject entry = new JsonObject();

            entry.addProperty("title", title);
            entry.addProperty("snippet", snippet);
            entry.addProperty("url", url);

            results.add(entry);
        }

        JsonObject json = new JsonObject();

        json.add("results", results);

        return ExecutionResult.success(json);
    }

    private static String resolveUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }

        URI uri;

        try {
            uri = URI.create(href);
        } catch (IllegalArgumentException e) {
            return null;
        }

        if (uri.getHost() != null && uri.getHost().endsWith("duckduckgo.com") && "/l/".equals(uri.getPath()) && uri.getRawQuery() != null) {
            for (String parameter : uri.getRawQuery().split("&")) {
                if (parameter.startsWith("uddg=")) {
                    return URLDecoder.decode(parameter.substring(5), StandardCharsets.UTF_8);
                }
            }

            return null;
        }

        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) ? href : null;
    }
}
