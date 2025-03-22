package org.homio.addon.firmata.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.WordUtils;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.homio.api.model.OptionModel;
import org.homio.api.model.endpoint.BaseDeviceEndpoint;
import org.homio.api.state.DecimalType;
import org.homio.api.state.OnOffType;
import org.homio.api.state.State;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.homio.addon.firmata.model.PinoutModel.getPinOrder;

@SuppressWarnings("rawtypes")
@Log4j2
@Getter
public class FirmataPinEndpoint extends BaseDeviceEndpoint<FirmataBaseEntity> {

  private final Map<String, Consumer<State>> listeners = new HashMap<>();
  private final Pin pin;
  private final String pinLabel;
  private final FirmataService service;
  private @NotNull FirmataBaseEntity entity;

  FirmataPinEndpoint(String pinLabel, Pin pin, @NotNull FirmataBaseEntity entity, FirmataService service) {
    super(PinoutModel.pinNameToIcon(pinLabel), "Firmata", entity.context(), entity,
      String.valueOf(pin.getIndex()), false, EndpointType.bool);
    this.pin = pin;
    this.entity = entity;
    this.service = service;
    this.pinLabel = pinLabel;
    var dto = findDto();
    if (dto.mode != pin.getMode()) {
      updatePinMode(pin, dto.mode);
    }
    setOrder(getPinOrder(pinLabel));
    setUpdateHandler(state -> pin.setValue(state.longValue()));
    // writable after updateHandler
    setWritable();
    pin.addEventListener(new PinEventListener() {
      @Override
      public void onModeChange(IOEvent event) {
        log.info("[{}]: Pin ({}) mode changed to: {}", entity.getEntityID(), pinLabel, event.getPin().getMode());
      }

      @Override
      public void onValueChange(IOEvent event) {
        var lastState = new DecimalType(event.getValue());
        log.debug("[{}]: Update state: '{}' for pin: '{}'", entity.getEntityID(), lastState, pinLabel);
        setValue(lastState, true);
      }
    });
  }

  private void updatePinMode(Pin pin, Pin.Mode mode) {
    if (mode == null || (mode == Pin.Mode.UNSUPPORTED && !pin.getSupportedModes().contains(mode))) {
      return;
    }
    try {
      pin.setMode(mode);
    } catch (Exception e) {
      log.error("[{}]: Unable to set pin mode: {}", entity.getEntityID(), e.getMessage());
    }
  }

  private void setWritable() {
    EndpointType prevEndpointType = getEndpointType();
    setWritable(getPinMode() == Pin.Mode.OUTPUT ||
                getPinMode() == Pin.Mode.ANALOG ||
                getPinMode() == Pin.Mode.SERVO);
    switch (getPinMode()) {
      case INPUT, PULLUP -> setEndpointType(EndpointType.bool);
      case PWM, ANALOG, SERVO -> {
        setEndpointType(EndpointType.dimmer);
        setMin(0F);
        setMax(1023F);
      }
    }
    if (getVariableID() != null && prevEndpointType != getEndpointType()) {
      deleteVariableID();
    }
    getOrCreateVariable();

    State value = getValue();
    if (getEndpointType() == EndpointType.bool) {
      if (!(value instanceof OnOffType)) {
        setValue(OnOffType.OFF, true);
      }
    } else if (getEndpointType() == EndpointType.number || getEndpointType() == EndpointType.dimmer) {
      if (!(value instanceof DecimalType)) {
        setValue(DecimalType.ZERO, true);
      }
    }
  }

  @Override
  public String getVariableGroupID() {
    return "firmata-" + entity.getIeeeAddress();
  }

  @Override
  public @NotNull String getName(boolean shortFormat) {
    return pinLabel;
  }

