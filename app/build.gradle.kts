import org.apache.tools.ant.util.JavaEnvUtils.VERSION_1_8

plugins {

	alias(libs.plugins.android.application)
	alias(libs.plugins.org.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler) // <-- Correct way to apply the plugin
	id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
	namespace = "com.example.stalp"
    compileSdk = 36

    defaultConfig {
		applicationId = "com.example.stalp"
		// minSdk 33 är ett bra val för moderna funktioner som Glance.
		minSdk = 33
		targetSdk = 36

		val gitCommitCount = try {
			"git rev-list --count HEAD".execute().trim().toInt()
		} catch (e: Exception) {
			1
		}

		versionCode = gitCommitCount
		versionName = "1.0.$gitCommitCount"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
            signingConfig = signingConfigs.getByName("debug")
        }
		getByName("debug") {
			isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false
        }
	}



    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
		// Aktiverar Jetpack Compose för modulen.
		compose = true
		// Aktiverar BuildConfig för att kunna läsa versionsnamn
		buildConfig = true
	}



	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
    compileSdkMinor = 1
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "36.0.0"
}

fun String.execute(): String = Runtime.getRuntime().exec(this).inputStream.reader().readText()

// Allt beroende (dependencies) ska ligga i ett enda block.
dependencies {
	// Kärnbibliotek
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.navigation.compose)

	// Jetpack Compose
	// Använder "Bill of Materials" (BOM) för att hantera Compose-versioner.
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)

	// Glance (för App Widgets)
	implementation(libs.androidx.glance)
	implementation(libs.androidx.glance.appwidget)
	implementation(libs.androidx.glance.material3)

	// WorkManager (för bakgrundsjobb)
	implementation(libs.androidx.work.runtime.ktx)

	// DataStore (för att spara data asynkront)
	implementation(libs.androidx.datastore.preferences)

	// Testbibliotek
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.test.ext.junit)
	androidTestImplementation(libs.androidx.test.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)

	// Debug-verktyg för Compose
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}
