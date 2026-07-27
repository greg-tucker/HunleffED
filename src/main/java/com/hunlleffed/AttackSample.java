package com.hunlleffed;

final class AttackSample
{
	private final DamageEstimate estimate;
	private final int attackSpeed;
	private final int elapsedTick;

	AttackSample(DamageEstimate estimate, int attackSpeed)
	{
		this(estimate, attackSpeed, 0);
	}

	AttackSample(DamageEstimate estimate, int attackSpeed, int elapsedTick)
	{
		if (attackSpeed <= 0)
		{
			throw new IllegalArgumentException("Attack speed must be positive");
		}
		this.estimate = estimate;
		this.attackSpeed = attackSpeed;
		this.elapsedTick = Math.max(0, elapsedTick);
	}

	DamageEstimate getEstimate()
	{
		return estimate;
	}

	int getAttackSpeed()
	{
		return attackSpeed;
	}

	int getElapsedTick()
	{
		return elapsedTick;
	}
}
