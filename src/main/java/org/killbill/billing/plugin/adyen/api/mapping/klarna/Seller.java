package org.killbill.billing.plugin.adyen.api.mapping.klarna;

public class Seller {
    private String product_name;
    private String product_category;
    private String sub_merchant_id;

    public String getProduct_name() {
        return product_name;
    }
    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public String getProduct_category() {
        return product_category;
    }
    public void setProduct_category(String product_category) {
        this.product_category = product_category;
    }

    public String getSub_merchant_id() {
        return sub_merchant_id;
    }
    public void setSub_merchant_id(String sub_merchant_id) {
        this.sub_merchant_id = sub_merchant_id;
    }
}

