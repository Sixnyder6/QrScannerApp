# PLANS — Google Auth Integration

## 🎯 Цель
Добавить вход через Google аккаунт параллельно с существующей системой логин/пароль.

## ❌ Что НЕ меняется
- База данных и все коллекции (`scooters`, `chats`, `shifts`, и т.д.)
- Существующая логика работы с `activeDeviceId`, `lastSeen`, `heartbeat`
- Проверка версии приложения (Force Update)
- Все остальные функции приложения

## ✅ Что нужно изменить

### 1. Структура `internal_users`
Добавить поле `email` в каждый документ пользователя:

```json
{
  "username": "muver.phone",
  "password": "001418",
  "email": "muver@gmail.com",     // ← ДОБАВИТЬ
  "displayName": "Мувер",
  "role": "muver",
  "activeDeviceId": "...",
  "lastSeen": 1777644725427,
  ...
}
2. Интерфейс добавления сотрудников (админка)
При создании нового пользователя добавить поле для ввода email:

kotlin
val newUser = mapOf(
    "username" to username,
    "password" to password,
    "email" to userEmail,           // ← ДОБАВИТЬ
    "role" to role,
    "displayName" to displayName,
    "isAllowedToWork" to true,
    "createdAt" to System.currentTimeMillis()
)

firestore.collection("internal_users").add(newUser).await()
3. AuthManager.kt — добавить метод loginWithGoogle()
kotlin
fun loginWithGoogle(email: String) {
    scope.launch {
        val query = firestore.collection("internal_users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
        
        if (query.isEmpty) {
            _authState.value = _authState.value.copy(
                error = "Аккаунт с этой почтой не зарегистрирован",
                isLoading = false
            )
            return@launch
        }
        
        val userDoc = query.documents.first()
        val userId = userDoc.id
        
        firestore.collection("internal_users").document(userId)
            .update(
                mapOf(
                    "activeDeviceId" to deviceId,
                    "lastSeen" to System.currentTimeMillis()
                )
            )
            .await()
        
        context.dataStore.edit { prefs ->
            prefs[LOGGED_IN_USER_ID_KEY] = userId
        }
        
        attachUserListener(userId)
    }
}
4. Экран входа — добавить кнопку "Войти через Google"
kotlin
// Google Sign-In кнопка
Button(
    onClick = { /* Запустить Google Sign-In */
        val googleSignInClient = ...
        googleSignInClient.signIn()
    }
) {
    Icon(Icons.Default.AccountCircle, null)
    Text("Войти через Google")
}
5. Обработка результата Google Sign-In
kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == RC_SIGN_IN) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (email != null) {
                authManager.loginWithGoogle(email)
            }
        } catch (e: ApiException) {
            // Ошибка входа через Google
        }
    }
}
📁 Файлы для изменений
Файл	Действие
internal_users (коллекция в Firestore)	Добавить поле email
Интерфейс добавления сотрудников	Добавить поле email
AuthManager.kt	Добавить loginWithGoogle()
LoginScreen (экран входа)	Добавить кнопку Google
AndroidManifest.xml	Добавить Google Services
🔧 Зависимости (build.gradle.kts)
kotlin
dependencies {
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
}
📊 Итоговая схема работы
text
Пользователь → Нажимает "Войти через Google" 
            → Выбирает аккаунт 
            → Получает email 
            → Поиск в internal_users по полю email
            → Если найден → Вход выполнен (старый activeDeviceId обновляется)
            → Если не найден → Ошибка "Аккаунт не зарегистрирован"
🧠 Для AI-агента
При любых изменениях в AuthManager.kt:

НЕ ломай loginWithGoogle()

НЕ удаляй поле email из запросов к internal_users

НЕ забывай про обновление activeDeviceId при входе через Google

Дата: май 2026


оптимизация тем и графики 
Подкоючить иторию обновлений сейчас демо 