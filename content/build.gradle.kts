// The data model and the loader for the design tree's data files: no libGDX.
plugins {
    id("vanguard.java-conventions")
    `java-library`
}

dependencies {
    api(project(":sim"))
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.yaml)
}
