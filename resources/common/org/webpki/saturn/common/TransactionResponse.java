/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.common;

import java.io.IOException;

import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

public class TransactionResponse implements BaseProperties {
    
    public static final String SOFTWARE_NAME    = "WebPKI.org - Payment Provider";
    public static final String SOFTWARE_VERSION = "1.00";
    
    public static enum ERROR {OUT_OF_FUNDS, DELETED_AUTHORIZATION}

    public TransactionResponse(JSONObjectReader rd) throws IOException {
        root = Messages.TRANSACTION_RESPONSE.parseBaseMessage(rd);
        transactionRequest = new TransactionRequest(Messages.TRANSACTION_REQUEST.getEmbeddedMessage(rd), null);
        if (rd.hasProperty(TRANSACTION_ERROR_JSON)) {
            transactionError = ERROR.valueOf(rd.getString(TRANSACTION_ERROR_JSON));
        }
        optionalLogData = rd.getStringConditional(LOG_DATA_JSON);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    JSONObjectReader root;

    Software software;

    GregorianCalendar dateTime;

    ERROR transactionError;
    public ERROR getTransactionError() {
        return transactionError;
    }

    String optionalLogData;
    public String getOptionalLogData() {
        return optionalLogData;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }


    TransactionRequest transactionRequest;
    public TransactionRequest getTransactionRequest() {
        return transactionRequest;
    }

    public static JSONObjectWriter encode(TransactionRequest transactionRequest,
                                          ERROR transactionError,
                                          String referenceId,
                                          String optionalLogData,
                                          ServerX509Signer signer) throws IOException {
        return Messages.TRANSACTION_RESPONSE.createBaseMessage()
            .setObject(Messages.TRANSACTION_REQUEST.lowerCamelCase(), transactionRequest.root)
            .setDynamic((wr) -> transactionError == null ? wr : wr.setString(TRANSACTION_ERROR_JSON, transactionError.toString()))
            .setDynamic((wr) -> optionalLogData == null ? wr : wr.setString(LOG_DATA_JSON, optionalLogData))
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_NAME, SOFTWARE_VERSION))
            .setSignature(signer);
    }
}
