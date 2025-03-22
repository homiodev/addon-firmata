package org.homio.addon.firmata.workspace;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.firmata4j.Pin;
import org.homio.addon.firmata.model.FirmataBaseEntity;
import org.homio.api.AddonEntrypoint;
import org.homio.api.Context;
import org.homio.api.workspace.WorkspaceBlock;
import org.homio.api.workspace.scratch.MenuBlock;
import org.homio.api.workspace.scratch.Scratch3Block;
import org.homio.api.workspace.scratch.Scratch3ExtensionBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.function.ThrowingBiFunction;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.util.function.ThrowingFunction;

import java.util.concurrent.TimeUnit;

@Log4j2
public abstract class Scratch3FirmataBaseBlock extends Scratch3ExtensionBlocks {

  public static final String FIRMATA_ID_MENU = "firmataIdMenu";
  public static final String REST_PIN = "rest/firmata/pin/";
  public static final String PIN = "PIN";
  static final String FIRMATA = "FIRMATA";

  final MenuBlock.ServerMenuBlock firmataIdMenu;

  public Scratch3FirmataBaseBlock(String color, Context context, AddonEntrypoint addonEntrypoint, String idSuffix) {
    super(color, context, addonEntrypoint, idSuffix);
    this.firmataIdMenu = menuServerItems(FIRMATA_ID_MENU, FirmataBaseEntity.class, "Firmata");
  }

  static Integer getPin(WorkspaceBlock workspaceBlock, MenuBlock.ServerMenuBlock menuBlock) {
    String pinNum = workspaceBlock.getMenuValue(PIN, menuBlock);
    return Integer.valueOf(pinNum);
  }

  void addPinMenu(Scratch3Block scratch3Block, MenuBlock.ServerMenuBlock pinMenuBlock, String overrideColor) {
    scratch3Block.addArgument(FIRMATA, this.firmataIdMenu);
    scratch3Block.addArgument(PIN, pinMenuBlock);
    scratch3Block.overrideColor(overrideColor);
  }

  @SneakyThrows
  <T> T execute(WorkspaceBlock workspaceBlock, boolean waitDeviceForReady, ThrowingFunction<FirmataBaseEntity<?>, T> consumer) {
    FirmataBaseEntity entity = workspaceBlock.getMenuValueEntity(FIRMATA, this.firmataIdMenu);

    if (entity != null && entity.getStatus().isOnline()) {
      return consumer.apply(entity);
    }
    return null;
  }

  @SneakyThrows
  void execute(WorkspaceBlock workspaceBlock, boolean waitDeviceForReady, ThrowingConsumer<FirmataBaseEntity<?>> consumer) {
    execute(workspaceBlock, waitDeviceForReady, (ThrowingFunction<FirmataBaseEntity<?>, Void>) entity -> {
      consumer.accept(entity);
      return null;
    });
  }

  @SneakyThrows
  <T> T execute(@NotNull WorkspaceBlock workspaceBlock, boolean waitDeviceForReady, @Nullable MenuBlock.ServerMenuBlock pinMenuBlock,
                @NotNull ThrowingBiFunction<FirmataBaseEntity<?>, Pin, T> consumer) {
    Integer pinNum = pinMenuBlock == null ? null : getPin(workspaceBlock, pinMenuBlock);
    String deviceId = workspaceBlock.getMenuValue(FIRMATA, this.firmataIdMenu);
    FirmataBaseEntity<?> entity = context.db().get(deviceId);
    if (entity == null) {
      return null;
    }

    if (waitDeviceForReady && !entity.getStatus().isOnline()) {
      var readyLock = workspaceBlock.getLockManager().getLock(workspaceBlock, "firmata-ready-" + entity.getDeviceID());
      if (readyLock.await(workspaceBlock, 60, TimeUnit.SECONDS)) {
        // fetch updated entity
        entity = context.db().getRequire(deviceId);
        if (entity.getStatus().isOnline()) {
          return consumer.apply(entity, pinNum == null ? null : entity.getService().getPin(pinNum));
        } else {
          log.error("Unable to execute step for firmata entity: <{}>. Waited for ready status but got: <{}>", entity.getTitle(), entity.getStatus());
        }
      }
    } else {
      if (entity.getStatus().isOnline()) {
        return consumer.apply(entity, pinNum == null ? null : entity.getService().getPin(pinNum));
      }
    }
    return null;
  }
}
