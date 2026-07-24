import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.winschneid.myliverecord"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.winschneid.myliverecord"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 署名の認証情報はリポジトリに含めない。keystore.properties（git管理外）を優先し、
    // 無ければ環境変数（CI/自動公開用）から読む。
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
    }
    fun signingValue(key: String, env: String): String? =
        keystoreProps.getProperty(key) ?: System.getenv(env)

    signingConfigs {
        getByName("debug") {
            // 任意: debug署名を端末間で揃えたい場合のみ keystore.properties で上書きする。
            // 未設定なら AGP 標準のデバッグ署名を使う。
            signingValue("debugStoreFile", "DEBUG_STORE_FILE")?.let { storePath ->
                storeFile = file(storePath)
                storePassword = signingValue("debugStorePassword", "DEBUG_STORE_PASSWORD")
                keyAlias = signingValue("debugKeyAlias", "DEBUG_KEY_ALIAS")
                keyPassword = signingValue("debugKeyPassword", "DEBUG_KEY_PASSWORD")
            }
        }
        create("release") {
            // Play公開用のアップロード鍵。keystore.properties または環境変数で指定する。
            signingValue("storeFile", "RELEASE_STORE_FILE")?.let { storePath ->
                storeFile = file(storePath)
                storePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 依存ライブラリの .so 由来のネイティブクラッシュ/ANRを Play Console で
            // シンボル化できるよう、デバッグシンボルをAABに同梱する（手動アップロード不要）。
            ndk {
                debugSymbolLevel = "FULL"
            }
            // release署名情報が揃っている時だけ専用鍵で署名する。
            // 無い環境（CI・鍵未配置のローカル）ではreleaseは未署名でビルドされる。
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore（設定の永続化）
    implementation(libs.androidx.datastore.preferences)

    // Compose Preview Screenshot Testing（src/screenshotTest の @Preview をレンダリングして比較）
    screenshotTestImplementation(platform(libs.androidx.compose.bom))
    screenshotTestImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
