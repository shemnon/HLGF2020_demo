/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.besu.hlgf2020.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

class EventsRegistry {

  static final Map<String, String> eventMap =
      new BufferedReader(
              new InputStreamReader(
                  EventsRegistry.class.getResourceAsStream("/event_signatures.txt")))
          .lines()
          .collect(Collectors.toMap(EventsRegistry::encode, Function.identity()));

  private static String encode(final String eventSignature) {
    return Numeric.toHexString(Hash.sha3(eventSignature.getBytes()));
  }

  static String getEventName(final String encoded) {
    return eventMap.getOrDefault(encoded, "Unknown Event " + encoded);
  }

  static String getEventNameNullable(final String encoded) {
    return eventMap.get(encoded);
  }
}
