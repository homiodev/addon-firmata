package org.homio.addon.firmata;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.homio.addon.firmata.setting.FirmataScanIpRangeSetting;
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

  public static final int FIRMATA_PORT = 3030;
  private static final int timeout = 5000;

  private final NetworkHardwareRepository networkHardwareRepository;

  @Override
  public @NotNull String getName() {
    return "firmata-network";
  }

  @Override
  public @Nullable DeviceScannerResult scan(@NotNull Context context, @NotNull ProgressBar progressBar) {
    DeviceScannerResult result = new DeviceScannerResult();
    Set<String> existedDevices = new HashSet<>();
    Map<String, Callable<Integer>> tasks = new HashMap<>();
    Set<String> ipRangeList = context.setting().getValue(FirmataScanIpRangeSetting.class);

    for (String ipRange : ipRangeList) {
      tasks.putAll(networkHardwareRepository.buildPingIpAddressTasks(ipRange, log::info, Set.of(FIRMATA_PORT), timeout, (ipAddress, integer) -> {
        // try to get board info
        try {
          if (FirmataEntrypoint.firmataFoundFromScanner(context, ipAddress)) {
            existedDevices.add(ipAddress);
            result.getExistedCount().incrementAndGet();
          }
        } catch (Exception ex) {
          log.warn("Arduino: unable to get board info for ip: {}", ipAddress, ex);
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
