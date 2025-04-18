package com.incogbyte.baiwogen;

import burp.api.montoya.ai.chat.Message;
import burp.api.montoya.ai.chat.Prompt;
import burp.api.montoya.ai.chat.PromptException;
import burp.api.montoya.ai.chat.PromptOptions;
import burp.api.montoya.ai.chat.PromptResponse;
import burp.api.montoya.logging.Logging;
import com.incogbyte.baiwogen.Extension;
import com.incogbyte.baiwogen.AIConversationManager;    

import java.util.ArrayList;
import java.util.List;

/**
 * Manages multi-turn conversations with the Burp AI.
 */
public class AIConversationManager {
    private final List<Message> conversationContext;
    private final Prompt prompt;
    private final Logging logging;
    private boolean isInitialized;

    /**
     * Creates a new AIConversationManager.
     *
     * @param prompt  The Burp AI prompt instance
     * @param logging The logging instance for error reporting
     */
    public AIConversationManager(Prompt prompt, Logging logging) {
        this.prompt = prompt;
        this.logging = logging;
        this.conversationContext = new ArrayList<>();
        this.isInitialized = false;
    }

    /**
     * Initializes the conversation with a system message defining the AI's role.
     */
    public void initializeContext() {
        if (!isInitialized) {
            conversationContext.clear();

            String systemPrompt = """
                        You are a expert security analyst and your job is to generate a wordlist for security testing based on a full HTTP request (request line + headers + body if present) sent to you, perform the following analysis:

                        1. Analyze the naming convention pattern used in the application (kebab-case, camelCase, etc.)
                        2. Based on the request (request line + headers + body if present), identify the main language used English Portuguese, Spanish, French, Biligual, etc. And generate variations wordlist based on the language(s)
                        3. Observe the directory structure and how it's organized
                        4. Identify patterns in how endpoints are constructed 
                        5. Deduce the possible tech stack of the application 

                        After this analysis, generate a list of possible "hidden" or undocumented paths that might exist in the application, including:
                        - Non-obvious API endpoints
                        - Administration directories
                        - Alternative login pages
                        - Debug/development endpoints
                        - Backup or archive versions
                        - Test endpoints
                        - Internal documentation

                        Don't use generic numeric suffixes (path1, path2). Instead, focus on semantically relevant and contextually appropriate variations, considering:
                        - The nature of the business
                        - The structure of existing paths
                        - The vocabulary used in the application
                        - Common conventions in web applications of the same sector.

                        In addition to the analysis I requested earlier, I also want you to:
                        1. Identify specific nomenclature patterns in the cybersecurity sector
                           - Naming patterns for security services and products
                        2. Look for backend/framework indicators:
                           - File extensions in URLs (.php, .aspx, .jsp, etc.)
                           - Specific routing patterns of frameworks (Rails, Laravel, Spring, etc.) only if you detect the tech stack. If you detect the tech stack, generate variations of paths, params, files, etc. based on the tech stack
                           - URL patterns suggesting specific CMS (WordPress, Drupal, etc.) only if you detect the tech stack. If you detect the tech stack, generate variations of paths, params, files, etc. based on the tech stack
                           - URL patterns suggesting specific cloud providers (AWS, Azure, etc.) only if you detect the tech stack. If you detect the tech stack, generate variations of paths, params, files, etc. based on the tech stack
                        3. Analyze potential sensitive endpoints:
                           - Routes related to authentication/login/register/forgot-password/reset-password/dashboard/admin/api/ etc.
                           - Endpoints of API that may expose sensitive data
                           - Administrative functionalities
                        4. Investigate possible development and testing paths:
                           - Staging/homologation environments
                           - Debug/test endpoints
                           - Development APIs
                        5. Propose variations based on:
                           - Common security misconfigurations
                           - Typical path patterns in similar applications
                           - Specific conventions observed in the company structure
                        6. Look for chronological patterns:
                           - Possible old versions of APIs (v1, v2, etc.)
                           - Backup or dated files
                        7. Create a specific section for potentially exposed configuration files:
                           - .env, config.json, web.config, etc. Based on the tech stack, generate variations based on the tech stack
                           - Backup files (.bak, .old, ~, etc.) Based on the tech stack, generate variations based on the tech stack
                        8. Suggest technical resource endpoints:
                           - Documentation (Swagger, API docs, etc.)
                           - Monitoring (health checks, status)
                           - Logs/metrics

                        Prioritize the quality of suggestions over quantity, focusing on paths that would have a higher probability of existing and greater security impact if discovered. DONT FORGET THE OUTPUT ONLY A SINGLE JSON OBJECT WITH KEYS fileVariations, pathVariations, paramVariations.
""";
    

            conversationContext.add(Message.systemMessage(systemPrompt));
            isInitialized = true;
        }
    }

    /**
     * Clears the conversation context and resets the initialized state.
     */
    public void resetContext() {
        conversationContext.clear();
        isInitialized = false;
    }

    /**
     * Starts a new conversation with the first user query.
     *
     * @param content The content to be analyzed
     * @return The AI's response as a string
     */
    public String startConversation(List<String> content) {
        initializeContext();
        
        // Create the user message with the content
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Generate a wordlist for security testing based on the following content from a web application:\n\n");
        
        // Add limited content to avoid overwhelming the AI
        int totalLength = 0;
        for (String text : content) {
            if (totalLength + text.length() > 10000) {
                break;
            }
            userPrompt.append(text).append("\n---\n");
            totalLength += text.length();
        }
        
       
        conversationContext.add(Message.userMessage(userPrompt.toString()));
        
       
        return sendPrompt();
    }

    /**
     * Adds a new user query to the conversation and sends the updated context.
     *
     * @param userQuery The new user query
     * @return The AI's response as a string
     */
    public String addUserQuery(String userQuery) {
        initializeContext();
        
       
        conversationContext.add(Message.userMessage(userQuery));
        
       
        return sendPrompt();
    }

    /**
     * Sends the prompt to the AI with the full conversation context and updates the context
     * with the response.
     *
     * @return The AI's response as a string
     */
    private String sendPrompt() {
        try {
            logging.logToOutput("Sending prompt to AI with context size: " + conversationContext.size());
            
       
            PromptOptions options = PromptOptions.promptOptions()
                .withTemperature(0.3);
            
            // Execute the prompt with the full context
            PromptResponse response = prompt.execute(
                options, 
                conversationContext.toArray(new Message[0])
            );
            
            // Store AI response as an assistant message
            String responseContent = response.content();
            Message assistantMessage = Message.assistantMessage(responseContent);
            conversationContext.add(assistantMessage);
            
            return responseContent;
            
        } catch (PromptException e) {
            // Log the exception specifically for AI prompt errors
            logging.logToError("AI Prompt Error: " + e.getMessage());
            return "Error generating wordlist with AI: " + e.getMessage() + 
                   "\n\nPossible reasons: " +
                   "\n- AI service communication issue" +
                   "\n- Insufficient AI credits" +
                   "\n- Invalid prompt format" +
                   "\n\nPlease try again later or contact support.";
        } catch (Exception e) {
            logging.logToError("Error using AI: " + e.getMessage());
            return "Error generating wordlist: " + e.getMessage();
        }
    }

    /**
     * Returns the current size of the conversation context.
     *
     * @return The number of messages in the conversation context
     */
    public int getContextSize() {
        return conversationContext.size();
    }
    
    /**
     * Checks if the conversation has been initialized.
     *
     * @return true if the conversation has been initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}