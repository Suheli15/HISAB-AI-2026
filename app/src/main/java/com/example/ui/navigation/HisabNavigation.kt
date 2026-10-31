package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.ApiKeyManagerScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.IncomeScreen
import com.example.ui.screens.ItemsProductsScreen
import com.example.ui.screens.ReceiptScannerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopsMerchantsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.launch

enum class Screen(val title: String, val titleBn: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", "ড্যাশবোর্ড", Icons.Default.Dashboard),
    AI_CHAT("AI Chat Entry", "এআই খরচ চ্যাট", Icons.Default.AutoAwesome),
    TRANSACTIONS("Transactions", "সকল লেনদেন", Icons.Default.ReceiptLong),
    ITEMS("Items & Products", "পণ্য ও আইটেম", Icons.Default.ShoppingBag),
    SHOPS("Shops & Merchants", "দোকান ও মার্চেন্ট", Icons.Default.Store),
    INCOME("Income", "আয়ের হিসাব", Icons.Default.Payments),
    BUDGET("Budget", "বাজেট পরিকল্পনা", Icons.Default.PieChart),
    ASSISTANT("AI Assistant", "ফাইন্যান্সিয়াল সহকারী", Icons.Default.Psychology),
    RECEIPT_SCANNER("Receipt Scanner", "রসিদ স্ক্যানার", Icons.Default.Receipt),
    REPORTS("Reports & Analytics", "রিপোর্ট ও অ্যানালিটিক্স", Icons.Default.Assessment),
    EXPORT("Export Center", "এক্সপোর্ট সেন্টার", Icons.Default.FileDownload),
    API_KEYS("API Key Manager", "এআই কি ম্যানেজার", Icons.Default.Key),
    SETTINGS("Settings", "সেটিংস", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(
    viewModel: HisabViewModel
) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val toastMsg by viewModel.uiToastMessage.collectAsState()
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Hardware Back Button returns to Dashboard before exiting
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Brand Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HISAB AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Your Money. Your Insights. Your AI.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                Screen.values().forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = "${screen.titleBn} (${screen.title})",
                                fontSize = 13.sp,
                                fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("nav_drawer_item_${screen.name}"),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentScreen.titleBn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "HISAB AI • ${currentScreen.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_open_nav_drawer")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                // Primary 5 Quick Tabs
                val bottomTabs = listOf(
                    Screen.DASHBOARD,
                    Screen.AI_CHAT,
                    Screen.TRANSACTIONS,
                    Screen.ASSISTANT,
                    Screen.REPORTS
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomTabs.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.titleBn.take(8), fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("bottom_nav_${screen.name}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { currentScreen = Screen.AI_CHAT },
                        onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS },
                        onNavigateToReceiptScanner = { currentScreen = Screen.RECEIPT_SCANNER }
                    )
                    Screen.AI_CHAT -> AiChatScreen(viewModel = viewModel)
                    Screen.TRANSACTIONS -> TransactionsScreen(viewModel = viewModel)
                    Screen.ITEMS -> ItemsProductsScreen(viewModel = viewModel)
                    Screen.SHOPS -> ShopsMerchantsScreen(viewModel = viewModel)
                    Screen.INCOME -> IncomeScreen(viewModel = viewModel)
                    Screen.BUDGET -> BudgetScreen(viewModel = viewModel)
                    Screen.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                    Screen.RECEIPT_SCANNER -> ReceiptScannerScreen(viewModel = viewModel)
                    Screen.REPORTS -> ReportsScreen(viewModel = viewModel)
                    Screen.EXPORT -> ExportScreen(viewModel = viewModel)
                    Screen.API_KEYS -> ApiKeyManagerScreen(viewModel = viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
