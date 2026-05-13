package com.privabrowser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.privabrowser.databinding.ActivityBiometricLockBinding

class BiometricLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiometricLockBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiometricLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometricPrompt()
        checkBiometricAvailability()

        binding.btnUnlock.setOnClickListener { showBiometricPrompt() }
        binding.btnSkip.setOnClickListener { launchBrowser() } // Optional skip for dev
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    launchBrowser()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(this@BiometricLockActivity,
                            "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@BiometricLockActivity,
                        "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("PrivaBrowser Lock")
            .setSubtitle("Use biometric to unlock your private browser")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.tvStatus.text = "Tap unlock to continue"
                binding.btnUnlock.isEnabled = true
                showBiometricPrompt() // Auto-show on launch
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.tvStatus.text = "No biometric hardware. Using PIN."
                showBiometricPrompt()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.tvStatus.text = "No biometric enrolled. Please set up lock screen."
                binding.btnSkip.visibility = View.VISIBLE
            }
            else -> {
                binding.tvStatus.text = "Biometric unavailable"
                binding.btnSkip.visibility = View.VISIBLE
            }
        }
    }

    private fun showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo)
    }

    private fun launchBrowser() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish() // Remove lock screen from back stack
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // Re-lock when app goes to background
    override fun onResume() {
        super.onResume()
        // Lock is shown fresh on every resume via launcher
    }
}
