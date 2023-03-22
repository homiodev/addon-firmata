package org.touchhome.bundle.firmata.arduino;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.touchhome.bundle.api.EntityContext;

@Log4j2
@RestController
@RequestMapping("/rest/arduino")
@RequiredArgsConstructor
public class ArduinoController {

  private final EntityContext entityContext;

}
