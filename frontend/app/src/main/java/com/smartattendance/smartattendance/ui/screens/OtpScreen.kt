package com.smartattendance.smartattendance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.smartattendance.smartattendance.ui.components.*
import com.smartattendance.smartattendance.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun OtpScreen(
    phoneNumber: String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    var otpState by remember { mutableStateOf(OtpVerifyState.IDLE) }
    var secondsLeft by remember { mutableStateOf(45) }
    var canResend by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("Incorrect code. Please try again.") }

    val activity = LocalContext.current as android.app.Activity
    var verificationId by remember { mutableStateOf("") }
    var forceResendingToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    val sendVerificationCode = {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This is handled automatically on Android if SMS retriever triggers
                    auth.currentUser?.linkWithCredential(credential)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) onVerified()
                    }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    otpState = OtpVerifyState.ERROR
                    errorMsg = e.localizedMessage ?: "Failed to send SMS."
                }
                override fun onCodeSent(
                    verId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = verId
                    forceResendingToken = token
                }
            })
        
        if (forceResendingToken != null) {
            optionsBuilder.setForceResendingToken(forceResendingToken!!)
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    // Shake offset for wrong OTP
    val shakeOffset by animateFloatAsState(
        targetValue = if (otpState == OtpVerifyState.ERROR) 10f else 0f,
        animationSpec = if (otpState == OtpVerifyState.ERROR)
            spring(dampingRatio = Spring.DampingRatioHighBouncy)
        else spring(),
        label = "shake"
    )

    // Countdown timer
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        canResend = true
    }

    // Auto-focus first box
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
        sendVerificationCode()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            WaveHeader(height = 160.dp, onBack = onBack) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 16.dp)
                ) {
                    StepProgressBar(currentStep = 2, modifier = Modifier.padding(end = 48.dp, bottom = 10.dp))
                    Text(
                        "Verify your number",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Enter the 6-digit code sent to\n${phoneNumber.replaceRange(4, 8, "XXXX")}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                )

                Spacer(Modifier.height(32.dp))

                // 6 OTP boxes
                Row(
                    modifier = Modifier
                        .graphicsLayer { translationX = shakeOffset },
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    otpDigits.forEachIndexed { index, digit ->
                        val boxColor = when {
                            otpState == OtpVerifyState.ERROR   -> ErrorRed
                            otpState == OtpVerifyState.SUCCESS -> SuccessGreen
                            digit.isNotEmpty()                 -> AuthPurple
                            else                               -> Color(0xFFE2E8F0)
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(2.dp, boxColor, RoundedCornerShape(10.dp))
                                .background(
                                    when (otpState) {
                                        OtpVerifyState.ERROR   -> ErrorRedLight
                                        OtpVerifyState.SUCCESS -> SuccessGreenLight
                                        else                   -> SurfaceWhite
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = digit,
                                onValueChange = { newVal ->
                                    val clean = newVal.filter { it.isDigit() }.take(1)
                                    otpDigits[index] = clean
                                    otpState = OtpVerifyState.IDLE
                                    if (clean.isNotEmpty() && index < 5) {
                                        focusRequesters[index + 1].requestFocus()
                                    } else if (clean.isEmpty() && index > 0) {
                                        focusRequesters[index - 1].requestFocus()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .focusRequester(focusRequesters[index]),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    color = boxColor,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Error/Success message
                AnimatedVisibility(visible = otpState == OtpVerifyState.ERROR) {
                    Text(
                        errorMsg,
                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Countdown / Resend
                if (!canResend) {
                    Text(
                        "Resend OTP in ${String.format("%02d", secondsLeft)}s",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                } else {
                    TextButton(onClick = {
                        secondsLeft = 45
                        canResend = false
                        otpDigits.fill("")
                        sendVerificationCode()
                    }) {
                        Text(
                            "Resend OTP",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AuthPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                PrimaryButton(
                    text = "Verify",
                    color = AuthPurple,
                    enabled = otpDigits.all { it.isNotEmpty() },
                    isLoading = otpState == OtpVerifyState.VERIFYING,
                    onClick = {
                        otpState = OtpVerifyState.VERIFYING
                        val enteredOtp = otpDigits.joinToString("")
                        
                        // We must link the credential to the ALREADY signed-in student
                        if (verificationId.isEmpty()) {
                            otpState = OtpVerifyState.ERROR
                            errorMsg = "Verification ID empty. Please wait or Resend."
                            return@PrimaryButton
                        }
                        
                        val credential = PhoneAuthProvider.getCredential(verificationId, enteredOtp)
                        auth.currentUser?.linkWithCredential(credential)
                            ?.addOnSuccessListener {
                                otpState = OtpVerifyState.SUCCESS
                                scope.launch {
                                    delay(500)
                                    onVerified()
                                }
                            }
                            ?.addOnFailureListener { e ->
                                otpState = OtpVerifyState.ERROR
                                errorMsg = e.localizedMessage ?: "Invalid code."
                            }
                    }
                )
            }
        }
    }
}

private enum class OtpVerifyState { IDLE, VERIFYING, ERROR, SUCCESS }
