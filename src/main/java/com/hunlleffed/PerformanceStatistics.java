package com.hunlleffed;

final class PerformanceStatistics
{
	int totalTicks;
	int missedTicks;
	int playerAttacks;
	int modelledPlayerAttacks;
	int unmodelledPlayerAttacks;
	int wrongAttackStyle;
	int wrongOffensivePrayer;
	int wrongDefensivePrayer;
	int hunlleffAttacks;
	int modelledHunlleffAttacks;
	int hunlleffStomps;
	int tornadoHits;
	int floorHits;
	int damageDealt;
	int damageTaken;
	double expectedDamageDealt;
	double expectedDamageTaken;

	void reset()
	{
		totalTicks = 0;
		missedTicks = 0;
		playerAttacks = 0;
		modelledPlayerAttacks = 0;
		unmodelledPlayerAttacks = 0;
		wrongAttackStyle = 0;
		wrongOffensivePrayer = 0;
		wrongDefensivePrayer = 0;
		hunlleffAttacks = 0;
		modelledHunlleffAttacks = 0;
		hunlleffStomps = 0;
		tornadoHits = 0;
		floorHits = 0;
		damageDealt = 0;
		damageTaken = 0;
		expectedDamageDealt = 0;
		expectedDamageTaken = 0;
	}

	double getTickUsePercent()
	{
		if (totalTicks <= 0)
		{
			return 0.0;
		}
		return Math.max(0.0, (1.0 - (double) missedTicks / totalTicks) * 100.0);
	}

	double getDps(int damage)
	{
		if (totalTicks <= 0)
		{
			return 0.0;
		}
		return damage / (totalTicks * 0.6);
	}

	double getDamageDealtDifference()
	{
		return damageDealt - expectedDamageDealt;
	}

	double getDamageTakenDifference()
	{
		return damageTaken - expectedDamageTaken;
	}
}

