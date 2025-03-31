package org.homio.addon.firmata.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.WordUtils;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.homio.api.Context;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.Icon;
import org.homio.api.model.OptionModel;
import org.homio.api.model.endpoint.BaseDeviceEndpoint;
import org.homio.api.state.DecimalType;
import org.homio.api.state.OnOffType;
import org.homio.api.state.State;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
@Log4j2
@Getter
public class FirmataPinEndpoint extends BaseDeviceEndpoint<FirmataBaseEntity> {

  private final Map<String, Consumer<State>> listeners = new HashMap<>();
  private final Pin pin;
  private final FirmataService service;
  private final String description;
  private String pinLabel;

  FirmataPinEndpoint(String pinLabel, String description, Pin pin, @NotNull FirmataBaseEntity entity, FirmataService service) {
    super(pinNameToIcon(pinLabel), "Firmata", entity.context(), entity,
      String.valueOf(pin.getIndex()), false, EndpointType.bool);
    this.pin = pin;
    this.description = Objects.requireNonNull(description, pinLabel);
    this.service = service;
    var dto = findDto();
    this.pinLabel = Objects.requireNonNullElse(dto.name, pinLabel);
    if (dto.mode != pin.getMode()) {
      updatePinMode(pin, dto.mode);
    }
    setOrder(getPinOrder());
    setIcon(dto.icon);
    setUpdateHandler(state -> pin.setValue(state.longValue()));
    // init must be fires after setUpdateHandler to override writable
    init();
    pin.addEventListener(new PinEventListener() {
      @Override
      public void onModeChange(IOEvent event) {
        log.info("[{}]: Pin ({}) mode changed to: {}", entity.getEntityID(), pinLabel, event.getPin().getMode());
      }

      @Override
      public void onValueChange(IOEvent event) {
        State lastState = getStateFromValue(event.getValue());
        log.debug("[{}]: Update state: '{}' for pin: '{}'", entity.getEntityID(), lastState, pinLabel);
        setValue(lastState, true);
      }
    });
  }

  private static Icon pinNameToIcon(String pinLabel) {
    if (pinLabel.startsWith("SD")) {
      return new Icon("fas fa-sd-card", "#808080");
    } else if (pinLabel.startsWith("PWM")) {
      return new Icon("fas fa-wave-square", "#FFA500");
    } else if (pinLabel.startsWith("A")) {
      return new Icon("fas fa-water", "#D43C19");
    } else if (pinLabel.startsWith("SDA") || pinLabel.startsWith("SCL")) {
      return new Icon("fas fa-link", "#800080");
    } else if (pinLabel.startsWith("D")) {
      return new Icon("fas fa-digital-tachograph", "#177096");
    } else if (pinLabel.startsWith("SPI")) {
      return new Icon("fas fa-microchip", "#808080");
    } else if (pinLabel.startsWith("UART") || pinLabel.startsWith("Serial")) {
      return new Icon("fas fa-tty", "#008080");
    } else if (pinLabel.equals("RX") || pinLabel.equals("TX")) {
      return new Icon("fas fa-arrows-turn-to-dots", "#808080");
    }
    return new Icon("fas fa-map-pin", "#808080");
  }

  private void updatePinMode(Pin pin, Pin.Mode mode) {
    if (mode == null || (mode == Pin.Mode.UNSUPPORTED && !pin.getSupportedModes().contains(mode))) {
      return;
    }
    try {
      pin.setMode(mode);
    } catch (Exception e) {
      log.error("[{}]: Unable to set pin mode: {}", getDevice().getEntityID(), e.getMessage());
    }
  }

  public void init() {
    EndpointType prevEndpointType = getEndpointType();
    setWritable(getPinMode() == Pin.Mode.OUTPUT ||
                getPinMode() == Pin.Mode.SERVO ||
                getPinMode() == Pin.Mode.SHIFT);
    setReadable(
      getPinMode() == Pin.Mode.INPUT ||
      getPinMode() == Pin.Mode.PULLUP ||
      getPinMode() == Pin.Mode.ANALOG);
    switch (getPinMode()) {
      case INPUT, PULLUP, OUTPUT -> setEndpointType(EndpointType.bool);
      case PWM, ANALOG, SERVO -> {
        setEndpointType(EndpointType.dimmer);
        setMin(0F);
        setMax(1023F);
      }
    }
    recreateVariable();

    State value = getStateFromValue(pin.getValue());
    setValue(value, true);
  }

  private State getStateFromValue(long value) {
    if (getEndpointType() == EndpointType.bool) {
      return OnOffType.of(value != 0);
    } else {
      return new DecimalType(value);
    }
  }

