# Burp Ai Wordlist Generator ( Baiwogen )  -  Context AI-Powered Wordlist Generator

Baiwogen is a Burp Suite extension that leverages AI to generate targeted wordlists for security testing. It traverses your target’s sitemap, filters relevant endpoints, and produces variations of paths, file names, and parameters.

## Features

- **Sitemap-Driven**: Select a node in Burp’s sitemap tree or table and automatically gather all requests under that prefix.
- **AI-Powered Variations**: Uses OpenAI-backed AI (via the Montoya API) to generate semantic synonyms, case-style variants, file-disclosure patterns, and parameter pollution wordlists.
- **Structured I/O**: Sends only essential data (method, host, path, query/body parameters) in JSON to the AI for efficiency and security.
- **Filtering**: Automatically excludes static assets (images, fonts, CSS, hashed JS, icons) to focus on business endpoints.
- **Interactive UI**: Tabbed interface for **Paths**, **Files**, and **Params**, with copy/save buttons, context reset, and refine functionality.
- **Context Management**: Maintains conversation context with the AI to refine wordlists iteratively.

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/incogbyte/baiwogen.git
   cd baiwogen
   ```
2. Build the JAR with Gradle:
   ```bash
   ./gradlew clean build
   ```
3. Load the extension in Burp Suite:
   - Go to **Extender → Extensions**
   - Click **Add**, select **Java** as the type
   - Browse to `build/libs/baiwogen-<version>.jar` and click **Next**

## Usage

1. **Enable AI Features**: In Burp Suite, under **Settings → Extensions → Baiwogen**, ensure AI features are enabled.
2. **Select a Node**: In the **Site map** panel, select a folder or request node.
3. **Generate Wordlist**: Right-click and choose **Gen Wordlist**.
4. **View Tabs**: Navigate the **Paths**, **Files**, and **Params** tabs:
   - **Copy**: Copies the current list to clipboard.
   - **Save**: Saves the list to a text file.
   - **Reset Context**: Clears AI conversation context and resets lists.
   - **Refine**: Prompt additional AI queries to refine results.

## Configuration

- **AI Model & Temperature**: Controlled via Montoya API’s `PromptOptions` (default temperature 0.3).
- **Request Filters**: Patterns in `generateWordlist` control which URLs are sent to AI. Customize regexes for your target’s tech stack.
- **Payload Encoding**: Unsafe characters in URLs are percent-encoded before sending.

## Development

- **Language**: Java 17
- **Dependencies**:
  - Montoya API for Burp Suite
  - Google Gson for JSON serialization
- **Build**: Gradle wrapper (`./gradlew`)

### Adding New Filters

In `Extension.java` → `generateWordlist`, modify the `path.matches(...)` and `path.contains(...)` blocks to include/exclude patterns specific to your target.

### Prompt Customization

Edit `AIConversationManager`’s system prompt to tune AI behavior:
- Adjust section headers
- Modify variation rules
- Change language detection

## License

MIT License. See [LICENSE](LICENSE) for details.

---

TODO
  - Fixing AI Context menu
  - Improve Wordlists prompt
  - Fixing the menu to save files

Powered By @BurpAI
