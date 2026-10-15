# Contributing

Use JDK 25 and Python 3. Run `python3 build.py` before submitting a code change; it builds the JAR and executes the lookup, cache, settings and bounds checks.

For bugs, include exact versions and a small reproduction case. Performance reports are most useful when recorded in the same location with the same camera position, model count, resource pack and graphics settings.

Rendering changes should preserve draw order, lighting and per-entity state. Keep frame caches scoped to a frame, avoid retaining world entities globally, and leave models visible when their bounds cannot be established safely.

Describe the behavior changed by a pull request and the validation performed. Keep generated dependencies, logs, local configs and build output out of commits.

Contributions are provided under the project's MIT license. Describe material use of generative AI in a contribution so reviewers can understand its origin.
