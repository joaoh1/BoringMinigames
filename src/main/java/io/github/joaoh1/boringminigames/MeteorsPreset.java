package io.github.joaoh1.boringminigames;


import com.github.vini2003.polyester.api.event.type.block.BlockStepEvent;
import com.github.vini2003.polyester.api.tracker.Tracker;
import com.github.vini2003.polyester.api.dimension.registry.DimensionRegistry;
import com.github.vini2003.polyester.api.entity.Player;
import com.github.vini2003.polyester.api.event.EventResult;
import com.github.vini2003.polyester.api.event.type.block.BlockLandEvent;
import com.github.vini2003.polyester.api.event.type.entity.EntityAddEvent;
import com.github.vini2003.polyester.api.event.type.entity.EntityRemoveEvent;
import com.github.vini2003.polyester.api.event.type.lobby.LobbyBindPlayerEvent;
import com.github.vini2003.polyester.api.event.type.lobby.LobbyUnbindPlayerEvent;
import com.github.vini2003.polyester.api.event.type.player.PlayerDamageEvent;
import com.github.vini2003.polyester.api.lobby.Lobby;
import com.github.vini2003.polyester.api.preset.Preset;
import com.github.vini2003.polyester.api.text.TextBuilder;
import com.github.vini2003.polyester.api.dimension.utilities.DimensionUtilities;
import com.github.vini2003.polyester.utility.WorldUtilities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MeteorsPreset extends Preset {
    public static final Identifier IDENTIFIER = new Identifier("boringminigames", "meteors");

    private static final Identifier METEORS_ARENA = new Identifier("boringminigames", "meteors_arena");

    private final Tracker<Integer> startTracker = new Tracker<Integer>(0) {
        @Override
        public void tick() {
            setValue(getValue() + 1);
        }
    };

    private final Predicate<Lobby> feedPredicate = (lobby -> true);

    private final Consumer<Lobby> feedAction = (lobby -> {
        lobby.getPlayers().forEach(player -> player.target().getHungerManager().setFoodLevel(40));
    });

    private int meteorsSpawned = 0;

    private final FallingBlockEntity createFallingMeteor(World world, BlockPos blockPos) {
    	meteorsSpawned++;
		if (meteorsSpawned < 40) {
			FallingBlockEntity fallingMeteor = new FallingBlockEntity(world, blockPos.getX() + 0.5, 128, blockPos.getZ() + 0.5, Blocks.MAGMA_BLOCK.getDefaultState());
			fallingMeteor.dropItem = false;
			fallingMeteor.timeFalling = 1;
			fallingMeteor.addVelocity(0, -1.0, 0);
			return fallingMeteor;
		} else {
			meteorsSpawned--;
			return new FallingBlockEntity(world, 15, -15, 15, Blocks.STONE.getDefaultState());
		}
	}

    private final Predicate<Lobby> startPredicate = (lobby -> {
        return lobby.getTrackers().get(startTracker).getValue().equals(200);
    });

    private final Consumer<Lobby> startAction = (lobby -> {
    	FallingBlockEntity fallingMeteor = createFallingMeteor(lobby.getWorld(), new BlockPos(15, 128, 15));
		lobby.getWorld().spawnEntity(fallingMeteor);
        lobby.getPlayers()
            .forEach(player -> {
				player.sendMessage(TextBuilder.builder().with("§cThe meteor has started to attack!").build());
				player.target().playSound(SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.75F, 1.0F);
			});
    });

    private final BlockStepEvent.Listener blockStepListener = ((world, entity, blockInformation) -> {
    	if (entity.getScoreboardTags().contains("targeted")) {
			BlockPos playerBlockPos = new BlockPos(entity.getBlockPos().getX(), 60, entity.getBlockPos().getZ());
			if (world.getBlockState(playerBlockPos).getBlock().equals(Blocks.STONE)) {
				world.spawnEntity(createFallingMeteor(world, playerBlockPos));
			} else if (world.getBlockState(playerBlockPos).getBlock().equals(Blocks.ANDESITE)) {
				world.spawnEntity(createFallingMeteor(world, playerBlockPos));
			} else if (world.getBlockState(playerBlockPos).getBlock().equals(Blocks.COBBLESTONE)) {
				world.spawnEntity(createFallingMeteor(world, playerBlockPos));
			}
			entity.removeScoreboardTag("targeted");
		}
    	return EventResult.CONTINUE;
	});

	private final BlockLandEvent.Listener preStartLandListener = ((world, entity, data, distance) -> {
		if (entity.getClass().equals(FallingBlockEntity.class)) {
			entity.teleport(100, 255, 100);
			world.setBlockState(data.getPosition().asBlockPosition().up(), Blocks.AIR.getDefaultState());
		}
		return EventResult.CONTINUE;
	});

	private final BlockLandEvent.Listener blockLandListener = ((world, entity, data, distance) -> {
		if (entity.collided) {
			if (entity.getClass().equals(FallingBlockEntity.class)) {
				entity.teleport(100, 255, 100);
				world.setBlockState(data.getPosition().asBlockPosition().up(), Blocks.AIR.getDefaultState());
				world.getPlayers().forEach(player -> {
					if (player.getBlockPos().isWithinDistance(data.getPosition(), 2)) {
						player.damage(DamageSource.OUT_OF_WORLD, 1.0F);
					}
				});
				if (data.getBlock().equals(Blocks.POLISHED_ANDESITE)) {
					BlockPos nextBlockPos = data.getPosition().asBlockPosition();
					world.setBlockState(nextBlockPos, Blocks.STONE.getDefaultState());
					if (world.getBlockState(nextBlockPos.north()).getBlock().equals(Blocks.POLISHED_ANDESITE)) {
						world.spawnEntity(createFallingMeteor(world, nextBlockPos.north()));
					}
					if (world.getBlockState(nextBlockPos.south()).getBlock().equals(Blocks.POLISHED_ANDESITE)) {
						world.spawnEntity(createFallingMeteor(world, nextBlockPos.south()));
					}
					if (world.getBlockState(nextBlockPos.west()).getBlock().equals(Blocks.POLISHED_ANDESITE)) {
						world.spawnEntity(createFallingMeteor(world, nextBlockPos.west()));
					}
					if (world.getBlockState(nextBlockPos.east()).getBlock().equals(Blocks.POLISHED_ANDESITE)) {
						world.spawnEntity(createFallingMeteor(world, nextBlockPos.east()));
					}
				} else if (data.getBlock().equals(Blocks.STONE)) {
					world.getPlayers().forEach(player -> {
						player.playSound(SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.5F, 1.0F);
					});
					world.setBlockState(data.getPosition().asBlockPosition(), Blocks.ANDESITE.getDefaultState());
				} else if (data.getBlock().equals(Blocks.ANDESITE)) {
					world.getPlayers().forEach(player -> {
						player.playSound(SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.5F, 0.75F);
					});
					world.setBlockState(data.getPosition().asBlockPosition(), Blocks.COBBLESTONE.getDefaultState());
				} else if (data.getBlock().equals(Blocks.COBBLESTONE)) {
					world.breakBlock(data.getPosition().asBlockPosition(), false);
				} else if (data.getBlock().equals(Blocks.MAGMA_BLOCK)) {
					world.breakBlock(data.getPosition().asBlockPosition(), false);
				}
				world.getPlayers().forEach(player -> {
					player.addScoreboardTag("targeted");
				});
				int timesExecuted = 0;
				for (int i = 0; i < 4; i++) {
					timesExecuted++;
					BlockPos randomBlockPos = new BlockPos(world.random.nextInt(30), 60, world.random.nextInt(30));
					if (world.getBlockState(randomBlockPos).getBlock().equals(Blocks.STONE)) {
						world.spawnEntity(createFallingMeteor(world, randomBlockPos));
					} else if (world.getBlockState(randomBlockPos).getBlock().equals(Blocks.ANDESITE)) {
						world.spawnEntity(createFallingMeteor(world, randomBlockPos));
					} else if (world.getBlockState(randomBlockPos).getBlock().equals(Blocks.COBBLESTONE)) {
						world.spawnEntity(createFallingMeteor(world, randomBlockPos));
					} else if (world.getBlockState(randomBlockPos).getBlock().equals(Blocks.POLISHED_ANDESITE)) {
						if (world.getBlockState(randomBlockPos.up()).getBlock().equals(Blocks.BARRIER)) {
							if (timesExecuted < 100) {
								i--;
							}
						} else if (world.getBlockState(randomBlockPos.up()).getBlock().equals(Blocks.AIR)) {
							world.spawnEntity(createFallingMeteor(world, randomBlockPos));
						}
					} else {
						if (timesExecuted < 100) {
							i--;
						}
					}
				}
			}
		}
		return EventResult.CONTINUE;
	});

	private final EntityAddEvent.Listener entityAddListener = ((entity) -> {
		if (entity.getClass().equals(BatEntity.class)) {
			entity.remove();
			return EventResult.CANCEL;
		}
		return EventResult.CONTINUE;
	});

	private final EntityRemoveEvent.Listener entityRemoveListener = ((entity) -> {
		if (entity.getClass().equals(FallingBlockEntity.class)) {
			if (((FallingBlockEntity) entity).getBlockState().equals(Blocks.MAGMA_BLOCK.getDefaultState())) {
				meteorsSpawned--;
			}
		}
		return EventResult.CONTINUE;
	});

	private static final LobbyBindPlayerEvent.Listener bindPlayerListener = ((lobby, player) -> {
        if (!player.getWorld().isClient()) {
            if (lobby.getPreset().getIdentifier().equals(IDENTIFIER)) {
                DimensionUtilities.teleport(player.target(), lobby.getDimension(), new BlockPos(15.5, 61, 15.5));
            }
        }

        return EventResult.CONTINUE;
    });

    private static final LobbyUnbindPlayerEvent.Listener unbindPlayerListener = ((lobby, player) -> {
        if (!player.getWorld().isClient()) {
			if (player.hasPreset() && lobby.getPreset().getIdentifier().equals(IDENTIFIER)) {
				DimensionUtilities.teleport(player.target(), DimensionType.OVERWORLD, new BlockPos(0, 64, 0));
			}
		}

		return EventResult.CONTINUE;
	});

	private static final PlayerDamageEvent.Listener damagePlayerListener = (player, source, amount) -> {
		if (source == DamageSource.OUT_OF_WORLD) {
			if (player.hasLobby() && player.getPresetIdentifier().equals(IDENTIFIER) && !player.getWorld().isClient()) {
				player.setGameMode(GameMode.SPECTATOR);

				ArrayList<Player> players = (ArrayList<Player>) player.getLobby().getPlayers();

				if (players.size() > 1) {
					player.target().setCameraEntity(players.get(player.getWorld().random.nextInt(players.size())).target());
				}

				Lobby lobby = player.getLobby();

				if (lobby.getPlayers().stream().noneMatch(member -> member.getGameMode() == GameMode.ADVENTURE)) {
					lobby.getPreset().restart(lobby);
					lobby.getPlayers().forEach(member -> {
						member.sendMessage(TextBuilder.builder()
								.with("§e" + player.getName() + "§f has survived the meteor shower!")
								.build());

						member.target().playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.5F);

						member.setGameMode(GameMode.ADVENTURE);
						
						member.target().teleport(15.5, 61, 15.5);
					});

					lobby.getPreset().restart(lobby);
				}
				return EventResult.CANCEL;
			}
		} else if (player.hasPreset() && player.getPresetIdentifier().equals(IDENTIFIER)) {
			return EventResult.CANCEL;
		}
		return EventResult.CONTINUE;
	};

	@Override
	public Identifier getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public void apply(Lobby lobby) {
		startTracker.setValue(0);
		meteorsSpawned = 0;

		if (!lobby.hasDimension()) {
			DimensionType superFlat = DimensionRegistry.INSTANCE.getByIdentifier(DimensionUtilities.getVoidDimension());

			lobby.bindDimension(superFlat);
			World world = WorldUtilities.getWorld(superFlat);
			world.setTimeOfDay(1000);
			lobby.bindWorld(WorldUtilities.getWorld(superFlat));
		}

		lobby.getWorld().getStructureManager().getStructure(METEORS_ARENA).place(lobby.getWorld(), new BlockPos(0, 60, 0), new StructurePlacementData());

		lobby.getTrackers().put(startTracker, startTracker);

		lobby.enqueueAction(startPredicate, startAction);

		lobby.enqueueAction(feedPredicate, feedAction);

		lobby.enqueueAction((state -> startTracker.getValue() == 200), (state -> {
			BlockLandEvent.unregister(preStartLandListener);
			BlockLandEvent.register(blockLandListener);
			BlockStepEvent.register(blockStepListener);
		}));

		BlockLandEvent.register(preStartLandListener);

		LobbyBindPlayerEvent.register(bindPlayerListener);

		LobbyUnbindPlayerEvent.register(unbindPlayerListener);

		PlayerDamageEvent.register(damagePlayerListener);

		EntityAddEvent.register(entityAddListener);

		EntityRemoveEvent.register(entityRemoveListener);
	}

	@Override
	public void retract(Lobby lobby) {
		lobby.unqueueAllActions();

		lobby.getTrackers().remove(startTracker);

		meteorsSpawned = 0;

		LobbyBindPlayerEvent.unregister(bindPlayerListener);

		LobbyUnbindPlayerEvent.unregister(unbindPlayerListener);

		BlockLandEvent.unregister(preStartLandListener);

		BlockLandEvent.unregister(blockLandListener);

		BlockStepEvent.unregister(blockStepListener);

		PlayerDamageEvent.unregister(damagePlayerListener);

		EntityAddEvent.unregister(entityAddListener);

		EntityRemoveEvent.unregister(entityRemoveListener);
	}

	@Override
	public void restart(Lobby lobby) {
		retract(lobby);
		apply(lobby);
	}
}