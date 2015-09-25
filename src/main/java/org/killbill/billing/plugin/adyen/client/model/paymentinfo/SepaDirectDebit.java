/*
 * Copyright 2015 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;

public class SepaDirectDebit extends PaymentInfo {

    public static final String SELECTED_BRAND = "sepadirectdebit";

    private String countryCode;
    private String iban;
    private String bic;
    private String sepaAccountHolder;

    public SepaDirectDebit(final PaymentProvider paymentProvider) {
        super(paymentProvider);
    }

    public String getBic() {
        return bic;
    }

    public void setBic(final String bic) {
        this.bic = bic;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        if (countryCode == null) {
            this.countryCode = null;
        } else {
            this.countryCode = countryCode.replaceAll("\\s", "");
        }
    }

    public String getIban() {
        return iban;
    }

    public void setIban(final String iban) {
        if (iban == null) {
            this.iban = null;
        } else {
            this.iban = iban.replaceAll("\\s", "");
        }
    }

    public String getSepaAccountHolder() {
        return sepaAccountHolder;
    }

    public void setSepaAccountHolder(final String sepaAccountHolder) {
        this.sepaAccountHolder = sepaAccountHolder;
    }

    @Override
    public String toString() {
        return "SepaDirectDebit{" +
                "countryCode='" + countryCode + '\'' +
                ", iban='" + iban + '\'' +
                ", bic='" + bic + '\'' +
                ", sepaAccountHolder='" + sepaAccountHolder + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SepaDirectDebit sepaDirectDebit = (SepaDirectDebit) o;

        if (sepaAccountHolder != null ? !sepaAccountHolder.equals(sepaDirectDebit.sepaAccountHolder) : sepaDirectDebit.sepaAccountHolder != null) {
            return false;
        }
        if (bic != null ? !bic.equals(sepaDirectDebit.bic) : sepaDirectDebit.bic != null) {
            return false;
        }
        if (countryCode != null ? !countryCode.equals(sepaDirectDebit.countryCode) : sepaDirectDebit.countryCode != null) {
            return false;
        }
        if (iban != null ? !iban.equals(sepaDirectDebit.iban) : sepaDirectDebit.iban != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = countryCode != null ? countryCode.hashCode() : 0;
        result = 31 * result + (iban != null ? iban.hashCode() : 0);
        result = 31 * result + (bic != null ? bic.hashCode() : 0);
        result = 31 * result + (sepaAccountHolder != null ? sepaAccountHolder.hashCode() : 0);
        return result;
    }
}
