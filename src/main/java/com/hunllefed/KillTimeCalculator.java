package com.hunllefed;

import java.util.List;
import java.util.function.IntToDoubleFunction;

final class KillTimeCalculator
{
	private static final double SURVIVAL_EPSILON = 1.0e-10;
	private static final int MAX_ATTACKS = 100_000;
	private static final int INITIAL_ATTACK_DELAY = 4;

	private KillTimeCalculator()
	{
	}

	/**
	 * Calculates the exact expected time for the observed attack sequence,
	 * repeated as a cycle with every attack made as soon as its weapon permits.
	 * Each attack uses the damage distribution captured in its estimate.
	 */
	static double expectedKillTicks(int hitpoints, List<AttackSample> attacks)
	{
		return expectedKillTicks(
			hitpoints,
			attacks,
			attackIndex -> attackIndex == 0
				? INITIAL_ATTACK_DELAY
				: attacks.get((attackIndex - 1) % attacks.size()).getAttackSpeed());
	}

	/**
	 * Calculates expected time with the player's observed gaps between attacks.
	 * The observed cadence repeats if alternate RNG requires more attacks than
	 * occurred in the real encounter.
	 */
	static double performanceExpectedKillTicks(
		int hitpoints,
		List<AttackSample> attacks)
	{
		if (attacks.isEmpty())
		{
			return Double.NaN;
		}

		double averageLostTicks = averageLostTicks(attacks);
		int firstAttackTick = Math.max(INITIAL_ATTACK_DELAY, attacks.get(0).getElapsedTick());
		return expectedKillTicks(
			hitpoints,
			attacks,
			attackIndex ->
			{
				if (attackIndex == 0)
				{
					return firstAttackTick;
				}

				int sampleIndex = attackIndex % attacks.size();
				if (sampleIndex == 0)
				{
					AttackSample last = attacks.get(attacks.size() - 1);
					return last.getAttackSpeed() + averageLostTicks;
				}

				AttackSample current = attacks.get(sampleIndex);
				AttackSample previous = attacks.get(sampleIndex - 1);
				return Math.max(
					1,
					current.getElapsedTick() - previous.getElapsedTick());
			});
	}

	private static double expectedKillTicks(
		int hitpoints,
		List<AttackSample> attacks,
		IntToDoubleFunction intervalTicks)
	{
		if (hitpoints <= 0)
		{
			return 0.0;
		}
		if (attacks.isEmpty()
			|| attacks.stream().noneMatch(sample ->
				sample.getEstimate().getExpectedDamage() > 0.0))
		{
			return Double.NaN;
		}

		double[] remainingHp = new double[hitpoints + 1];
		remainingHp[hitpoints] = 1.0;
		double expectedTicks = 0.0;

		for (int attackIndex = 0; attackIndex < MAX_ATTACKS; attackIndex++)
		{
			double aliveProbability = aliveProbability(remainingHp);
			if (aliveProbability < SURVIVAL_EPSILON)
			{
				return expectedTicks;
			}

			AttackSample sample = attacks.get(attackIndex % attacks.size());
			expectedTicks += aliveProbability * intervalTicks.applyAsDouble(attackIndex);
			remainingHp = applyAttack(remainingHp, sample.getEstimate());
		}

		return Double.NaN;
	}

	private static double averageLostTicks(List<AttackSample> attacks)
	{
		if (attacks.size() < 2)
		{
			return 0.0;
		}

		double lostTicks = 0.0;
		for (int index = 1; index < attacks.size(); index++)
		{
			AttackSample previous = attacks.get(index - 1);
			int observedInterval =
				attacks.get(index).getElapsedTick() - previous.getElapsedTick();
			lostTicks += Math.max(0, observedInterval - previous.getAttackSpeed());
		}
		return lostTicks / (attacks.size() - 1);
	}

	private static double[] applyAttack(double[] remainingHp, DamageEstimate estimate)
	{
		double[] next = new double[remainingHp.length];
		next[0] = remainingHp[0];

		int maxHit = estimate.getMaxHit();
		double hitChance = estimate.getHitChance();
		if (maxHit <= 0 || hitChance <= 0.0)
		{
			for (int hp = 1; hp < remainingHp.length; hp++)
			{
				next[hp] += remainingHp[hp];
			}
			return next;
		}

		double probabilityPerAccurateRoll = hitChance / (maxHit + 1);
		double zeroDamageProbability = 1.0 - hitChance;
		if (!estimate.clampsSuccessfulZero())
		{
			zeroDamageProbability += probabilityPerAccurateRoll;
		}
		for (int hp = 1; hp < remainingHp.length; hp++)
		{
			double stateProbability = remainingHp[hp];
			if (stateProbability == 0.0)
			{
				continue;
			}

			next[hp] += stateProbability * zeroDamageProbability;
			for (int damage = 1; damage <= maxHit; damage++)
			{
				double damageProbability = probabilityPerAccurateRoll;
				if (estimate.clampsSuccessfulZero() && damage == 1)
				{
					damageProbability += probabilityPerAccurateRoll;
				}
				next[Math.max(0, hp - damage)] +=
					stateProbability * damageProbability;
			}
		}
		return next;
	}

	private static double aliveProbability(double[] remainingHp)
	{
		double probability = 0.0;
		for (int hp = 1; hp < remainingHp.length; hp++)
		{
			probability += remainingHp[hp];
		}
		return probability;
	}
}
