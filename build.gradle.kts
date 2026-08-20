import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
    `jvm-test-suite`
}

group = "de.flyingmana.personalworktimetracker"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

// 2. FORCE both Java and Kotlin tasks to emit matching Java 23 bytecode profiles
java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
}


compose.desktop {
    application {
        mainClass = "de.flyingmana.personalworktimetracker.MainKt"
                // Explicitly force the local launch runner task to use your Microsoft OpenJDK 25
        javaHome = "C:/Program Files/Microsoft/jdk-25.0.4.7-hotspot"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PersonalWorktimeTracker"
            packageVersion = version.toString()
        }
    }
}

testing {
    suites {
        // Use type-safe compilation strings for naming standard suites
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:5.9.1")
                implementation("io.kotest:kotest-assertions-core:5.9.1")
            }
        }

        register<JvmTestSuite>("uiTest") {
            useJUnit()
            dependencies {
                implementation(project())
                implementation("junit:junit:4.13.2")
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(testing.suites.named("test"))
                    }
                }
            }
        }
    }
}


tasks.named("check") {
    dependsOn(testing.suites.named("uiTest"))
}

dependencies {
    "uiTestImplementation"(compose.desktop.currentOs)
    "uiTestImplementation"(compose.desktop.uiTestJUnit4)
}