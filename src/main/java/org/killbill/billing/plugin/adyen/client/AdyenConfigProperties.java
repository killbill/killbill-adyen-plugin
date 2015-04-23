/*
 * Copyright 2014-2015 Groupon, Inc
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

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Strings;

public class AdyenConfigProperties {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.adyen.";

    private static final String ENTRY_DELIMITER = "|";
    private static final String KEY_VALUE_DELIMITER = "#";

    private static final Locale LOCALE_EN_UK = new Locale("en", "UK");

    private final Map<String, String> merchantAccountMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> countryCodeMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> userMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> passwordMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> skinMap = new ConcurrentHashMap<String, String>();
    private final Map<String, String> secretMap = new ConcurrentHashMap<String, String>();

    private final String merchantAccounts;
    private final String userNames;
    private final String passwords;
    private final String skins;
    private final String hmacSecrets;
    private final String paymentUrl;
    private final String paymentWsdlUrl;
    private final String recurringUrl;
    private final String recurringWsdlUrl;
    private final String recurringConnectionTimeout;
    private final String recurringReceiveTimeout;
    private final String hppTarget;
    private final String allowChunking;
    private final String threeDSTermUrl;
    private final String hppTargetOverride;
    private final String hppVariantOverride;
    private final String acquirersList;
    private final String defaultAcquirer;
    private final String defaultCountryIsoCode;

    public AdyenConfigProperties(final Properties properties) {
        this.allowChunking = properties.getProperty(PROPERTY_PREFIX + "allowChunking");
        this.hppTarget = properties.getProperty(PROPERTY_PREFIX + "hpp.target");
        this.recurringReceiveTimeout = properties.getProperty(PROPERTY_PREFIX + "recurring.receiveTimeout");
        this.recurringConnectionTimeout = properties.getProperty(PROPERTY_PREFIX + "recurring.connectionTimeout");
        this.recurringWsdlUrl = properties.getProperty(PROPERTY_PREFIX + "recurringWsdlUrl");
        this.recurringUrl = properties.getProperty(PROPERTY_PREFIX + "recurringUrl");
        this.paymentWsdlUrl = properties.getProperty(PROPERTY_PREFIX + "paymentWsdlUrl");
        this.paymentUrl = properties.getProperty(PROPERTY_PREFIX + "paymentUrl");
        this.threeDSTermUrl = properties.getProperty(PROPERTY_PREFIX + "threeDSTermUrl");
        this.hppTargetOverride = properties.getProperty(PROPERTY_PREFIX + "hppTargetOverride");
        this.hppVariantOverride = properties.getProperty(PROPERTY_PREFIX + "hppVariantOverride");
        this.acquirersList = properties.getProperty(PROPERTY_PREFIX + "acquirersList");
        this.defaultAcquirer = properties.getProperty(PROPERTY_PREFIX + "defaultAcquirer");
        this.defaultCountryIsoCode = properties.getProperty(PROPERTY_PREFIX + "defaultCountryIsoCode");

        this.hmacSecrets = properties.getProperty(PROPERTY_PREFIX + "hmac.secret");
        refillMap(secretMap, hmacSecrets);

        this.passwords = properties.getProperty(PROPERTY_PREFIX + "password");
        refillMap(passwordMap, passwords);

        this.userNames = properties.getProperty(PROPERTY_PREFIX + "username");
        refillMap(userMap, userNames);

        this.skins = properties.getProperty(PROPERTY_PREFIX + "skin");
        refillMap(skinMap, skins);

        this.merchantAccounts = properties.getProperty(PROPERTY_PREFIX + "merchantAccount");
        merchantAccountMap.clear();
        if (merchantAccounts != null && merchantAccounts.contains(ENTRY_DELIMITER)) {
            for (final String account : merchantAccounts.split("\\" + ENTRY_DELIMITER)) {
                final String countryIsoCode = account.split(KEY_VALUE_DELIMITER)[0];
                final String merchantAccount = account.split(KEY_VALUE_DELIMITER)[1];
                merchantAccountMap.put(countryIsoCode, merchantAccount);
                countryCodeMap.put(merchantAccount, countryIsoCode);
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

    private void refillMap(final Map<String, String> map, final String stringToSplit) {
        map.clear();
        if (!Strings.isNullOrEmpty(stringToSplit) && stringToSplit.contains(ENTRY_DELIMITER)) {
            for (final String entry : stringToSplit.split("\\" + ENTRY_DELIMITER)) {
                map.put(entry.split("#")[0], entry.split(KEY_VALUE_DELIMITER)[1]);
            }
        }
    }
}
