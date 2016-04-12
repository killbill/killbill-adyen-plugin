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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

public class Signer {

    private static final Logger logger = LoggerFactory.getLogger(Signer.class);
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final BaseEncoding BASE_64_ENCODING = BaseEncoding.base64();

    private final AdyenConfigProperties adyenConfigProperties;

    public Signer(final AdyenConfigProperties adyenConfigProperties) {
        this.adyenConfigProperties = adyenConfigProperties;
    }

    public String computeSignature(final Long amount, final String currency, final String shipBeforeDate, final String reference, final String skin, final String merchantAccount, final String email, final String customerId, final PaymentInfo paymentInfo, final String sessionValidity) {
        try {
            final StringBuilder signingString = new StringBuilder();
            signingString.append(amount);
            signingString.append(currency);
            signingString.append(shipBeforeDate);
            signingString.append(reference);
            signingString.append(skin);
            signingString.append(merchantAccount);
            signingString.append(sessionValidity);
            if (email != null) {
                signingString.append(email);
            }
            if (customerId != null) {
                signingString.append(customerId);
            }
            // shopperLocale is ignored
            if (paymentInfo.getPaymentProvider().isRecurringEnabled()) {
                signingString.append(paymentInfo.getPaymentProvider().getRecurringType().name());
            }
            final String variant = paymentInfo.getPaymentProvider().getHppVariantOverride();
            if (variant == null && paymentInfo.getPaymentProvider().getAllowedMethods() != null) {
                signingString.append(paymentInfo.getPaymentProvider().getAllowedMethods());
            }
            return getBase64EncodedSignature(adyenConfigProperties.getHmacSecret(paymentInfo.getPaymentProvider().getCountryIsoCode()), signingString.toString());
        } catch (final SignatureGenerationException e) {
            logger.warn("Could not build hpp signature", e);
            return "";
        }
    }

    public String getBase64EncodedSignature(final String secret, final String signingData) throws SignatureGenerationException {
        final SecretKey key = getMacKey(secret);
        try {
            final Mac mac = Mac.getInstance(key.getAlgorithm());
            mac.init(getMacKey(secret));
            final byte[] digest = mac.doFinal(signingData.getBytes("UTF8"));
            return BASE_64_ENCODING.encode(digest);
        } catch (final NoSuchAlgorithmException nsae) {
            throw new SignatureGenerationException("Error while signature generation.", nsae);
        } catch (final IllegalStateException ise) {
            throw new SignatureGenerationException("Error while signature generation.", ise);
        } catch (final UnsupportedEncodingException uee) {
            throw new SignatureGenerationException("Error while signature generation.", uee);
        } catch (final InvalidKeyException ike) {
            throw new SignatureGenerationException("Error while signature generation.", ike);
        }
    }

    private SecretKey getMacKey(final String secret) {
        try {
            return new SecretKeySpec(secret.getBytes("ASCII"), HMAC_ALGORITHM);
        } catch (final UnsupportedEncodingException e) {
            return null;
        }
    }

    public boolean verifyBase64EncodedSignature(final String countryCode, final String sig, final String signedData)
            throws SignatureVerificationException {
        return !(countryCode == null || sig == null || signedData == null) && sig.equals(computeSign(countryCode, signedData));
    }

    public String computeSign(final String countryCode, final String signedData) throws SignatureVerificationException {
        final String secret = adyenConfigProperties.getHmacSecret(countryCode);
        final SecretKey key = getMacKey(secret);
        final String sign;
        try {
            final Mac mac = Mac.getInstance(key.getAlgorithm());
            mac.init(getMacKey(secret));
            final byte[] digest = mac.doFinal(signedData.getBytes("UTF8"));
            sign = BASE_64_ENCODING.encode(digest);
        } catch (final NoSuchAlgorithmException nsae) {
            throw new SignatureVerificationException("Error while signature verififcation.", nsae);
        } catch (final IllegalStateException ise) {
            throw new SignatureVerificationException("Error while signature verififcation.", ise);
        } catch (final UnsupportedEncodingException uee) {
            throw new SignatureVerificationException("Error while signature verififcation.", uee);
        } catch (final InvalidKeyException ike) {
            throw new SignatureVerificationException("Error while signature verififcation.", ike);
        }
        return sign;
    }
}
