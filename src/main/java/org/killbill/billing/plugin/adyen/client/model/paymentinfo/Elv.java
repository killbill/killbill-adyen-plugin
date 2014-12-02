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

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;

public class Elv extends PaymentInfo {

    private String elvBlz;
    private String elvKontoNummer;
    private String elvBankName;
    private String elvAccountHolder;

    public Elv(final PaymentProvider paymentProvider) {
        super(paymentProvider);
    }

    public String getElvBankName() {
        return elvBankName;
    }

    public void setElvBankName(final String elvBankName) {
        this.elvBankName = elvBankName;
    }

    public String getElvBlz() {
        return elvBlz;
    }

    public void setElvBlz(final String elvBlz) {
        if (elvBlz == null) {
            this.elvBlz = null;
        } else {
            this.elvBlz = elvBlz.replaceAll("\\s", "");
        }
    }

    public String getElvKontoNummer() {
        return elvKontoNummer;
    }

    public void setElvKontoNummer(final String elvKontoNummer) {
        if (elvKontoNummer == null) {
            this.elvKontoNummer = null;
        } else {
            this.elvKontoNummer = elvKontoNummer.replaceAll("\\s", "");
        }
    }

    public String getElvAccountHolder() {
        return elvAccountHolder;
    }

    public void setElvAccountHolder(final String elvAccountHolder) {
        this.elvAccountHolder = elvAccountHolder;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Elv{");
        sb.append("elvBlz='").append(elvBlz).append('\'');
        sb.append(", elvKontoNummer='").append(elvKontoNummer).append('\'');
        sb.append(", elvBankName='").append(elvBankName).append('\'');
        sb.append(", elvAccountHolder='").append(elvAccountHolder).append('\'');
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

        final Elv elv = (Elv) o;

        if (elvAccountHolder != null ? !elvAccountHolder.equals(elv.elvAccountHolder) : elv.elvAccountHolder != null) {
            return false;
        }
        if (elvBankName != null ? !elvBankName.equals(elv.elvBankName) : elv.elvBankName != null) {
            return false;
        }
        if (elvBlz != null ? !elvBlz.equals(elv.elvBlz) : elv.elvBlz != null) {
            return false;
        }
        if (elvKontoNummer != null ? !elvKontoNummer.equals(elv.elvKontoNummer) : elv.elvKontoNummer != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = elvBlz != null ? elvBlz.hashCode() : 0;
        result = 31 * result + (elvKontoNummer != null ? elvKontoNummer.hashCode() : 0);
        result = 31 * result + (elvBankName != null ? elvBankName.hashCode() : 0);
        result = 31 * result + (elvAccountHolder != null ? elvAccountHolder.hashCode() : 0);
        return result;
    }
}
