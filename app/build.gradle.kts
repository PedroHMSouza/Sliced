plugins {
    // No AGP 9 o suporte a Kotlin já vem embutido no plugin Android ("built-in Kotlin"),
    // então não aplicamos mais o org.jetbrains.kotlin.android.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.pedrohmsouza.sliced"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pedrohmsouza.sliced"
        minSdk = 26
        targetSdk = 36 // Android 16
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // Keystore de debug versionado de propósito: assim o APK gerado no CI, no
        // Android Studio ou em qualquer máquina tem a mesma assinatura e instala por
        // cima da versão anterior sem precisar desinstalar (o que apagaria o histórico).
        // Não é uma chave secreta; para publicar na Play Store use outra chave.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // R8 (minify) fica desligado no MVP: o ganho de desempenho é pequeno para um
            // app deste tamanho e evita bugs de reflexão difíceis de diagnosticar. O build
            // release já é bem mais rápido que o debug porque não é "debuggable".
            isMinifyEnabled = false
            // APK "release" assinado com a chave acima, para instalar direto no celular.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

room {
    // Exporta o esquema do banco em JSON para versionar no Git. Quando o banco mudar
    // (versão 2, 3...), esses arquivos permitem escrever e testar as migrações.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
