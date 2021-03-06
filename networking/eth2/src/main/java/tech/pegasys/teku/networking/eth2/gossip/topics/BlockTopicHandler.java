/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.networking.eth2.gossip.topics;

import com.google.common.eventbus.EventBus;
import tech.pegasys.teku.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.datastructures.state.ForkInfo;
import tech.pegasys.teku.networking.eth2.gossip.encoding.GossipEncoding;
import tech.pegasys.teku.networking.eth2.gossip.events.GossipedBlockEvent;
import tech.pegasys.teku.networking.eth2.gossip.topics.validation.BlockValidator;
import tech.pegasys.teku.networking.eth2.gossip.topics.validation.ValidationResult;

public class BlockTopicHandler extends Eth2TopicHandler<SignedBeaconBlock> {
  public static String TOPIC_NAME = "beacon_block";
  private final BlockValidator blockValidator;

  public BlockTopicHandler(
      final GossipEncoding gossipEncoding,
      final ForkInfo forkInfo,
      final BlockValidator blockValidator,
      final EventBus eventBus) {
    super(gossipEncoding, forkInfo, eventBus);
    this.blockValidator = blockValidator;
  }

  @Override
  protected Object createEvent(final SignedBeaconBlock block) {
    return new GossipedBlockEvent(block);
  }

  @Override
  public String getTopicName() {
    return TOPIC_NAME;
  }

  @Override
  protected Class<SignedBeaconBlock> getValueType() {
    return SignedBeaconBlock.class;
  }

  @Override
  protected ValidationResult validateData(final SignedBeaconBlock block) {
    return blockValidator.validate(block);
  }
}
