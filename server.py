#!/usr/bin/env python3
from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_socketio import SocketIO, emit, join_room, leave_room
import bcrypt
import jwt
import psycopg2
from psycopg2.extras import RealDictCursor
import datetime
import threading

app = Flask(__name__)
CORS(app)
socketio = SocketIO(app, cors_allowed_origins="*", async_mode='threading')

app.config['SECRET_KEY'] = '12345'

# ПОДКЛЮЧЕНИЕ К БД
DB_CONFIG = {
    'host': 'localhost',
    'database': 'legacy_messenger',
    'user': 'legacy_user',
    'password': '12580',
    'port': 5432
}

def get_db():
    try:
        conn = psycopg2.connect(**DB_CONFIG, cursor_factory=RealDictCursor)
        return conn
    except Exception as e:
        print(f"❌ Ошибка БД: {e}")
        return None

# СОЗДАНИЕ ТАБЛИЦ
def init_db():
    conn = get_db()
    if not conn:
        return
    cur = conn.cursor()
    
    cur.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            login VARCHAR(50) UNIQUE NOT NULL,
            name VARCHAR(100) NOT NULL,
            surname VARCHAR(100),
            password_hash VARCHAR(255) NOT NULL,
            avatar VARCHAR(10) DEFAULT '🐱',
            online BOOLEAN DEFAULT FALSE,
            last_seen TIMESTAMP DEFAULT NOW(),
            created_at TIMESTAMP DEFAULT NOW()
        )
    ''')
    
    cur.execute('''
        CREATE TABLE IF NOT EXISTS messages (
            id SERIAL PRIMARY KEY,
            from_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            to_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            text TEXT,
            file_url TEXT,
            file_type VARCHAR(20),
            type VARCHAR(20) DEFAULT 'text',
            sent_at TIMESTAMP DEFAULT NOW(),
            delivered BOOLEAN DEFAULT FALSE
        )
    ''')
    
    cur.execute('''
        CREATE TABLE IF NOT EXISTS friend_requests (
            id SERIAL PRIMARY KEY,
            from_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            to_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            status VARCHAR(20) DEFAULT 'pending',
            created_at TIMESTAMP DEFAULT NOW(),
            UNIQUE(from_user_id, to_user_id)
        )
    ''')
    
    cur.execute('''
        CREATE TABLE IF NOT EXISTS friends (
            id SERIAL PRIMARY KEY,
            user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            friend_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            created_at TIMESTAMP DEFAULT NOW(),
            UNIQUE(user_id, friend_id)
        )
    ''')
    
    conn.commit()
    cur.close()
    conn.close()
    print("✅ База данных готова")

# ============ РЕГИСТРАЦИЯ ============
@app.route('/api/register', methods=['POST'])
def register():
    data = request.json
    login = data.get('login')
    name = data.get('name')
    surname = data.get('surname')
    password = data.get('password')
    avatar = data.get('avatar', '🐱')
    
    print(f"📝 Регистрация: {login}")
    
    conn = get_db()
    if not conn:
        return jsonify({'error': 'Ошибка БД'}), 500
    
    cur = conn.cursor()
    
    cur.execute('SELECT id FROM users WHERE login = %s', (login,))
    if cur.fetchone():
        cur.close()
        conn.close()
        return jsonify({'error': 'Логин уже занят'}), 400
    
    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt())
    
    cur.execute(
        'INSERT INTO users (login, name, surname, password_hash, avatar) VALUES (%s, %s, %s, %s, %s) RETURNING id',
        (login, name, surname, hashed.decode(), avatar)
    )
    user_id = cur.fetchone()['id']
    conn.commit()
    
    cur.close()
    conn.close()
    
    print(f"✅ Зарегистрирован: {login}")
    return jsonify({'success': True, 'userId': user_id})

# ============ ВХОД ============
@app.route('/api/login', methods=['POST'])
def login():
    data = request.json
    login = data.get('login')
    password = data.get('password')
    
    print(f"🔑 Вход: {login}")
    
    conn = get_db()
    if not conn:
        return jsonify({'error': 'Ошибка БД'}), 500
    
    cur = conn.cursor()
    cur.execute('SELECT id, login, name, surname, avatar, password_hash FROM users WHERE login = %s', (login,))
    user = cur.fetchone()
    cur.close()
    conn.close()
    
    if not user:
        return jsonify({'error': 'Неверный логин или пароль'}), 401
    
    if not bcrypt.checkpw(password.encode(), user['password_hash'].encode()):
        return jsonify({'error': 'Неверный логин или пароль'}), 401
    
    token = jwt.encode({
        'id': user['id'],
        'login': user['login'],
        'exp': datetime.datetime.utcnow() + datetime.timedelta(days=30)
    }, app.config['SECRET_KEY'], algorithm='HS256')
    
    print(f"✅ Вошёл: {login}")
    return jsonify({
        'token': token,
        'user': {
            'id': user['id'],
            'login': user['login'],
            'name': user['name'],
            'surname': user['surname'],
            'avatar': user['avatar']
        }
    })

# ============ ВСЕ ПОЛЬЗОВАТЕЛИ ============
@app.route('/api/users/all/<int:user_id>', methods=['GET'])
def get_all_users(user_id):
    conn = get_db()
    if not conn:
        return jsonify([]), 500
    
    cur = conn.cursor()
    cur.execute('''
        SELECT id, login, name, surname, avatar, online 
        FROM users 
        WHERE id != %s
        ORDER BY name ASC
    ''', (user_id,))
    
    users = cur.fetchall()
    cur.close()
    conn.close()
    
    return jsonify(users)

# ============ ПОИСК ============
@app.route('/api/users/search', methods=['GET'])
def search_users():
    query = request.args.get('q', '')
    user_id = request.args.get('user_id')
    
    if not query or len(query) < 2:
        return jsonify([])
    
    conn = get_db()
    if not conn:
        return jsonify([]), 500
    
    cur = conn.cursor()
    cur.execute('''
        SELECT id, login, name, surname, avatar, online
        FROM users 
        WHERE (login ILIKE %s OR name ILIKE %s) AND id != %s
        LIMIT 30
    ''', (f'%{query}%', f'%{query}%', user_id))
    
    users = cur.fetchall()
    cur.close()
    conn.close()
    
    return jsonify(users)

# ============ ОТПРАВИТЬ ЗАЯВКУ ============
@app.route('/api/friend/request', methods=['POST'])
def send_friend_request():
    data = request.json
    from_user_id = data.get('from_user_id')
    to_user_id = data.get('to_user_id')
    
    print(f"📨 Заявка: {from_user_id} -> {to_user_id}")
    
    conn = get_db()
    if not conn:
        return jsonify({'error': 'Ошибка БД'}), 500
    
    cur = conn.cursor()
    
    cur.execute('SELECT id FROM friends WHERE (user_id = %s AND friend_id = %s) OR (user_id = %s AND friend_id = %s)',
                (from_user_id, to_user_id, to_user_id, from_user_id))
    if cur.fetchone():
        cur.close()
        conn.close()
        return jsonify({'error': 'Вы уже друзья'}), 400
    
    cur.execute('SELECT id FROM friend_requests WHERE from_user_id = %s AND to_user_id = %s AND status = %s',
                (from_user_id, to_user_id, 'pending'))
    if cur.fetchone():
        cur.close()
        conn.close()
        return jsonify({'error': 'Заявка уже отправлена'}), 400
    
    cur.execute('INSERT INTO friend_requests (from_user_id, to_user_id) VALUES (%s, %s)',
                (from_user_id, to_user_id))
    conn.commit()
    
    cur.close()
    conn.close()
    
    return jsonify({'success': True})

# ============ ПРИНЯТЬ ЗАЯВКУ ============
@app.route('/api/friend/accept', methods=['POST'])
def accept_friend_request():
    data = request.json
    request_id = data.get('request_id')
    user_id = data.get('user_id')
    
    print(f"✅ Принять заявку: {request_id}")
    
    conn = get_db()
    if not conn:
        return jsonify({'error': 'Ошибка БД'}), 500
    
    cur = conn.cursor()
    
    cur.execute('SELECT from_user_id, to_user_id FROM friend_requests WHERE id = %s AND to_user_id = %s',
                (request_id, user_id))
    req = cur.fetchone()
    
    if not req:
        cur.close()
        conn.close()
        return jsonify({'error': 'Заявка не найдена'}), 404
    
    cur.execute('INSERT INTO friends (user_id, friend_id) VALUES (%s, %s), (%s, %s)',
                (req['from_user_id'], req['to_user_id'], req['to_user_id'], req['from_user_id']))
    
    cur.execute('UPDATE friend_requests SET status = %s WHERE id = %s', ('accepted', request_id))
    conn.commit()
    
    cur.close()
    conn.close()
    
    return jsonify({'success': True})

# ============ ОТКЛОНИТЬ ЗАЯВКУ ============
@app.route('/api/friend/reject', methods=['POST'])
def reject_friend_request():
    data = request.json
    request_id = data.get('request_id')
    user_id = data.get('user_id')
    
    print(f"❌ Отклонить заявку: {request_id}")
    
    conn = get_db()
    if not conn:
        return jsonify({'error': 'Ошибка БД'}), 500
    
    cur = conn.cursor()
    cur.execute('UPDATE friend_requests SET status = %s WHERE id = %s AND to_user_id = %s',
                ('rejected', request_id, user_id))
    conn.commit()
    
    cur.close()
    conn.close()
    
    return jsonify({'success': True})

# ============ ВХОДЯЩИЕ ЗАЯВКИ ============
@app.route('/api/friend/requests/incoming/<int:user_id>', methods=['GET'])
def get_incoming_requests(user_id):
    conn = get_db()
    if not conn:
        return jsonify([]), 500
    
    cur = conn.cursor()
    cur.execute('''
        SELECT fr.id, fr.from_user_id, fr.status, fr.created_at,
               u.login, u.name, u.surname, u.avatar
        FROM friend_requests fr
        JOIN users u ON fr.from_user_id = u.id
        WHERE fr.to_user_id = %s AND fr.status = 'pending'
        ORDER BY fr.created_at DESC
    ''', (user_id,))
    
    requests = cur.fetchall()
    cur.close()
    conn.close()
    
    return jsonify(requests)

# ============ ДРУЗЬЯ ============
@app.route('/api/friends/<int:user_id>', methods=['GET'])
def get_friends(user_id):
    conn = get_db()
    if not conn:
        return jsonify([]), 500
    
    cur = conn.cursor()
    cur.execute('''
        SELECT u.id, u.login, u.name, u.surname, u.avatar, u.online
        FROM friends f
        JOIN users u ON f.friend_id = u.id
        WHERE f.user_id = %s
        ORDER BY u.name ASC
    ''', (user_id,))
    
    friends = cur.fetchall()
    cur.close()
    conn.close()
    
    return jsonify(friends)

# ============ ОТПРАВИТЬ СООБЩЕНИЕ ============
@app.route('/api/message', methods=['POST'])
def send_message():
    data = request.json
    from_user_id = data.get('from_user_id')
    to_user_id = data.get('to_user_id')
    text = data.get('text', '')
    
    print(f"💬 Сообщение: {from_user_id} -> {to_user_id}: {text[:50]}")
    
    conn = get_db()
    if not conn:
        return jsonify({'error': 'Ошибка БД'}), 500
    
    cur = conn.cursor()
    cur.execute('''
        INSERT INTO messages (from_user_id, to_user_id, text, type) 
        VALUES (%s, %s, %s, 'text') RETURNING id, sent_at
    ''', (from_user_id, to_user_id, text))
    result = cur.fetchone()
    conn.commit()
    cur.close()
    conn.close()
    
    # Отправляем через WebSocket
    socketio.emit('new_message', {
        'id': result['id'],
        'from_user_id': from_user_id,
        'to_user_id': to_user_id,
        'text': text,
        'sent_at': result['sent_at'].isoformat()
    }, room=f"user_{to_user_id}")
    
    return jsonify({'success': True})

# ============ ПОЛУЧИТЬ СООБЩЕНИЯ ============
@app.route('/api/messages/<int:user_id>/<int:other_user_id>', methods=['GET'])
def get_messages(user_id, other_user_id):
    conn = get_db()
    if not conn:
        return jsonify([]), 500
    
    cur = conn.cursor()
    cur.execute('''
        SELECT id, from_user_id, to_user_id, text, sent_at
        FROM messages 
        WHERE (from_user_id = %s AND to_user_id = %s) 
           OR (from_user_id = %s AND to_user_id = %s)
        ORDER BY sent_at ASC
        LIMIT 100
    ''', (user_id, other_user_id, other_user_id, user_id))
    
    messages = cur.fetchall()
    cur.close()
    conn.close()
    
    return jsonify(messages)

# ============ WebSocket СОБЫТИЯ ============
@socketio.on('connect')
def handle_connect():
    print(f"🔌 WebSocket подключен: {request.sid}")

@socketio.on('auth')
def handle_auth(data):
    user_id = data.get('user_id')
    if user_id:
        join_room(f"user_{user_id}")
        print(f"✅ Пользователь {user_id} вошел в комнату")
        
        # Обновляем статус онлайн
        conn = get_db()
        if conn:
            cur = conn.cursor()
            cur.execute('UPDATE users SET online = TRUE WHERE id = %s', (user_id,))
            conn.commit()
            cur.close()
            conn.close()
        
        # Оповещаем всех
        socketio.emit('user_status', {'user_id': user_id, 'online': True}, broadcast=True)

@socketio.on('disconnect')
def handle_disconnect():
    print(f"🔌 WebSocket отключен: {request.sid}")

# ============ ЗАПУСК ============
if __name__ == '__main__':
    init_db()
    print("=" * 50)
    print("🚀 LEGACY MESSENGER СЕРВЕР")
    print("📍 API: http://0.0.0.0:3000/api")
    print("🔌 WebSocket: ws://72.56.13.117:3000")
    print("=" * 50)
    socketio.run(app, host='72.56.13.117', port=3000, debug=False)