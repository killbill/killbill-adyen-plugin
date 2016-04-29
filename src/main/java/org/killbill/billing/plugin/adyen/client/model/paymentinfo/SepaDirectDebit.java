/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;

public class SepaDirectDebit extends PaymentInfo {

    private String countryCode;
    private String iban;
    private String bic;
    private String sepaAccountHolder;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(final String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(final String bic) {
        this.bic = bic;
    }

    public String getSepaAccountHolder() {
        return sepaAccountHolder;
    }

    public void setSepaAccountHolder(final String sepaAccountHolder) {
        this.sepaAccountHolder = sepaAccountHolder;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SepaDirectDebit{");
        sb.append("countryCode='").append(countryCode).append('\'');
        sb.append(", iban='").append(iban).append('\'');
        sb.append(", bic='").append(bic).append('\'');
        sb.append(", sepaAccountHolder='").append(sepaAccountHolder).append('\'');
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
        if (!super.equals(o)) {
            return false;
        }

        final SepaDirectDebit that = (SepaDirectDebit) o;

        if (countryCode != null ? !countryCode.equals(that.countryCode) : that.countryCode != null) {
            return false;
        }
        if (iban != null ? !iban.equals(that.iban) : that.iban != null) {
            return false;
        }
        if (bic != null ? !bic.equals(that.bic) : that.bic != null) {
            return false;
        }
        return sepaAccountHolder != null ? sepaAccountHolder.equals(that.sepaAccountHolder) : that.sepaAccountHolder == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (countryCode != null ? countryCode.hashCode() : 0);
        result = 31 * result + (iban != null ? iban.hashCode() : 0);
        result = 31 * result + (bic != null ? bic.hashCode() : 0);
        result = 31 * result + (sepaAccountHolder != null ? sepaAccountHolder.hashCode() : 0);
        return result;
    }
}
