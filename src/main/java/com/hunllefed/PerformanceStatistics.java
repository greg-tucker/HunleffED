package com.hunllefed;

import java.util.ArrayList;
import java.util.List;

final class PerformanceStatistics
{
	private static final int PADDLEFISH_HEAL = 20;

	private final List<AttackSample> playerAttackCycle = new ArrayList<>();

	int totalTicks;
	int modelledPlayerAttacks;
	int unmodelledPlayerAttacks;
	int damageDealt;
	int damageTaken;
	int startingHitpoints;
	int endingHitpoints;
	int lethalOpportunities;
	double expectedDamageDealt;
	double expectedDamageTaken;
	double expectedDamageTakenWithoutAvoidableHits;
	double lethalSurvivalProbability;
	double performanceExpectedKillTicks;
	double noLossExpectedKillTicks;
	boolean hunllefDefeated;

	void reset()
	{
		playerAttackCycle.clear();
		totalTicks = 0;
		modelledPlayerAttacks = 0;
		unmodelledPlayerAttacks = 0;
		damageDealt = 0;
		damageTaken = 0;
		startingHitpoints = 0;
		endingHitpoints = 0;
		lethalOpportunities = 0;
		expectedDamageDealt = 0;
		expectedDamageTaken = 0;
		expectedDamageTakenWithoutAvoidableHits = 0;
		lethalSurvivalProbability = 1.0;
		performanceExpectedKillTicks = Double.NaN;
		noLossExpectedKillTicks = Double.NaN;
		hunllefDefeated = false;
	}

	void addPlayerAttack(DamageEstimate estimate, int attackSpeed, int elapsedTick)
	{
		playerAttackCycle.add(new AttackSample(estimate, attackSpeed, elapsedTick));
		expectedDamageDealt += estimate.getExpectedDamage();
		modelledPlayerAttacks++;
	}

	void addLethalOpportunity(DamageEstimate estimate, int hitpoints)
	{
		double probability = LethalChanceCalculator.singleAttackChance(estimate, hitpoints);
		if (probability <= 0.0)
		{
			return;
		}

		lethalOpportunities++;
		lethalSurvivalProbability *= 1.0 - probability;
	}

	void finish(GauntletDifficulty difficulty)
	{
		performanceExpectedKillTicks = KillTimeCalculator.performanceExpectedKillTicks(
			difficulty.getHitpoints(),
			playerAttackCycle);
		noLossExpectedKillTicks = KillTimeCalculator.expectedKillTicks(
			difficulty.getHitpoints(),
			playerAttackCycle);
	}

	double getLethalChance()
	{
		return 1.0 - lethalSurvivalProbability;
	}

	double getActualKillTicks()
	{
		return hunllefDefeated ? totalTicks : Double.NaN;
	}

	double getDamageDealtDifference()
	{
		return damageDealt - expectedDamageDealt;
	}

	double getDamageTakenDifference()
	{
		return damageTaken - expectedDamageTaken;
	}

	double getActualFishEquivalent()
	{
		return fishEquivalent(damageTaken, startingHitpoints, endingHitpoints);
	}

	double getFishEquivalentWithoutAvoidableHits()
	{
		return fishEquivalent(
			expectedDamageTakenWithoutAvoidableHits,
			startingHitpoints,
			endingHitpoints);
	}

	double getObservedMechanicsExpectedFishEquivalent()
	{
		return fishEquivalent(
			expectedDamageTaken,
			startingHitpoints,
			endingHitpoints);
	}

	double getAvoidableHitHealingFishEquivalent()
	{
		return Math.max(
			0.0,
			getObservedMechanicsExpectedFishEquivalent()
				- getFishEquivalentWithoutAvoidableHits());
	}

	private static double fishEquivalent(
		double damage,
		int startingHitpoints,
		int endingHitpoints)
	{
		double healingRequired = damage + endingHitpoints - startingHitpoints;
		return Math.max(0.0, healingRequired) / PADDLEFISH_HEAL;
	}
}