  @Override
  public @Nullable String getDescription() {
    String modeIcon = switch (getPinMode()) {
      case INPUT -> "<i class=\"fas fa-sign-in-alt\" style=\"color: #D4A373\"></i>";
      case OUTPUT -> "<i class=\"fas fa-sign-out-alt\" style=\"color: #E76F51\"></i>";
      case ANALOG -> "<i class=\"fas fa-wave-square\" style=\"color: #8ECAE6\"></i>";
      case PWM -> "<i class=\"fas fa-wave-square\" style=\"color: #9B5DE5\"></i>";
      case SERVO -> "<i class=\"fas fa-cog\" style=\"color: #2A9D8F\"></i>";
      case SHIFT -> "<i class=\"fas fa-exchange-alt\" style=\"color: #00B4D8\"></i>";
      case I2C -> "<i class=\"fas fa-sitemap\" style=\"color: #3A86FF\"></i>";
      case ONEWIRE -> "<i class=\"fas fa-link\" style=\"color: #C77DFF\"></i>";
      case STEPPER -> "<i class=\"fas fa-cogs\" style=\"color: #02P3E8A\"></i>";
      case ENCODER -> "<i class=\"fas fa-sync\" style=\"color: #FF70A6\"></i>";
      case SERIAL -> "<i class=\"fas fa-terminal\" style=\"color: #52B788\"></i>";
      case PULLUP -> "<i class=\"fas fa-arrow-up\" style=\"color: #B08968\"></i>";
      case UNSUPPORTED -> "<i class=\"fas fa-question-circle\" style=\"color: #6C757D\"></i>";
      case IGNORED -> "<i class=\"fas fa-eye-slash\" style=\"color: #ADB5BD\"></i>";
    };

    String modeStr = WordUtils.capitalizeFully(getPinMode().name());
    return pinLabel + ". " + modeIcon + modeStr;
  }

  private Pin.Mode getPinMode() {
    return pin.getMode() == null ? Pin.Mode.UNSUPPORTED : pin.getMode();
  }

  @Override
  public @Nullable UIInputBuilder createSettingsBuilder() {
    UIInputBuilder settingsBuilder = context().ui().inputBuilder();
    var modeSelectionList = OptionModel.enumList(Pin.Mode.class, pinMode ->
      pin.getSupportedModes().contains(pinMode));
    settingsBuilder.addSelectBox(getEntityID() + "mode", (context, params) -> {
      String modeStr = params.getString("value");
      var mode = Pin.Mode.valueOf(modeStr);

      if (mode != getPinMode()) {
        pin.setMode(mode);
        setWritable();
        updateEntityDto(true);
      }
      return null;
    }).setValue(getPinMode().name()).setOptions(modeSelectionList).setSeparatedText("field.pinMode");
    return settingsBuilder;
  }

  public void setEntity(@NotNull FirmataBaseEntity entity) {
    this.entity = entity;
    updateEntityDto(false);
  }

  private void updateEntityDto(boolean createIfNotFound) {
    List<PinDto> pins = entity.getJsonDataList("pins", PinDto.class);
    boolean found = false;
    boolean updated = false;
    for (PinDto pinDto : pins) {
      if (pinDto.address == pin.getIndex()) {
        if (pinDto.mode != getPinMode()) {
          pinDto.mode = getPinMode();
          updated = true;
        }
        found = true;
        break;
      }
    }
    if (!found && createIfNotFound) {
      pins.add(new PinDto(pin.getIndex(), getPinMode()));
      updated = true;
    }
    if (updated) {
      entity.setJsonDataObject("pins", pins);
      entity = context().db().save(entity);
    }
  }

  private PinDto findDto() {
    List<PinDto> pins = entity.getJsonDataList("pins", PinDto.class);
    for (PinDto pin : pins) {
      if (pin.address == this.pin.getIndex()) {
        return pin;
      }
    }
    return new PinDto(pin.getIndex(), getPinMode());
  }

  @Override
  public String toString() {
    return pinLabel;
  }

  public void removePin() {
    List<PinDto> pins = entity.getJsonDataList("pins", PinDto.class);
    for (PinDto pin : pins) {
      if (pin.address == this.pin.getIndex()) {
        pins.remove(pin);
        entity.setJsonDataObject("pins", pins);
        entity = context().db().save(entity);
      }
    }
    if (getVariableID() != null) {
      context().db().delete(getVariableID());
    }
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PinDto {
    private byte address;
    private @Nullable Pin.Mode mode;
  }
}