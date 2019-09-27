# PlayerRider

PlayerRider is a very simple plugin for [Spigot](https://www.spigotmc.org) *(Minecraft server)* which allows you to ride other players. You can also make towers by riding a player who already rides another, and organize funny run because the rider can whip the player to give him a temporary boost.

## How to use it?

Right click on a player to ride him. Press the sneak key to get off.

**You can ride a player if**:

* You have the permission `playerrider.ride`
* This player has the permission `playerrider.duck` (and didn't disabled it, see commands)
* You are not in a vehicle / already on a player`
* Your hand contains one of the allowed items (configurable)

To get off a player, simply use the crounch / sneak key *(shift by default)*.
If you right click on a player who bears (an)other player(s), you will appear on the top of them.

**When riding a player**:

You can whip him by right clicking on his head while holding one the allowed items (also configurable). This will give him a speed boost you can adjust in the configuration. You need the permission `playerrider.whip` for this.

**When being rided**:

If you have the permission `playerrider.ride`, you can eject your rider by raise up your head and right click on him.

## Requirements

You need a [Spigot](https://www.spigotmc.org) / [PaperSpigot](https://papermc.io), version 1.13 **(only tested with 1.13.2)**. There is no plugin dependencies.

## Installation

Simply copy [PlayerRider.jar](https://github.com/arboriginal/PlayerRider/releases) into your "plugins" directory, then start the server. A new folder containing the default `config.yml` is automatically generated.

## Commands

* **prider-reload**: Reload the configuration.
* **prider-toggle**: Toggle own ridability.

## Permissions

* **playerrider.reload**: Allows to use `/prider-reload` command.
* **playerrider.ride**: Allows to ride a player.
* **playerrider.ride.keepitem**: Allows to ride a player without consuming the item (if activated).
* **playerrider.whip**t: Allows to whip he player you are riding.
* **playerrider.whip.keepitem**t: Allows to whip he player you are riding without consuming the item (if activated).
* **playerrider.duck**: Allows to be ridden.
* **playerrider.ducktoggle**: Allows to use `/prider-toggle` command.
* **playerrider.eject**: Allows to eject your passenger.

## Configuration

Edit `plugins/PlayerRider/config.yml` then reload the plugin, or the server if you prefer (it also works well with [plugman](https://dev.bukkit.org/projects/plugman)). All parameters are explained in the [config.yml from the source code](https://github.com/arboriginal/PlayerRider/blob/master/src/config.yml).

## Previous version

I did this plugin in 2012, as an easter egg, players on the server I was got tons of fun with it *(we mainly used it on new guys who are not correct)*. So I've decided to extract this part, and shared it as an independent plugin on [Bukkit.org page](http://dev.bukkit.org/projects/playerrider).

## TODO
Because hitboxes have changed, the player ridden can see his rider's legs. I will work soon to improve this not to reduce the FOV. But the plugin is fully working except this visual effect.
