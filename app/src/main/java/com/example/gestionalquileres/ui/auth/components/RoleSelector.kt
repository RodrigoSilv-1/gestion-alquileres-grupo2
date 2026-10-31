package com.example.gestionalquileres.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gestionalquileres.domain.model.UserRole

/** Selector de rol (Secretario / Administrador) del registro. */
@Composable
fun RoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Selecciona un rol",
            color = Color(0xFF222222),
            style = MaterialTheme.typography.titleMedium
        )

        RoleOption("Secretario", selectedRole == UserRole.SECRETARIO) {
            onRoleSelected(UserRole.SECRETARIO)
        }
        RoleOption("Administrador", selectedRole == UserRole.ADMIN) {
            onRoleSelected(UserRole.ADMIN)
        }
    }
}

@Composable
private fun RoleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = text, color = Color(0xFF222222))
    }
}
