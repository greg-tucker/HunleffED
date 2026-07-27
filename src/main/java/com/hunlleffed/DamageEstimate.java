package com.hunlleffed;

final class DamageEstimate
{
	private final double hitChance;
	private final int maxHit;
	private final double expectedDamage;

	DamageEstimate(double hitChance, int maxHit)
	{
		this.hitChance = hitChance;
		this.maxHit = maxHit;
		this.expectedDamage = hitChance * maxHit / 2.0;
	}

	static DamageEstimate blocked()
	{
		return new DamageEstimate(0.0, 0);
	}

	double getHitChance()
	{
		return hitChance;
	}

	int getMaxHit()
	{
		return maxHit;
	}

	double getExpectedDamage()
	{
		return expectedDamage;
	}
}

