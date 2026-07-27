package com.hunllefed;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(HunllefExpectedDamageConfig.GROUP)
public interface HunllefExpectedDamageConfig extends Config
{
	String GROUP = "hunllefExpectedDamage";

	@ConfigItem(
		keyName = "hideOverlayDuringFight",
		name = "Hide during fight",
		description = "Hide the live overlay while fighting Hunllef",
		position = 0
	)
	default boolean hideOverlayDuringFight()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showFishAnalysis",
		name = "Show fish analysis",
		description = "Show health-adjusted food needs and expected healing from avoidable hits in 20-HP paddlefish equivalents",
		position = 1
	)
	default boolean showFishAnalysis()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendChatSummary",
		name = "Chat summary",
		description = "Post the enabled result statistics to local game chat",
		position = 2
	)
	default boolean sendChatSummary()
	{
		return false;
	}
}
