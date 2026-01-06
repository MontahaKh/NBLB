package com.Shadows.recommendationservice.service;

import com.Shadows.recommendationservice.client.OrderServiceClient;
import com.Shadows.recommendationservice.model.OrderSummaryDto;
import com.Shadows.recommendationservice.model.ProductDto;

import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final Pattern COUNT_PATTERN = Pattern.compile("^(.*?) \\(x(\\d+)\\)$");

    // Simple in-memory cache: Key = PurchaseHistoryString, Value = Recommendations
    private final Map<String, List<ProductDto>> recommendationCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Seller product suggestions cache: Key = Top-sold items signature
    private final Map<String, List<String>> sellerSuggestionsCache = new java.util.concurrent.ConcurrentHashMap<>();

    private final OrderServiceClient client;
    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder chatClientBuilder, OrderServiceClient client) {
        this.client = client;
        this.chatClient = chatClientBuilder.build();
    }

    // init() method removed as Spring AI handles connection checks lazily or via actuator


    // ... (Helper methods for parsing remain the same) ...
    private SimpleEntry<String, Long> parseNameAndCount(String rawName) {
        Matcher m = COUNT_PATTERN.matcher(rawName);
        if (m.find()) {
            String name = m.group(1);
            long count = Long.parseLong(m.group(2));
            return new SimpleEntry<>(name, count);
        }
        return new SimpleEntry<>(rawName, 1L);
    }

    public List<ProductDto> getRecommendations() {
        // 1. Get User History
        List<OrderSummaryDto> orders;
        try {
            orders = client.getMyOrders();
        } catch (Exception e) {
            log.error("Failed to fetch orders", e);
            orders = Collections.emptyList();
        }

        Map<String, Long> purchasedItems = orders.stream()
                .filter(o -> o.getProductNames() != null)
                .flatMap(o -> o.getProductNames().stream())
                .map(this::parseNameAndCount)
                .collect(Collectors.groupingBy(SimpleEntry::getKey, 
                        Collectors.summingLong(SimpleEntry::getValue)));

        // 2. Get Catalog
        List<ProductDto> catalog;
        try {
            catalog = client.getProducts();
        } catch (Exception e) {
            log.error("Failed to fetch products from Order Service", e);
            catalog = Collections.emptyList();
        }

        if (catalog.isEmpty()) {
            log.warn("Catalog is empty or unreachable. Returning empty recommendations.");
            return Collections.emptyList();
        }

        // 3. If no history, return empty list so the UI prompts to buy something
        if (purchasedItems.isEmpty()) {
            log.info("No purchase history found. Returning empty list.");
            return Collections.emptyList();
        }

        // Cache Check: Determine key from purchased items signature
        // Sorting keys ensures {A=1, B=1} is same as {B=1, A=1}
        String cacheKey = purchasedItems.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("|"));

        if (recommendationCache.containsKey(cacheKey)) {
            log.info("Returning cached recommendations for key: {}", cacheKey);
            return recommendationCache.get(cacheKey);
        }

        // Find Most Bought Item
        Map.Entry<String, Long> mostBoughtEntry = purchasedItems.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        String mostBoughtName = (mostBoughtEntry != null) ? mostBoughtEntry.getKey() : null;

        // 4. Construct Prompt
        String prompt = buildPrompt(purchasedItems, catalog, mostBoughtName);

        // 5. Call Gemini
        String aiResponse = callGemini(prompt);
        log.info("Gemini Response: {}", aiResponse);

        // 6. Parse and Filter
        List<ProductDto> recommendations = parseRecommendations(aiResponse, catalog);
        
        // Ensure the list is mutable
        recommendations = new ArrayList<>(recommendations);

        if (recommendations.isEmpty()) {
             log.warn("Gemini returned no valid recommendations. Using fallback.");
             recommendations = new ArrayList<>(getFallbackRecommendations(catalog, purchasedItems));
        }

        // Explicitly add the most bought item to the TOP of the recommendations
        if (mostBoughtName != null) {
            final String targetName = mostBoughtName;
            ProductDto mostBoughtProduct = catalog.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(null);
            
            if (mostBoughtProduct != null) {
                // Remove if already present (to avoid duplicates, or move to top)
                recommendations.removeIf(p -> p.getId().equals(mostBoughtProduct.getId()));
                // Add to the front
                recommendations.add(0, mostBoughtProduct);
                log.info("Added most bought item '{}' to recommendations.", mostBoughtName);
            }
        }
        
        // Trim to reasonable size (e.g., 5) if needed, but 3 is the prompt target
        
        // Cache successful AI recommendations
        recommendationCache.put(cacheKey, recommendations);
        return recommendations;
    }

    private List<ProductDto> getFallbackRecommendations(List<ProductDto> catalog, Map<String, Long> purchasedItems) {
        if (catalog.isEmpty()) return Collections.emptyList();

        // 1. Identify categories of products the user has purchased
        Set<String> purchasedProductNames = purchasedItems.keySet();
        
        // Find categories from the catalog based on names (simple matching)
        Set<String> preferredCategories = catalog.stream()
            .filter(p -> purchasedProductNames.stream().anyMatch(name -> name.equalsIgnoreCase(p.getName())))
            .map(ProductDto::getCategory)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (preferredCategories.isEmpty()) {
            // No category data found, random shuffle
            List<ProductDto> shuffled = new ArrayList<>(catalog);
            Collections.shuffle(shuffled);
            return shuffled.stream().limit(3).collect(Collectors.toList());
        }

        log.info("Generating weighted fallback recommendations based on categories: {}", preferredCategories);

        // 2. Separate catalog into "Preferred" (same category) and "Others"
        Map<Boolean, List<ProductDto>> partitioned = catalog.stream()
            .collect(Collectors.partitioningBy(p -> preferredCategories.contains(p.getCategory())));

        List<ProductDto> preferred = partitioned.getOrDefault(true, new ArrayList<>());
        List<ProductDto> others = partitioned.getOrDefault(false, new ArrayList<>());

        // Shuffle both lists to add variety
        Collections.shuffle(preferred);
        Collections.shuffle(others);

        // 3. Construct result: Fill with preferred, then others
        List<ProductDto> result = new ArrayList<>(preferred);
        
        // If we don't have enough preferred items, fill with others
        if (result.size() < 3) {
            result.addAll(others);
        }

        return result.stream().limit(3).collect(Collectors.toList());
    }

    private String buildPrompt(Map<String, Long> purchased, List<ProductDto> catalog, String mostBoughtName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: You are an expert shopping assistant.\n");
        sb.append("User History (Purchased Items with frequency):\n");
        purchased.forEach((name, count) -> 
            sb.append("- ").append(name).append(" (x").append(count).append(")\n")
        );
        
        if (mostBoughtName != null) {
            sb.append("\nUser's Most Frequently Bought Item: ").append(mostBoughtName).append("\n");
        }

        sb.append("\nAvailable Catalog (ID: Name - Category):\n");
        for (ProductDto p : catalog) {
            sb.append(p.getId()).append(": ").append(p.getName()).append(" - ").append(p.getCategory()).append("\n");
        }
        sb.append("\nTask: Recommend 3 products from the catalog based on the user's purchase history. Heavily weigh items that are frequently bought or complement frequently bought items.\n");
        sb.append("Constraint: The User's Most Frequently Bought Item (" + (mostBoughtName != null ? mostBoughtName : "none") + ") is very important. Consider recommending it explicitly if it fits.\n");
        sb.append("Format: Return ONLY a JSON array of the recommended Product IDs. Example: [1, 5, 10]. Do not include markdown formatting or explanations.");
        return sb.toString();
    }

    private String callGemini(String promptText) {
        int maxRetries = 2; // Total attempts = 1 + retries
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                log.info("Attempting to call Gemini via Spring AI (Attempt {})", attempt + 1);
                
                // Using Spring AI ChatClient as requested
                return chatClient.prompt()
                        .user(promptText)
                        .call()
                        .content();
                        
            } catch (Exception e) {
                boolean isQuotaError = e.getMessage().contains("429") || 
                                       e.getMessage().contains("Too Many Requests") ||
                                       e.getMessage().contains("Quota exceeded");
                
                if (isQuotaError) {
                    if (attempt < maxRetries) {
                        long waitTime = (long) Math.pow(2, attempt) * 1000; // 1s, 2s
                        log.warn("Quota exceeded. Retrying in {}ms...", waitTime);
                        try { Thread.sleep(waitTime); } catch (InterruptedException ignored) {}
                        continue; 
                    } else {
                        log.warn("Quota exceeded after {} retries.", maxRetries);
                    }
                } else {
                    log.warn("Failed to call Gemini: {}", e.getMessage());
                    break; // Break loop to try fallback logic (handled in getRecommendations)
                }
            }
        }
        
        log.error("All Gemini model attempts failed.");
        return "[]";
    }

    private List<ProductDto> parseRecommendations(String jsonResponse, List<ProductDto> catalog) {
        try {
            Pattern arrayPattern = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher = arrayPattern.matcher(jsonResponse);
            
            String idsString = "";
            if (matcher.find()) {
                idsString = matcher.group(1); 
            } else {
                idsString = jsonResponse;
            }

            if (idsString.isBlank()) {
                log.warn("Gemini response contained no IDs. Raw response: {}", jsonResponse);
                return Collections.emptyList();
            }

            Set<Long> ids = Arrays.stream(idsString.split(","))
                    .map(String::trim)
                    .filter(s -> s.matches("\\d+")) 
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
            
            return catalog.stream()
                    .filter(p -> ids.contains(p.getId()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse Gemini/Recommend response: " + jsonResponse, e);
            return Collections.emptyList();
        }
    }

    // ========== SELLER RECOMMENDATIONS METHODS ==========

    /**
     * Get top sold items for seller dashboard
     * Analyzes actual order data to determine most popular products
     * Requires seller context via authorization header
     */
    public List<ProductDto> getTopSoldItems(int limit, String authHeader) {
        List<OrderSummaryDto> orders;
        List<ProductDto> catalog;
        
        try {
            // Get grouped orders where the seller is the product owner
            // This returns one entry per order with product names aggregated
            orders = client.getSellerSalesGrouped(authHeader);
            catalog = client.getProducts();
        } catch (Exception e) {
            log.error("Failed to fetch seller sales or products", e);
            return Collections.emptyList();
        }

        if (orders == null || orders.isEmpty() || catalog.isEmpty()) {
            log.warn("No seller sales or catalog data available");
            return Collections.emptyList();
        }

        // Count sales by product name
        // Each order can have multiple products, so we flatmap to count each product occurrence
        Map<String, Long> salesCount = orders.stream()
                .filter(o -> o.getProductNames() != null)
                .flatMap(o -> o.getProductNames().stream())
                .collect(Collectors.groupingBy(
                    name -> name,  // Group by product name directly
                    Collectors.counting()  // Count occurrences
                ));

        log.info("Seller sales count by product: {}", salesCount);

        // Filter catalog to only seller's products and sort by sales count (descending)
        // Only include products that the seller has actually sold
        return catalog.stream()
                .filter(product -> salesCount.containsKey(product.getName()))  // Only seller's sold products
                .map(product -> {
                    // Get sales count for this product
                    long sales = salesCount.getOrDefault(product.getName(), 0L);
                    return new SimpleEntry<>(product, sales);
                })
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())) // Sort by sales descending
                .map(SimpleEntry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * AI-powered suggestion for seller to add new products
     * Based on top-sold items, suggests complementary products
     */
    public List<String> suggestNewProducts(List<ProductDto> topSoldItems, List<ProductDto> currentCatalog) {
        if (topSoldItems.isEmpty() || currentCatalog.isEmpty()) {
            log.warn("Cannot suggest products: empty input data");
            return fallbackProductSuggestions(topSoldItems);
        }

        // Create cache key from top-sold items
        String cacheKey = topSoldItems.stream()
                .sorted(Comparator.comparing(p -> p.getId()))
                .map(p -> p.getName())
                .collect(Collectors.joining("|"));

        if (sellerSuggestionsCache.containsKey(cacheKey)) {
            log.info("Returning cached product suggestions for key: {}", cacheKey);
            return sellerSuggestionsCache.get(cacheKey);
        }

        // Build prompt for AI
        String prompt = buildSellerSuggestionsPrompt(topSoldItems, currentCatalog);

        // Call Gemini
        String aiResponse = callGemini(prompt);
        log.info("Gemini Seller Suggestions Response: {}", aiResponse);

        // Parse response
        List<String> suggestions = parseSellerSuggestions(aiResponse);

        if (suggestions.isEmpty()) {
            log.warn("Gemini returned no suggestions. Using fallback.");
            suggestions = fallbackProductSuggestions(topSoldItems);
        }

        // Cache successful suggestions
        sellerSuggestionsCache.put(cacheKey, suggestions);
        return suggestions;
    }

    /**
     * Build prompt for seller product suggestions
     */
    private String buildSellerSuggestionsPrompt(List<ProductDto> topSoldItems, List<ProductDto> catalog) {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: You are a retail business expert and product strategist.\n");
        sb.append("Your seller's top-selling items (by sales volume):\n");
        topSoldItems.forEach(item -> 
            sb.append("- ").append(item.getName()).append(" (Category: ").append(item.getCategory()).append(")\n")
        );

        sb.append("\nCurrent inventory:\n");
        catalog.forEach(p -> 
            sb.append("- ").append(p.getName()).append(" (Category: ").append(p.getCategory()).append(")\n")
        );

        sb.append("\nTask: Suggest 3-5 NEW product names (NOT in current inventory) that would complement the seller's top-selling items.\n");
        sb.append("Consider:\n");
        sb.append("1. Products that pair well with current best sellers (e.g., if selling beef, suggest marinades, seasonings)\n");
        sb.append("2. Products within the same category that aren't yet stocked\n");
        sb.append("3. Complementary items from adjacent categories\n");
        sb.append("Format: Return ONLY a JSON array of product name suggestions. Example: [\"Product A\", \"Product B\", \"Product C\"]. Do not include markdown formatting or explanations.");
        return sb.toString();
    }

    /**
     * Parse seller suggestions from AI response
     */
    private List<String> parseSellerSuggestions(String jsonResponse) {
        try {
            Pattern arrayPattern = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL);
            Matcher matcher = arrayPattern.matcher(jsonResponse);

            if (!matcher.find()) {
                log.warn("No JSON array found in response: {}", jsonResponse);
                return Collections.emptyList();
            }

            String arrayContent = matcher.group(1);
            String[] items = arrayContent.split(",");

            return Arrays.stream(items)
                    .map(String::trim)
                    .map(s -> s.replaceAll("^\"|\"$", "")) // Remove quotes
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse seller suggestions: " + jsonResponse, e);
            return Collections.emptyList();
        }
    }

    /**
     * Fallback: Suggest products based on categories of top-sold items
     */
    private List<String> fallbackProductSuggestions(List<ProductDto> topSoldItems) {
        if (topSoldItems.isEmpty()) {
            return Arrays.asList(
                "Organic Salt",
                "Premium Olive Oil",
                "Fresh Herbs Bundle"
            );
        }

        // Extract categories and suggest related items
        Set<String> categories = topSoldItems.stream()
                .map(ProductDto::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> fallbackSuggestions = new ArrayList<>();

        for (String category : categories) {
            if (category.contains("PRODUCE")) {
                fallbackSuggestions.addAll(Arrays.asList("Organic Vegetable Seeds", "Composting Kit"));
            } else if (category.contains("MEAT")) {
                fallbackSuggestions.addAll(Arrays.asList("Specialty Marinades", "Grilling Seasonings", "Premium Sea Salt"));
            } else if (category.contains("DAIRY")) {
                fallbackSuggestions.addAll(Arrays.asList("Artisanal Butter", "Specialty Cheese Selections"));
            }
        }

        return fallbackSuggestions.isEmpty() 
            ? Arrays.asList("Organic Spice Mix", "Gourmet Condiments", "Specialty Oils")
            : fallbackSuggestions.stream().distinct().limit(5).collect(Collectors.toList());
    }
}
