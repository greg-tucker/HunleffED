package com.hunlleffed;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Hunlleff Expected Damage",
	description = "Compares damage and kill time with performance and no-lost-ticks expected values"
)
public class HunlleffExpectedDamagePlugin extends Plugin
{
	private static final int GAUNTLET_LOBBY_REGION = 12127;
	private static final int DAMAGE_FLOOR_OBJECT = 36048;

	private static final Set<Integer> HUNLLEFF_IDS = Set.of(
		NpcID.CRYSTAL_HUNLLEF_MELEE,
		NpcID.CRYSTAL_HUNLLEF_RANGED,
		NpcID.CRYSTAL_HUNLLEF_MAGIC,
		NpcID.CRYSTAL_HUNLLEF_DEATH,
		NpcID.CRYSTAL_HUNLLEF_MELEE_HM,
		NpcID.CRYSTAL_HUNLLEF_RANGED_HM,
		NpcID.CRYSTAL_HUNLLEF_MAGIC_HM,
		NpcID.CRYSTAL_HUNLLEF_DEATH_HM);

	private static final Set<Integer> CORRUPTED_HUNLLEFF_IDS = Set.of(
		NpcID.CRYSTAL_HUNLLEF_MELEE_HM,
		NpcID.CRYSTAL_HUNLLEF_RANGED_HM,
		NpcID.CRYSTAL_HUNLLEF_MAGIC_HM,
		NpcID.CRYSTAL_HUNLLEF_DEATH_HM);

	private static final Set<Integer> TORNADO_IDS = Set.of(
		NpcID.CRYSTAL_HUNLLEF_CRYSTALS,
		NpcID.CRYSTAL_HUNLLEF_CRYSTALS_HM);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HunlleffExpectedDamageOverlay overlay;

	@Inject
	private HunlleffExpectedDamageConfig config;

	private final PerformanceStatistics statistics = new PerformanceStatistics();
	private final List<NPC> tornadoes = new ArrayList<>();

	private NPC hunlleff;
	private boolean bossActive;
	private boolean inLobby;
	private boolean showResults;
	private boolean hunlleffUsingMagic;
	private GauntletDifficulty difficulty = GauntletDifficulty.CRYSTALLINE;
	private int encounterEndTick = -1;

