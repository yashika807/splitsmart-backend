package com.splitsmart.backend.controller;

import com.splitsmart.backend.dto.ParsedExpense;
import com.splitsmart.backend.dto.ReceiptParseResult;
import com.splitsmart.backend.exception.ReceiptParseException;
import com.splitsmart.backend.model.Expense;
import com.splitsmart.backend.repository.ExpenseRepository;
import com.splitsmart.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {
    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private GeminiService geminiService;

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    @PostMapping
    public Expense addExpense(@RequestBody Expense expense) {
        return expenseRepository.save(expense);
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
    }

    @PostMapping("/parse")
    public List<ParsedExpense> parseExpenses(@RequestBody Map<String, String> body) throws Exception {
        String text = body.get("text");
        return geminiService.parseExpenses(text);
    }

    @PostMapping("/parse-receipt")
    public ResponseEntity<?> parseReceipt(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded."));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload an image file."));
        }

        try {
            ReceiptParseResult result = geminiService.parseReceipt(file.getBytes(), contentType);
            return ResponseEntity.ok(result);
        } catch (ReceiptParseException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Something went wrong reading that receipt. Try again?"));
        }
    }
}