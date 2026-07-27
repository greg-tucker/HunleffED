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
		keyName = "showFishAnalysis",
		name = "Show fish analysis",
		description = "Show health-adjusted food needs and expected healing caused by mistakes in 20-HP paddlefish equivalents",
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
