// LWJGL3 desktop launcher, benchmark entry point and Construo packaging.
import io.github.fourlastor.construo.Target

plugins {
    id("spike.java-conventions")
    application
    alias(libs.plugins.construo)
}

val atlases = configurations.dependencyScope("atlases")
val atlasFiles = configurations.resolvable("atlasFiles") { extendsFrom(atlases.get()) }

dependencies {
    implementation(project(":game"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    implementation(libs.gdx.controllers.desktop)
    atlases.name(project(path = ":tools", configuration = "atlases"))
}

application {
    mainClass = "vanguard.desktop.DesktopLauncher"
    applicationName = "terran-vanguard-spike"
}

val design = rootProject.layout.projectDirectory.dir("../design")

tasks.processResources {
    from(atlasFiles) { into("atlas") }
    from(design.file("audio/music/concept/coalition-rising-full-r08-a.ogg")) { into("music") }
    from(design.dir("audio/sfx/concept")) {
        include("*.ogg")
        exclude("ambience-*")
        into("sfx")
    }
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").startsWith("Mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}

/** One jar with all dependencies, the input Construo bundles with the trimmed runtime. */
val fatJar = tasks.register<Jar>("fatJar") {
    archiveClassifier = "all"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = application.mainClass }
    from(sourceSets.main.map { it.output })
    from(configurations.runtimeClasspath.map { jars -> jars.map { zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/INDEX.LIST", "module-info.class")
}

val temurin = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk"

construo {
    name = "terran-vanguard-spike"
    humanName = "Terran Vanguard Spike"
    jarTask = fatJar.name
    roast {
        // Roast starts the JVM with ZGC by default; on Java 21 ZGC is generational only with this
        // flag (gate 2: generational ZGC pauses stayed below 0.02 ms, G1 reached 2.3 ms).
        vmArgs.add("-XX:+ZGenerational")
    }
    targets {
        create<Target.Linux>("linuxX64") {
            architecture = Target.Architecture.X86_64
            jdkUrl = "${temurin}_x64_linux_hotspot_21.0.12.1_1.tar.gz"
            jdkSha256 = "ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"
        }
        create<Target.Windows>("winX64") {
            architecture = Target.Architecture.X86_64
            jdkUrl = "${temurin}_x64_windows_hotspot_21.0.12.1_1.zip"
            jdkSha256 = "f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e"
            useConsole = true
        }
        create<Target.MacOs>("macX64") {
            architecture = Target.Architecture.X86_64
            jdkUrl = "${temurin}_x64_mac_hotspot_21.0.12.1_1.tar.gz"
            jdkSha256 = "44db0f08196daf19a47f90d13388b0c943b67663cb537f998fe29e836fa842ce"
            identifier = "nl.terranvanguard.spike"
        }
        create<Target.MacOs>("macM1") {
            architecture = Target.Architecture.AARCH64
            jdkUrl = "${temurin}_aarch64_mac_hotspot_21.0.12.1_1.tar.gz"
            jdkSha256 = "3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998"
            identifier = "nl.terranvanguard.spike"
        }
    }
}
