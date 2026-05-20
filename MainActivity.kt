package com.dacksec.appvault

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.dacksec.appvault.data.PinManager
import com.dacksec.appvault.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pinManager: PinManager
    private var enteredPin = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pinManager = PinManager(this)

        // First launch: no PIN set yet → go to setup
        if (!pinManager.hasPinSet()) {
            startActivity(Intent(this, PinSetupActivity::class.java))
            finish()
            return
        }

        setupPinPad()
        tryBiometric()
    }

    private fun tryBiometric() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            binding.btnBiometric.visibility = View.GONE
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                openVault()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Fall back to PIN – no action needed
            }
            override fun onAuthenticationFailed() {
                Toast.makeText(this@MainActivity, "Biometrie fehlgeschlagen", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Vault")
            .setSubtitle("Mit Biometrie entsperren")
            .setNegativeButtonText("PIN verwenden")
            .build()

        binding.btnBiometric.setOnClickListener { prompt.authenticate(promptInfo) }
        prompt.authenticate(promptInfo)
    }

    private fun setupPinPad() {
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (enteredPin.length < 6) {
                    enteredPin.append(index)
                    updatePinDots()
                    if (enteredPin.length == pinManager.getPinLength()) checkPin()
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDots()
            }
        }
    }

    private fun updatePinDots() {
        val dots = listOf(
            binding.dot1, binding.dot2, binding.dot3,
            binding.dot4, binding.dot5, binding.dot6
        )
        dots.forEachIndexed { i, dot ->
            dot.isSelected = i < enteredPin.length
        }
    }

    private fun checkPin() {
        if (pinManager.verifyPin(enteredPin.toString())) {
            openVault()
        } else {
            binding.layoutPinDots.animate().translationX(16f).setDuration(50)
                .withEndAction {
                    binding.layoutPinDots.animate().translationX(-16f).setDuration(50)
                        .withEndAction {
                            binding.layoutPinDots.animate().translationX(0f).setDuration(50).start()
                        }.start()
                }.start()
            enteredPin.clear()
            updatePinDots()
            Toast.makeText(this, "Falscher PIN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openVault() {
        startActivity(Intent(this, VaultActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
