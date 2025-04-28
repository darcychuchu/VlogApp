package com.vlog.app.screens.home

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.vlog.app.screens.components.CommonTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.screens.components.BannerCarousel
import com.vlog.app.screens.components.CategoryRow
import com.vlog.app.screens.components.ErrorView
import com.vlog.app.screens.components.LoadingView


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context.applicationContext as Application))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.app_name),
                navController = navController,
                currentRoute = NavigationRoutes.MainRoute.Home.route
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingView(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorView(
                    message = uiState.error ?: stringResource(R.string.loading_error),
                    onRetry = { viewModel.loadHomeData() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                HomeContent(
                    uiState = uiState,
                    onVideoClick = { videoId ->
                        navController.navigate("video/$videoId")
                    },
                    onCategoryClick = { categoryId ->
                        // 导航到对应的筛选页面，并传递分类 ID
                        navController.navigate("filter?typed=$categoryId")
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onVideoClick: (String) -> Unit,
    onCategoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 轮播图
        if (uiState.banners.isNotEmpty()) {
            BannerCarousel(
                banners = uiState.banners,
                onBannerClick = { onVideoClick(it.id) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.recommendedMovies.isNotEmpty()) {
            CategoryRow(
                title = stringResource(R.string.movies),
                videos = uiState.recommendedMovies,
                onVideoClick = { onVideoClick(it.id) },
                onMoreClick = { onCategoryClick(1) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.tvSeries.isNotEmpty()) {
            CategoryRow(
                title = stringResource(R.string.tv_series),
                videos = uiState.tvSeries,
                onVideoClick = { onVideoClick(it.id) },
                onMoreClick = { onCategoryClick(2) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.comics.isNotEmpty()) {
            CategoryRow(
                title = stringResource(R.string.anime),
                videos = uiState.comics,
                onVideoClick = { onVideoClick(it.id) },
                onMoreClick = { onCategoryClick(3) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
