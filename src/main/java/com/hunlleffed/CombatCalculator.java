package com.hunlleffed;

final class CombatCalculator
{
	static final int HUNLLEFF_DEFENCE_LEVEL = 240;
	static final int HUNLLEFF_DEFENCE_BONUS = 20;
	static final int FLOOR_MIN_HIT = 10;
	static final int FLOOR_MAX_HIT = 20;

	private CombatCalculator()
	{
	}

	static DamageEstimate playerAttack(
		CombatStyle style,
		int accuracyLevel,
		int damageLevel,
		int attackBonus,
		int strengthBonus,
		int accuracyStanceBonus,
		int strengthStanceBonus,
		double accuracyPrayer,
		double strengthPrayer,
		int fixedMagicMaxHit,
		int magicDamagePermille,
		boolean protectedByHunlleff)
	{
		if (protectedByHunlleff)
		{
			return DamageEstimate.blocked();
		}

		int effectiveAccuracy;
		int maxHit;

		switch (style)
		{
			case MELEE:
				effectiveAccuracy = scaleLevel(accuracyLevel, accuracyPrayer)
					+ 8 + accuracyStanceBonus;
				int effectiveStrength = scaleLevel(damageLevel, strengthPrayer)
					+ 8 + strengthStanceBonus;
				maxHit = maxHit(effectiveStrength, strengthBonus);
				break;
			case RANGED:
				effectiveAccuracy = scaleLevel(accuracyLevel, accuracyPrayer)
					+ 8 + accuracyStanceBonus;
				int effectiveRangedStrength = scaleLevel(damageLevel, strengthPrayer)
					+ 8 + strengthStanceBonus;
				maxHit = maxHit(effectiveRangedStrength, strengthBonus);
				break;
			case MAGIC:
				if (fixedMagicMaxHit <= 0)
				{
					return DamageEstimate.blocked();
				}
				effectiveAccuracy = scaleLevel(accuracyLevel, accuracyPrayer)
					+ 9 + accuracyStanceBonus;
				maxHit = fixedMagicMaxHit
					+ (fixedMagicMaxHit * magicDamagePermille / 1000);
				break;
			default:
				throw new IllegalArgumentException("Unsupported combat style: " + style);
		}

		int attackRoll = effectiveAccuracy * (attackBonus + 64);
		int defenceRoll = (HUNLLEFF_DEFENCE_LEVEL + 9) * (HUNLLEFF_DEFENCE_BONUS + 64);
		return new DamageEstimate(hitChance(attackRoll, defenceRoll), maxHit);
	}

	static DamageEstimate hunlleffAttack(
		GauntletDifficulty difficulty,
		CombatStyle style,
		int defenceLevel,
		int magicLevel,
		int defenceBonus,
		int defensiveStanceBonus,
		double defencePrayer,
		double magicDefencePrayer,
		boolean correctProtectionPrayer,
		int armourTierSum)
	{
		int effectiveDefence = scaleLevel(defenceLevel, defencePrayer);
		if (style == CombatStyle.MAGIC)
		{
			int effectiveMagic = scaleLevel(magicLevel, magicDefencePrayer);
			effectiveDefence = effectiveMagic * 7 / 10 + effectiveDefence * 3 / 10;
		}

		effectiveDefence += defensiveStanceBonus;
		int defenceRoll = (effectiveDefence + 8) * (defenceBonus + 64);
		int attackRoll = (HUNLLEFF_DEFENCE_LEVEL + 9) * (difficulty.getAttackBonus() + 64);
		int maxHit = correctProtectionPrayer
			? difficulty.getProtectedMaxHit(armourTierSum)
			: difficulty.getUnprotectedMaxHit();

		return new DamageEstimate(hitChance(attackRoll, defenceRoll), maxHit);
	}

	static double stompExpectedDamage(GauntletDifficulty difficulty)
	{
		return stompAttack(difficulty).getExpectedDamage();
	}

	static DamageEstimate stompAttack(GauntletDifficulty difficulty)
	{
		return new DamageEstimate(1.0, difficulty.getUnprotectedMaxHit());
	}

	static double floorExpectedDamage()
	{
		return (FLOOR_MIN_HIT + FLOOR_MAX_HIT) / 2.0;
	}

	static double hitChance(int attackRoll, int defenceRoll)
	{
		if (attackRoll > defenceRoll)
		{
			return 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
		}
		return attackRoll / (2.0 * (defenceRoll + 1.0));
	}

	private static int scaleLevel(int level, double multiplier)
	{
		return (int) Math.floor(level * multiplier);
	}

	private static int maxHit(int effectiveStrength, int strengthBonus)
	{
		return (effectiveStrength * (strengthBonus + 64) + 320) / 640;
	}

}
