/*
 * Copyright 2015 Groupon, Inc
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

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public interface AdyenCallResult<T> {
    Optional<T> getResult();

    Optional<AdyenCallErrorStatus> getResponseStatus();

    boolean receivedWellFormedResponse();

    Optional<String> getErrorMessage();

}

class SuccessfulAdyenCall<T> implements AdyenCallResult<T> {

    private final T result;

    public SuccessfulAdyenCall(T result) {

        checkNotNull(result, "result");

        this.result = result;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.of(result);
    }

    @Override
    public Optional<AdyenCallErrorStatus> getResponseStatus() {
        return Optional.absent();
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return true;
    }

    @Override
    public Optional<String> getErrorMessage() {
        return Optional.absent();
    }

    @Override
    public String toString() {
        return "SuccessfulAdyenCall{" +
                "result=" + result +
                '}';
    }
}

class UnSuccessfulAdyenCall<T> implements AdyenCallResult<T> {


    private final AdyenCallErrorStatus responseStatus;
    private final String errorMessage;

    UnSuccessfulAdyenCall(AdyenCallErrorStatus responseStatus, String errorMessage) {
        this.responseStatus = responseStatus;
        this.errorMessage = errorMessage;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.absent();
    }

    @Override
    public Optional<AdyenCallErrorStatus> getResponseStatus() {
        return Optional.of(responseStatus);
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return false;
    }

    @Override
    public Optional<String> getErrorMessage() {
        return Optional.of(errorMessage);
    }

    @Override
    public String toString() {
        return "UnSuccessfulAdyenCall{" +
                "responseStatus=" + responseStatus +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
