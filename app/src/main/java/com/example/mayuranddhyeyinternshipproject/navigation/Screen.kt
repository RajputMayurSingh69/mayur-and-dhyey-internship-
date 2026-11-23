package com.example.mayuranddhyeyinternshipproject.navigation

/**
 * App navigation destinations.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object Home : Screen("home")
    object History : Screen("history")
    object MedicineList : Screen("medicine_list")
    object MedicineDetails : Screen("medicine_details/{medicineId}") {
        fun createRoute(medicineId: String) = "medicine_details/$medicineId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Terms : Screen("terms")
    object PrivacyPolicy : Screen("privacy_policy")
}
