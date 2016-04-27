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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import java.util.Calendar;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;

public class Card extends PaymentInfo {

    private String ccHolderName;
    private String ccNumber;
    private String ccSecCode;
    private Integer validUntilMonth;
    private Integer validUntilYear;
    private String acceptHeader;
    private String userAgent;
    private String md;
    private String paRes;
    private String acquirer;

    public Card(final PaymentProvider paymentProvider) {
        super(paymentProvider);
    }

    public String getCcNumber() {
        return ccNumber;
    }

    public void setCcNumber(final String ccNumber) {
        if (ccNumber == null) {
            this.ccNumber = null;
        } else {
            this.ccNumber = ccNumber.replaceAll("\\s", "");
        }
    }

    public Integer getValidUntilMonth() {
        return validUntilMonth;
    }

    public void setValidUntilMonth(final Integer validUntilMonth) {
        this.validUntilMonth = validUntilMonth;
    }

    public Integer getValidUntilYear() {
        return validUntilYear;
    }

    public void setValidUntilYear(final Integer validUntilYear) {
        this.validUntilYear = validUntilYear;
    }

    public String getCcHolderName() {
        return ccHolderName;
    }

    public void setCcHolderName(final String ccHolderName) {
        this.ccHolderName = ccHolderName;
    }

    public String getCcSecCode() {
        return ccSecCode;
    }

    public void setCcSecCode(final String ccSecCode) {
        this.ccSecCode = ccSecCode;
    }

    public String getMd() {
        return md;
    }

    public void setMd(final String md) {
        this.md = md;
    }

    public String getPaRes() {
        return paRes;
    }

    public void setPaRes(final String paRes) {
        this.paRes = paRes;
    }

    public void setAcceptHeader(final String acceptHeader) {
        this.acceptHeader = acceptHeader;
    }

    public String getAcceptHeader() {
        return acceptHeader;
    }

    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getAcquirer() {
        return acquirer;
    }

    public void setAcquirer(final String acquirer) {
        this.acquirer = acquirer;
    }

    public Calendar getValidUntilDate() {
        if (validUntilMonth == null || validUntilYear == null) {
            return null;
        }
        return new DateTime(validUntilYear, validUntilMonth, 1, 23, 59, 59)
                .dayOfMonth()
                .withMaximumValue()
                .toGregorianCalendar();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Card{");
        sb.append("ccHolderName='").append(ccHolderName).append('\'');
        sb.append(", ccNumber='").append(ccNumber).append('\'');
        sb.append(", ccSecCode='").append(ccSecCode).append('\'');
        sb.append(", validUntilMonth=").append(validUntilMonth);
        sb.append(", validUntilYear=").append(validUntilYear);
        sb.append(", acceptHeader='").append(acceptHeader).append('\'');
        sb.append(", userAgent='").append(userAgent).append('\'');
        sb.append(", md='").append(md).append('\'');
        sb.append(", paRes='").append(paRes).append('\'');
        sb.append(", acquirer='").append(acquirer).append('\'');
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

        final Card card = (Card) o;

        if (acceptHeader != null ? !acceptHeader.equals(card.acceptHeader) : card.acceptHeader != null) {
            return false;
        }
        if (acquirer != null ? !acquirer.equals(card.acquirer) : card.acquirer != null) {
            return false;
        }
        if (ccHolderName != null ? !ccHolderName.equals(card.ccHolderName) : card.ccHolderName != null) {
            return false;
        }
        if (ccNumber != null ? !ccNumber.equals(card.ccNumber) : card.ccNumber != null) {
            return false;
        }
        if (ccSecCode != null ? !ccSecCode.equals(card.ccSecCode) : card.ccSecCode != null) {
            return false;
        }
        if (md != null ? !md.equals(card.md) : card.md != null) {
            return false;
        }
        if (paRes != null ? !paRes.equals(card.paRes) : card.paRes != null) {
            return false;
        }
        if (userAgent != null ? !userAgent.equals(card.userAgent) : card.userAgent != null) {
            return false;
        }
        if (validUntilMonth != null ? !validUntilMonth.equals(card.validUntilMonth) : card.validUntilMonth != null) {
            return false;
        }
        if (validUntilYear != null ? !validUntilYear.equals(card.validUntilYear) : card.validUntilYear != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = ccHolderName != null ? ccHolderName.hashCode() : 0;
        result = 31 * result + (ccNumber != null ? ccNumber.hashCode() : 0);
        result = 31 * result + (ccSecCode != null ? ccSecCode.hashCode() : 0);
        result = 31 * result + (validUntilMonth != null ? validUntilMonth.hashCode() : 0);
        result = 31 * result + (validUntilYear != null ? validUntilYear.hashCode() : 0);
        result = 31 * result + (acceptHeader != null ? acceptHeader.hashCode() : 0);
        result = 31 * result + (userAgent != null ? userAgent.hashCode() : 0);
        result = 31 * result + (md != null ? md.hashCode() : 0);
        result = 31 * result + (paRes != null ? paRes.hashCode() : 0);
        result = 31 * result + (acquirer != null ? acquirer.hashCode() : 0);
        return result;
    }
}
