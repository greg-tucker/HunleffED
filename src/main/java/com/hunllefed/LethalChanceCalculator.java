package com.hunllefed;

final class LethalChanceCalculator
{
	private LethalChanceCalculator()
	{
	}

	static double singleAttackChance(DamageEstimate estimate, int hitpoints)
	{
		if (hitpoints <= 0)
		{
			return 1.0;
		}

		int maxHit = estimate.getMaxHit();
		if (maxHit < hitpoints)
		{
			return 0.0;
		}

		int lethalDamageRolls = maxHit - hitpoints + 1;
		double lethalRollChance = (double) lethalDamageRolls / (maxHit + 1);
		return estimate.getHitChance() * lethalRollChance;
	}
}
