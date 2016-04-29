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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;

public class WebPaymentFrontend extends PaymentInfo {

    private String shipBeforeDate;
    private String skinCode;
    private String orderData;
    private String sessionValidity;
    private String merchantReturnData;
    private String allowedMethods;
    private String blockedMethods;
    private String brandCode;
    private String issuerId;
    private String offerEmail;
    private String resURL;

    public String getShipBeforeDate() {
        return shipBeforeDate;
    }

    public void setShipBeforeDate(final String shipBeforeDate) {
        this.shipBeforeDate = shipBeforeDate;
    }

    public String getSkinCode() {
        return skinCode;
    }

    public void setSkinCode(final String skinCode) {
        this.skinCode = skinCode;
    }

    public String getOrderData() {
        return orderData;
    }

    public void setOrderData(final String orderData) {
        this.orderData = orderData;
    }

    public String getSessionValidity() {
        return sessionValidity;
    }

    public void setSessionValidity(final String sessionValidity) {
        this.sessionValidity = sessionValidity;
    }

    public String getMerchantReturnData() {
        return merchantReturnData;
    }

    public void setMerchantReturnData(final String merchantReturnData) {
        this.merchantReturnData = merchantReturnData;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(final String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getBlockedMethods() {
        return blockedMethods;
    }

    public void setBlockedMethods(final String blockedMethods) {
        this.blockedMethods = blockedMethods;
    }

    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(final String brandCode) {
        this.brandCode = brandCode;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(final String issuerId) {
        this.issuerId = issuerId;
    }

    public String getOfferEmail() {
        return offerEmail;
    }

    public void setOfferEmail(final String offerEmail) {
        this.offerEmail = offerEmail;
    }

    public String getResURL() {
        return resURL;
    }

    public void setResURL(final String resURL) {
        this.resURL = resURL;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebPaymentFrontend{");
        sb.append("shipBeforeDate='").append(shipBeforeDate).append('\'');
        sb.append(", skinCode='").append(skinCode).append('\'');
        sb.append(", orderData='").append(orderData).append('\'');
        sb.append(", sessionValidity='").append(sessionValidity).append('\'');
        sb.append(", merchantReturnData='").append(merchantReturnData).append('\'');
        sb.append(", allowedMethods='").append(allowedMethods).append('\'');
        sb.append(", blockedMethods='").append(blockedMethods).append('\'');
        sb.append(", brandCode='").append(brandCode).append('\'');
        sb.append(", issuerId='").append(issuerId).append('\'');
        sb.append(", offerEmail='").append(offerEmail).append('\'');
        sb.append(", resURL='").append(resURL).append('\'');
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
        if (!super.equals(o)) {
            return false;
        }

        final WebPaymentFrontend that = (WebPaymentFrontend) o;

        if (shipBeforeDate != null ? !shipBeforeDate.equals(that.shipBeforeDate) : that.shipBeforeDate != null) {
            return false;
        }
        if (skinCode != null ? !skinCode.equals(that.skinCode) : that.skinCode != null) {
            return false;
        }
        if (orderData != null ? !orderData.equals(that.orderData) : that.orderData != null) {
            return false;
        }
        if (sessionValidity != null ? !sessionValidity.equals(that.sessionValidity) : that.sessionValidity != null) {
            return false;
        }
        if (merchantReturnData != null ? !merchantReturnData.equals(that.merchantReturnData) : that.merchantReturnData != null) {
            return false;
        }
        if (allowedMethods != null ? !allowedMethods.equals(that.allowedMethods) : that.allowedMethods != null) {
            return false;
        }
        if (blockedMethods != null ? !blockedMethods.equals(that.blockedMethods) : that.blockedMethods != null) {
            return false;
        }
        if (brandCode != null ? !brandCode.equals(that.brandCode) : that.brandCode != null) {
            return false;
        }
        if (issuerId != null ? !issuerId.equals(that.issuerId) : that.issuerId != null) {
            return false;
        }
        if (offerEmail != null ? !offerEmail.equals(that.offerEmail) : that.offerEmail != null) {
            return false;
        }
        return resURL != null ? resURL.equals(that.resURL) : that.resURL == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (shipBeforeDate != null ? shipBeforeDate.hashCode() : 0);
        result = 31 * result + (skinCode != null ? skinCode.hashCode() : 0);
        result = 31 * result + (orderData != null ? orderData.hashCode() : 0);
        result = 31 * result + (sessionValidity != null ? sessionValidity.hashCode() : 0);
        result = 31 * result + (merchantReturnData != null ? merchantReturnData.hashCode() : 0);
        result = 31 * result + (allowedMethods != null ? allowedMethods.hashCode() : 0);
        result = 31 * result + (blockedMethods != null ? blockedMethods.hashCode() : 0);
        result = 31 * result + (brandCode != null ? brandCode.hashCode() : 0);
        result = 31 * result + (issuerId != null ? issuerId.hashCode() : 0);
        result = 31 * result + (offerEmail != null ? offerEmail.hashCode() : 0);
        result = 31 * result + (resURL != null ? resURL.hashCode() : 0);
        return result;
    }
}
