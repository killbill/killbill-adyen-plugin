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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.FraudCheckResult;
import org.killbill.adyen.payment.FraudResult;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.ADYEN_CALL_ERROR_STATUS;
import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.EXCEPTION_CLASS;
import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.EXCEPTION_MESSAGE;
import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.UNKNOWN;

public class AdyenPaymentServiceProviderPort extends BaseAdyenPaymentServiceProviderPort implements Closeable {

    private final AdyenRequestFactory adyenRequestFactory;
    private final AdyenPaymentRequestSender adyenPaymentRequestSender;

    public AdyenPaymentServiceProviderPort(final AdyenRequestFactory adyenRequestFactory,
                                           final AdyenPaymentRequestSender adyenPaymentRequestSender) {
        this.adyenRequestFactory = adyenRequestFactory;
        this.adyenPaymentRequestSender = adyenPaymentRequestSender;

        this.logger = LoggerFactory.getLogger(AdyenPaymentServiceProviderPort.class);
    }

    @Override
    public void close() throws IOException {
        adyenPaymentRequestSender.close();
    }

    public PurchaseResult authorise(final String merchantAccount,
                                    final PaymentData paymentData,
                                    final UserData userData,
                                    final SplitSettlementData splitSettlementData,
                                    final Map<String, String> additionalData) {
        return authoriseOrCredit(true, merchantAccount, paymentData, userData, splitSettlementData, additionalData);
    }

    public PurchaseResult credit(final String merchantAccount,
                                 final PaymentData paymentData,
                                 final UserData userData,
                                 final SplitSettlementData splitSettlementData,
                                 final Map<String, String> additionalData) {
        return authoriseOrCredit(false, merchantAccount, paymentData, userData, splitSettlementData, additionalData);
    }

    private PurchaseResult authoriseOrCredit(final boolean authorize,
                                             final String merchantAccount,
                                             final PaymentData paymentData,
                                             final UserData userData,
                                             final SplitSettlementData splitSettlementData,
                                             final Map<String, String> additionalData) {
        final String operation = authorize ? "authorize" : "credit";
        final PaymentRequest request = adyenRequestFactory.createPaymentRequest(merchantAccount, paymentData, userData, splitSettlementData, additionalData);

        final AdyenCallResult<PaymentResult> adyenCallResult;
        if (authorize) {
            adyenCallResult = adyenPaymentRequestSender.authorise(merchantAccount, request);
        } else {
            adyenCallResult = adyenPaymentRequestSender.refundWithData(merchantAccount, request);
        }

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase(operation, userData, merchantAccount, paymentData, adyenCallResult);
        }

