package org.touchhome.bundle.firmata;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.service.scan.BaseItemsDiscovery;
import org.touchhome.bundle.api.service.scan.MicroControllerScanner;
import org.touchhome.bundle.firmata.setting.FirmataScanPortRangeSetting;
import org.touchhome.bundle.api.ui.field.ProgressBar;
import org.touchhome.bundle.hquery.hardware.network.NetworkHardwareRepository;

@Log4j2
@Component
@RequiredArgsConstructor
public class FirmataNetworkControllerScanner implements MicroControllerScanner {

  private static final int port = 3132;
  private static final int timeout = 2000;

  private final NetworkHardwareRepository networkHardwareRepository;

  @Override
  public String getName() {
    return "firmata-network";
  }

  @Override
  public BaseItemsDiscovery.DeviceScannerResult scan(EntityContext entityContext, ProgressBar progressBar, String headerConfirmButtonKey) {
    Set<String> existedDevices = new HashSet<>();
    Map<String, Callable<Integer>> tasks = new HashMap<>();
    Set<String> ipRangeList = entityContext.setting().getValue(FirmataScanPortRangeSetting.class);
    for (String ipRange : ipRangeList) {
      tasks.putAll(networkHardwareRepository.buildPingIpAddressTasks(ipRange, log, Collections.singleton(port), timeout, (ipAddress, integer) -> {
        if (!FirmataBundleEntrypoint.foundController(entityContext, null, null, ipAddress, headerConfirmButtonKey)) {
          existedDevices.add(ipAddress);
        }
      }));
    }

    List<Integer> availableIpAddresses = entityContext.bgp().runInBatchAndGet("firmata-ip-scan",
        Duration.ofMinutes(5), 8, tasks,
        completedTaskCount -> progressBar.progress(100 / 256F * completedTaskCount, "Firmata bundle scanned " + completedTaskCount + "/255"));
    long availableIpAddressesSize = availableIpAddresses.stream().filter(Objects::nonNull).count();
    log.debug("Found {} devices", availableIpAddressesSize);
    return new BaseItemsDiscovery.DeviceScannerResult(existedDevices.size(), (int) (availableIpAddressesSize - existedDevices.size()));
  }
}
