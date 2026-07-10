package com.splitsmart.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitsmart.backend.dto.ParsedExpense;
import com.splitsmart.backend.dto.ReceiptItem;
import com.splitsmart.backend.dto.ReceiptParseResult;
import com.splitsmart.backend.exception.ReceiptParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ParsedExpense> parseExpenses(String text) throws Exception {
        String prompt = "Extract expenses from this text and return ONLY a JSON array, no other text, no markdown formatting:\n"
                + "\"" + text + "\"\n\n"
                + "Format: [{\"name\": \"person name\", \"amount\": number}]\n"
                + "Example: [{\"name\": \"Rahul\", \"amount\": 500}, {\"name\": \"Mini\", \"amount\": 300}]";

        String cleaned = callGemini(List.of(new Part(prompt)));

        List<ParsedExpense> expenses = new ArrayList<>();
        JsonNode expenseArray = objectMapper.readTree(cleaned);
        for (JsonNode node : expenseArray) {
            expenses.add(new ParsedExpense(
                    node.get("name").asText(),
                    node.get("amount").asDouble()
            ));
        }

        return expenses;
    }

    public ReceiptParseResult parseReceipt(byte[] imageBytes, String mimeType) throws Exception {
        String prompt = "You are reading a photo of a receipt. Extract every line item with its "
                + "name, unit price (price for ONE unit, not price times quantity), and quantity. "
                + "Also read the subtotal, tax, and tip if they appear on the receipt.\n"
                + "Return ONLY JSON, no markdown formatting, no extra text, in exactly this shape:\n"
                + "{\"items\": [{\"itemName\": string, \"price\": number, \"quantity\": integer}], "
                + "\"subtotal\": number or null, \"tax\": number or null, \"tip\": number or null}\n"
                + "If a value isn't visible on the receipt, use null instead of guessing. "
                + "If you cannot read the receipt at all, return {\"items\": [], \"subtotal\": null, \"tax\": null, \"tip\": null}.";

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String cleaned;
        try {
            cleaned = callGemini(List.of(
                    new Part(prompt),
                    new Part(new InlineData(mimeType, base64Image))
            ));
        } catch (RuntimeException e) {
            throw new ReceiptParseException("The AI couldn't process that receipt image. Try again?");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new ReceiptParseException("Couldn't make sense of that receipt. Try a clearer photo?");
        }

        List<ReceiptItem> items = new ArrayList<>();
        if (root.has("items") && root.get("items").isArray()) {
            for (JsonNode node : root.get("items")) {
                if (!node.hasNonNull("itemName") || !node.hasNonNull("price")) {
                    continue;
                }
                items.add(new ReceiptItem(
                        node.get("itemName").asText(),
                        node.get("price").asDouble(),
                        node.path("quantity").isMissingNode() || node.get("quantity").isNull()
                                ? 1
                                : node.get("quantity").asInt(1)
                ));
            }
        }

        if (items.isEmpty()) {
            throw new ReceiptParseException("Couldn't find any line items on that receipt. Try a clearer, well-lit photo.");
        }

        Double subtotal = root.hasNonNull("subtotal") ? root.get("subtotal").asDouble() : null;
        Double tax = root.hasNonNull("tax") ? root.get("tax").asDouble() : null;
        Double tip = root.hasNonNull("tip") ? root.get("tip").asDouble() : null;

        return new ReceiptParseResult(items, subtotal, tax, tip);
    }

    private String callGemini(List<Part> parts) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + apiKey;

        try {
            String requestBody = objectMapper.writeValueAsString(new GeminiRequest(parts));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.has("candidates")) {
                throw new RuntimeException("Gemini API error: " + response.body());
            }

            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            return rawText.replace("```json", "").replace("```", "").trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }

    private static class GeminiRequest {
        public List<Content> contents;

        public GeminiRequest(List<Part> parts) {
            this.contents = List.of(new Content(parts));
        }
    }

    private static class Content {
        public List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Part {
        public String text;

        @JsonProperty("inline_data")
        public InlineData inlineData;

        public Part(String text) {
            this.text = text;
        }

        public Part(InlineData inlineData) {
            this.inlineData = inlineData;
        }
    }

    private static class InlineData {
        @JsonProperty("mime_type")
        public String mimeType;
        public String data;

        public InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }
    }
}
