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

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;

import org.web3j.abi.EventEncoder;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.protocol.websocket.events.Log;
import org.web3j.protocol.websocket.events.LogNotification;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

public class WebSocketsDemo {
  static final String TRANSFER_ID = EventEncoder.encode(ERC20.TRANSFER_EVENT);
  private final Web3j web3j;
  private final WebSocketService web3jService;

  private WebSocketsDemo(final String serviceUrl) {
    final WebSocketClient webSocketClient = new WebSocketClient(URI.create(serviceUrl));
    web3jService = new WebSocketService(webSocketClient, false);
    web3j = Web3j.build(web3jService);
  }

  public static void main(final String[] args) throws IOException {
    new WebSocketsDemo("ws://127.0.0.1:8556").run();
  }

  private void run() throws ConnectException {

    web3jService.connect();
    web3j
        .logsNotifications(List.of(), List.of())
        .blockingSubscribe(this::logEvent);
  }

  private void logEvent(final LogNotification logNotification) {
    final Log log = logNotification.getParams().getResult();
    final ReadonlyTransactionManager roTxManager =
        new ReadonlyTransactionManager(web3j, log.getAddress());
    final DefaultGasProvider contractGasProvider = new DefaultGasProvider();

    final String eventType = log.getTopics().get(0);
    if (eventType.equals(TRANSFER_ID)) {
      final String address = log.getAddress();
      System.out.printf("%s ", address);
      try {
        // query ERC 20 contract
        final ERC20 erc20 = ERC20.load(log.getAddress(), web3j, roTxManager, contractGasProvider);
        final String tokenName = erc20.name().sendAsync().get();
        final String tokenSymbol = erc20.symbol().sendAsync().get();
        final BigInteger decimals = erc20.decimals().sendAsync().get();
        // extract event information
        final String fromAddress = log.getTopics().get(1).substring(26);
        final String toAddress = log.getTopics().get(2).substring(26);
        final BigInteger value = new BigInteger(log.getData().substring(2), 16).divide(decimals);
        final String blockNumber = log.getBlockNumber();

        // stand in for DB upload
        System.out.printf(
            "Transfer %s (%s) from 0x%s to 0x%s value %s at block %s%n",
            tokenName, tokenSymbol, fromAddress, toAddress, value, blockNumber);
      } catch (final Exception e) {
        System.out.printf(" - Error extracting data - %s%n", e.toString());
      }

    } else {
      final String name = EventsRegistry.getEventNameNullable(eventType);
      if (name != null) {
        System.out.printf(
            "%s %s%n", log.getAddress(), EventsRegistry.getEventName(log.getTopics().get(0)));
      }
    }
  }
}
