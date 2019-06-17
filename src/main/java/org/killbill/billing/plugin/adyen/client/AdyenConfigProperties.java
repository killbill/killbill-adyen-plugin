/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import org.joda.time.Period;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class AdyenConfigProperties {

    public static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";

    public static final String DEFAULT_PENDING_PAYMENT_EXPIRATION_PERIOD = "P3d";
    public static final String DEFAULT_PENDING_3DS_PAYMENT_EXPIRATION_PERIOD = "PT3h";
    public static final String DEFAULT_PENDING_HPP_PAYMENT_WITHOUT_COMPLETION_EXPIRATION_PERIOD = "PT3h";
    // Online (real-time) bank transfers offer merchants payment with immediate online authorisation via a customer’s bank, usually followed by next-day settlement.
    public static final List<String> DEFAULT_ONLINE_BANK_TRANSFER_PAYMENT_METHODS = ImmutableList.<String>of("giropay", "ideal", "paypal");
    // Period is a bit generous by default. Decision is synchronous with the redirect, but Adyen might have notification delays.
    public static final String DEFAULT_ONLINE_BANK_TRANSFER_PENDING_PAYMENT_EXPIRATION_PERIOD = "P1d";
    // Offine bank transfers: the customer is presented with a reference number, which the customer must then quote to the bank (either online, via telephone or in a branch).
    public static final List<String> DEFAULT_OFFLINE_BANK_TRANSFER_PAYMENT_METHODS = ImmutableList.<String>of("boletobancario_bradesco",
                                                                                                              "boletobancario_santander",
                                                                                                              "directEbanking",
                                                                                                              "sepadirectdebit");
    // Period is a bit aggressive by default. SOFORT (directEbanking) payment can take up to 14 days.
    public static final String DEFAULT_OFFLINE_BANK_TRANSFER_PENDING_PAYMENT_EXPIRATION_PERIOD = "P7d";
    public static final String FALL_BACK_MERCHANT_ACCOUNT_KEY = "FALLBACK";

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.adyen.";
    private static final String ENTRY_DELIMITER = "|";
    private static final String KEY_VALUE_DELIMITER = "#";
    private static final String DEFAULT_CONNECTION_TIMEOUT = "30000";
    private static final String DEFAULT_READ_TIMEOUT = "60000";

    private final Map<String, String> paymentProcessorAccountIdToMerchantAccountMap = new LinkedHashMap<>();
    private final Map<String, String> countryToMerchantAccountMap = new LinkedHashMap<String, String>();
    private final Map<String, String> merchantAccountToUsernameMap = new LinkedHashMap<String, String>();
    private final Map<String, String> usernameToPasswordMap = new LinkedHashMap<String, String>();
    private final Map<String, String> merchantAccountToSkinMap = new LinkedHashMap<String, String>();
    private final Map<String, String> skinToSecretMap = new LinkedHashMap<String, String>();
    private final Map<String, String> skinToSecretAlgorithmMap = new LinkedHashMap<String, String>();
    private final Map<String, Period> paymentMethodToExpirationPeriod = new LinkedHashMap<String, Period>();
    private final Map<String, String> regionToPaymentUrlMap = new LinkedHashMap<String, String>();
    private final Map<String, String> regionToRecurringUrlMap = new LinkedHashMap<String, String>();
    private final Map<String, String> regionToDirectoryUrlMap = new LinkedHashMap<String, String>();
    private final List<String> sensitivePropertyKeys = new ArrayList<>();

    private final String paymentProcessorAccountIdToMerchantAccount;
    private final String merchantAccounts;
    private final String userNames;
    private final String passwords;
    private final String skins;
    private final String hmacSecrets;
    private final String hmacAlgorithms;
    private final String defaultPaymentUrl;
    private final String defaultRecurringUrl;
    private final String defaultDirectoryUrl;
    private final String recurringConnectionTimeout;
    private final String recurringReadTimeout;
    private final String hppTarget;
    private final String hppSkipDetailsTarget;
    private final String proxyServer;
    private final String proxyPort;
    private final String proxyType;
    private final String trustAllCertificates;
    private final String allowChunking;
    private final String hppVariantOverride;
    private final String acquirersList;
    private final String paymentConnectionTimeout;
    private final String paymentReadTimeout;
    private final String fallBackMerchantAccount;

    private final Period pendingPaymentExpirationPeriod;

    private final Period pendingHppPaymentWithoutCompletionExpirationPeriod;

    private final Period pending3DsPaymentExpirationPeriod;

    private final String currentRegion;

    private final String invoicePaymentEnabled;
    private final Set<String> chargebackAsFailurePaymentMethods;

    public AdyenConfigProperties(final Properties properties) {
        this(properties, null);
    }

    public AdyenConfigProperties(final Properties properties, final String currentRegion) {
        this.currentRegion = currentRegion;

        this.invoicePaymentEnabled = properties.getProperty(PROPERTY_PREFIX + "invoicePaymentEnabled", "false");
        this.chargebackAsFailurePaymentMethods = ImmutableSet.<String>copyOf(properties.getProperty(PROPERTY_PREFIX + "chargebackAsFailurePaymentMethods", "").split(","));

        this.proxyServer = properties.getProperty(PROPERTY_PREFIX + "proxyServer");
        this.proxyPort = properties.getProperty(PROPERTY_PREFIX + "proxyPort");
        this.proxyType = properties.getProperty(PROPERTY_PREFIX + "proxyType");
        this.trustAllCertificates = properties.getProperty(PROPERTY_PREFIX + "trustAllCertificates", "false");
        this.allowChunking = properties.getProperty(PROPERTY_PREFIX + "allowChunking", "false");

        this.defaultPaymentUrl = properties.getProperty(PROPERTY_PREFIX + "paymentUrl");
        refillUrlMap(regionToPaymentUrlMap, properties, "paymentUrl");

        this.paymentConnectionTimeout = properties.getProperty(PROPERTY_PREFIX + "paymentConnectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        this.paymentReadTimeout = properties.getProperty(PROPERTY_PREFIX + "paymentReadTimeout", DEFAULT_READ_TIMEOUT);

        this.defaultRecurringUrl = properties.getProperty(PROPERTY_PREFIX + "recurringUrl");
        refillUrlMap(regionToRecurringUrlMap, properties, "recurringUrl");

        this.recurringConnectionTimeout = properties.getProperty(PROPERTY_PREFIX + "recurringConnectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        this.recurringReadTimeout = properties.getProperty(PROPERTY_PREFIX + "recurringReadTimeout", DEFAULT_READ_TIMEOUT);

        this.defaultDirectoryUrl = properties.getProperty(PROPERTY_PREFIX + "directoryUrl");
        refillUrlMap(regionToDirectoryUrlMap, properties, "directoryUrl");

        this.hppTarget = properties.getProperty(PROPERTY_PREFIX + "hpp.target");
        this.hppSkipDetailsTarget = this.hppTarget != null ? this.hppTarget.replace(this.hppTarget.substring(this.hppTarget.lastIndexOf('/') + 1), "skipDetails.shtml") : null;
        this.hppVariantOverride = properties.getProperty(PROPERTY_PREFIX + "hppVariantOverride");

        this.pendingPaymentExpirationPeriod = readPendingExpirationProperty(properties);
        this.pending3DsPaymentExpirationPeriod = read3DsPendingExpirationProperty(properties);
        this.pendingHppPaymentWithoutCompletionExpirationPeriod = readPendingHppPaymentWithoutCompletionExpirationPeriod(properties);

        this.acquirersList = properties.getProperty(PROPERTY_PREFIX + "acquirersList");

        this.paymentProcessorAccountIdToMerchantAccount = properties.getProperty(PROPERTY_PREFIX + "paymentProcessorAccountIdToMerchantAccount");
        refillMap(paymentProcessorAccountIdToMerchantAccountMap, paymentProcessorAccountIdToMerchantAccount);

        this.merchantAccounts = properties.getProperty(PROPERTY_PREFIX + "merchantAccount");
        refillMap(countryToMerchantAccountMap, merchantAccounts);
        if (this.countryToMerchantAccountMap.containsKey(FALL_BACK_MERCHANT_ACCOUNT_KEY)) {
            this.fallBackMerchantAccount = this.countryToMerchantAccountMap.get(FALL_BACK_MERCHANT_ACCOUNT_KEY);
            this.countryToMerchantAccountMap.remove(FALL_BACK_MERCHANT_ACCOUNT_KEY);
        }
        else {
            this.fallBackMerchantAccount = null;
        }

        this.userNames = properties.getProperty(PROPERTY_PREFIX + "username");
        final Map<String, String> countryOrMerchantAccountToUsernameMap = new LinkedHashMap<String, String>();
        refillMap(countryOrMerchantAccountToUsernameMap, userNames);
        for (final String countryOrMerchantAccount : countryOrMerchantAccountToUsernameMap.keySet()) {
            final String merchantAccountOrNull = countryToMerchantAccountMap.get(countryOrMerchantAccount);
            final String merchantAccount = MoreObjects.firstNonNull(merchantAccountOrNull, countryOrMerchantAccount);
            final String username = countryOrMerchantAccountToUsernameMap.get(countryOrMerchantAccount);
            merchantAccountToUsernameMap.put(merchantAccount, username);
        }

        this.passwords = properties.getProperty(PROPERTY_PREFIX + "password");
        final Map<String, String> countryOrUsernameToPasswordMap = new LinkedHashMap<String, String>();
        refillMap(countryOrUsernameToPasswordMap, passwords);
        for (final String countryOrUsername : countryOrUsernameToPasswordMap.keySet()) {
            final String merchantAccountOrNull = countryToMerchantAccountMap.get(countryOrUsername);
            final String userName = merchantAccountOrNull != null ? merchantAccountToUsernameMap.get(merchantAccountOrNull) : countryOrUsername;
            final String password = countryOrUsernameToPasswordMap.get(countryOrUsername);
            usernameToPasswordMap.put(userName, password);
        }

        this.skins = properties.getProperty(PROPERTY_PREFIX + "skin");
        final Map<String, String> countryOrMerchantAccountToSkinMap = new LinkedHashMap<String, String>();
        refillMap(countryOrMerchantAccountToSkinMap, skins);
        for (final String countryOrMerchantAccount : countryOrMerchantAccountToSkinMap.keySet()) {
            final String merchantAccountOrNull = countryToMerchantAccountMap.get(countryOrMerchantAccount);
            final String merchantAccount = MoreObjects.firstNonNull(merchantAccountOrNull, countryOrMerchantAccount);
            final String skin = countryOrMerchantAccountToSkinMap.get(countryOrMerchantAccount);
            merchantAccountToSkinMap.put(merchantAccount, skin);
        }

        this.hmacSecrets = properties.getProperty(PROPERTY_PREFIX + "hmac.secret");
        final Map<String, String> countryOrSkinToSecretMap = new LinkedHashMap<String, String>();
        refillMap(countryOrSkinToSecretMap, hmacSecrets);
        for (final String countryOrSkin : countryOrSkinToSecretMap.keySet()) {
            final String merchantAccountOrNull = countryToMerchantAccountMap.get(countryOrSkin);
            final String skin = merchantAccountOrNull != null ? merchantAccountToSkinMap.get(merchantAccountOrNull) : countryOrSkin;
            final String secret = countryOrSkinToSecretMap.get(countryOrSkin);
            skinToSecretMap.put(skin, secret);
        }

        this.hmacAlgorithms = properties.getProperty(PROPERTY_PREFIX + "hmac.algorithm", DEFAULT_HMAC_ALGORITHM);
        final Map<String, String> countryOrSkinToSecretAlgorithmMap = new LinkedHashMap<String, String>();
        refillMap(countryOrSkinToSecretAlgorithmMap, hmacAlgorithms);
        for (final String countryOrSkin : countryOrSkinToSecretAlgorithmMap.keySet()) {
            final String merchantAccountOrNull = countryToMerchantAccountMap.get(countryOrSkin);
            final String skin = merchantAccountOrNull != null ? merchantAccountToSkinMap.get(merchantAccountOrNull) : countryOrSkin;
            final String secretAlgorithm = countryOrSkinToSecretAlgorithmMap.get(countryOrSkin);
            skinToSecretAlgorithmMap.put(skin, secretAlgorithm);
        }

        readSensitivePropertyKeys(properties.getProperty(PROPERTY_PREFIX + "sensitiveProperties"));
    }

    private void readSensitivePropertyKeys(final String property) {
        sensitivePropertyKeys.clear();
        if(!Strings.isNullOrEmpty(property)) {
            for (final String entry : property.split("\\" + ENTRY_DELIMITER)) {
                sensitivePropertyKeys.add(entry);
            }
        }
    }

    private Period readPendingHppPaymentWithoutCompletionExpirationPeriod(final Properties properties) {
        final String value = properties.getProperty(PROPERTY_PREFIX + "pendingHppPaymentWithoutCompletionExpirationPeriod");
        if (value != null) {
            try {
                return Period.parse(value);
            } catch (final IllegalArgumentException e) { /* Ignore */ }
        }

        return Period.parse(DEFAULT_PENDING_HPP_PAYMENT_WITHOUT_COMPLETION_EXPIRATION_PERIOD);
    }

    private Period readPendingExpirationProperty(final Properties properties) {
        // Legacy days value has precedence
        final String valueInDays = properties.getProperty(PROPERTY_PREFIX + "pendingPaymentExpirationPeriodInDays");
        if (valueInDays != null) {
            try {
                return Period.days(Integer.parseInt(valueInDays));
            } catch (final NumberFormatException e) { /* Ignore */ }
        }

        final String pendingExpirationPeriods = properties.getProperty(PROPERTY_PREFIX + "pendingPaymentExpirationPeriod");
        final Map<String, String> paymentMethodToExpirationPeriodString = new HashMap<String, String>();
        refillMap(paymentMethodToExpirationPeriodString, pendingExpirationPeriods);
        // No per-payment method override, just a global setting
        if (pendingExpirationPeriods != null && paymentMethodToExpirationPeriodString.isEmpty()) {
            try {
                return Period.parse(pendingExpirationPeriods);
            } catch (final IllegalArgumentException e) { /* Ignore */ }
        }

        for (final String paymentMethod : DEFAULT_ONLINE_BANK_TRANSFER_PAYMENT_METHODS) {
            paymentMethodToExpirationPeriod.put(paymentMethod.toLowerCase(), Period.parse(DEFAULT_ONLINE_BANK_TRANSFER_PENDING_PAYMENT_EXPIRATION_PERIOD));
        }
        for (final String paymentMethod : DEFAULT_OFFLINE_BANK_TRANSFER_PAYMENT_METHODS) {
            paymentMethodToExpirationPeriod.put(paymentMethod.toLowerCase(), Period.parse(DEFAULT_OFFLINE_BANK_TRANSFER_PENDING_PAYMENT_EXPIRATION_PERIOD));
        }

        // User has defined per-payment method overrides
        for (final String paymentMethod : paymentMethodToExpirationPeriodString.keySet()) {
            try {
                paymentMethodToExpirationPeriod.put(paymentMethod.toLowerCase(), Period.parse(paymentMethodToExpirationPeriodString.get(paymentMethod)));
            } catch (final IllegalArgumentException e) { /* Ignore */ }
        }

        return Period.parse(DEFAULT_PENDING_PAYMENT_EXPIRATION_PERIOD);
    }

    private Period read3DsPendingExpirationProperty(final Properties properties) {
        final String value = properties.getProperty(PROPERTY_PREFIX + "pending3DsPaymentExpirationPeriod");
        if (value != null) {
            try {
                return Period.parse(value);
            } catch (IllegalArgumentException e) { /* Ignore */ }
        }

        return Period.parse(DEFAULT_PENDING_3DS_PAYMENT_EXPIRATION_PERIOD);
    }

    public Boolean getInvoicePaymentEnabled() {
        return Boolean.valueOf(invoicePaymentEnabled);
    }

    public Set<String> getChargebackAsFailurePaymentMethods() {
        return chargebackAsFailurePaymentMethods;
    }

    public Optional<String> getMerchantAccountOfPaymentProcessorAccountId(final String paymentProcessorAccountId) {
        return Optional.ofNullable(paymentProcessorAccountIdToMerchantAccountMap.get(paymentProcessorAccountId));
    }

    public String getMerchantAccount(final String countryIsoCode) {
        if (countryToMerchantAccountMap.isEmpty()) {
            return merchantAccounts;
        } else if (countryIsoCode == null) {
            // In case no country is specified, but the user configured the merchant accounts per country, take the fallback one if configured. Otherwise, take the first one.
            return MoreObjects.firstNonNull(fallBackMerchantAccount, countryToMerchantAccountMap.values().iterator().next());
        } else {
            try {
                return MoreObjects.firstNonNull(countryToMerchantAccountMap.get(adjustCountryCode(countryIsoCode)), fallBackMerchantAccount);
            } catch (NullPointerException exception) {
                throw new IllegalStateException(String.format("Failed to find merchant account for countryCode='%s'", countryIsoCode), exception);
            }
        }
    }

    public String getUserName(final String merchantAccount) {
        if (merchantAccountToUsernameMap.isEmpty()) {
            return userNames;
        } else {
            return merchantAccountToUsernameMap.get(merchantAccount);
        }
    }

    public String getPassword(final String userName) {
        if (usernameToPasswordMap.isEmpty()) {
            return passwords;
        } else {
            return usernameToPasswordMap.get(userName);
        }
    }

    public String getSkin(final String merchantAccount) {
        if (merchantAccountToSkinMap.isEmpty()) {
            return skins;
        } else {
            return merchantAccountToSkinMap.get(merchantAccount);
        }
    }

    public String getHmacSecret(final String skin) {
        if (skinToSecretMap.isEmpty()) {
            return hmacSecrets;
        } else {
            return skinToSecretMap.get(skin);
        }
    }

    public String getHmacAlgorithm(final String skin) {
        if (skinToSecretAlgorithmMap.isEmpty()) {
            return hmacAlgorithms;
        } else {
            return skinToSecretAlgorithmMap.get(skin);
        }
    }

    public String getHppTarget() {
        return hppTarget;
    }

    public String getHppSkipDetailsTarget() {
        return hppSkipDetailsTarget;
    }

    public String getHppVariantOverride() {
        return hppVariantOverride;
    }

    public Period getPendingPaymentExpirationPeriod(@Nullable final String paymentMethod) {
        if (paymentMethod != null && paymentMethodToExpirationPeriod.get(paymentMethod.toLowerCase()) != null) {
            return paymentMethodToExpirationPeriod.get(paymentMethod.toLowerCase());
        } else {
            return pendingPaymentExpirationPeriod;
        }
    }

    public Period getPending3DsPaymentExpirationPeriod() {
        return pending3DsPaymentExpirationPeriod;
    }

    public String getAcquirersList() {
        return acquirersList;
    }

    public String getProxyServer() {
        return proxyServer;
    }

    public Integer getProxyPort() {
        return proxyPort == null ? null : Integer.valueOf(proxyPort);
    }

    public String getProxyType() {
        return proxyType;
    }

    public Boolean getTrustAllCertificates() {
        return Boolean.valueOf(trustAllCertificates);
    }

    public Boolean getAllowChunking() {
        return Boolean.valueOf(allowChunking);
    }

    public String getPaymentUrl() {
        final String perRegionUrl = currentRegion == null ? null : regionToPaymentUrlMap.get(currentRegion);
        return perRegionUrl != null ? perRegionUrl : defaultPaymentUrl;
    }

    public String getPaymentConnectionTimeout() {
        return paymentConnectionTimeout;
    }

    public String getPaymentReadTimeout() {
        return paymentReadTimeout;
    }

    public String getRecurringUrl() {
        final String perRegionUrl = currentRegion == null ? null : regionToRecurringUrlMap.get(currentRegion);
        return perRegionUrl != null ? perRegionUrl : defaultRecurringUrl;
    }

    public String getDirectoryUrl() {
        final String perRegionUrl = currentRegion == null ? null : regionToDirectoryUrlMap.get(currentRegion);
        return perRegionUrl != null ? perRegionUrl : defaultDirectoryUrl;
    }

    public String getRecurringConnectionTimeout() {
        return recurringConnectionTimeout;
    }

    public String getRecurringReadTimeout() {
        return recurringReadTimeout;
    }

    public List<String> getSensitivePropertyKeys() {
        return sensitivePropertyKeys;
    }

    private static String adjustCountryCode(final String countryIsoCode) {
        if (Strings.isNullOrEmpty(countryIsoCode)) {
            return null;
        }
        final String countryCodeUpperCase = countryIsoCode.toUpperCase();
        if ("GB".equalsIgnoreCase(countryCodeUpperCase)) {
            return "UK";
        } else if ("QC".equalsIgnoreCase(countryCodeUpperCase)) {
            return "CA";
        } else {
            return countryCodeUpperCase;
        }
    }

    private synchronized void refillUrlMap(final Map<String, String> map, final Properties properties, final String suffix) {
        for (final Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
            final String key = e.nextElement().toString();
            final int idx = key.indexOf("." + PROPERTY_PREFIX + suffix);
            if (idx == -1) {
                continue;
            }

            final String region = key.substring(0, idx);
            final String url = properties.get(key).toString();

            map.put(region, url);
        }
    }

    private synchronized void refillMap(final Map<String, String> map, final String stringToSplit) {
        map.clear();
        if (!Strings.isNullOrEmpty(stringToSplit)) {
            for (final String entry : stringToSplit.split("\\" + ENTRY_DELIMITER)) {
                final String[] split = entry.split(KEY_VALUE_DELIMITER, 2);
                if (split.length > 1) {
                    map.put(split[0], split[1]);
                }
            }
        }
    }

    public Period getPendingHppPaymentWithoutCompletionExpirationPeriod() {
        return pendingHppPaymentWithoutCompletionExpirationPeriod;
    }
}
