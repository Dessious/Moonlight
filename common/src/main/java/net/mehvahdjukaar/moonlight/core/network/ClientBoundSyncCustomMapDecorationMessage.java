package net.mehvahdjukaar.moonlight.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mehvahdjukaar.moonlight.api.map.*;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.moonlight.core.Moonlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Map;


public class ClientBoundSyncCustomMapDecorationMessage implements Message {
    private final int mapId;

    private final CustomMapDecoration[] customDecorations;
    private final CustomMapData[] customData;

    public ClientBoundSyncCustomMapDecorationMessage(int mapId,
            CustomMapDecoration[] customDecorations, CustomMapData[] customData) {
        this.mapId = mapId;

        this.customData = customData;
        this.customDecorations = customDecorations;
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.mapId);

        buffer.writeVarInt(this.customDecorations.length);

        for (CustomMapDecoration decoration : this.customDecorations) {
            buffer.writeResourceLocation(Utils.getID(decoration.getType()));
            decoration.saveToBuffer(buffer);
        }

        buffer.writeVarInt(this.customData.length);

        for (CustomMapData data : this.customData) {
            buffer.writeResourceLocation(data.getType().id());
            data.saveToBuffer(buffer);
        }
    }

    //receivers
    public ClientBoundSyncCustomMapDecorationMessage(FriendlyByteBuf pBuffer) {
        this.mapId = pBuffer.readVarInt();

        this.customDecorations = new CustomMapDecoration[pBuffer.readVarInt()];

        for (int m = 0; m < this.customDecorations.length; ++m) {
            MapDecorationType<?, ?> type = MapDecorationRegistry.get(pBuffer.readResourceLocation());
            if (type != null) {
                this.customDecorations[m] = type.loadDecorationFromBuffer(pBuffer);
            }
        }
        //TODO: I really could have merged the 2 systems
        this.customData = new CustomMapData[pBuffer.readVarInt()];
        for (int m = 0; m < this.customData.length; ++m) {
            CustomMapData.Type<?> type = MapDecorationRegistry.CUSTOM_MAP_DATA_TYPES.getOrDefault(pBuffer.readResourceLocation(), null);
            if (type != null) {
                this.customData[m] = type.createFromBuffer(pBuffer);
            }
        }
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (context.getDirection() == NetworkDir.PLAY_TO_CLIENT) {

            MapRenderer mapRenderer = Minecraft.getInstance().gameRenderer.getMapRenderer();

            int i = this.getMapId();
            String s = MapItem.makeKey(i);
            MapItemSavedData mapData = Minecraft.getInstance().level.getMapData(s);

            if (mapData != null) {
                //mapData = MapItemSavedData.createForClient(message.scale, message.locked, Minecraft.getInstance().level.dimension());
                //Minecraft.getInstance().level.setMapData(s, mapData);
                this.applyToMap(mapData);
                mapRenderer.update(i, mapData);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public int getMapId() {
        return this.mapId;
    }

    @Environment(EnvType.CLIENT)
    public void applyToMap(MapItemSavedData data) {
        if (data instanceof ExpandedMapData mapData) {
            Map<String, CustomMapDecoration> decorations = mapData.getCustomDecorations();
            decorations.clear();
            for (int i = 0; i < this.customDecorations.length; ++i) {
                CustomMapDecoration customDecoration = this.customDecorations[i];
                if (customDecoration != null) decorations.put("icon-" + i, customDecoration);
                else {
                    Moonlight.LOGGER.warn("Failed to load custom map decoration, skipping");
                }
            }
            Map<ResourceLocation,CustomMapData> customData = mapData.getCustomData();
            customData.clear();
            for (CustomMapData instance : this.customData) {
                if (instance != null) customData.put(instance.getType().id(), instance);
                else {
                    Moonlight.LOGGER.warn("Failed to load custom map data, skipping");
                }
            }

        }
    }
}