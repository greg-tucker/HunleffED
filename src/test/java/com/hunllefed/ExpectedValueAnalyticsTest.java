package com.hunllefed;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpectedValueAnalyticsTest
{
	private static final double EPSILON = 0.000_001;

	@Test
	public void lethalChanceUsesAccuracyAndInclusiveDamageRolls()
	{
		DamageEstimate attack = new DamageEstimate(0.5, 10);

		// Five of the eleven accurate damage rolls (6..10) are lethal.
		assertEquals(
			5.0 / 22.0,
			LethalChanceCalculator.singleAttackChance(attack, 6),
			EPSILON);
		assertEquals(0.0, LethalChanceCalculator.singleAttackChance(attack, 11), 0.0);
	}

	@Test
	public void cumulativeLethalChanceUsesComplementOfSurvival()
	{
		PerformanceStatistics stats = new PerformanceStatistics();
		stats.reset();
		DamageEstimate attack = new DamageEstimate(0.5, 10);
		double singleChance = 5.0 / 22.0;

		stats.addLethalOpportunity(attack, 6);
		stats.addLethalOpportunity(attack, 6);

		assertEquals(2, stats.lethalOpportunities);
		assertEquals(1.0 - Math.pow(1.0 - singleChance, 2), stats.getLethalChance(), EPSILON);
	}

	@Test
	public void playerExpectedKillTimeClampsSuccessfulZeroDamageToOne()
	{
		double ticks = KillTimeCalculator.expectedKillTicks(
			1,
			Collections.singletonList(
				new AttackSample(new DamageEstimate(1.0, 2, true), 4)));

		// Accurate rolls 0, 1 and 2 become damage 1, 1 and 2.
		assertEquals(4.0, ticks, EPSILON);
	}

	@Test
	public void npcExpectedKillTimeRetainsSuccessfulZeroDamage()
	{
		double ticks = KillTimeCalculator.expectedKillTicks(
			1,
			Collections.singletonList(
				new AttackSample(new DamageEstimate(1.0, 2), 4)));

		// NPC accurate rolls remain 0, 1 and 2, so E[attacks] is 1.5.
		assertEquals(6.0, ticks, EPSILON);
	}

	@Test
	public void expectedKillTimeRepeatsObservedAttackCycleWithoutLostTicks()
	{
		double ticks = KillTimeCalculator.expectedKillTicks(
			1,
			Arrays.asList(
				new AttackSample(DamageEstimate.blocked(), 4),
				new AttackSample(new DamageEstimate(0.5, 2, true), 5)));

		// First live roll is at tick 8. Accuracy misses add a nine-tick cycle;
		// E[time] = 8 + (1/2)/(1/2) * 9 = 17 ticks.
		assertEquals(17.0, ticks, EPSILON);
	}

	@Test
	public void performanceExpectedKillTimePreservesObservedAttackGaps()
	{
		DamageEstimate attack = new DamageEstimate(0.5, 1, true);
		double ticks = KillTimeCalculator.performanceExpectedKillTicks(
			1,
			Arrays.asList(
				new AttackSample(attack, 4, 6),
				new AttackSample(attack, 4, 12)));

		// Every attack has a 50% kill chance. The observed six-tick cadence
		// therefore produces an expected kill on the second attack at tick 12.
		assertEquals(12.0, ticks, EPSILON);
		assertEquals(
			8.0,
			KillTimeCalculator.expectedKillTicks(
				1,
				Arrays.asList(
					new AttackSample(attack, 4, 6),
					new AttackSample(attack, 4, 12))),
			EPSILON);
	}

	@Test
	public void expectedKillTimeIsUnavailableWithoutDamage()
	{
		assertTrue(Double.isNaN(KillTimeCalculator.expectedKillTicks(
			1000,
			Collections.singletonList(new AttackSample(DamageEstimate.blocked(), 4)))));
	}

	@Test
	public void corruptedHitpointDistributionConverges()
	{
		double ticks = KillTimeCalculator.expectedKillTicks(
			1000,
			Collections.singletonList(
				new AttackSample(new DamageEstimate(0.72, 40, true), 4)));

		assertTrue(Double.isFinite(ticks));
		assertTrue(ticks > 250.0);
		assertTrue(ticks < 350.0);
	}

	@Test
	public void difficultiesExposeCorrectBossHitpoints()
	{
		assertEquals(600, GauntletDifficulty.CRYSTALLINE.getHitpoints());
		assertEquals(1000, GauntletDifficulty.CORRUPTED.getHitpoints());
	}

	@Test
	public void fishEquivalentAccountsForStartingAndEndingHitpoints()
	{
		PerformanceStatistics stats = new PerformanceStatistics();
		stats.reset();
		stats.startingHitpoints = 99;
		stats.endingHitpoints = 19;
		stats.damageTaken = 100;
		stats.expectedDamageTaken = 110;
		stats.expectedDamageTakenWithoutAvoidableHits = 90;

		assertEquals(1.0, stats.getActualFishEquivalent(), EPSILON);
		assertEquals(0.5, stats.getFishEquivalentWithoutAvoidableHits(), EPSILON);
		assertEquals(1.5, stats.getObservedMechanicsExpectedFishEquivalent(), EPSILON);
		assertEquals(1.0, stats.getAvoidableHitHealingFishEquivalent(), EPSILON);
	}

	@Test
	public void unusedStartingHitpointsCanCoverAllDamage()
	{
		PerformanceStatistics stats = new PerformanceStatistics();
		stats.reset();
		stats.startingHitpoints = 99;
		stats.endingHitpoints = 39;
		stats.damageTaken = 60;
		stats.expectedDamageTaken = 40;
		stats.expectedDamageTakenWithoutAvoidableHits = 40;

		assertEquals(0.0, stats.getActualFishEquivalent(), EPSILON);
		assertEquals(0.0, stats.getFishEquivalentWithoutAvoidableHits(), EPSILON);
		assertEquals(0.0, stats.getAvoidableHitHealingFishEquivalent(), EPSILON);
	}
}
