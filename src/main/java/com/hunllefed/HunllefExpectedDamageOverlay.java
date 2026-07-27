package com.hunllefed;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

final class HunllefExpectedDamageOverlay extends OverlayPanel
{
	private static final Color GOOD = new Color(0x66, 0xCC, 0x66);
	private static final Color BAD = new Color(0xFF, 0x66, 0x66);
	private static final Color NEUTRAL = Color.LIGHT_GRAY;

	private final HunllefExpectedDamagePlugin plugin;
	private final HunllefExpectedDamageConfig config;

	@Inject
	private HunllefExpectedDamageOverlay(
		HunllefExpectedDamagePlugin plugin,
		HunllefExpectedDamageConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(270, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.isBossActive())
		{
			if (config.hideOverlayDuringFight())
			{
				return null;
			}
			renderLive(plugin.getStatistics());
			return super.render(graphics);
		}

		if (!plugin.shouldShowResults())
		{
			return null;
		}

		renderResults(plugin.getStatistics());
		return super.render(graphics);
	}

	private void renderLive(PerformanceStatistics stats)
	{
		addTitle("Hunllef analysis");
		addDamageLine("Damage dealt", stats.damageDealt, stats.expectedDamageDealt, true);
		addDamageLine("Damage taken", stats.damageTaken, stats.expectedDamageTaken, false);
		addLine("Lethal opportunities", Integer.toString(stats.lethalOpportunities), NEUTRAL);
		addLine("Death chance", percent(stats.getLethalChance()), riskColor(stats));
	}

	private void renderResults(PerformanceStatistics stats)
	{
		addTitle("Hunllef analysis");
		addLine("Damage: actual / expected", "", NEUTRAL);
		addDamageLine("Damage dealt", stats.damageDealt, stats.expectedDamageDealt, true);
		addDamageLine("Damage taken", stats.damageTaken, stats.expectedDamageTaken, false);
		addLine("Kill time", "", NEUTRAL);
		addKillTimeLines(stats);

		if (config.showFishAnalysis())
		{
			addLine("Food check (20 HP each)", "", NEUTRAL);
			addLine(
				"HP: start / end",
				stats.startingHitpoints + " / " + stats.endingHitpoints,
				NEUTRAL);
			addLine(
				"Needed this fight",
				format(stats.getActualFishEquivalent()),
				NEUTRAL);
			addLine(
				"Expected without avoidable hits",
				format(stats.getFishEquivalentWithoutAvoidableHits()),
				NEUTRAL);
			addLine(
				"Healing from avoidable hits",
				format(stats.getAvoidableHitHealingFishEquivalent()),
				avoidableHitHealingColor(stats.getAvoidableHitHealingFishEquivalent()));
		}

		addLine("Lethal opportunities", Integer.toString(stats.lethalOpportunities), NEUTRAL);
		addLine("Death chance", percent(stats.getLethalChance()), riskColor(stats));

		if (stats.unmodelledPlayerAttacks > 0)
		{
			int observedAttacks = stats.modelledPlayerAttacks + stats.unmodelledPlayerAttacks;
			addLine(
				"Damage model coverage",
				stats.modelledPlayerAttacks + " / " + observedAttacks + " attacks",
				Color.ORANGE);
		}
	}

	private void addKillTimeLines(PerformanceStatistics stats)
	{
		double actualTicks = stats.getActualKillTicks();
		addLine(
			"Actual",
			Double.isFinite(actualTicks) ? formatTime(actualTicks) : "DNF",
			Double.isFinite(actualTicks) ? NEUTRAL : BAD);
		addExpectedTimeLine("Performance EV", stats.performanceExpectedKillTicks);
		addExpectedTimeLine("No-loss EV", stats.noLossExpectedKillTicks);
	}

	private void addExpectedTimeLine(String label, double ticks)
	{
		addLine(
			label,
			Double.isFinite(ticks) ? formatTime(ticks) : "n/a",
			Double.isFinite(ticks) ? NEUTRAL : Color.ORANGE);
	}

	private void addDamageLine(String label, int actual, double expected, boolean higherIsBetter)
	{
		double difference = actual - expected;
		Color color;
		if (Math.abs(difference) < 0.05)
		{
			color = NEUTRAL;
		}
		else
		{
			boolean good = higherIsBetter ? difference > 0 : difference < 0;
			color = good ? GOOD : BAD;
		}

		addLine(
			label,
			actual + " / " + format(expected) + " (" + signed(difference) + ")",
			color);
	}

	private void addTitle(String title)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(title)
			.color(Color.WHITE)
			.build());
	}

	private void addLine(String left, String right, Color rightColor)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(rightColor)
			.build());
	}

	private static Color riskColor(PerformanceStatistics stats)
	{
		return stats.lethalOpportunities == 0 ? GOOD : Color.ORANGE;
	}

	private static Color avoidableHitHealingColor(double fishEquivalent)
	{
		return fishEquivalent < 0.05 ? GOOD : BAD;
	}

	private static String formatTime(double ticks)
	{
		double totalSeconds = ticks * 0.6;
		int minutes = (int) (totalSeconds / 60.0);
		double seconds = totalSeconds - minutes * 60.0;
		return String.format(Locale.ROOT, "%d:%04.1f", minutes, seconds);
	}

	private static String percent(double probability)
	{
		return format(probability * 100.0) + "%";
	}

	private static String format(double value)
	{
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static String signed(double value)
	{
		return String.format(Locale.ROOT, "%+.1f", value);
	}
}
