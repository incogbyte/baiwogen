package com.incogbyte.baiwogen;

import java.awt.Component;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.EnhancedCapability;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.MimeType;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.sitemap.SiteMap;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.InvocationType;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.ai.chat.Prompt;
import burp.api.montoya.logging.Logging;
import java.util.Set;
import java.lang.reflect.Type;
import com.google.gson.JsonSyntaxException; 

public class Extension implements BurpExtension, ContextMenuItemsProvider {
    private static final Gson GSON = new Gson();

    private MontoyaApi api;
    private Logging logging;
    private Prompt aiPrompt;
    private Ai ai;
    private AIConversationManager conversationManager;
    private WordlistPanel wordlistPanel;
    private List<String> lastExtractedContent;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.logging = api.logging();
        this.ai = api.ai();
        this.aiPrompt = api.ai().prompt();
        this.conversationManager = new AIConversationManager(aiPrompt, logging);

        api.extension().setName("Baiwogen - AI-Powered Wordlist Generator");
        api.userInterface().registerContextMenuItemsProvider(this);

        wordlistPanel = new WordlistPanel();
        api.userInterface().registerSuiteTab("Baiwogen", wordlistPanel);

        wordlistPanel.addResetContextListener(e -> {
            conversationManager.resetContext();
            wordlistPanel.resetAll();
            wordlistPanel.updateContextSize(conversationManager.getContextSize());
            wordlistPanel.updateStatus("Context reset.");
        });
        wordlistPanel.addRefineListener(e -> {
            String query = JOptionPane.showInputDialog(null, "Enter refinement query:");
            if (query != null && !query.isBlank()) {
                String result = conversationManager.addUserQuery(query);
                populateCategories(result);
                wordlistPanel.updateContextSize(conversationManager.getContextSize());
                wordlistPanel.updateStatus("Wordlist refined successfully!");
            }
        });

