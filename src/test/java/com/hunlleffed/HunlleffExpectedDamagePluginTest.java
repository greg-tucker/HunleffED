package com.hunlleffed;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HunlleffExpectedDamagePluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HunlleffExpectedDamagePlugin.class);
		RuneLite.main(args);
	}
}
