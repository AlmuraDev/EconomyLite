![EconomyLite](http://i.imgur.com/MsuOo9w.png)

[![Issues](https://img.shields.io/github/issues/flibio/economylite.svg?style=flat-square)](http://www.github.com/Flibio/EconomyLite/issues/)
[![Gitter](https://img.shields.io/badge/chat-on_gitter-3F51B5.svg?style=flat-square)](https://gitter.im/Flibio/EconomyLite)

EconomyLite is a lightweight, yet advanced economy plugin for your Sponge server. The plugin is built to be customizable, and you can enable and disable features at your will.

### Features

- Drag & Drop Installation
  - Just drop EconomyLite into your "mods" folder with SpongeNeo and start your server
- Local Storage
  - Balances are kept in an H2 database that EconomyLite brings with it
- Customizable
  - Much of EconomyLite can be changed to suit your needs
- Virtual Management
  - EconomyLite includes commands to manage virtual accounts
- Multiple Currencies
  - EconomyLite has built-in support for multiple currencies

### Installation

Drag and drop EconomyLite into your "mods" folder along with SpongeNeo and start your server. The configuration files can be found in `config/economylite`.

### Version 3 (branch `4.0`): SpongeAPI 21 / SpongeNeo

This branch runs on SpongeAPI 21, the API of SpongeNeo for Minecraft 26.3, and builds with JDK 25 (`gradlew build`, jar in `build/libs`).
The SpongeAPI 7 version stays on `master`.

- The commands, permissions, `config.conf`, `currencies.conf` and `messages.conf` are those of EconomyLite 2.x.
- The balances stay in `data.mv.db` with the same tables. SpongeAPI 21 has no SQL service, so EconomyLite bundles H2 2
  and loads it in a class loader of its own. On the first start a database written by EconomyLite 2.x (H2 1.4) is moved to
  H2 2 once, with a bundled H2 1.4.196; the old file is kept as `data.mv.db.h2-1.4.bak`, and the move is checked by
  comparing the account counts and the total of all player balances.
- The MySQL, loan and business modules, the `/migrate` command and bStats are not part of this version.

---

:inbox_tray: [**Download EconomyLite**][1]

:speech_balloon: [**Issues & Suggestions**][3]

:newspaper: [**Forum Post**][4]

:books: [**Docs**][5]

:heavy_dollar_sign: [**Support Me**][6]

---

![EconomyLite Statistics](http://i.flibio.net/economylite.php)

[1]: https://ore.spongepowered.org/Flibio/EconomyLite/versions
[3]: https://github.com/Flibio/EconomyLite/issues
[4]: https://forums.spongepowered.org/t/economylite/16502
[5]: http://flibiostudio.github.io/EconomyLiteDocs/index.html
[6]: http://flibio.weebly.com/support-me.html
