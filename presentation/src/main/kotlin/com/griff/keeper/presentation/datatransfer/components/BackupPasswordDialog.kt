package com.griff.keeper.presentation.datatransfer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.datatransfer.BackupPasswordProblem
import com.griff.keeper.presentation.datatransfer.BackupPasswordRules
import com.griff.keeper.presentation.datatransfer.asOptionalEmailRecipient
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * Sets the password that protects a new backup.
 *
 * ### The warning is not decoration
 *
 * The app is offline and holds no copy of the password: there is no reset, no recovery question and
 * no master key. A forgotten password means a file that nobody - including the person who wrote it -
 * can open. That is the honest consequence of the design, so it is stated plainly next to the field
 * rather than hidden in a help screen.
 *
 * ### Where the password lives
 *
 * The typed text is `remember`, never `rememberSaveable`: it survives a rotation because the
 * composition does, and it is never written into the saved-state bundle that Android may persist to
 * disk. On confirm it is handed over as a [CharArray], which the caller overwrites once the crypto is
 * done. Compose text fields work in `String`, so the app cannot promise the characters never existed
 * as one - what it can do, and does, is keep that string out of every place it would outlive the
 * dialog.
 *
 * @param recipientField when true the dialog also offers an optional e-mail address, used by "send a
 * copy". The address is only a hint for the mail app; the app never sends anything itself.
 */
@Composable
internal fun BackupPasswordDialog(
    confirmLabelRes: Int,
    recipientField: Boolean,
    onConfirm: (password: CharArray, recipient: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<BackupPasswordProblem?>(null) }
    var recipientInvalid by remember { mutableStateOf(false) }

    val submit = {
        val found = BackupPasswordRules.validate(password, confirmation)
        val trimmedRecipient = recipient.trim()
        val resolvedRecipient = recipient.asOptionalEmailRecipient()
        val recipientProblem = recipientField &&
            trimmedRecipient.isNotEmpty() &&
            resolvedRecipient == null

        problem = found
        recipientInvalid = recipientProblem
        if (found == null && !recipientProblem) {
            onConfirm(password.toCharArray(), resolvedRecipient)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.data_transfer_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(
                    text = stringResource(R.string.data_transfer_password_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        problem = null
                    },
                    label = { Text(stringResource(R.string.data_transfer_password_label)) },
                    singleLine = true,
                    visualTransformation = passwordTransformation(isVisible),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    isError = problem == BackupPasswordProblem.EMPTY ||
                        problem == BackupPasswordProblem.TOO_SHORT,
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            isVisible = isVisible,
                            onToggle = { isVisible = !isVisible },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = confirmation,
                    onValueChange = {
                        confirmation = it
                        problem = null
                    },
                    label = { Text(stringResource(R.string.data_transfer_password_confirm_label)) },
                    singleLine = true,
                    visualTransformation = passwordTransformation(isVisible),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (recipientField) ImeAction.Next else ImeAction.Done,
                    ),
                    isError = problem == BackupPasswordProblem.MISMATCH ||
                        problem == BackupPasswordProblem.CONFIRMATION_EMPTY,
                    modifier = Modifier.fillMaxWidth(),
                )

                problem?.let { PasswordProblemText(it) }

                if (recipientField) {
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = {
                            recipient = it
                            recipientInvalid = false
                        },
                        label = {
                            Text(stringResource(R.string.data_transfer_share_recipient_label))
                        },
                        supportingText = {
                            Text(
                                text = if (recipientInvalid) {
                                    stringResource(R.string.data_transfer_share_recipient_invalid)
                                } else {
                                    stringResource(R.string.data_transfer_share_recipient_hint)
                                },
                            )
                        },
                        isError = recipientInvalid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Text(
                    text = stringResource(R.string.data_transfer_password_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit) { Text(stringResource(confirmLabelRes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Asks for the password of a file that is being restored.
 *
 * Deliberately not the same dialog: there is no confirmation field (the password either opens the
 * file or it does not), and a failed attempt reports back here instead of throwing the user out of
 * the flow - the file they picked is still selected, so correcting a typo costs one keystroke.
 */
@Composable
internal fun ImportPasswordDialog(
    errorText: String?,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var isEmpty by remember { mutableStateOf(false) }

    val submit = {
        if (password.isEmpty()) {
            isEmpty = true
        } else {
            onConfirm(password.toCharArray())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.data_transfer_import_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(
                    text = stringResource(R.string.data_transfer_import_password_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isEmpty = false
                    },
                    label = { Text(stringResource(R.string.data_transfer_password_label)) },
                    singleLine = true,
                    visualTransformation = passwordTransformation(isVisible),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    isError = isEmpty || errorText != null,
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            isVisible = isVisible,
                            onToggle = { isVisible = !isVisible },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = when {
                    isEmpty -> stringResource(R.string.data_transfer_password_error_empty)
                    else -> errorText
                }
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = submit) {
                Text(stringResource(R.string.data_transfer_import_password_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PasswordProblemText(problem: BackupPasswordProblem) {
    val text = when (problem) {
        BackupPasswordProblem.EMPTY ->
            stringResource(R.string.data_transfer_password_error_empty)

        BackupPasswordProblem.TOO_SHORT -> stringResource(
            R.string.data_transfer_password_error_too_short,
            BackupPasswordRules.MIN_LENGTH,
        )

        BackupPasswordProblem.CONFIRMATION_EMPTY ->
            stringResource(R.string.data_transfer_password_error_confirm_empty)

        BackupPasswordProblem.MISMATCH ->
            stringResource(R.string.data_transfer_password_error_mismatch)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun PasswordVisibilityToggle(isVisible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = stringResource(
                if (isVisible) {
                    R.string.data_transfer_password_hide
                } else {
                    R.string.data_transfer_password_show
                },
            ),
            modifier = Modifier.size(ToggleIconSize),
        )
    }
}

private fun passwordTransformation(isVisible: Boolean): VisualTransformation =
    if (isVisible) VisualTransformation.None else PasswordVisualTransformation()

private val ToggleIconSize = 20.dp

@ThemePreviews
@Composable
private fun BackupPasswordDialogPreview() {
    GriffThemePreview {
        Row(
            modifier = Modifier.padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackupPasswordDialog(
                confirmLabelRes = R.string.data_transfer_password_create,
                recipientField = false,
                onConfirm = { _, _ -> },
                onDismiss = {},
            )
        }
    }
}
