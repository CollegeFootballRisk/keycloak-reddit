# keycloak-reddit

Keycloak Social Login extension for Reddit.

## Install

Download `keycloak-reddit-<version>.jar` from [Releases page](https://github.com/collegefootballrisk/keycloak-reddit/releases).
Then deploy it into `$KEYCLOAK_HOME/providers` directory.

## Setup

### Reddit

Access to [Reddit authorized applications portal](https://www.reddit.com/prefs/apps) and create your application.
You can get Client ID and Client Secret from the created application.

### Keycloak

Note: You don't need to setup the theme in `master` realm from v0.3.0.

1. Add `reddit` Identity Provider in the realm which you want to configure.
2. In the `reddit` identity provider page, set `Client Id` and `Client Secret`.
3. (Optional) Set Guild Id(s) to allow federation if you want.

## Source Build

Clone this repository and run `mvn package`.
You can see `keycloak-reddit-<version>.jar` under `target` directory.

## Licence

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Author

- [Hiroyuki Wada](https://github.com/wadahiro): Discord version, on which this is based
- [Mautamu](https://github.com/mautamu): Reddit version
