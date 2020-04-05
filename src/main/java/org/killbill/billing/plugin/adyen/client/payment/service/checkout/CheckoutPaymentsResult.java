package org.killbill.billing.plugin.adyen.client.payment.service.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CheckoutPaymentsResult {
    private String resultCode;
    private String paymentData;
    private String pspReference;
    private String paymentMethod;
    private String formMethod;
    private String formUrl;
    private Map<String, String> formParameter;
    private Map<String, String> authResultKeys;
    private String merchantAccount;
    private String paymentExternalKey;
    private KlarnaPaymentInfo paymentInfo;
    private final Logger logger;

    public CheckoutPaymentsResult() {
        this.logger = LoggerFactory.getLogger(CheckoutPaymentsResult.class);
    }

    public String getResultCode() {
        return resultCode;
    }
    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getPaymentData() {
        return paymentData;
    }
    public void setPaymentData(String paymentData) {
        this.paymentData = paymentData;
    }

    public String getPspReference() {
        return pspReference;
    }
    public void setPspReference(String pspReference) {
        this.pspReference = pspReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getFormUrl() {
        return formUrl;
    }
    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }

    public String getFormMethod() {
        return formMethod;
    }
    public void setFormMethod(String formMethod) {
        this.formMethod = formMethod;
    }

    public Map<String, String> getFormParameter() {
        return formParameter;
    }
    public void setFormParameter(Map<String, String> formParameter) {
        this.formParameter = formParameter;
    }

    public Map<String, String> getAuthResultKeys() {
        return authResultKeys;
    }
    public void setAuthResultKeys(Map<String, String> authResultKeys) {
        this.authResultKeys = authResultKeys;
    }

    public String getMerchantAccount() { return merchantAccount; }
    public void setMerchantAccount(final String merchantAccount) { this.merchantAccount = merchantAccount; }

    public String getPaymentExternalKey() { return paymentExternalKey; }
    public void setPaymentExternalKey(final String paymentExternalKey) { this.paymentExternalKey = paymentExternalKey; }

    public KlarnaPaymentInfo getPaymentInfo() { return paymentInfo; }
    public void setPaymentInfo(final KlarnaPaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }

    public PurchaseResult toPurchaseResult() {
        //convert the result to PurchaseResult
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> additionalData = new HashMap<>();

        if(paymentData != null) {
            additionalData.put("paymentData", paymentData);
        }
        if(paymentMethod != null) {
            additionalData.put("paymentMethod", paymentMethod);
        }
        if(formUrl != null) {
            additionalData.put("formUrl", formUrl);
        }
        if(formMethod != null) {
            additionalData.put("formMethod", formMethod);
        }
        if(merchantAccount != null) {
            additionalData.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, merchantAccount);
        }
        if(paymentInfo.getIdentifierMap() != null) {
            try {
                String hashedIdStr = mapper.writeValueAsString(paymentInfo.getIdentifierMap());
                additionalData.put("identifierMap", hashedIdStr);
            } catch (JsonProcessingException ex) {
                logger.error("Failed to save hashIdentifiers: " + ex.getMessage());
            }
        }
        if(formParameter != null) {
            try {
                String formParams = mapper.writeValueAsString(formParameter);
                additionalData.put("formParameter", formParams);
            } catch (JsonProcessingException ex) {
                logger.error("Failed to save formParameters: " + ex.getMessage());
            }
        }
        if(authResultKeys != null) {
            try {
                String authKeyNames = mapper.writeValueAsString(authResultKeys);
                additionalData.put("resultKeys", authKeyNames);
            } catch (JsonProcessingException ex) {
                logger.warn("Failed to save details keys: " + ex.getMessage());
            }
        }

        final PaymentServiceProviderResult pspResultCode =
                PaymentServiceProviderResult.getPaymentResultForId(resultCode);
        return new PurchaseResult(pspResultCode,
                null,
                pspReference,
                null,
                resultCode,
                paymentExternalKey,
                null,
                null,
                additionalData);
    }

    @Override
    public String toString() {
        return "CheckoutPaymentsResult{" +
               "resultCode='" + resultCode + '\'' +
               ", paymentData='" + paymentData + '\'' +
               ", pspReference='" + pspReference + '\'' +
               ", paymentMethod='" + paymentMethod + '\'' +
               ", formMethod='" + formMethod + '\'' +
               ", formUrl='" + formUrl + '\'' +
               ", formParameter=" + formParameter +
               ", authResultKeys=" + authResultKeys +
               '}';
    }
}
