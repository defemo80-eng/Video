package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DeiPlayViewModel
import com.example.ui.theme.DeiBlack
import com.example.ui.theme.DeiPlayCoverBanner
import com.example.ui.theme.DeiRed
import com.example.ui.theme.DeiWhite

@Composable
fun AuthScreen(
    viewModel: DeiPlayViewModel,
    modifier: Modifier = Modifier
) {
    val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()

    var mode by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Register, 2 = Recover
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeiBlack, MaterialTheme.colorScheme.background)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DeiPlayCoverBanner(maxWidth = 176.dp)
            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (mode != 2) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = mode == 0,
                                onClick = { mode = 0 },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.testTag("tab_login")
                            ) {
                                Text("Iniciar sesión")
                            }
                            SegmentedButton(
                                selected = mode == 1,
                                onClick = { mode = 1 },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.testTag("tab_register")
                            ) {
                                Text("Crear cuenta")
                            }
                        }
                    } else {
                        Text(
                            "Recuperar contraseña",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(if (mode == 0) "Correo electrónico o @usuario" else "Correo electrónico")
                        },
                        leadingIcon = { Icon(Icons.Filled.Email, null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    if (mode == 1) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Nombre de usuario (@handle)") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_username_input")
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nombre visible") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_displayname_input")
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(if (mode == 2) "Nueva contraseña (mín. 6 caracteres)" else "Contraseña")
                        },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Mostrar u ocultar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    Button(
                        onClick = {
                            when (mode) {
                                0 -> viewModel.login(email, password)
                                1 -> viewModel.register(email, username, displayName, password)
                                2 -> viewModel.recoverPassword(email, password) { mode = 0 }
                            }
                        },
                        enabled = !authBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = DeiRed, contentColor = DeiWhite),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_btn")
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DeiWhite, strokeWidth = 2.dp)
                        } else {
                            Text(
                                when (mode) {
                                    0 -> "Entrar a DeiPlay"
                                    1 -> "Registrarme en DeiPlay"
                                    else -> "Actualizar contraseña"
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (mode != 2) {
                            TextButton(onClick = { mode = 2 }) {
                                Text("¿Olvidaste tu contraseña?")
                            }
                        } else {
                            TextButton(onClick = { mode = 0 }) {
                                Text("Volver a iniciar sesión")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Transparent status about Google Sign-In & YouTube OAuth without Firebase
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = DeiRed)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "Inicio con Google en la nube desactivado al rechazar Firebase; puedes crear tu cuenta segura local ahora y conectar tu canal de YouTube mediante OAuth dentro de Mi perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
