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
import java.util.List;

import org.web3j.abi.EventEncoder;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthLog.LogObject;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

public class JsonRPCDemo {
  static final String TRANSFER_ID = EventEncoder.encode(ERC20.TRANSFER_EVENT);

  public static void main(final String[] args) throws IOException {

    final Web3j web3 = Web3j.build(new HttpService("http://127.0.0.1:8555"));
    final BigInteger blockNumber = web3.ethBlockNumber().send().getBlockNumber();
    final Request<?, EthLog> request =
        web3.ethGetLogs(
            new EthFilter(
                DefaultBlockParameter.valueOf(blockNumber.subtract(BigInteger.TEN)),
                DefaultBlockParameter.valueOf("latest"),
                List.of()));

    request
        .flowable()
        .flatMapIterable(EthLog::getLogs)
        .map(o -> (LogObject) o)
        .forEach(logObject -> logEvent(logObject, web3))
        .dispose();
  }

  private static void logEvent(final LogObject log, final Web3j web3j) throws Exception {
    final ReadonlyTransactionManager roTxManager =
        new ReadonlyTransactionManager(web3j, log.getAddress());
    final DefaultGasProvider contractGasProvider = new DefaultGasProvider();
    if (log.getTopics().get(0).equals(TRANSFER_ID)) {
      // query ERC 20 contract
      final ERC20 erc20 = ERC20.load(log.getAddress(), web3j, roTxManager, contractGasProvider);

      // extract event information
      final String tokenName = erc20.name().send();
      final String tokenSymbol = erc20.symbol().send();
      final String fromAddress = log.getTopics().get(1).substring(26);
      final String toAddress = log.getTopics().get(2).substring(26);
      final BigInteger value =
          new BigInteger(log.getData().substring(2), 16).divide(erc20.decimals().send());
      final BigInteger blockNumber = log.getBlockNumber();

      // stand in for DB upload
      System.out.printf(
          "%s (%s) from 0x%s to 0x%s value %s at block %s%n",
          tokenName, tokenSymbol, fromAddress, toAddress, value, blockNumber);
    } else {
      System.out.printf(
          "%s %s%n", log.getAddress(), EventsRegistry.getEventName(log.getTopics().get(0)));
    }
  }
}
