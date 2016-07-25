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

package org.killbill.billing.plugin.adyen.api.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.Acquirer;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.clock.Clock;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ACCEPT_HEADER;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ACQUIRER;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_ACQUIRER_MID;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_CAPTURE_DELAY_HOURS;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_CONTINUOUS_AUTHENTICATION;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_DD_BANK_IDENTIFIER_CODE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_INSTALLMENTS;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MD;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_DATA_AUTHENTICATION_RESPONSE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_DATA_CAVV;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_DATA_CAVV_ALGORITHM;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_DATA_DIRECTORY_RESPONSE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_DATA_ECI;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_DATA_XID;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_MPI_IMPLEMENTATION_TYPE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_PA_RES;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_RECURRING_DETAIL_ID;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_RECURRING_TYPE;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_TERM_URL;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_THREE_D_THRESHOLD;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_USER_AGENT;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.decode;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_ADDRESS1;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_ADDRESS2;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CC_TYPE;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CITY;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_STATE;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_ZIP;

public abstract class PaymentInfoMappingService {

    public static PaymentInfo toPaymentInfo(final String merchantAccount,
                                            @Nullable final String countryCode,
                                            final AdyenConfigProperties configuration,
                                            final Clock clock,
                                            @Nullable final AccountData account,
                                            @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord,
                                            final Iterable<PluginProperty> properties) {
        final PaymentInfo paymentInfo;

        if (paymentMethodsRecord == null) {
            paymentInfo = WebPaymentFrontendMappingService.toPaymentInfo(merchantAccount, configuration, clock, properties);
        } else {
            final String recurringDetailReference = PluginProperties.getValue(PROPERTY_RECURRING_DETAIL_ID, paymentMethodsRecord.getToken(), properties);
            if (recurringDetailReference != null) {
                paymentInfo = RecurringMappingService.toPaymentInfo(paymentMethodsRecord, properties);
            } else {
                final String ddBic = PluginProperties.findPluginPropertyValue(PROPERTY_DD_BANK_IDENTIFIER_CODE, properties);
                if (ddBic != null) {
                    paymentInfo = SepaDirectDebitMappingService.toPaymentInfo(account, paymentMethodsRecord, properties);
                } else {
                    // Will be used as a fallback
                    paymentInfo = CardMappingService.toPaymentInfo(paymentMethodsRecord, properties);
                }
            }
        }

        set3DSecureFields(paymentInfo, properties);
        setBillingAddress(countryCode, account, paymentInfo, paymentMethodsRecord, properties);
        setCaptureDelayHours(paymentInfo, properties);
        setContractAndContinuousAuthentication(paymentInfo, properties);
        setInstallments(paymentInfo, properties);
        setSelectedBrand(paymentInfo, properties);
        setAcquirer(configuration, paymentInfo, properties);

        return paymentInfo;
    }

    private static void set3DSecureFields(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String ccUserAgent = PluginProperties.findPluginPropertyValue(PROPERTY_USER_AGENT, properties);
        paymentInfo.setUserAgent(ccUserAgent);

        final String ccAcceptHeader = PluginProperties.findPluginPropertyValue(PROPERTY_ACCEPT_HEADER, properties);
        paymentInfo.setAcceptHeader(ccAcceptHeader);

        final String md = PluginProperties.findPluginPropertyValue(PROPERTY_MD, properties);
        if (md != null) {
            paymentInfo.setMd(decode(md));
        }

        final String paRes = PluginProperties.findPluginPropertyValue(PROPERTY_PA_RES, properties);
        if (paRes != null) {
            paymentInfo.setPaRes(decode(paRes));
        }

        final String threeDThreshold = PluginProperties.findPluginPropertyValue(PROPERTY_THREE_D_THRESHOLD, properties);
        if (!Strings.isNullOrEmpty(threeDThreshold)) {
            // Expected in minor units
            paymentInfo.setThreeDThreshold(Long.valueOf(threeDThreshold));
        }

        final String mpiDataDirectoryResponse = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_DIRECTORY_RESPONSE, properties);
        paymentInfo.setMpiDataDirectoryResponse(mpiDataDirectoryResponse);

