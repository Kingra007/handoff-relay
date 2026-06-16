# Contributing to Handoff Relay

Thank you for your interest in contributing to Handoff Relay.

## Before You Start

Please ensure you:

* Use the latest version of the mod and development environment.
* Search existing issues before creating a new one.
* Keep discussions respectful and constructive.

## Reporting Bugs

When reporting a bug, please include:

* Minecraft version
* Fabric Loader version
* Mod version
* Steps to reproduce the issue
* Expected behaviour
* Actual behaviour
* Relevant logs, screenshots, or crash reports

Issues that cannot be reproduced may be closed until additional information is provided.

## Suggesting Features

Feature requests are welcome.

When submitting a feature request, please explain:

* The problem you are trying to solve
* Your proposed solution
* Any alternatives you considered
* How the feature fits the relay gameplay experience

Not all feature requests will be accepted, particularly if they conflict with the core design goals of the project.

## Development Setup

Requirements:

* Java 21
* Gradle
* Minecraft 1.21.11
* Fabric Loader
* Fabric API

Clone the repository:

```bash
git clone <repository-url>
cd handoff-relay
```

Build the project:

```bash
./gradlew build
```

The compiled JAR will be available in:

```text
build/libs/
```

## Pull Requests

Before opening a pull request:

* Test your changes.
* Ensure the project builds successfully.
* Keep changes focused on a single feature or fix.
* Update documentation when necessary.

Pull requests may be reviewed, modified, or declined based on project goals and maintenance requirements.

## Coding Standards

* Follow existing code style where possible.
* Use meaningful variable and method names.
* Keep code simple and maintainable.
* Add comments only where they improve understanding.

## Project Goals

Handoff Relay is designed to provide a fair, single-world relay experience where players hand off progress to the next participant.

Contributions should support:

* Stability
* Fair gameplay
* Data integrity
* Maintainability
* Performance

## Code of Conduct

Be respectful and professional when interacting with other contributors.

Harassment, discrimination, personal attacks, or disruptive behaviour will not be tolerated.

## License

By contributing to this project, you agree that your contributions will be licensed under the same license as the project.
