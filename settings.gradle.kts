rootProject.name = "terran-vanguard"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("sim", "content", "game", "desktop", "pipeline")