	@Override
	protected void startUp()
	{
		statistics.reset();
		overlayManager.add(overlay);
		clientThread.invokeLater(() ->
		{
			if (client.getVarbitValue(VarbitID.GAUNTLET_BOSS_STARTED) == 1)
			{
				startEncounter();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		resetRuntimeState();
		statistics.reset();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.PLAYER_IN_GAUNTLET)
		{
			if (event.getValue() == 1)
			{
				showResults = false;
			}
			return;
		}

		if (event.getVarbitId() != VarbitID.GAUNTLET_BOSS_STARTED)
		{
			return;
		}

		if (event.getValue() == 1 && !bossActive)
		{
			startEncounter();
		}
		else if (event.getValue() == 0 && bossActive)
		{
			endEncounter();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		inLobby = isInGauntletLobby();

		if (!bossActive)
		{
			return;
		}

		statistics.totalTicks++;
		updateHazardTracking();
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!bossActive)
		{
			return;
		}

		Actor actor = event.getActor();
		int animationId = actor.getAnimation();
		Player localPlayer = client.getLocalPlayer();
		if (actor == localPlayer && isPlayerAttackAnimation(animationId))
		{
			recordPlayerAttack(animationId);
			return;
		}

		if (!(actor instanceof NPC) || !isHunlleff((NPC) actor))
		{
			return;
		}

		hunlleff = (NPC) actor;
		switch (animationId)
		{
			case AnimationID.HUNLLEF_ATTACK_RANGED:
				recordHunlleffAttack();
				break;
			case AnimationID.HUNLLEF_ATTACK_MELEE:
				DamageEstimate stomp = CombatCalculator.stompAttack(difficulty);
				statistics.expectedDamageTaken += stomp.getExpectedDamage();
				recordLethalOpportunity(stomp);
				break;
			case AnimationID.HUNLLEF_ATTACK_TRANSITION_MAGIC:
				hunlleffUsingMagic = true;
				break;
			case AnimationID.HUNLLEF_ATTACK_TRANSITION_RANGED:
				hunlleffUsingMagic = false;
				break;
			case AnimationID.HUNLLEF_DEATH_PART_A:
			case AnimationID.HUNLLEF_DEATH_PART_B:
				statistics.hunlleffDefeated = true;
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!bossActive && client.getTickCount() != encounterEndTick)
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();
		if (!hitsplat.isMine() && !hitsplat.isOthers())
		{
			return;
		}

		Actor actor = event.getActor();
		if (actor == client.getLocalPlayer())
		{
			statistics.damageTaken += hitsplat.getAmount();
		}
		else if (actor instanceof NPC && isHunlleff((NPC) actor))
		{
			statistics.damageDealt += hitsplat.getAmount();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (TORNADO_IDS.contains(npc.getId()))
		{
			tornadoes.add(npc);
		}
		else if (HUNLLEFF_IDS.contains(npc.getId()))
		{
			hunlleff = npc;
			difficulty = difficultyFor(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (TORNADO_IDS.contains(npc.getId()))
		{
			tornadoes.remove(npc);
		}
		else if (npc == hunlleff)
		{
			hunlleff = null;
		}
	}

	private void startEncounter()
	{
		bossActive = true;
		showResults = false;
		encounterEndTick = -1;
		hunlleffUsingMagic = false;
		tornadoes.clear();
		statistics.reset();
		statistics.startingHitpoints =
			client.getBoostedSkillLevel(Skill.HITPOINTS);

		difficulty = client.getVarbitValue(VarbitID.GAUNTLET_CORRUPTED) == 1
			? GauntletDifficulty.CORRUPTED
			: GauntletDifficulty.CRYSTALLINE;
		findHunlleff();
	}

	private void endEncounter()
	{
		bossActive = false;
		encounterEndTick = client.getTickCount();
		statistics.endingHitpoints =
			client.getBoostedSkillLevel(Skill.HITPOINTS);
		showResults = statistics.totalTicks > 0;
		statistics.finish(difficulty);
		tornadoes.clear();
		hunlleff = null;
		if (config.sendChatSummary() && showResults)
		{
			sendChatSummary();
		}
	}

	private void resetRuntimeState()
	{
		bossActive = false;
		inLobby = false;
		showResults = false;
		encounterEndTick = -1;
		hunlleffUsingMagic = false;
		hunlleff = null;
		tornadoes.clear();
	}

	private void recordPlayerAttack(int animationId)
	{
		CombatStyle style = combatStyleForAnimation(animationId);
		boolean protectedByHunlleff = isProtectedByHunlleff(style);
		boolean hasHunlleffSnapshot = hunlleff != null;
		int elapsedTick = statistics.totalTicks;

		clientThread.invokeLater(() ->
		{
			if (!bossActive)
			{
				return;
			}

			EquipmentSnapshot equipment = EquipmentSnapshot.capture(client);
			PrayerModifiers prayers = PrayerModifiers.capture(client, style);
			if (!hasHunlleffSnapshot || !equipment.supports(style))
			{
				statistics.unmodelledPlayerAttacks++;
				return;
			}

			int stance = client.getVarpValue(VarPlayerID.COM_MODE);
			int attackSpeed = equipment.getAttackSpeed(stance);
			MeleeType meleeType = meleeType(animationId, equipment, stance);
			int[] stanceBonuses = offensiveStanceBonuses(style, animationId, equipment, stance);
			int accuracyLevel = style == CombatStyle.MELEE
				? client.getBoostedSkillLevel(Skill.ATTACK)
				: client.getBoostedSkillLevel(style == CombatStyle.RANGED ? Skill.RANGED : Skill.MAGIC);
			int damageLevel = style == CombatStyle.MELEE
				? client.getBoostedSkillLevel(Skill.STRENGTH)
				: accuracyLevel;

			DamageEstimate estimate = CombatCalculator.playerAttack(
				style,
				accuracyLevel,
				damageLevel,
				equipment.getAttackBonus(style, meleeType),
				equipment.getStrengthBonus(style),
				stanceBonuses[0],
				stanceBonuses[1],
				prayers.getAccuracy(),
				prayers.getStrength(),
				equipment.getMagicMaxHit(),
				prayers.getMagicDamagePermille(),
				protectedByHunlleff);

			statistics.addPlayerAttack(estimate, attackSpeed, elapsedTick);
		});
	}

	private void recordHunlleffAttack()
	{
		CombatStyle style = hunlleffUsingMagic ? CombatStyle.MAGIC : CombatStyle.RANGED;
		boolean correctPrayer = hasCorrectProtectionPrayer(style);

		EquipmentSnapshot equipment = EquipmentSnapshot.capture(client);
		PrayerModifiers prayers = PrayerModifiers.capture(client, style);
		int stance = client.getVarpValue(VarPlayerID.COM_MODE);
		int defensiveStanceBonus = defensiveStanceBonus(
			combatStyleForWeapon(equipment.getWeaponId()),
			stance);

		DamageEstimate estimate = CombatCalculator.hunlleffAttack(
			difficulty,
			style,
			client.getBoostedSkillLevel(Skill.DEFENCE),
			client.getBoostedSkillLevel(Skill.MAGIC),
			equipment.getDefenceBonus(style),
			defensiveStanceBonus,
			prayers.getDefence(),
			prayers.getMagicDefence(),
			correctPrayer,
			equipment.getArmourTierSum());

		DamageEstimate cleanMechanicsEstimate = CombatCalculator.hunlleffAttack(
			difficulty,
			style,
			client.getBoostedSkillLevel(Skill.DEFENCE),
			client.getBoostedSkillLevel(Skill.MAGIC),
			equipment.getDefenceBonus(style),
			defensiveStanceBonus,
			prayers.getDefence(),
			prayers.getMagicDefence(),
			true,
			equipment.getArmourTierSum());

		statistics.expectedDamageTaken += estimate.getExpectedDamage();
		statistics.cleanMechanicsExpectedDamageTaken +=
			cleanMechanicsEstimate.getExpectedDamage();
		recordLethalOpportunity(estimate);
	}

	private void recordLethalOpportunity(DamageEstimate estimate)
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			statistics.addLethalOpportunity(
				estimate,
				client.getBoostedSkillLevel(Skill.HITPOINTS));
		}
	}

	private void updateHazardTracking()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		WorldPoint playerLocation = player.getWorldLocation();
		EquipmentSnapshot equipment = EquipmentSnapshot.capture(client);
		for (NPC tornado : tornadoes)
		{
			if (playerLocation.equals(tornado.getWorldLocation()))
			{
				statistics.expectedDamageTaken +=
					difficulty.getTornadoExpectedDamage(equipment.getArmourTierSum());
			}
		}

		if (isStandingOnDamageFloor(player))
		{
			statistics.expectedDamageTaken += CombatCalculator.floorExpectedDamage();
		}
	}

	private boolean isStandingOnDamageFloor(Player player)
	{
		WorldView worldView = player.getWorldView();
		LocalPoint location = player.getLocalLocation();
		if (worldView == null || location == null)
		{
			return false;
		}

		Scene scene = worldView.getScene();
		int sceneX = location.getSceneX();
		int sceneY = location.getSceneY();
		Tile[][][] tiles = scene.getTiles();
		int plane = worldView.getPlane();
		if (plane < 0 || plane >= tiles.length
			|| sceneX < 0 || sceneX >= tiles[plane].length
			|| sceneY < 0 || sceneY >= tiles[plane][sceneX].length)
		{
			return false;
		}

		Tile tile = tiles[plane][sceneX][sceneY];
		if (tile == null)
		{
			return false;
		}

		GroundObject groundObject = tile.getGroundObject();
		return groundObject != null && groundObject.getId() == DAMAGE_FLOOR_OBJECT;
	}

	private boolean isProtectedByHunlleff(CombatStyle style)
	{
		if (hunlleff == null)
		{
			return false;
		}

		switch (hunlleff.getId())
		{
			case NpcID.CRYSTAL_HUNLLEF_MELEE:
			case NpcID.CRYSTAL_HUNLLEF_MELEE_HM:
				return style == CombatStyle.MELEE;
			case NpcID.CRYSTAL_HUNLLEF_RANGED:
			case NpcID.CRYSTAL_HUNLLEF_RANGED_HM:
				return style == CombatStyle.RANGED;
			case NpcID.CRYSTAL_HUNLLEF_MAGIC:
			case NpcID.CRYSTAL_HUNLLEF_MAGIC_HM:
				return style == CombatStyle.MAGIC;
			default:
				return false;
		}
	}

	private boolean hasCorrectProtectionPrayer(CombatStyle style)
	{
		return style == CombatStyle.MAGIC
			? isPrayerActive(net.runelite.api.Prayer.PROTECT_FROM_MAGIC)
			: isPrayerActive(net.runelite.api.Prayer.PROTECT_FROM_MISSILES);
	}

	private boolean isPrayerActive(net.runelite.api.Prayer prayer)
	{
		return client.getVarbitValue(prayer.getVarbit()) == 1;
	}

	private void findHunlleff()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldView() == null)
		{
			return;
		}

		for (NPC npc : player.getWorldView().npcs())
		{
			if (isHunlleff(npc))
			{
				hunlleff = npc;
				difficulty = difficultyFor(npc);
				return;
			}
		}
	}

