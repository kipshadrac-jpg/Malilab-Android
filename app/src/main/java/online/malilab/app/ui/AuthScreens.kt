package online.malilab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.R
import online.malilab.app.data.ApiError
import online.malilab.app.data.Auth
import online.malilab.app.ui.theme.Mali

private enum class AuthPage { Landing, SignIn, Register, Reset }

/** Everything a signed-out person sees: welcome, sign in, create an account, reset a password. */
@Composable
fun AuthFlow() {
    var page by rememberSaveable { mutableStateOf(AuthPage.Landing) }
    BackHandler(enabled = page != AuthPage.Landing) { page = AuthPage.Landing }
    StatusBarIcons(darkIcons = false)
    when (page) {
        AuthPage.Landing -> Landing(
            onSignIn = { page = AuthPage.SignIn },
            onRegister = { page = AuthPage.Register },
        )
        AuthPage.SignIn -> SignInPage(
            onBack = { page = AuthPage.Landing },
            onForgot = { page = AuthPage.Reset },
            onRegister = { page = AuthPage.Register },
        )
        AuthPage.Register -> CodePage(reset = false, onBack = { page = AuthPage.Landing }, onSignIn = { page = AuthPage.SignIn })
        AuthPage.Reset -> CodePage(reset = true, onBack = { page = AuthPage.SignIn }, onSignIn = { page = AuthPage.SignIn })
    }
}

@Composable
private fun AuthBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mali.TealTop, Mali.TealBottom))),
    ) { content() }
}

@Composable
private fun Landing(onSignIn: () -> Unit, onRegister: () -> Unit) {
    AuthBackground {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(Mali.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.logo_mark), contentDescription = "MaliLab", modifier = Modifier.size(90.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Welcome to MaliLab", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "Make an account to keep your progress, notes and plan safe on every phone.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Mali.TealDeep,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            BigButton("Create account", filled = true, onClick = onRegister)
            Spacer(Modifier.height(12.dp))
            BigButton("Sign in", filled = false, onClick = onSignIn)
        }
    }
}

@Composable
private fun BigButton(text: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = if (filled) Mali.TealFixed else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

/** A teal page with a title on top and a white card for the form. */
@Composable
private fun FormPage(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AuthBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(Modifier.padding(start = 12.dp, top = 8.dp)) { BackCircle(onBack, tint = Color.White) }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                Text(title, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontSize = 15.sp, lineHeight = 22.sp, color = Mali.TealDeep)
            }
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Mali.Surface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType,
    secret: Boolean = false,
    hidden: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (secret && hidden) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Mali.Teal,
            unfocusedBorderColor = Mali.Sand,
            focusedLabelColor = Mali.Teal,
            cursorColor = Mali.Teal,
        ),
    )
}

@Composable
private fun ShowToggle(hidden: Boolean, onToggle: () -> Unit) {
    Text(
        if (hidden) "Show password" else "Hide password",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Mali.Teal,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(vertical = 2.dp),
    )
}

@Composable
private fun ErrorText(message: String?) {
    if (message != null) {
        Text(
            message,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Mali.Pink,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Mali.Pink.copy(alpha = 0.08f))
                .padding(12.dp),
        )
    }
}

@Composable
private fun SubmitButton(text: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        if (busy) "Please wait…" else text,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (enabled && !busy) Mali.Teal else Mali.Teal.copy(alpha = 0.45f))
            .clickable(enabled = enabled && !busy, onClick = onClick)
            .padding(vertical = 15.dp),
    )
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Mali.Teal,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun SignInPage(onBack: () -> Unit, onForgot: () -> Unit, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    FormPage("Sign in", "Welcome back.", onBack) {
        Field(email, { email = it }, "Email address", KeyboardType.Email, enabled = !busy)
        Field(password, { password = it }, "Password", KeyboardType.Password, secret = true, hidden = hidden, enabled = !busy)
        ShowToggle(hidden) { hidden = !hidden }
        ErrorText(error)
        SubmitButton("Sign in", busy, email.isNotBlank() && password.isNotEmpty()) {
            scope.launch {
                busy = true
                error = null
                try {
                    Auth.signIn(email, password)
                } catch (e: ApiError) {
                    error = e.message
                }
                busy = false
            }
        }
        LinkText("Forgot your password?", onForgot)
        LinkText("New here? Create an account", onRegister)
    }
}

/** Create an account (reset = false) or choose a new password (reset = true): email, then the code and a password. */
@Composable
private fun CodePage(reset: Boolean, onBack: () -> Unit, onSignIn: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var ticket by rememberSaveable { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val purpose = if (reset) "reset" else "register"

    val title = if (reset) "Reset password" else "Create account"
    val subtitle = if (ticket == null) {
        "Enter your email address. We will email you a 6-digit code to confirm it."
    } else {
        "Check your inbox for the code, then choose your password."
    }

    FormPage(title, subtitle, onBack) {
        val current = ticket
        if (current == null) {
            Field(email, { email = it }, "Email address", KeyboardType.Email, enabled = !busy)
            ErrorText(error)
            SubmitButton("Email me a code", busy, email.isNotBlank()) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        ticket = Auth.askCode(email, purpose)
                    } catch (e: ApiError) {
                        error = e.message
                    }
                    busy = false
                }
            }
        } else {
            Text(
                "If ${email.trim()} can be used, we have emailed it a code. It works for 10 minutes.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Mali.InkSoft,
            )
            Field(code, { code = it.filter { c -> c.isDigit() }.take(6) }, "6-digit code", KeyboardType.Number, enabled = !busy)
            Field(
                password,
                { password = it },
                if (reset) "New password" else "Choose a password",
                KeyboardType.Password,
                secret = true,
                hidden = hidden,
                enabled = !busy,
            )
            ShowToggle(hidden) { hidden = !hidden }
            Text("Use at least 8 characters that are not easy to guess.", fontSize = 13.sp, color = Mali.InkSoft)
            ErrorText(error)
            SubmitButton(if (reset) "Save new password" else "Create account", busy, code.length == 6 && password.isNotEmpty()) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        Auth.verify(current, code, password)
                    } catch (e: ApiError) {
                        error = e.message
                        if (e.code == "expired" || e.code == "locked") {
                            ticket = null
                            code = ""
                        }
                    }
                    busy = false
                }
            }
            LinkText("Use a different email or ask for a new code") {
                ticket = null
                code = ""
                error = null
            }
        }
        LinkText("I already have an account", onSignIn)
    }
}
