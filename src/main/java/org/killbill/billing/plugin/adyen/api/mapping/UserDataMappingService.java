/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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
package org.killbill.billing.plugin.adyen.api.mapping;

import java.util.Locale;

import javax.annotation.Nullable;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.api.PluginProperties;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

public class UserDataMappingService {

    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_IP = "ip";
    public static final String PROPERTY_CUSTOMER_LOCALE = "customerLocale";
    public static final String PROPERTY_CUSTOMER_ID = "customerId";
    public static final String PROPERTY_EMAIL = "email";

    public static UserData toUserData(@Nullable final Account account, final Iterable<PluginProperty> properties) {
        final UserData userData = new UserData();

        // determine the customer id
        final String customerIdProperty = PluginProperties.findPluginPropertyValue(PROPERTY_CUSTOMER_ID, properties);
        final Optional<String> optionalCustomerId = toCustomerId( customerIdProperty, account);
        final String customerId = optionalCustomerId.isPresent() ? optionalCustomerId.get() : null;
        userData.setCustomerId(customerId);

        // determine the customer locale
        final String propertyLocaleString = PluginProperties.findPluginPropertyValue(PROPERTY_CUSTOMER_LOCALE, properties);
        final Optional<Locale> customerLocaleOptional = toCustomerLocale(propertyLocaleString, account);
        final Locale customerLocale = customerLocaleOptional.isPresent() ? customerLocaleOptional.get() : null;
        userData.setCustomerLocale(customerLocale);

        // determine the email
        final String propertyEmail = PluginProperties.findPluginPropertyValue(PROPERTY_EMAIL, properties);
        final Optional<String> optionalEmail = toCustomerEmail(propertyEmail, account);
        final String email = optionalEmail.isPresent() ? optionalEmail.get() : null;
        userData.setEmail(email);

        // determine first Name
        final String propertyFirstName = PluginProperties.findPluginPropertyValue(PROPERTY_FIRST_NAME, properties);
        final Optional<String> optionalFirstName = toFirstName(propertyFirstName, account);
        final String firstName = optionalFirstName.isPresent() ? optionalFirstName.get() : null;
        userData.setFirstName(firstName);

        // determine last Name
        final String propertyLastName = PluginProperties.findPluginPropertyValue(PROPERTY_LAST_NAME, properties);
        final Optional<String> optionalLastName = toLastName(propertyLastName, account);
        final String lastName = optionalLastName.isPresent() ? optionalLastName.get() : null;
        userData.setLastName(lastName);

        // set ip
        userData.setIP(PluginProperties.findPluginPropertyValue(PROPERTY_IP, properties));

        return userData;
    }

    /**
     *  This function determines the customer id that will be used in the communication with Adyen.
     *
     *  The following heuristic is used to determine it:
     *  1. If there was a customerId in the plugin properties this will be used.
     *  2. If the account has an externalKey this will be used.
     *  3. The id of the account is used.
     *  4. In the unlikely case that there is no account, this function returns an empty Optional.
     *
     * @param customerId the customerId that has been sent in the plugin properties
     * @param account the Kill Bill account
     * @return the customer id as an Optional
     */
    public static Optional<String> toCustomerId( String customerId, Account account) {
        if (customerId != null) {
            return Optional.of(customerId);
        } else if (account != null) {
            if (account.getExternalKey() != null) {
                return Optional.of(account.getExternalKey());
            } else {
                return Optional.of(account.getId().toString());
            }
        } else {
            return Optional.absent();
        }
    }

    /**
     * This function determines the customer locale that will be used in the communication with Adyen.
     *
     * The following heuristic is used to determine it:
     * 1. if there is a local provided in the plugin properties this will be used
     * 2. if there the account has a locale, that will be used
     * 3. if there should be no account or an account without locale, an empty optional will be returned
     *
     * @param propertyLocaleString the locale that has been sent in the plugin properties
     * @param account the Kill Bill account
     * @return the locale as an Optional
     */
    public static Optional<Locale> toCustomerLocale(String propertyLocaleString, Account account) {
        if (propertyLocaleString != null) {
            return Optional.of(Locale.forLanguageTag(propertyLocaleString));
        } else if (account != null && account.getLocale() != null) {
            return Optional.of(new Locale(account.getLocale()));
        } else {
            return Optional.absent();
        }
    }

    /**
     * This function determines the customer locale that will be used in the communication with Adyen.
     *
     * The following heuristic is used to determine it:
     * 1. if there is an email provided in the plugin properties, this will be used.
     * 2. If the account has an email, this will be used.
     * 3. If there is no account or no email for the account an empty Optional will be returned.
     *
     * @param propertyEmail the email that has been sent in the plugin properties
     * @param account the Kill Bill account
     * @return the email as an Optional
     */
    public static Optional<String> toCustomerEmail(String propertyEmail, Account account) {
        if (propertyEmail != null) {
            return Optional.of(propertyEmail);
        } else if (account != null && account.getEmail() != null) {
            return Optional.of(account.getEmail());
        } else {
            return Optional.absent();
        }
    }

    /**
     * This function determines the first name of the customer that will be used in the communication with Adyen.
     *
     * The following heuristic is used to determine it:
     * 1. if there is an first name provided in the plugin properties, this will be used.
     * 2. if the account has a name, the substring is used that is indicated by firstNameLength
     * 3. if no firstNameLenght was provided the complete name is used as first name.
     * 4. if no account or account name was provided an empty Optional is returned.
     *
     *
     * @param propertyFirstName the firstName that has been sent in the plugin properties
     * @param account the Kill Bill account
     * @return the first Name as an Optional
     */
    public static Optional<String> toFirstName(String propertyFirstName, Account account) {
        if (propertyFirstName != null) {
            return Optional.of(propertyFirstName);
        } else if (account != null && account.getName() != null) {
            return Optional.of(account.getName().substring(0, MoreObjects.firstNonNull(account.getFirstNameLength(), account.getName().length())).trim());
        } else {
            return Optional.absent();
        }
    }

    /**
     * This function determines the first name of the customer that will be used in the communication with Adyen.
     *
     * The following heuristic is used to determine it:
     * 1. if there is an last name provided in the plugin properties, this will be used.
     * 2. if the account has a name, the substring is used that is derived from the firstNameLength
     * 3. if no firstNameLenght was provided the complete name is used as last name.
     * 4. if no account or account name was provided an empty Optional is returned.
     *
     * @param propertyLastName the lastName that has been sent in the plugin properties
     * @param account the Kill Bill account
     * @return the first Name as an Optional
     */
    public static Optional<String> toLastName(String propertyLastName, Account account) {
        if (propertyLastName != null) {
            return Optional.of(propertyLastName);
        } else if (account != null && account.getName() != null) {
            return Optional.of(account.getName().substring(MoreObjects.firstNonNull(account.getFirstNameLength(), account.getName().length())).trim());
        } else {
            return Optional.absent();
        }
    }
}
