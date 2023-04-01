package org.homio.bundle.firmata.repository;

import org.homio.bundle.api.repository.AbstractRepository;
import org.homio.bundle.firmata.model.FirmataBaseEntity;
import org.springframework.stereotype.Repository;

@Repository
public class FirmataDeviceRepository extends AbstractRepository<FirmataBaseEntity> {

  public FirmataDeviceRepository() {
    super(FirmataBaseEntity.class);
  }
}