        logging.logToOutput("Baiwogen extension loaded.");
    }

    @Override
    public Set<EnhancedCapability> enhancedCapabilities() {
        return Set.of(EnhancedCapability.AI_FEATURES);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        if (event.isFrom(InvocationType.SITE_MAP_TREE, InvocationType.SITE_MAP_TABLE)) {
            JMenuItem generate = new JMenuItem("Gen Wordlist");
            generate.addActionListener(e -> generateWordlist(event));
            menuItems.add(generate);
        }
        return menuItems;
    }

    private void generateWordlist(ContextMenuEvent event) {
    new Thread(() -> {
        try {
            List<HttpRequestResponse> sel = event.selectedRequestResponses();
            if (sel.isEmpty()) {
                logging.logToOutput("Select a node in the sitemap first.");
                return;
            }
            String first = sel.get(0).request().url();
            String prefix = first.substring(0, first.lastIndexOf('/') + 1);

            List<HttpRequestResponse> items = api.siteMap()
                .requestResponses()
                .stream()
                .filter(rr -> rr.request().url().startsWith(prefix))
                .collect(Collectors.toList());

            if (items.isEmpty()) {
                wordlistPanel.updateStatus("No items under " + prefix);
                return;
            }

            List<String> payloads = new ArrayList<>();
            for (HttpRequestResponse rr : items) {
                String payload = buildJsonPayload(rr);
                if (payload != null) {
                    payloads.add(payload);
                }
            }

            if (payloads.isEmpty()) {
                wordlistPanel.updateStatus("No relevant items under " + prefix);
                return;
            }

            wordlistPanel.updateStatus("Sending JSON payloads to AI...");
            String resultJson = conversationManager.startConversation(payloads);

            if (resultJson.startsWith("Error")) {
                wordlistPanel.updateStatus("Error: " + resultJson);
                return;
            }
            
            wordlistPanel.updateStatus("Everything is fine, generating wordlist...");
            populateCategories(resultJson);

            wordlistPanel.updateContextSize(conversationManager.getContextSize());
            wordlistPanel.updateStatus("Wordlist generated successfully!");
        }
        catch (Exception ex) {
            logging.logToError("Error generating wordlist: " + ex.getMessage());
            wordlistPanel.updateStatus("Error: " + ex.getMessage());
        }
    }).start();
}

  private String buildJsonPayload(HttpRequestResponse rr) {
    try {
        String rawUrl = rr.request().url()
            .replace("{", "%7B")
            .replace("}", "%7D")
            .replace(" ", "%20");
        
        URI uri = new URI(rawUrl);
        
        URI safeUri = new URI(
            uri.getScheme(),
            uri.getAuthority(),
            uri.getRawPath(),
            uri.getRawQuery(),
            null
        );

        String safePath = safeUri.getRawPath();
        if (safePath.matches(".*\\.(css|png|jpe?g|gif|svg|woff2?|ttf|otf|ico)$")) {
            return null;
        }
        
        String host  = safeUri.getHost();
        String path  = safePath;
        String query = safeUri.getRawQuery();
        
        Map<String,String> params = new LinkedHashMap<>();
        if (query != null) {
            for (String kv : query.split("&")) {
                String[] p = kv.split("=", 2);
                params.put(p[0], p.length>1 ? p[1] : "");
            }
        }
        
        Map<String,Object> obj = new LinkedHashMap<>();
        obj.put("method", rr.request().method());
        obj.put("host",   host);
        obj.put("path",   path);
        obj.put("queryParams", params);
        
        String body = rr.request().bodyToString();
        String contentType = rr.request().headers().stream()
            .filter(h -> h.name().equalsIgnoreCase("Content-Type"))
            .map(h -> h.value())
            .findFirst().orElse("");
        
        if (contentType.startsWith("application/x-www-form-urlencoded")) {
            Map<String,String> form = new LinkedHashMap<>();
            for (String kv : body.split("&")) {
                String[] p = kv.split("=",2);
                form.put(p[0], p.length>1 ? p[1] : "");
            }
            obj.put("bodyParams", form);
        }
        else if (contentType.contains("application/json")) {
            try {
                Type t = new TypeToken<Map<String,Object>>(){}.getType();
                obj.put("jsonBody", GSON.fromJson(body, t));
            } catch (Exception e) {
                obj.put("body", body.length()>2000? body.substring(0,2000): body);
            }
        }
        else if (!body.isBlank()) {
            obj.put("body", body.length()>2000? body.substring(0,2000): body);
        }
        
        String json = GSON.toJson(obj);
        logging.logToOutput("Sending JSON payload: " + json);
        return json;
        
    } catch (Exception e) {
        logging.logToError("Skipping URL: " + rr.request().url() + " → " + e.getMessage());
        return null;
    }
}

    private void populateCategories(String aiOutput) {
    
    String text = aiOutput.trim();

    int start = text.indexOf('{');
    int end   = text.lastIndexOf('}');
    logging.logToOutput("AI output: " + text);
    if (start < 0 || end <= start) {
        logging.logToError("No JSON object found in AI output");
        wordlistPanel.updateStatus("Error parsing AI output");
        return;
    }
    String json = text.substring(start, end + 1);

    if ((json.startsWith("\"") && json.endsWith("\"")) ||
        (json.startsWith("'") && json.endsWith("'"))) {
        json = json.substring(1, json.length() - 1)
                   .replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\\\", "\\");
    }

    try {
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> map = GSON.fromJson(json, mapType);

        List<String> files  = splitCommaList(map.getOrDefault("fileVariations",  List.of()));
        List<String> paths  = splitCommaList(map.getOrDefault("pathVariations",  List.of()));
        List<String> params = splitCommaList(map.getOrDefault("paramVariations", List.of()));

        wordlistPanel.setCategoryItems("Files", files);
        wordlistPanel.setCategoryItems("Paths", paths);
        wordlistPanel.setCategoryItems("Params", params);

    } catch (JsonSyntaxException|IllegalStateException ex) {
        logging.logToError("Failed to parse AI JSON: " + ex.getMessage());
        logging.logToOutput("Raw AI output:\n" + aiOutput);
        wordlistPanel.updateStatus("Error parsing AI output — check logs");
    }
}

private List<String> splitCommaList(List<String> raw) {
    List<String> out = new ArrayList<>();
    for (String item : raw) {
        for (String part : item.split(",")) {
            part = part.trim();
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
    }
    return out;
}

private String extractJsonObject(String aiOutput) {
    String s = aiOutput.trim();
    if (!s.startsWith("{")) {
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            s = s.substring(start, end+1);
        }
    }
    return s;
}
}
