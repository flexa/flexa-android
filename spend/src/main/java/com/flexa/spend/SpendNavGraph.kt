package com.flexa.spend

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.flexa.core.Flexa
import com.flexa.core.toNavArgument
import com.flexa.identity.buildIdentity
import com.flexa.identity.getActivity
import com.flexa.identity.shared.ConnectResult
import com.flexa.spend.main.assets.AssetsViewModel
import com.flexa.spend.main.flexa_id.ConfirmDeleteAccount
import com.flexa.spend.main.flexa_id.DataAndPrivacy
import com.flexa.spend.main.flexa_id.DeleteAccount
import com.flexa.spend.main.flexa_id.ManageAccount
import com.flexa.spend.main.keypad.InputAmountScreen
import com.flexa.spend.main.main_screen.SpendScreen
import com.flexa.spend.main.main_screen.SpendViewModel
import com.flexa.spend.main.web_view.WebView
import com.flexa.spend.merchants.BrandsViewModel
import com.flexa.spend.merchants.MerchantsEdit
import com.flexa.spend.welcome.Welcome

const val PAY_ROUTE = "com.flexa.spend"

sealed class Route(val name: String) {
    data object Entrance : Route("$PAY_ROUTE.entrance")
    data object Welcome : Route("$PAY_ROUTE.welcome")
    data object Pay : Route("$PAY_ROUTE.pay")
    data object Brands : Route("$PAY_ROUTE.brands")
    data object InputAmount : Route("$PAY_ROUTE.input_amount")
    data object Account : Route("$PAY_ROUTE.account")
    data object DataAndPrivacy : Route("$PAY_ROUTE.data_and_privacy")
    data object DeleteAccount : Route("$PAY_ROUTE.delete_account")
    data object ConfirmDeleteAccount : Route("$PAY_ROUTE.confirm_delete_account")
    data object WebView : Route("$PAY_ROUTE.web_view?url={url}") {
        const val KEY = "url"
        val arguments = listOf(navArgument(KEY) {
            type = NavType.StringType
            defaultValue = ""
        })

        fun createRoute(url: String = ""): String {
            return "$PAY_ROUTE.web_view?$KEY=${url.toNavArgument()}"
        }
    }

}

val enterTransition = fadeIn() + scaleIn(initialScale = 1.1F)
val popEnterTransition = fadeIn() + scaleIn(initialScale = .9F)
val exitTransition = fadeOut() + scaleOut(targetScale = .9F)
val popExitTransition = fadeOut() + scaleOut(targetScale = 1.1F)

