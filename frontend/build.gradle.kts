// Projeto raiz dos frontends Kotlin Multiplatform.
// Os plugins sao declarados aqui apenas para fixar as versoes; quem os aplica e :composeApp.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}
