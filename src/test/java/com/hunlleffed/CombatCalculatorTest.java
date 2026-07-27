package com.hunlleffed;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CombatCalculatorTest
{
	private static final double EPSILON = 0.000_001;

	@Test
	public void normalAccuracyFormulaHandlesBothBranches()
	{
		assertEquals(1.0 - 52.0 / 202.0, CombatCalculator.hitChance(100, 50), EPSILON);
		assertEquals(50.0 / 202.0, CombatCalculator.hitChance(50, 100), EPSILON);
	}

	@Test
	public void perfectedStaffUsesFixedBaseHitAndAuguryDamage()
	{
		EquipmentSnapshot equipment = EquipmentSnapshot.fromItemIds(
			ItemID.GAUNTLET_MAGIC_T3_HM,
			ItemID.GAUNTLET_MAGIC_T3_HM,
			ItemID.GAUNTLET_HELMET_T1_HM,
			ItemID.GAUNTLET_CHESTPLATE_T1_HM,
			ItemID.GAUNTLET_PLATELEGS_T1_HM);

		DamageEstimate estimate = CombatCalculator.playerAttack(
			CombatStyle.MAGIC,
			99,
			99,
			equipment.getAttackBonus(CombatStyle.MAGIC, MeleeType.CRUSH),
			0,
			2,
			0,
			1.25,
			1.0,
			equipment.getMagicMaxHit(),
			40,
			false);

		int attackRoll = 134 * (200 + 64);
		int defenceRoll = 249 * 84;
		assertEquals(40, estimate.getMaxHit());
		assertEquals(CombatCalculator.hitChance(attackRoll, defenceRoll),
			estimate.getHitChance(), EPSILON);
		assertEquals(estimate.getHitChance() * 20.0, estimate.getExpectedDamage(), EPSILON);
	}

	@Test
	public void perfectedBowRapidUsesRangedLevelAndStrengthPrayer()
	{
		EquipmentSnapshot equipment = EquipmentSnapshot.fromItemIds(
			ItemID.GAUNTLET_RANGED_T3_HM,
			ItemID.GAUNTLET_RANGED_T3_HM,
			ItemID.GAUNTLET_HELMET_T1_HM,
			ItemID.GAUNTLET_CHESTPLATE_T1_HM,
			ItemID.GAUNTLET_PLATELEGS_T1_HM);

		DamageEstimate estimate = CombatCalculator.playerAttack(
			CombatStyle.RANGED,
			99,
			99,
			equipment.getAttackBonus(CombatStyle.RANGED, MeleeType.CRUSH),
			equipment.getStrengthBonus(CombatStyle.RANGED),
			0,
			0,
			1.20,
			1.23,
			0,
			0,
			false);

		assertEquals(41, estimate.getMaxHit());
		assertEquals(
			CombatCalculator.hitChance(126 * 252, 249 * 84),
			estimate.getHitChance(),
			EPSILON);
	}

	@Test
	public void hunlleffMagicAttackUsesPlayerMagicAndDefenceRoll()
	{
		EquipmentSnapshot equipment = EquipmentSnapshot.fromItemIds(
			ItemID.GAUNTLET_MAGIC_T3_HM,
			ItemID.GAUNTLET_MAGIC_T3_HM,
			ItemID.GAUNTLET_HELMET_T2_HM,
			ItemID.GAUNTLET_CHESTPLATE_T2_HM,
			ItemID.GAUNTLET_PLATELEGS_T2_HM);

		DamageEstimate protectedEstimate = CombatCalculator.hunlleffAttack(
			GauntletDifficulty.CORRUPTED,
			CombatStyle.MAGIC,
			99,
			99,
			equipment.getDefenceBonus(CombatStyle.MAGIC),
			0,
			1.25,
			1.25,
			true,
			equipment.getArmourTierSum());

		assertEquals(224, equipment.getDefenceBonus(CombatStyle.MAGIC));
		assertEquals(6, equipment.getArmourTierSum());
		assertEquals(10, protectedEstimate.getMaxHit());
		assertEquals(
			CombatCalculator.hitChance(249 * 154, 130 * 288),
			protectedEstimate.getHitChance(),
			EPSILON);

		DamageEstimate unprotectedEstimate = CombatCalculator.hunlleffAttack(
			GauntletDifficulty.CORRUPTED,
			CombatStyle.MAGIC,
			99,
			99,
			equipment.getDefenceBonus(CombatStyle.MAGIC),
			0,
			1.25,
			1.25,
			false,
			equipment.getArmourTierSum());
		assertEquals(68, unprotectedEstimate.getMaxHit());
	}

	@Test
	public void hunlleffProtectionAndTornadoCapsUseArmourTierSum()
	{
		assertEquals(16, GauntletDifficulty.CORRUPTED.getProtectedMaxHit(0));
		assertEquals(13, GauntletDifficulty.CORRUPTED.getProtectedMaxHit(3));
		assertEquals(10, GauntletDifficulty.CORRUPTED.getProtectedMaxHit(6));
		assertEquals(8, GauntletDifficulty.CORRUPTED.getProtectedMaxHit(9));
		assertEquals(19.5, GauntletDifficulty.CORRUPTED.getTornadoExpectedDamage(3), EPSILON);
		assertEquals(15.0, GauntletDifficulty.CORRUPTED.getTornadoExpectedDamage(6), EPSILON);

		assertEquals(12, GauntletDifficulty.CRYSTALLINE.getProtectedMaxHit(0));
		assertEquals(6, GauntletDifficulty.CRYSTALLINE.getProtectedMaxHit(9));
		assertEquals(7.5, GauntletDifficulty.CRYSTALLINE.getTornadoExpectedDamage(9), EPSILON);
	}

	@Test
	public void protectedPlayerAttackHasZeroExpectedDamage()
	{
		DamageEstimate estimate = CombatCalculator.playerAttack(
			CombatStyle.RANGED,
			99,
			99,
			188,
			138,
			0,
			0,
			1.20,
			1.23,
			0,
			0,
			true);

		assertEquals(0, estimate.getMaxHit());
		assertEquals(0.0, estimate.getExpectedDamage(), EPSILON);
	}

	@Test
	public void attackSpeedAccountsForBowStyleAndSceptre()
	{
		EquipmentSnapshot bow = EquipmentSnapshot.fromItemIds(ItemID.GAUNTLET_RANGED_T3_HM);
		assertEquals(5, bow.getAttackSpeed(0));
		assertEquals(4, bow.getAttackSpeed(1));
		assertEquals(5, bow.getAttackSpeed(3));

		EquipmentSnapshot sceptre = EquipmentSnapshot.fromItemIds(ItemID.GAUNTLET_SCEPTRE);
		assertEquals(5, sceptre.getAttackSpeed(0));
	}
}
