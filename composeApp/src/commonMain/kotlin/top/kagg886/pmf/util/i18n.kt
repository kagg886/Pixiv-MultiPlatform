@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.pmf.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.DefaultResourceReader
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.LocalResourceReader
import org.jetbrains.compose.resources.Qualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.Resource
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.ResourceReader
import org.jetbrains.compose.resources.StringItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.currentOrPreview
import org.jetbrains.compose.resources.getStringItem
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.compose.resources.rememberResourceState
import org.jetbrains.compose.resources.replaceWithArgs
import top.kagg886.pmf.backend.AppConfig

object ComposeI18N {
    val locale = MutableStateFlow<Locale?>(AppConfig.locale.locale)
}

@Composable
fun stringResource(resource: StringResource): String {
    val resourceReader = LocalResourceReader.currentOrPreview
    val locale by ComposeI18N.locale.collectAsState()
    val str by rememberResourceState(resource, locale ?: Unit, { "" }) { env ->
        loadString(resource, resourceReader, env, locale)
    }
    return str
}

@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String {
    val resourceReader = LocalResourceReader.currentOrPreview
    val locale by ComposeI18N.locale.collectAsState()
    val args = formatArgs.map { it.toString() }
    val str by rememberResourceState(resource, locale ?: Unit, { "" }) { env ->
        loadString(resource, args, resourceReader, env, locale)
    }
    return str
}

suspend fun getString(resource: StringResource): String = loadString(
    resource,
    DefaultResourceReader,
    getSystemResourceEnvironment(),
    ComposeI18N.locale.value,
)

suspend fun getString(resource: StringResource, vararg formatArgs: Any): String = loadString(
    resource,
    formatArgs.map { it.toString() },
    DefaultResourceReader,
    getSystemResourceEnvironment(),
    ComposeI18N.locale.value,
)

@OptIn(InternalResourceApi::class)
private suspend fun loadString(
    resource: StringResource,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment,
    locale: Locale? = null,
): String {
    val resourceItem = resource.getResourceItemByEnvironment(environment, locale)
    val item = getStringItem(resourceItem, resourceReader) as StringItem.Value
    return item.text
}

private suspend fun loadString(
    resource: StringResource,
    args: List<String>,
    resourceReader: ResourceReader,
    environment: ResourceEnvironment,
    locale: Locale? = null,
): String {
    val str = loadString(resource, resourceReader, environment, locale)
    return str.replaceWithArgs(args)
}

@OptIn(InternalResourceApi::class)
private fun Resource.getResourceItemByEnvironment(
    environment: ResourceEnvironment,
    locale: Locale? = null,
): ResourceItem {
    // Priority of environments: https://developer.android.com/guide/topics/resources/providing-resources#table2
    items.toList()
        .filterByLocale(
            locale?.toLanguageQualifier() ?: environment.language,
            locale?.toRegionQualifier() ?: environment.region,
        )
        .also { if (it.size == 1) return it.first() }
        .filterBy(environment.theme)
        .also { if (it.size == 1) return it.first() }
        .filterByDensity(environment.density)
        .also { if (it.size == 1) return it.first() }
        .let { items ->
            if (items.isEmpty()) {
                error("Resource with ID='$id' not found")
            } else {
                error("Resource with ID='$id' has more than one file: ${items.joinToString { it.path }}")
            }
        }
}

@OptIn(InternalResourceApi::class)
private fun Locale.toLanguageQualifier(): LanguageQualifier = LanguageQualifier(language)

@OptIn(InternalResourceApi::class)
private fun Locale.toRegionQualifier(): RegionQualifier = RegionQualifier(region)

@OptIn(InternalResourceApi::class)
private fun List<ResourceItem>.filterByDensity(density: DensityQualifier): List<ResourceItem> {
    val items = this
    var withQualifier = emptyList<ResourceItem>()

    // filter with the same or better density
    val exactAndHigherQualifiers = DensityQualifier.entries
        .filter { it.dpi >= density.dpi }
        .sortedBy { it.dpi }

    for (qualifier in exactAndHigherQualifiers) {
        withQualifier = items.filter { item -> item.qualifiers.any { it == qualifier } }
        if (withQualifier.isNotEmpty()) break
    }
    if (withQualifier.isNotEmpty()) return withQualifier

    // filter with low density
    val lowQualifiers = DensityQualifier.entries
        .minus(DensityQualifier.LDPI)
        .filter { it.dpi < density.dpi }
        .sortedByDescending { it.dpi }
    for (qualifier in lowQualifiers) {
        withQualifier = items.filter { item -> item.qualifiers.any { it == qualifier } }
        if (withQualifier.isNotEmpty()) break
    }
    if (withQualifier.isNotEmpty()) return withQualifier

    // items with no DensityQualifier (default)
    // The system assumes that default resources (those from a directory without configuration qualifiers)
    // are designed for the baseline pixel density (mdpi) and resizes those bitmaps
    // to the appropriate size for the current pixel density.
    // https://developer.android.com/training/multiscreen/screendensities#DensityConsiderations
    val withNoDensity = items.filter { item ->
        item.qualifiers.none { it is DensityQualifier }
    }
    if (withNoDensity.isNotEmpty()) return withNoDensity

    // items with LDPI density
    return items.filter { item ->
        item.qualifiers.any { it == DensityQualifier.LDPI }
    }
}

@OptIn(InternalResourceApi::class)
private fun List<ResourceItem>.filterBy(qualifier: Qualifier): List<ResourceItem> {
    // Android has a slightly different algorithm,
    // but it provides the same result: https://developer.android.com/guide/topics/resources/providing-resources#BestMatch

    // filter items with the requested qualifier
    val withQualifier = filter { item ->
        item.qualifiers.any { it == qualifier }
    }

    if (withQualifier.isNotEmpty()) return withQualifier

    // items with no requested qualifier type (default)
    return filter { item ->
        item.qualifiers.none { it::class == qualifier::class }
    }
}

@OptIn(InternalResourceApi::class)
private fun List<ResourceItem>.filterByLocale(
    language: LanguageQualifier,
    region: RegionQualifier,
): List<ResourceItem> {
    val withLanguage = filter { item ->
        item.qualifiers.any { it == language }
    }

    val withExactLocale = withLanguage.filter { item ->
        item.qualifiers.any { it == region }
    }

    // if there are the exact language + the region items
    if (withExactLocale.isNotEmpty()) return withExactLocale

    val withDefaultRegion = withLanguage.filter { item ->
        item.qualifiers.none { it is RegionQualifier }
    }

    // if there are the language without a region items
    if (withDefaultRegion.isNotEmpty()) return withDefaultRegion

    // items without any locale qualifiers
    return filter { item ->
        item.qualifiers.none { it is LanguageQualifier || it is RegionQualifier }
    }
}
