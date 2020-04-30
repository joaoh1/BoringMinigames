package io.github.joaoh1.boringminigames;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.vini2003.polyester.api.event.EventResult;
import com.github.vini2003.polyester.api.event.type.player.PlayerConnectEvent;
import com.github.vini2003.polyester.api.lobby.Lobby;
import com.github.vini2003.polyester.api.manager.LobbyManager;
import com.github.vini2003.polyester.api.preset.registry.PresetRegistry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class BoringMinigamesMod implements ModInitializer {
	@Override
	public void onInitialize() {
		PresetRegistry.INSTANCE.register(new Identifier("boringminigames", "preset"), new MeteorsPreset());
		Lobby mainLobby = new Lobby(new Identifier("boringminigames", "lobby"));

		Predicate<Lobby> bootPredicate = (lobby -> true);
		Consumer<Lobby> bootAction = (lobby -> {
			lobby.bindPreset(new MeteorsPreset());
			lobby.getPreset().apply(lobby);

			lobby.unqueueAction(bootPredicate);
		});

		mainLobby.enqueueAction(bootPredicate, bootAction);

		LobbyManager.INSTANCE.add(mainLobby);

		PlayerConnectEvent.register(player -> {
			player.setGameMode(GameMode.ADVENTURE);
			return EventResult.CONTINUE;
		});

		//ExampleDisplay.initialize();
	}
}
