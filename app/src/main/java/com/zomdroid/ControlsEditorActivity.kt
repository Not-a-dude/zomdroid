package com.zomdroid

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zomdroid.input.AbstractControlElement
import com.zomdroid.input.ControlElementDescription
import com.zomdroid.input.GLFWBinding
import com.zomdroid.input.InputControlsView

class ControlsEditorActivity : ComponentActivity() {
    private lateinit var inputControlsView: InputControlsView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                ControlsEditorScreen(
                    onInitView = { view -> inputControlsView = view }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::inputControlsView.isInitialized) {
            inputControlsView.saveControlElementsToDisk()
        }
    }
}

@Composable
fun ControlsEditorScreen(onInitView: (InputControlsView) -> Unit) {
    var selectedElement by remember { mutableStateOf<AbstractControlElement?>(null) }
    var settingsVisible by remember { mutableStateOf(false) }
    var settingsFromLeft by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val inputControlsView = remember {
        InputControlsView(context, null).apply {
            setEditMode(true)
            setBackgroundColor(0xFF232323.toInt())
            setElementSettingsController(object : InputControlsView.ElementSettingsController() {
                override fun open() {
                    selectedElement = getSelectedElement()
                    settingsFromLeft = fromLeft
                    settingsVisible = true
                }

                override fun close() {
                    settingsVisible = false
                }

                override fun hide() {
                    settingsVisible = false
                }
            })
            onInitView(this)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { inputControlsView },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = settingsVisible && selectedElement != null,
            enter = slideInHorizontally(initialOffsetX = { if (settingsFromLeft) -it else it }),
            exit = slideOutHorizontally(targetOffsetX = { if (settingsFromLeft) -it else it }),
            modifier = Modifier
                .align(if (settingsFromLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            selectedElement?.let { element ->
                ElementSettingsPanel(
                    element = element,
                    onDelete = {
                        inputControlsView.deleteSelectedElement()
                    },
                    onRefresh = {
                        inputControlsView.invalidate()
                    }
                )
            }
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text(stringResource(R.string.dialog_button_ok))
                    }
                },
                title = { Text(stringResource(R.string.dialog_title_info)) },
                text = { Text(stringResource(R.string.controls_editor_instructions)) }
            )
        }
    }
}

@Composable
fun ElementSettingsPanel(
    element: AbstractControlElement,
    onDelete: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Control Settings", style = MaterialTheme.typography.titleLarge)

            // Scale
            var scale by remember(element) { mutableFloatStateOf(element.scale) }
            Column {
                Text(text = stringResource(R.string.percentage_format, (scale * 100).toInt()))
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                        element.scale = it
                        onRefresh()
                    },
                    valueRange = 0.5f..2.0f
                )
            }

            // Opacity
            var opacity by remember(element) { mutableFloatStateOf(element.alpha / 255f) }
            Column {
                Text(text = "Opacity: ${(opacity * 100).toInt()}%")
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        element.alpha = (it * 255).toInt()
                        onRefresh()
                    },
                    valueRange = 0f..1f
                )
            }

            // Input Type
            var inputType by remember(element) { mutableStateOf(element.inputType) }
            ExposedDropdown(
                label = "Input Type",
                options = AbstractControlElement.InputType.entries.toList(),
                selectedOption = inputType,
                onOptionSelected = {
                    inputType = it
                    element.inputType = it
                    onRefresh()
                }
            )

            // Type specific settings
            when (element.type) {
                AbstractControlElement.Type.BUTTON_CIRCLE, AbstractControlElement.Type.BUTTON_RECT -> {
                    // Text
                    var text by remember(element) { mutableStateOf(element.text ?: "") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            element.text = it
                            onRefresh()
                        },
                        label = { Text("Button Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Icon
                    IconDropdown(
                        label = "Icon",
                        selectedOption = element.icon,
                        onOptionSelected = {
                            element.icon = it
                            onRefresh()
                        }
                    )

                    // Bindings
                    Text(text = "Bindings", style = MaterialTheme.typography.titleMedium)
                    val bindings = remember(element, inputType) { mutableStateListOf(*element.bindings) }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bindings.forEachIndexed { index, binding ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExposedDropdown(
                                    modifier = Modifier.weight(1f),
                                    label = "Binding ${index + 1}",
                                    options = GLFWBinding.valuesForType(inputType).toList(),
                                    selectedOption = binding,
                                    onOptionSelected = {
                                        element.setBinding(index, it)
                                        bindings[index] = it
                                        onRefresh()
                                    }
                                )
                                IconButton(onClick = {
                                    element.removeBinding(index)
                                    bindings.removeAt(index)
                                    onRefresh()
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.mt_icon_delete),
                                        contentDescription = "Delete Binding",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val newBinding = GLFWBinding.valuesForType(inputType)[0]
                                element.addBinding(newBinding)
                                bindings.add(newBinding)
                                onRefresh()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.mt_icon_add),
                                contentDescription = "Add Binding",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                AbstractControlElement.Type.DPAD, AbstractControlElement.Type.STICK -> {
                    if (inputType == AbstractControlElement.InputType.MNK) {
                        DirectionalBindings(element, onRefresh)
                    } else if (element.type == AbstractControlElement.Type.STICK) {
                        var stickBinding by remember(element) { mutableStateOf(element.bindingStick) }
                        ExposedDropdown(
                            label = "Stick Binding",
                            options = listOf(GLFWBinding.LEFT_JOYSTICK, GLFWBinding.RIGHT_JOYSTICK),
                            selectedOption = stickBinding,
                            onOptionSelected = {
                                stickBinding = it
                                element.bindingStick = it
                                onRefresh()
                            }
                        )
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mt_icon_delete),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Delete Element")
            }
        }
    }
}

@Composable
fun DirectionalBindings(element: AbstractControlElement, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Directional Bindings (MNK)", style = MaterialTheme.typography.titleMedium)

        val mnkBindings = GLFWBinding.valuesForType(AbstractControlElement.InputType.MNK).toList()

        DirectionalDropdown("Up", element.bindingUp, mnkBindings) { element.bindingUp = it; onRefresh() }
        DirectionalDropdown("Down", element.bindingDown, mnkBindings) { element.bindingDown = it; onRefresh() }
        DirectionalDropdown("Left", element.bindingLeft, mnkBindings) { element.bindingLeft = it; onRefresh() }
        DirectionalDropdown("Right", element.bindingRight, mnkBindings) { element.bindingRight = it; onRefresh() }
    }
}

@Composable
fun DirectionalDropdown(label: String, selected: GLFWBinding, options: List<GLFWBinding>, onSelected: (GLFWBinding) -> Unit) {
    var current by remember(selected) { mutableStateOf(selected) }
    ExposedDropdown(
        label = label,
        options = options,
        selectedOption = current,
        onOptionSelected = {
            current = it
            onSelected(it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExposedDropdown(
    modifier: Modifier = Modifier,
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconDropdown(
    label: String,
    selectedOption: ControlElementDescription.Icon,
    onOptionSelected: (ControlElementDescription.Icon) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var current by remember(selectedOption) { mutableStateOf(selectedOption) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = current.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = current.resId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ControlElementDescription.Icon.entries.forEach { icon ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = icon.resId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(icon.name)
                        }
                    },
                    onClick = {
                        current = icon
                        onOptionSelected(icon)
                        expanded = false
                    }
                )
            }
        }
    }
}
