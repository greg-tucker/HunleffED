package com.hunlleffed;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(HunlleffExpectedDamageConfig.GROUP)
public interface HunlleffExpectedDamageConfig extends Config
{
	String GROUP = "hunlleffExpectedDamage";

	@ConfigItem(
		keyName = "hideOverlayDuringFight",
		name = "Hide during fight",
		description = "Hide the live overlay while fighting Hunlleff",
		position = 0
	)
	default boolean hideOverlayDuringFight()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDetailedResults",
		name = "Detailed results",
		description = "Show tick, prayer, attack-style, and hazard details after the fight",
		position = 1
	)
	default boolean showDetailedResults()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countUnarmedOffPrayer",
		name = "Count unarmed off-prayer",
		description = "Count punches, kicks, and sceptre attacks as an offensive-prayer mistake when no melee prayer is active",
		position = 2
	)
	default boolean countUnarmedOffPrayer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendChatSummary",
		name = "Chat summary",
		description = "Post the actual-versus-expected damage result to your local game chat when the fight ends",
		position = 3
	)
	default boolean sendChatSummary()
	{
		return false;
	}
}

