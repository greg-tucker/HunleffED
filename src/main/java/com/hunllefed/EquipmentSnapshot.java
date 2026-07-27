package com.hunllefed;

import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;

final class EquipmentSnapshot
{
	private static final int NO_WEAPON = -1;

	private int stabAttack;
	private int slashAttack;
	private int crushAttack;
	private int magicAttack;
	private int rangedAttack;
	private int meleeStrength;
	private int rangedStrength;
	private int magicDefence;
	private int rangedDefence;
	private int armourTierSum;
	private int weaponId = NO_WEAPON;

	static EquipmentSnapshot capture(Client client)
	{
		EquipmentSnapshot snapshot = new EquipmentSnapshot();
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment == null)
		{
			return snapshot;
		}

		Item[] items = equipment.getItems();
		int weaponSlot = EquipmentInventorySlot.WEAPON.getSlotIdx();
		for (int slot = 0; slot < items.length; slot++)
		{
			Item item = items[slot];
			if (item == null || item.getId() <= 0)
			{
				continue;
			}

			if (slot == weaponSlot)
			{
				snapshot.weaponId = item.getId();
			}

			snapshot.add(statsFor(item.getId()));
		}
		return snapshot;
	}

	static EquipmentSnapshot fromItemIds(int weaponId, int... itemIds)
	{
		EquipmentSnapshot snapshot = new EquipmentSnapshot();
		snapshot.weaponId = weaponId;
		for (int itemId : itemIds)
		{
			snapshot.add(statsFor(itemId));
		}
		return snapshot;
	}

	int getAttackBonus(CombatStyle style, MeleeType meleeType)
	{
		switch (style)
		{
			case MELEE:
				switch (meleeType)
				{
					case STAB:
						return stabAttack;
					case SLASH:
						return slashAttack;
					case CRUSH:
					default:
						return crushAttack;
				}
			case RANGED:
				return rangedAttack;
			case MAGIC:
				return magicAttack;
			default:
				return 0;
		}
	}

	int getStrengthBonus(CombatStyle style)
	{
		return style == CombatStyle.RANGED ? rangedStrength : meleeStrength;
	}

	int getDefenceBonus(CombatStyle style)
	{
		return style == CombatStyle.MAGIC ? magicDefence : rangedDefence;
	}

	int getArmourTierSum()
	{
		return armourTierSum;
	}

	int getWeaponId()
	{
		return weaponId;
	}

	int getMagicMaxHit()
	{
		switch (weaponId)
		{
			case ItemID.GAUNTLET_MAGIC_T1:
			case ItemID.GAUNTLET_MAGIC_T1_HM:
				return 23;
			case ItemID.GAUNTLET_MAGIC_T2:
			case ItemID.GAUNTLET_MAGIC_T2_HM:
				return 31;
			case ItemID.GAUNTLET_MAGIC_T3:
			case ItemID.GAUNTLET_MAGIC_T3_HM:
				return 39;
			default:
				return 0;
		}
	}

	int getAttackSpeed(int stance)
	{
		if (isRangedWeapon(weaponId))
		{
			// Gauntlet bows have a base speed of 5; Rapid (style index 1) is 4.
			return stance == 1 ? 4 : 5;
		}
		return isSceptre() ? 5 : 4;
	}

	boolean supports(CombatStyle style)
	{
		switch (style)
		{
			case MAGIC:
				return getMagicMaxHit() > 0;
			case RANGED:
				return isRangedWeapon(weaponId);
			case MELEE:
				return weaponId == NO_WEAPON
					|| weaponId == ItemID.GAUNTLET_SCEPTRE
					|| isMeleeWeapon(weaponId);
			default:
				return false;
		}
	}

	boolean isSceptre()
	{
		return weaponId == ItemID.GAUNTLET_SCEPTRE;
	}

	boolean isHalberd()
	{
		return isMeleeWeapon(weaponId);
	}

	private void add(ItemCombatStats stats)
	{
		stabAttack += stats.stabAttack;
		slashAttack += stats.slashAttack;
		crushAttack += stats.crushAttack;
		magicAttack += stats.magicAttack;
		rangedAttack += stats.rangedAttack;
		meleeStrength += stats.meleeStrength;
		rangedStrength += stats.rangedStrength;
		magicDefence += stats.magicDefence;
		rangedDefence += stats.rangedDefence;
		armourTierSum += stats.armourTier;
	}

	private static boolean isMeleeWeapon(int itemId)
	{
		switch (itemId)
		{
			case ItemID.GAUNTLET_MELEE_T1:
			case ItemID.GAUNTLET_MELEE_T2:
			case ItemID.GAUNTLET_MELEE_T3:
			case ItemID.GAUNTLET_MELEE_T1_HM:
			case ItemID.GAUNTLET_MELEE_T2_HM:
			case ItemID.GAUNTLET_MELEE_T3_HM:
				return true;
			default:
				return false;
		}
	}

	private static boolean isRangedWeapon(int itemId)
	{
		switch (itemId)
		{
			case ItemID.GAUNTLET_RANGED_T1:
			case ItemID.GAUNTLET_RANGED_T2:
			case ItemID.GAUNTLET_RANGED_T3:
			case ItemID.GAUNTLET_RANGED_T1_HM:
			case ItemID.GAUNTLET_RANGED_T2_HM:
			case ItemID.GAUNTLET_RANGED_T3_HM:
				return true;
			default:
				return false;
		}
	}

	private static ItemCombatStats statsFor(int itemId)
	{
		switch (itemId)
		{
			// Basic helms
			case ItemID.GAUNTLET_HELMET_T1:
			case ItemID.GAUNTLET_HELMET_T1_HM:
				return stats(2, 2, 1, 2, 2, 0, 0, 28, 1);
			// Attuned helms
			case ItemID.GAUNTLET_HELMET_T2:
			case ItemID.GAUNTLET_HELMET_T2_HM:
				return stats(6, 6, 2, 6, 6, 0, 0, 48, 2);
			// Perfected helms
			case ItemID.GAUNTLET_HELMET_T3:
			case ItemID.GAUNTLET_HELMET_T3_HM:
				return stats(10, 10, 3, 10, 10, 0, 0, 68, 3);

			// Basic bodies
			case ItemID.GAUNTLET_CHESTPLATE_T1:
			case ItemID.GAUNTLET_CHESTPLATE_T1_HM:
				return stats(8, 8, 3, 8, 8, 0, 0, 86, 1);
			// Attuned bodies
			case ItemID.GAUNTLET_CHESTPLATE_T2:
			case ItemID.GAUNTLET_CHESTPLATE_T2_HM:
				return stats(12, 12, 4, 12, 12, 0, 0, 102, 2);
			// Perfected bodies
			case ItemID.GAUNTLET_CHESTPLATE_T3:
			case ItemID.GAUNTLET_CHESTPLATE_T3_HM:
				return stats(15, 16, 5, 16, 16, 0, 0, 124, 3);

			// Basic legs
			case ItemID.GAUNTLET_PLATELEGS_T1:
			case ItemID.GAUNTLET_PLATELEGS_T1_HM:
				return stats(6, 6, 2, 6, 6, 0, 0, 52, 1);
			// Attuned legs
			case ItemID.GAUNTLET_PLATELEGS_T2:
			case ItemID.GAUNTLET_PLATELEGS_T2_HM:
				return stats(10, 10, 3, 10, 10, 0, 0, 74, 2);
			// Perfected legs
			case ItemID.GAUNTLET_PLATELEGS_T3:
			case ItemID.GAUNTLET_PLATELEGS_T3_HM:
				return stats(14, 14, 4, 14, 14, 0, 0, 92, 3);

			// Basic halberds
			case ItemID.GAUNTLET_MELEE_T1:
			case ItemID.GAUNTLET_MELEE_T1_HM:
				return stats(68, 68, 4, 0, 0, 42, 0, 0, 0);
			// Attuned halberds
			case ItemID.GAUNTLET_MELEE_T2:
			case ItemID.GAUNTLET_MELEE_T2_HM:
				return stats(114, 114, 12, 0, 0, 88, 0, 0, 0);
			// Perfected halberds
			case ItemID.GAUNTLET_MELEE_T3:
			case ItemID.GAUNTLET_MELEE_T3_HM:
				return stats(166, 166, 28, 0, 0, 138, 0, 0, 0);

			// Basic bows
			case ItemID.GAUNTLET_RANGED_T1:
			case ItemID.GAUNTLET_RANGED_T1_HM:
				return stats(0, 0, 0, 0, 72, 0, 42, 0, 0);
			// Attuned bows
			case ItemID.GAUNTLET_RANGED_T2:
			case ItemID.GAUNTLET_RANGED_T2_HM:
				return stats(0, 0, 0, 0, 118, 0, 88, 0, 0);
			// Perfected bows
			case ItemID.GAUNTLET_RANGED_T3:
			case ItemID.GAUNTLET_RANGED_T3_HM:
				return stats(0, 0, 0, 0, 172, 0, 138, 0, 0);

			// Powered staves
			case ItemID.GAUNTLET_MAGIC_T1:
			case ItemID.GAUNTLET_MAGIC_T1_HM:
				return stats(0, 0, 0, 84, 0, 0, 0, 0, 0);
			case ItemID.GAUNTLET_MAGIC_T2:
			case ItemID.GAUNTLET_MAGIC_T2_HM:
				return stats(0, 0, 0, 128, 0, 0, 0, 0, 0);
			case ItemID.GAUNTLET_MAGIC_T3:
			case ItemID.GAUNTLET_MAGIC_T3_HM:
				return stats(0, 0, 0, 184, 0, 0, 0, 0, 0);

			case ItemID.GAUNTLET_SCEPTRE:
				return stats(8, 10, 16, 0, 0, 20, 0, 0, 0);
			default:
				return ItemCombatStats.EMPTY;
		}
	}

	private static ItemCombatStats stats(
		int stabAttack,
		int slashAttack,
		int crushAttack,
		int magicAttack,
		int rangedAttack,
		int meleeStrength,
		int rangedStrength,
		int defence,
		int armourTier)
	{
		return new ItemCombatStats(
			stabAttack,
			slashAttack,
			crushAttack,
			magicAttack,
			rangedAttack,
			meleeStrength,
			rangedStrength,
			defence,
			defence,
			armourTier);
	}

	private static final class ItemCombatStats
	{
		private static final ItemCombatStats EMPTY =
			new ItemCombatStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		private final int stabAttack;
		private final int slashAttack;
		private final int crushAttack;
		private final int magicAttack;
		private final int rangedAttack;
		private final int meleeStrength;
		private final int rangedStrength;
		private final int magicDefence;
		private final int rangedDefence;
		private final int armourTier;

		private ItemCombatStats(
			int stabAttack,
			int slashAttack,
			int crushAttack,
			int magicAttack,
			int rangedAttack,
			int meleeStrength,
			int rangedStrength,
			int magicDefence,
			int rangedDefence,
			int armourTier)
		{
			this.stabAttack = stabAttack;
			this.slashAttack = slashAttack;
			this.crushAttack = crushAttack;
			this.magicAttack = magicAttack;
			this.rangedAttack = rangedAttack;
			this.meleeStrength = meleeStrength;
			this.rangedStrength = rangedStrength;
			this.magicDefence = magicDefence;
			this.rangedDefence = rangedDefence;
			this.armourTier = armourTier;
		}
	}
}