  @Override
  public String getVariableGroupID() {
    return "firmata-" + getDevice().getIeeeAddress();
  }

  @Override
  public @NotNull String getName(boolean shortFormat) {
    return pinLabel;
  }

  @Override
  public @Nullable String getDescription() {
    Pin.Mode pinMode = getPinMode();
    if (pinMode == Pin.Mode.UNSUPPORTED) {
      return description;
    }
    String modeIcon = switch (pinMode) {
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
      case IGNORED -> "<i class=\"fas fa-eye-slash\" style=\"color: #ADB5BD\"></i>";
      default -> throw new IllegalStateException("Unexpected value: " + pinMode);
    };
    String modeStr = WordUtils.capitalizeFully(pinMode.name());
    // avoid duplicates to make description smaller
    if (description.toLowerCase().contains(modeStr.toLowerCase())) {
      modeStr = "";
    }
    return description + ". " + modeIcon + modeStr;
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
        init();
        updateEntityDto();
      }
      return null;
    }).setValue(getPinMode().name()).setOptions(modeSelectionList).setSeparatedText("field.pinMode");
    settingsBuilder.addIconPicker(getEntityID() + "icon", getIcon().getIcon())
      .setActionHandler((context, params) ->
        updateIcon(context, new Icon(params.getString("value"), getIcon().getColor())))
      .setSeparatedText("field.icon");
    settingsBuilder.addColorPicker(getEntityID() + "color", getIcon().getColor())
      .setActionHandler((context, params) ->
        updateIcon(context, new Icon(getIcon().getIcon(), params.getString("value"))))
      .setSeparatedText("field.iconColor");
    settingsBuilder.addTextInput(getEntityID() + "name", pinLabel, true)
      .setRequireApply(true)
      .setActionHandler((context, params) -> {
        pinLabel = params.getString("value");
        updateEntityDto();
        // fire update variable name
        recreateVariable();
        return null;
      })
      .setSeparatedText("field.name");
    return settingsBuilder;
  }

  // delay save icon if a user wants to change it
  private ActionResponseModel updateIcon(Context context, Icon icon) {
    context.bgp().builder(getEntityID() + "-request-change-icon-color")
      .delay(Duration.ofSeconds(3))
      .execute(() -> {
        recreateVariable();
        setIcon(icon);
        updateEntityDto();
      });
    return null;
  }

  private void updateEntityDto() {
    List<PinDto> pins = getDevice().getJsonDataList("pins", PinDto.class);
    boolean found = false;
    boolean updated = false;
    for (PinDto pinDto : pins) {
      if (pinDto.address == pin.getIndex()) {
        updated = pinDto.tryUpdate(pin.getMode(), getIcon(), pinLabel);
        found = true;
        break;
      }
    }
    if (!found) {
      pins.add(new PinDto(pin.getIndex(), getPinMode(), getIcon(), pinLabel));
      updated = true;
    }
    if (updated) {
      getDevice().setJsonDataObject("pins", pins);
      context().db().save(getDevice());
    }
  }

  private PinDto findDto() {
    List<PinDto> pins = getDevice().getJsonDataList("pins", PinDto.class);
    for (PinDto pin : pins) {
      if (pin.address == this.pin.getIndex()) {
        return pin;
      }
    }
    return new PinDto(pin.getIndex(), getPinMode(), getIcon(), pinLabel);
  }

  private int getPinOrder() {
    var label = pinLabel.split(" ")[0];
    try {
      if (label.startsWith("D")) {
        return Integer.parseInt(label.substring(1));
      } else if (label.startsWith("A")) {
        return 100 + Integer.parseInt(label.substring(1));
      } else if (label.startsWith("Pin")) {
        return 200 + Integer.parseInt(label.substring(3));
      } else if (label.startsWith("SD")) {
        return 250 + Integer.parseInt(label.substring(2));
      } else if (label.equals("SDA")) {
        return 300;
      } else if (label.equals("SCL")) {
        return 301;
      } else if (label.equals("RX")) {
        return 350;
      } else if (label.equals("TX")) {
        return 355;
      } else if (label.startsWith("SPI_")) {
        return 400 + label.charAt(4);
      }
    } catch (Exception ignore) {
    }
    return 1000;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PinDto {
    private byte address;
    private @Nullable Pin.Mode mode;
    private @Nullable Icon icon;
    private @Nullable String name;

    public boolean tryUpdate(Pin.Mode mode, Icon icon, String name) {
      long hash = Objects.hash(address, this.mode, this.icon, this.name);
      this.mode = mode;
      this.icon = icon;
      this.name = name;
      return hash != Objects.hash(address, this.mode, this.icon, this.name);
    }
  }
}