	private boolean isInGauntletLobby()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}

		LocalPoint localLocation = player.getLocalLocation();
		if (localLocation == null)
		{
			return false;
		}

		WorldPoint point = WorldPoint.fromLocalInstance(client, localLocation);
		return point != null && point.getRegionID() == GAUNTLET_LOBBY_REGION;
	}

	private void sendChatSummary()
	{
		String message = String.format(
			Locale.ROOT,
			"Hunlleff analysis - dealt %d / %.1f expected; taken %d / %.1f expected; "
				+ "kill %s actual / %s performance EV / %s no-loss EV; "
				+ "lethal opportunities %d (%.1f%% death chance).",
			statistics.damageDealt,
			statistics.expectedDamageDealt,
			statistics.damageTaken,
			statistics.expectedDamageTaken,
			statistics.hunlleffDefeated
				? formatTime(statistics.getActualKillTicks())
				: "DNF",
			Double.isFinite(statistics.performanceExpectedKillTicks)
				? formatTime(statistics.performanceExpectedKillTicks)
				: "n/a",
			Double.isFinite(statistics.noLossExpectedKillTicks)
				? formatTime(statistics.noLossExpectedKillTicks)
				: "n/a",
			statistics.lethalOpportunities,
			statistics.getLethalChance() * 100.0);

		if (config.showFishAnalysis())
		{
			message += String.format(
				Locale.ROOT,
				" Food check (20 HP each, HP %d to %d): %.1f needed this fight / "
					+ "%.1f expected with clean play / %.1f healing required by "
					+ "mistakes (EV).",
				statistics.startingHitpoints,
				statistics.endingHitpoints,
				statistics.getActualFishEquivalent(),
				statistics.getCleanMechanicsFishEquivalent(),
				statistics.getMistakeHealingFishEquivalent());
		}

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private static String formatTime(double ticks)
	{
		double totalSeconds = ticks * 0.6;
		int minutes = (int) (totalSeconds / 60.0);
		double seconds = totalSeconds - minutes * 60.0;
		return String.format(Locale.ROOT, "%d:%04.1f", minutes, seconds);
	}

	private static boolean isPlayerAttackAnimation(int animationId)
	{
		return animationId == AnimationID.HUMAN_CASTWAVE_STAFF
			|| animationId == AnimationID.HUMAN_BOW
			|| animationId == AnimationID.HUMAN_SPEAR_SPIKE
			|| animationId == AnimationID.HUMAN_SCYTHE_SWEEP
			|| animationId == AnimationID.HUMAN_BLUNT_POUND
			|| animationId == AnimationID.HUMAN_UNARMEDPUNCH
			|| animationId == AnimationID.HUMAN_UNARMEDKICK;
	}

	private static CombatStyle combatStyleForAnimation(int animationId)
	{
		if (animationId == AnimationID.HUMAN_CASTWAVE_STAFF)
		{
			return CombatStyle.MAGIC;
		}
		if (animationId == AnimationID.HUMAN_BOW)
		{
			return CombatStyle.RANGED;
		}
		return CombatStyle.MELEE;
	}

	private static boolean isUnarmedAnimation(int animationId)
	{
		return animationId == AnimationID.HUMAN_UNARMEDPUNCH
			|| animationId == AnimationID.HUMAN_UNARMEDKICK;
	}

	private static MeleeType meleeType(int animationId, EquipmentSnapshot equipment, int stance)
	{
		if (isUnarmedAnimation(animationId) || equipment.isSceptre())
		{
			return MeleeType.CRUSH;
		}
		return stance == 1 ? MeleeType.SLASH : MeleeType.STAB;
	}

	private static int[] offensiveStanceBonuses(
		CombatStyle style,
		int animationId,
		EquipmentSnapshot equipment,
		int stance)
	{
		if (style == CombatStyle.RANGED)
		{
			int bonus = stance == 0 ? 3 : 0;
			return new int[]{bonus, bonus};
		}
		if (style == CombatStyle.MAGIC)
		{
			return new int[]{stance == 0 ? 2 : 0, 0};
		}
		if (equipment.isHalberd())
		{
			return new int[]{
				stance == 0 ? 1 : 0,
				stance == 0 ? 1 : (stance == 1 ? 3 : 0)
			};
		}

		// Unarmed and sceptre styles are Accurate, Aggressive, and Defensive.
		return new int[]{
			stance == 0 || animationId == AnimationID.HUMAN_UNARMEDPUNCH ? 3 : 0,
			stance == 1 || animationId == AnimationID.HUMAN_UNARMEDKICK ? 3 : 0
		};
	}

	private static CombatStyle combatStyleForWeapon(int weaponId)
	{
		switch (weaponId)
		{
			case ItemID.GAUNTLET_RANGED_T1:
			case ItemID.GAUNTLET_RANGED_T2:
			case ItemID.GAUNTLET_RANGED_T3:
			case ItemID.GAUNTLET_RANGED_T1_HM:
			case ItemID.GAUNTLET_RANGED_T2_HM:
			case ItemID.GAUNTLET_RANGED_T3_HM:
				return CombatStyle.RANGED;
			case ItemID.GAUNTLET_MAGIC_T1:
			case ItemID.GAUNTLET_MAGIC_T2:
			case ItemID.GAUNTLET_MAGIC_T3:
			case ItemID.GAUNTLET_MAGIC_T1_HM:
			case ItemID.GAUNTLET_MAGIC_T2_HM:
			case ItemID.GAUNTLET_MAGIC_T3_HM:
				return CombatStyle.MAGIC;
			default:
				return CombatStyle.MELEE;
		}
	}

	private static int defensiveStanceBonus(CombatStyle style, int stance)
	{
		if (style == CombatStyle.MELEE)
		{
			return stance == 0 ? 1 : (stance == 3 ? 3 : 0);
		}
		if (style == CombatStyle.RANGED)
		{
			return stance == 3 ? 3 : 0;
		}
		return stance == 1 || stance == 3 ? 3 : 0;
	}

	private static boolean isHunlleff(NPC npc)
	{
		return HUNLLEFF_IDS.contains(npc.getId());
	}

	private static GauntletDifficulty difficultyFor(NPC npc)
	{
		return CORRUPTED_HUNLLEFF_IDS.contains(npc.getId())
			? GauntletDifficulty.CORRUPTED
			: GauntletDifficulty.CRYSTALLINE;
	}

	PerformanceStatistics getStatistics()
	{
		return statistics;
	}

	boolean isBossActive()
	{
		return bossActive;
	}

	boolean shouldShowResults()
	{
		return showResults && inLobby;
	}

	@Provides
	HunlleffExpectedDamageConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HunlleffExpectedDamageConfig.class);
	}

}
