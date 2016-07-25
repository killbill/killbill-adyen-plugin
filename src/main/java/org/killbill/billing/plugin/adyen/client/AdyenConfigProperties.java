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

package org.killbill.billing.plugin.adyen.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

public class AdyenConfigProperties {

    public static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.adyen.";
    private static final String ENTRY_DELIMITER = "|";
    private static final String KEY_VALUE_DELIMITER = "#";
    private static final String DEFAULT_CONNECTION_TIMEOUT = "30000";
    private static final String DEFAULT_READ_TIMEOUT = "60000";

    private final Map<String, String> countryToMerchantAccountMap = new LinkedHashMap<String, String>();
    private final Map<String, String> merchantAccountToUsernameMap = new LinkedHashMap<String, String>();
    private final Map<String, String> usernameToPasswordMap = new LinkedHashMap<String, String>();
    private final Map<String, String> merchantAccountToSkinMap = new LinkedHashMap<String, String>();
    private final Map<String, String> skinToSecretMap = new LinkedHashMap<String, String>();
    private final Map<String, String> skinToSecretAlgorithmMap = new LinkedHashMap<String, String>();

    private final String merchantAccounts;
    private final String userNames;
    private final String passwords;
    private final String skins;
    private final String hmacSecrets;
    private final String hmacAlgorithms;
    private final String paymentUrl;
    private final String recurringUrl;
    private final String directoryUrl;
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

    public AdyenConfigProperties(final Properties properties) {
        this.proxyServer = properties.getProperty(PROPERTY_PREFIX + "proxyServer");
        this.proxyPort = properties.getProperty(PROPERTY_PREFIX + "proxyPort");
        this.proxyType = properties.getProperty(PROPERTY_PREFIX + "proxyType");
        this.trustAllCertificates = properties.getProperty(PROPERTY_PREFIX + "trustAllCertificates", "false");
        this.allowChunking = properties.getProperty(PROPERTY_PREFIX + "allowChunking", "false");

        this.paymentUrl = properties.getProperty(PROPERTY_PREFIX + "paymentUrl");
        this.paymentConnectionTimeout = properties.getProperty(PROPERTY_PREFIX + "paymentConnectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        this.paymentReadTimeout = properties.getProperty(PROPERTY_PREFIX + "paymentReadTimeout", DEFAULT_READ_TIMEOUT);

        this.recurringUrl = properties.getProperty(PROPERTY_PREFIX + "recurringUrl");
        this.recurringConnectionTimeout = properties.getProperty(PROPERTY_PREFIX + "recurringConnectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
        this.recurringReadTimeout = properties.getProperty(PROPERTY_PREFIX + "recurringReadTimeout", DEFAULT_READ_TIMEOUT);

        this.directoryUrl = properties.getProperty(PROPERTY_PREFIX + "directoryUrl");

        this.hppTarget = properties.getProperty(PROPERTY_PREFIX + "hpp.target");
        this.hppSkipDetailsTarget = this.hppTarget != null ? this.hppTarget.replace(this.hppTarget.substring(this.hppTarget.lastIndexOf('/') + 1), "skipDetails.shtml") : null;
        this.hppVariantOverride = properties.getProperty(PROPERTY_PREFIX + "hppVariantOverride");
        this.acquirersList = properties.getProperty(PROPERTY_PREFIX + "acquirersList");

        this.merchantAccounts = properties.getProperty(PROPERTY_PREFIX + "merchantAccount");
        refillMap(countryToMerchantAccountMap, merchantAccounts);

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
    }

    public String getMerchantAccount(final String countryIsoCode) {
        if (countryToMerchantAccountMap.isEmpty()) {
            return merchantAccounts;
        } else if (countryIsoCode == null) {
            // In case no country is specified, but the user configured the merchant accounts per country, take the first one
            return countryToMerchantAccountMap.values().iterator().next();
        } else {
            return countryToMerchantAccountMap.get(adjustCountryCode(countryIsoCode));
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
        return paymentUrl;
    }

    public String getPaymentConnectionTimeout() {
        return paymentConnectionTimeout;
    }

    public String getPaymentReadTimeout() {
        return paymentReadTimeout;
    }

    public String getRecurringUrl() {
        return recurringUrl;
    }

    public String getDirectoryUrl() {
        return directoryUrl;
    }

    public String getRecurringConnectionTimeout() {
        return recurringConnectionTimeout;
    }

    public String getRecurringReadTimeout() {
        return recurringReadTimeout;
    }

    private static String adjustCountryCode(final String countryIsoCode) {
        if (Strings.isNullOrEmpty(countryIsoCode)) {
            return null;
        }
        final String countryCodeUpperCase = countryIsoCode.toUpperCase();
        return "GB".equalsIgnoreCase(countryCodeUpperCase) ? "UK" : countryCodeUpperCase;
    }

    private synchronized void refillMap(final Map<String, String> map, final String stringToSplit) {
        map.clear();
        if (!Strings.isNullOrEmpty(stringToSplit) && stringToSplit.contains(ENTRY_DELIMITER)) {
            for (final String entry : stringToSplit.split("\\" + ENTRY_DELIMITER)) {
                map.put(entry.split(KEY_VALUE_DELIMITER)[0], entry.split(KEY_VALUE_DELIMITER)[1]);
            }
        }
    }
}
