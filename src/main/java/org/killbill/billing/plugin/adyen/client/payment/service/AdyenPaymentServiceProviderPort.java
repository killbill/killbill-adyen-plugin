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
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenPaymentServiceProviderPort extends BaseAdyenPaymentServiceProviderPort implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentServiceProviderPort.class);

    private final AdyenRequestFactory adyenRequestFactory;
    private final AdyenPaymentRequestSender adyenPaymentRequestSender;

    public AdyenPaymentServiceProviderPort(final AdyenRequestFactory adyenRequestFactory,
                                           final AdyenPaymentRequestSender adyenPaymentRequestSender) {
        this.adyenRequestFactory = adyenRequestFactory;
        this.adyenPaymentRequestSender = adyenPaymentRequestSender;
    }

    @Override
    public void close() throws IOException {
        adyenPaymentRequestSender.close();
    }

    public PurchaseResult authorise(final PaymentData paymentData,
                                    final UserData userData,
                                    final SplitSettlementData splitSettlementData) {
        return authoriseOrCredit(true, paymentData, userData, splitSettlementData);
    }

    public PurchaseResult credit(final PaymentData paymentData,
                                 final UserData userData,
                                 final SplitSettlementData splitSettlementData) {
        return authoriseOrCredit(false, paymentData, userData, splitSettlementData);
    }

    private PurchaseResult authoriseOrCredit(final boolean authorize,
                                             final PaymentData paymentData,
                                             final UserData userData,
                                             final SplitSettlementData splitSettlementData) {
        final String operation = authorize ? "authorize" : "credit";
        logOperation(logger, operation, paymentData, userData, null);

        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();

        final PaymentRequest request = adyenRequestFactory.createPaymentRequest(paymentData, userData, splitSettlementData);

        final AdyenCallResult<PaymentResult> adyenCallResult;
        if (authorize) {
            adyenCallResult = adyenPaymentRequestSender.authorise(paymentInfo.getCountry(), request);
        } else {
            adyenCallResult = adyenPaymentRequestSender.refundWithData(paymentInfo.getCountry(), request);
        }

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase(operation, paymentData, adyenCallResult);
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
                                                anyType2AnyTypeMapToStringMap(result.getAdditionalData()));
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
                                                anyType2AnyTypeMapToStringMap(result.getAdditionalData()));
        }

        logger.info("op='{}', {}", operation, purchaseResult);
        return purchaseResult;
    }

    private PurchaseResult handleTechnicalFailureAtPurchase(final String callKey, final PaymentData paymentData, final AdyenCallResult<PaymentResult> adyenCallResult) {
        logger.warn("op='{}', paymentTransactionExternalKey='{}', {}", callKey, paymentData.getPaymentTransactionExternalKey(), adyenCallResult);
        return new PurchaseResult(paymentData.getPaymentTransactionExternalKey(), adyenCallResult);
    }

    public PurchaseResult authorize3DSecure(final PaymentData paymentData,
                                            final UserData userData,
                                            final SplitSettlementData splitSettlementData) {
        final String operation = "authorize3DSecure";
        logOperation(logger, operation, paymentData, userData, null);

        final PaymentRequest3D request = adyenRequestFactory.paymentRequest3d(paymentData,
                                                                              userData,
                                                                              splitSettlementData);
        final AdyenCallResult<PaymentResult> adyenCallResult = adyenPaymentRequestSender.authorise3D(paymentData.getPaymentInfo().getCountry(), request);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase(operation, paymentData, adyenCallResult);
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
                                                anyType2AnyTypeMapToStringMap(result.getAdditionalData()));
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
                                                anyType2AnyTypeMapToStringMap(result.getAdditionalData()));
        }

        logger.info("op='{}', {}", operation, purchaseResult);
        return purchaseResult;
    }

    public PaymentModificationResponse refund(final PaymentData paymentData,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData) {
        return modify("refund",
                      new ModificationExecutor() {
                          @Override
                          public AdyenCallResult<ModificationResult> execute(final String country, final ModificationRequest modificationRequest) {
                              return adyenPaymentRequestSender.refund(country, modificationRequest);
                          }
                      },
                      paymentData,
                      pspReference,
                      splitSettlementData);
    }

    public PaymentModificationResponse cancel(final PaymentData paymentData,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData) {
        return modify("cancel",
                      new ModificationExecutor() {
                          @Override
                          public AdyenCallResult<ModificationResult> execute(final String country, final ModificationRequest modificationRequest) {
                              return adyenPaymentRequestSender.cancel(country, modificationRequest);
                          }
                      },
                      paymentData,
                      pspReference,
                      splitSettlementData);
    }

    public PaymentModificationResponse capture(final PaymentData paymentData,
                                               final String pspReference,
                                               final SplitSettlementData splitSettlementData) {
        return modify("capture",
                      new ModificationExecutor() {
                          @Override
                          public AdyenCallResult<ModificationResult> execute(final String country, final ModificationRequest modificationRequest) {
                              return adyenPaymentRequestSender.capture(country, modificationRequest);
                          }
                      },
                      paymentData,
                      pspReference,
                      splitSettlementData);
    }

    private PaymentModificationResponse modify(final String operation,
                                               final ModificationExecutor modificationExecutor,
                                               final PaymentData paymentData,
                                               final String pspReference,
                                               final SplitSettlementData splitSettlementData) {
        logOperation(logger, operation, paymentData, null, pspReference);

        final ModificationRequest modificationRequest = adyenRequestFactory.createModificationRequest(paymentData, pspReference, splitSettlementData);
        final AdyenCallResult<ModificationResult> adyenCallResult = modificationExecutor.execute(paymentData.getPaymentInfo().getCountry(), modificationRequest);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            logger.warn("op='{}', success='false', {}", operation, adyenCallResult);
            return new PaymentModificationResponse(pspReference, adyenCallResult);
        } else {
            final ModificationResult result = adyenCallResult.getResult().get();
            final PaymentModificationResponse response = new PaymentModificationResponse(result.getResponse(),
                                                                                         result.getPspReference(),
                                                                                         entriesToMap(result.getAdditionalData()));
            logger.info("op='{}', success='true', {}", operation, response);
            return response;
        }
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

    private Map<Object, Object> entriesToMap(final AnyType2AnyTypeMap dataMap) {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        if (dataMap != null) {
            final List<AnyType2AnyTypeMap.Entry> result = dataMap.getEntry();
            for (final AnyType2AnyTypeMap.Entry entry : result) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    private abstract static class ModificationExecutor {

        public AdyenCallResult<ModificationResult> execute(final String country, final ModificationRequest modificationRequest) {
            throw new UnsupportedOperationException();
        }
    }
}
