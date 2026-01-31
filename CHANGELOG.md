# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.1] - 2026-01-31
### Changed
- Improved last location logic

### Fixed
- Error when setting portal behavior on a new world

## [1.4.0] - 2026-01-04
### Added
- World export command
- Option to pick multiple worlds to import from an archive
- Set portal behavior command

### Changed
- Removed message-api library

## [1.3.7] - 2025-10-28
### Changed
- Load worlds earlier to improve mod compatibility

## [1.3.6] - 2025-08-08
### Fixed
- Create world gui not working with certain setups

## [1.3.5] - 2025-07-25
### Changed
- Improve world loading for old worlds (@chililisoup)

## [1.3.4] - 2025-07-24
### Fixed
- NullPointerException when importing worlds with `level.dat` in an archive top level (@chililisoup)

## [1.3.3] - 2025-06-25
### Fixed
- ClassNotFoundException on servers

## [1.3.2] - 2025-06-23
### Added
- Tar archive import support
- Option to specify custom config when importing

## [1.3.1] - 2025-05-28
### Changed
- Updated dependencies

## [1.3.0] - 2025-05-28
### Added
- World icons
- World management gui
- World list / management command

### Changed
- Improved gui layout

### Fixed
- Set world spawn translation missing

## [1.2.2] - 2025-05-21
### Changed
- Allow teleportation to non-custom worlds

## [1.2.1] - 2025-05-17
### Changed
- Use stonecutter to support 1.21.1, 1.21.4 and 1.21.5

### Fixed
- Teleport command message

## [1.2.0] - 2025-05-16
### Added
- World importing from zips, rars and folders
- World spawn locations
- Save players last world locations
- Superflat world generator presets

## [1.1.0] - 2025-04-10
### Added
- Time ticking configuration option

## [1.0.0] - 2025-04-04
init