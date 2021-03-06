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

import java.security.GeneralSecurityException;

import java.util.GregorianCalendar;

import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONDecryptionDecoder;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.json.encryption.DataEncryptionAlgorithms;

public class ProviderUserResponse implements BaseProperties {
 
    public ProviderUserResponse(JSONObjectReader rd) throws IOException {
        Messages.PROVIDER_USER_RESPONSE.parseBaseMessage(rd);
        encryptedData = rd.getObject(ENCRYPTED_MESSAGE_JSON).getEncryptionObject().require(false);
        rd.checkForUnread();
    }

    JSONDecryptionDecoder encryptedData;
    
    public EncryptedMessage getEncryptedMessage(byte[] dataEncryptionKey,
                                                DataEncryptionAlgorithms dataEncryptionAlgorithm)
    throws IOException, GeneralSecurityException {
        if (encryptedData.getDataEncryptionAlgorithm() != dataEncryptionAlgorithm) {
            throw new IOException("Unexpected data encryption algorithm:" + encryptedData.getDataEncryptionAlgorithm().toString());
        }
        return new EncryptedMessage(JSONParser.parse(encryptedData.getDecryptedData(dataEncryptionKey))); 
    }

    public static JSONObjectWriter encode(String requester,
                                          String text,
                                          UserChallengeItem[] optionalUserChallengeItems,
                                          byte[] dataEncryptionKey,
                                          DataEncryptionAlgorithms dataEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        JSONObjectWriter wr = new JSONObjectWriter()
            .setString(REQUESTER_JSON, requester)
            .setString(TEXT_JSON, text);
        if (optionalUserChallengeItems != null && optionalUserChallengeItems.length > 0) {
            JSONArrayWriter aw = wr.setArray(USER_CHALLENGE_ITEMS_JSON);
            for (UserChallengeItem UserChallengeItem : optionalUserChallengeItems) {
                aw.setObject(UserChallengeItem.writeObject());
            }
        }
        wr.setDateTime(TIME_STAMP_JSON, new GregorianCalendar(), true);
        return Messages.PROVIDER_USER_RESPONSE.createBaseMessage()
            .setObject(ENCRYPTED_MESSAGE_JSON,
                       JSONObjectWriter.createEncryptionObject(wr.serializeToBytes(JSONOutputFormats.NORMALIZED),
                                                               dataEncryptionAlgorithm,
                                                               null,
                                                               dataEncryptionKey));
    }
}
