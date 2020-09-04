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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;

public class Card extends PaymentInfo {

    private String holderName;
    private String number;
    private String cvc;
    private Integer expiryMonth;
    private Integer expiryYear;
    // Special fields
    private String issuerCountry;
    private String token;
    private String encryptedJson;

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(final String holderName) {
        this.holderName = holderName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(final String cvc) {
        this.cvc = cvc;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(final Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(final Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getIssuerCountry() {
        return issuerCountry;
    }

    public void setIssuerCountry(final String issuerCountry) {
        this.issuerCountry = issuerCountry;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getEncryptedJson() {
        return encryptedJson;
    }

    public void setEncryptedJson(final String encryptedJson) {
        this.encryptedJson = encryptedJson;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Card{");
        sb.append("holderName='").append(holderName).append('\'');
        sb.append(", number='").append(number).append('\'');
        sb.append(", cvc='").append(cvc).append('\'');
        sb.append(", expiryMonth=").append(expiryMonth);
        sb.append(", expiryYear=").append(expiryYear);
        sb.append(", issuerCountry='").append(issuerCountry).append('\'');
        sb.append(", token='").append(token).append('\'');
        sb.append(", encryptedJson='").append(encryptedJson).append('\'');
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

        final Card card = (Card) o;

        if (holderName != null ? !holderName.equals(card.holderName) : card.holderName != null) {
            return false;
        }
        if (number != null ? !number.equals(card.number) : card.number != null) {
            return false;
        }
        if (cvc != null ? !cvc.equals(card.cvc) : card.cvc != null) {
            return false;
        }
        if (expiryMonth != null ? !expiryMonth.equals(card.expiryMonth) : card.expiryMonth != null) {
            return false;
        }
        if (expiryYear != null ? !expiryYear.equals(card.expiryYear) : card.expiryYear != null) {
            return false;
        }
        if (issuerCountry != null ? !issuerCountry.equals(card.issuerCountry) : card.issuerCountry != null) {
            return false;
        }
        if (token != null ? !token.equals(card.token) : card.token != null) {
            return false;
        }
        return encryptedJson != null ? encryptedJson.equals(card.encryptedJson) : card.encryptedJson == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (holderName != null ? holderName.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (cvc != null ? cvc.hashCode() : 0);
        result = 31 * result + (expiryMonth != null ? expiryMonth.hashCode() : 0);
        result = 31 * result + (expiryYear != null ? expiryYear.hashCode() : 0);
        result = 31 * result + (issuerCountry != null ? issuerCountry.hashCode() : 0);
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + (encryptedJson != null ? encryptedJson.hashCode() : 0);
        return result;
    }
}
