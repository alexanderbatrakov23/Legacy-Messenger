# 📱 Legacy Messenger

**Legacy Messenger** — это полнофункциональный мессенджер для Android с собственной серверной частью на Python. Проект включает регистрацию, авторизацию, текстовые и голосовые сообщения, систему друзей, поиск пользователей и онлайн-статусы в реальном времени через WebSocket.

| Платформа | Версия | Технология | Версия |
|-----------|--------|------------|--------|
| ![Android](https://img.shields.io/badge/Android-6.0%2B-green) | 6.0+ | ![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple) | 1.9+ |
| ![Python](https://img.shields.io/badge/Python-3.8%2B-blue) | 3.8+ | ![Flask](https://img.shields.io/badge/Flask-2.0-red) | 2.0 |
| ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13-blue) | 13+ | ![License](https://img.shields.io/badge/License-MIT-yellow) | MIT |

---
## Как выглядят оболочки 
![Screenshot](https://forumstatic.ru/files/0019/f0/2b/16370.png)

## Установка 
[![GitHub Badge](https://github.com/machiav3lli/oandbackupx/raw/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png)](https://github.com/alexanderbatrakov23/Legacy-Messenger/releases/tag/legacymessanger)

---

## ✨ Возможности

### 📱 Клиентская часть (Android)

| Функция | Описание |
|---------|----------|
| 🔐 **Авторизация** | Регистрация и вход с JWT-токеном, выбор аватара-эмодзи |
| 💬 **Чаты** | Отправка текстовых сообщений, автообновление каждые 2 секунды |
| 🎙️ **Голосовые сообщения** | Запись через микрофон, отправка в Base64 |
| 👥 **Друзья** | Добавление в друзья, список друзей с онлайн-статусом |
| 📨 **Заявки** | Принять или отклонить входящие заявки в друзья |
| 🔍 **Поиск** | Поиск пользователей по логину или имени |
| 🌐 **Онлайн-статусы** | WebSocket для обновления статусов в реальном времени |
| ⚙️ **Настройки** | Просмотр профиля, выход из аккаунта |

### 🖥️ Серверная часть (Python)

| Функция | Описание |
|---------|----------|
| 🚀 **REST API** | Полный набор эндпоинтов для всех операций |
| 🔌 **WebSocket** | Flask-SocketIO для онлайн-статусов и уведомлений |
| 🔒 **Безопасность** | Хеширование паролей (bcrypt), JWT-токены |
| 🗄️ **База данных** | PostgreSQL с таблицами users, messages, friends, friend_requests |

---

## 🏗️ Архитектура
### REST API
Android App  >  WebSocket   >  Flask Server  >  PostgreSQL   

---

## 🛠️ Технологии

### Android (Клиент)

| Компонент | Технология |
|-----------|------------|
| Язык | Kotlin |
| UI | ViewBinding, RecyclerView, BottomNavigationView, TabLayout |
| Сеть | HttpURLConnection, OkHttp (WebSocket) |
| Аудио | MediaRecorder |
| Форматы | JSON, Base64 |
| Хранение | SharedPreferences |

### Сервер (Python)

| Компонент | Технология |
|-----------|------------|
| Фреймворк | Flask + Flask-SocketIO |
| Аутентификация | JWT, bcrypt |
| База данных | PostgreSQL + psycopg2 |
| CORS | flask-cors |

---

## 🚀 Запуск проекта

### 1. Установка и запуск сервера

#### Требования
- Python 3.8+
- PostgreSQL 13+

#### Шаги

```bash
# 1
git clone https://github.com/alexanderbatrakov23/legacy-messenger.git
cd legacy-messenger

# 2
pip install flask flask-cors flask-socketio bcrypt pyjwt psycopg2-binary

# 3
sudo -u postgres psql
CREATE DATABASE legacy_messenger;
CREATE USER legacy_user WITH PASSWORD '12345';
GRANT ALL PRIVILEGES ON DATABASE legacy_messenger TO legacy_user;
\q

# 4
python server.py
```

- Сервер запустится на http://localhost:3000

- Важно: При деплое на VPS измените host в socketio.run() на '0.0.0.0' и обновите API_URL в Android-клиенте.

## Настройка и запуск Android-клиента
### Требования
- Android Studio Hedgehog | 2023.1.1+
- Android SDK 23+ (Android 6.0)

### Шаги
- Откройте проект в Android Studio
- Измените API_URL в файлах клиента на адрес вашего сервера:
### kotlin

- private val API_URL = "http://ваш-ip:3000/api"
- Соберите проект (Build → Make Project)
- Запустите на устройстве или эмуляторе
- Разрешения: Приложение запрашивает доступ к микрофону для голосовых сообщений.

## 📡 API Endpoints

| Метод | URL | Описание |
|-------|-----|----------|
| `POST` | `/api/register` | Регистрация |
| `POST` | `/api/login` | Вход |
| `GET` | `/api/users/all/<user_id>` | Все пользователи |
| `GET` | `/api/users/search?q=&user_id=` | Поиск пользователей |
| `GET` | `/api/friends/<user_id>` | Список друзей |
| `POST` | `/api/friend/request` | Отправить заявку |
| `POST` | `/api/friend/accept` | Принять заявку |
| `POST` | `/api/friend/reject` | Отклонить заявку |
| `GET` | `/api/friend/requests/incoming/<user_id>` | Входящие заявки |
| `GET` | `/api/messages/<from>/<to>` | История сообщений |
| `POST` | `/api/message` | Отправить сообщение |

## 🔌 WebSocket События

| Событие | Направление | Описание |
|---------|-------------|----------|
| `connect` | Клиент → Сервер | Подключение |
| `auth` | Клиент → Сервер | Аутентификация `{"user_id": 1}` |
| `new_message` | Сервер → Клиент | Новое сообщение |
| `user_status` | Сервер → Клиент | Смена онлайн-статуса |
| `disconnect` | Клиент → Сервер | Отключение |
## 🗄️ Схема базы данных

```sql
-- Пользователи
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    avatar VARCHAR(10) DEFAULT '🐱',
    online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Сообщения
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    from_user_id INTEGER REFERENCES users(id),
    to_user_id INTEGER REFERENCES users(id),
    text TEXT,
    file_url TEXT,
    file_type VARCHAR(20),
    type VARCHAR(20) DEFAULT 'text',
    sent_at TIMESTAMP DEFAULT NOW()
);

-- Заявки в друзья
CREATE TABLE friend_requests (
    id SERIAL PRIMARY KEY,
    from_user_id INTEGER REFERENCES users(id),
    to_user_id INTEGER REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Друзья
CREATE TABLE friends (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    friend_id INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);
```

## 🔮 Планы по развитию
- 📷 Отправка изображений и файлов

- 🔔 Push-уведомления (Firebase Cloud Messaging)

- ✏️ Редактирование профиля (смена аватара, имени)

- 🔒 Сквозное шифрование сообщений

- 🌙 Темная тема

- 👥 Групповые чаты

- 🎨 Кастомные аватары (загрузка фото)

## 🤝 Вклад в проект
- Форкните репозиторий

- Создайте ветку для фичи (git checkout -b feature/amazing-feature)

- Закоммитьте изменения (git commit -m 'Add amazing feature')

- Запушьте ветку (git push origin feature/amazing-feature)

- Откройте Pull Request

## 📄 Лицензия
### Проект распространяется под лицензией MIT.

## 👨‍💻 Автор
### Alexander Batrakov
- GitHub: @alexanderbatrakov23

## ⭐ Поддержка проекта
- Если вам понравился проект, поставьте звезду на GitHub — это поможет другим найти его!

### 📌 Примечание: При развертывании на своем сервере не забудьте изменить API_URL во всех файлах клиента и host в socketio.run().
