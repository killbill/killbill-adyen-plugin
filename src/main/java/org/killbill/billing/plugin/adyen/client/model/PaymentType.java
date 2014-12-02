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

import java.util.Arrays;
import java.util.Collection;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.CreditCardSchemaType.IS_CREDITCARD;
import static org.killbill.billing.plugin.adyen.client.model.PaymentType.CreditCardSchemaType.NO_CREDITCARD;
import static org.killbill.billing.plugin.adyen.client.model.WebFrontendType.FORWARDING_FRONTEND;
import static org.killbill.billing.plugin.adyen.client.model.WebFrontendType.POPUP_FRONTEND;

public enum PaymentType {

    ELV(1, "elv", "ec", null, null, null, 1, NO_CREDITCARD),
    CREDITCARD(2, new String[]{"creditcard", "visa", "mc", "bijcard", "visadankort"}, "creditcard", null, null, null, 2, IS_CREDITCARD), // gc & allpago don't support multiple cards through one type
    MAESTROUK(3, new String[]{"maestrouk", "solo"}, "maestrouk", null, 117, "MAESTRO", 2, NO_CREDITCARD),
    AMEX(4, "amex", "amex", null, 2, "AMEX", 2, IS_CREDITCARD),
    CARTE_BLEUE(5, "carte_bleue", "carte_bleue", null, 130, "CARTEBLEUE", 2, IS_CREDITCARD),
    DIRECT_EBANKING(6, "directEbanking", "direct_ebanking", FORWARDING_FRONTEND, null, null, 22, NO_CREDITCARD),
    PAYPAL(7, "paypal", "paypal", FORWARDING_FRONTEND, null, null, 10, NO_CREDITCARD),
    IDEAL(8, "ideal", "ideal", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    DOTPAY(9, "dotpay", "dotpay", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    LASER(10, "laser", "laser", null, 124, "LASER", 6, IS_CREDITCARD),
    BOLETO_BANCARIO(11, "boleto", "boleto", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD), // we use wpf so we don't need a brandcode for allpago boleto
    MAESTRO(12, "maestro", "maestro", null, 117, "MAESTRO", 2, NO_CREDITCARD),
    VISA(13, "visa", "visa", null, 1, "VISA", 2, IS_CREDITCARD),
    MASTERCARD(14, "mc", "mc", null, 3, "MASTER", 2, IS_CREDITCARD),
    DINERO_MAIL(15, "dineromail", "dineromail", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    VERKKOMAKSUT(16, "ebanking_FI", "verkkomaksut", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    HIPERCARD(17, "hipercard", "hipercard", null, null, null, 6, IS_CREDITCARD),
    AURA(18, "aura", "aura", null, null, null, 6, IS_CREDITCARD),
    DINERSCLUB(19, "diners", "dinersclub_long", null, null, null, 6, IS_CREDITCARD),
    CREDENCIAL(20, "credencial", "credencial", null, null, null, 6, IS_CREDITCARD),
    JCB(21, "jcb", "jcb", null, null, null, 6, IS_CREDITCARD),
    IXE(22, "ixe", "ixe", null, null, null, 6, IS_CREDITCARD),
    HSBC(23, "hsbc", "hsbc", null, null, null, 6, IS_CREDITCARD),
    SANTANDER_CREDIT(24, "santandercredit", "santander", null, null, null, 6, IS_CREDITCARD),
    SANTANDER_DEBIT(25, "santanderdebit", "santander", null, null, null, 6, IS_CREDITCARD),
    BANCOMER(26, "bancomer", "bancomer", null, null, null, 6, IS_CREDITCARD),
    BANORTE(27, "banorte", "banorte", null, null, null, 6, IS_CREDITCARD),
    BANAMEX(28, "banamex", "banamex", null, null, null, 6, IS_CREDITCARD),
    SCOTIABANK(29, "scotiabank", "scotiabank", null, null, null, 6, IS_CREDITCARD),
    UNIONPAY(30, "unionpay", "china_unionpay", FORWARDING_FRONTEND, null, null, 6, IS_CREDITCARD),
    ALIPAY(31, "alipay", "alipay", POPUP_FRONTEND, null, null, 6, NO_CREDITCARD),
    FUNDS_TRANSFER(32, new String[]{"oft", "bankTransfer_DE"}, "eft", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    TENPAY(33, "tenpay", "tenpay", POPUP_FRONTEND, null, null, 6, NO_CREDITCARD),
    BANCNET(34, "bancnet", "bancnet", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    GLOBEGCASH(35, "globegcash", "globalgcash", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    ISRACARD(36, "isracard", "isracard", null, null, null, 6, IS_CREDITCARD),
    ASIAPAY(37, "asiapay", "creditcard", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    VISA_HIPOTECARIO(38, "visahipotecario", "visahipotecario", null, null, null, 6, IS_CREDITCARD),
    NARANJA(39, "naranja", "tarjeta_naranja", null, null, null, 6, IS_CREDITCARD),
    CABAL(40, "cabal", "cabal", null, null, null, 6, IS_CREDITCARD),
    SHOPPING(41, "shopping", "tarjeta_shopping", null, null, null, 6, IS_CREDITCARD),
    ITALCRED(42, "italcred", "italcred", null, null, null, 6, IS_CREDITCARD),
    ARGEN(43, "argencard", "argencard", null, null, null, 6, IS_CREDITCARD),
    PSE(44, "pse", "pse", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    SAFETYPAY(45, "safetypay", "safetypay", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    MISTER_CASH(46, "bcmc", "bancontact_mistercash", null, null, null, 6, NO_CREDITCARD),
    IPAY88(47, "ipay88", "ipay88", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    CCAVENUE(48, "ccavenue", "ccavenue", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    HDFC(49, "hdfc", "hdfc", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    PPS(50, "pps", "pps", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    CASH_ON_DELIVERY(51, "cod", "cod", null, null, null, 6, NO_CREDITCARD),
    BANCODOBRASIL(52, "bancodobrasil", "bancobrasil", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    MERCADO_PAGO(53, "mercadopago", "mercadopago", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    AUTOPAY(54, "autopay", "autopay", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    CELLPAYPOINT(55, "cellpaypoint", "cellpaypoint", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    EBUCKS(56, "ebucks", "ebucks", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    MIMONEY(57, "mimoney", "mimoney", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    NEDBANK(58, "nedbank", "nedbank_nsp", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    PAYU(59, "payu", "payu", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    WIWALLET(60, "wiwallet", "wiwallet", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    BRADESCO(61, "bradesco", "bancobradesco", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    YAPI_KREDI(62, new String[]{"yapikredi", "visa", "mc"}, "yapikredi_world", null, null, null, 2, NO_CREDITCARD),
    DANKORT(63, "dankort", "dankort_dk", null, null, null, 2, IS_CREDITCARD),
    PAGOS_ONLINE(64, "pagosonline", "pagos", null, null, null, 6, NO_CREDITCARD),
    EPS(65, "eps", "eps", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    INVOICE(66, "invoice", "invoice", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    DEBITCARDS_HPP(67, new String[]{"debitcard", "visa", "mc"}, "debitcard", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    CASHU(68, "cashu", "cashu", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    SCBEASY(69, "scbeasy", "scb", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    KTBONLINE(70, "ktbonline", "ktbonline", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    UOBDIRECTDEBIT(71, "uobdirectdebit", "uob", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    KRUNGSRIONLINE(72, "krungsrionline", "krungsri", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    BUALUANGIBANKING(73, "bualuangibanking", "ibanking", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    TMBINTERNETBANKING(74, "tmbinternetbanking", "tmb", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    PAYTHRU_AMT(75, "paythru_amt", "paythru", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    MULTIPLUSPOINTS(76, "multipluspoints", "multipluspoints", null, null, null, 6, NO_CREDITCARD),
    ELO(77, "elo", "elo", null, null, null, 2, IS_CREDITCARD),
    SMARTMONEY(78, "smartmoney", "smartmoney", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    PAYTHRU_EFT(79, "paythru_eft", "eft", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    ONETWOTHREE(80, new String[]{"onetwothree_atm", "onetwothree_bankcounter", "onetwothree_ibanking", "onetwothree_overthecounter"}, null, FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    TRUSTLY(81, "trustly", "trustly", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    MULTIBANCO(82, "multibanco", "multibanco", FORWARDING_FRONTEND, null, null, 6, NO_CREDITCARD),
    SEPA_DIRECT_DEBIT(83, "sepadirectdebit", "sepadirectdebit", null, null, null, 6, NO_CREDITCARD),

    EMPTY(null, "", null, null, null, null, 6, NO_CREDITCARD); // extra payment type for better caching of recurring payment infos (when were is no info object available we use this payment type)

    private final Integer id;
    private final String[] names;// under this names adyen supplies the payment method
    private final String iconName;
    // a web frontend payment is some hpp style payment where we are not collecting the payment data and using a special api to transfer it,
    // but instead the payment service provider will collect the data directly from the customer and afterwards returns the customer back to us.
    private final WebFrontendType webFrontendType;

    private final Integer globalCollectPaymentProductId;
    private final String allpagoBrandCode;
    private final CreditCardSchemaType creditCardSchema;

    /**
     * payment code for TrustedShops Käuferschutz prozess provided at the payment confirmation,
     * this is the number we submit so they can identify the payment method
     */
    private final Integer tSPaymentCode;

    PaymentType(final Integer id, final String name, final String iconName, final WebFrontendType webFrontendType, final Integer globalCollectPaymentProductId, final String allpagoBrandCode, final Integer tSPaymentCode, final CreditCardSchemaType creditCardSchema) {
        this(id, new String[]{name}, iconName, webFrontendType, globalCollectPaymentProductId, allpagoBrandCode, tSPaymentCode, creditCardSchema);
    }

    PaymentType(final Integer id, final String[] names, final String iconName, final WebFrontendType webFrontendType, final Integer globalCollectPaymentProductId, final String allpagoBrandCode, final Integer tSPaymentCode, final CreditCardSchemaType creditCardSchema) {
        this.id = id;
        this.names = names;
        this.iconName = iconName;
        this.webFrontendType = webFrontendType;
        this.globalCollectPaymentProductId = globalCollectPaymentProductId;
        this.allpagoBrandCode = allpagoBrandCode;
        this.tSPaymentCode = tSPaymentCode;
        this.creditCardSchema = creditCardSchema;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.names[0]; // always use the first name as unique name for everything
    }

    public static PaymentType getByName(final String name) {
        final Collection<PaymentType> result = Collections2.filter(Arrays.asList(PaymentType.values()), new Predicate<PaymentType>() {
            @Override
            public boolean apply(final PaymentType input) {
                return input.getName().equals(name);
            }
        });
        return result.size() == 1 ? result.iterator().next() : null;
    }

    public String[] getNames() {
        return Arrays.copyOf(this.names, this.names.length);
    }

    public String getIconName() {
        return this.iconName;
    }

    public Boolean isWebFrontendType() {
        return this.webFrontendType != null;
    }

    public WebFrontendType getWebFrontendType() {
        return this.webFrontendType;
    }

    public Integer getGlobalCollectPaymentProductId() {
        return this.globalCollectPaymentProductId;
    }

    public String getAllpagoBrandCode() {
        return this.allpagoBrandCode;
    }

    /**
     * @return the tSPaymentCode
     */
    public Integer gettSPaymentCode() {
        return tSPaymentCode;
    }

    public boolean isCreditCardSchema() {
        return creditCardSchema == IS_CREDITCARD;
    }

    public enum CreditCardSchemaType {
        IS_CREDITCARD, NO_CREDITCARD
    }
}
