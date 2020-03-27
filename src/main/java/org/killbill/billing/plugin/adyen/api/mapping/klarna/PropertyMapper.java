/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.api.mapping.klarna;

import org.jooq.tools.StringUtils;

public abstract class PropertyMapper {

    public static class Address {
        private String address1;
        private String address2;
        private String city;
        private String state;
        private String country;
        private String postalCode;

        public String getAddress1() {
            return address1;
        }

        public void setAddress1(final String address1) {
            this.address1 = address1;
        }

        public String getAddress2() {
            return address2;
        }

        public void setAddress2(final String address2) {
            this.address2 = address2;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(final String state) {
            this.state = state;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(final String country) {
            this.country = country;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(final String postalCode) {
            this.postalCode = postalCode;
        }
    }

    public static class LineItem {
        private String id;
        private String description;
        private Long quantity;
        private Long taxAmount;
        private Long taxPercentage;
        private Long amountExcludingTax;
        private Long amountIncludingTax;

        // fields included for merchant data
        private String inventoryService;
        private String productName;
        private String productCategory;
        private String merchantId;
        private String merchantName;

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }

        public Long getQuantity() { return quantity; }
        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }

        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getProductCategory() { return productCategory; }
        public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

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

        public String getInventoryService() { return inventoryService; }
        public void setInventoryService(String inventoryService) { this.inventoryService = inventoryService; }

        public boolean isVoucher() {
            boolean itemIsVoucher = true;
            if(!StringUtils.isEmpty(inventoryService) && inventoryService.toLowerCase().equals("goods")) {
                itemIsVoucher = false;
            }

            return itemIsVoucher;
        }
    }

    public static class CustomerAccount {
        private String accountId;
        private String registrationDate;
        private String lastModifiedDate;

        public String getAccountId() {
            return accountId;
        }
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getRegistrationDate() {
            return registrationDate;
        }
        public void setRegistrationDate(String registrationDate) {
            this.registrationDate = registrationDate;
        }

        public String getLastModifiedDate() {
            return lastModifiedDate;
        }
        public void setLastModifiedDate(String lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
        }
    }
}