        final PurchaseResult purchaseResult;
        final PaymentResult result = adyenCallResult.getResult().get();
        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode());
        if (paymentServiceProviderResult != PaymentServiceProviderResult.REDIRECT_SHOPPER) {
            purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                                                result.getAuthCode(),
                                                result.getPspReference(),
                                                result.getRefusalReason(),
                                                result.getResultCode(),
                                                paymentData.getPaymentTransactionExternalKey(),
                                                getAdditionalData(result, merchantAccount));
        } else {
            final Map<String, String> formParams = new HashMap<String, String>();
            formParams.put(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result.getPaRequest());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_MD, result.getMd());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result.getDccAmount() == null ? null : String.valueOf(result.getDccAmount().getValue()));
            formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result.getDccAmount() == null ? null : result.getDccAmount().getCurrency());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result.getDccSignature());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result.getIssuerUrl());
            formParams.putAll(extractMpiAdditionalData(result));
            formParams.put(AdyenPaymentPluginApi.PROPERTY_TERM_URL, paymentData.getPaymentInfo().getTermUrl());

            purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                                                result.getAuthCode(),
                                                result.getPspReference(),
                                                result.getRefusalReason(),
                                                result.getResultCode(),
                                                paymentData.getPaymentTransactionExternalKey(),
                                                result.getIssuerUrl(),
                                                formParams,
                                                getAdditionalData(result, merchantAccount));
        }

        logTransaction(operation, userData, merchantAccount, paymentData, purchaseResult, adyenCallResult);
        return purchaseResult;
    }

    private Map<String, String> getAdditionalData(final PaymentResult result, final String merchantAccount) {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        additionalDataMap.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, merchantAccount);
        additionalDataMap.putAll(anyType2AnyTypeMapToStringMap(result.getAdditionalData()));

        // Adyen needs to enable it manually
        if (result.getFraudResult() != null) {
            final FraudResult fraudResult = result.getFraudResult();
            additionalDataMap.put("fraudResult.accountScore", String.valueOf(fraudResult.getAccountScore()));

            if (fraudResult.getResults() != null) {
                final List<FraudCheckResult> fraudCheckResults = fraudResult.getResults().getFraudCheckResult();
                for (int i = 0; i < fraudCheckResults.size(); i++) {
                    final FraudCheckResult fraudCheckResult = fraudCheckResults.get(i);
                    final String key = "fraudResult." + i + ".";
                    additionalDataMap.put(key + "accountScore", String.valueOf(fraudCheckResult.getAccountScore()));
                    additionalDataMap.put(key + "checkId", String.valueOf(fraudCheckResult.getCheckId()));
                    additionalDataMap.put(key + "name", fraudCheckResult.getName());
                }
            }
        }

        return additionalDataMap;
    }

    private PurchaseResult handleTechnicalFailureAtPurchase(final String transactionType, final UserData userData, final String merchantAccount, final PaymentData paymentData, final AdyenCallResult<PaymentResult> adyenCall) {
        logTransactionError(transactionType, userData, merchantAccount, paymentData, adyenCall);
        return new PurchaseResult(paymentData.getPaymentTransactionExternalKey(), adyenCall);
    }

    public PurchaseResult authorize3DSecure(final String merchantAccount,
                                            final PaymentData paymentData,
                                            final UserData userData,
                                            final SplitSettlementData splitSettlementData,
                                            final Map<String, String> additionalData) {
        final String operation = "authorize3DSecure";
        final PaymentRequest3D request = adyenRequestFactory.paymentRequest3d(merchantAccount,
                                                                              paymentData,
                                                                              userData,
                                                                              splitSettlementData,
                                                                              additionalData);
        final AdyenCallResult<PaymentResult> adyenCallResult = adyenPaymentRequestSender.authorise3D(merchantAccount, request);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase(operation, userData, merchantAccount, paymentData, adyenCallResult);
        }

        final PurchaseResult purchaseResult;
        final PaymentResult result = adyenCallResult.getResult().get();
        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode());
        if (paymentServiceProviderResult != PaymentServiceProviderResult.REDIRECT_SHOPPER) {
            purchaseResult = new PurchaseResult(PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode()),
                                                result.getAuthCode(),
                                                result.getPspReference(),
                                                result.getRefusalReason(),
                                                result.getResultCode(),
                                                paymentData.getPaymentTransactionExternalKey(),
                                                getAdditionalData(result, merchantAccount));
        } else {
            final Map<String, String> formParams = new HashMap<String, String>();
            formParams.put("PaReq", result.getPaRequest());
            formParams.put("MD", result.getMd());

            purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                                                result.getAuthCode(),
                                                result.getPspReference(),
                                                result.getRefusalReason(),
                                                result.getResultCode(),
                                                paymentData.getPaymentTransactionExternalKey(),
                                                result.getIssuerUrl(),
                                                formParams,
                                                getAdditionalData(result, merchantAccount));
        }

        logTransaction(operation, userData, merchantAccount, paymentData, purchaseResult, adyenCallResult);
        return purchaseResult;
    }

    public PaymentModificationResponse refund(final String merchantAccount,
                                              final PaymentData paymentData,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData,
                                              final Map<String, String> additionalData) {
        return modify("refund",
                      new ModificationExecutor() {
                          @Override
                          public AdyenCallResult<ModificationResult> execute(final ModificationRequest modificationRequest) {
                              return adyenPaymentRequestSender.refund(merchantAccount, modificationRequest);
                          }
                      },
                      merchantAccount,
                      paymentData,
                      pspReference,
                      splitSettlementData,
                      additionalData);
    }

    public PaymentModificationResponse cancel(final String merchantAccount,
                                              final PaymentData paymentData,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData,
                                              final Map<String, String> additionalData) {
        return modify("cancel",
                      new ModificationExecutor() {
                          @Override
                          public AdyenCallResult<ModificationResult> execute(final ModificationRequest modificationRequest) {
                              return adyenPaymentRequestSender.cancel(merchantAccount, modificationRequest);
                          }
                      },
                      merchantAccount,
                      paymentData,
                      pspReference,
                      splitSettlementData,
                      additionalData);
    }

    public PaymentModificationResponse capture(final String merchantAccount,
                                               final PaymentData paymentData,
                                               final String pspReference,
                                               final SplitSettlementData splitSettlementData,
                                               final Map<String, String> additionalData) {
        return modify("capture",
                      new ModificationExecutor() {
                          @Override
                          public AdyenCallResult<ModificationResult> execute(final ModificationRequest modificationRequest) {
                              return adyenPaymentRequestSender.capture(merchantAccount, modificationRequest);
                          }
                      },
                      merchantAccount,
                      paymentData,
                      pspReference,
                      splitSettlementData,
                      additionalData);
    }

    private PaymentModificationResponse modify(final String operation,
                                               final ModificationExecutor modificationExecutor,
                                               final String merchantAccount,
                                               final PaymentData paymentData,
                                               final String pspReference,
                                               final SplitSettlementData splitSettlementData,
                                               final Map<String, String> additionalData) {
        final ModificationRequest modificationRequest = adyenRequestFactory.createModificationRequest(merchantAccount, paymentData, pspReference, splitSettlementData, additionalData);
        final AdyenCallResult<ModificationResult> adyenCall = modificationExecutor.execute(modificationRequest);

        final PaymentModificationResponse response;
        if (!adyenCall.receivedWellFormedResponse()) {
            response = new PaymentModificationResponse(pspReference, adyenCall, getModificationAdditionalErrorData(adyenCall, merchantAccount));

            logTransactionError(operation, pspReference, merchantAccount, paymentData, adyenCall);
        } else {
            final ModificationResult result = adyenCall.getResult().get();
            response = new PaymentModificationResponse(result.getResponse(), result.getPspReference(), getModificationAdditionalData(result, merchantAccount));

            logTransaction(operation, pspReference, merchantAccount, paymentData, response, adyenCall);
        }
        response.getAdditionalData().put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, merchantAccount);
        return response;
    }

    private Map<Object, Object> getModificationAdditionalData(final ModificationResult modificationResult, final String merchantAccount) {
        final Map<Object, Object> additionalDataMap = new HashMap<Object, Object>();
        additionalDataMap.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, merchantAccount);
        additionalDataMap.putAll(anyType2AnyTypeMapToStringMap(modificationResult.getAdditionalData()));

        return additionalDataMap;
    }

    private Map<Object, Object> getModificationAdditionalErrorData(final AdyenCallResult<ModificationResult> adyenCallResult, final String merchantAccount) {
        final Map<Object, Object> additionalDataMap = new HashMap<Object, Object>();
        additionalDataMap.put(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, merchantAccount);
        final Optional<AdyenCallErrorStatus> responseStatus = adyenCallResult.getResponseStatus();
        additionalDataMap.putAll(ImmutableMap.<Object, Object>of(ADYEN_CALL_ERROR_STATUS, responseStatus.isPresent()? responseStatus.get(): "",
                                                                 EXCEPTION_CLASS, adyenCallResult.getExceptionClass().or(UNKNOWN),
                                                                 EXCEPTION_MESSAGE, adyenCallResult.getExceptionMessage().or(UNKNOWN)));

        return additionalDataMap;
    }

    /**
     * Extract MPI specific form data from the result's additional data.
     *
     * @param result result
     * @return form parameters to be sent to the issuerUrl
     */
    private Map<String, String> extractMpiAdditionalData(final PaymentResult result) {
        final AnyType2AnyTypeMap additionalData = result.getAdditionalData();
        final Map<String, String> additionalDataMap = anyType2AnyTypeMapToStringMap(additionalData);
        final String mpiImplementation = additionalDataMap.get(AdyenPaymentPluginApi.PROPERTY_MPI_IMPLEMENTATION_TYPE);

        final Map<String, String> mpiData = new HashMap<String, String>();
        if (mpiImplementation != null) {
            final String prefix = mpiImplementation + ".";
            for (final Map.Entry<String, String> e : additionalDataMap.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    mpiData.put(e.getKey().substring(prefix.length()), e.getValue());
                }
            }
        }
        mpiData.put(AdyenPaymentPluginApi.PROPERTY_MPI_IMPLEMENTATION_TYPE, mpiImplementation);
        return mpiData;
    }

    /**
     * Convert {@link AnyType2AnyTypeMap} to {@link Map}.
     *
     * @param additionalData additionalData
     * @return map
     */
    private Map<String, String> anyType2AnyTypeMapToStringMap(final AnyType2AnyTypeMap additionalData) {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        if (additionalData != null) {
            for (final AnyType2AnyTypeMap.Entry e : additionalData.getEntry()) {
                additionalDataMap.put(e.getKey().toString(), e.getValue().toString());
            }
        }
        return additionalDataMap;
    }

    private abstract static class ModificationExecutor {

        public AdyenCallResult<ModificationResult> execute(final ModificationRequest modificationRequest) {
            throw new UnsupportedOperationException();
        }
    }
}
