package turbotechmods.creativeutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreativeUtils implements ModInitializer {
	public static final String MOD_ID = "creativeutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private final Map<UUID, GameMode> playerGameModeMap = new HashMap<>();

	@Override
	public void onInitialize() {
		LOGGER.info("CreativeUtils initialized!");
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			for (ServerPlayerEntity player : world.getPlayers()) {
				GameMode previousGameMode = playerGameModeMap.get(player.getUuid());
				GameMode currentGameMode = player.interactionManager.getGameMode();

				if (previousGameMode == GameMode.CREATIVE && currentGameMode != GameMode.CREATIVE) {
					disableOnePunch(player);
				}

				playerGameModeMap.put(player.getUuid(), currentGameMode);

				if (currentGameMode == GameMode.CREATIVE) {
					restrictStackSizes(player);
				}
			}
		});
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("CreativeUtils")
				.then(CommandManager.literal("FlightSpeedSet")
						.then(CommandManager.argument("speed", FloatArgumentType.floatArg(0.0f, 10.0f))
								.executes(context -> setFlightSpeed(context, FloatArgumentType.getFloat(context, "speed"))))
				)
				.then(CommandManager.literal("FlightSpeedReset")
						.executes(this::resetFlightSpeed)
				)
				.then(CommandManager.literal("SetupNewWorld")
						.executes(this::setupNewWorld)
				)
				.then(CommandManager.literal("SetAir")
						.executes(this::setAirWithSuggestion)
				)
				.then(CommandManager.literal("Onepunch")
						.then(CommandManager.literal("On")
								.executes(this::onePunchOn)
						)
						.then(CommandManager.literal("Off")
								.executes(this::onePunchOff)
						)
				));

		dispatcher.register(CommandManager.literal("air")
				.executes(this::setAir));
	}

	private void restrictStackSizes(PlayerEntity player) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty() && stack.getCount() > 1) {
				stack.setCount(1);
			}
		}
	}

	private int setupNewWorld(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
		ServerCommandSource source = serverCommandSourceCommandContext.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (isCreative(player)) {
			source.getServer().getCommandManager().executeWithPrefix(source, "difficulty peaceful");
			source.getServer().getCommandManager().executeWithPrefix(source, "gamerule doDaylightCycle false");
			source.getServer().getCommandManager().executeWithPrefix(source, "gamerule doWeatherCycle false");
			source.getServer().getCommandManager().executeWithPrefix(source, "gamerule doFireTick false");
			source.getServer().getCommandManager().executeWithPrefix(source, "time set day");
			source.getServer().getCommandManager().executeWithPrefix(source, "gamerule doMobSpawning false");
			return 1;
		} else {
			source.sendFeedback(() -> Text.literal("This command only works in Creative mode!"), false);
			return 0;
		}
	}

	private int setFlightSpeed(CommandContext<ServerCommandSource> context, float speedMultiplier) {
		ServerPlayerEntity player = context.getSource().getPlayer();

		if (isCreative(player)) {
			player.getAbilities().setFlySpeed(speedMultiplier / 20.0f);
			player.sendAbilitiesUpdate();
			context.getSource().sendFeedback(() -> Text.literal("Flight speed set to " + speedMultiplier + "x"), false);
			return 1;
		} else {
			context.getSource().sendFeedback(() -> Text.literal("This command only works in Creative mode!"), false);
			return 0;
		}
	}

	private int resetFlightSpeed(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();

		if (isCreative(player)) {
			player.getAbilities().setFlySpeed(0.05f);
			player.sendAbilitiesUpdate();
			context.getSource().sendFeedback(() -> Text.literal("Flight speed reset to default"), false);
			return 1;
		} else {
			context.getSource().sendFeedback(() -> Text.literal("This command only works in Creative mode!"), false);
			return 0;
		}
	}

	private int onePunchOn(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
		ServerCommandSource source = serverCommandSourceCommandContext.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (isCreative(player)) {
			EntityAttributeInstance attackDamageAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			if (attackDamageAttribute != null) {
				attackDamageAttribute.setBaseValue(1000.0);
				player.sendMessage(Text.literal("Onepunch has been enabled"), false);
			}
			return 1;
		} else {
			source.sendFeedback(() -> Text.literal("This command only works in Creative mode!"), false);
			return 0;
		}
	}

	private int onePunchOff(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
		ServerCommandSource source = serverCommandSourceCommandContext.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (isCreative(player)) {
			EntityAttributeInstance attackDamageAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			if (attackDamageAttribute != null) {
				attackDamageAttribute.setBaseValue(1.0);
				player.sendMessage(Text.literal("Onepunch has been disabled"), false);
			}
			return 1;
		} else {
			source.sendFeedback(() -> Text.literal("This command only works in Creative mode!"), false);
			return 0;
		}
	}

	private void disableOnePunch(ServerPlayerEntity player) {
		EntityAttributeInstance attackDamageAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
		if (attackDamageAttribute != null) {
			attackDamageAttribute.setBaseValue(1.0);
			player.sendMessage(Text.literal("Onepunch has been disabled due to game mode change"), false);
		}
	}

	private int setAir(CommandContext<ServerCommandSource> clientCommandSourceCommandContext) {
		ServerCommandSource source = clientCommandSourceCommandContext.getSource();
		source.getServer().getCommandManager().executeWithPrefix(source, "//replace #region air");
		return 1;
	}

	private int setAirWithSuggestion(CommandContext<ServerCommandSource> clientCommandSourceCommandContext) {
		ServerCommandSource source = clientCommandSourceCommandContext.getSource();
		source.getServer().getCommandManager().executeWithPrefix(source, "//replace #region air");
		source.sendFeedback(() -> Text.literal("Tip: You can use /air instead. Why? Convenience."), false);
		return 1;
	}

	private boolean isCreative(ServerPlayerEntity player) {
		return player != null && player.isCreative();
	}
}
