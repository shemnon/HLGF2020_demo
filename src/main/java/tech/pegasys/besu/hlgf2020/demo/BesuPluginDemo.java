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

import org.hyperledger.besu.plugin.BesuContext;
import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.data.LogWithMetadata;
import org.hyperledger.besu.plugin.services.BesuEvents;

import java.util.List;
import java.util.Optional;

import com.google.auto.service.AutoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.web3j.abi.EventEncoder;
import org.web3j.contracts.eip20.generated.ERC20;

@AutoService(BesuPlugin.class)
public class BesuPluginDemo implements BesuPlugin {

  private static Logger LOG = LogManager.getLogger();

  private static String PLUGIN_NAME = "erc-20-transfers";

  @Override
  public Optional<String> getName() {
    return Optional.of("ERC-20 Transfers");
  }

  private BesuContext context;

  @Override
  public void register(final BesuContext context) {
    LOG.info("Registering ERC-20 Transfers Plugin");
    this.context = context;
  }

  @Override
  public void start() {
    LOG.info("Starting ERC-20 Transfers Plugin");
    context
        .getService(BesuEvents.class)
        .ifPresentOrElse(this::startEvents, () -> LOG.error("Could not obtain BesuEvents"));
  }

  @Override
  public void stop() {
    LOG.info("Starting ERC-20 Transfers Plugin");
    context
        .getService(BesuEvents.class)
        .ifPresentOrElse(this::stopEvents, () -> LOG.error("Could not obtain BesuEvents"));
  }

  private long listenerIdentifier;

  private void startEvents(final BesuEvents events) {
    listenerIdentifier =
        events.addLogListener(
            List.of(), List.of(List.of(Bytes32.fromHexString(TRANSFER_ID))), this::logEvent);
  }

  private void stopEvents(final BesuEvents events) {
    events.removeBlockPropagatedListener(listenerIdentifier);
  }

  static final String TRANSFER_ID = EventEncoder.encode(ERC20.TRANSFER_EVENT);
  static final Bytes32 TRANSFER_ID_BYTES = Bytes32.fromHexString(TRANSFER_ID);

  private void logEvent(final LogWithMetadata logWithMetadata) {
    try {
      if (logWithMetadata.getTopics().get(0).equals(TRANSFER_ID_BYTES)) {
        // TODO besu plugins should support static contract executions
        final String fromAddress = logWithMetadata.getTopics().get(1).toShortHexString();
        final String toAddress = logWithMetadata.getTopics().get(2).toShortHexString();
        final UInt256 value = UInt256.fromBytes(logWithMetadata.getData());
        final long blockNumber = logWithMetadata.getBlockNumber();

        System.out.printf(
            "%s  from %s to %s value %s at block %d%n",
            logWithMetadata.getLogger(), fromAddress, toAddress, value, blockNumber);
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
