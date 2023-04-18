package io.redlink.more.more_app_mutliplatform.android.activities.login.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.redlink.more.more_app_mutliplatform.android.R
import io.redlink.more.more_app_mutliplatform.android.activities.login.LoginViewModel
import io.redlink.more.more_app_mutliplatform.android.extensions.getStringResource
import io.redlink.more.more_app_mutliplatform.android.ui.theme.MoreColors

@Composable
fun ParticipationKeyInput(
    model: LoginViewModel,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = getStringResource(id = R.string.more_registration_token_label),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MoreColors.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                value = model.participantKey.value,
                onValueChange = {
                    model.participantKey.value = it
                    model.error.value = null
                },
                trailingIcon = {
                    if (model.isTokenError()) {
                        Icon(Icons.Filled.Error, "Error", tint = MoreColors.Important)
                    }
                },
                textStyle = TextStyle(fontSize = 16.sp),
                placeholder = {
                    Text(
                        text = getStringResource(id = R.string.more_registration_token_placeholder),
                        fontSize = 16.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                isError = model.isTokenError(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MoreColors.Primary,
                    focusedLabelColor = MoreColors.Primary,
                    backgroundColor = Color.Transparent,
                    unfocusedLabelColor = MoreColors.Primary,
                    errorLabelColor = MoreColors.Important,
                    cursorColor = MoreColors.Primary,
                    errorBorderColor = MoreColors.Important,
                    errorCursorColor = MoreColors.Important,
                    errorLeadingIconColor = MoreColors.Important,
                    errorTrailingIconColor = MoreColors.Important,
                    placeholderColor = MoreColors.TextInactive,
                    unfocusedBorderColor = MoreColors.Primary,
                    focusedBorderColor = MoreColors.Primary
                ),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .height(60.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorMessage(
                    hasError = model.error.value != null,
                    errorMsg = model.error.value ?: getStringResource(
                        id = R.string.more_token_error
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ValidationButton(model = model, focusManager = focusManager)
        }

    }
}