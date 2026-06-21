package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.viewmodel.TunisiaSatViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Force Right-to-Left (RTL) layout direction globally for perfect Arabic delivery
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TunisiaSatApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunisiaSatApp(viewModel: TunisiaSatViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    val activeTopic by viewModel.selectedTopic.collectAsState()
    val activeConversation by viewModel.activeConversation.collectAsState()
    val latestPush by viewModel.latestPushNotification.collectAsState()

    val topics by viewModel.topics.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSearchSection by viewModel.selectedSearchSection.collectAsState()
    val isSearchingActive by viewModel.isSearchingActive.collectAsState()

    // Filtered topics based on search settings
    val displayedTopics = remember(topics, searchQuery, selectedSearchSection) {
        topics.filter { topic ->
            val matchesQuery = topic.title.contains(searchQuery, ignoreCase = true) || 
                               topic.body.contains(searchQuery, ignoreCase = true) ||
                               topic.author.contains(searchQuery, ignoreCase = true)
            val matchesSection = selectedSearchSection == "all" || topic.sectionId == selectedSearchSection
            matchesQuery && matchesSection
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                // Header with the beautiful generated brand banner
                TunisiaSatHeader(
                    currentUser = currentUser,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    selectedSection = selectedSearchSection,
                    onSectionChange = { viewModel.selectSearchSection(it) },
                    sections = sections,
                    onProfileTap = { viewModel.setTab(4) }
                )
            },
            bottomBar = {
                TunisiaSatBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { 
                        viewModel.setTab(it)
                        // Reset sub-flows when navigating
                        if (it != 0) viewModel.selectTopic(null)
                        if (it != 2) viewModel.selectConversation(null)
                    },
                    unreadBadgeCount = viewModel.getUnreadCount()
                )
            },
            floatingActionButton = {
                // Floating Action Button to post new topic (Only on main feed & sections)
                if (currentTab == 0 || currentTab == 1) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.setShowEditor(true) },
                        containerColor = BrandRedAccent,
                        contentColor = BrandWhite,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = "أضف منشوراً") },
                        text = { Text("موضوع جديد", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        modifier = Modifier.padding(bottom = 16.dp, start = 16.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Direct Screens transition handling
                when (currentTab) {
                    0 -> { // General Home Feed Screen
                        if (activeTopic != null) {
                            TopicDetailsScreen(
                                topic = activeTopic!!,
                                onBack = { viewModel.selectTopic(null) },
                                onPostReply = { viewModel.addReply(it) }
                            )
                        } else {
                            HomeFeedScreen(
                                topics = displayedTopics,
                                onTopicSelected = { viewModel.selectTopic(it.id) },
                                isFiltered = isSearchingActive,
                                onClearFilters = { viewModel.updateSearchQuery("") }
                            )
                        }
                    }
                    1 -> { // Forum classification Tree Screen
                        if (activeTopic != null) {
                            TopicDetailsScreen(
                                topic = activeTopic!!,
                                onBack = { viewModel.selectTopic(null) },
                                onPostReply = { viewModel.addReply(it) }
                            )
                        } else {
                            ForumSectionsScreen(
                                sections = sections,
                                topics = topics,
                                onSectionSelected = { sectionId ->
                                    viewModel.selectSearchSection(sectionId)
                                    viewModel.setTab(0) // Switch to feed
                                }
                            )
                        }
                    }
                    2 -> { // Direct Conversations Screen
                        if (activeConversation != null) {
                            ActiveChatScreen(
                                conversation = activeConversation!!,
                                onBack = { viewModel.selectConversation(null) },
                                onSendMessage = { viewModel.sendDirectMessage(it) }
                            )
                        } else {
                            ConversationsInboxScreen(
                                conversations = conversations,
                                onConversationSelected = { viewModel.selectConversation(it.id) }
                            )
                        }
                    }
                    3 -> { // Notifications & Simulator Panel
                        NotificationsScreen(
                            notifications = notifications,
                            onNotificationClick = { topicId ->
                                if (topicId == "pm") {
                                    viewModel.setTab(2)
                                } else {
                                    viewModel.selectTopic(topicId)
                                    viewModel.setTab(0)
                                }
                            },
                            onSimulateNotification = { title, message ->
                                viewModel.simulatePushNotification(title, message)
                            },
                            onClearBadge = { viewModel.markAllNotificationsRead() }
                        )
                    }
                    4 -> { // Profile & stats Screen
                        ProfileScreen(
                            profile = currentUser
                        )
                    }
                }
            }
        }

        // 1. Sleek BBCode Text Editor Overlay Dialog (محرر النصوص المتطور)
        if (showEditor) {
            BBCodeEditorDialog(
                sections = sections,
                onClose = { viewModel.setShowEditor(false) },
                onSubmit = { title, body, sectionId ->
                    viewModel.createTopic(title, body, sectionId)
                }
            )
        }

        // 2. Heads Up Live Floating Push Notification Banner
        AnimatedVisibility(
            visible = latestPush != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 45.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            latestPush?.let { push ->
                ElevatedCard(
                    onClick = {
                        viewModel.clearActivePushNotification()
                        if (push.topicId == "pm") {
                            viewModel.setTab(2)
                        } else {
                            viewModel.selectTopic(push.topicId)
                            viewModel.setTab(0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = BrandBlueDark,
                        contentColor = BrandWhite
                    ),
                    elevation = CardDefaults.elevatedCardElevation(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandRedAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (push.type == "pm") Icons.Default.Email else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = BrandWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = push.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandWhite
                                )
                                Text(
                                    text = "الآن",
                                    fontSize = 11.sp,
                                    color = TextLightSlate
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = push.message,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = BrandWhite.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(onClick = { viewModel.clearActivePushNotification() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextLightSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// UI COMPONENT: TunisiaSatHeader
// -------------------------------------------------------------
@Composable
fun TunisiaSatHeader(
    currentUser: UserProfile,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSection: String,
    onSectionChange: (String) -> Unit,
    sections: List<ForumSection>,
    onProfileTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = BrandBlueDark),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            // Hero graphic with abstract connectivity design generated by AIDeep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_tunisiasat_banner),
                    contentDescription = "Tunisia-Sat Logo Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Overlay to preserve design readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    BrandBlueDark.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Branding Elements inside the Hero Panel
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // National Flag Accent Spot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BrandRedAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تونيزيا سات",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = BrandWhite
                            )
                        }
                        Text(
                            text = "منتدى الإبداع والتميز العربي الأول",
                            fontSize = 12.sp,
                            color = BrandWhite.copy(alpha = 0.75f)
                        )
                    }

                    // Logged in User Profile Avatar Quick Shortcut
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandWhite.copy(alpha = 0.15f))
                            .clickable { onProfileTap() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(currentUser.avatarColorSeed)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.username.take(1),
                                color = BrandWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentUser.username,
                            color = BrandWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick search & category pills row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("ابحث في مواضيع وأخبار تونيزيا سات...", color = TextLightSlate, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = TextLightSlate) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextLightSlate)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BrandWhite.copy(alpha = 0.08f),
                        unfocusedContainerColor = BrandWhite.copy(alpha = 0.08f),
                        focusedBorderColor = DarkAccentBlue,
                        unfocusedBorderColor = BrandWhite.copy(alpha = 0.15f),
                        focusedTextColor = BrandWhite,
                        unfocusedTextColor = BrandWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable category shortcut filter pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSection == "all",
                            onClick = { onSectionChange("all") },
                            label = { Text("الكل", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = BrandWhite,
                                selectedLabelColor = BrandWhite,
                                selectedContainerColor = BrandRedAccent
                            )
                        )
                    }
                    items(sections) { section ->
                        FilterChip(
                            selected = selectedSection == section.id,
                            onClick = { onSectionChange(section.id) },
                            label = { Text(section.name.replace("منتدى", "").replace("قسم", "").trim(), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = BrandWhite,
                                selectedLabelColor = BrandWhite,
                                selectedContainerColor = BrandSoftBlue
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// UI COMPONENT: TunisiaSatBottomBar
// -------------------------------------------------------------
@Composable
fun TunisiaSatBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    unreadBadgeCount: Int
) {
    NavigationBar(
        containerColor = BrandBlueDark,
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        val tabs = listOf(
            Triple("الرئيسية", Icons.Default.Home, 0),
            Triple("الأقسام", Icons.Default.Menu, 1),
            Triple("الرسائل", Icons.Default.Email, 2),
            Triple("الإشعارات", Icons.Default.Notifications, 3),
            Triple("الملف", Icons.Default.Person, 4)
        )

        tabs.forEach { (title, icon, index) ->
            NavigationBarItem(
                selected = currentTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Box {
                        Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp))
                        // Display reactive notifications alerts badges
                        if ((index == 3 || index == 2) && unreadBadgeCount > 0) {
                            val badgeText = if (index == 2) "١" else "$unreadBadgeCount"
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-8).dp)
                                    .background(BrandRedAccent, shape = CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = BrandWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandWhite,
                    unselectedIconColor = TextLightSlate,
                    selectedTextColor = BrandWhite,
                    unselectedTextColor = TextLightSlate,
                    indicatorColor = BrandSoftBlue
                )
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN: HomeFeedScreen
// -------------------------------------------------------------
@Composable
fun HomeFeedScreen(
    topics: List<ForumTopic>,
    onTopicSelected: (ForumTopic) -> Unit,
    isFiltered: Boolean,
    onClearFilters: () -> Unit
) {
    if (topics.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextLightSlate)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "لم يتم العثور على نتائج للبحث في تونيزيا سات",
                fontSize = 16.sp,
                color = TextLightSlate,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedAccent)
            ) {
                Text("عرض كل المواضيع العامة")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFiltered) "نتائج التصفية والبحث الحية" else "العناصر الرائجة وأحدث المشاركات",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "عدد المواضيع: ${topics.size}",
                        fontSize = 12.sp,
                        color = TextLightSlate
                    )
                }
            }

            items(topics) { topic ->
                TopicCardItem(topic = topic, onClick = { onTopicSelected(topic) })
            }
        }
    }
}

@Composable
fun TopicCardItem(topic: ForumTopic, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Section label & Pinned tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(BrandSoftBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = topic.sectionName.replace("منتدى", "").replace("قسم", "").trim(),
                            color = BrandSoftBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (topic.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(BrandRedAccent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "مثبت 📌",
                                color = BrandRedAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(text = topic.date, fontSize = 11.sp, color = TextLightSlate)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Title
            Text(
                text = topic.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body Text Preview
            Text(
                text = topic.body,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tag badges row
            if (topic.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topic.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "#$tag", fontSize = 10.sp, color = TextLightSlate)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Author & Replies/Views stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(BrandSoftBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(topic.author.take(1), fontSize = 11.sp, color = BrandWhite)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = topic.author, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = topic.authorRank, fontSize = 10.sp, color = TextLightSlate)
                    }
                }

                // Stats
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "ردود", tint = TextLightSlate, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${topic.replies}", fontSize = 12.sp, color = TextLightSlate)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = "مشاهدات", tint = TextLightSlate, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${topic.views}", fontSize = 12.sp, color = TextLightSlate)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: ForumSectionsScreen (شجرة الأقسام الهرمية)
// -------------------------------------------------------------
@Composable
fun ForumSectionsScreen(
    sections: List<ForumSection>,
    topics: List<ForumTopic>,
    onSectionSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "الأقسام والمنتديات المتخصصة لتونيزيا سات",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(sections) { section ->
            val iconVector = when (section.iconName) {
                "satellite" -> Icons.Default.Settings
                "tech" -> Icons.Default.Build
                "sports" -> Icons.Default.Star
                "news" -> Icons.Default.Info
                "trade" -> Icons.Default.ShoppingCart
                else -> Icons.Default.Share
            }

            Card(
                onClick = { onSectionSelected(section.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandSoftBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = section.name,
                            tint = BrandSoftBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = section.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = section.description,
                            fontSize = 12.sp,
                            color = TextLightSlate,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "المواضيع: ${section.topicsCount}",
                                fontSize = 11.sp,
                                color = TextLightSlate
                            )
                            Text(
                                "المشاركات: ${section.postsCount}",
                                fontSize = 11.sp,
                                color = TextLightSlate
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ArrowBack, // Back is forward in RTL
                        contentDescription = "دخول",
                        tint = TextLightSlate,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: TopicDetailsScreen (صفحة تفاصيل وعرض الردود)
// -------------------------------------------------------------
@Composable
fun TopicDetailsScreen(
    topic: ForumTopic,
    onBack: () -> Unit,
    onPostReply: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowForward, // RTL back orientation
                    contentDescription = "رجوع",
                    tint = BrandSoftBlue
                )
            }
            Text(
                "جدول الردود والنقاش",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Original post content
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BrandSoftBlue.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, BrandSoftBlue.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandRedAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(topic.author.take(1), color = BrandWhite, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(topic.author, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(topic.authorRank, fontSize = 10.sp, color = BrandRedAccent)
                                }
                            }
                            Text(topic.date, fontSize = 11.sp, color = TextLightSlate)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = topic.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = topic.body,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Replies header
            item {
                Text(
                    "ردود الأعضاء الموثقة (${topic.repliesList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextLightSlate,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (topic.repliesList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("كن الأول في كتابة رد مميز على هذا الموضوع!", color = TextLightSlate, fontSize = 13.sp)
                    }
                }
            } else {
                items(topic.repliesList) { reply ->
                    var likedCount by remember { mutableStateOf(reply.likes) }
                    var hasLiked by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(reply.avatarColorSeed)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(reply.author.take(1), color = BrandWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(reply.author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(reply.authorRank, fontSize = 10.sp, color = TextLightSlate)
                                }
                                Text(reply.date, fontSize = 11.sp, color = TextLightSlate)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(reply.content, fontSize = 13.sp, lineHeight = 18.sp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (hasLiked) {
                                                likedCount--
                                                hasLiked = false
                                            } else {
                                                likedCount++
                                                hasLiked = true
                                            }
                                        }
                                        .background(if (hasLiked) BrandRedAccent.copy(alpha = 0.1f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "أعجبني",
                                        tint = if (hasLiked) BrandRedAccent else TextLightSlate,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("شكر ($likedCount)", fontSize = 11.sp, color = if (hasLiked) BrandRedAccent else TextLightSlate)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Input bar to compose reply at the bottom
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("أكتب تعليقاً أو رداً على الموضوع...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandSoftBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        if (replyText.isNotEmpty()) {
                            onPostReply(replyText)
                            replyText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandRedAccent),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = BrandWhite)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "إرسال الرد", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: ConversationsInboxScreen (الرسائل الخاصة)
// -------------------------------------------------------------
@Composable
fun ConversationsInboxScreen(
    conversations: List<DirectMessage>,
    onConversationSelected: (DirectMessage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "الرسائل الخاصة والمباحثات الشخصية",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(conversations) { dm ->
                Card(
                    onClick = { onConversationSelected(dm) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dm.isUnread) BrandSoftBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (dm.isUnread) BorderStroke(1.dp, BrandSoftBlue.copy(alpha = 0.25f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(dm.avatarColorSeed)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dm.sender.take(1), color = BrandWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dm.sender,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = dm.relativeTime,
                                    fontSize = 11.sp,
                                    color = TextLightSlate
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dm.lastMessage,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (dm.isUnread) MaterialTheme.colorScheme.primary else TextLightSlate
                            )
                        }

                        if (dm.isUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BrandRedAccent)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: ActiveChatScreen (صندوق المحادثة الخاصة)
// -------------------------------------------------------------
@Composable
fun ActiveChatScreen(
    conversation: DirectMessage,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = BrandSoftBlue)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(conversation.avatarColorSeed)),
                contentAlignment = Alignment.Center
            ) {
                Text(conversation.sender.take(1), color = BrandWhite, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(conversation.sender, fontWeight = FontWeight.Bold)
                Text("متصل بـ XenForo API", fontSize = 10.sp, color = BrandSoftBlue)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(conversation.messages) { msg ->
                val alignment = if (msg.isMines) Alignment.End else Alignment.Start
                val bg = if (msg.isMines) BrandSoftBlue else MaterialTheme.colorScheme.surface
                val tc = if (msg.isMines) BrandWhite else MaterialTheme.colorScheme.onSurface

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                bg,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (msg.isMines) 12.dp else 2.dp,
                                    bottomEnd = if (msg.isMines) 2.dp else 12.dp
                                )
                            )
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(text = msg.text, fontSize = 13.sp, color = tc, lineHeight = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = msg.relativeTime, fontSize = 9.sp, color = TextLightSlate)
                }
            }
        }

        // Input bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillPadding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("أرسل رسالة فورية آمنة...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandSoftBlue
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotEmpty()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandRedAccent)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "إرسال", tint = BrandWhite)
                }
            }
        }
    }
}

// Helper expansion mapping
fun Modifier.fillPadding(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp) = this.padding(horizontal = horizontal, vertical = vertical)

// -------------------------------------------------------------
// SCREEN: NotificationsScreen (الإشعارات ومحاكي الإرسال الفوري)
// -------------------------------------------------------------
@Composable
fun NotificationsScreen(
    notifications: List<InAppNotification>,
    onNotificationClick: (String) -> Unit,
    onSimulateNotification: (String, String) -> Unit,
    onClearBadge: () -> Unit
) {
    var customTitle by remember { mutableStateOf("") }
    var customMsg by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // notification badge indicator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "تنبيهات وإشعارات تونيزيا سات المباشرة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                TextButton(onClick = onClearBadge) {
                    Text("تعليم المقروء ✔", color = BrandSoftBlue, fontSize = 12.sp)
                }
            }
        }

        // A. PUSH NOTIFICATIONS SIMULATOR DRAWER (مهمة الإشعارات الفورية)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandRedAccent.copy(alpha = 0.25f)),
                colors = CardDefaults.cardColors(
                    containerColor = BrandRedAccent.copy(alpha = 0.04f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandRedAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = BrandWhite, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "محاكي التنبيهات الفورية (Push Push Mock server)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandRedAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "جرب سرعة استقبال الإشعارات! أدخل نصاً وسيصلك إشعار فوري منسدل تفاعلي من أعلى الشاشة وتحديث مؤشر الأرقام بالأسفل تلقائياً.",
                        fontSize = 11.sp,
                        color = TextLightSlate,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("عنوان التنبيه", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customMsg,
                        onValueChange = { customMsg = it },
                        label = { Text("محتوى الإشعار الصادر", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (customTitle.isNotBlank() && customMsg.isNotBlank()) {
                                onSimulateNotification(customTitle, customMsg)
                                customTitle = ""
                                customMsg = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إرسال إشعار فوري الآن 🚀", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                "سجل الإشعارات المستلمة",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextLightSlate,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد إشعارات حالياً.", color = TextLightSlate)
                }
            }
        } else {
            items(notifications) { notif ->
                Card(
                    onClick = { onNotificationClick(notif.topicId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else BrandSoftBlue.copy(alpha = 0.05f)
                    ),
                    border = if (!notif.isRead) BorderStroke(1.dp, BrandSoftBlue.copy(alpha = 0.15f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (notif.isRead) TextLightSlate.copy(alpha = 0.2f) else BrandSoftBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (notif.type == "like") Icons.Default.Favorite else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (notif.isRead) TextLightSlate else BrandSoftBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = notif.relativeTime,
                            fontSize = 11.sp,
                            color = TextLightSlate
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: ProfileScreen (الملف الشخصي)
// -------------------------------------------------------------
@Composable
fun ProfileScreen(profile: UserProfile) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "ركن العضوية والملف الشخصي الموحد",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(getAvatarColor(profile.avatarColorSeed)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.username.take(1),
                            fontSize = 32.sp,
                            color = BrandWhite,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = profile.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .background(BrandRedAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = profile.rank,
                            color = BrandRedAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Raw user counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${profile.postsCount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BrandSoftBlue
                            )
                            Text("المشاركات", fontSize = 12.sp, color = TextLightSlate)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${profile.reactionPoints}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BrandSoftBlue
                            )
                            Text("نقاط التفاعل", fontSize = 12.sp, color = TextLightSlate)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${profile.points}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BrandSoftBlue
                            )
                            Text("النقاط الإجمالية", fontSize = 12.sp, color = TextLightSlate)
                        }
                    }
                }
            }
        }

        // Additional information list
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البريد الإلكتروني", fontSize = 13.sp, color = TextLightSlate)
                        Text("saidiahmed45@gmail.com", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تاريخ التسجيل بالمنتدى", fontSize = 13.sp, color = TextLightSlate)
                        Text(profile.registrationDate, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("طواقم الإدارة والتوثيق", fontSize = 13.sp, color = TextLightSlate)
                        Text("عضوية معتمدة رسمياً ✔", fontSize = 13.sp, color = BrandSoftBlue)
                    }
                }
            }
        }

        // Logout/Sign out button
        item {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedAccent.copy(alpha = 0.1f), contentColor = BrandRedAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("تسجيل الخروج من الحساب الموحد", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// OVERLAY DIALOG: BBCodeEditorDialog (محرر النصوص المتقدم)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BBCodeEditorDialog(
    sections: List<ForumSection>,
    onClose: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("satellite") }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "محرر نصوص تونيزيا سات المتطور ✍",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select section field
                Text("اختر القسم المناسب للموضوع:", fontSize = 12.sp, color = TextLightSlate)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sections) { section ->
                        val isSelected = selectedSection == section.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandSoftBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { selectedSection = section.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = section.name.replace("منتدى", "").replace("قسم", "").trim(),
                                fontSize = 11.sp,
                                color = if (isSelected) BrandWhite else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("أدخل عنوان الموضوع الجديد...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // BBCODE SHORTCUTS PANEL (أمر: محرر النصوص متطور)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shortcuts = listOf(
                        "B" to "[B]النص العريض[/B]",
                        "I" to "[I]النص المائل[/I]",
                        "اقتباس" to "[QUOTE]هذا نص مقتبس للدراسة[/QUOTE]",
                        "رابط" to "[URL='https://tunisia-sat.com']تونيزيا-سات[/URL]",
                        "صورة" to "[IMG]https://image.url/logo.png[/IMG]"
                    )

                    shortcuts.forEach { (label, insertion) ->
                        TextButton(
                            onClick = { rawText += insertion },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = BrandSoftBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text body input
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("اكتب محتوى موضوعك هنا... يمكنك الاستعانة بأزرار التنسيق المرفقة بالأعلى.", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandSoftBlue
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Panel
                Text("المعاينة الفورية للتنسيقات (Live BBQ preview):", fontSize = 11.sp, color = TextLightSlate)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val renderedText = remember(rawText) {
                        rawText
                            .replace("[B]", "★ ")
                            .replace("[/B]", " ★")
                            .replace("[I]", "✓ ")
                            .replace("[/I]", " ✓")
                            .replace("[QUOTE]", "« ")
                            .replace("[/QUOTE]", " »")
                            .replace("\\[URL='.*?'\\]".toRegex(), "🌐 [رابط] ")
                            .replace("[/URL]", "")
                            .replace("[IMG]", "🖼 [صورة] (")
                            .replace("[/IMG]", ")")
                    }
                    Text(
                        text = if (renderedText.isEmpty()) "جاري المعاينة التلقائية..." else renderedText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء الأمر")
                    }

                    Button(
                        onClick = {
                            if (title.isNotEmpty() && rawText.isNotEmpty()) {
                                onSubmit(title, rawText, selectedSection)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نشر الموضوع بالمنتدى", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper colors for simulated avatars to avoid plain look
fun getAvatarColor(seed: Int): Color {
    return when (seed % 6) {
        0 -> Color(0xFF0077B6)
        1 -> Color(0xFFD62828)
        2 -> Color(0xFF00B4D8)
        3 -> Color(0xFF4361EE)
        4 -> Color(0xFFF77F00)
        else -> Color(0xFF415A77)
    }
}
