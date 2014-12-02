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

import java.util.Locale;

/**
 * User-specific data required for payment processing.
 */
public class UserData {

    private String email;
    private String customerId;
    private Locale customerLocale;
    private String firstName;
    private String lastName;
    private String IP;

    public String getEmail() {
        return email;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Locale getCustomerLocale() {
        return customerLocale;
    }

    /**
     * @return M for male or F for female
     */
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getIP() {
        return IP;
    }

    public void setCustomerId(final String customerId) {
        this.customerId = customerId;
    }

    public void setCustomerLocale(final Locale customerLocale) {
        this.customerLocale = customerLocale;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setIP(final String IP) {
        this.IP = IP;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserData{");
        sb.append("email='").append(email).append('\'');
        sb.append(", customerId='").append(customerId).append('\'');
        sb.append(", customerLocale=").append(customerLocale);
        sb.append(", firstName='").append(firstName).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", IP='").append(IP).append('\'');
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

        final UserData userData = (UserData) o;

        if (IP != null ? !IP.equals(userData.IP) : userData.IP != null) {
            return false;
        }
        if (customerId != null ? !customerId.equals(userData.customerId) : userData.customerId != null) {
            return false;
        }
        if (customerLocale != null ? !customerLocale.equals(userData.customerLocale) : userData.customerLocale != null) {
            return false;
        }
        if (email != null ? !email.equals(userData.email) : userData.email != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(userData.firstName) : userData.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(userData.lastName) : userData.lastName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (customerLocale != null ? customerLocale.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (IP != null ? IP.hashCode() : 0);
        return result;
    }
}
