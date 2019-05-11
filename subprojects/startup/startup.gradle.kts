import org.gradle.gradlebuild.unittestandcompile.ModuleType

plugins {
    gradlebuild.classycle
}

dependencies {
    compile(project(":baseServices"))
    compile(project(":jvmServices"))
    compile(project(":core"))
    compile(project(":cli"))
    compile(project(":buildOption"))
    compile(project(":toolingApi"))
    compile(project(":native"))
    compile(project(":logging"))
    compile(project(":docs"))

    compile(library("asm"))
    compile(library("commons_io"))
    compile(library("commons_lang"))
    compile(library("slf4j_api"))
}

gradlebuildJava {
    moduleType = ModuleType.CORE
}

