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

public class PayeeAuthority implements BaseProperties {
    
    public static JSONObjectWriter encode(String authorityUrl,
                                          String providerAuthorityUrl,
                                          PayeeCoreProperties payeeCoreProperties,
                                          GregorianCalendar expires,
                                          ServerX509Signer attestSigner) throws IOException {
        return payeeCoreProperties.writeObject(Messages.PAYEE_AUTHORITY.createBaseMessage()
                                     .setString(AUTHORITY_URL_JSON, authorityUrl)
                                     .setString(PROVIDER_AUTHORITY_URL_JSON, providerAuthorityUrl))
            .setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true)
            .setDateTime(BaseProperties.EXPIRES_JSON, expires, true)
            .setSignature(attestSigner);
    }

    public PayeeAuthority(JSONObjectReader rd, String expectedAuthorityUrl) throws IOException {
        root = Messages.PAYEE_AUTHORITY.parseBaseMessage(rd);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        if (!authorityUrl.equals(expectedAuthorityUrl)) {
            throw new IOException("\"" + AUTHORITY_URL_JSON + "\" mismatch, read=" + authorityUrl +
                                  " expected=" + expectedAuthorityUrl);
        }
        providerAuthorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        payeeCoreProperties = new PayeeCoreProperties(rd);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        expiresInMillis = expires.getTimeInMillis();
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    long expiresInMillis;

    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    String providerAuthorityUrl;
    public String getProviderAuthorityUrl() {
        return providerAuthorityUrl;
    }

    PayeeCoreProperties payeeCoreProperties;
    public PayeeCoreProperties getPayeeCoreProperties() {
        return payeeCoreProperties;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    GregorianCalendar timeStamp;
     public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }
}
