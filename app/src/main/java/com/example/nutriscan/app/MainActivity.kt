package com.nutriscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.data.repository.AuthRepository
import com.nutriscan.app.ui.navigation.NavGraph
import com.nutriscan.app.ui.navigation.Screen
import com.nutriscan.app.ui.theme.NutriScanTheme
import kotlinx.coroutines.launch

/**
 * NUTRISCAN - Aplikasi Scan Nutrisi Makanan Kemasan

 * ANGGOTA KELOMPOK:
  1. Dafan Rusyda Hakim - NIM: 23523011
  2. Fattan Prabowoningtyas - NIM: 23523156
  3. Roid Hylmi - NIM: 23523131
  4. Bintang - NIM: 23523195

 * INFORMASI LOGIN (Untuk Testing):

 * Email    : dafanrusyda@gmail.com
 * Password : 123456

 * CATATAN PENTING:
 * 1. Pastikan device/emulator terhubung ke internet
 * 2. Berikan izin kamera saat diminta
 * 3. API Keys sudah dikonfigurasi di Constants.kt
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            NutriScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authRepository = remember { AuthRepository() }
                    val scope = rememberCoroutineScope()

                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var isLoading by remember { mutableStateOf(true) }

                    // Validasi session saat app dibuka
                    LaunchedEffect(Unit) {
                        scope.launch {
                            try {
                                // Gunakan validasi session yang proper
                                val isLoggedIn = authRepository.isLoggedIn()
                                startDestination = if (isLoggedIn) {
                                    Screen.Home.route
                                } else {
                                    Screen.Login.route
                                }
                            } catch (e: Exception) {
                                // Jika error, arahkan ke login
                                startDestination = Screen.Login.route
                            } finally {
                                isLoading = false
                            }
                        }
                    }

                    if (isLoading) {
                        // Loading screen saat validasi session
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        startDestination?.let { destination ->
                            NavGraph(
                                navController = navController,
                                startDestination = destination
                            )
                        }
                    }
                }
            }
        }
    }
}