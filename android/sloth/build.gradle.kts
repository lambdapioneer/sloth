@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    `maven-publish`
    signing
}

android {
    namespace = "com.lambdapioneer.sloth"
    compileSdk = libs.versions.compilesdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minsdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
            artifactId = "sloth"
            version = findProperty("POM_VERSION") as String

            pom {
                name.set("Sloth")
                description.set("Key stretching and deniable encryption using Secure Elements on Android")
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
    implementation(libs.androidx.core.ktx)

    androidTestImplementation(project(":testing"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.assertj.core)
}
