package com.splitsmart.backend.dto;

public class ParsedExpense {
    private String name;
    private Double amount;

    public ParsedExpense() {
    }

    public ParsedExpense(String name, Double amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}