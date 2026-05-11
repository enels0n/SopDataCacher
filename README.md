# SopDataCacher

`SopDataCacher` is a PlaceholderAPI-powered cache plugin.

It periodically resolves configured placeholders for online players, stores values in a local SQLite database, and exposes fast cached placeholders for:

- direct per-player values,
- top-10 leaderboards,
- formatted top lines for scoreboards/chat.

## Features

- Periodic placeholder polling (`placeholders-update-time` in seconds).
- Named placeholder aliases via config (`named-placeholders`).
- SQLite storage (`plugins/SopDataCacher/database.db`) using JSON fields per player.
- Built-in top-10 ranking for numeric aliases.
- Flexible output formats (`int`, `round`, `duration`, `divide_*`, etc.).
- Custom top-line templates with filler/width controls.
- Runtime reload command.

## Requirements

- Java 8+
- Spigot/Paper 1.16.5+ (plugin targets API `1.16`)
- PlaceholderAPI (required dependency)

## Build

From repository root:

```bash
mvn -f "SopDataCacher/pom.xml" -DskipTests package
```

Output JAR:

- `SopDataCacher/target/SopDataCacher.jar`

## Installation

1. Put `SopDataCacher.jar` into your server `plugins/` directory.
2. Make sure PlaceholderAPI is installed.
3. Start the server once to generate `config.yml` and database files.
4. Edit `plugins/SopDataCacher/config.yml` as needed.
5. Reload with `/sopdatacacher reload` or restart server.

## Command

- `/sopdatacacher reload` - reloads plugin config and restarts cache updater task.

## Permissions

- `sopdatacacher.admin` (default: `op`)

## How It Works

1. The plugin loads aliases from `named-placeholders` in `config.yml`.
2. Every update cycle, it resolves each alias placeholder for every online player.
3. Values are saved into SQLite JSON data by alias key.
4. Cached values and top rankings are served through `%sopdatacacher_*%` placeholders.

## Config Structure

Top-level options:

- `placeholders-update-time`: update interval in seconds.
- `named-placeholders`: alias definitions.

Each alias supports:

- `placeholder`: original PlaceholderAPI placeholder to poll.
- `default`: fallback value when source is empty.
- `format`: default output format (`none` by default).
- `top-line` (optional): rendering settings for `%sopdatacacher_topline_*%`.

Example alias:

```yaml
named-placeholders:
  kills:
    placeholder: "%playerstats_only:number_raw,me,player_kills%"
    default: "0"
    format: none
    top-line:
      width: 32
      filler: "."
      template: "&a{name} &8{fill} &a{value}"
      use-minecraft-width: true
      min-fill: 3
```

## Placeholder Syntax

All plugin placeholders use prefix: `%sopdatacacher_...%`

### 1) Cached value for specific player and alias

Pattern:

- `%sopdatacacher_{<player_expr>}_{<alias>}%`
- `%sopdatacacher_{<player_expr>}_{<alias>}_<format>%`
- `%sopdatacacher_{<player_expr>}_{<alias>}_<format>_<precision>%`

Example:

- `%sopdatacacher_{player_name}_{kills}%`
- `%sopdatacacher_{player_name}_{balance}_round_2%`

`<player_expr>` is resolved through PlaceholderAPI as `%<player_expr>%`.

### 2) Nested player expression variant

Pattern:

- `%sopdatacacher_{##<player_expr>##}_{<alias>}%`

This is useful when you need a more complex dynamic player expression.

### 3) Top placeholders

Pattern:

- `%sopdatacacher_top_{<alias>}_<position>_name%`
- `%sopdatacacher_top_{<alias>}_<position>_value%`
- value variant also supports `_<format>` and `_<format>_<precision>`.

Examples:

- `%sopdatacacher_top_{kills}_1_name%`
- `%sopdatacacher_top_{kills}_1_value_int%`
- `%sopdatacacher_top_{balance}_3_value_round_2%`

### 4) Preformatted top line

Single alias (name + value in one line):

- `%sopdatacacher_topline_{<alias>}_<position>%`

Dual alias (sort by one alias, show another alias as value):

- `%sopdatacacher_topline_{<sort_alias>}_{<value_alias>}_<position>%`

Examples:

- `%sopdatacacher_topline_{kills}_1%`
- `%sopdatacacher_topline_{kills}_{balance}_1_round_2%`

## Supported Formats

Global/explicit format keywords:

- `none`
- `int`
- `floor`
- `ceil`
- `round`
- `duration` (input treated as seconds)
- `durationticks` / `duration_ticks` (input treated as ticks)

Division formats:

- `divide_<divisor>`
- `divide_<divisor>_<mode>`
- `divide_<divisor>_<mode>_<precision>`

Where `<mode>` can be:

- `int`
- `floor`
- `ceil`
- `round`

Examples:

- `divide_1000`
- `divide_1000_round_2`
- `divide_20_floor`

## Storage Details

- Database file: `plugins/SopDataCacher/database.db`
- Table: `players`
  - `player` (TEXT, primary key)
  - `data` (JSON object as TEXT)
- Alias values are stored as JSON fields under `data`.
- Numeric values are stored as numbers when possible, enabling proper top sorting.

## Notes

- Top leaderboards include only aliases with numeric defaults.
- If PlaceholderAPI is missing, the plugin logs a warning and placeholders are unavailable.
- Unknown alias/player resolution returns fallback default or `null` string depending on context.
