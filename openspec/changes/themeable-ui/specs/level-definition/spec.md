## ADDED Requirements

### Requirement: Distinct villagers
Each level in `index.json` SHALL have its own villager. Loading SHALL reject an index in which two levels have the same villager id, with an error that names both level folders and the shared id.

#### Scenario: Two levels with the same villager
- **WHEN** `index.json` lists `000-hello-main-street` and `005-second-bakery`, and both levels' villager id is `rosa`
- **THEN** loading fails with an error naming `000-hello-main-street`, `005-second-bakery` and `rosa`

#### Scenario: Bundled levels
- **WHEN** the five Main Street levels are loaded
- **THEN** loading succeeds, with the villager ids `rosa`, `tom`, `marco`, `lucia` and `iara`
