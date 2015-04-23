/*
 * Copyright 2014-2015 Groupon, Inc
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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.soap.SOAPFaultException;

import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.adyen.payment.ServiceException;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.OrderData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.exception.ModificationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class AdyenPaymentServiceProviderPort implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger("adyen");

    private final PaymentInfoConverterManagement paymentInfoConverterManagement;
    private final AdyenRequestFactory adyenRequestFactory;
    private final AdyenPaymentRequestSender adyenPaymentRequestSender;

    /**
     * received error constants *
     */
    private final static String REFUSED_FAILURE = "Refused";

    public AdyenPaymentServiceProviderPort(final PaymentInfoConverterManagement paymentInfoConverterManagement,
                                           final AdyenRequestFactory adyenRequestFactory,
                                           final AdyenPaymentRequestSender adyenPaymentRequestSender) {
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;
        this.adyenRequestFactory = adyenRequestFactory;
        this.adyenPaymentRequestSender = adyenPaymentRequestSender;
    }

    @Override
    public void close() throws IOException {
        adyenPaymentRequestSender.close();
    }

    public PurchaseResult authorise(final Long amount,
                                    final PaymentData paymentData,
                                    final OrderData orderData,
                                    final UserData userData,
                                    final String termUrl,
                                    final SplitSettlementData splitSettlementData) {
        logger.info("authorise Start");

        Preconditions.checkNotNull(paymentData.getPaymentTxnInternalRef(), "paymentTxnInternalRef");

        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        Preconditions.checkNotNull(paymentInfo, "paymentInfo");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider(), "paymentProvider");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider().getCurrency(), "currency");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider().getCountryIsoCode(), "countryIsoCode");

        final PaymentRequest request = adyenRequestFactory.createPaymentRequest(amount, orderData, paymentData, userData, termUrl, splitSettlementData);
        return authorise(request, paymentInfo.getPaymentProvider().getCountryIsoCode(), paymentData, !paymentInfo.getPaymentProvider().send3DSTermUrl() ? termUrl : null);
    }

    @VisibleForTesting
    PurchaseResult authorise(final PaymentRequest request, final String countryIsoCode, final PaymentData paymentData, final String termUrl) {
        try {
            final PaymentResult result = adyenPaymentRequestSender.authorise(countryIsoCode, request);

            final PurchaseResult purchaseResult;
            final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode());
            if (paymentServiceProviderResult != PaymentServiceProviderResult.REDIRECT_SHOPPER) {
                purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                                                    result.getAuthCode(),
                                                    result.getPspReference(),
                                                    result.getRefusalReason(),
                                                    result.getResultCode(),
                                                    paymentData.getPaymentInternalRef());
            } else {
                final Map<String, String> formParams = new HashMap<String, String>();
                formParams.put(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result.getPaRequest());
                formParams.put(AdyenPaymentPluginApi.PROPERTY_MD, result.getMd());
                formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result.getDccAmount() == null ? null : String.valueOf(result.getDccAmount().getValue()));
                formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result.getDccAmount() == null ? null : result.getDccAmount().getCurrency());
                formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result.getDccSignature());
                formParams.put(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result.getIssuerUrl());
                formParams.putAll(extractMpiAdditionalData(result));
                if (termUrl != null) {
                    formParams.put(AdyenPaymentPluginApi.PROPERTY_TERM_URL, termUrl);
                }

                purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                                                    result.getAuthCode(),
                                                    result.getPspReference(),
                                                    result.getRefusalReason(),
                                                    result.getResultCode(),
                                                    paymentData.getPaymentInternalRef(),
                                                    result.getIssuerUrl(),
                                                    formParams);
            }

            logger.info("authorise Done", purchaseResult);
            return purchaseResult;
        } catch (final ServiceException e) {
            logger.warn("authorise", e);
            return handleAuthorizePaymentServiceFault(paymentData.getPaymentInternalRef(), request.getReference(), e);
        } catch (final SOAPFaultException e) {
            logger.warn("authorise", e);
            return handleAuthorizePaymentSOAPFault(paymentData.getPaymentInternalRef(), request.getReference(), e);
        }
    }

    public PurchaseResult authorize3DSecure(final Long billedAmount,
                                            final PaymentData<Card> paymentData,
                                            final UserData userData,
                                            final Map<String, String[]> requestParameterMap,
                                            final SplitSettlementData splitSettlementData) {
        logger.info("authorize3DSecure Start {} [}", billedAmount, userData);

        final Card paymentInfo = paymentData.getPaymentInfo();
        final BrowserInfo info = (BrowserInfo) paymentInfoConverterManagement.getBrowserInfoFor3DSecureAuth(billedAmount, paymentData.getPaymentInfo());
        final PaymentRequest3D request = adyenRequestFactory.paymentRequest3d(paymentData.getPaymentInternalRef(),
                                                                              paymentInfo,
                                                                              info,
                                                                              requestParameterMap,
                                                                              splitSettlementData,
                                                                              userData.getIP(),
                                                                              userData.getEmail(),
                                                                              userData.getCustomerId());
        try {
            final PaymentResult result = adyenPaymentRequestSender.authorise3D(paymentInfo.getPaymentProvider().getCountryIsoCode(), request);

            final PurchaseResult purchaseResult;
            final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode());
            if (paymentServiceProviderResult != PaymentServiceProviderResult.REDIRECT_SHOPPER) {
                purchaseResult = new PurchaseResult(PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode()),
                                                    result.getAuthCode(),
                                                    result.getPspReference(),
                                                    result.getRefusalReason(),
                                                    result.getResultCode(),
                                                    paymentData.getPaymentInternalRef());
            } else {
                final Map<String, String> formParams = new HashMap<String, String>();
                formParams.put("PaReq", result.getPaRequest());
                formParams.put("MD", result.getMd());

                purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                                                    result.getAuthCode(),
                                                    result.getPspReference(),
                                                    result.getRefusalReason(),
                                                    result.getResultCode(),
                                                    paymentData.getPaymentInternalRef(),
                                                    result.getIssuerUrl(),
                                                    formParams);
            }

            logger.info("authorize3DSecure Done {}", purchaseResult);
            return purchaseResult;
        } catch (final ServiceException e) {
            logger.warn("authorize3DSecure", e);
            return handleAuthorizePaymentServiceFault(paymentData.getPaymentInternalRef(), request.getReference(), e);
        } catch (final SOAPFaultException e) {
            logger.warn("authorize3DSecure", e);
            return handleAuthorizePaymentSOAPFault(paymentData.getPaymentInternalRef(), request.getReference(), e);
        }
    }

    public PaymentModificationResponse refund(final Long externalAmount,
                                              final PaymentProvider paymentProvider,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData) throws ModificationFailedException {
        logger.info("refund Start for pspReference {}", pspReference);

        try {
            final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(paymentProvider, externalAmount, pspReference, splitSettlementData);
            final ModificationResult result = adyenPaymentRequestSender.refund(paymentProvider.getCountryIsoCode(), modificationRequest);

            logger.debug("refund for pspReference: {}, got new pspReference: {} and response {}", pspReference, result.getPspReference(), result.getResponse());

            final PaymentModificationResponse paymentModificationResponse = new PaymentModificationResponse(result.getResponse(),
                                                                                                            result.getPspReference(),
                                                                                                            entriesToMap(result.getAdditionalData()));
            logger.info("refund End: {}", paymentModificationResponse);
            return paymentModificationResponse;
        } catch (final ServiceException e) {
            logger.warn("refund", e);
            throw new ModificationFailedException(e);
        } catch (final SOAPFaultException e) {
            logger.warn("refund", e);
            throw new ModificationFailedException(e);
        }
    }

    public PaymentModificationResponse cancel(final PaymentProvider paymentProvider,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData) throws ModificationFailedException {
        logger.info("cancel Start for pspReference {}", pspReference);

        try {
            final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(paymentProvider, pspReference, splitSettlementData);
            final ModificationResult result = adyenPaymentRequestSender.cancel(paymentProvider.getCountryIsoCode(), modificationRequest);

            logger.debug("cancel for pspReference: {}, got new pspReference: {} and response {}", pspReference, result.getPspReference(), result.getResponse());

            final PaymentModificationResponse response = new PaymentModificationResponse(result.getResponse(),
                                                                                         result.getPspReference(),
                                                                                         entriesToMap(result.getAdditionalData()));
            logger.info("cancel End", response);
            return response;
        } catch (final ServiceException e) {
            logger.warn("cancel", e);
            throw new ModificationFailedException(e);
        } catch (final SOAPFaultException e) {
            logger.warn("cancel", e);
            throw new ModificationFailedException(e);
        }
    }

    public PaymentModificationResponse capture(final Long amount,
                                               final PaymentProvider paymentProvider,
                                               final String pspReference,
                                               final SplitSettlementData splitSettlementData) throws ModificationFailedException {
        logger.info("capture Start for pspReference {}", pspReference);

        try {
            final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(paymentProvider, amount, pspReference, splitSettlementData);
            final ModificationResult result = adyenPaymentRequestSender.capture(paymentProvider.getCountryIsoCode(), modificationRequest);

            logger.debug("capture for pspReference: {}, got new pspReference: {} and response {}", pspReference, result.getPspReference(), result.getResponse());

            final PaymentModificationResponse response = new PaymentModificationResponse(result.getResponse(),
                                                                                         result.getPspReference(),
                                                                                         entriesToMap(result.getAdditionalData()));
            logger.info("capture End", response);
            return response;
        } catch (final ServiceException e) {
            logger.warn("capture", e);
            throw new ModificationFailedException(e);
        } catch (final SOAPFaultException e) {
            logger.warn("capture", e);
            throw new ModificationFailedException(e);
        }
    }

    private PurchaseResult handleAuthorizePaymentServiceFault(final String internalRef, final String reference, final ServiceException se) {
        logger.error("payment failed for internalReference: " + internalRef, se);
        return new PurchaseResult(PaymentServiceProviderResult.ERROR, se.getMessage(), reference, AdyenPaymentExceptionParser.parsePaymentException(se.getMessage()), internalRef);
    }

    private PurchaseResult handleAuthorizePaymentSOAPFault(final String paymentInternalRef, final String reference, final SOAPFaultException e) {
        final PaymentServiceProviderResult pspResult;
        if (e.getMessage().contains(REFUSED_FAILURE)) {
            pspResult = PaymentServiceProviderResult.REFUSED;
            logger.info("Payment authentication failed for internalReference: " + paymentInternalRef + ", error message: " + e.getMessage());
        } else {
            pspResult = PaymentServiceProviderResult.ERROR;
            logger.warn("Payment authentication failed for internalReference: " + paymentInternalRef + ", error message: " + e.getMessage());
        }

        return new PurchaseResult(pspResult, e.getMessage(), reference, AdyenPaymentExceptionParser.parsePaymentException(e.getMessage()), paymentInternalRef);
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
        final String mpiImplementation = additionalDataMap.get(AdyenRequestFactory.MPI_IMPLEMENTATION_TYPE);

        final Map<String, String> mpiData = new HashMap<String, String>();
        if (mpiImplementation != null) {
            final String prefix = mpiImplementation + ".";
            for (final Map.Entry<String, String> e : additionalDataMap.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    mpiData.put(e.getKey().substring(prefix.length()), e.getValue());
                }
            }
        }
        mpiData.put(AdyenRequestFactory.MPI_IMPLEMENTATION_TYPE, mpiImplementation);
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
        Map<Object, Object> map = null;
        if (dataMap != null) {
            final List<AnyType2AnyTypeMap.Entry> result = dataMap.getEntry();
            map = new HashMap<Object, Object>(result.size());
            for (final AnyType2AnyTypeMap.Entry entry : result) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }
}
