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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.util.Locale;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSortedSet;

public class PayPalCountryCodes {

    private static final Set<String> paypalIsoCountryCodes = ImmutableSortedSet.<String>naturalOrder()
                                                                               .add("AU")
                                                                               .add("AT")
                                                                               .add("BE")
                                                                               .add("BR")
                                                                               .add("CA")
                                                                               .add("CH")
                                                                               .add("CN")
                                                                               .add("DE")
                                                                               .add("ES")
                                                                               .add("GB")
                                                                               .add("UK")
                                                                               .add("FR")
                                                                               .add("IT")
                                                                               .add("NL")
                                                                               .add("PL")
                                                                               .add("RU")
                                                                               .add("US")
                                                                               .build();

    private static final Set<String> paypalLocaleCountryCodes = ImmutableSortedSet.<String>naturalOrder()
                                                                                  .add("da_DK")
                                                                                  .add("he_IL")
                                                                                  .add("id_ID")
                                                                                  .add("jp_JP")
                                                                                  .add("no_NO")
                                                                                  .add("pt_BR")
                                                                                  .add("ru_RU")
                                                                                  .add("sv_SE")
                                                                                  .add("th_TH")
                                                                                  .add("tr_TR")
                                                                                  .add("zh_CN")
                                                                                  .add("zh_HK")
                                                                                  .add("zh_TW")
                                                                                  .build();

    public static boolean isNotPayPalIsoCode(final String countryIsoCode) {
        return Collections2.filter(paypalIsoCountryCodes, new Predicate<String>() {
            @Override
            public boolean apply(final String country) {
                return country != null && country.equalsIgnoreCase(countryIsoCode);
            }
        }).isEmpty();
    }

    public static boolean isNotPayPalLocale(final Locale customerLocale) {
        return Collections2.filter(paypalLocaleCountryCodes, new Predicate<String>() {
            @Override
            public boolean apply(final String locale) {

                return locale != null && locale.equalsIgnoreCase(customerLocale.toString());
            }
        }).isEmpty();
    }
}
