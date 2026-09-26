plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "Strata"

include(":engine:core")
include(":engine:desktop")
include(":engine:tools")
include(":sandbox")
