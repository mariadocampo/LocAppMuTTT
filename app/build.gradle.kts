plugins {
    id("com.android.application")
}

android {
    namespace = "cat.uab.tradumatica.tripnote"
    compileSdk = 35

    defaultConfig {
        applicationId = "cat.uab.tradumatica.tripnote"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // Activa los pseudoidiomas en-XA (texto acentuado y alargado) y ar-XB (derecha a izquierda).
            // Para verlos: Opciones de desarrollador activadas > Ajustes > Idiomas > English (XA) / Arabic (XB).
            isPseudoLocalesEnabled = true
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // El informe de lint es material de clase: no debe impedir que se genere el APK.
        abortOnError = false
        checkReleaseBuilds = false
    }
}
