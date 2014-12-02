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

public enum PaymentServiceProvider {

    MOCK(-1, "mock", ""),
    NO_CHARGE_PROVIDER(0, "noCharge", ""),
    ADYEN(1, "adyen", ""),
    FINANSBANK(2, "finansbank", ""),
    ALLPAGO(3, "allpago", ""),
    GLOBAL_COLLECT(4, "globalCollect", ""),
    OFFLINE(5, "offlineFundsTransfer", ""),
    ALIPAY_PSP(6, "alipay", ""),
    TENPAY_PSP(7, "tenpay", ""),
    SAFESHOP(8, "safeShop", ""),
    TRANZILA(9, "tranzila", ""),
    ADYEN_DINERO(10, "adyen", "dinero"),
    ADYEN_ISRACARD(11, "adyen", "isracard"),
    ARAMEX(12, "aramex", ""),
    MULTIPLUS(13, "multiplus", "");

    private final Integer id;
    private final String name;
    private final String subBrand;

    private PaymentServiceProvider(final Integer id, final String name, final String subBrand) {
        this.id = id;
        this.name = name;
        this.subBrand = subBrand;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubBrand() {
        return this.subBrand;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentServiceProvider{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", subBrand='").append(subBrand).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
