package com.hunlleffed;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
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
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
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
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Hunlleff Expected Damage",
	description = "Tracks Gauntlet performance and actual versus expected damage"
)
public class HunlleffExpectedDamagePlugin extends Plugin
{
	private static final int GAUNTLET_LOBBY_REGION = 12127;
	private static final int DAMAGE_FLOOR_OBJECT = 36048;
	private static final int STANDARD_WEAPON_SPEED = 4;
	private static final int FOOD_DELAY = 3;
	private static final int COMBO_FOOD_DELAY = 2;

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
	private final Map<Integer, PendingEat> pendingEats = new HashMap<>();

	private NPC hunlleff;
	private boolean bossActive;
	private boolean inGauntlet;
	private boolean inLobby;
	private boolean showResults;
	private boolean hunlleffUsingMagic;
	private GauntletDifficulty difficulty = GauntletDifficulty.CRYSTALLINE;
	private TickLossState tickLossState = TickLossState.NONE;
	private int previousAttackTick;
	private int previousWeaponSpeed = STANDARD_WEAPON_SPEED;
	private int encounterEndTick = -1;

	@Override
	protected void startUp()
	{
		statistics.reset();
		overlayManager.add(overlay);
		clientThread.invokeLater(() ->
		{
			inGauntlet = client.getVarbitValue(VarbitID.PLAYER_IN_GAUNTLET) == 1;
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
			inGauntlet = event.getValue() == 1;
			if (inGauntlet)
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
		updateTickLossState();
		updateHazardTracking();
		pendingEats.entrySet().removeIf(entry ->
			client.getTickCount() - entry.getValue().tick > 3);
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
				statistics.hunlleffStomps++;
				statistics.expectedDamageTaken += CombatCalculator.stompExpectedDamage(difficulty);
				break;
			case AnimationID.HUNLLEF_ATTACK_TRANSITION_MAGIC:
				hunlleffUsingMagic = true;
				break;
			case AnimationID.HUNLLEF_ATTACK_TRANSITION_RANGED:
				hunlleffUsingMagic = false;
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

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!bossActive)
		{
			return;
		}

		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		if (!option.startsWith("eat"))
		{
			return;
		}

		int itemId = event.getItemId();
		if (!isTrackedFood(itemId))
		{
			return;
		}

		int slot = event.getMenuEntry().getParam0();
		pendingEats.put(slot, new PendingEat(itemId, client.getTickCount()));
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!bossActive || event.getItemContainer().getId() != InventoryID.INV)
		{
			return;
		}

		Item[] items = event.getItemContainer().getItems();
		Set<Integer> completedSlots = new HashSet<>();
		pendingEats.forEach((slot, pending) ->
		{
			if (slot < 0 || slot >= items.length || items[slot].getId() != pending.itemId)
			{
				previousAttackTick += foodDelay(pending.itemId);
				completedSlots.add(slot);
			}
		});
		completedSlots.forEach(pendingEats::remove);
	}

	private void startEncounter()
	{
		bossActive = true;
		inGauntlet = true;
		showResults = false;
		encounterEndTick = -1;
		hunlleffUsingMagic = false;
		tornadoes.clear();
		pendingEats.clear();
		statistics.reset();
		previousAttackTick = client.getTickCount();
		previousWeaponSpeed = STANDARD_WEAPON_SPEED;
		tickLossState = TickLossState.NONE;

		difficulty = client.getVarbitValue(VarbitID.GAUNTLET_CORRUPTED) == 1
			? GauntletDifficulty.CORRUPTED
			: GauntletDifficulty.CRYSTALLINE;
		findHunlleff();
	}

	private void endEncounter()
	{
		bossActive = false;
		encounterEndTick = client.getTickCount();
		inGauntlet = false;
		showResults = statistics.totalTicks > 0;
		tornadoes.clear();
		pendingEats.clear();
		hunlleff = null;
		if (config.sendChatSummary() && showResults)
		{
			sendChatSummary();
		}
	}

	private void resetRuntimeState()
	{
		bossActive = false;
		inGauntlet = false;
		inLobby = false;
		showResults = false;
		encounterEndTick = -1;
		hunlleffUsingMagic = false;
		hunlleff = null;
		tornadoes.clear();
		pendingEats.clear();
	}

