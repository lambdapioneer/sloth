@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    `maven-publish`
    signing
}

android {
    namespace = "com.lambdapioneer.sloth.pwhash_libsodium"
    compileSdk = libs.versions.compilesdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minsdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.lambdapioneer.sloth"
            artifactId = "sloth-pwhash-libsodium"
            version = findProperty("POM_VERSION") as String

            pom {
                name.set("Sloth extension with libsodium's Argon2")
                description.set("Key stretching and deniable encryption using Secure Elements on Android. This artifact adds support for using libsodium's Argon2 as the password hashing algorithm.")
                url.set(findProperty("POM_URL") as String)
                licenses {
                    license {
                        name.set(findProperty("POM_LICENCE_NAME") as String)
                        url.set(findProperty("POM_LICENCE_URL") as String)
                    }
                }
                scm {
                    url.set(findProperty("POM_SCM_URL") as String)
                    connection.set(findProperty("POM_SCM_CONNECTION") as String)
                    developerConnection.set(findProperty("POM_SCM_DEV_CONNECTION") as String)
                }
                developers {
                    developer {
                        id.set(findProperty("POM_DEVELOPER_ID") as String)
                        name.set(findProperty("POM_DEVELOPER_NAME") as String)
                        url.set(findProperty("POM_DEVELOPER_URL") as String)
                    }
                }
            }

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri("../maven")
        }
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = findProperty("ossrhUsername") as String? ?: ""
                password = findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}

tasks.withType(Sign::class.java) {
    enabled = findProperty("disableSigning") == null
}

dependencies {
    implementation(project(":sloth"))

    implementation(libs.androidx.core.ktx)
    implementation("com.goterl:lazysodium-android:5.0.2@aar") // the "@aar" is important
    implementation("net.java.dev.jna:jna:5.8.0@aar") // the "@aar" is important

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.assertj.core)
}
