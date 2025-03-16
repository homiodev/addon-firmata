package org.homio.addon.firmata;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.homio.addon.firmata.setting.FirmataScanPortRangeSetting;
import org.homio.api.Context;
import org.homio.api.service.discovery.ItemDiscoverySupport;
import org.homio.hquery.ProgressBar;
import org.homio.hquery.hardware.network.NetworkHardwareRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

@Log4j2
@Component
@RequiredArgsConstructor
public class FirmataNetworkControllerScanner implements ItemDiscoverySupport {

  private static final int port = 3132;
  private static final int timeout = 2000;

  private final NetworkHardwareRepository networkHardwareRepository;

  @Override
  public String getName() {
    return "firmata-network";
  }

  @Override
  public @Nullable DeviceScannerResult scan(@NotNull Context context, @NotNull ProgressBar progressBar) {
    DeviceScannerResult result = new DeviceScannerResult();
    Set<String> existedDevices = new HashSet<>();
    Map<String, Callable<Integer>> tasks = new HashMap<>();
    Set<String> ipRangeList = context.setting().getValue(FirmataScanPortRangeSetting.class);
    for (String ipRange : ipRangeList) {
      tasks.putAll(networkHardwareRepository.buildPingIpAddressTasks(ipRange, log::info, Set.of(port), timeout, (ipAddress, integer) -> {
        if (!FirmataEntrypoint.foundController(context, null, null, ipAddress, null)) {
          existedDevices.add(ipAddress);
          result.getExistedCount().incrementAndGet();
        }
      }));
    }

    List<Integer> availableIpAddresses = context.bgp().runInBatchAndGet("firmata-ip-scan",
      Duration.ofMinutes(5), 8, tasks,
      completedTaskCount -> progressBar.progress(100 / 256F * completedTaskCount, "Firmata bundle scanned " + completedTaskCount + "/255"));
    long availableIpAddressesSize = availableIpAddresses.stream().filter(Objects::nonNull).count();
    log.debug("Found {} devices", availableIpAddressesSize);
    result.getNewCount().set((int) (availableIpAddressesSize - existedDevices.size()));
    return result;
  }
}
