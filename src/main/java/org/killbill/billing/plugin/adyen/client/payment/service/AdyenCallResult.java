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

import java.io.IOException;

import com.adyen.service.exception.ApiException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public interface AdyenCallResult<T> {

    Optional<T> getResult();

    long getDuration();

    Optional<AdyenCallErrorStatus> getResponseStatus();

    Optional<String> getExceptionClass();

    Optional<String> getExceptionMessage();

    boolean receivedWellFormedResponse();

}

@VisibleForTesting
class SuccessfulAdyenCall<T> implements AdyenCallResult<T> {

    private final T result;

    private final long duration;

    @VisibleForTesting
    public SuccessfulAdyenCall(final T result, long duration) {
        this.result = checkNotNull(result, "result");
        this.duration = duration;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.of(result);
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public Optional<AdyenCallErrorStatus> getResponseStatus() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getExceptionClass() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.absent();
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SuccessfulAdyenCall{");
        sb.append("result=").append(result);
        sb.append(" }");
        return sb.toString();
    }
}

class UnSuccessfulAdyenCall<T> implements AdyenCallResult<T> {

    private final AdyenCallErrorStatus responseStatus;
    private final String exceptionClass;
    private final String exceptionMessage;
    private long duration;

    UnSuccessfulAdyenCall(final AdyenCallErrorStatus responseStatus, final Throwable rootCause) {
        this.responseStatus = responseStatus;
        this.exceptionClass = rootCause.getClass().getCanonicalName();
        this.exceptionMessage = rootCause.getMessage();
    }

    @Override
    public Optional<T> getResult() {
        return Optional.absent();
    }

    @Override
    public long getDuration() {
        return duration;
    }

    public void setDuration(final long duration) {
        this.duration = duration;
    }

    @Override
    public Optional<AdyenCallErrorStatus> getResponseStatus() {
        return Optional.of(responseStatus);
    }

    @Override
    public Optional<String> getExceptionClass() {
        return Optional.of(exceptionClass);
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.of(exceptionMessage);
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UnSuccessfulAdyenCall{");
        sb.append("responseStatus=").append(responseStatus);
        sb.append(", exceptionMessage='").append(exceptionMessage).append('\'');
        sb.append(", exceptionClass='").append(exceptionClass).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}

class FailedCheckoutApiCall<T> extends UnSuccessfulAdyenCall<T> {
    private Exception exception;
    FailedCheckoutApiCall(final AdyenCallErrorStatus responseStatus,
                          final Throwable rootCause,
                          final Exception exception) {
        super(responseStatus, rootCause);
        this.exception = exception;
    }

    ApiException getApiException() {
        return (exception instanceof ApiException) ? (ApiException)exception : null;
    }

    IOException getIOException() {
        return (exception instanceof IOException) ? (IOException)exception : null;
    }
}
