package com.hunlleffed;

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

final class HunlleffExpectedDamageOverlay extends OverlayPanel
{
	private static final Color GOOD = new Color(0x66, 0xCC, 0x66);
	private static final Color BAD = new Color(0xFF, 0x66, 0x66);
	private static final Color NEUTRAL = Color.LIGHT_GRAY;

	private final HunlleffExpectedDamagePlugin plugin;
	private final HunlleffExpectedDamageConfig config;

	@Inject
	private HunlleffExpectedDamageOverlay(
		HunlleffExpectedDamagePlugin plugin,
		HunlleffExpectedDamageConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(260, 0));
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
		TickLossState tickState = plugin.getTickLossState();
		String tickText = tickState == TickLossState.NONE ? "No tick loss" : "Losing ticks";
		Color tickColor = tickState == TickLossState.NONE
			? GOOD
			: (tickState == TickLossState.POTENTIAL ? Color.ORANGE : BAD);

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(tickText)
			.color(tickColor)
			.build());
		addDamageLine("To Hunlleff", stats.damageDealt, stats.expectedDamageDealt, true);
		addDamageLine("To you", stats.damageTaken, stats.expectedDamageTaken, false);
		addLine("Tick use", format(stats.getTickUsePercent()) + "%", NEUTRAL);
	}

	private void renderResults(PerformanceStatistics stats)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Hunlleff performance")
			.color(Color.WHITE)
			.build());

		addLine("Actual / expected (difference)", "", NEUTRAL);
		addDamageLine("To Hunlleff", stats.damageDealt, stats.expectedDamageDealt, true);
		addDamageLine("To you", stats.damageTaken, stats.expectedDamageTaken, false);
		addLine(
			"Damage DPS",
			format(stats.getDps(stats.damageDealt)) + " dealt / "
				+ format(stats.getDps(stats.damageTaken)) + " taken",
			NEUTRAL);

		if (!config.showDetailedResults())
		{
			return;
		}

		addLine("Total ticks", Integer.toString(stats.totalTicks), NEUTRAL);
		addLine("Lost ticks", Integer.toString(stats.missedTicks), stats.missedTicks == 0 ? GOOD : BAD);
		addLine("Tick use", format(stats.getTickUsePercent()) + "%", NEUTRAL);
		addLine("Player attacks", Integer.toString(stats.playerAttacks), NEUTRAL);
		addLine("Wrong attack style", Integer.toString(stats.wrongAttackStyle),
			countColor(stats.wrongAttackStyle));
		addLine("Wrong offensive prayer", Integer.toString(stats.wrongOffensivePrayer),
			countColor(stats.wrongOffensivePrayer));
		addLine("Hunlleff attacks", Integer.toString(stats.hunlleffAttacks), NEUTRAL);
		addLine("Wrong defensive prayer", Integer.toString(stats.wrongDefensivePrayer),
			countColor(stats.wrongDefensivePrayer));
		addLine("Hunlleff stomps", Integer.toString(stats.hunlleffStomps),
			countColor(stats.hunlleffStomps));
		addLine("Tornado hits", Integer.toString(stats.tornadoHits),
			countColor(stats.tornadoHits));
		addLine("Floor hits", Integer.toString(stats.floorHits),
			countColor(stats.floorHits));

		if (stats.unmodelledPlayerAttacks > 0)
		{
			addLine(
				"Damage model coverage",
				stats.modelledPlayerAttacks + " / " + stats.playerAttacks + " attacks",
				Color.ORANGE);
		}
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

	private void addLine(String left, String right, Color rightColor)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(left)
			.right(right)
			.rightColor(rightColor)
			.build());
	}

	private static Color countColor(int value)
	{
		return value == 0 ? GOOD : BAD;
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

