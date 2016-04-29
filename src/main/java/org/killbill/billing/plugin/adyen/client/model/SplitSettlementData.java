/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.adyen.client.model;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class SplitSettlementData {

    private final int api;
    private final String currencyCode;
    private final List<Item> items;

    public SplitSettlementData(final int api, final String currencyCode, final List<Item> items) {
        this.api = api;
        this.currencyCode = currencyCode;
        this.items = items;
    }

    public int getApi() {
        return api;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public List<Item> getItems() {
        return ImmutableList.<Item>copyOf(items);
    }

    public long getTotalAmount() {
        long totalAmount = 0;
        for (final Item item : items) {
            totalAmount += item.getAmount();
        }
        return totalAmount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SplitSettlementData{");
        sb.append("api=").append(api);
        sb.append(", currencyCode='").append(currencyCode).append('\'');
        sb.append(", items=").append(items);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SplitSettlementData that = (SplitSettlementData) o;

        if (api != that.api) {
            return false;
        }
        if (currencyCode != null ? !currencyCode.equals(that.currencyCode) : that.currencyCode != null) {
            return false;
        }
        if (items != null ? !items.equals(that.items) : that.items != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = api;
        result = 31 * result + (currencyCode != null ? currencyCode.hashCode() : 0);
        result = 31 * result + (items != null ? items.hashCode() : 0);
        return result;
    }

    public static class Item {

        private final long amount;
        private final String group;
        private final String reference;
        private final String type;

        public Item(final long amount, final String group, final String reference, final String type) {
            this.amount = amount;
            this.group = group;
            this.reference = reference;
            this.type = type;
        }

        public long getAmount() {
            return amount;
        }

        public String getGroup() {
            return group;
        }

        public String getReference() {
            return reference;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Item{");
            sb.append("amount=").append(amount);
            sb.append(", group='").append(group).append('\'');
            sb.append(", reference='").append(reference).append('\'');
            sb.append(", type='").append(type).append('\'');
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Item item = (Item) o;

            if (amount != item.amount) {
                return false;
            }
            if (group != null ? !group.equals(item.group) : item.group != null) {
                return false;
            }
            if (reference != null ? !reference.equals(item.reference) : item.reference != null) {
                return false;
            }
            if (type != null ? !type.equals(item.type) : item.type != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (amount ^ (amount >>> 32));
            result = 31 * result + (group != null ? group.hashCode() : 0);
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

    public static class Builder {

        private final List<Item> items = new ArrayList<Item>();
        private final int api;
        private final String currencyCode;

        private Builder(final int api, final String currencyCode) {
            this.api = api;
            this.currencyCode = currencyCode;
        }

        public Builder withItem(final Item item) {
            items.add(item);
            return this;
        }

        public SplitSettlementData build() {
            return new SplitSettlementData(api, currencyCode, items);
        }
    }
}
