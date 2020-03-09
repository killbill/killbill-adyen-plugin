package org.killbill.billing.plugin.adyen.api.mapping.klarna;

//TODO: Not needed
public class LineItem {
    private String id;
    private Long quantity;
    private String description;
    private Long taxAmount;
    private Long taxPercentage;
    private Long amountExcludingTax;
    private Long amountIncludingTax;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public Long getQuantity() {
        return quantity;
    }
    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTaxAmount() {
        return taxAmount;
    }
    public void setTaxAmount(Long taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Long getTaxPercentage() {
        return taxPercentage;
    }
    public void setTaxPercentage(Long taxPercentage) {
        this.taxPercentage = taxPercentage;
    }

    public Long getAmountExcludingTax() {
        return amountExcludingTax;
    }
    public void setAmountExcludingTax(Long amountExcludingTax) {
        this.amountExcludingTax = amountExcludingTax;
    }

    public Long getAmountIncludingTax() {
        return amountIncludingTax;
    }
    public void setAmountIncludingTax(Long amountIncludingTax) {
        this.amountIncludingTax = amountIncludingTax;
    }
}
