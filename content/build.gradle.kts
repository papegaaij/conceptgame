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

/**
 * The game reads its numbers from design/**/data.yaml at run time: they are copied with their paths
 * under design/ into the resources, with design/data-files.txt listing them for the loader.
 */
val designData = tasks.register<DesignData>("designData") {
    source.from(rootProject.layout.projectDirectory.dir("design").asFileTree.matching { include("**/data.yaml") })
    designDir = rootProject.layout.projectDirectory.dir("design")
    outputDir = layout.buildDirectory.dir("generated/design-data")
}

sourceSets.main {
    resources.srcDir(designData)
}

abstract class DesignData : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: ConfigurableFileCollection

    @get:Internal
    abstract val designDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val root = designDir.get().asFile
        val target = outputDir.get().asFile.resolve("design")
        target.deleteRecursively()
        val paths = source.files.map { it.relativeTo(root).invariantSeparatorsPath }.sorted()
        for (path in paths) {
            root.resolve(path).copyTo(target.resolve(path))
        }
        target.resolve("data-files.txt").writeText(paths.joinToString("\n", postfix = "\n"))
    }
}

tasks.test {
    // The loader test reads the data files straight from the design tree as well.
    systemProperty("vanguard.designDir", rootProject.layout.projectDirectory.dir("design").asFile.absolutePath)
    // Re-recording the replay (see ReplayTest): -Dvanguard.recordDir=<dir>, relative to the root.
    providers.systemProperty("vanguard.recordDir").orNull?.let {
        systemProperty("vanguard.recordDir", rootProject.layout.projectDirectory.dir(it).asFile.absolutePath)
    }
}