        final String mpiDataAuthenticationResponse = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_AUTHENTICATION_RESPONSE, properties);
        paymentInfo.setMpiDataAuthenticationResponse(mpiDataAuthenticationResponse);

        final String mpiDataCavv = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_CAVV, properties);
        paymentInfo.setMpiDataCavv(mpiDataCavv);

        final String mpiDataCavvAlgorithm = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_CAVV_ALGORITHM, properties);
        paymentInfo.setMpiDataCavvAlgorithm(mpiDataCavvAlgorithm);

        final String mpiDataXid = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_XID, properties);
        paymentInfo.setMpiDataXid(mpiDataXid);

        final String mpiDataEci = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_ECI, properties);
        paymentInfo.setMpiDataEci(mpiDataEci);

        final String mpiImplementationType = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_IMPLEMENTATION_TYPE, properties);
        paymentInfo.setMpiImplementationType(mpiImplementationType);
        if (mpiImplementationType != null) {
            paymentInfo.setMpiImplementationTypeValues(Maps.filterKeys(PluginProperties.toStringMap(properties), Predicates.containsPattern(mpiImplementationType + ".")));
        }

        final String termUrl = PluginProperties.findPluginPropertyValue(PROPERTY_TERM_URL, properties);
        paymentInfo.setTermUrl(termUrl);
    }

    private static void setBillingAddress(@Nullable final String countryCode, @Nullable final AccountData account, final PaymentInfo paymentInfo, @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        String street = PluginProperties.getValue(PROPERTY_ADDRESS1, paymentMethodsRecord == null ? null : paymentMethodsRecord.getAddress1(), properties);
        if (street == null && account != null) {
            street = account.getAddress1();
        }
        paymentInfo.setStreet(street);

        String houseNumberOrName = PluginProperties.getValue(PROPERTY_ADDRESS2, paymentMethodsRecord == null ? null : paymentMethodsRecord.getAddress2(), properties);
        if (houseNumberOrName == null && account != null) {
            houseNumberOrName = account.getAddress2();
        }
        paymentInfo.setHouseNumberOrName(houseNumberOrName);

        String city = PluginProperties.getValue(PROPERTY_CITY, paymentMethodsRecord == null ? null : paymentMethodsRecord.getCity(), properties);
        if (city == null && account != null) {
            city = account.getCity();
        }
        paymentInfo.setCity(city);

        String postalCode = PluginProperties.getValue(PROPERTY_ZIP, paymentMethodsRecord == null ? null : paymentMethodsRecord.getZip(), properties);
        if (postalCode == null && account != null) {
            postalCode = account.getPostalCode();
        }
        paymentInfo.setPostalCode(postalCode);

        String stateOrProvince = PluginProperties.getValue(PROPERTY_STATE, paymentMethodsRecord == null ? null : paymentMethodsRecord.getState(), properties);
        if (stateOrProvince == null && account != null) {
            stateOrProvince = account.getStateOrProvince();
        }
        paymentInfo.setStateOrProvince(stateOrProvince);

        paymentInfo.setCountry(countryCode);
    }

    private static void setCaptureDelayHours(final PaymentInfo paymentInfo, final Iterable<PluginProperty> mergedProperties) {
        final String captureDelayHours = PluginProperties.findPluginPropertyValue(PROPERTY_CAPTURE_DELAY_HOURS, mergedProperties);
        if (captureDelayHours != null) {
            paymentInfo.setCaptureDelayHours(Integer.valueOf(captureDelayHours));

        }
    }

    private static void setContractAndContinuousAuthentication(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        String contract = PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_TYPE, properties);
        if (contract == null && paymentInfo instanceof Recurring) {
            contract = "RECURRING";
        }
        paymentInfo.setContract(contract);

        final String contAuthProperty = PluginProperties.findPluginPropertyValue(PROPERTY_CONTINUOUS_AUTHENTICATION, properties);
        final boolean contAuth;
        if (contAuthProperty != null) {
            contAuth = Boolean.parseBoolean(contAuthProperty);
        } else {
            // https://docs.adyen.com/developers/recurring-manual
            if ("ONECLICK".equals(contract)) {
                contAuth = false;
            } else if ("RECURRING".equals(contract)) {
                contAuth = true;
            } else {
                // By default, send Ecommerce when we have the CVC, ContAuth otherwise (card on file)
                contAuth = paymentInfo instanceof Card && ((Card) paymentInfo).getCvc() == null;
            }
        }

        if (contAuth) {
            // Recurring flag
            paymentInfo.setShopperInteraction("ContAuth");
        } else {
            paymentInfo.setShopperInteraction("Ecommerce");
        }
    }

    private static void setInstallments(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String installments = PluginProperties.findPluginPropertyValue(PROPERTY_INSTALLMENTS, properties);
        if (installments != null) {
            paymentInfo.setInstallments(Integer.valueOf(installments));
        }
    }

    private static void setSelectedBrand(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        // For the MisterCash payment method, it can be set to maestro (default, to be processed like a Maestro card) or bcmc (to be processed like a MisterCash card)
        // It can also be set to specific values for DineroMail or to force recurring ELV contracts to be handled as SEPA
        final String selectedBrand = PluginProperties.findPluginPropertyValue(PROPERTY_CC_TYPE, properties);
        paymentInfo.setSelectedBrand(selectedBrand);
    }

    private static void setAcquirer(final AdyenConfigProperties configuration, final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String acquirerName = PluginProperties.findPluginPropertyValue(PROPERTY_ACQUIRER, properties);
        String acquirerMid = PluginProperties.findPluginPropertyValue(PROPERTY_ACQUIRER_MID, properties);
        if (acquirerName != null && acquirerMid == null) {
            final Acquirer acquirer = getAcquirers(configuration).get(acquirerName);
            acquirerMid = acquirer.getMid();
        }
        paymentInfo.setAcquirer(acquirerName);
        paymentInfo.setAcquirerMID(acquirerMid);
    }

    private static Map<String, Acquirer> getAcquirers(final AdyenConfigProperties configuration) {
        final Map<String, Acquirer> acquirers = new HashMap<String, Acquirer>();
        final String str = configuration.getAcquirersList();
        // Is there an acquirer list?
        if (!Strings.isNullOrEmpty(str)) {
            // Parse the list of acquirers
            final String[] acquirerList = str.split(",");
            for (final String current : acquirerList) {
                final Acquirer acquirer = parseAcquirer(current);
                if (acquirer != null) {
                    acquirers.put(acquirer.getName(), acquirer);
                }
            }
        }
        return acquirers;
    }

    private static Acquirer parseAcquirer(final String str) {
        final Acquirer acquirer;
        if (Strings.isNullOrEmpty(str)) {
            return null;
        }

        // If the acquirer code is followed by a | and a second value, that value represents the MID.
        final String[] keyValue = str.split("\\|");
        if (keyValue.length > 1) {
            acquirer = new Acquirer(keyValue[0], keyValue[1]);
        } else {
            acquirer = new Acquirer(keyValue[0]);
        }

        return acquirer;
    }
}
