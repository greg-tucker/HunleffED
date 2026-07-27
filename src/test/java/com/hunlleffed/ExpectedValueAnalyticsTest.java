package com.hunlleffed;

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
	public void expectedKillTimeIncludesZeroDamageAndOverkillOutcomes()
	{
		double ticks = KillTimeCalculator.expectedKillTicks(
			1,
			Collections.singletonList(new AttackSample(new DamageEstimate(1.0, 2), 4)));

		// Damage 0, 1 and 2 are equally likely, so the expected attack count is 1.5.
		assertEquals(6.0, ticks, EPSILON);
	}

	@Test
	public void expectedKillTimeRepeatsObservedAttackCycleWithoutLostTicks()
	{
		double ticks = KillTimeCalculator.expectedKillTicks(
			1,
			Arrays.asList(
				new AttackSample(DamageEstimate.blocked(), 4),
				new AttackSample(new DamageEstimate(1.0, 2), 5)));

		// First live roll is at tick 8. Failures add a nine-tick cycle;
		// E[time] = 8 + (1/3)/(2/3) * 9 = 12.5 ticks.
		assertEquals(12.5, ticks, EPSILON);
	}

	@Test
	public void performanceExpectedKillTimePreservesObservedAttackGaps()
	{
		DamageEstimate attack = new DamageEstimate(1.0, 1);
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
				new AttackSample(new DamageEstimate(0.72, 40), 4)));

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
		stats.cleanMechanicsExpectedDamageTaken = 90;

		assertEquals(1.0, stats.getActualFishEquivalent(), EPSILON);
		assertEquals(0.5, stats.getCleanMechanicsFishEquivalent(), EPSILON);
		assertEquals(1.5, stats.getObservedMechanicsExpectedFishEquivalent(), EPSILON);
		assertEquals(1.0, stats.getMistakeHealingFishEquivalent(), EPSILON);
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
		stats.cleanMechanicsExpectedDamageTaken = 40;

		assertEquals(0.0, stats.getActualFishEquivalent(), EPSILON);
		assertEquals(0.0, stats.getCleanMechanicsFishEquivalent(), EPSILON);
		assertEquals(0.0, stats.getMistakeHealingFishEquivalent(), EPSILON);
	}
}
