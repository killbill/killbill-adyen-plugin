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

package org.killbill.billing.plugin.adyen.client.model;

import java.util.Locale;

import org.joda.time.DateTime;

public class UserData {

    private String shopperEmail;
    private String shopperReference;
    private Locale shopperLocale;
    private String firstName;
    private String infix;
    private String lastName;
    private String gender;
    private String telephoneNumber;
    private String socialSecurityNumber;
    private DateTime dateOfBirth;
    private String shopperIP;

    public String getShopperEmail() {
        return shopperEmail;
    }

    public void setShopperEmail(final String shopperEmail) {
        this.shopperEmail = shopperEmail;
    }

    public String getShopperReference() {
        return shopperReference;
    }

    public void setShopperReference(final String shopperReference) {
        this.shopperReference = shopperReference;
    }

    public Locale getShopperLocale() {
        return shopperLocale;
    }

    public void setShopperLocale(final Locale shopperLocale) {
        this.shopperLocale = shopperLocale;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getInfix() {
        return infix;
    }

    public void setInfix(final String infix) {
        this.infix = infix;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(final String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public String getSocialSecurityNumber() {
        return socialSecurityNumber;
    }

    public void setSocialSecurityNumber(final String socialSecurityNumber) {
        this.socialSecurityNumber = socialSecurityNumber;
    }

    public DateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final DateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getShopperIP() {
        return shopperIP;
    }

    public void setShopperIP(final String shopperIP) {
        this.shopperIP = shopperIP;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserData{");
        sb.append("shopperEmail='").append(shopperEmail).append('\'');
        sb.append(", shopperReference='").append(shopperReference).append('\'');
        sb.append(", shopperLocale=").append(shopperLocale);
        sb.append(", firstName='").append(firstName).append('\'');
        sb.append(", infix='").append(infix).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", gender='").append(gender).append('\'');
        sb.append(", telephoneNumber='").append(telephoneNumber).append('\'');
        sb.append(", socialSecurityNumber='").append(socialSecurityNumber).append('\'');
        sb.append(", dateOfBirth=").append(dateOfBirth);
        sb.append(", shopperIP='").append(shopperIP).append('\'');
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

        if (shopperEmail != null ? !shopperEmail.equals(userData.shopperEmail) : userData.shopperEmail != null) {
            return false;
        }
        if (shopperReference != null ? !shopperReference.equals(userData.shopperReference) : userData.shopperReference != null) {
            return false;
        }
        if (shopperLocale != null ? !shopperLocale.equals(userData.shopperLocale) : userData.shopperLocale != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(userData.firstName) : userData.firstName != null) {
            return false;
        }
        if (infix != null ? !infix.equals(userData.infix) : userData.infix != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(userData.lastName) : userData.lastName != null) {
            return false;
        }
        if (gender != null ? !gender.equals(userData.gender) : userData.gender != null) {
            return false;
        }
        if (telephoneNumber != null ? !telephoneNumber.equals(userData.telephoneNumber) : userData.telephoneNumber != null) {
            return false;
        }
        if (socialSecurityNumber != null ? !socialSecurityNumber.equals(userData.socialSecurityNumber) : userData.socialSecurityNumber != null) {
            return false;
        }
        if (dateOfBirth != null ? !dateOfBirth.equals(userData.dateOfBirth) : userData.dateOfBirth != null) {
            return false;
        }
        return shopperIP != null ? shopperIP.equals(userData.shopperIP) : userData.shopperIP == null;

    }

    @Override
    public int hashCode() {
        int result = shopperEmail != null ? shopperEmail.hashCode() : 0;
        result = 31 * result + (shopperReference != null ? shopperReference.hashCode() : 0);
        result = 31 * result + (shopperLocale != null ? shopperLocale.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (infix != null ? infix.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (telephoneNumber != null ? telephoneNumber.hashCode() : 0);
        result = 31 * result + (socialSecurityNumber != null ? socialSecurityNumber.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + (shopperIP != null ? shopperIP.hashCode() : 0);
        return result;
    }
}
