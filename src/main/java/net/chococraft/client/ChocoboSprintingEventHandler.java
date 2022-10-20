package net.chococraft.client;

import net.chococraft.Chococraft;
import net.chococraft.network.PacketManager;
import net.chococraft.network.packets.ChocoboSprintingMessage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Chococraft.MODID, value = Dist.CLIENT)
public class ChocoboSprintingEventHandler {
	private static boolean isSprinting = false;

	@SubscribeEvent
	public static void onKeyPress(KeyInputEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && minecraft.player.getVehicle() != null) {
			KeyMapping keyBinding = minecraft.options.keySprint;
			if (keyBinding.consumeClick()) {
				if (!isSprinting) {
					isSprinting = true;
					PacketManager.CHANNEL.sendToServer(new ChocoboSprintingMessage(true));
				}
			} else {
				if (isSprinting) {
					isSprinting = false;
					PacketManager.CHANNEL.sendToServer(new ChocoboSprintingMessage(false));
				}
			}
		} else {
			isSprinting = false;
		}
	}
}
