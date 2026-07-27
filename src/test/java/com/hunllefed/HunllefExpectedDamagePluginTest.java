package com.hunllefed;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HunllefExpectedDamagePluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HunllefExpectedDamagePlugin.class);
		RuneLite.main(args);
	}
}