	private void recordPlayerAttack(int animationId)
	{
		CombatStyle style = combatStyleForAnimation(animationId);
		boolean protectedByHunlleff = isProtectedByHunlleff(style);
		boolean hasHunlleffSnapshot = hunlleff != null;
		int attackTick = client.getTickCount();

		clientThread.invokeLater(() ->
		{
			if (!bossActive)
			{
				return;
			}

			statistics.playerAttacks++;
			if (protectedByHunlleff)
			{
				statistics.wrongAttackStyle++;
			}

			EquipmentSnapshot equipment = EquipmentSnapshot.capture(client);
			PrayerModifiers prayers = PrayerModifiers.capture(client, style);
			boolean unarmedLike = isUnarmedAnimation(animationId) || equipment.isSceptre();
			if (!prayers.hasOffensivePrayer()
				&& (config.countUnarmedOffPrayer() || !unarmedLike))
			{
				statistics.wrongOffensivePrayer++;
			}

			int ticksBetweenAttacks = attackTick - previousAttackTick;
			statistics.missedTicks += Math.max(0, ticksBetweenAttacks - previousWeaponSpeed);
			previousAttackTick = attackTick;
			if (!hasHunlleffSnapshot || !equipment.supports(style))
			{
				previousWeaponSpeed = equipment.getAttackSpeed(
					client.getVarpValue(VarPlayerID.COM_MODE));
				statistics.unmodelledPlayerAttacks++;
				return;
			}

			int stance = client.getVarpValue(VarPlayerID.COM_MODE);
			previousWeaponSpeed = equipment.getAttackSpeed(stance);
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

			statistics.expectedDamageDealt += estimate.getExpectedDamage();
			statistics.modelledPlayerAttacks++;
		});
	}

	private void recordHunlleffAttack()
	{
		statistics.hunlleffAttacks++;
		CombatStyle style = hunlleffUsingMagic ? CombatStyle.MAGIC : CombatStyle.RANGED;
		boolean correctPrayer = hasCorrectProtectionPrayer(style);
		if (!correctPrayer)
		{
			statistics.wrongDefensivePrayer++;
		}

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

		statistics.expectedDamageTaken += estimate.getExpectedDamage();
		statistics.modelledHunlleffAttacks++;
	}

	private void updateTickLossState()
	{
		int ticksSinceAttack = client.getTickCount() - previousAttackTick;
		if (ticksSinceAttack >= previousWeaponSpeed + FOOD_DELAY)
		{
			tickLossState = TickLossState.LOSING;
		}
		else if (ticksSinceAttack >= previousWeaponSpeed)
		{
			tickLossState = TickLossState.POTENTIAL;
		}
		else
		{
			tickLossState = TickLossState.NONE;
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
				statistics.tornadoHits++;
				statistics.expectedDamageTaken +=
					difficulty.getTornadoExpectedDamage(equipment.getArmourTierSum());
			}
		}

		if (isStandingOnDamageFloor(player))
		{
			statistics.floorHits++;
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
			"Hunlleff damage - dealt: %d / %.1f expected (%+.1f); taken: %d / %.1f expected (%+.1f).",
			statistics.damageDealt,
			statistics.expectedDamageDealt,
			statistics.getDamageDealtDifference(),
			statistics.damageTaken,
			statistics.expectedDamageTaken,
			statistics.getDamageTakenDifference());
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
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

	private static boolean isTrackedFood(int itemId)
	{
		return itemId == ItemID.GAUNTLET_FOOD
			|| itemId == ItemID.GAUNTLET_COMBO_FOOD
			|| itemId == ItemID.GAUNTLET_COMBO_FOOD_HM;
	}

	private static int foodDelay(int itemId)
	{
		return itemId == ItemID.GAUNTLET_FOOD ? FOOD_DELAY : COMBO_FOOD_DELAY;
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

	TickLossState getTickLossState()
	{
		return tickLossState;
	}

	@Provides
	HunlleffExpectedDamageConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HunlleffExpectedDamageConfig.class);
	}

	private static final class PendingEat
	{
		private final int itemId;
		private final int tick;

		private PendingEat(int itemId, int tick)
		{
			this.itemId = itemId;
			this.tick = tick;
		}
	}
}
