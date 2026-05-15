# Fake Activity Models

These models are intentionally soft. They guide random choices toward realistic outcomes without hardcoding one
play style, one time of day pattern, or one forced player path.

## Model Summary

| Feature | Runtime model | Learned data used |
| --- | --- | --- |
| Achievements | Soft progression graph + weighted frequency + per-player memory | Achievement message templates and timing samples |
| Deaths/kills | Weighted templates + validity filters + cooldown/history | Death message templates and timing samples |
| Join/leave | Population drift + interval histogram + username cooldown | Join/quit timing samples and captured player profiles |
| Usernames | Available-profile pool + recent-use cooldown | Captured join/quit profile data |

## Achievement Progression

The achievement graph is a probability shape, not a strict route. A fake player is more likely to get
achievements that fit its current progress and known prerequisites, but common server achievements can still win.

```mermaid
flowchart LR
  Start["fake player session"] --> Stone["story/mine_stone"]
  Stone --> Upgrade["story/upgrade_tools"]
  Stone --> Iron["story/smelt_iron"]
  Iron --> Armor["story/obtain_armor"]
  Iron --> Lava["story/lava_bucket"]
  Iron --> IronTools["story/iron_tools"]
  IronTools --> Diamond["story/mine_diamond"]
  Lava --> Obsidian["story/form_obsidian"]
  Obsidian --> Nether["story/enter_the_nether"]
  Nether --> Blaze["nether/obtain_blaze_rod"]
  Blaze --> Brew["nether/brew_potion"]
  Nether --> Eye["story/follow_ender_eye"]
  Eye --> End["story/enter_the_end"]
  End --> Dragon["end/kill_dragon"]

  Start --> Sleep["adventure/sleep_in_bed"]
  Start --> Fish["husbandry/fishy_business"]
  Start --> Seeds["husbandry/plant_seed"]
  Start --> Mob["adventure/kill_a_mob"]
  Mob --> Bow["adventure/shoot_arrow"]
```

Runtime scoring:

```text
achievement weight =
  server-observed message count
  * novelty for this fake player
  * soft prerequisite/progression score
  * small random variation
```

Stored per fake player:

```text
achievement-score
shown-achievements
shown-achievement-keys
last-achievement-millis
```

## Death/Kill Messages

Deaths do not need a prerequisite graph. They need context validity and repetition control.

```mermaid
flowchart LR
  Online["online fake player"] --> Select["select weighted death template"]
  Select --> Env["environment template"]
  Select --> Mob["mob template"]
  Select --> PvP["%player2% template"]

  PvP --> ValidKiller{"another fake player online?"}
  ValidKiller -->|"yes"| Broadcast["broadcast fake death"]
  ValidKiller -->|"no"| Reject["skip template"]

  Env --> Cooldown["template/player cooldown"]
  Mob --> Cooldown
  Cooldown --> Broadcast
```

Runtime scoring:

```text
death weight =
  server-observed template count
  * recent-template penalty
  * victim cooldown penalty
  * second-player validity score
```

## Join/Leave Population Drift

Join/leave messages are controlled by a target population model. The plugin picks a target inside the configured
range and moves toward it one join/leave event at a time.

```mermaid
flowchart LR
  Current["current fake count"] --> Compare{"compare to target"}
  Compare -->|"below target"| Join["join one fake player"]
  Compare -->|"above target"| Leave["leave one fake player"]
  Compare -->|"at target"| Wait["wait or retarget later"]

  Join --> Cooldown["record username cooldown"]
  Leave --> Cooldown
  Wait --> Retarget{"stable long enough?"}
  Retarget -->|"yes"| NewTarget["choose new target in min/max range"]
  Retarget -->|"no"| Current
  NewTarget --> Current
  Cooldown --> Current
```

Runtime scoring:

```text
join/leave action =
  configured min/max fake player range
  + learned join/quit interval timing
  + current fake count
  + stable target count
  + available username profiles
```

Username cooldown is a preference. If every available profile is on cooldown, the plugin still falls back to the
available pool so sparse data does not stop fake activity.

## Data Flow

```mermaid
flowchart TD
  RealEvents["real join, quit, death, advancement events"] --> Templates["safe templates"]
  Templates --> State["plugins/FakePlayer/data/state.yml"]
  State --> ActivityModel["FakeActivityModel"]

  ActivityModel --> AchievementModel["AchievementProgressionModel"]
  ActivityModel --> DeathModel["DeathMessageModel"]
  ActivityModel --> PopulationModel["JoinQuitPopulationModel"]

  AchievementModel --> FakeMessages["fake achievement messages"]
  DeathModel --> FakeMessages
  PopulationModel --> FakeJoins["fake join/leave messages"]
```
