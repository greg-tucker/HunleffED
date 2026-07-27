package com.hunlleffed;

enum GauntletDifficulty
{
	CRYSTALLINE(600, 76, 50, new int[]{12, 10, 8, 6}, new int[][]{
		{10, 20},
		{10, 16},
		{7, 13},
		{5, 10}
	}),
	CORRUPTED(1000, 90, 68, new int[]{16, 13, 10, 8}, new int[][]{
		{15, 30},
		{14, 25},
		{10, 20},
		{8, 15}
	});

	private final int hitpoints;
	private final int attackBonus;
	private final int unprotectedMaxHit;
	private final int[] protectedMaxHits;
	private final int[][] tornadoRanges;

	GauntletDifficulty(
		int hitpoints,
		int attackBonus,
		int unprotectedMaxHit,
		int[] protectedMaxHits,
		int[][] tornadoRanges)
	{
		this.hitpoints = hitpoints;
		this.attackBonus = attackBonus;
		this.unprotectedMaxHit = unprotectedMaxHit;
		this.protectedMaxHits = protectedMaxHits;
		this.tornadoRanges = tornadoRanges;
	}

	int getHitpoints()
	{
		return hitpoints;
	}

	int getAttackBonus()
	{
		return attackBonus;
	}

	int getUnprotectedMaxHit()
	{
		return unprotectedMaxHit;
	}

	int getProtectedMaxHit(int armourTierSum)
	{
		return protectedMaxHits[damageReductionTier(armourTierSum)];
	}

	double getTornadoExpectedDamage(int armourTierSum)
	{
		int[] range = tornadoRanges[damageReductionTier(armourTierSum)];
		return (range[0] + range[1]) / 2.0;
	}

	private static int damageReductionTier(int armourTierSum)
	{
		if (armourTierSum >= 9)
		{
			return 3;
		}
		if (armourTierSum >= 6)
		{
			return 2;
		}
		if (armourTierSum >= 3)
		{
			return 1;
		}
		return 0;
	}
}
