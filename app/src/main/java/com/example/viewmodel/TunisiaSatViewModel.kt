package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TunisiaSatViewModel : ViewModel() {

    // Bottom Navigation Active Tab Index (0: Home, 1: Forums, 2: PMs, 3: Notifications, 4: Profile)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Forum Sections Tree
    private val _sections = MutableStateFlow<List<ForumSection>>(emptyList())
    val sections: StateFlow<List<ForumSection>> = _sections.asStateFlow()

    // Forum Topics List
    private val _topics = MutableStateFlow<List<ForumTopic>>(emptyList())
    val topics: StateFlow<List<ForumTopic>> = _topics.asStateFlow()

    // Currently Selected Detailed Topic for Reading/Replying
    private val _selectedTopic = MutableStateFlow<ForumTopic?>(null)
    val selectedTopic: StateFlow<ForumTopic?> = _selectedTopic.asStateFlow()

    // Conversations / Private Messages List
    private val _conversations = MutableStateFlow<List<DirectMessage>>(emptyList())
    val conversations: StateFlow<List<DirectMessage>> = _conversations.asStateFlow()

    // Active Conversation Message Thread View
    private val _activeConversation = MutableStateFlow<DirectMessage?>(null)
    val activeConversation: StateFlow<DirectMessage?> = _activeConversation.asStateFlow()

    // Notifications List
    private val _notifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notifications: StateFlow<List<InAppNotification>> = _notifications.asStateFlow()

    // Current User Profile State
    private val _currentUser = MutableStateFlow(
        UserProfile(
            username = "أحمد التونسي",
            rank = "عضو مميز بالمنتدى",
            postsCount = 1240,
            reactionPoints = 948,
            registrationDate = "15-04-2021",
            points = 2500,
            avatarColorSeed = 3
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Search Settings
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSearchSection = MutableStateFlow("all")
    val selectedSearchSection: StateFlow<String> = _selectedSearchSection.asStateFlow()

    private val _isSearchingActive = MutableStateFlow(false)
    val isSearchingActive: StateFlow<Boolean> = _isSearchingActive.asStateFlow()

    // Post Editor State
    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor.asStateFlow()

    val editorTitle = MutableStateFlow("")
    val editorBody = MutableStateFlow("")
    val editorSection = MutableStateFlow("satellite")

    // Heads up/Snackbar Custom Notification Event (Simulated Push Notifications)
    private val _latestPushNotification = MutableStateFlow<InAppNotification?>(null)
    val latestPushNotification: StateFlow<InAppNotification?> = _latestPushNotification.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        // Init Sections
        _sections.value = listOf(
            ForumSection("satellite", "قسم الفضائيات والساتلايت", "متابعة أحدث أخبار أجهزة الاستقبال والقنوات الفضائية والشيرنج والدنغل.", "satellite", 45120, 120400),
            ForumSection("tech", "منتدى التكنولوجيا والبرمجيات", "مواضيع البرمجة، الحواسيب، شبكات الاتصال وهندسة البرمجيات والهواتف الذكية.", "developer_board", 23110, 89400),
            ForumSection("sports", "المنتدى الرياضي", "مواضيع كرة القدم العالمية والمحلية، التغطيات الحصرية للبطولات التونسية والأوروبية.", "sports_soccer", 67800, 310500),
            ForumSection("news", "الأخبار المحلية والعالمية", "متابعة حيّة للأخبار السياسية والاقتصادية بموضوعية ومصداقية تامة.", "newspaper", 98400, 412400),
            ForumSection("trade", "منتدى المعاملات التجارية والبيع", "مساحة مخصصة للبيع والشراء للأجهزة الإلكترونية والخدمات القانونية.", "shopping_cart", 15200, 48300),
            ForumSection("general", "المنتدى العام والمناقشات", "مساحة حرة للتحاور الفكري، نقاشات هادفة حول الحياة والمجتمع والتنمية.", "forum", 31200, 150800)
        )

        // Init Topics
        _topics.value = listOf(
            ForumTopic(
                id = "topic_1",
                title = "📌 متابعة جديد قنوات الـ MULTISTREAM على القمر Eutelsat 9B وجديد الترددات",
                author = "كبير مشرفي الساتلايت",
                authorRank = "إداري محترف",
                sectionId = "satellite",
                sectionName = "قسم الفضائيات والساتلايت",
                date = "اليوم، 14:20",
                replies = 4,
                views = 1204,
                body = "السلام عليكم ورحمة الله وبركاته،\nأحييكم أعضاء وزوار منتدى تونيزيا سات الفضائي. نفتح هذا الموضوع الموحد لمتابعة ترددات قنوات الملتيستريم على القمر الصناعي الحصري Eutelsat 9B مع رصد لأقوى الإشارات وكيفية معالجة تيونر الأجهزة لاستقطاب الترددات الحساسة.",
                isPinned = true,
                tags = listOf("Satellite", "Multistream", "Eutelsat"),
                repliesList = listOf(
                    TopicReply("r1", "طارق سات", "عضو نشط", 1, "اليوم، 14:35", "شكراً جزيلاً أخي الغالي على التحديث المستمر. قمت اليوم بالتقاط التردد 12111 عمودي والاشارة مستقرة بنسبة 82% في تونس العاصمة بجهاز StarSat 200 Extreme.", 4),
                    TopicReply("r2", "سليم الفضائي", "عضو ذهبي", 2, "اليوم, 14:50", "موضوع ممتاز وقيم! هل من الممكن تزويدنا بالترددات المخصصة للقنوات الإيطالية والفرنسية المفتوحة حالياً؟ بارك الله فيكم.", 2)
                )
            ),
            ForumTopic(
                id = "topic_2",
                title = "📌 دليلك الكامل لبرمجة تطبيقات الأندرويد الحديثة باستخدام Jetpack Compose",
                author = "أستاذ البرمجيات",
                authorRank = "مستشار فني",
                sectionId = "tech",
                sectionName = "منتدى التكنولوجيا والبرمجيات",
                date = "أمس، 09:12",
                replies = 3,
                views = 840,
                body = "أهلاً بالجميع في منتدى البرمجة بتونيزيا سات.\nاليوم سنستعرض ورشة كاملة من الصفر لاحتراف تصميم واجهات وتطبيقات الهاتف باستعمال Jetpack Compose والمكتبات الحديثة مثل Coroutines, StateFlow و Clean Architecture. تابعوا الردود للمصادر البرمجية وقائمة المشاريع.",
                isPinned = true,
                tags = listOf("Android", "Kotlin", "Compose"),
                repliesList = listOf(
                    TopicReply("tech_r1", "مجدي مطور", "عضو محترف", 4, "أمس، 11:32", "أفضل ورشة رأيتها! كنت أبحث عن كيفية تنظيم الـ System Architecture بالشكل الصحيح باستعمال ViewModel والـ Resource binding. متابع بشغف أخي المبدع.", 8),
                    TopicReply("tech_r2", "فاطمة بروغ", "عضو جديد", 5, "أمس، 13:10", "شكراً على هذا الشرح المبسط باللغة العربية البليغة. هل سيتم التطرق لدمج واجهات API وقواعد البيانات المحلية Room؟", 3)
                )
            ),
            ForumTopic(
                id = "topic_3",
                title = "متابعة سوق الانتقالات الصيفية الحصرية للأندية التونسية لموسم 2026/2027",
                author = "صوت الرياضة",
                authorRank = "صحفي المنتدى",
                sectionId = "sports",
                sectionName = "المنتدى الرياضي",
                date = "أمس، 18:30",
                replies = 15,
                views = 4210,
                body = "متابعة حصرية لانتقالات اللاعبين وملاكات الأندية التونسية في كرة القدم المحترفة (الترجي الرياضي، النادي الإفريقي، النجم الساحلي، النادي الصفاقسي). سنقوم بنشر الأخبار الرسمية الموثقة والعقود المبرمة فور صدورها من الجامعة التونسية.",
                isPinned = false,
                tags = listOf("انتقالات", "تونس", "كرة قدم"),
                repliesList = emptyList()
            ),
            ForumTopic(
                id = "topic_4",
                title = "آخر المستجدات الاقتصادية الوطنية: خطط تفعيل التنمية الرقمية والصادرات البرمجية",
                author = "تونس الرقمية",
                authorRank = "محرر أخبار أول",
                sectionId = "news",
                sectionName = "الأخبار المحلية والعالمية",
                date = "منذ يومين، 10:45",
                replies = 28,
                views = 5600,
                body = "شهد المجلس الوزاري التونسي الأخير إقرار حزمة من التسهيلات الموجهة للشركات الناشئة والمطورين المصدرين للخدمات الرقمية، والرامية لتسهيل جلب العملة الصعبة وتحويل الأموال للمبدعين والعمل المستقل.",
                isPinned = false,
                tags = listOf("أخبار", "اقتصاد", "رقمنة"),
                repliesList = emptyList()
            ),
            ForumTopic(
                id = "topic_5",
                title = "عرض مميز: جهاز استقبال أندرويد جيون Geant OTT 750 4K EVO للبيع بسوسة",
                author = "بائع الالكترونيات",
                authorRank = "تاجر موثوق",
                sectionId = "trade",
                sectionName = "منتدى المعاملات التجارية والبيع",
                date = "منذ 3 أيام",
                replies = 2,
                views = 310,
                body = "السلام عليكم للبيع بسوسة جهاز استقبال Geant 750 EVO الشهير بدقة 4K الفائقة مع اشتراكات سارية المفعول لجودة فورايفر الممتازة ودونجل داخلي. الجهاز بحالة المصنع كالجديد تماماً مع كامل إكسسواراته الأساسية والمقايضة غير مقبولة.",
                isPinned = false,
                tags = listOf("بيع", "جهاز استقبال", "سوسة"),
                repliesList = emptyList()
            )
        )

        // Init PM Conversations
        _conversations.value = listOf(
            DirectMessage(
                id = "pm_1",
                sender = "بشير المشرف العام",
                relativeTime = "دقيقتين",
                lastMessage = "مرحباً أحمد، لقد تم قبول مقترحك تطوير واجهة تطبيق تونيزيا سات الجديدة، يرجى التنسيق معنا.",
                isUnread = true,
                avatarColorSeed = 1,
                messages = listOf(
                    ConversationMessage("m1", "أحمد التونسي", "السلام عليكم سيدي المشرف المحترم. لقد قمت بصقل النسخة الأولية من تطبيق المنتدى الحديث ليعمل بسلاسة مطلقة وبألوان الهيئة البصرية الرسمية.", "أمس، 22:15", true),
                    ConversationMessage("m2", "بشير المشرف العام", "مرحباً أحمد، لقد تم قبول مقترحك تطوير واجهة تطبيق تونيزيا سات الجديدة، يرجى التنسيق معنا وتوفير الكود لرفعه على الخادم الرئيسي.", "منذ دقيقتين", false)
                )
            ),
            DirectMessage(
                id = "pm_2",
                sender = "مطور تونسي",
                relativeTime = "ساعتين",
                lastMessage = "هل قمت بتفعيل الـ Rest API لصفحات الـ XenForo؟",
                isUnread = false,
                avatarColorSeed = 4,
                messages = listOf(
                    ConversationMessage("dm1", "مطور تونسي", "مرحباً أخي العزيز. أعمل على نفس مشروع تطبيق تونيزيا سات. هل قمت بتفعيل الـ Rest API لصفحات الـ XenForo؟", "منذ ساعتين", false)
                )
            )
        )

        // Init Notifications
        _notifications.value = listOf(
            InAppNotification("n_1", "رد جديد في موضوعك", "قام 'سليم الفضائي' بكتابة رد جديد في موضوع متابعة الملتيستريم.", false, "منذ 5 د", "topic_1", "reply"),
            InAppNotification("n_2", "مشاركة أعجبت العضو", "أعجب 'أستاذ البرمجيات' بمشاركتك القيمة في منتدى المطورين.", true, "منذ ساعة", "topic_2", "like")
        )
    }

    // Set Active Navigation Tab
    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // Select Topic Detail
    fun selectTopic(topicId: String?) {
        if (topicId == null) {
            _selectedTopic.value = null
        } else {
            _selectedTopic.value = _topics.value.find { it.id == topicId }
        }
    }

    // Add Reply to Selected Topic
    fun addReply(content: String) {
        val topic = _selectedTopic.value ?: return
        if (content.isBlank()) return

        val newReply = TopicReply(
            id = "reply_${System.currentTimeMillis()}",
            author = _currentUser.value.username,
            authorRank = _currentUser.value.rank,
            avatarColorSeed = _currentUser.value.avatarColorSeed,
            date = "الآن",
            content = content,
            likes = 0
        )

        val updatedTopics = _topics.value.map { t ->
            if (t.id == topic.id) {
                t.copy(
                    replies = t.replies + 1,
                    repliesList = t.repliesList + newReply
                )
            } else {
                t
            }
        }

        _topics.value = updatedTopics
        _selectedTopic.value = updatedTopics.find { it.id == topic.id }

        // Boost User statistics as visual reward
        _currentUser.value = _currentUser.value.copy(
            postsCount = _currentUser.value.postsCount + 1,
            points = _currentUser.value.points + 15
        )
    }

    // Add New Forum Post/Topic
    fun createTopic(title: String, body: String, sectionId: String) {
        if (title.isBlank() || body.isBlank()) return

        val section = _sections.value.find { it.id == sectionId }
        val sectionName = section?.name ?: "المنتدى العام"

        val newTopic = ForumTopic(
            id = "topic_${System.currentTimeMillis()}",
            title = title,
            author = _currentUser.value.username,
            authorRank = _currentUser.value.rank,
            sectionId = sectionId,
            sectionName = sectionName,
            date = "الآن",
            replies = 0,
            views = 1,
            body = body,
            isPinned = false,
            tags = listOf("جديد", sectionId),
            repliesList = emptyList()
        )

        // Prepend new topic to live state
        _topics.value = listOf(newTopic) + _topics.value
        _currentUser.value = _currentUser.value.copy(
            postsCount = _currentUser.value.postsCount + 1,
            points = _currentUser.value.points + 50
        )

        // Close Editor
        setShowEditor(false)
        editorTitle.value = ""
        editorBody.value = ""
    }

    // Set Editor Open/Close State
    fun setShowEditor(show: Boolean) {
        _showEditor.value = show
    }

    // Conversation Detailed Thread Selection
    fun selectConversation(dmId: String?) {
        if (dmId == null) {
            _activeConversation.value = null
        } else {
            val conv = _conversations.value.find { it.id == dmId }
            _activeConversation.value = conv
            // Mark conversation as read
            if (conv != null && conv.isUnread) {
                _conversations.value = _conversations.value.map {
                    if (it.id == conv.id) it.copy(isUnread = false) else it
                }
            }
        }
    }

    // Send Direct PM Message (Includes Bot intelligent replies simulator)
    fun sendDirectMessage(text: String) {
        val activeConv = _activeConversation.value ?: return
        if (text.isBlank()) return

        val newMsg = ConversationMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = _currentUser.value.username,
            text = text,
            relativeTime = "الآن",
            isMines = true
        )

        val updatedConv = activeConv.copy(
            lastMessage = text,
            relativeTime = "الآن",
            isUnread = false,
            messages = activeConv.messages + newMsg
        )

        _conversations.value = _conversations.value.map {
            if (it.id == activeConv.id) updatedConv else it
        }
        _activeConversation.value = updatedConv

        // XenForo Admin trigger smart auto-response sequence to make the app interactive!
        viewModelScope.launch {
            delay(1500)
            val responderName = updatedConv.sender
            val systemResponseText = when {
                text.contains("السلام") || text.contains("أهلاً") -> "وعليكم السلام ورحمة الله وبركاته أخي أحمد. كيف يمكننا مساعدتك اليوم في إدارة الواجهة وتفاصيل خادم XenForo؟"
                text.contains("رابط") || text.contains("XenForo") || text.contains("API") -> "بالفعل، تم تفعيل الإضافة الرسمية لـ REST API على النطاق الفرعي لـ Tunisia-Sat ونحن نجرب حالياً استجابة التوكن الآمن (Bearer Token)."
                else -> "أهلاً بك أخي الكريم، تم استلام رسالتك وسيتم فحص الكود من قبل وحدة الهندسة الفنية للمنتدى وإشعارك بالنتيجة قريباً إن شاء الله."
            }

            val botMsg = ConversationMessage(
                id = "bot_msg_${System.currentTimeMillis()}",
                sender = responderName,
                text = systemResponseText,
                relativeTime = "الآن",
                isMines = false
            )

            // Overwrite in lists
            val finalConv = updatedConv.copy(
                lastMessage = systemResponseText,
                relativeTime = "الآن",
                isUnread = true, // Highlight chat tab
                messages = updatedConv.messages + botMsg
            )

            _conversations.value = _conversations.value.map {
                if (it.id == activeConv.id) finalConv else it
            }

            if (_activeConversation.value?.id == activeConv.id) {
                _activeConversation.value = finalConv
            }

            // Simulating a system-wide push notification representing the new reply!
            simulatePushNotification(
                title = "رسالة خاصة جديدة من $responderName",
                message = systemResponseText,
                type = "pm",
                topicId = "pm"
            )
        }
    }

    // Set Search Settings
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearchingActive.value = query.isNotBlank()
    }

    fun selectSearchSection(sectionId: String) {
        _selectedSearchSection.value = sectionId
    }

    // Interactive Trigger of Instant Notification (محاكي الملقم الفوري للاشعارات)
    fun simulatePushNotification(title: String, message: String, type: String = "reply", topicId: String = "topic_1") {
        val newNotification = InAppNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = title,
            message = message,
            isRead = false,
            relativeTime = "الآن",
            topicId = topicId,
            type = type
        )

        // prepend notifications list
        _notifications.value = listOf(newNotification) + _notifications.value

        // Flash as custom in-app heads-up notification card at the top
        _latestPushNotification.value = newNotification

        // Fade away snackbar notification after 4.5 seconds automatically
        viewModelScope.launch {
            delay(4500)
            if (_latestPushNotification.value?.id == newNotification.id) {
                _latestPushNotification.value = null
            }
        }
    }

    fun clearActivePushNotification() {
        _latestPushNotification.value = null
    }

    // Clear Notification Badge (marks all as read)
    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun getUnreadCount(): Int {
        val unreadNotifs = _notifications.value.count { !it.isRead }
        val unreadPMs = _conversations.value.count { it.isUnread }
        return unreadNotifs + unreadPMs
    }
}
