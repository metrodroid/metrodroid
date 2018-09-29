# material-design-icons

This directory imports the upstream Google `material-design-icons` into the `icons` subdirectory.

Metrodroid includes Gradle build file, which:

- Builds it as an Android library
- Transforms all resources to use Android colour attributes.

It takes the false package name `com.google.material_design_icons`, because Android requires that we
give it a package name.
