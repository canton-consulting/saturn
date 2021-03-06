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

//Saturn "AccountDescriptor" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');

function AccountDescriptor(typeUri_or_rd, id) {
  if (typeUri_or_rd instanceof JsonUtil.ObjectReader) {
    this.typeUri = typeUri_or_rd.getString(BaseProperties.TYPE_JSON);
    this.id = typeUri_or_rd.getString(BaseProperties.ID_JSON);
  } else {
    this.typeUri = typeUri_or_rd;
    this.id = id;
  }
}

AccountDescriptor.prototype.writeObject = function() {
  return new JsonUtil.ObjectWriter()
    .setString(BaseProperties.TYPE_JSON, this.typeUri)
    .setString(BaseProperties.ID_JSON, this.id);
};

AccountDescriptor.prototype.getType = function() {
  return this.typeUri;
};

AccountDescriptor.prototype.getId = function() {
  return this.id;
};


module.exports = AccountDescriptor;