fun NavGraphBuilder.spendNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    deepLink: String? = null
) {
    navigation(startDestination = Route.Entrance.name, route = PAY_ROUTE) {
        composable(Route.Entrance.name) {
            val context = LocalContext.current
            Entrance(
                deepLink = deepLink,
                toDeepLink = {
                    if (context is Activity) {
                        Flexa.buildIdentity().onResult { res ->
                            when (res) {
                                is ConnectResult.Connected -> {
                                    navController.navigate(Route.Pay.name) {
                                        popUpTo(PAY_ROUTE) { inclusive = true }
                                    }
                                }

                                else -> {
                                    close(context, navController)
                                }
                            }
                        }.build().open(context, deepLink)
                    }
                },
                toLogin = {
                    if (context is Activity)
                        Flexa.buildIdentity().onResult { res ->
                            Log.d("TAG", "spendNavGraph: onResult: $res")
                            when (res) {
                                is ConnectResult.Connected -> {
                                    navController.navigate(Route.Pay.name) {
                                        popUpTo(PAY_ROUTE) { inclusive = true }
                                    }
                                }

                                else -> {
                                    close(context, navController)
                                }
                            }
                        }.build().open(context)
                },
                toPay = {
                    navController.navigate(Route.Pay.name) {
                        popUpTo(PAY_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Route.Welcome.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            Welcome(
                modifier = modifier,
                toStoresAndRestaurants = {},
                toLogin = {
                    if (context is Activity)
                        Flexa.buildIdentity().build().open(context)
                },
                toBack = { close(context, navController) },
                toPay = {
                    navController.navigate(Route.Pay.name) {
                        popUpTo(PAY_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Route.Pay.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            SpendScreen(
                modifier = modifier,
                viewModel = viewModel(
                    initializer = {
                        SpendViewModel(
                            interactor = Spend.interactor,
                            selectedAsset = Spend.selectedAsset
                        )
                    }, viewModelStoreOwner = context.getActivity() ?: it
                ),
                assetsViewModel = viewModel(initializer = {
                    AssetsViewModel(
                        interactor = Spend.interactor,
                        selectedAsset = Spend.selectedAsset
                    )
                }, viewModelStoreOwner = context.getActivity() ?: it),
                brandsViewModel = viewModel(
                    initializer = { BrandsViewModel(Spend.interactor) },
                    viewModelStoreOwner = context.getActivity() ?: it
                ),
                toBack = { close(context, navController) },
                toEdit = { navController.navigate(Route.Brands.name) },
                toManageAccount = { navController.navigate(Route.Account.name) },
                toUrl = { url ->
                    navController.navigate(Route.WebView.createRoute(url))
                },
                toInputAmount = {
                    navController.navigate(Route.InputAmount.name)
                },
                toLogin = {
                    if (context is Activity)
                        Flexa.buildIdentity().build().open(context)
                }
            )
        }
        composable(
            Route.Brands.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            MerchantsEdit(
                modifier = modifier,
                viewModel = viewModel(
                    initializer = { BrandsViewModel(Spend.interactor) },
                    viewModelStoreOwner = context.getActivity() ?: it
                )
            ) { navController.navigateUp() }
        }
        composable(
            Route.InputAmount.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            InputAmountScreen(
                modifier = modifier,
                viewModel = viewModel(),
                spendViewModel = viewModel(context.getActivity() ?: it),
                assetsViewModel = viewModel(context.getActivity() ?: it),
                toUrl = { url -> navController.navigate(Route.WebView.createRoute(url)) },
                toBack = { navController.navigateUp() }
            )
        }
        composable(
            Route.Account.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            ManageAccount(
                modifier = modifier,
                viewModel = viewModel(context.getActivity() ?: it),
                spendViewModel = viewModel(
                    initializer = {
                        SpendViewModel(Spend.interactor)
                    }, viewModelStoreOwner = context.getActivity() ?: it
                ),
                toBack = { navController.navigateUp() },
                toDataAndPrivacy = {
                    navController.navigate(Route.DataAndPrivacy.name)
                },
                onSignOut = {
                    Flexa.buildIdentity().build().disconnect {
                        close(context, navController)
                    }
                }
            )
        }
        composable(
            Route.DataAndPrivacy.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            DataAndPrivacy(
                modifier = modifier,
                viewModel = viewModel(context.getActivity() ?: it),
                toDeleteAccount = { navController.navigate(Route.DeleteAccount.name) },
                toLearnMore = {
                    navController.navigate(Route.WebView.createRoute("https://flexa.co/legal/privacy"))
                },
                toBack = { navController.navigateUp() }
            )
        }
        composable(
            Route.DeleteAccount.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            DeleteAccount(
                modifier = modifier,
                viewModel = viewModel(context.getActivity() ?: it),
                toContinue = { navController.navigate(Route.ConfirmDeleteAccount.name) },
                toBack = { navController.navigateUp() }
            )
        }
        composable(
            Route.ConfirmDeleteAccount.name,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
            val context = LocalContext.current
            ConfirmDeleteAccount(
                modifier = modifier,
                viewModel = viewModel(context.getActivity() ?: it),
                toContinue = {
                    context.openEmail()
                    navController.popBackStack(Route.DataAndPrivacy.name, false)
                },
                toBack = { navController.popBackStack(Route.DataAndPrivacy.name, false) }
            )
        }
        composable(
            Route.WebView.name,
            arguments = Route.WebView.arguments,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) { entry ->
            val url = entry.arguments?.getString(Route.WebView.KEY) ?: ""
            WebView(
                modifier = modifier,
                url = url
            ) { navController.popBackStack() }
        }
    }
}

@Composable
private fun Entrance(
    deepLink: String?,
    toLogin: () -> Unit,
    toPay: () -> Unit,
    toDeepLink: () -> Unit,
) {
    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            toDeepLink.invoke()
        } else {
            Flexa.buildIdentity().build().collect {
                when (it) {
                    is ConnectResult.Connected -> toPay.invoke()
                    else -> toLogin.invoke()
                }
            }
        }
    }
}

private fun close(context: Context, navController: NavHostController) {
    if (context is SpendActivity) context.finish()
    else
        navController.popBackStack(route = PAY_ROUTE, true)
}
