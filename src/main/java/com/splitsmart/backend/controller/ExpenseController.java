package com.splitsmart.backend.controller;

import com.splitsmart.backend.dto.ParsedExpense;
import com.splitsmart.backend.model.Expense;
import com.splitsmart.backend.repository.ExpenseRepository;
import com.splitsmart.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}