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

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UserDataMappingServiceTest {

    @Test(groups = "fast")
    public void testToCustomerIdForCustomerId() throws Exception {
        final String customerId = "customerId";
        final String externalKey = "externalKey";
        final UUID accountId = UUID.randomUUID();

        final Account account = mock(Account.class);
        when(account.getExternalKey()).thenReturn(externalKey);
        when(account.getId()).thenReturn(accountId);

        Optional<String> customerIdOptional = UserDataMappingService.toCustomerId(null, customerId, account);
        assertTrue(customerIdOptional.isPresent());
        assertEquals(customerIdOptional.get(), customerId);
    }

    @Test(groups = "fast")
    public void testToCustomerIdForExternalKey() throws Exception {
        final String customerId = null;
        final String externalKey = "externalKey";
        final UUID accountId = UUID.randomUUID();

        final Account account = mock(Account.class);
        when(account.getExternalKey()).thenReturn(externalKey);
        when(account.getId()).thenReturn(accountId);

        Optional<String> customerIdOptional = UserDataMappingService.toCustomerId(null, customerId, account);
        assertTrue(customerIdOptional.isPresent());
        assertEquals(customerIdOptional.get(), externalKey);
    }

    @Test(groups = "fast")
    public void testToCustomerIdForAccountId() throws Exception {
        final String customerId = null;
        final String externalKey = null;
        final UUID accountId = UUID.randomUUID();

        final Account account = mock(Account.class);
        when(account.getExternalKey()).thenReturn(externalKey);
        when(account.getId()).thenReturn(accountId);

        Optional<String> customerIdOptional = UserDataMappingService.toCustomerId(null, customerId, account);
        assertTrue(customerIdOptional.isPresent());
        assertEquals(customerIdOptional.get(), accountId.toString());
    }

    @Test(groups = "fast")
    public void testToCustomerIdForNoAccount() throws Exception {
        final String customerId = null;
        final Account account = null;

        Optional<String> customerIdOptional = UserDataMappingService.toCustomerId(null, customerId, account);
        assertFalse(customerIdOptional.isPresent());
    }

    @Test(groups = "fast")
    public void testToCustomerLocalForCustomerLocale() {
        final String customerLocaleProperty = "de";
        final String accountLocaleProperty = "en";

        final Account account = mock(Account.class);
        when(account.getLocale()).thenReturn(accountLocaleProperty);

        Optional<Locale> customerLocale = UserDataMappingService.toCustomerLocale(customerLocaleProperty, account);
        assertTrue(customerLocale.isPresent());
        assertEquals(customerLocale.get(), Locale.GERMAN);
    }

    @Test(groups = "fast")
    public void testToCustomerLocalForAccountLocale() {
        final String customerLocaleProperty = null;
        final String accountLocaleProperty = "en";

        final Account account = mock(Account.class);
        when(account.getLocale()).thenReturn(accountLocaleProperty);

        Optional<Locale> customerLocale = UserDataMappingService.toCustomerLocale(customerLocaleProperty, account);
        assertTrue(customerLocale.isPresent());
        assertEquals(customerLocale.get(), Locale.ENGLISH);
    }

    @Test(groups = "fast")
    public void testToCustomerLocalForNoAccount() {
        final String customerLocaleProperty = null;
        final Account account = null;

        Optional<Locale> customerLocale = UserDataMappingService.toCustomerLocale(customerLocaleProperty, account);
        assertFalse(customerLocale.isPresent());
    }

    @Test(groups = "fast")
    public void testToCustomerLocalForNoAccountLocale() {
        final String customerLocaleProperty = null;

        final Account account = mock(Account.class);
        when(account.getLocale()).thenReturn(null);

        Optional<Locale> customerLocale = UserDataMappingService.toCustomerLocale(customerLocaleProperty, account);
        assertFalse(customerLocale.isPresent());
    }

    @Test(groups = "fast")
    public void testToCustomerEmailForCustomerEmail() {
        final String customerEmailProperty = "erl@koenig.de";
        final String accountEmailProperty = "kind@koenig.de";

        final Account account = mock(Account.class);
        when(account.getEmail()).thenReturn(accountEmailProperty);

        Optional<String> customerEmail = UserDataMappingService.toCustomerEmail(customerEmailProperty, account);
        assertTrue(customerEmail.isPresent());
        assertEquals(customerEmailProperty, customerEmail.get());
    }

    @Test(groups = "fast")
    public void testToCustomerEmailForAccountEmail() {
        final String customerEmailProperty = null;
        final String accountEmailProperty = "kind@koenig.de";

        final Account account = mock(Account.class);
        when(account.getEmail()).thenReturn(accountEmailProperty);

        Optional<String> customerEmail = UserDataMappingService.toCustomerEmail(customerEmailProperty, account);
        assertTrue(customerEmail.isPresent());
        assertEquals(accountEmailProperty, customerEmail.get());
    }

    @Test(groups = "fast")
    public void testToCustomerEmailForNoAccount() {
        final String customerEmailProperty = null;
        final Account account = null;

        Optional<String> customerEmail = UserDataMappingService.toCustomerEmail(customerEmailProperty, account);
        assertFalse(customerEmail.isPresent());
    }

    @Test(groups = "fast")
    public void testToCustomerEmailForNoAccountEmail() {
        final String customerEmailProperty = null;

        final Account account = mock(Account.class);
        when(account.getEmail()).thenReturn(null);

        Optional<String> customerEmail = UserDataMappingService.toCustomerEmail(customerEmailProperty, account);
        assertFalse(customerEmail.isPresent());
    }


    @Test(groups = "fast")
    public void testToFirstNameForPropertyFirstName() {
        final String propertyFirstName = "Hans";
        final String accountName = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(3);

        Optional<String> firstName = UserDataMappingService.toFirstName(propertyFirstName, account);
        assertTrue(firstName.isPresent());
        assertEquals(firstName.get(), propertyFirstName);
    }

    @Test(groups = "fast")
    public void testToFirstNameForAccountNameWithFirstNameLength() {
        final String firstNameProperty = null;
        final String accountName = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(3);

        Optional<String> firstName = UserDataMappingService.toFirstName(firstNameProperty, account);
        assertTrue(firstName.isPresent());
        assertEquals(firstName.get(), "Erl");
    }

    @Test(groups = "fast")
    public void testToFirstNameForAccountNameWithoutFirstNameLength() {
        final String firstNameProperty = null;
        final String accountName = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(null);

        Optional<String> firstName = UserDataMappingService.toFirstName(firstNameProperty, account);
        assertTrue(firstName.isPresent());
        assertEquals(firstName.get(), "Erl Koenig");
    }

    @Test(groups = "fast")
    public void testToFirstNameForNoAccountName() {
        final String firstNameProperty = null;
        final String accountName = null;

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(3);

        Optional<String> firstName = UserDataMappingService.toFirstName(firstNameProperty, account);
        assertFalse(firstName.isPresent());
    }

    @Test(groups = "fast")
    public void testToFirstNameForNoAccount() {
        final String firstNameProperty = null;
        final Account account = null;

        Optional<String> firstName = UserDataMappingService.toFirstName(firstNameProperty, account);
        assertFalse(firstName.isPresent());
    }

    @Test(groups = "fast")
    public void testToLastNameForPropertyLastName() {
        final String lastNameProperty = "Meier";
        final String accountName = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(3);

        Optional<String> lastName = UserDataMappingService.toLastName(lastNameProperty, account);
        assertTrue(lastName.isPresent());
        assertEquals(lastName.get(), lastNameProperty);
    }

    @Test(groups = "fast")
    public void testToLastNameForAccountNameWithFirstNameLength() {
        final String lastNameProperty = null;
        final String accountName = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(3);

        Optional<String> lastName = UserDataMappingService.toLastName(lastNameProperty, account);
        assertTrue(lastName.isPresent());
        assertEquals(lastName.get(), "Koenig");
    }

    @Test(groups = "fast")
    public void testToLastNameForPropertyAccountNameWithNoFirstNameLength() {
        final String lastNameProperty = null;
        final String accountName = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(null);

        Optional<String> lastName = UserDataMappingService.toLastName(lastNameProperty, account);
        assertTrue(lastName.isPresent());
        assertEquals(lastName.get(), "");
    }

    @Test(groups = "fast")
    public void testToLastNameForNoAccountName() {
        final String lastNameProperty = null;
        final String accountName = null;

        final Account account = mock(Account.class);
        when(account.getName()).thenReturn(accountName);
        when(account.getFirstNameLength()).thenReturn(3);

        Optional<String> lastName = UserDataMappingService.toLastName(lastNameProperty, account);
        assertFalse(lastName.isPresent());
    }

    @Test(groups = "fast")
    public void testToLastNameForNoAccount() {
        final String lastNameProperty = null;
        final Account account = null;

        Optional<String> lastName = UserDataMappingService.toLastName(lastNameProperty, account);
        assertFalse(lastName.isPresent());
    }

    @Test(groups = "fast")
    public void testToUserDataForPluginProperties() throws Exception {
        final String customerIdProperty = "customerId";
        final String customerLocaleProperty = "de";
        final String customerEmailProperty = "erl@koenig.de";
        final String customerFirstNameProperty = "Hans";
        final String customerLastNameProperty = "Meier";
        final String customerIpProperty = "1.2.3.4";

        List<PluginProperty> pluginProperties = ImmutableList.of(
                new PluginProperty(UserDataMappingService.PROPERTY_CUSTOMER_ID, customerIdProperty, false),
                new PluginProperty(UserDataMappingService.PROPERTY_CUSTOMER_LOCALE,  customerLocaleProperty, false),
                new PluginProperty(UserDataMappingService.PROPERTY_EMAIL, customerEmailProperty, false),
                new PluginProperty(UserDataMappingService.PROPERTY_FIRST_NAME,  customerFirstNameProperty, false),
                new PluginProperty(UserDataMappingService.PROPERTY_LAST_NAME,  customerLastNameProperty, false),
                new PluginProperty(UserDataMappingService.PROPERTY_IP, customerIpProperty, false));

        UserData userData = UserDataMappingService.toUserData(null, pluginProperties);
        assertEquals(userData.getCustomerId(), customerIdProperty);
        assertEquals(userData.getCustomerLocale().toString(), customerLocaleProperty);
        assertEquals(userData.getEmail(), customerEmailProperty);
        assertEquals(userData.getFirstName(), customerFirstNameProperty);
        assertEquals(userData.getLastName(), customerLastNameProperty);
        assertEquals(userData.getIP(), customerIpProperty);
    }

    @Test(groups = "fast")
    public void testToUserDataForAccount() throws Exception {
        final String externalKey = "customerId";
        final String accountLocale = "en";
        final String email = "erl@koenig.de";
        final String name = "Erl Koenig";

        final Account account = mock(Account.class);
        when(account.getExternalKey()).thenReturn(externalKey);
        when(account.getLocale()).thenReturn(accountLocale);
        when(account.getEmail()).thenReturn(email);
        when(account.getName()).thenReturn(name);
        when(account.getFirstNameLength()).thenReturn(3);

        final String customerIpProperty = "1.2.3.4";

        List<PluginProperty> pluginProperties =
                ImmutableList.of(new PluginProperty(UserDataMappingService.PROPERTY_IP, customerIpProperty, false));

        UserData userData = UserDataMappingService.toUserData(account, pluginProperties);
        assertEquals(userData.getCustomerId(), externalKey);
        assertEquals(userData.getCustomerLocale().toString(), accountLocale);
        assertEquals(userData.getEmail(), email);
        assertEquals(userData.getFirstName(), "Erl");
        assertEquals(userData.getLastName(), "Koenig");
        assertEquals(userData.getIP(), customerIpProperty);
    }


    @Test(groups = "fast")
    public void testToUserDataForZeroInput() throws Exception {
        List<PluginProperty> pluginProperties = ImmutableList.of();

        UserData userData = UserDataMappingService.toUserData(null, pluginProperties);
        assertEquals(userData.getCustomerId(), null);
        assertEquals(userData.getCustomerLocale(), null);
        assertEquals(userData.getEmail(), null);
        assertEquals(userData.getFirstName(), null);
        assertEquals(userData.getLastName(), null);
        assertEquals(userData.getIP(), null);
    }
}