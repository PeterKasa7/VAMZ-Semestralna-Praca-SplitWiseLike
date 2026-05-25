package com.example.semestralna_praca_vamz.data.firebase

/**
 * Typy rozdelenia výdavkov v aplikácii.
 */
enum class SplitType {
    EQUAL, EXACT, PERCENTAGE
}

/**
 * Reprezentuje používateľa v systéme.
 * @property id Unikátne ID priradené systémom Firebase Auth.
 * @property name Zobrazované meno používateľa.
 * @property email Registračný e-mail slúžiaci na vyhľadávanie a pridávanie do skupín.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

/**
 * Reprezentuje skupinu (tím) v aplikácii.
 * @property id Unikátne ID dokumentu vo Firestore.
 * @property name Názov skupiny.
 * @property notificationsEnabled Príznak, či sú pre túto skupinu zapnuté notifikácie.
 * @property members Zoznam ID používateľov, ktorí sú členmi skupiny.
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val notificationsEnabled: Boolean = false,
    val members: List<String> = emptyList()
)

/**
 * Reprezentuje konkrétny finančný výdavok.
 * @property id Unikátne ID výdavku.
 * @property groupId ID skupiny, ktorej výdavok patrí.
 * @property paidByUserId ID používateľa, ktorý výdavok zaplatil.
 * @property amountCents Celková suma v centoch (kvôli numerickej presnosti).
 * @property description Krátky popis výdavku.
 * @property createdAt Čas vytvorenia v milisekundách.
 * @property splitType Spôsob, akým je výdavok rozdelený.
 * @property shares Mapa určujúca, koľko konkrétne dlhuje každý účastník (UserID -> suma v centoch).
 */
data class Expense(
    val id: String = "",
    val groupId: String = "",
    val paidByUserId: String = "",
    val amountCents: Long = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val splitType: SplitType = SplitType.EQUAL,
    val shares: Map<String, Long> = emptyMap()
)
