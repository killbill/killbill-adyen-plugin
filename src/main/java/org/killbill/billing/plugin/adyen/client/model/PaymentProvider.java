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
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

public class PaymentProvider {

    private static final Long DEFAULT_THREE_D_THRESHOLD = Long.MAX_VALUE;

    private static final Joiner JOINER = Joiner.on(',');

    private final AdyenConfigProperties configuration;

    private PaymentServiceProvider paymentServiceProvider;
    private RecurringType recurringType;
    private String hppTargetOverride;
    private String hppVariantOverride;
    private PaymentType paymentType;
    private String countryIsoCode;
    private Currency currency;
    private Integer threeDThresholdOriginal;
    private List<Acquirer> acquirers;
    private Acquirer defaultAcquirer;

    public PaymentProvider(final AdyenConfigProperties configuration) {
        this.configuration = configuration;
    }

    public PaymentServiceProvider getPaymentServiceProvider() {
        return paymentServiceProvider;
    }

    public void setPaymentServiceProvider(final PaymentServiceProvider paymentServiceProvider) {
        this.paymentServiceProvider = paymentServiceProvider;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(final PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getCountryIsoCode() {
        if (countryIsoCode == null) {
            countryIsoCode = configuration.getDefaultCountryIsoCode();
        }
        return countryIsoCode;
    }

    public void setCountryIsoCode(final String countryIsoCode) {
        this.countryIsoCode = countryIsoCode;
    }

    public RecurringType getRecurringType() {
        return recurringType;
    }

    public void setRecurringType(final RecurringType recurringType) {
        this.recurringType = recurringType;
    }

    public Boolean isRecurringEnabled() {
        return getRecurringType() != null;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    /**
     * Indicates, whether we need to include the {@code termUrl} in the initial auth request
     * to Adyen, if 3D-Secure is configured. This is for example the case for PayU India.
     *
     * @return true, if we need to send the termUrl
     */
    public boolean send3DSTermUrl() {
        return Boolean.valueOf(MoreObjects.firstNonNull(configuration.getThreeDSTermUrl(), "false"));
    }

    public String getHppTargetOverride() {
        if (hppTargetOverride == null) {
            hppTargetOverride = configuration.getHppTargetOverride();
        }
        return hppTargetOverride;
    }

    public void setHppTargetOverride(final String hppTargetOverride) {
        this.hppTargetOverride = hppTargetOverride;
    }

    public String getHppVariantOverride() {
        if (hppVariantOverride == null) {
            hppVariantOverride = configuration.getHppVariantOverride();
        }
        return hppVariantOverride;
    }

    public void setHppVariantOverride(final String hppVariantOverride) {
        this.hppVariantOverride = hppVariantOverride;
    }

    public String getAllowedMethods() {
        if ((paymentType == PaymentType.DEBITCARDS_HPP || paymentType == PaymentType.FUNDS_TRANSFER) && paymentType.getNames().length > 1) {
            final String[] names = Arrays.copyOfRange(paymentType.getNames(), 1, paymentType.getNames().length);
            return JOINER.join(names);
        } else {
            return paymentType.getName();
        }
    }

    public Integer getThreeDThresholdOriginal() {
        return threeDThresholdOriginal;
    }

    public void setThreeDThresholdOriginal(final Integer threeDThresholdOriginal) {
        this.threeDThresholdOriginal = threeDThresholdOriginal;
    }

    public void setThreeDThreshold(final Long threeDThreshold) {
        this.threeDThresholdOriginal = threeDThreshold != null ? (int) Math.min(threeDThreshold, Integer.MAX_VALUE) : null;
    }

    /**
     * Reads the {@link Integer}, multiplies by 100 and returns the result as {@link Long}.
     *
     * @return a {@link Long} representing the threshold for 3D Secure
     */
    public Long getThreeDThreshold() {
        return toLong(threeDThresholdOriginal, DEFAULT_THREE_D_THRESHOLD);
    }

    private static Long toLong(final Number number, final Long defaultValue) {
        if (number != null) {
            return number.longValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * If the {@code PaymentProvider} has a default acquirer in its config details, then parse that to an {@code Acquirer} object.
     * If the acquirer code is followed by a | and a second value, that value is set to the {@code Acquirer} object and represents the MID.
     *
     * @return {@code List} of {@code Acquirer}, or an empty {@code List} if not set
     */
    public List<Acquirer> getAcquirers() {
        if (acquirers == null) {
            final List<Acquirer> newAcquirers;
            final String str = configuration.getAcquirersList();
            // Is there an acquirer list?
            if (!Strings.isNullOrEmpty(str)) {
                newAcquirers = new ArrayList<Acquirer>();
                // Parse the list of acquirers
                final String[] acquirerList = str.split(",");
                for (final String current : acquirerList) {
                    final Acquirer acquirer = parseAcquirer(current);
                    if (acquirer != null) {
                        newAcquirers.add(acquirer);
                    }
                }
            } else {
                newAcquirers = Collections.emptyList();
            }
            acquirers = newAcquirers;
        }
        return acquirers;
    }

    public Acquirer getDefaultAcquirer() {
        if (defaultAcquirer == null) {
            defaultAcquirer = parseAcquirer(configuration.getDefaultAcquirer());
        }
        return defaultAcquirer;
    }

    public Acquirer getAcquirerByName(final String name) {
        for (final Acquirer acquirer : getAcquirers()) {
            if (acquirer.getName().equals(name)) {
                return acquirer;
            }
        }
        return null;
    }

    private Acquirer parseAcquirer(final String str) {
        final Acquirer acquirer;
        if (!Strings.isNullOrEmpty(str)) {
            // Is there a second value separated by an | ?
            final String[] keyValue = str.split("\\|");
            if (keyValue.length > 1) {
                acquirer = new Acquirer(keyValue[0], keyValue[1]);
            } else {
                acquirer = new Acquirer(keyValue[0]);
            }
        } else {
            acquirer = null;
        }
        return acquirer;
    }
}
