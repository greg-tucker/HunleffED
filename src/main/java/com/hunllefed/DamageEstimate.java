package com.hunllefed;

final class DamageEstimate
{
	private final double hitChance;
	private final int maxHit;
	private final double expectedDamage;
	private final boolean clampsSuccessfulZero;

	DamageEstimate(double hitChance, int maxHit)
	{
		this(hitChance, maxHit, false);
	}

	DamageEstimate(double hitChance, int maxHit, boolean clampsSuccessfulZero)
	{
		this.hitChance = hitChance;
		this.maxHit = maxHit;
		this.clampsSuccessfulZero = clampsSuccessfulZero;
		double averageAccurateHit = maxHit / 2.0;
		if (clampsSuccessfulZero && maxHit > 0)
		{
			averageAccurateHit += 1.0 / (maxHit + 1);
		}
		this.expectedDamage = hitChance * averageAccurateHit;
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

	boolean clampsSuccessfulZero()
	{
		return clampsSuccessfulZero;
	}
}
