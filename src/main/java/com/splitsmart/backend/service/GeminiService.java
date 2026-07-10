package com.splitsmart.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitsmart.backend.dto.ParsedExpense;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ParsedExpense> parseExpenses(String text) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + apiKey;        String prompt = "Extract expenses from this text and return ONLY a JSON array, no other text, no markdown formatting:\n"
                + "\"" + text + "\"\n\n"
                + "Format: [{\"name\": \"person name\", \"amount\": number}]\n"
                + "Example: [{\"name\": \"Rahul\", \"amount\": 500}, {\"name\": \"Mini\", \"amount\": 300}]";

        String requestBody = objectMapper.writeValueAsString(
                new GeminiRequest(prompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Gemini raw response: " + response.body());

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

        String cleaned = rawText.replace("```json", "").replace("```", "").trim();

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

    private static class GeminiRequest {
        public List<Content> contents;

        public GeminiRequest(String prompt) {
            this.contents = List.of(new Content(prompt));
        }
    }

    private static class Content {
        public List<Part> parts;

        public Content(String text) {
            this.parts = List.of(new Part(text));
        }
    }

    private static class Part {
        public String text;

        public Part(String text) {
            this.text = text;
        }
    }
}
