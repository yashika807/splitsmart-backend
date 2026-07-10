package com.splitsmart.backend.dto;

import java.util.List;

public class ReceiptParseResult {
    private List<ReceiptItem> items;
    private Double subtotal;
    private Double tax;
    private Double tip;

    public ReceiptParseResult() {
    }

    public ReceiptParseResult(List<ReceiptItem> items, Double subtotal, Double tax, Double tip) {
        this.items = items;
        this.subtotal = subtotal;
        this.tax = tax;
        this.tip = tip;
    }

    public List<ReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItem> items) {
        this.items = items;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getTip() {
        return tip;
    }

    public void setTip(Double tip) {
        this.tip = tip;
    }
}
