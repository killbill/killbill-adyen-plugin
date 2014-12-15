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

package org.killbill.billing.plugin.adyen.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class AdyenConfigProperties {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.adyen.";

    private static final String ENTRY_DELIMITER = "|";
    private static final String KEY_VALUE_DELIMITER = "#";

    private static final Locale LOCALE_EN_UK = new Locale("en", "UK");

    private final List<Runnable> onChangeFunctions = Collections.synchronizedList(new ArrayList<Runnable>());

    private Map<String, Function<String, Void>> propertiesToObserve = ImmutableMap.<String, Function<String, Void>>builder()
                                                                                  .put(PROPERTY_PREFIX + "allowChunking", new SetAllowChunking())
                                                                                  .put(PROPERTY_PREFIX + "hpp.target", new SetHppTarget())
                                                                                  .put(PROPERTY_PREFIX + "recurring.receiveTimeout", new SetRecurringReceiveTimeout())
                                                                                  .put(PROPERTY_PREFIX + "recurring.connectionTimeout", new SetRecurringConnectionTimeout())
                                                                                  .put(PROPERTY_PREFIX + "recurringWsdlUrl", new SetRecurringWsdlUrl())
                                                                                  .put(PROPERTY_PREFIX + "recurringUrl", new SetRecurringUrl())
                                                                                  .put(PROPERTY_PREFIX + "paymentWsdlUrl", new SetPaymentWsdlUrl())
                                                                                  .put(PROPERTY_PREFIX + "paymentUrl", new SetPaymentUrl())
                                                                                  .put(PROPERTY_PREFIX + "hmac.secret", new SetSecrets())
                                                                                  .put(PROPERTY_PREFIX + "password", new SetPasswords())
                                                                                  .put(PROPERTY_PREFIX + "username", new SetUserNames())
                                                                                  .put(PROPERTY_PREFIX + "merchantAccount", new SetMerchantAccounts())
                                                                                  .put(PROPERTY_PREFIX + "skin", new SetSkins())
                                                                                  .put(PROPERTY_PREFIX + "threeDSTermUrl", new Set3DSTermUrl())
                                                                                  .put(PROPERTY_PREFIX + "hppTargetOverride", new SetHppTargetOverride())
                                                                                  .put(PROPERTY_PREFIX + "hppVariantOverride", new SetHppVariantOverride())
                                                                                  .put(PROPERTY_PREFIX + "acquirersList", new SetAcquirersList())
                                                                                  .put(PROPERTY_PREFIX + "defaultAcquirer", new SetDefaultAcquirer())
                                                                                  .put(PROPERTY_PREFIX + "defaultCountryIsoCode", new SetDefaultCountryIsoCode())
                                                                                  .build();

    private final Map<String, String> merchantAccountMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> countryCodeMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> userMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> passwordMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> skinMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> secretMap = new ConcurrentHashMap<String, String>();

    private String merchantAccounts;
    private String userNames;
    private String passwords;
    private String skins;
    private String hmacSecrets;
    private String paymentUrl;
    private String paymentWsdlUrl;
    private String recurringUrl;
    private String recurringWsdlUrl;
    private String recurringConnectionTimeout;
    private String recurringReceiveTimeout;
    private String hppTarget;
    private String allowChunking;
    private String threeDSTermUrl;
    private String hppTargetOverride;
    private String hppVariantOverride;
    private String acquirersList;
    private String defaultAcquirer;
    private String defaultCountryIsoCode;

    public AdyenConfigProperties(final Properties properties) {
        for (final Map.Entry<String, Function<String, Void>> propertyToObserve : propertiesToObserve.entrySet()) {
            final String propertyName = propertyToObserve.getKey();
            final Function<String, Void> consumer = propertyToObserve.getValue();

            if (properties.containsKey(propertyName)) {
                final Object property = properties.get(propertyName);
                final String value = property != null ? property.toString() : null;
                consumer.apply(value);
            }
        }
    }

    /**
     * Translates "GB" to "UK".
     *
     * @param countryIsoCode country iso code
     * @return same as input, except for when the input is GB, then UK is returned
     */
    public static String gbToUK(final String countryIsoCode) {
        if (Strings.isNullOrEmpty(countryIsoCode)) {
            return null;
        }
        return countryIsoCode.equalsIgnoreCase("GB") ? "UK" : countryIsoCode;
    }

    public static String adjustCountryCode(final String countryIsoCode) {
        final String countryCodeUpperCase = countryIsoCode.toUpperCase();
        return gbToUK(countryCodeUpperCase);
    }

    /**
     * Translates "en_GB" to "en_UK".
     *
     * @param locale locale
     * @return same as input, except for when the input is en_GB, then en_UK is returned
     */
    public static Locale gbToUK(final Locale locale) {
        if (locale == null) {
            return null;
        }
        // NOTE: Locale.UK is defined as "en_GB".
        return locale.equals(Locale.UK) ? LOCALE_EN_UK : locale;
    }

    public String getMerchantAccount(final String countryIsoCode) {
        if (countryIsoCode == null || merchantAccountMap.isEmpty()) {
            return merchantAccounts;
        }

        return merchantAccountMap.get(adjustCountryCode(countryIsoCode));
    }

    public String getPassword(final String countryIsoCode) {
        if (countryIsoCode == null || passwordMap.isEmpty()) {
            return passwords;
        }

        return passwordMap.get(adjustCountryCode(countryIsoCode));
    }

    public String getUserName(final String countryIsoCode) {
        if (countryIsoCode == null || userMap.isEmpty()) {
            return userNames;
        }

        return userMap.get(adjustCountryCode(countryIsoCode));
    }

    public String getSkin(final String countryIsoCode) {
        if (countryIsoCode == null || skinMap.isEmpty()) {
            return skins;
        }

        return skinMap.get(adjustCountryCode(countryIsoCode));
    }

    public String getHmacSecret(final String countryIsoCode) {
        if (countryIsoCode == null || secretMap.isEmpty()) {
            return hmacSecrets;
        }

        return secretMap.get(adjustCountryCode(countryIsoCode));
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public String getPaymentWsdlUrl() {
        return paymentWsdlUrl;
    }

    public String getRecurringUrl() {
        return recurringUrl;
    }

    public String getRecurringWsdlUrl() {
        return recurringWsdlUrl;
    }

    public String getRecurringConnectionTimeout() {
        return recurringConnectionTimeout;
    }

    public String getRecurringReceiveTimeout() {
        return recurringReceiveTimeout;
    }

    public String getHppTarget() {
        return hppTarget;
    }

    public String getShipBeforeDatePattern() {
        return "yyyy-MM-dd";
    }

    public String getSessionValidityDatePattern() {
        return "yyyy-MM-dd'T'HH:mm:ssZZ";
    }

    public Boolean getAllowChunking() {
        return Boolean.valueOf(allowChunking);
    }

    public String getThreeDSTermUrl() {
        return threeDSTermUrl;
    }

    public String getHppTargetOverride() {
        return hppTargetOverride;
    }

    public String getHppVariantOverride() {
        return hppVariantOverride;
    }

    public String getAcquirersList() {
        return acquirersList;
    }

    public String getDefaultAcquirer() {
        return defaultAcquirer;
    }

    public String getDefaultCountryIsoCode() {
        return defaultCountryIsoCode;
    }

    public void addOnChangeFunction(final Runnable executeOnChange) {
        onChangeFunctions.add(executeOnChange);
    }

    private class SetAllowChunking extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            allowChunking = input;
        }
    }

    private class SetHppTarget extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            hppTarget = input;
        }
    }

    private class SetRecurringReceiveTimeout extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            recurringReceiveTimeout = input;
        }
    }

    private class SetRecurringConnectionTimeout extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            recurringConnectionTimeout = input;
        }
    }

    private class SetRecurringWsdlUrl extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            recurringWsdlUrl = input;
        }
    }

    private class SetRecurringUrl extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            recurringUrl = input;
        }
    }

    private class SetPaymentWsdlUrl extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            paymentWsdlUrl = input;
        }
    }

    private class SetPaymentUrl extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            paymentUrl = input;
        }
    }

    private class Set3DSTermUrl extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            threeDSTermUrl = input;
        }
    }

    private class SetHppTargetOverride extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            hppTargetOverride = input;
        }
    }

    private class SetHppVariantOverride extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            hppVariantOverride = input;
        }
    }

    private class SetAcquirersList extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            acquirersList = input;
        }
    }

    private class SetDefaultAcquirer extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            defaultAcquirer = input;
        }
    }

    private class SetDefaultCountryIsoCode extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            defaultCountryIsoCode = input;
        }
    }

    private class SetSecrets extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            hmacSecrets = input;
            refillMap(secretMap, hmacSecrets);
        }
    }

    private class SetSkins extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            skins = input;
            refillMap(skinMap, skins);
        }
    }

    private class SetPasswords extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            passwords = input;
            refillMap(passwordMap, passwords);
        }
    }

    private class SetUserNames extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            userNames = input;
            refillMap(userMap, userNames);
        }
    }

    private class SetMerchantAccounts extends ChangeAttributeFunction {

        @Override
        public void onApply(final String input) {
            merchantAccountMap.clear();
            merchantAccounts = input;
            if (merchantAccounts.contains(ENTRY_DELIMITER)) {
                for (final String account : merchantAccounts.split("\\" + ENTRY_DELIMITER)) {
                    final String countryIsoCode = account.split(KEY_VALUE_DELIMITER)[0];
                    final String merchantAccount = account.split(KEY_VALUE_DELIMITER)[1];
                    merchantAccountMap.put(countryIsoCode, merchantAccount);
                    countryCodeMap.put(merchantAccount, countryIsoCode);
                }
            }
        }
    }

    private void refillMap(final Map<String, String> map, final String stringToSplit) {
        map.clear();
        if (!Strings.isNullOrEmpty(stringToSplit) && stringToSplit.contains(ENTRY_DELIMITER)) {
            for (final String entry : stringToSplit.split("\\" + ENTRY_DELIMITER)) {
                map.put(entry.split("#")[0], entry.split(KEY_VALUE_DELIMITER)[1]);
            }
        }
    }

    private abstract class ChangeAttributeFunction implements Function<String, Void> {

        @Nullable
        @Override
        public Void apply(@Nullable final String input) {
            if (Strings.isNullOrEmpty(input)) {
                return null;
            }

            onApply(input);
            for (final Runnable function : onChangeFunctions) {
                function.run();
            }
            return null;
        }

        public abstract void onApply(String input);
    }
}
