# ChipTextField

> [!TIP]
> This repository is fork from [dokar3/ChipTextField](https://github.com/dokar3/ChipTextField)
> 
> if it supported IOS at the first time, the fork will be deleted

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.dokar3/chiptextfield/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.dokar3/chiptextfield)

Editable and customizable chips for Compose Multiplatform. Available on these platforms:

- Android (Jetpack Compose)
- Desktop
- Browser (wasmJs and canvas)

iOS support is currently lacking, see [#131](https://github.com/dokar3/ChipTextField/issues/131).

# Usage

Gradle dependency:

```groovy
// Material 2
implementation "io.github.dokar3:chiptextfield:latest_version"

// Material 3
implementation "io.github.dokar3:chiptextfield-m3:latest_version"
```

**Default (filled style)**

```kotlin
var value by remember { mutableStateOf("Initial text") }
val state = rememberChipTextFieldState<Chip>()
ChipTextField(
    state = state,
    value = value,
    onValueChange = { value = it },
    onSubmit = { text -> Chip(text) },
)
```

Simplified version if do not care about the text field value:

```kotlin
val state = rememberChipTextFieldState<Chip>()
ChipTextField(
    state = state,
    onSubmit = ::Chip,
)
```

![](/images/screenshot_filled.jpg)

**Outlined**

```kotlin
val state = rememberChipTextFieldState<Chip>()
OutlinedChipTextField(
    state = state,
    onSubmit = ::Chip,
)
```

![](/images/screenshot_outlined.jpg)

**Need a classic underline style?**

```kotlin
val state = rememberChipTextFieldState<Chip>()
ChipTextField(
    state = state,
    onSubmit = ::Chip,
    colors = TextFieldDefaults.textFieldColors(
        backgroundColor = Color.Transparent,
    ),
    contentPadding = PaddingValues(bottom = 8.dp),
)
```

![](/images/screenshot_light.png)

**Checkable chips**

```kotlin
class CheckableChip(text: String, isChecked: Boolean = false) : Chip(text) {
    var isChecked by mutableStateOf(isChecked)
}

val state = rememberChipTextFieldState(
    chips = listOf(CheckableChip(""), /*...*/),
)
BasicChipTextField(
    state = state,
    onSubmit = { null },
    readOnly = true, // Disable editing
    chipLeadingIcon = { chip -> CheckIcon(chip) }, // Show check icon if checked
    chipTrailingIcon = {}, // Hide default close button
    onChipClick = { chip -> chip.isChecked = !chip.isChecked }
)

@Composable
fun CheckIcon(chip: CheckableChip, modifier: Modifier = Modifier) { /*...*/ }
```

![](/images/screenshot_checkable.jpg)

**Avatar chips**

```kotlin
class AvatarChip(text: String, val avatarUrl: String) : Chip(text)

val state = rememberChipTextFieldState<AvatarChip>()
ChipTextField(
    state = state,
    onSubmit = { AvatarChip(it.text, AVATAR_URL) },
    chipLeadingIcon = { chip -> Avatar(chip) } // Load and display avatar
)

@Composable
fun Avatar(chip: AvatarChip, modifier: Modifier = Modifier) { /*...*/ }
```

![](/images/screenshot_avatar.png)

**Material 3**

```diff
- import com.dokar.chiptextfield.OutlinedChipTextField
+ import com.dokar.chiptextfield.m3.OutlinedChipTextField
```

![](/images/screenshot_m3.jpg)


# License

```
Copyright 2021 dokar3

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
