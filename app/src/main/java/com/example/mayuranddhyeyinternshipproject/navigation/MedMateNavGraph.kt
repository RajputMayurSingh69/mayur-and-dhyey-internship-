package com.example.mayuranddhyeyinternshipproject.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.ui.screens.auth.AuthTab
import com.example.mayuranddhyeyinternshipproject.ui.screens.auth.AuthViewModel
import com.example.mayuranddhyeyinternshipproject.ui.screens.auth.LoginScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.history.HistoryViewModel
import com.example.mayuranddhyeyinternshipproject.ui.screens.history.MedicineHistoryScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.HomeScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.HomeViewModel
import com.example.mayuranddhyeyinternshipproject.ui.screens.medicine.MedicineDetailsScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.medicine.MedicineListScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.medicine.MedicineViewModel
import com.example.mayuranddhyeyinternshipproject.ui.screens.notification.NotificationScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.profile.ProfileScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.profile.ProfileViewModel
import com.example.mayuranddhyeyinternshipproject.ui.screens.settings.PrivacyPolicyScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.settings.SettingsScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.settings.TermsScreen
import com.example.mayuranddhyeyinternshipproject.ui.screens.splash.SplashScreen

@Composable
fun MedMateNavGraph(
    navController: NavHostController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    isScreenPrivacy: Boolean,
    onToggleScreenPrivacy: (Boolean) -> Unit,
    onExitApp: () -> Unit
) {
    val authRepository = AuthRepository()
    val isUserLoggedIn = authRepository.isUserLoggedIn()
    val isEmailVerified = authRepository.isEmailVerified()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                isUserLoggedIn = isUserLoggedIn,
                isEmailVerified = isEmailVerified,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                authViewModel = authViewModel,
                initialTab = AuthTab.SIGN_IN,
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Sign Up Screen (opens unified auth screen with Create Account tab selected)
        composable(Screen.SignUp.route) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                authViewModel = authViewModel,
                initialTab = AuthTab.CREATE_ACCOUNT,
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        // Verify Email Screen (Phase 2B)
        composable(Screen.VerifyEmail.route) {
            val authViewModel: AuthViewModel = viewModel()
            com.example.mayuranddhyeyinternshipproject.ui.screens.auth.VerifyEmailScreen(
                authViewModel = authViewModel,
                onVerified = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                    }
                },
                onSignOut = {
                    authRepository.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                homeViewModel = homeViewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToMedicineList = { navController.navigate(Screen.MedicineList.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notification.route) },
                onAddMedicine = {
                    navController.navigate(Screen.MedicineDetails.createRoute("new"))
                },
                onExitApp = onExitApp
            )
        }

        // History Screen
        composable(Screen.History.route) {
            val historyViewModel: HistoryViewModel = viewModel()
            MedicineHistoryScreen(
                historyViewModel = historyViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToMedicines = { navController.navigate(Screen.MedicineList.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notification.route) }
            )
        }

        // Medicine List Screen
        composable(Screen.MedicineList.route) {
            val medicineViewModel: MedicineViewModel = viewModel()
            MedicineListScreen(
                medicineViewModel = medicineViewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddMedicine = {
                    navController.navigate(Screen.MedicineDetails.createRoute("new"))
                },
                onEditMedicine = { medId ->
                    navController.navigate(Screen.MedicineDetails.createRoute(medId))
                },
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notification.route) }
            )
        }

        // Medicine Details / Add Screen
        composable(
            route = Screen.MedicineDetails.route,
            arguments = listOf(navArgument("medicineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId") ?: "new"
            val medicineViewModel: MedicineViewModel = viewModel()
            MedicineDetailsScreen(
                medicineId = medicineId,
                medicineViewModel = medicineViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                profileViewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToMedicines = { navController.navigate(Screen.MedicineList.route) }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            val authViewModel: AuthViewModel = viewModel()
            SettingsScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                isScreenPrivacy = isScreenPrivacy,
                onToggleScreenPrivacy = onToggleScreenPrivacy,
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authRepository.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Notification Screen
        composable(Screen.Notification.route) {
            NotificationScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Terms Screen
        composable(Screen.Terms.route) {
            TermsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Privacy Policy Screen
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
