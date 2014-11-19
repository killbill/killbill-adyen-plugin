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

import org.killbill.billing.plugin.adyen.core.AdyenConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdyenConfigProperties {


    private static final Locale LOCALE = new Locale("en", "UK");

    private String merchantAccounts;
    private String userNames;
    private String passwords;
    private String paymentUrl;
    private final Map<String, String> merchantAccountMap = new HashMap<String, String>();

    private final Map<String, String> countryCodeMap = new HashMap<String, String>();

    private final Map<String, String> userMap = new HashMap<String, String>();

    private final Map<String, String> passwordMap = new HashMap<String, String>();


    /**
     * Fix for GPS-633 - translates "GB" to "UK".
     *
     * @param countryIsoCode country iso code
     * @return same as input, except for when the input is GB, then UK is returned
     */
    public static String gbToUK(final String countryIsoCode) {
        if (countryIsoCode == null) return null;
        return countryIsoCode.equalsIgnoreCase("GB") ? "UK" : countryIsoCode;
    }

    /**
     * Fix for GPS-633 - translates "en_GB" to "en_UK".
     *
     * @param locale locale
     * @return same as input, except for when the input is en_GB, then en_UK is returned
     */
    public static Locale gbToUK(final Locale locale) {
        if (locale == null) return null;
        // NOTE: Locale.UK is defined as "en_GB".
        return locale.equals(Locale.UK) ? LOCALE : locale;
    }

    public AdyenConfigProperties(final AdyenConfig config) {
        this.merchantAccounts = config.getMerchantAccounts();
        this.userNames = config.getUserNames();
        this.passwords = config.getPasswords();
        this.paymentUrl = config.getPaymentUrl();

        if (merchantAccounts.contains("|")) {
            for (String account : merchantAccounts.split("\\|")) {
                final String countryIsoCode = account.split("\\#")[0];
                final String merchantAccount = account.split("\\#")[1];
                this.merchantAccountMap.put(countryIsoCode, merchantAccount);
                this.countryCodeMap.put(merchantAccount, countryIsoCode);
            }
            for (String user : userNames.split("\\|")) {
                this.userMap.put(user.split("\\#")[0], user.split("\\#")[1]);
            }
            for (String password : passwords.split("\\|")) {
                this.passwordMap.put(password.split("\\#")[0], password.split("\\#")[1]);
            }
        }
    }


    public String getMerchantAccount(final String countryIsoCode) {
        if (countryIsoCode == null || merchantAccountMap.isEmpty()) {
            return merchantAccounts;
        }

        return merchantAccountMap.get(gbToUK(countryIsoCode));
    }

    public String getPassword(final String countryIsoCode) {
        if (countryIsoCode == null || passwordMap.isEmpty()) {
            return passwords;
        }

        return passwordMap.get(gbToUK(countryIsoCode));
    }

    public String getUserName(final String countryIsoCode) {
        if (countryIsoCode == null || userMap.isEmpty()) {
            return userNames;
        }

        return userMap.get(countryIsoCode);
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }
}
