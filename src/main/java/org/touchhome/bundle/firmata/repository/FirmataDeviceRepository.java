package org.touchhome.bundle.firmata.repository;

import org.springframework.stereotype.Repository;
import org.touchhome.bundle.api.repository.AbstractRepository;
import org.touchhome.bundle.firmata.model.FirmataBaseEntity;

@Repository
public class FirmataDeviceRepository extends AbstractRepository<FirmataBaseEntity> {

  public FirmataDeviceRepository() {
    super(FirmataBaseEntity.class);
  }
}
