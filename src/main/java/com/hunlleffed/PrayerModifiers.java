package com.hunlleffed;

import net.runelite.api.Client;
import net.runelite.api.Prayer;

final class PrayerModifiers
{
	private final double accuracy;
	private final double strength;
	private final int magicDamagePermille;
	private final double defence;
	private final double magicDefence;

	private PrayerModifiers(
		double accuracy,
		double strength,
		int magicDamagePermille,
		double defence,
		double magicDefence)
	{
		this.accuracy = accuracy;
		this.strength = strength;
		this.magicDamagePermille = magicDamagePermille;
		this.defence = defence;
		this.magicDefence = magicDefence;
	}

	static PrayerModifiers capture(Client client, CombatStyle style)
	{
		double accuracy = 1.0;
		double strength = 1.0;
		int magicDamagePermille = 0;

		switch (style)
		{
			case MELEE:
				if (active(client, Prayer.PIETY))
				{
					accuracy = 1.20;
					strength = 1.23;
				}
				else if (active(client, Prayer.CHIVALRY))
				{
					accuracy = 1.15;
					strength = 1.18;
				}
				else
				{
					accuracy = meleeAccuracy(client);
					strength = meleeStrength(client);
				}
				break;
			case RANGED:
				if (active(client, Prayer.RIGOUR))
				{
					accuracy = 1.20;
					strength = 1.23;
				}
				else if (active(client, Prayer.DEADEYE))
				{
					accuracy = 1.18;
					strength = 1.18;
				}
				else if (active(client, Prayer.EAGLE_EYE))
				{
					accuracy = 1.15;
					strength = 1.15;
				}
				else if (active(client, Prayer.HAWK_EYE))
				{
					accuracy = 1.10;
					strength = 1.10;
				}
				else if (active(client, Prayer.SHARP_EYE))
				{
					accuracy = 1.05;
					strength = 1.05;
				}
				break;
			case MAGIC:
				if (active(client, Prayer.AUGURY))
				{
					accuracy = 1.25;
					magicDamagePermille = 40;
				}
				else if (active(client, Prayer.MYSTIC_VIGOUR))
				{
					accuracy = 1.18;
					magicDamagePermille = 30;
				}
				else if (active(client, Prayer.MYSTIC_MIGHT))
				{
					accuracy = 1.15;
					magicDamagePermille = 20;
				}
				else if (active(client, Prayer.MYSTIC_LORE))
				{
					accuracy = 1.10;
					magicDamagePermille = 10;
				}
				else if (active(client, Prayer.MYSTIC_WILL))
				{
					accuracy = 1.05;
				}
				break;
			default:
				break;
		}

		return new PrayerModifiers(
			accuracy,
			strength,
			magicDamagePermille,
			defence(client),
			magicDefence(client));
	}

	double getAccuracy()
	{
		return accuracy;
	}

	double getStrength()
	{
		return strength;
	}

	int getMagicDamagePermille()
	{
		return magicDamagePermille;
	}

	double getDefence()
	{
		return defence;
	}

	double getMagicDefence()
	{
		return magicDefence;
	}

	boolean hasOffensivePrayer()
	{
		return accuracy > 1.0 || strength > 1.0 || magicDamagePermille > 0;
	}

	private static double meleeAccuracy(Client client)
	{
		if (active(client, Prayer.INCREDIBLE_REFLEXES))
		{
			return 1.15;
		}
		if (active(client, Prayer.IMPROVED_REFLEXES))
		{
			return 1.10;
		}
		if (active(client, Prayer.CLARITY_OF_THOUGHT))
		{
			return 1.05;
		}
		return 1.0;
	}

	private static double meleeStrength(Client client)
	{
		if (active(client, Prayer.ULTIMATE_STRENGTH))
		{
			return 1.15;
		}
		if (active(client, Prayer.SUPERHUMAN_STRENGTH))
		{
			return 1.10;
		}
		if (active(client, Prayer.BURST_OF_STRENGTH))
		{
			return 1.05;
		}
		return 1.0;
	}

	private static double defence(Client client)
	{
		if (active(client, Prayer.PIETY)
			|| active(client, Prayer.RIGOUR)
			|| active(client, Prayer.AUGURY))
		{
			return 1.25;
		}
		if (active(client, Prayer.CHIVALRY))
		{
			return 1.20;
		}
		if (active(client, Prayer.STEEL_SKIN))
		{
			return 1.15;
		}
		if (active(client, Prayer.ROCK_SKIN))
		{
			return 1.10;
		}
		if (active(client, Prayer.DEADEYE)
			|| active(client, Prayer.MYSTIC_VIGOUR)
			|| active(client, Prayer.THICK_SKIN))
		{
			return 1.05;
		}
		return 1.0;
	}

	private static double magicDefence(Client client)
	{
		if (active(client, Prayer.AUGURY))
		{
			return 1.25;
		}
		if (active(client, Prayer.MYSTIC_VIGOUR))
		{
			return 1.18;
		}
		if (active(client, Prayer.MYSTIC_MIGHT))
		{
			return 1.15;
		}
		if (active(client, Prayer.MYSTIC_LORE))
		{
			return 1.10;
		}
		if (active(client, Prayer.MYSTIC_WILL))
		{
			return 1.05;
		}
		return 1.0;
	}

	private static boolean active(Client client, Prayer prayer)
	{
		return client.getVarbitValue(prayer.getVarbit()) == 1;
	}
}
