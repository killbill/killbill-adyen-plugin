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

import java.util.Calendar;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;

public class MaestroUK extends CreditCard {

    private String ccIssueNumber;
    private Integer validStartMonth;
    private Integer validStartYear;

    public MaestroUK(final PaymentProvider paymentProvider) {
        super(paymentProvider);
    }

    public String getCcIssueNumber() {
        return ccIssueNumber;
    }

    public void setCcIssueNumber(final String ccIssueNumber) {
        if (ccIssueNumber == null) {
            this.ccIssueNumber = null;
        } else {
            this.ccIssueNumber = ccIssueNumber.replaceAll("\\s", "");
        }
    }

    public Integer getValidStartMonth() {
        return validStartMonth;
    }

    public void setValidStartMonth(final Integer validStartMonth) {
        this.validStartMonth = validStartMonth;
    }

    public Integer getValidStartYear() {
        return validStartYear;
    }

    public void setValidStartYear(final Integer validStartYear) {
        this.validStartYear = validStartYear;
    }

    public Calendar getValidStartDate() {
        if (validStartMonth == null || validStartYear == null) {
            return null;
        }
        return new DateTime(validStartYear, validStartMonth, 1, 23, 59, 59)
                .dayOfMonth()
                .withMaximumValue()
                .toGregorianCalendar();
    }

    public String getValidCCNoSizes() {
        return "16,18,19";
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MaestroUK{");
        sb.append("ccIssueNumber='").append(ccIssueNumber).append('\'');
        sb.append(", validStartMonth=").append(validStartMonth);
        sb.append(", validStartYear=").append(validStartYear);
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

        final MaestroUK maestroUK = (MaestroUK) o;

        if (ccIssueNumber != null ? !ccIssueNumber.equals(maestroUK.ccIssueNumber) : maestroUK.ccIssueNumber != null) {
            return false;
        }
        if (validStartMonth != null ? !validStartMonth.equals(maestroUK.validStartMonth) : maestroUK.validStartMonth != null) {
            return false;
        }
        return !(validStartYear != null ? !validStartYear.equals(maestroUK.validStartYear) : maestroUK.validStartYear != null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (ccIssueNumber != null ? ccIssueNumber.hashCode() : 0);
        result = 31 * result + (validStartMonth != null ? validStartMonth.hashCode() : 0);
        result = 31 * result + (validStartYear != null ? validStartYear.hashCode() : 0);
        return result;
    }
}
