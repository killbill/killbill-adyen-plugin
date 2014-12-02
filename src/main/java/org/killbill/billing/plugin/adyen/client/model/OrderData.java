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

import org.joda.time.DateTime;

/**
 * Encapsulates some obscure data like <code>holderName</code> and <code>shipBeforeDate</code>
 * we need for payment service providers.
 */
public class OrderData {

    private DateTime shipBeforeDate;
    private String holderName;

    public DateTime getShipBeforeDate() {
        return shipBeforeDate;
    }

    public void setShipBeforeDate(final DateTime shipBeforeDate) {
        this.shipBeforeDate = shipBeforeDate;
    }

    /**
     * Usually describes the credit card holder name. That is <code>firstname</code>[space]<code>lastname</code>.
     *
     * @return holder name
     */
    public String getHolderName() {
        return holderName;
    }

    /**
     * @param holderName holder name (<code>firstname[space]lastname</code>)
     * @see #getHolderName()
     */
    public void setHolderName(final String holderName) {
        this.holderName = holderName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OrderData{");
        sb.append("shipBeforeDate=").append(shipBeforeDate);
        sb.append(", holderName='").append(holderName).append('\'');
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

        final OrderData orderData = (OrderData) o;

        if (holderName != null ? !holderName.equals(orderData.holderName) : orderData.holderName != null) {
            return false;
        }
        if (shipBeforeDate != null ? !shipBeforeDate.equals(orderData.shipBeforeDate) : orderData.shipBeforeDate != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = shipBeforeDate != null ? shipBeforeDate.hashCode() : 0;
        result = 31 * result + (holderName != null ? holderName.hashCode() : 0);
        return result;
    }
}
