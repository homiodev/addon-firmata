package org.homio.bundle.firmata.arduino;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.homio.bundle.api.EntityContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/rest/arduino")
@RequiredArgsConstructor
public class ArduinoController {

  private final EntityContext entityContext;

}
