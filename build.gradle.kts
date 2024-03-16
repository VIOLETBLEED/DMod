import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.architectury.pack200.java.Pack200Adapter
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("gg.essential.loom") version "1.3.12"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    idea
}

version = "1.16.10"
group = "me"

loom {
    silentMojangMappingsLicense()
    runConfigs {
        getByName("client") {
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            programArgs("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
            programArgs("--mixin", "dmod.mixins.json")
        }
    }
    forge {
        pack200Provider.set(Pack200Adapter())
        mixinConfig("dmod.mixins.json")
        accessTransformer("src/main/resources/META-INF/DMod_at.cfg")
    }
    mixin {
        defaultRefmapName = "mixins.dmod.refmap.json"
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.sk1er.club/repository/maven-releases/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.spongepowered.org/maven/")
}


val shadowMe: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val packageLib: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowMe("gg.essential:loader-launchwrapper:1.2.1")
    implementation("gg.essential:essential-1.8.9-forge:2581") {
        exclude(module = "asm")
        exclude(module = "asm-commons")
        exclude(module = "asm-tree")
        exclude(module = "gson")
    }

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("org.spongepowered:mixin:0.8.5")
    shadowMe("org.spongepowered:mixin:0.8.5") {
        exclude(module = "guava")
        exclude(module = "gson")
        exclude(module = "commons-io")
    }
}

sourceSets {
    main {
        output.setResourcesDir(file("${buildDir}/classes/kotlin/main"))
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("mcversion", "1.8.9")

        filesMatching("mcmod.info") {}
        expand(mapOf("version" to project.version, "mcversion" to "1.8.9"))

        dependsOn(compileJava)
    }
    named<RemapJarTask>("remapJar") {
        archiveBaseName.set("DMod")
        input.set(shadowJar.get().archiveFile)
    }
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("DMod")
        archiveClassifier.set("dev")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        configurations = listOf(shadowMe)

        exclude(
            "**/LICENSE.md",
            "**/LICENSE.txt",
            "**/LICENSE",
            "**/NOTICE",
            "**/NOTICE.txt",
            "pack.mcmeta",
            "dummyThing",
            "**/module-info.class",
            "META-INF/proguard/**",
            "META-INF/maven/**",
            "META-INF/versions/**",
            "META-INF/com.android.tools/**",
            "fabric.mod.json",
            "/**/META-INF/services/io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider"
        )
        mergeServiceFiles {
            ///
        }
    }
    named<Jar>("jar") {
        manifest {
            attributes(
                    mapOf(
                            "ModSide" to "CLIENT",
                            "ForceLoadAsMod" to true,
                            "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
                            "TweakOrder" to "0",
                            "MixinConfigs" to "dmod.mixins.json",
                            "FMLAT" to "DMod_at.cfg"
                    )
            )
        }
        dependsOn(shadowJar)
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

kotlin {
    jvmToolchain(8)
}