package com.dacksec.appvault

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dacksec.appvault.data.PinManager
import com.dacksec.appvault.databinding.ActivityAppLockBinding

class AppLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private lateinit var pinManager: PinManager
    private var enteredPin = StringBuilder()
    private var lockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pinManager = PinManager(this)
        lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)

        setupPinPad()

        binding.btnDismiss.setOnClickListener { goHome() }
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
        dots.forEachIndexed { i, dot -> dot.isSelected = i < enteredPin.length }
    }

    private fun checkPin() {
        if (pinManager.verifyPin(enteredPin.toString())) {
            // Correct PIN → close lock screen, app resumes behind it
            finish()
        } else {
            enteredPin.clear()
            updatePinDots()
            Toast.makeText(this, "Falscher PIN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onBackPressed() {
        goHome() // Prevent back-pressing past the lock
    }

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "locked_package"
    }
}
