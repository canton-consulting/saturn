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

'use strict';

// Saturn "TransactionRequest" object

const JsonUtil  = require('webpki.org').JsonUtil;
const ByteArray = require('webpki.org').ByteArray;

const BaseProperties        = require('./BaseProperties');
const Messages              = require('./Messages');
const Software              = require('./Software');
const AuthorizationResponse = require('./AuthorizationResponse');
const AuthorizationRequest  = require('./AuthorizationRequest');
const ProtectedAccountData  = require('./ProtectedAccountData');

function TransactionRequest(rd) {
  this.root = Messages.parseBaseMessage(Messages.TRANSACTION_REQUEST, rd);
  this.authorizationResponse = new AuthorizationResponse(Messages.getEmbeddedMessage(Messages.AUTHORIZATION_RESPONSE, rd));
  this.recepientUrl = rd.getString(BaseProperties.RECEPIENT_URL_JSON);
  this.actualAmount = rd.getBigDecimal(BaseProperties.AMOUNT_JSON,
                                       this.authorizationResponse.authorizationRequest.paymentRequest.currency.decimals);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature().getPublicKey();
  if (!this.authorizationResponse.authorizationRequest.payerAccountType.isCardPayment()) {
    throw new TypeError('Payment method is not card: ' + 
        this.authorizationResponse.authorizationRequest.payerAccountType.getTypeUri());
  }
  rd.checkForUnread();
}

TransactionRequest.prototype.getTimeStamp = function() {
  return this.timeStamp;
};

TransactionRequest.prototype.getReferenceId = function() {
  return this.referenceId;
};

TransactionRequest.prototype.getAmount = function() {
  return this.actualAmount;
};

TransactionRequest.prototype.getTestMode = function() {
  return this.authorizationResponse.authorizationRequest.testMode;
};

TransactionRequest.prototype.getPayee = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest.payee;
};

TransactionRequest.prototype.getPublicKey = function() {
  return this.publicKey;
};

TransactionRequest.prototype.getAuthorizationPublicKey = function() {
  return this.authorizationResponse.authorizationRequest.publicKey;
};

TransactionRequest.prototype.getAuthorizationResponse = function() {
  return this.authorizationResponse;
};

TransactionRequest.prototype.getPaymentRequest = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest;
};

TransactionRequest.prototype.verifyPayerProvider = function(paymentRoot) {
  this.authorizationResponse.signatureDecoder.verifyTrust(paymentRoot);
};

TransactionRequest.prototype.getProtectedAccountData = function(decryptionKeys) {
  return new ProtectedAccountData(JsonUtil.ObjectReader.parse(
                                      this.authorizationResponse.encryptedAccountData
                                          .getDecryptedData(decryptionKeys)),
                                  this.authorizationResponse.authorizationRequest.payerAccountType);
};

module.exports = TransactionRequest;
