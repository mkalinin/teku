/*
 * Copyright 2020 ConsenSys AG.
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

package tech.pegasys.teku.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.teku.cli.BeaconNodeCommand.CONFIG_FILE_OPTION_NAME;
import static tech.pegasys.teku.cli.options.LoggingOptions.DEFAULT_LOG_FILE;
import static tech.pegasys.teku.cli.options.LoggingOptions.DEFAULT_LOG_FILE_NAME_PATTERN;
import static tech.pegasys.teku.cli.options.MetricsOptions.DEFAULT_METRICS_CATEGORIES;
import static tech.pegasys.teku.util.config.LoggingDestination.DEFAULT_BOTH;
import static tech.pegasys.teku.util.config.StateStorageMode.PRUNE;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.pegasys.teku.util.config.Eth1Address;
import tech.pegasys.teku.util.config.LoggingDestination;
import tech.pegasys.teku.util.config.NetworkDefinition;
import tech.pegasys.teku.util.config.TekuConfiguration;
import tech.pegasys.teku.util.config.TekuConfigurationBuilder;

public class BeaconNodeCommandTest extends AbstractBeaconNodeCommandTest {

  @Test
  public void unknownOptionShouldDisplayShortHelpMessage() {
    final String[] args = {"--hlp"};

    beaconNodeCommand.parse(args);
    String str = getCommandLineOutput();
    assertThat(str).contains("Unknown option");
    assertThat(str).contains("To display full help:");
    assertThat(str).contains("--help");
    assertThat(str).doesNotContain("Default");
  }

  @Test
  public void invalidValueShouldDisplayShortHelpMessage() {
    final String[] args = {"--metrics-enabled=bob"};

    beaconNodeCommand.parse(args);
    String str = getCommandLineOutput();
    assertThat(str).contains("Invalid value");
    assertThat(str).contains("To display full help:");
    assertThat(str).contains("--help");
    assertThat(str).doesNotContain("Default");
  }

  @Test
  public void helpOptionShouldDisplayFullHelp() {
    final String[] args = {"--help"};

    beaconNodeCommand.parse(args);
    String str = getCommandLineOutput();
    assertThat(str).contains("Description:");
    assertThat(str).contains("Default");
    assertThat(str).doesNotContain("To display full help:");
  }

  @Test
  public void loadDefaultsWhenNoArgsArePassed() {
    // p2p-enabled default is "true" which require p2p-private-key-file to be non-null
    final String[] args = {"--data-path", dataPath.toString(), "--p2p-enabled", "false"};

    beaconNodeCommand.parse(args);

    assertTekuConfiguration(expectedDefaultConfigurationBuilder().build());
  }

  @Test
  public void overrideEnvironmentValuesIfKeyIsPresentInCLIOptions() {
    final String[] args = createCliArgs();
    args[5] = "1.2.3.5";
    beaconNodeCommand =
        new BeaconNodeCommand(
            outputWriter,
            errorWriter,
            Collections.singletonMap("TEKU_P2P_INTERFACE", "1.2.3.4"),
            startAction);

    beaconNodeCommand.parse(args);

    assertTekuConfiguration(expectedConfigurationBuilder().setP2pInterface("1.2.3.5").build());
  }

  @Test
  public void overrideConfigFileValuesIfKeyIsPresentInEnvironmentVariables() throws IOException {
    final Path configFile = createConfigFile();
    final String[] args = {CONFIG_FILE_OPTION_NAME, configFile.toString()};
    beaconNodeCommand =
        new BeaconNodeCommand(
            outputWriter,
            errorWriter,
            Collections.singletonMap("TEKU_P2P_INTERFACE", "1.2.3.5"),
            startAction);

    beaconNodeCommand.parse(args);

    assertTekuConfiguration(
        expectedCompleteConfigInFileBuilder().setP2pInterface("1.2.3.5").build());
  }

  @Test
  public void overrideConfigFileValuesIfKeyIsPresentInCLIOptions() throws IOException {
    final Path configFile = createConfigFile();
    final String[] args = {
      CONFIG_FILE_OPTION_NAME, configFile.toString(), "--p2p-interface", "1.2.3.5"
    };

    beaconNodeCommand.parse(args);

    assertTekuConfiguration(
        expectedCompleteConfigInFileBuilder().setP2pInterface("1.2.3.5").build());
  }

  @Test
  public void overrideDefaultValuesIfKeyIsPresentInEnvironmentVariables() {
    beaconNodeCommand =
        new BeaconNodeCommand(
            outputWriter,
            errorWriter,
            Map.of("TEKU_DATA_PATH", dataPath.toString(), "TEKU_P2P_ENABLED", "false"),
            startAction);

    beaconNodeCommand.parse(new String[] {});

    assertTekuConfiguration(expectedDefaultConfigurationBuilder().build());
  }

  @Test
  public void overrideDefaultValuesIfKeyIsPresentInCLIOptions() {
    final String[] args = createCliArgs();

    beaconNodeCommand.parse(args);

    assertTekuConfiguration(expectedConfigurationBuilder().build());
  }

  @Test
  public void overrideDefaultValuesIfKeyIsPresentInConfigFile() throws IOException {
    final Path configFile = createConfigFile();
    final String[] args = {CONFIG_FILE_OPTION_NAME, configFile.toString()};

    beaconNodeCommand.parse(args);

    assertTekuConfiguration(expectedCompleteConfigInFileBuilder().build());
  }

  @Test
  public void interopEnabled_shouldNotRequireAValue() {
    final TekuConfiguration tekuConfiguration =
        getTekuConfigurationFromArguments("--Xinterop-enabled");
    assertThat(tekuConfiguration.isInteropEnabled()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"OFF", "FATAL", "WARN", "INFO", "DEBUG", "TRACE", "ALL"})
  public void loglevel_shouldAcceptValues(String level) {
    final String[] args = {"--logging", level};
    beaconNodeCommand.parse(args);
    assertThat(beaconNodeCommand.getLogLevel().toString()).isEqualTo(level);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"Off", "Fatal", "WaRN", "InfO", "DebUG", "trACE", "all"})
  public void loglevel_shouldAcceptValuesMixedCase(String level) {
    final String[] args = {"--logging", level};
    beaconNodeCommand.parse(args);
    assertThat(beaconNodeCommand.getLogLevel().toString()).isEqualTo(level.toUpperCase());
  }

  @Test
  public void logLevel_shouldRejectInvalidValues() {
    final String[] args = {"--logging", "invalid"};
    beaconNodeCommand.parse(args);
    String str = getCommandLineOutput();
    assertThat(str).contains("'invalid' is not a valid log level. Supported values are");
  }

  private Path createConfigFile() throws IOException {
    final URL configFile = this.getClass().getResource("/complete_config.yaml");
    final String updatedConfig =
        Resources.toString(configFile, UTF_8)
            .replace("data-path: \".\"", "data-path: \"" + dataPath.toString() + "\"");
    return createTempFile(updatedConfig.getBytes(UTF_8));
  }

  private String[] createCliArgs() {
    return new String[] {
      "--network", "minimal",
      "--p2p-enabled", "false",
      "--p2p-interface", "1.2.3.4",
      "--p2p-port", "1234",
      "--p2p-discovery-enabled", "false",
      "--p2p-advertised-port", "9000",
      "--p2p-private-key-file", "path/to/file",
      "--Xinterop-genesis-time", "1",
      "--Xinterop-owned-validator-start-index", "0",
      "--Xinterop-owned-validator-count", "64",
      "--initial-state", "",
      "--Xinterop-number-of-validators", "64",
      "--Xinterop-enabled", "true",
      "--eth1-deposit-contract-address", "0x77f7bED277449F51505a4C54550B074030d989bC",
      "--eth1-endpoint", "http://localhost:8545",
      "--metrics-enabled", "false",
      "--metrics-port", "8008",
      "--metrics-interface", "127.0.0.1",
      "--metrics-categories", "BEACON,LIBP2P,NETWORK,EVENTBUS,JVM,PROCESS",
      "--data-path", dataPath.toString(),
      "--data-storage-mode", "prune",
      "--rest-api-port", "5051",
      "--rest-api-docs-enabled", "false",
      "--rest-api-enabled", "false",
      "--rest-api-interface", "127.0.0.1"
    };
  }

  private TekuConfigurationBuilder expectedDefaultConfigurationBuilder() {
    return expectedConfigurationBuilder()
        .setEth1DepositContractAddress(null)
        .setEth1Endpoint(null)
        .setMetricsCategories(
            DEFAULT_METRICS_CATEGORIES.stream().map(Object::toString).collect(Collectors.toList()))
        .setP2pAdvertisedPort(OptionalInt.empty())
        .setP2pDiscoveryEnabled(true)
        .setP2pInterface("0.0.0.0")
        .setP2pPort(9000)
        .setP2pPrivateKeyFile(null)
        .setInteropEnabled(false)
        .setInteropGenesisTime(0)
        .setInteropOwnedValidatorCount(0)
        .setLogDestination(DEFAULT_BOTH)
        .setLogFile(DEFAULT_LOG_FILE)
        .setLogFileNamePattern(DEFAULT_LOG_FILE_NAME_PATTERN);
  }

  private TekuConfigurationBuilder expectedCompleteConfigInFileBuilder() {
    return expectedConfigurationBuilder()
        .setLogFile("teku.log")
        .setLogDestination(LoggingDestination.BOTH)
        .setLogFileNamePattern("teku_%d{yyyy-MM-dd}.log");
  }

  private TekuConfigurationBuilder expectedConfigurationBuilder() {
    Eth1Address address = Eth1Address.fromHexString("0x77f7bED277449F51505a4C54550B074030d989bC");
    return TekuConfiguration.builder()
        .setNetwork(NetworkDefinition.fromCliArg("minimal"))
        .setP2pEnabled(false)
        .setP2pInterface("1.2.3.4")
        .setP2pPort(1234)
        .setP2pDiscoveryEnabled(false)
        .setP2pDiscoveryBootnodes(Collections.emptyList())
        .setP2pAdvertisedPort(OptionalInt.of(9000))
        .setP2pAdvertisedIp(Optional.empty())
        .setP2pPrivateKeyFile("path/to/file")
        .setP2pPeerLowerBound(20)
        .setP2pPeerUpperBound(30)
        .setP2pStaticPeers(Collections.emptyList())
        .setInteropGenesisTime(1)
        .setInitialState("")
        .setInteropOwnedValidatorStartIndex(0)
        .setInteropOwnedValidatorCount(64)
        .setInteropNumberOfValidators(64)
        .setInteropEnabled(true)
        .setEth1DepositContractAddress(address)
        .setEth1Endpoint("http://localhost:8545")
        .setEth1Enabled(true)
        .setMetricsEnabled(false)
        .setMetricsPort(8008)
        .setMetricsInterface("127.0.0.1")
        .setMetricsCategories(
            Arrays.asList("BEACON", "LIBP2P", "NETWORK", "EVENTBUS", "JVM", "PROCESS"))
        .setMetricsHostWhitelist(List.of("127.0.0.1", "localhost"))
        .setLogColorEnabled(true)
        .setLogDestination(DEFAULT_BOTH)
        .setLogFile(DEFAULT_LOG_FILE)
        .setLogFileNamePattern(DEFAULT_LOG_FILE_NAME_PATTERN)
        .setLogIncludeEventsEnabled(true)
        .setValidatorKeystoreFiles(Collections.emptyList())
        .setValidatorKeystorePasswordFiles(Collections.emptyList())
        .setValidatorExternalSignerTimeout(1000)
        .setDataPath(dataPath.toString())
        .setDataStorageMode(PRUNE)
        .setRestApiPort(5051)
        .setRestApiDocsEnabled(false)
        .setRestApiEnabled(false)
        .setRestApiInterface("127.0.0.1");
  }

  private void assertTekuConfiguration(final TekuConfiguration expected) {
    final TekuConfiguration actual = getResultingTekuConfiguration();
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  private Path createTempFile(final byte[] contents) throws IOException {
    final Path file = java.nio.file.Files.createTempFile("config", "yaml");
    java.nio.file.Files.write(file, contents);
    file.toFile().deleteOnExit();
    return file;
  }
}
