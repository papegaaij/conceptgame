// LWJGL3 desktop launcher, settings file and Construo packaging.
import io.github.fourlastor.construo.Target

plugins {
    id("vanguard.java-conventions")
    application
    alias(libs.plugins.construo)
}

/** The texture atlases packed by the pipeline from assets/sprites and assets/backdrop. */
val atlases by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    atlases(project(":pipeline", "atlases"))
    implementation(project(":game"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    implementation(libs.gdx.controllers.desktop)
}

/** Generational ZGC: pauses stayed below 0.02 ms in the spike, G1 reached 2.3 ms (gate 2). */
val zgc = listOf("-XX:+UseZGC", "-XX:+ZGenerational")

application {
    mainClass = "vanguard.desktop.DesktopLauncher"
    applicationName = "terran-vanguard"
    applicationDefaultJvmArgs = zgc
}

tasks.processResources {
    // The sprite frames reach the game packed into atlases, not one by one.
    from(rootProject.layout.projectDirectory.dir("assets")) {
        exclude("sprites/**", "backdrop/**")
    }
    from(atlases) {
        into("atlas")
    }
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    // GLFW must run on the process's first thread on macOS; the Construo launcher does this itself.
    if (System.getProperty("os.name").startsWith("Mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}

/** One jar with all dependencies: Construo bundles it with the trimmed runtime. */
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
    name = "terran-vanguard"
    humanName = "Terran Vanguard"
    jarTask = fatJar.name
    roast {
        // Roast starts the JVM with ZGC and, on macOS, on the first thread (both by default);
        // on Java 21 ZGC is generational only with this flag.
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
        }
        create<Target.MacOs>("macX64") {
            architecture = Target.Architecture.X86_64
            jdkUrl = "${temurin}_x64_mac_hotspot_21.0.12.1_1.tar.gz"
            jdkSha256 = "44db0f08196daf19a47f90d13388b0c943b67663cb537f998fe29e836fa842ce"
            identifier = "nl.terranvanguard.game"
        }
        create<Target.MacOs>("macM1") {
            architecture = Target.Architecture.AARCH64
            jdkUrl = "${temurin}_aarch64_mac_hotspot_21.0.12.1_1.tar.gz"
            jdkSha256 = "3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998"
            identifier = "nl.terranvanguard.game"
        }
    }
}
